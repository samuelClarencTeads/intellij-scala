package org.jetbrains.plugins.scala.tasty

import dotty.tools.tasty.TastyFormat._

import scala.collection.mutable

// TODO refactor
// TODO use StringBuilder
object TreePrinter {

  def textOf(node: Node, definition: Option[Node] = None): String = node match {
    case Node(PACKAGE, _, Seq(Node(TERMREFpkg, Seq(name), _), tail: _*)) =>
      "package " + name + "\n" +
        "\n" +
        tail.map(textOf(_)).filter(_.nonEmpty).mkString("\n")

    case node @ Node(TYPEDEF, Seq(name), Seq(template, _: _*)) if !node.hasFlag(SYNTHETIC) =>
      val isEnum = node.hasFlag(ENUM)
      val isObject = !isEnum && node.hasFlag(OBJECT)
      val isImplicitClass = !isObject && node.nextSibling.exists(it => it.is(DEFDEF) && it.hasFlag(SYNTHETIC) && it.name == name)
      val isTypeMember = !template.is(TEMPLATE)
      val keyword = if (isEnum) (if (node.hasFlag(CASE)) "case " else "enum ") else (if (isObject) "object " else (if (node.hasFlag(TRAIT)) "trait " else if (isTypeMember) "type " else "class "))
      val identifier = if (isObject) node.previousSibling.fold(name)(_.name) else name
      val modifiers = modifiersIn(if (isObject) node.previousSibling.getOrElse(node) else node,
        if (isEnum) Set(ABSTRACT, SEALED, CASE, FINAL) else (if (isTypeMember) Set.empty else Set(OPAQUE))) + (if (isImplicitClass) "implicit " else "")
      modifiers + keyword + identifier + (if (!isTypeMember) textOf(template, Some(node)) else {
        val repr = node.children.headOption.filter(_.is(LAMBDAtpt)).getOrElse(node)
        val isAbstract = repr.children.exists(_.is(TYPEBOUNDStpt))
        parametersIn(repr, Some(repr)) + (if (isAbstract) ""else " = " + (if (node.hasFlag(OPAQUE)) "???" else textOf(repr.children.findLast(_.isTypeTree).get))) // TODO parameter, { /* compiled code */ }
      })

    case node @ Node(TEMPLATE, _, children) => // TODO method?
      val primaryConstructor = children.find(it => it.is(DEFDEF) && it.names == Seq("<init>"))
      val modifiers = primaryConstructor.map(modifiersIn(_)).map(it => if (it.nonEmpty) " " + it else "").getOrElse("")
      val parameters = primaryConstructor.map(it => parametersIn(it, Some(node), definition)).getOrElse("")
      val parents0 = children.collect {
        case node if node.isTypeTree => Some(textOf(node))
        case Node(APPLY, _, Seq(Node(SELECTin, _, Seq(Node(NEW, _, Seq(tpe, _: _*)), _: _*)), args: _*)) =>
          Some(textOf(tpe)).filter(_ != "Object") // TODO FQN
        case Node(APPLY, _, Seq(Node(APPLY, _, Seq(Node(SELECTin, _, Seq(Node(NEW, _, Seq(tpe, _: _*)), _: _*)), _: _*)), args: _*)) =>
          Some(textOf(tpe)).filter(_ != "Object") // TODO FQN
        case Node(APPLY, _, Seq(Node(TYPEAPPLY, _, Seq(Node(SELECTin, _, Seq(Node(NEW, _, Seq(tpe, _: _*)), _: _*)), _: _*)), args: _*)) =>
          Some(textOf(tpe)).filter(_ != "Object") // TODO FQN
      }
      val parents = parents0.collect{ case Some(s) if s.nonEmpty => s }
      val cases = // TODO check element types
        if (definition.exists(_.hasFlag(ENUM))) definition.get.nextSibling.get.nextSibling.get.children.head.children.filter(it => it.is(VALDEF) || it.is(TYPEDEF))
        else Seq.empty
      val members = (children.filter(it => it.is(DEFDEF, VALDEF, TYPEDEF) && !primaryConstructor.contains(it)) ++ cases) // TODO type member
        .map(textOf(_, definition)).filter(_.nonEmpty).map(indent).mkString("\n\n")
      (modifiers + (if (modifiers.nonEmpty && parameters.isEmpty) "()" else parameters)) +
        (if (parents.isEmpty) "" else " extends " + parents.mkString(" with ")) +
        (if (members.isEmpty) "" else " {\n" + members + "\n}")

    case node @ Node(DEFDEF, Seq(name), children) if !node.hasFlag(FIELDaccessor) && !node.hasFlag(SYNTHETIC) && !name.contains("$default$") => // TODO why it's not synthetic?
      val isDeclaration = children.filter(!_.isModifier).lastOption.exists(_.isTypeTree)
      val tpe = children.find(_.isTypeTree)
      children.filter(_.is(EMPTYCLAUSE, PARAM))
      if (name == "<init>") {
        modifiersIn(node) + "def this" + parametersIn(node) + " = ???" // TODO parameter, { /* compiled code */ }
      } else {
        (if (node.hasFlag(EXTENSION)) "extension " + parametersIn(node, target = Target.Extension) + "\n  " else "") +
          modifiersIn(node) + "def " + name + parametersIn(node, target = if (node.hasFlag(EXTENSION)) Target.ExtensionMethod else Target.Definition) +
          ": " + tpe.map(textOf(_)).getOrElse("") + (if (isDeclaration) "" else " = ???") // TODO parameter, { /* compiled code */ }
      }

    case node @ Node(VALDEF, Seq(name), children) if !node.hasFlag(SYNTHETIC) && !node.hasFlag(OBJECT) =>
      val isDeclaration = children.filter(!_.isModifier).lastOption.exists(_.isTypeTree)
      val isCase = node.hasFlag(CASE)
      val tpe = children.find(_.isTypeTree)
      val template = // TODO check element types
        if (isCase) children.lift(1).flatMap(_.children.lift(1)).flatMap(_.children.headOption).map(textOf(_)).getOrElse("")
        else ""
      if (isCase && !definition.exists(_.hasFlag(ENUM))) "" else modifiersIn(node) + (if (isCase) name +  template else
        (if (node.hasFlag(MUTABLE)) "var " else "val ") + name + ": " + tpe.map(textOf(_)).getOrElse("") + (if (isDeclaration) "" else " = ???")) // TODO parameter, /* compiled code */

    // TODO method?
    case Node(IDENTtpt, Seq(name), _) => name
    case Node(TYPEREF, Seq(name), _) => name
    case Node(APPLIEDtpt, _, Seq(constructor, arguments: _*)) => textOf(constructor) + "[" + arguments.map(textOf(_)).mkString(", ") + "]"
    case Node(ANNOTATEDtpt | ANNOTATEDtype, _, Seq(tpe, annotation)) =>
      annotation match {
        case Node(APPLY, _, Seq(Node(SELECTin, _, Seq(Node(NEW, _, Seq(tpe0, _: _*)), _: _*)), args: _*)) if textOf(tpe0) == "Repeated" => // TODO FQN
          textOf(tpe.children(1)) + "*" // TODO check tree (APPLIEDtpt)
        case _ => textOf(tpe)
      }
    case Node(BYNAMEtpt, _, Seq(tpe)) => "=> " + textOf(tpe)

    case _ => ""
  }

  private def indent(s: String): String = s.split("\n").map(s => if (s.forall(_.isWhitespace)) "" else "  " + s).mkString("\n") // TODO use indent: Int parameter instead

  private enum Target {
    case Definition
    case Extension
    case ExtensionMethod
  }

  private def popExtensionParams(stack: mutable.Stack[Node]): Seq[Node] = {
    val buffer = mutable.Buffer[Node]()
    buffer ++= stack.popWhile(!_.is(PARAM))
    buffer += stack.pop()
    while (stack(0).is(SPLITCLAUSE) && stack(1).hasFlag(GIVEN)) {
      buffer += stack.pop()
      buffer ++= stack.popWhile(_.is(PARAM))
    }
    buffer.toSeq
  }

  private def parametersIn(node: Node, template: Option[Node] = None, definition: Option[Node] = None, target: Target = Target.Definition): String = {
    val tps = target match {
      case Target.Extension => node.children.takeWhile(!_.is(PARAM))
      case Target.ExtensionMethod => node.children.dropWhile(!_.is(PARAM))
      case Target.Definition => node.children
    }

    val templateTypeParams = template.map(_.children.filter(_.is(TYPEPARAM)).iterator)

    var params = ""
    var open = false
    var next = false

    tps.foreach {
      case Node(TYPEPARAM, Seq(name), Seq(Node(TYPEBOUNDStpt, _, Seq(lower, upper)), _: _*)) =>
        if (!open) {
          params += "["
          open = true
          next = false
        }
        if (next) {
          params += ", "
        }
        templateTypeParams.map(_.next()).foreach { typeParam =>
          if (typeParam.hasFlag(COVARIANT)) {
            params += "+"
          }
          if (typeParam.hasFlag(CONTRAVARIANT)) {
            params += "-"
          }
        }
        params += name
        val l = textOf(lower)
        if (l.nonEmpty && l != "Nothing") {
          params += " >: " + l
        }
        val u = textOf(upper)
        if (u.nonEmpty && u != "Any") {
          params += " <: " + u
        }
        next = true
      case _ =>
    }
    if (open) params += "]"

    val ps = target match {
      case Target.Extension =>
        popExtensionParams(mutable.Stack[Node](node.children: _*))
      case Target.ExtensionMethod =>
        val stack = mutable.Stack[Node](node.children: _*)
        popExtensionParams(stack)
        stack.toSeq.dropWhile(_.is(SPLITCLAUSE))
      case Target.Definition => node.children
    }

    val templateValueParams = template.map(_.children.filter(_.is(PARAM)).iterator)

    open = false
    next = false

    ps.foreach {
      case Node(EMPTYCLAUSE, _, _) =>
        params += "()"
      case Node(SPLITCLAUSE, _, _) =>
        params += ")"
        open = false
      case node @ Node(PARAM, Seq(name), children) =>
        if (!open) {
          params += "("
          open = true
          next = false
          if (node.hasFlag(GIVEN)) {
            params += "using "
          }
          if (node.hasFlag(IMPLICIT)) {
            params += "implicit "
          }
        }
        if (next) {
          params += ", "
        }
        templateValueParams.map(_.next()).foreach { valueParam =>
          if (!valueParam.hasFlag(LOCAL)) {
            params += modifiersIn(valueParam)
            if (valueParam.hasFlag(MUTABLE)) {
              params += "var "
            } else {
              if (!(definition.exists(_.hasFlag(CASE)) && valueParam.flags.forall(_.is(CASEaccessor, HASDEFAULT)))) {
                params += "val "
              }
            }
          }
        }
        params += name + ": " + textOf(children.head)
        if (node.hasFlag(HASDEFAULT)) {
          params += " = ???" // TODO parameter, /* compiled code */
        }
        next = true
      case _ =>
    }
    if (open) params += ")"
    if (template.isEmpty || definition.exists(it => it.hasFlag(CASE) && !it.hasFlag(OBJECT))) params else params.stripSuffix("()")
  }

  private def modifiersIn(node: Node, excluding: Set[Int] = Set.empty): String = { // TODO Optimize
    var s = ""
    if (node.hasFlag(OVERRIDE)) {
      s += "override "
    }
    if (node.hasFlag(PRIVATE)) {
      s += "private "
    } else if (node.hasFlag(PROTECTED)) {
      s += "protected "
    }
    if (node.hasFlag(GIVEN)) {
      s += "using "
    }
    if (node.hasFlag(IMPLICIT)) {
      s += "implicit "
    }
    if (node.hasFlag(FINAL) && !excluding(FINAL)) {
      s += "final "
    }
    if (node.hasFlag(LAZY)) {
      s += "lazy "
    }
    if (node.hasFlag(ABSTRACT) && !excluding(ABSTRACT)) {
      s += "abstract "
    }
    if (node.hasFlag(SEALED) && !excluding(SEALED)) {
      s += "sealed "
    }
    if (node.hasFlag(OPEN)) {
      s += "open "
    }
    if (node.hasFlag(TRANSPARENT)) {
      s += "transparent "
    }
    if (node.hasFlag(OPAQUE) && !excluding(OPAQUE)) {
      s += "opaque "
    }
    if (node.hasFlag(CASE) && !excluding(CASE)) {
      s += "case "
    }
    s
  }
}
