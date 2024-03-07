import besom.*
import besom.util.*
import besom.api.kubernetes as k8s
import k8s.core.v1.inputs.*
import k8s.core.v1.{ConfigMap, ConfigMapArgs, Namespace, Service, ServiceArgs}
import k8s.apps.v1.inputs.*
import k8s.apps.v1.{Deployment, DeploymentArgs}
import k8s.meta.v1.inputs.*
import besom.internal.{Context, Output}
import besom.internal.Config

object VSS {
  val appName: NonEmptyString = "vss-app" // todo fix inference in NonEmptyString
  val labels                  = Map("app" -> "vss-app")
  val ports = Map(
    "main-http" -> (None, 8080),
    "main-grpc" -> (None, 8081),
    "stats-http" -> (None, 8180),
    "stats-grpc" -> (None, 8181)
  )

  def deploy(using Context)(
    config: Config,
    namespace: Output[Namespace],
    postgresService: Output[Service],
    kafkaService: Output[Service],
    jaegerService: Output[Service]
  ) = {
    val localRegistry = config.requireString("localRegistry")
    val imageName     = config.requireString("imageName")
    val imageTag      = config.requireString("imageTag")
    val image         = pulumi"$localRegistry/$imageName:$imageTag"

    Deployment(
      appName,
      DeploymentArgs(
        spec = DeploymentSpecArgs(
          selector = LabelSelectorArgs(matchLabels = labels),
          replicas = 1,
          template = PodTemplateSpecArgs(
            metadata = ObjectMetaArgs(
              name = s"$appName-deployment",
              labels = labels,
              namespace = namespace.metadata.name
            ),
            spec = PodSpecArgs(
              containers = List(
                ContainerArgs(
                  name = appName,
                  image = image,
                  imagePullPolicy = "IfNotPresent",
                  ports = ports.map { case (name, (protocol, port)) =>
                    ContainerPortArgs(containerPort = port, protocol)
                  }.toList,
                  env = List(
                    EnvVarArgs(name = "BASE_DB_HOST", value = postgresService.metadata.name),
                    EnvVarArgs(name = "BASE_DB_PORT", value = Postgres.port.toString),
                    EnvVarArgs(name = "BASE_DB_NAME", value = "vss"),
                    EnvVarArgs(name = "BASE_DB_USER", value = "postgres"),
                    EnvVarArgs(name = "BASE_DB_PASSWORD", value = "postgres"),
                    EnvVarArgs(name = "KAFKA_HOST", value = kafkaService.metadata.name),
                    EnvVarArgs(name = "KAFKA_PORT", value = Kafka.port.toString),
                    EnvVarArgs(
                      name = "JEAGER_URI",
                      value = pulumi"${jaegerService.metadata.name.map(_.get)}:${Jaeger.ports("frontend")._2}"
                    )
                  )
                )
              )
            )
          )
        ),
        metadata = ObjectMetaArgs(
          name = s"$appName-deployment",
          namespace = namespace.metadata.name
        )
      )
    )
  }

  def deployService(using Context)(namespace: Output[Namespace]) = Service(
    appName,
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        ports = ports.map { case (name, (protocol, port)) =>
          ServicePortArgs(name = name, port = port, targetPort = port, protocol = protocol)
        }.toList
      ),
      metadata = ObjectMetaArgs(
        name = s"$appName-service",
        namespace = namespace.metadata.name
      )
    )
  )

}
