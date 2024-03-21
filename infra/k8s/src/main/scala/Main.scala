import VSS.appName
import besom.*
import besom.api.{docker, kubernetes as k8s}
import besom.api.kubernetes.meta.v1.inputs.ObjectMetaArgs
import besom.api.kubernetes.rbac.v1.inputs.PolicyRuleArgs
import besom.api.kubernetes.rbac.v1.{ClusterRole, ClusterRoleArgs}
import k8s.core.v1.{Namespace, NamespaceArgs, Service, ServiceAccount, ServiceAccountArgs}
import k8s.core.v1.enums.ServiceSpecType
import besom.internal.{Config, Output}
import besom.json.DefaultJsonProtocol.StringJsonFormat

@main def main = Pulumi.run {

  val serviceType = config
    .requireString("cluster")
    .map:
      case "remote" =>
        ServiceSpecType.LoadBalancer
      case _ =>
        ServiceSpecType.ClusterIP

  val k8sProvider = config
    .requireString("cluster")
    .flatMap:
      case "local" =>
        k8s.Provider(name = "vss-local-provider")
      case "remote" =>
        for
          k8sOrgName     <- config.getString("cluster-org").getOrElse("organization")
          k8sProjName    <- config.requireString("cluster-project")
          k8sStackName   <- config.requireString("cluster-stack")
          stack          <- StackReference(name = s"$k8sOrgName/$k8sProjName/$k8sStackName")
          kubeconfigJson <- stack.requireOutput("kubeconfig")
          provider <- k8s.Provider(
            name = "vss-remote-provider",
            k8s.ProviderArgs(kubeconfig = kubeconfigJson.convertTo[String])
          )
        yield provider
      case str =>
        throw Exception(
          s"$str value not allowed. Available values are local or remote. Change vss:cluster configuration"
        )

  val appNamespace = Namespace(
    name = "vss",
    NamespaceArgs(metadata = ObjectMetaArgs(name = "vss")),
    opts = opts(provider = k8sProvider)
  )

  val imageTag        = config.requireString("imageTag")
  val localRepository = config.requireString("localRepository")

  val out = config
    .requireString("cluster")
    .flatMap:
      case "local" =>
        Output(None, p"$localRepository:$imageTag")
      case "remote" =>
        for
          k8sOrgName       <- config.getString("cluster-org").getOrElse("organization")
          k8sProjName      <- config.requireString("cluster-project")
          k8sStackName     <- config.requireString("cluster-stack")
          stack            <- StackReference(name = s"$k8sOrgName/$k8sProjName/$k8sStackName")
          registryEndpoint <- stack.requireOutput("registryEndpoint").map(_.convertTo[String])
          repositoryUrl    <- stack.requireOutput("repositoryUrl").map(_.convertTo[String])
          secretAccessKey  <- stack.requireOutput("secretAccessKey").map(_.convertTo[String])
          accessKeyId      <- stack.requireOutput("accessKeyId").map(_.convertTo[String])
          dockerProvider <- docker.Provider(
            s"$appName-docker-provider",
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
              data = Map(
                ".dockerconfigjson" ->
                  p"""{
                     |            "auths": {
                     |                "$repositoryUrl": {
                     |                    "username": "$accessKeyId",
                     |                    "password": "$secretAccessKey",
                     |                    "auth": "${p"$accessKeyId:$secretAccessKey".map(base64)}"
                     |                }
                     |            }
                     |}""".stripMargin.map(base64)
              )
            ),
            opts = opts(provider = k8sProvider, dependsOn = image)
          )
        yield (Some(k8sRegistry), tag.targetImage)
      case str =>
        throw Exception(
          s"$str value not allowed. Available values are local or remote. Change vss:cluster configuration"
        )

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
