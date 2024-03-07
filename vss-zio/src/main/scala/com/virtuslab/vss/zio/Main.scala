package com.virtuslab.vss.zio

import zio.*
import zio.logging.{consoleJsonLogger, consoleLogger}
import com.virtuslab.vss.zio.base.BaseMain
import com.virtuslab.vss.zio.stats.StatsMain
import zio.logging.slf4j.bridge.Slf4jBridge

object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger() >+> Slf4jBridge.init()

  override def run: RIO[Environment & ZIOAppArgs & Scope, Any] =
    ZIO.collectAllParDiscard(Vector(BaseMain.run, StatsMain.run))
