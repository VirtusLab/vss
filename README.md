# VSS bootstrap

The goal of this project is to enable jumpstarting a new typical Scala project with the choice of different flavors:
* Scala Vanilla - as simple as possible, pure scala setup,
* ZIO - native stack for ZIO framework,
* cats - native stack for cats-effect toolkit. 

VSS bootstrap enables generating full non-trivial scala backend app together with the infrastructure layer. Everything is ready to be deployed with one command.

## Why did we create VSS bootstrap?

Scala has many great libraries and it is not a trivial task to find the best subset for your project. The same problem is also visible with many ways of providing an infrastructure layer. 
These two things together generate unnecessary traction, harm time to market, and cause problems with maintaining the solution (wrong set of libraries, multiple ways to do the same thing).
VSS bootstrap enables jump-start the new project and building quickly valuable complete solution, solution easy to maintain in the long run.

## VSS assumptions

Assumptions:
* bootstrap should only focus on scala-based backend part,
* architecture should be simple but not oversimplified, with a typical use case for Scala language,
* the source code should be easily understandable,
* bootstrap should enforce an opinionated library set, optimal for each flavor,
* the architecture layer should be set up out of the box,
* we will use only prod-ready solutions (or in the worst-case scenario - solutions that are the closest to be prod ready in a given ecosystem).

## VSS bootstrap architecture example

What exemplary problem VSS bootstrap solves?
The goal of the system is to get password hashed or check whether a given password hash has been queried before.

![VSS bootstrap architecture](docs/architecture.drawio.svg)


## Technology and functionalities

Base Service:
* HTTP/gRPC endpoints
* Providing password hashes for the given password hash type pair
* Checking if a given password hash has been queried in the hash service before
* Pub/Sub solution for sending events about checked passwords
* Tracing information about performed operations

Stats Service:
* Read check results events from Pub/Sub
* Displaying all the accumulated events by HTTP/gRPC

## Solution

### Setting up the local environment

Docker compose
```
docker-compose up
```

Pulumi
```
TODO
```

### Run VSS demo app:

## VSS Vanilla

```sh
sbt "vss_vanilla/runMain com.virtuslab.vss.vanilla.mainVanilla"
```

## VSS ZIO

```sh
sbt "vss_zio/runMain com.virtuslab.vss.zio.MainZIO"
```

## VSS Cats

VSS Cats
```sh
sbt "vss_cats/run"
```

# Use it:

HTTP docs:
```
http://localhost:8080/docs/
```

HTTP request example:
```bash
curl -X 'POST' \
  'http://localhost:8080/hash' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "hashType": "SHA256",
  "password": "some_password"
}'
```

gRPC request example (for vss cats):

```bash
grpcurl -d '{"hashType": "SHA256", "password": "somepassword"}' --import-path vss-cats/src/main/protobuf --proto password.proto --plaintext localhost:8081 com.virtuslab.vss.proto.cats.HashPasswordService/HashPassword
```
