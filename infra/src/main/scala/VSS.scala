import besom.*
import besom.util.*
import besom.api.kubernetes as k8s
import k8s.core.v1.*
import k8s.core.v1.inputs.*
import k8s.apps.v1.inputs.*
import k8s.meta.v1.inputs.*
import k8s.apps.v1.{DeploymentArgs, deployment}
import k8s.core.v1.{ConfigMapArgs, Service, ServiceArgs, configMap, namespace, service}
import besom.internal.{Context, Output}
import besom.api.kubernetes.apps.v1.Deployment
import besom.internal.Config

object VSS {
  val appName = "vss-app"
  val labels  = Map("app" -> "vss-app")
  val ports = Map(
    "main-http" -> (None, 8080),
    "main-grpc" -> (None, 8081),
    "stats-http" -> (None, 8180),
    "stats-grpc" -> (None, 8181)
  )

  val localRegistry = "localhost:5001"
  val imageName = "vss-cats"
  val imageTag = "0.1.0-SNAPSHOT"

  def deploy(using context: Context)(
    config: Config,
    namespace: Namespace,
    postgresService: Service,
    kafkaService: Service,
    jaegerService: Service
  ) = {
    val localRegistry = config.get("localRegistry")
    val imageName =config.get("imageName")
    val imageTag = config.get("imageTag")
    val image = pulumi"$localRegistry/$imageName:$imageTag" 

    deployment(
      NonEmptyString(appName).get,
      DeploymentArgs(
        spec = DeploymentSpecArgs(
          selector = LabelSelectorArgs(matchLabels = labels),
          replicas = 1,
          template = PodTemplateSpecArgs(
            metadata = ObjectMetaArgs(
              name = s"$appName-deployment",
              labels = labels,
              namespace = namespace.metadata.name.orEmpty
            ),
            spec = PodSpecArgs(
              containers = List(
                ContainerArgs(
                  name = appName,
                  image = image,
                  ports = ports.map { case (name, (protocol, port)) =>
                    ContainerPortArgs(containerPort = port, protocol.getOrElse(NotProvided))
                  }.toList,
                  env = List(
                    EnvVarArgs(name = "BASE_DB_HOST", value = postgresService.metadata.name.orEmpty),
                    EnvVarArgs(name = "BASE_DB_PORT", value = Postgres.port.toString),
                    EnvVarArgs(name = "BASE_DB_NAME", value = "vss"),
                    EnvVarArgs(name = "BASE_DB_USER", value = "postgres"),
                    EnvVarArgs(name = "BASE_DB_PASSWORD", value = "postgres"),
                    EnvVarArgs(name = "KAFKA_HOST", value = kafkaService.metadata.name.orEmpty),
                    EnvVarArgs(name = "KAFKA_PORT", value = Kafka.port.toString),
                    EnvVarArgs(
                      name = "JEAGER_URI",
                      value = pulumi"${jaegerService.metadata.name.orEmpty}:${Jaeger.ports("frontend")._2}"
                    )
                  )
                )
              )
            )
          )
        ),
        metadata = ObjectMetaArgs(
          name = s"$appName-deployment",
          namespace = namespace.metadata.name.orEmpty
        )
      )
    )
  }

  def deployService(using context: Context)(namespace: Namespace) = service(
    NonEmptyString(appName).get,
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        ports = ports.map { case (name, (protocol, port)) =>
          ServicePortArgs(name = name, port = port, targetPort = port, protocol = protocol.getOrElse(NotProvided))
        }.toList
      ),
      metadata = ObjectMetaArgs(
        name = s"$appName-service",
        namespace = namespace.metadata.name.orEmpty
      )
    )
  )

}
