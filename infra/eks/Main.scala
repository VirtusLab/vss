import besom.*
import besom.api.{aws, awsx, eks}

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

  val repo = aws.ecr.Repository(
    s"$appName-repository",
    aws.ecr.RepositoryArgs(
      imageTagMutability = "MUTABLE"
    )
  )

  val authorizationToken =
    aws.ecr.getAuthorizationToken(aws.ecr.GetAuthorizationTokenArgs(registryId = repo.registryId))

  Stack(cluster).exports(
    registryEndpoint = authorizationToken.proxyEndpoint,
    repositoryUrl = repo.repositoryUrl,
    accessKeyId = authorizationToken.map(_.userName).asSecret,
    secretAccessKey = authorizationToken.map(_.password).asSecret,
    kubeconfig = cluster.kubeconfigJson,
    organization = pulumiOrganization
  )
}
