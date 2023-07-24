import besom.*
import besom.util.NonEmptyString
import besom.api.kubernetes as k8s
import k8s.core.v1.inputs.*
import k8s.apps.v1.inputs.*
import k8s.meta.v1.inputs.*
import k8s.apps.v1.{DeploymentArgs, deployment}
import k8s.core.v1.{
  ConfigMapArgs,
  Namespace,
  Service,
  ServiceArgs,
  configMap,
  namespace,
  persistentVolume,
  persistentVolumeClaim,
  service
}
import besom.internal.{Context, Output}
import besom.internal.{Context, Output}
import besom.util.NotProvided

object Jaeger {
  val appName = "jaeger"
  val labels  = Map("app" -> "jaeger")
  // https://www.jaegertracing.io/docs/1.6/getting-started/#all-in-one-docker-image - port descriptions
  val ports = Map(
    "zipkin-thrift-compact" -> (Some("UDP"), 5775),
    "frontend" -> (None, 16686),
    "jaeger-thrift-compact" -> (Some("UDP"), 6831),
    "jaeger-thrift-binary" -> (Some("UDP"), 6832),
    "configs" -> (None, 5778),
    "jaeger-thrift-client" -> (None, 14268),
    "zipkin-collector" -> (None, 9411)
  )
  def deploy(using context: Context)(namespace: Namespace) = deployment(
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
                image = "jaegertracing/all-in-one:latest",
                ports = ports.map { case (name, (protocol, port)) =>
                  ContainerPortArgs(containerPort = port, protocol.getOrElse(NotProvided))
                }.toList,
                env = List(
                  EnvVarArgs(name = "COLLECTOR_ZIPKIN_HTTP_PORT", value = ports("zipkin-collector")._2.toString())
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
