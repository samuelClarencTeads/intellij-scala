package org.jetbrains.plugins.scala.dfa

object DfaOps extends lattice.HasTopOps
  with lattice.HasBottomOps
  with lattice.SemiLatticeOps
  with lattice.JoinSemiLatticeOps
  with lattice.MeetSemiLatticeOps