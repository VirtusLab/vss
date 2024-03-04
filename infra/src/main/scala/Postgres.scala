import besom.*
import besom.api.kubernetes as k8s
import besom.api.kubernetes.apps.v1.inputs.*
import besom.api.kubernetes.apps.v1.{DeploymentArgs, deployment}
import besom.api.kubernetes.core.v1.inputs.*
import besom.api.kubernetes.core.v1.{ConfigMapArgs, ServiceArgs, *}
import besom.api.kubernetes.meta.v1.inputs.*
import besom.internal.{Context, Output}
import besom.util.NonEmptyString
import besom.api.kubernetes.apps.v1.outputs.Deployment

object Postgres {
  val appName = "postgres"
  val labels  = Map("app" -> "postgres")
  val port = 5432

  def deploy(using Context)(namespace: Namespace) = {

    val postgresPV = persistentVolume(
      NonEmptyString(appName).get,
      k8s.core.v1.PersistentVolumeArgs(
        metadata = ObjectMetaArgs(name = s"$appName-pv", namespace = namespace.metadata.name.orEmpty),
        spec = PersistentVolumeSpecArgs(
          capacity = Map("storage" -> "8Gi"),
          accessModes = List("ReadWriteMany"),
          hostPath = HostPathVolumeSourceArgs("/data/db")
        )
      )
    )

    val postgresPVC = persistentVolumeClaim(
      NonEmptyString(appName).get,
      k8s.core.v1.PersistentVolumeClaimArgs(
        metadata = ObjectMetaArgs(name = s"$appName-pvc", namespace = namespace.metadata.name.orEmpty),
        spec = PersistentVolumeClaimSpecArgs(
          accessModes = List("ReadWriteMany"),
          resources = ResourceRequirementsArgs(
            requests = Map("storage" -> "8Gi")
          )
        )
      )
    )

    val postgresConfigMap = configMap(
      NonEmptyString(s"$appName-config").get,
      ConfigMapArgs(
        metadata = ObjectMetaArgs(name = s"$appName-config-map", namespace = namespace.metadata.name.orEmpty),
        data = Map(
          "DEBUG" -> "true",
          "POSTGRES_DB" -> "vss",
          "POSTGRES_USER" -> "postgres",
          "POSTGRES_PASSWORD" -> "postgres"
        )
      )
    )

    val initConfigMap = configMap(
      NonEmptyString(s"$appName-init").get,
      ConfigMapArgs(
        metadata = ObjectMetaArgs(name = s"$appName-init-config-map", namespace = namespace.metadata.name.orEmpty),
        // path starts from  besom/.scala-build
        data = Ops.readFileIntoConfigMap("../commons/src/main/resources/tables.sql", Some("init.sql"))
      )
    )

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
                  image = "postgres:14.1-alpine",
                  ports = List(
                    ContainerPortArgs(containerPort = port)
                  ),
                  envFrom = List(
                    EnvFromSourceArgs(configMapRef = ConfigMapEnvSourceArgs(postgresConfigMap.metadata.name.orEmpty))
                  ),
                  volumeMounts = List(
                    VolumeMountArgs(mountPath = "/var/lib/postgres/data", name = "postgres-data"),
                    VolumeMountArgs(mountPath = "/docker-entrypoint-initdb.d", name = "postgres-init")
                  ),
                  livenessProbe = ProbeArgs(
                    exec = ExecActionArgs(List("pg_isready", "-U", "postgres")),
                    periodSeconds = 5,
                    failureThreshold = 5,
                    initialDelaySeconds = 30
                  )
                )
              ),
              volumes = List(
                VolumeArgs(
                  name = s"$appName-data",
                  persistentVolumeClaim =
                    PersistentVolumeClaimVolumeSourceArgs(claimName = postgresPVC.metadata.name.orEmpty)
                ),
                VolumeArgs(
                  name = s"$appName-init",
                  configMap = ConfigMapVolumeSourceArgs(
                    name = initConfigMap.metadata.name.orEmpty,
                    defaultMode = 444
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

  def deployService(using Context)(namespace: Namespace) = service(
    NonEmptyString(appName).get,
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        ports = List(
          ServicePortArgs(port = port, targetPort = port)
        )
      ),
      metadata = ObjectMetaArgs(
        name = s"$appName-service",
        namespace = namespace.metadata.name.orEmpty
      )
    )
  )
}
