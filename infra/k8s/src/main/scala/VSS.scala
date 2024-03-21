import besom.*
import besom.util.*
import besom.api.kubernetes as k8s
import k8s.core.v1.inputs.*
import k8s.core.v1.{ConfigMap, ConfigMapArgs, Namespace, Service, ServiceArgs}
import k8s.core.v1.enums.ServiceSpecType
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
    k8sRegistrySecret: Output[Option[k8s.core.v1.Secret]],
    image: Output[String],
    namespace: Output[Namespace],
    postgresService: Output[Service],
    kafkaService: Output[Service],
    jaegerService: Output[Service],
    k8sProvider: Output[k8s.Provider]
  ) = {
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
              imagePullSecrets =
                k8sRegistrySecret.map(_.map(secret => LocalObjectReferenceArgs(name = secret.metadata.name)).toList),
              containers = List(
                ContainerArgs(
                  name = appName,
                  image = image,
                  imagePullPolicy = "Always",
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
      ),
      opts(provider = k8sProvider)
    )
  }

  def deployService(using Context)(
    serviceType: Output[ServiceSpecType],
    namespace: Output[Namespace],
    vssDeployment: Output[Deployment],
    k8sProvider: Output[k8s.Provider]
  ) = Service(
    appName,
    ServiceArgs(
      spec = ServiceSpecArgs(
        `type` = serviceType,
        selector = labels,
        ports = ports.map { case (name, (protocol, port)) =>
          ServicePortArgs(name = name, port = port, targetPort = port, protocol = protocol)
        }.toList
      ),
      metadata = ObjectMetaArgs(
        name = s"$appName-service",
        namespace = namespace.metadata.name
      )
    ),
    opts(dependsOn = vssDeployment, provider = k8sProvider)
  )

}
