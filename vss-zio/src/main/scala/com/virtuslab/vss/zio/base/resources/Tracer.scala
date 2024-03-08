package com.virtuslab.vss.zio.base.resources

import io.jaegertracing.Configuration
import io.jaegertracing.internal.JaegerTracer
import io.jaegertracing.internal.samplers.ConstSampler
import io.jaegertracing.internal.reporters.RemoteReporter
import io.jaegertracing.zipkin.ZipkinV2Reporter
import org.apache.http.client.utils.URIBuilder
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender
import com.virtuslab.vss.zio.base.config.JaegerConfig
import zio.telemetry.opentracing.OpenTracing
import java.net.URI
import zio.*

object Tracer:

  def layer: TaskLayer[OpenTracing] = ZLayer.scoped {
    for
      config      <- ZIO.config(JaegerConfig.config)
      _           <- ZIO.logInfo(s"Creating Jaeger tracer for ${config.uri}")
      tracer      <- makeTracer(config.uri, "vss")
      openTracing <- OpenTracing.scoped(tracer, "ROOT")
    yield openTracing
  }

  private def makeTracer(uri: String, serviceName: String): Task[JaegerTracer] =
    for
      host <- ZIO.attempt(uri.split(':').head)
      _    <- ZIO.logInfo(s"Parsed host for Jaeger tracer: ${host}")
      url <- ZIO.attempt(
        new URIBuilder().setScheme("http").setHost(host).setPort(9411).setPath("/api/v2/spans").build.toString
      )
      _             <- ZIO.logInfo(s"Created URL for Jaeger tracer: ${url}")
      senderBuilder <- ZIO.attempt(OkHttpSender.newBuilder.compressionEnabled(true).endpoint(url))
      tracer <- ZIO.attempt(
        new Configuration(serviceName).getTracerBuilder
          .withSampler(new ConstSampler(true))
          .withReporter(new ZipkinV2Reporter(AsyncReporter.create(senderBuilder.build)))
          .build
      )
    yield tracer

object TracingOps:
  extension [R, E, A](zio: ZIO[R, E, A])
    def span(openTracing: OpenTracing, name: String): ZIO[R, E, A] =
      openTracing.span(zio, name, tagError = true, logError = true)
    def root(openTracing: OpenTracing, name: String): ZIO[R, E, A] =
      openTracing.root(zio, name, tagError = true, logError = true)
    def tagMultiple(openTracing: OpenTracing, tags: Map[String, String]): ZIO[R, E, A] =
      tags
        .map { case (key, value) => openTracing.tag(zio, key, value) }
        .reduce { case (a, b) => a *> b }
