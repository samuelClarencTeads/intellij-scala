package org.jetbrains.plugins.scala
package lang
package psi
package api
package base

import org.jetbrains.plugins.scala.lang.psi.api.toplevel.*

/**
  * @author ilyas
  */
//wrapper over an identifier for variable declarations 'var v : T' 
trait ScFieldId extends ScTypedDefinition