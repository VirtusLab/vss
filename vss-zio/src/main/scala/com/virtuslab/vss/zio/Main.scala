package com.virtuslab.vss.zio

import zio.*
import zio.logging.consoleLogger
import com.virtuslab.vss.zio.base.BaseMain
import com.virtuslab.vss.zio.stats.StatsMain

object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger()

  override def run: ZIO[Environment & ZIOAppArgs & Scope, Throwable, Any] = for 
    baseFiber <- BaseMain.run.fork
    statsFiber <- StatsMain.run.fork
    _ <- baseFiber.zip(statsFiber).join
  yield ()

