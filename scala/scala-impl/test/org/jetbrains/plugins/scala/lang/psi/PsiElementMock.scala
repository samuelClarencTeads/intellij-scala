package org.jetbrains.plugins.scala.lang.psi

import com.intellij.psi.PsiElement

import scala.util.parsing.combinator.*

/**
 * Pavel.Fatin, 11.05.2010
 */

class PsiElementMock(val name: String, children: PsiElementMock*) extends AbstractPsiElementMock {
  private var parent: PsiElement = _
  private var prevSibling: PsiElement = _
  private var nextSibling: PsiElement = _
  private val firstChild: PsiElement = children.headOption.orNull
  private val lastChild: PsiElement = children.lastOption.orNull
  
  
  for(child <- children) { child.parent = this }
  
  if(children.nonEmpty) {
    for(case (a, b) <- children.zip(children.tail)) {
      a.nextSibling = b
      b.prevSibling = a
    }
  }

  override def getParent: PsiElement = parent

  override def getContext: PsiElement = parent

  override def getPrevSibling: PsiElement = prevSibling

  override def getNextSibling: PsiElement = nextSibling

  override def getChildren: Array[PsiElement] = children.toArray

  override def getFirstChild: PsiElement = firstChild

  override def getLastChild: PsiElement = lastChild

  override def toString: String = name
  
  override def getText: String = {
    if(children.isEmpty) 
      toString 
    else 
      toString + "(" + children.map(_.getText).mkString(", ") + ")"
  }
}

object PsiElementMock extends JavaTokenParsers {
  def apply(name: String, children: PsiElementMock*) = new PsiElementMock(name, children*)

  def parse(s: String): PsiElementMock = parse(element, s).get
 
  private def element: Parser[PsiElementMock] = identifier~opt(elements) ^^ {
      case name~children => PsiElementMock(name, children.getOrElse(Nil)*)
  } 
 
  private def identifier: Parser[String] = """[^,() ]+""".r
    
  private def elements: Parser[List[PsiElementMock]] = "("~>repsep(element, ",")<~")"
}
