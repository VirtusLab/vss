enum Cluster(val name: String):
  case Local extends Cluster("local")
  case Remote extends Cluster("remote")

object Cluster:
  val parseName = Cluster.values
    .map(c => c.name -> c)
    .toMap
    .withDefault(str =>
      throw Exception(s"$str value not allowed. Available values are local or remote. Change vss:cluster configuration")
    )
