package com.virtuslab.vss.cats

import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.virtuslab.vss.cats.base.BaseMain
import com.virtuslab.vss.cats.stats.StatsMain

object Main extends IOApp.Simple {

  /**
    * Run the application. Run the main programs of the submodules in parallel.
    */
  override def run: IO[Unit] =
    (
      BaseMain.run,
      StatsMain.run
    ).parTupled.void
}
