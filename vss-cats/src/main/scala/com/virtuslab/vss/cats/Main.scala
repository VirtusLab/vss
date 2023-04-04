package com.virtuslab.vss.cats

import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.virtuslab.vss.cats.base.BaseMain
import com.virtuslab.vss.cats.stats.StatsMain

object Main extends IOApp.Simple {

  override def run: IO[Unit] =
    Parallel.parTuple2(
      BaseMain.run,
      StatsMain.run
    ).void

}