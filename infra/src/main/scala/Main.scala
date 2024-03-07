import besom.*
import besom.api.kubernetes.core.v1.{Namespace, Service}
import besom.internal.{Config, Output}

@main def main = Pulumi.run {

  val appNamespace = Namespace(name = "vss")

  // zookeeper
  val zooDeployment = Zookeeper.deploy(appNamespace)
  val zooService    = Zookeeper.deployService(appNamespace)

  // kafka
  val kafkaDeployment = Kafka.deploy(appNamespace, zooService)
  val kafkaService    = Kafka.deployService(appNamespace)

  // postgres
  val postgresDeployment = Postgres.deploy(appNamespace)
  val postgresService    = Postgres.deployService(appNamespace)

  // jaeger
  val jaegerDeployment = Jaeger.deploy(appNamespace)
  val jaegerService    = Jaeger.deployService(appNamespace)

  // vss
  val vssDeployment = VSS.deploy(config, appNamespace, postgresService, kafkaService, jaegerService)
  val vssService    = VSS.deployService(appNamespace)

  Stack.exports(
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
