import besom.*
import besom.aliases.NonEmptyString
import besom.api.kubernetes as k8s
import k8s.apps.v1.inputs.*
import k8s.apps.v1.{Deployment, DeploymentArgs}
import k8s.core.v1.inputs.*
import k8s.core.v1.{ConfigMapArgs, ServiceArgs, *}
import k8s.meta.v1.inputs.*
import besom.internal.{Context, Output}
import besom.util.NonEmptyString
import besom.aliases.NonEmptyString

object Loki:
  val appName: NonEmptyString = "loki"
  val labels                  = Map("app" -> "loki")
  val port                    = 3100
  private val configFileName  = "loki.yaml"

  def deploy(using Context)(namespace: Output[Namespace], k8sProvider: Output[k8s.Provider]) =
    val configMap = ConfigMap(
      s"$appName-config",
      ConfigMapArgs(
        metadata = ObjectMetaArgs(name = s"$appName-config", namespace = namespace.metadata.name),
        data = Map(
          configFileName ->
            s"""auth_enabled: false
            |
            |server:
            |  http_listen_port: $port
            |
            |ingester:
            |  wal:
            |    dir: /tmp/wal
            |  lifecycler:
            |    address: 127.0.0.1
            |    ring:
            |      kvstore:
            |        store: inmemory
            |      replication_factor: 1
            |  chunk_idle_period: 15m
            |  chunk_retain_period: 30s
            |
            |schema_config:
            |  configs:
            |  - from: 2020-10-24
            |    store: boltdb-shipper
            |    object_store: filesystem
            |    schema: v11
            |    index:
            |      prefix: index_
            |      period: 24h
            |
            |storage_config:
            |  boltdb_shipper:
            |    active_index_directory: /tmp/loki/index
            |    cache_location: /tmp/loki/cache
            |    cache_ttl: 24h
            |    shared_store: filesystem
            |  filesystem:
            |    directory: /tmp/loki/chunks
            |
            |compactor:
            |  working_directory: /tmp/loki/compactor
            |  shared_store: filesystem
            |
            |limits_config:
            |  reject_old_samples: true
            |  reject_old_samples_max_age: 168h
            |
            |chunk_store_config:
            |  max_look_back_period: 0s
            |
            |table_manager:
            |  retention_deletes_enabled: false
            |  retention_period: 0s
            """.stripMargin
        )
      ),
      opts(provider = k8sProvider)
    )

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
                  image = "grafana/loki:latest",
                  ports = List(
                    ContainerPortArgs(containerPort = port)
                  ),
                  args = List(s"-config.file=/etc/loki/$configFileName"),
                  volumeMounts = List(
                    VolumeMountArgs(
                      mountPath = "/etc/loki",
                      readOnly = true,
                      name = s"$appName-config-volume"
                    )
                  )
                )
              ),
              volumes = List(
                VolumeArgs(
                  name = s"$appName-config-volume",
                  configMap = ConfigMapVolumeSourceArgs(
                    name = configMap.metadata.name
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

  def deployService(using
    Context
  )(namespace: Output[Namespace], lokiDeployment: Output[Deployment], k8sProvider: Output[k8s.Provider]) = Service(
    appName,
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        `type` = k8s.core.v1.enums.ServiceSpecType.ClusterIP,
        ports = List(
          ServicePortArgs(port = port, targetPort = port)
        )
      ),
      metadata = ObjectMetaArgs(
        name = s"$appName-service",
        namespace = namespace.metadata.name
      )
    ),
    opts(dependsOn = lokiDeployment, provider = k8sProvider)
  )
