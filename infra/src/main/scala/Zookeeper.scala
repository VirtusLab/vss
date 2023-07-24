import besom.*
import besom.util.NonEmptyString
import besom.api.kubernetes as k8s
import k8s.core.v1.*
import k8s.core.v1.inputs.*
import k8s.apps.v1.inputs.*
import k8s.meta.v1.inputs.*
import k8s.apps.v1.{DeploymentArgs, deployment}
import k8s.core.v1.{ConfigMapArgs, Service, ServiceArgs, configMap, namespace, service}
import besom.internal.{Context, Output}

object Zookeeper {
  val appName = "zookeeper"
  val labels  = Map("app" -> "zookeeper")

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
                image = "confluentinc/cp-zookeeper:7.0.1",
                ports = List(
                  ContainerPortArgs(containerPort = 2181)
                ),
                env = List(
                  EnvVarArgs(name = "ZOOKEEPER_CLIENT_PORT", value = "2181"),
                  EnvVarArgs(name = "ZOOKEEPER_TICK_TIME", value = "2000")
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
        ports = List(
          ServicePortArgs(protocol = "TCP", port = 2181, targetPort = 2181)
        )
      ),
      metadata = ObjectMetaArgs(
        name = s"$appName-service",
        namespace = namespace.metadata.name.orEmpty
      )
    )
  )

}
