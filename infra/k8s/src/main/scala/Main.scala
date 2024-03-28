import besom.*
import besom.api.kubernetes.core.v1.enums.ServiceSpecType
import besom.api.kubernetes.core.v1.{Namespace, NamespaceArgs}
import besom.api.kubernetes.meta.v1.inputs.ObjectMetaArgs
import besom.api.{docker, kubernetes as k8s}
import besom.json.*
import besom.json.DefaultJsonProtocol.StringJsonFormat

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
      orgName   <- config.getString("clusterOrg").getOrElse("organization")
      projName  <- config.requireString("clusterProject")
      stackName <- config.requireString("clusterStack")
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

  val appImage = clusterConfig.flatMap:
    case Cluster.Local =>
      p"$localRepository:$imageTag"
    case Cluster.Remote =>
      val dockerProvider = docker.Provider(
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
      val tag = docker.Tag(
        name = s"$appName-tag",
        docker.TagArgs(
          sourceImage = p"$localRepository:$imageTag",
          targetImage = p"$repositoryUrl:$imageTag"
        ),
        opts = opts(provider = dockerProvider)
      )
      docker
        .RegistryImage(
          name = s"$appName-image",
          docker.RegistryImageArgs(name = tag.targetImage),
          opts = opts(provider = dockerProvider)
        )
        .name

  val appImagePullSecret = clusterConfig.flatMap:
    case Cluster.Local =>
      Output(None)
    case Cluster.Remote =>
      val secret = k8s.core.v1.Secret(
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
        opts = opts(provider = k8sProvider)
      )
      secret.map(Some(_))

  // loki
  val lokiDeployment = Loki.deploy(appNamespace, k8sProvider)
  val lokiService    = Loki.deployService(appNamespace, lokiDeployment, k8sProvider)
  val lokiUrl        = p"http://${lokiService.metadata.name.map(_.get)}:${Loki.port}"

  // promtail
  val promtailDaemonSet = Promtail.deploy(lokiUrl, appNamespace, k8sProvider)

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
  val jaegerUrl        = p"http://${jaegerService.metadata.name.map(_.get)}:${Jaeger.ports("frontend")._2}"

  // grafana
  val grafanaDeployment = Grafana.deploy(appNamespace, k8sProvider)
  val grafanaServiceUrl =
    Grafana.deployService(lokiUrl, jaegerUrl, serviceType, appNamespace, grafanaDeployment, k8sProvider)

  // vss
  val vssDeployment =
    VSS.deploy(appImagePullSecret, appImage, appNamespace, postgresService, kafkaService, jaegerService, k8sProvider)
  val vssServiceUrl = VSS.deployService(serviceType, appNamespace, vssDeployment, k8sProvider)

  Stack.exports(
    appImage = appImage,
    grafanaServiceUrl = grafanaServiceUrl,
    vssServiceUrl = vssServiceUrl,
    namespaceName = appNamespace.metadata.name,
    lokiDeploymentName = lokiDeployment.metadata.name,
    lokiServiceName = lokiService.metadata.name,
    grafanaDeploymentName = grafanaDeployment.metadata.name,
    promtailDaemonSetName = promtailDaemonSet.metadata.name,
    zookeeperDeploymentName = zooDeployment.metadata.name,
    zookeeperServiceName = zooService.metadata.name,
    kafkaDeploymentName = kafkaDeployment.metadata.name,
    kafkaServiceName = kafkaService.metadata.name,
    postgresDeploymentName = postgresDeployment.metadata.name,
    postgresServiceName = postgresService.metadata.name,
    jaegerDeploymentName = jaegerDeployment.metadata.name,
    jaegerServiceName = jaegerService.metadata.name,
    vssDeploymentName = vssDeployment.metadata.name
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
