package com.virtuslab.vss.zio.base.resources

import zio.*
import zio.telemetry.opentracing.OpenTracing

object TracingOps:
  extension [R, E, A](zio: ZIO[R, E, A])
    def span(openTracing: OpenTracing, name: String) =
      openTracing.span(zio, name, tagError = true, logError = true)
    def root(openTracing: OpenTracing, name: String) =
      openTracing.root(zio, name, tagError = true, logError = true)
    def tagMultiple(openTracing: OpenTracing, tags: Map[String, String]): ZIO[R, E, A] =
      tags
        .map { case (key, value) => openTracing.tag(zio, key, value) }
        .reduce { case (a, b) => a *> b }
