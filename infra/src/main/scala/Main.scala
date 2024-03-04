import besom.*
import besom.api.kubernetes as k8s
import besom.internal.Output
import k8s.core.v1.{Service, namespace}
import besom.internal.Config

@main def main = Pulumi.run {
  val conf = config
  for
    appNamespace <- namespace(name = "vss")
    // zookeeper
    zooDeployment <- Zookeeper.deploy(appNamespace)
    zooService    <- Zookeeper.deployService(appNamespace)
    // kafka
    kafkaDeployment <- Kafka.deploy(appNamespace, zooService)
    kafkaService    <- Kafka.deployService(appNamespace)
    // postgres
    postgresDeployment <- Postgres.deploy(appNamespace)
    postgresService    <- Postgres.deployService(appNamespace)
    // jaeger
    jaegerDeployment <- Jaeger.deploy(appNamespace)
    jaegerService    <- Jaeger.deployService(appNamespace)
    // vss
    vssDeployment <- VSS.deploy(conf, appNamespace, postgresService, kafkaService, jaegerService)
    vssService    <- VSS.deployService(appNamespace)
  yield Pulumi.exports(
    namespaceName = appNamespace.metadata.name,
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
