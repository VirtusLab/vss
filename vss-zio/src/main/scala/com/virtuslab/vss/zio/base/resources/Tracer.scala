package com.virtuslab.vss.zio.base.resources

import io.jaegertracing.Configuration
import io.jaegertracing.internal.JaegerTracer
import io.jaegertracing.internal.samplers.ConstSampler
import io.jaegertracing.internal.reporters.RemoteReporter
import io.jaegertracing.thrift.internal.senders.UdpSender
import com.virtuslab.vss.zio.base.config.JaegerConfig
import zio.telemetry.opentracing.OpenTracing
import java.net.URI
import zio.*

object Tracer {

  def layer: TaskLayer[OpenTracing] = ZLayer.scoped {
    for
      config <- ZIO.config(JaegerConfig.config)
      tracer <- makeTracer(config.uri, "vss")
      openTracing <- OpenTracing.scoped(tracer, "ROOT")
    yield openTracing
  }

  private def makeTracer(uri: String, serviceName: String): Task[JaegerTracer] =
    for {
      parsedURI <- ZIO.attempt(new URI(uri))
      (host, port) = (parsedURI.getHost(), parsedURI.getPort())
      tracer <- ZIO.attempt(
        new Configuration(serviceName).getTracerBuilder
          .withSampler(new ConstSampler(true))
          .withReporter(
            new RemoteReporter.Builder().withSender(new UdpSender(host, port, 0)).build()
          )
          .build()
      )
    } yield tracer
}
