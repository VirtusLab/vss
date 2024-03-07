import besom.*
import besom.api.kubernetes as k8s
import k8s.apps.v1.inputs.*
import k8s.apps.v1.{Deployment, DeploymentArgs}
import k8s.core.v1.inputs.*
import k8s.core.v1.{ConfigMapArgs, ServiceArgs, *}
import k8s.meta.v1.inputs.*
import besom.internal.{Context, Output}
import besom.util.NonEmptyString
import besom.aliases.NonEmptyString

object Postgres {
  val appName: NonEmptyString = "postgres" // todo fix inference in NonEmptyString
  val labels                  = Map("app" -> "postgres")
  val port                    = 5432

  def deploy(using Context)(namespace: Output[Namespace]) = {

    val postgresPV = PersistentVolume(
      appName,
      k8s.core.v1.PersistentVolumeArgs(
        metadata = ObjectMetaArgs(name = s"$appName-pv", namespace = namespace.metadata.name),
        spec = PersistentVolumeSpecArgs(
          capacity = Map("storage" -> "8Gi"),
          accessModes = List("ReadWriteMany"),
          hostPath = HostPathVolumeSourceArgs("/data/db")
        )
      )
    )

    val postgresPVC = PersistentVolumeClaim(
      appName,
      k8s.core.v1.PersistentVolumeClaimArgs(
        metadata = ObjectMetaArgs(name = s"$appName-pvc", namespace = namespace.metadata.name),
        spec = PersistentVolumeClaimSpecArgs(
          accessModes = List("ReadWriteMany"),
          resources = VolumeResourceRequirementsArgs(
            requests = Map("storage" -> "8Gi")
          )
        )
      )
    )

    val postgresConfigMap = ConfigMap(
      s"$appName-config",
      ConfigMapArgs(
        metadata = ObjectMetaArgs(name = s"$appName-config-map", namespace = namespace.metadata.name),
        data = Map(
          "DEBUG" -> "true",
          "POSTGRES_DB" -> "vss",
          "POSTGRES_USER" -> "postgres",
          "POSTGRES_PASSWORD" -> "postgres"
        )
      )
    )

    val initConfigMap = ConfigMap(
      s"$appName-init",
      ConfigMapArgs(
        metadata = ObjectMetaArgs(name = s"$appName-init-config-map", namespace = namespace.metadata.name),
        // path starts from  besom/.scala-build
        data = Ops.readFileIntoConfigMap("../commons/src/main/resources/tables.sql", Some("init.sql"))
      )
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
                  image = "postgres:14.1-alpine",
                  ports = List(
                    ContainerPortArgs(containerPort = port)
                  ),
                  envFrom = List(
                    EnvFromSourceArgs(configMapRef = ConfigMapEnvSourceArgs(postgresConfigMap.metadata.name))
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
                  persistentVolumeClaim = PersistentVolumeClaimVolumeSourceArgs(claimName =
                    postgresPVC.metadata.name.map(_.get) // TODO fix this
                  )
                ),
                VolumeArgs(
                  name = s"$appName-init",
                  configMap = ConfigMapVolumeSourceArgs(
                    name = initConfigMap.metadata.name,
                    defaultMode = 444
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
        ports = List(
          ServicePortArgs(port = port, targetPort = port)
        )
      ),
      metadata = ObjectMetaArgs(
        name = s"$appName-service",
        namespace = namespace.metadata.name
      )
    )
  )
}
