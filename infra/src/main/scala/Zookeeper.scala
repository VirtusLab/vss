import besom.*
import besom.util.NonEmptyString
import besom.api.kubernetes as k8s
import k8s.core.v1.inputs.*
import k8s.core.v1.{ConfigMap, Namespace, Service, ConfigMapArgs, ServiceArgs}
import k8s.apps.v1.inputs.*
import k8s.apps.v1.{Deployment, DeploymentArgs}
import k8s.meta.v1.inputs.*
import besom.internal.{Context, Output}
import besom.aliases.NonEmptyString

object Zookeeper {
  val appName: NonEmptyString = "zookeeper" // todo fix inference in NonEmptyString
  val labels                  = Map("app" -> "zookeeper")

  def deploy(using Context)(namespace: Output[Namespace]) = Deployment(
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
        namespace = namespace.metadata.name
      )
    )
  )

  def deployService(using Context)(namespace: Output[Namespace]) = Service(
    appName,
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        ports = List(
          ServicePortArgs(protocol = "TCP", port = 2181, targetPort = 2181)
        )
      ),
      metadata = ObjectMetaArgs(
        name = s"$appName-service",
        namespace = namespace.metadata.name
      )
    )
  )

}
