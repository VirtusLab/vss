import besom.*
import besom.aliases.NonEmptyString
import besom.api.kubernetes as k8s
import k8s.apps.v1.inputs.*
import k8s.apps.v1.{DaemonSet, DaemonSetArgs, Deployment, DeploymentArgs}
import k8s.core.v1.inputs.*
import k8s.core.v1.{ConfigMapArgs, ServiceAccountArgs, *}
import k8s.meta.v1.inputs.*
import k8s.rbac.v1.inputs.{PolicyRuleArgs, RoleRefArgs, SubjectArgs}
import k8s.rbac.v1.{ClusterRole, ClusterRoleArgs, ClusterRoleBinding, ClusterRoleBindingArgs}
import besom.internal.{Context, Output}
import besom.util.NonEmptyString
import besom.aliases.NonEmptyString

object Promtail:
  val appName: NonEmptyString = "promtail"
  val labels                  = Map("app" -> "promtail")
  val port                    = 9080
  private val configFileName  = "promtail.yaml"

  def deploy(using
    Context
  )(lokiService: Output[Service], namespace: Output[Namespace], k8sProvider: Output[k8s.Provider]) =
    val configMap = ConfigMap(
      s"$appName-config",
      ConfigMapArgs(
        metadata = ObjectMetaArgs(name = s"$appName-config", namespace = namespace.metadata.name),
        data = Map(
          configFileName ->
            p"""server:
               |  http_listen_port: $port
               |  grpc_listen_port: 0
               |clients:
               |- url: http://${lokiService.metadata.name.map(_.get)}:3100/loki/api/v1/push
               |positions:
               |  filename: /tmp/positions.yaml
               |target_config:
               |  sync_period: 10s
               |scrape_configs:
               |- job_name: pod-logs
               |  kubernetes_sd_configs:
               |    - role: pod
               |  pipeline_stages:
               |    - docker: {}
               |  relabel_configs:
               |    - source_labels:
               |        - __meta_kubernetes_pod_node_name
               |      target_label: __host__
               |    - action: labelmap
               |      regex: __meta_kubernetes_pod_label_(.+)
               |    - action: replace
               |      replacement: $$1
               |      separator: /
               |      source_labels:
               |        - __meta_kubernetes_namespace
               |        - __meta_kubernetes_pod_name
               |      target_label: job
               |    - action: replace
               |      source_labels:
               |        - __meta_kubernetes_namespace
               |      target_label: namespace
               |    - action: replace
               |      source_labels:
               |        - __meta_kubernetes_pod_name
               |      target_label: pod
               |    - action: replace
               |      source_labels:
               |        - __meta_kubernetes_pod_container_name
               |      target_label: container
               |    - replacement: /var/log/pods/*$$1/*.log
               |      separator: /
               |      source_labels:
               |        - __meta_kubernetes_pod_uid
               |        - __meta_kubernetes_pod_container_name
               |      target_label: __path__
               |""".stripMargin
        )
      ),
      opts(provider = k8sProvider)
    )

    val serviceAccount = ServiceAccount(
      s"$appName-service-account",
      ServiceAccountArgs(
        metadata = ObjectMetaArgs(name = s"$appName-service-account" /*, namespace = namespace.metadata.name*/ )
      ),
      opts(provider = k8sProvider)
    )

    val clusterRole = ClusterRole(
      s"$appName-cluster-role",
      ClusterRoleArgs(
        metadata = ObjectMetaArgs(name = s"$appName-cluster-role", namespace = namespace.metadata.name),
        rules = List(
          PolicyRuleArgs(
            apiGroups = List(""),
            resources = List("nodes", "services", "pods"),
            verbs = List("get", "watch", "list")
          )
        )
      ),
      opts(provider = k8sProvider)
    )

    val clusterRoleBinding = ClusterRoleBinding(
      s"$appName-cluster-role-binding",
      ClusterRoleBindingArgs(
        metadata = ObjectMetaArgs(name = s"$appName-cluster-role-binding", namespace = namespace.metadata.name),
        subjects = List(
          SubjectArgs(
            kind = "ServiceAccount",
            name = serviceAccount.metadata.name.map(_.get),
            namespace = namespace.metadata.name
          )
        ),
        roleRef = RoleRefArgs(
          kind = "ClusterRole",
          name = clusterRole.metadata.name.map(_.get),
          apiGroup = "rbac.authorization.k8s.io"
        )
      ),
      opts = opts(provider = k8sProvider, retainOnDelete = false, dependsOn = List(clusterRole, serviceAccount))
    )

    DaemonSet(
      s"$appName-daemon-set",
      DaemonSetArgs(
        metadata = ObjectMetaArgs(name = s"$appName-daemon-set", namespace = namespace.metadata.name),
        spec = DaemonSetSpecArgs(
          selector = LabelSelectorArgs(matchLabels = labels),
          template = PodTemplateSpecArgs(
            metadata = ObjectMetaArgs(
              name = s"$appName-deployment",
              labels = labels,
              namespace = namespace.metadata.name
            ),
            spec = PodSpecArgs(
              serviceAccount = serviceAccount.metadata.name,
              containers = List(
                ContainerArgs(
                  name = appName,
                  image = "grafana/promtail:latest",
                  ports = List(
                    ContainerPortArgs(containerPort = port)
                  ),
                  args = List(s"-config.file=/etc/promtail/$configFileName"),
                  env = List(
                    EnvVarArgs(
                      name = "HOSTNAME",
                      valueFrom = EnvVarSourceArgs(
                        fieldRef = ObjectFieldSelectorArgs(fieldPath = "spec.nodeName")
                      )
                    )
                  ),
                  volumeMounts = List(
                    VolumeMountArgs(
                      mountPath = "/var/log",
                      name = "logs"
                    ),
                    VolumeMountArgs(
                      mountPath = "/etc/promtail",
                      name = s"$appName-config"
                    ),
                    VolumeMountArgs(
                      mountPath = "/var/lib/docker/containers",
                      readOnly = true,
                      name = "varlibdockercontainers"
                    )
                  )
                )
              ),
              volumes = List(
                VolumeArgs(
                  name = "logs",
                  hostPath = HostPathVolumeSourceArgs(path = "/var/log")
                ),
                VolumeArgs(
                  name = "varlibdockercontainers",
                  hostPath = HostPathVolumeSourceArgs(path = "/var/lib/docker/containers")
                ),
                VolumeArgs(
                  name = s"$appName-config",
                  configMap = ConfigMapVolumeSourceArgs(
                    name = configMap.metadata.name
                  )
                )
              )
            )
          )
        )
      ),
      opts = opts(provider = k8sProvider, dependsOn = clusterRoleBinding)
    )
