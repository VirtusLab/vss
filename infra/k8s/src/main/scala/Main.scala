import besom.*
import besom.api.kubernetes as k8s
import k8s.core.v1.{Namespace, Service}
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

  val appNamespace = Namespace(name = "vss", opts = opts(provider = k8sProvider))

  // loki
  val lokiDeployment = Loki.deploy(appNamespace, k8sProvider)
  val lokiService    = Loki.deployService(appNamespace, lokiDeployment, k8sProvider)

  // promtail
  val promtailDaemonSet = Promtail.deploy(lokiService, appNamespace, k8sProvider)

  // grafana
  val grafanaDeployment = Grafana.deploy(appNamespace, k8sProvider)
  val grafanaService    = Grafana.deployService(appNamespace, grafanaDeployment, k8sProvider)

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
  val vssDeployment = VSS.deploy(config, appNamespace, postgresService, kafkaService, jaegerService, k8sProvider)
  val vssService    = VSS.deployService(serviceType, appNamespace, vssDeployment, k8sProvider)

  val vssServiceUrl =
    vssService.status.loadBalancer.ingress
      .map(
        _.flatMap(_.headOption.flatMap(_.hostname))
          .map(host => p"http://$host:${VSS.ports("main-http")._2}/docs")
          .getOrElse("Host not find. Probably vss:cluster is set to local")
      )

  Stack.exports(
    serviceUrl = vssServiceUrl,
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
