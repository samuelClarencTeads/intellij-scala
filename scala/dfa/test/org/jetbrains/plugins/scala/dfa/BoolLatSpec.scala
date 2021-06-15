package org.jetbrains.plugins.scala.dfa

import org.jetbrains.plugins.scala.dfa.BoolLat.*
import org.jetbrains.plugins.scala.dfa.lattice.{Lattice, LatticeSpec}
import org.scalatest.prop.TableFor3

class BoolLatSpec extends LatticeSpec[BoolLat] {
  override protected lazy val lattice: Lattice[BoolLat] = BoolLat.lattice

  override protected lazy val latticeElementSamples: Seq[BoolLat] =
    BoolSemiLatSpec.latticeElementSamples :+ Bottom

  override protected lazy val latticeJoinSamples: TableFor3[BoolLat, BoolLat, BoolLat] =
    Table(
      ("A", "B", "A join B"),
      BoolSemiLatSpec.latticeJoinSamples ++ (
              // pairs with bottom
              for {
                a <- latticeElementSamples
                b <- latticeElementSamples
                if a == Bottom || b == Bottom
              } yield (a, b, if (a == Bottom) b else a)
      ): _*
    )


  override protected lazy val latticeMeetSamples: TableFor3[BoolLat, BoolLat, BoolLat] =
    Table(
      ("A", "B", "A meet B"),

      Seq(
        (True,  True,  True),
        (False, False, False),
        (True,  False, Bottom),
        (False, True,  Bottom),
      ) ++ (
        // pairs with Top
        for {
          a <- latticeElementSamples
          b <- latticeElementSamples
          if a == Top || b == Top
        } yield (a, b, if (a == Top) b else a)
        ) ++ (
        // pairs with Bottom
        for {
          a <- latticeElementSamples
          b <- latticeElementSamples
          if (a == Bottom || b == Bottom) &&
            a != Top && b != Top // we already have them
        } yield (a, b, Bottom)
        )
        :_*
    )
}
