import besom.*
import besom.api.{docker, kubernetes as k8s}
import besom.api.kubernetes.meta.v1.inputs.ObjectMetaArgs
import besom.api.kubernetes.rbac.v1.inputs.PolicyRuleArgs
import besom.api.kubernetes.rbac.v1.{ClusterRole, ClusterRoleArgs}
import k8s.core.v1.{Namespace, NamespaceArgs, Service, ServiceAccount, ServiceAccountArgs}
import k8s.core.v1.enums.ServiceSpecType
import besom.internal.{Config, Output}
import besom.json.DefaultJsonProtocol.StringJsonFormat
import besom.json.{JsField, JsObject, JsString, JsValue, JsonReader}

@main def main = Pulumi.run {
  val appName: NonEmptyString = "vss"
  val clusterConfig = config
    .requireString("cluster")
    .map(Cluster.parseName)

  val serviceType = clusterConfig.map:
    case Cluster.Remote =>
      ServiceSpecType.LoadBalancer
    case _ =>
      ServiceSpecType.ClusterIP

  val clusterStack =
    for
      orgName   <- config.getString("cluster-org").getOrElse("organization")
      projName  <- config.requireString("cluster-project")
      stackName <- config.requireString("cluster-stack")
      stack     <- StackReference(name = s"$orgName/$projName/$stackName")
    yield stack

  val k8sProvider = clusterConfig.flatMap:
    case Cluster.Local =>
      k8s.Provider(name = s"$appName-local-provider")
    case Cluster.Remote =>
      k8s.Provider(
        name = s"$appName-remote-provider",
        k8s.ProviderArgs(kubeconfig = clusterStack.requireOutput("kubeconfig").convertTo[String])
      )

  val appNamespace = Namespace(
    name = appName,
    NamespaceArgs(metadata = ObjectMetaArgs(name = appName)),
    opts = opts(provider = k8sProvider)
  )

  val imageTag        = config.requireString("imageTag")
  val localRepository = config.requireString("localRepository")

  val registryEndpoint       = clusterStack.requireOutput("registryEndpoint").convertTo[String]
  val repositoryUrl          = clusterStack.requireOutput("repositoryUrl").convertTo[String]
  val secretAccessKeyJsValue = clusterStack.requireOutput("secretAccessKey")
  val accessKeyIdJsValue     = clusterStack.requireOutput("accessKeyId")
  val secretAccessKey        = secretAccessKeyJsValue.convertTo[String]
  val accessKeyId            = accessKeyIdJsValue.convertTo[String]

  val out = clusterConfig.flatMap:
    case Cluster.Local =>
      Output(None, p"$localRepository:$imageTag")
    case Cluster.Remote =>
      for
        dockerProvider <- docker.Provider(
          name = s"$appName-docker-provider",
          docker.ProviderArgs(
            registryAuth = List(
              docker.inputs.ProviderRegistryAuthArgs(
                address = registryEndpoint,
                username = accessKeyId,
                password = secretAccessKey
              )
            )
          )
        )
        tag <- docker.Tag(
          s"$appName-tag",
          docker.TagArgs(
            sourceImage = p"$localRepository:$imageTag",
            targetImage = p"$repositoryUrl:$imageTag"
          ),
          opts = opts(provider = dockerProvider)
        )
        image <- docker.RegistryImage(
          s"$appName-image",
          docker.RegistryImageArgs(name = tag.targetImage),
          opts = opts(provider = dockerProvider)
        )
        k8sRegistry <- k8s.core.v1.Secret(
          s"$appName-registry-secret",
          k8s.core.v1.SecretArgs(
            metadata = ObjectMetaArgs(
              name = s"$appName-registry-secret",
              namespace = appNamespace.metadata.name
            ),
            `type` = "kubernetes.io/dockerconfigjson",
            stringData = Map(
              ".dockerconfigjson" ->
                jsObjectOutput(
                  "auths" -> jsObjectOutput(
                    repositoryUrl -> jsObjectOutput(
                      "username" -> accessKeyIdJsValue,
                      "password" -> secretAccessKeyJsValue,
                      "auth" -> p"$accessKeyId:$secretAccessKey".map(base64).map(JsString(_))
                    )
                  )
                ).prettyPrint
            )
          ),
          opts = opts(provider = k8sProvider, dependsOn = image)
        )
      yield (Some(k8sRegistry), tag.targetImage)

  val k8sRegistrySecret = out.map(_._1)
  val image             = out.flatMap(_._2)

  // loki
  val lokiDeployment = Loki.deploy(appNamespace, k8sProvider)
  val lokiService    = Loki.deployService(appNamespace, lokiDeployment, k8sProvider)

  // promtail
  val promtailDaemonSet = Promtail.deploy(lokiService, appNamespace, k8sProvider)

  // grafana
  val grafanaDeployment = Grafana.deploy(appNamespace, k8sProvider)
  val grafanaService    = Grafana.deployService(serviceType, appNamespace, grafanaDeployment, k8sProvider)

  // zookeeper
  val zooDeployment = Zookeeper.deploy(appNamespace, k8sProvider)
  val zooService    = Zookeeper.deployService(appNamespace, zooDeployment, k8sProvider)

  // kafka
  val kafkaDeployment = Kafka.deploy(appNamespace, zooService, k8sProvider)
  val kafkaService    = Kafka.deployService(appNamespace, kafkaDeployment, k8sProvider)

  // postgres
  val postgresDeployment = Postgres.deploy(appNamespace, k8sProvider)
  val postgresService    = Postgres.deployService(appNamespace, postgresDeployment, k8sProvider)

  // jaeger
  val jaegerDeployment = Jaeger.deploy(appNamespace, k8sProvider)
  val jaegerService    = Jaeger.deployService(appNamespace, jaegerDeployment, k8sProvider)

  // vss
  val vssDeployment =
    VSS.deploy(k8sRegistrySecret, image, appNamespace, postgresService, kafkaService, jaegerService, k8sProvider)
  val vssService = VSS.deployService(serviceType, appNamespace, vssDeployment, k8sProvider)

  val grafanaServiceUrl =
    grafanaService.status.loadBalancer.ingress
      .map(
        _.flatMap(_.headOption.flatMap(_.hostname))
          .map(host => p"http://$host:${Grafana.port}")
          .getOrElse("Host not find. Probably vss:cluster is set to local")
      )

  val vssServiceUrl =
    vssService.status.loadBalancer.ingress
      .map(
        _.flatMap(_.headOption.flatMap(_.hostname))
          .map(host => p"http://$host:${VSS.ports("main-http")._2}/docs")
          .getOrElse("Host not find. Probably vss:cluster is set to local")
      )

  Stack.exports(
    grafanaServiceUrl = grafanaServiceUrl,
    vssServiceUrl = vssServiceUrl,
    namespaceName = appNamespace.metadata.name,
    lokiDeploymentName = lokiDeployment.metadata.name,
    lokiServiceName = lokiService.metadata.name,
    grafanaDeploymentName = grafanaDeployment.metadata.name,
    grafanaServiceName = grafanaService.metadata.name,
    promtailDaemonSetName = promtailDaemonSet.metadata.name,
    zookeeperDeploymentName = zooDeployment.metadata.name,
    zookeeperServiceName = zooService.metadata.name,
    kafkaDeploymentName = kafkaDeployment.metadata.name,
    kafkaServiceName = kafkaService.metadata.name,
    postgresDeploymentName = postgresDeployment.metadata.name,
    postgresServiceName = postgresService.metadata.name,
    jaegerDeploymentName = jaegerDeployment.metadata.name,
    jaegerServiceName = jaegerService.metadata.name,
    vssDeploymentName = vssDeployment.metadata.name,
    vssServiceName = vssService.metadata.name
  )
}

private def base64: String => String = v => java.util.Base64.getEncoder.encodeToString(v.getBytes)

private def jsObjectOutput(
  members: (String | Output[String], JsValue | Output[JsValue])*
)(using Context): Output[JsObject] =
  Output
    .sequence(
      members.toSeq.map {
        case (k: String, v: JsValue) =>
          Output(k, v)
        case (k: String, ov: Output[JsValue]) =>
          ov.map(v => (k, v))
        case (ok: Output[String], v: JsValue) =>
          ok.map(k => (k, v))
        case (ok: Output[String], ov: Output[JsValue]) =>
          ok.zip(ov)
      }
    )
    .map(o => JsObject.apply(o*))

extension (o: Output[StackReference])
  def requireOutput(name: NonEmptyString)(using Context): Output[JsValue] = o.flatMap(_.requireOutput(name))

extension (o: Output[JsValue])
  def convertTo[T : JsonReader]: Output[T] = o.map(_.convertTo[T])
  def prettyPrint: Output[String]          = o.map(_.prettyPrint)
