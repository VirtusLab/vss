import besom.*
import besom.api.{awsx, eks}

@main def main = Pulumi.run {
  val appName = "vss"
  val vpc = awsx.ec2.Vpc(
    name = s"$appName-vpc",
    awsx.ec2.VpcArgs(
      cidrBlock = "10.1.0.0/16",
      enableDnsHostnames = true,
      enableDnsSupport = true
    )
  )

  val cluster = eks.Cluster(
    name = s"$appName-cluster",
    eks.ClusterArgs(
      vpcId = vpc.vpcId,
      subnetIds = vpc.publicSubnetIds,
      desiredCapacity = 2,
      minSize = 1,
      maxSize = 2,
      storageClasses = "gp2"
    )
  )

  Stack(cluster).exports(kubeconfig = cluster.kubeconfigJson)
}
