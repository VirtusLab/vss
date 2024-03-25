import besom.*
import besom.api.kubernetes as k8s
import k8s.apps.v1.inputs.*
import k8s.apps.v1.{Deployment, DeploymentArgs}
import k8s.core.v1.inputs.*
import k8s.core.v1.{ConfigMapArgs, ServiceArgs, *}
import k8s.core.v1.enums.ServiceSpecType
import k8s.meta.v1.inputs.*

object Grafana:
  val appName: NonEmptyString = "grafana"
  val labels                  = Map("app" -> "grafana")
  val port                    = 3000

  def deploy(using Context)(namespace: Output[Namespace], k8sProvider: Output[k8s.Provider]) = {
    val grafanaPV = PersistentVolume(
      appName,
      k8s.core.v1.PersistentVolumeArgs(
        metadata = ObjectMetaArgs(name = s"$appName-pv", namespace = namespace.metadata.name),
        spec = PersistentVolumeSpecArgs(
          storageClassName = "standard",
          capacity = Map("storage" -> "1Gi"),
          accessModes = List("ReadWriteOnce"),
          hostPath = HostPathVolumeSourceArgs("/data/grafana")
        )
      ),
      opts(provider = k8sProvider)
    )
    val grafanaPVC = PersistentVolumeClaim(
      appName,
      k8s.core.v1.PersistentVolumeClaimArgs(
        metadata = ObjectMetaArgs(name = s"$appName-pvc", namespace = namespace.metadata.name),
        spec = PersistentVolumeClaimSpecArgs(
          storageClassName = "standard",
          volumeName = grafanaPV.metadata.name,
          accessModes = List("ReadWriteOnce"),
          resources = VolumeResourceRequirementsArgs(
            requests = Map("storage" -> "1Gi")
          )
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
              securityContext = PodSecurityContextArgs(runAsUser = 0, fsGroup = 0),
              containers = List(
                ContainerArgs(
                  name = appName,
                  image = "grafana/grafana:latest",
                  ports = List(
                    ContainerPortArgs(containerPort = port)
                  ),
                  readinessProbe = ProbeArgs(
                    failureThreshold = 3,
                    httpGet = HttpGetActionArgs(
                      path = "/robots.txt",
                      port = port,
                      scheme = "HTTP"
                    ),
                    initialDelaySeconds = 10,
                    periodSeconds = 30,
                    successThreshold = 1,
                    timeoutSeconds = 2
                  ),
                  livenessProbe = ProbeArgs(
                    failureThreshold = 3,
                    tcpSocket = TcpSocketActionArgs(port = port),
                    initialDelaySeconds = 30,
                    periodSeconds = 10,
                    successThreshold = 1,
                    timeoutSeconds = 1
                  ),
                  resources = ResourceRequirementsArgs(
                    requests = Map("cpu" -> "250m", "memory" -> "750Mi")
                  ),
                  volumeMounts = List(
                    VolumeMountArgs(mountPath = "/var/lib/grafana", name = s"$appName-pv")
                  )
                )
              ),
              volumes = List(
                VolumeArgs(
                  name = s"$appName-pv",
                  persistentVolumeClaim = PersistentVolumeClaimVolumeSourceArgs(
                    claimName = grafanaPVC.metadata.name.map(_.get)
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

  def deployService(using
    Context
  )(
    serviceType: Output[ServiceSpecType],
    namespace: Output[Namespace],
    grafanaDeployment: Output[Deployment],
    k8sProvider: Output[k8s.Provider]
  ) = Service(
    appName,
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        sessionAffinity = "None",
        `type` = serviceType,
        ports = List(
          ServicePortArgs(port = port, targetPort = port)
        )
      ),
      metadata = ObjectMetaArgs(
        name = s"$appName-service",
        namespace = namespace.metadata.name
      )
    ),
    opts(dependsOn = grafanaDeployment, provider = k8sProvider)
  )
