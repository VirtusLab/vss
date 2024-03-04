import besom.*
import besom.util.*
import besom.api.kubernetes as k8s
import k8s.core.v1.inputs.*
import k8s.core.v1.{Namespace, Service, ConfigMapArgs, ServiceArgs, configMap, namespace, service}
import k8s.apps.v1.inputs.*
import k8s.apps.v1.{DeploymentArgs, deployment}
import k8s.meta.v1.inputs.*
import besom.internal.{Context, Output}

object Kafka {
  val appName          = "kafka"
  val labels           = Map("app" -> "kafka")
  val kafkaServiceName = s"$appName-service"
  val port = 9092

  def deploy(using Context)(namespace: Namespace, zookeeperService: Service) = deployment(
    NonEmptyString(appName).get,
    DeploymentArgs(
      spec = DeploymentSpecArgs(
        selector = LabelSelectorArgs(matchLabels = labels),
        replicas = 1,
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(
            name = "kafka-deployment",
            labels = labels,
            namespace = namespace.metadata.name.orEmpty
          ),
          spec = PodSpecArgs(
            containers = List(
              ContainerArgs(
                name = "kafka-broker",
                image = "confluentinc/cp-kafka:7.0.1",
                ports = List(
                  ContainerPortArgs(containerPort = port)
                ),
                env = List(
                  EnvVarArgs(name = "KAFKA_BROKER_ID", value = "1"),
                  EnvVarArgs(name = "KAFKA_ZOOKEEPER_CONNECT", value = pulumi"${zookeeperService.metadata.name.orEmpty}:2181"),
                  EnvVarArgs(
                    name = "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
                    value = "PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT"
                  ),
                  EnvVarArgs(
                    name = "KAFKA_ADVERTISED_LISTENERS",
                    value = s"PLAINTEXT://$kafkaServiceName:${port},PLAINTEXT_INTERNAL://localhost:29092"
                    ),
                  EnvVarArgs(name = "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", value = "1"),
                  EnvVarArgs(name = "KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", value = "1"),
                  EnvVarArgs(name = "KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", value = "1")
                )
              )
            )
          )
        )
      ),
      metadata = ObjectMetaArgs(
        name = "kafka-deployment",
        namespace = namespace.metadata.name.orEmpty
      )
    )
  )

  def deployService(using Context)(namespace: Namespace) = service(
    NonEmptyString(appName).get,
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        ports = List(
          ServicePortArgs(protocol = "TCP", port = port, targetPort = port)
        )
      ),
      metadata = ObjectMetaArgs(
        name = kafkaServiceName,
        namespace = namespace.metadata.name.orEmpty
      )
    )
  )

}
