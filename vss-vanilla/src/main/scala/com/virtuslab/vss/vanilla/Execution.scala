package com.virtuslab.vss.vanilla

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object Execution:

  given FutureContext: ExecutionContext = ExecutionContext.fromExecutor(
    null,
    t => {
      t.printStackTrace()
      sys.exit(1)
    }
  )

  Thread.setDefaultUncaughtExceptionHandler((thread, throwable) => {
    if !NonFatal(throwable) then
      println(s"Encountered a fatal error on thread $thread: $throwable")
      throwable.printStackTrace()
      sys.exit(1)
  })
