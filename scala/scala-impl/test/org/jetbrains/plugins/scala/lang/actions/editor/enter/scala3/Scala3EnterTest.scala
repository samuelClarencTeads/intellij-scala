package org.jetbrains.plugins.scala.lang.actions.editor.enter.scala3

import com.intellij.application.options.CodeStyle
import org.jetbrains.plugins.scala.{ScalaFileType, ScalaLanguage}

/** NOTE: much more tests are generated and run in [[Scala3BracelessSyntaxEnterHandlerTest_Exhaustive]] */
class Scala3EnterTest extends Scala3EnterBaseTest
  with CheckIndentAfterTypingCodeOps
  with DoEditorStateTestOps {

  import Scala3TestDataBracelessCode.*

  private def doTypingTestInAllContexts(
    contextCode: String,
    codeToType: CodeWithDebugName,
    wrapperContexts: Seq[CodeWithDebugName]
  ): Unit =
    for {
      wrapperContext <- wrapperContexts
    } {
      val contextCodeNew = injectCodeWithIndentAdjust(contextCode, wrapperContext.code)
      checkIndentAfterTypingCode(contextCodeNew, codeToType.code)
    }

  private def doEnterAfterFunctionTypeArrow(contextCode: String): Unit ={
    doTypingTestInAllContexts(contextCode, CodeToType.BlankLines, WrapperCodeContexts.AllContexts)
    assert(contextCode.contains("=>"))

    val contextCode2 = contextCode.replace("=>", "?=>")
    doTypingTestInAllContexts(contextCode2, CodeToType.BlankLines, WrapperCodeContexts.AllContexts)
  }

  def testAfterFunctionTypeArrow_1(): Unit =
    doEnterAfterFunctionTypeArrow(s"""type Contextual1[T] = ExecutionContext =>$CARET""")
  def testAfterFunctionTypeArrow_2(): Unit =
    doEnterAfterFunctionTypeArrow(s"""type Contextual1[T] = ExecutionContext =>$CARET   T""")
  def testAfterFunctionTypeArrow_3(): Unit =
    doEnterAfterFunctionTypeArrow(s"""type Contextual1[T] = ExecutionContext =>   ${CARET}T""")
  def testAfterFunctionTypeArrow_4(): Unit =
    doEnterAfterFunctionTypeArrow(s"""type Contextual1[T] = ExecutionContext =>  $CARET  T""")

  def textTopLevelFunctionWithNonEmptyBody_EOF(): Unit = {
    def doMyTest(context: String): Unit =
      checkIndentAfterTypingCode(context, CodeToType.BlankLines.code)

    doMyTest(
      s"""def foo =
         |  1$CARET""".stripMargin
    )
    doMyTest(
      s"""def foo =
         |  1$CARET   ${""}""".stripMargin
    )
    doMyTest(
      s"""def foo =
         |  1   $CARET   ${""}""".stripMargin
    )
    doMyTest(
      s"""def foo =
         |  1   $CARET""".stripMargin
    )
    doMyTest(
      s"""def foo =
         |  1   $CARET
         |""".stripMargin
    )
    doMyTest(
      s"""def foo =
         |  1   $CARET   ${""}
         |""".stripMargin

    )
    doMyTest(
      s"""def foo =
         |  1$CARET   ${""}
         |""".stripMargin
    )
    doMyTest(
      s"""def foo =
         |  1
         |
         |  $CARET""".stripMargin
    )
    doMyTest(
      s"""def foo =
         |  1
         |
         |  $CARET   ${""}""".stripMargin
    )
    doMyTest(
      s"""def foo =
         |  1
         |
         |  $CARET
         |     ${""}
         |     ${""}
         |  """.stripMargin
    )
  }

  def testUnindentedCaret_InIndentedFunctionBody_BetweenMultipleStatements(): Unit = {
    val text1 =
      s"""class A {
         |  def foo =
         |    val x = 1
         |    val y = 2
         |  $CARET
         |    val z = 3
         |}""".stripMargin
    val text2 =
      s"""class A {
         |  def foo =
         |    val x = 1
         |    val y = 2
         |
         |    $CARET
         |    val z = 3
         |}""".stripMargin
    doTestForAllDefTypes(text1, text2)
  }

  protected def doTestForAllDefTypes(textBefore: String, textAfter: String): Unit = {
    doEnterTest(textBefore, textAfter)
    if (textBefore.contains("def")) {
      doEnterTest(textBefore.replace("def", "val"), textAfter.replace("def", "val"))
      doEnterTest(textBefore.replace("def", "var"), textAfter.replace("def", "var"))
    }
  }

  def testUnindentedCaret_Extension_Collective(): Unit =
    doEnterTest(
      s"""object A {
         |  extension[T] (xs: List[T])(using Ordering[T])
         |    def smallest(n: Int): List[T] = xs.sorted.take(n)
         |$CARET
         |    def smallestIndices(n: Int): List[Int] =
         |      val limit = smallest(n).max
         |      xs.zipWithIndex.collect { case (x, i) if x <= limit => i }
         |}""".stripMargin,
      s"""object A {
         |  extension[T] (xs: List[T])(using Ordering[T])
         |    def smallest(n: Int): List[T] = xs.sorted.take(n)
         |
         |    $CARET
         |    def smallestIndices(n: Int): List[Int] =
         |      val limit = smallest(n).max
         |      xs.zipWithIndex.collect { case (x, i) if x <= limit => i }
         |}""".stripMargin
    )

  def testAfterFunctionBodyOnSameLineWithEquals(): Unit = doEditorStateTest((
    s"""class B:
       |  def foo = 1$CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText.Enter
  ), (
    s"""class B:
       |  def foo = 1
       |  $CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText("def baz = 23")
  ), (
    s"""class B:
       |  def foo = 1
       |  def baz = 23$CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText("def baz = 23")
  ))

  def testAfterFunctionBodyOnSameLineWithEquals_IndentedCaretAfterCompleteBody(): Unit = doEnterTest(
    s"""class B:
       |  def foo = 1
       |    $CARET
       |
       |  def bar = 2
       |""".stripMargin,
    s"""class B:
       |  def foo = 1
       |
       |  $CARET
       |
       |  def bar = 2
       |""".stripMargin
  )

  def testAfterFunctionBodyOnSameLineWithEquals_InfixOp(): Unit = doEditorStateTest((
    s"""class B:
       |  def foo = 1 + $CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText.Enter
  ), (
    s"""class B:
       |  def foo = 1 +
       |  $CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText("42")
  ), (
    s"""class B:
       |  def foo = 1 +
       |    42$CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText("def baz = 23")
  ))

  def testInNestedFunction(): Unit = doEnterTest(
    s"""class A {
       |  def f1 =
       |    1
       |    2
       |    def f2 =
       |      1
       |      2$CARET
       |}
       |""".stripMargin,
    s"""class A {
       |  def f1 =
       |    1
       |    2
       |    def f2 =
       |      1
       |      2
       |      $CARET
       |}
       |""".stripMargin
  )

  private def doEnterTestWithAndWithoutTabs(before: String, after: String): Unit = {
    doEnterTest(before, after)

    val tabSizeNew = 3

    val indentOptions = CodeStyle.getSettings(getProject).getIndentOptions(ScalaFileType.INSTANCE)
    val useTabsBefore = indentOptions.USE_TAB_CHARACTER
    val tabSizeBefore = indentOptions.TAB_SIZE

    try {
      indentOptions.TAB_SIZE = tabSizeNew
      indentOptions.USE_TAB_CHARACTER = true

      def useTabs(text: String): String = text.replaceAll(" " * 3, "\t")

      val beforeWithTabs = useTabs(before)
      val afterWithTabs = useTabs(after)
      doEnterTest(beforeWithTabs, afterWithTabs)
    } finally {
      indentOptions.USE_TAB_CHARACTER = useTabsBefore
      indentOptions.TAB_SIZE = tabSizeBefore
    }
  }

  def testCaretIsIndentedToTheRightFromLastElementInIndentationContext_0(): Unit = doEditorStateTest((
    s"""class B:
       |  def foo =     $CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText("\nprintln(1)")
  ), (
    s"""class B:
       |  def foo =
       |    println(1)$CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText("   \n    ")
  ), (
    s"""class B:
       |  def foo =
       |    println(1)
       |        $CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText("\nprintln(2)")
  ), (
    s"""class B:
       |  def foo =
       |    println(1)
       |
       |    println(2)$CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText("   \n    ")
  ), (
    s"""class B:
       |  def foo =
       |    println(1)
       |
       |    println(2)
       |        $CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText("\nprintln(3)")
  ), (
    s"""class B:
       |  def foo =
       |    println(1)
       |
       |    println(2)
       |
       |    println(3)$CARET
       |
       |  def bar = 2
       |""".stripMargin,
    TypeText.Ignored
  ))

  def testCaretIsIndentedToTheRightFromLastElementInIndentationContext_1(): Unit = doEnterTestWithAndWithoutTabs(
    s"""class A:
      |    def foo =
      |        println("start")
      |        if 2 + 2 == 42 then
      |            println(1)
      |            println(2)
      |              $CARET
      |""".stripMargin,
    s"""class A:
      |    def foo =
      |        println("start")
      |        if 2 + 2 == 42 then
      |            println(1)
      |            println(2)
      |
      |            $CARET
      |""".stripMargin,
  )

  def testCaretIsUnindentedToTheLeftFromLastElementInIndentationContext_0(): Unit = doEnterTestWithAndWithoutTabs(
    s"""class A:
      |    def foo =
      |        println("start")
      |        if 2 + 2 == 42 then
      |            println(1)
      |            println(2)
      |          $CARET
      |""".stripMargin,
    s"""class A:
      |    def foo =
      |        println("start")
      |        if 2 + 2 == 42 then
      |            println(1)
      |            println(2)
      |
      |        $CARET
      |""".stripMargin,
  )

  def testCaretIsUnindentedToTheLeftFromLastElementInIndentationContext_1(): Unit = doEnterTestWithAndWithoutTabs(
    s"""class A:
      |    def foo =
      |        println("start")
      |        if 2 + 2 == 42 then
      |            println(1)
      |            println(2)
      |      $CARET
      |""".stripMargin,
    s"""class A:
      |    def foo =
      |        println("start")
      |        if 2 + 2 == 42 then
      |            println(1)
      |            println(2)
      |
      |    $CARET
      |""".stripMargin,
  )

  def testCaretIsUnindentedToTheLeftFromLastElementInIndentationContext_2(): Unit = doEnterTestWithAndWithoutTabs(
    s"""class A:
      |    def foo =
      |        println("start")
      |        if 2 + 2 == 42 then
      |            println(1)
      |            println(2)
      |  $CARET
      |""".stripMargin,
    s"""class A:
      |    def foo =
      |        println("start")
      |        if 2 + 2 == 42 then
      |            println(1)
      |            println(2)
      |
      |$CARET
      |""".stripMargin,
  )

  def test_UnindentedCaret_InClass_WithBraces(): Unit = doEnterTest(
    s"""class A {
       |  def f1 =
       |    1
       |    2
       |$CARET
       |}
       |""".stripMargin,
    s"""class A {
       |  def f1 =
       |    1
       |    2
       |
       |  $CARET
       |}
       |""".stripMargin
  )

  def test_UnindentedCaret_InClass_WithEndMarker(): Unit = doEnterTest(
    s"""class A:
       |  def f1 =
       |    1
       |    2
       |$CARET
       |end A
       |""".stripMargin,
    s"""class A:
       |  def f1 =
       |    1
       |    2
       |
       |  $CARET
       |end A
       |""".stripMargin
  )

  def test_UnindentedCaret_InClass_WithoutEndMarker(): Unit = doEnterTest(
    s"""class A:
       |  def f1 =
       |    1
       |    2
       |$CARET
       |""".stripMargin,
    s"""class A:
       |  def f1 =
       |    1
       |    2
       |
       |$CARET
       |""".stripMargin
  )

  def test_UnindentedCaret_InNestedClass_WithBraces(): Unit = doEnterTest(
    s"""class Outer {
       |  class A {
       |    def f1 =
       |      1
       |      2
       |  $CARET
       |  }
       |}""".stripMargin,
    s"""class Outer {
       |  class A {
       |    def f1 =
       |      1
       |      2
       |
       |    $CARET
       |  }
       |}""".stripMargin
  )

  def test_UnindentedCaret_InNestedClass_WithEndMarker(): Unit = doEnterTest(
    s"""class Outer {
       |  class A:
       |    def f1 =
       |      1
       |      2
       |  $CARET
       |  end A
       |}""".stripMargin,
    s"""class Outer {
       |  class A:
       |    def f1 =
       |      1
       |      2
       |
       |    $CARET
       |  end A
       |}""".stripMargin
  )

  def test_UnindentedCaret_InNestedClass_WithoutEndMarker(): Unit = doEnterTest(
    s"""class Outer {
       |  class A:
       |    def f1 =
       |      1
       |      2
       |  $CARET
       |}""".stripMargin,
    s"""class Outer {
       |  class A:
       |    def f1 =
       |      1
       |      2
       |
       |  $CARET
       |}""".stripMargin
  )

  def test_UnindentedCaret_InNestedClass_WithoutEndMarker_1(): Unit = doEnterTest(
    s"""class Outer {
       |  class A:
       |    def f1 =
       |      1
       |      2
       |$CARET
       |}""".stripMargin,
    s"""class Outer {
       |  class A:
       |    def f1 =
       |      1
       |      2
       |
       |  $CARET
       |}""".stripMargin
  )

  def test_Return_WithIndentationBasedBlock(): Unit = {
    val contextCode =
      s"""def foo123: String =
         |  return$CARET
         |""".stripMargin
    doTypingTestInAllContexts(contextCode, CodeToType.BlankLines, WrapperCodeContexts.AllContexts)
    doTypingTestInAllContexts(contextCode, CodeToType.BlockStatements, WrapperCodeContexts.AllContexts)
  }

  def testAfterCodeInCaseClause_EOF_1(): Unit = doEnterTest_NonStrippingTrailingSpaces(
    s"""Option(42) match
       |  case Some(1) =>
       |    111$CARET""",
    s"""Option(42) match
       |  case Some(1) =>
       |    111
       |    $CARET""".stripMargin
  )

  def testAfterCodeInCaseClause_EOF_2(): Unit = doEnterTest_NonStrippingTrailingSpaces(
    s"""Option(42) match
       |  case Some(1) =>
       |    111$CARET  ${""}""",
    s"""Option(42) match
       |  case Some(1) =>
       |    111
       |    $CARET""".stripMargin
  )

  def testAfterCodeInCaseClause_EOF_3(): Unit = doEnterTest_NonStrippingTrailingSpaces(
    s"""Option(42) match
       |  case Some(1) =>
       |    111   $CARET  ${""}""",
    s"""Option(42) match
       |  case Some(1) =>
       |    111   ${""}
       |    $CARET""".stripMargin
  )

  def testAfterCodeInCaseClause_EOF_4(): Unit = doEnterTest_NonStrippingTrailingSpaces(
    s"""Option(42) match
       |  case Some(1) =>
       |    111$CARET
       |""",
    s"""Option(42) match
       |  case Some(1) =>
       |    111
       |    $CARET
       |""".stripMargin
  )

  def testAfterCodeInMiddleCaseClause_SameLine(): Unit = {
    doEnterTest(
      s"""42 match
         |  case first => 1$CARET
         |  case last  => 1
         |""".stripMargin,
      s"""42 match
         |  case first => 1
         |  $CARET
         |  case last  => 1
         |""".stripMargin
    )

    doEnterTest(
      s"""42 match
         |  case first => 1
         |      $CARET
         |  case last  => 1
         |""".stripMargin,
      s"""42 match
         |  case first => 1
         |
         |  $CARET
         |  case last  => 1
         |""".stripMargin
    )
  }

  def testAfterCodeInLastCaseClause_SameLine(): Unit = {
    doEnterTest(
      s"""42 match
         |  case first => 1
         |  case last  => 1$CARET
         |""".stripMargin,
      s"""42 match
         |  case first => 1
         |  case last  => 1
         |  $CARET
         |""".stripMargin
    )

    doEnterTest(
      s"""42 match
         |  case first => 1
         |  case last  => 1
         |      $CARET
         |""".stripMargin,
      s"""42 match
         |  case first => 1
         |  case last  => 1
         |
         |  $CARET
         |""".stripMargin
    )
  }

  def testAfterCodeInLastCaseClause_EOF(): Unit =
    doEnterTest(
      s"""42 match
         |  case first => 1
         |  case last  =>$CARET""".stripMargin,
      s"""42 match
         |  case first => 1
         |  case last  =>
         |    $CARET""".stripMargin
    )

  def testEnterHandlerShouldRespectIndentOptions(): Unit = {
    val settings = CodeStyle.getSettings(getProject).getCommonSettings(ScalaLanguage.INSTANCE)
    val indentOptions = settings.getIndentOptions

    val USE_TAB_CHARACTER_BEFORE = indentOptions.USE_TAB_CHARACTER
    val TAB_SIZE_BEFORE = indentOptions.TAB_SIZE
    val INDENT_SIZE_BEFORE = indentOptions.INDENT_SIZE

    indentOptions.USE_TAB_CHARACTER = true
    indentOptions.TAB_SIZE = 3
    indentOptions.INDENT_SIZE = 5

    try doEnterTest(
      s"""class A {
         |\t  class B {
         |\t\t\t 1 match
         |\t\t\t\tcase 11 =>$CARET
         |\t\t\t\tcase 22 =>
         |\t  }
         |}
         |""".stripMargin,
      s"""class A {
         |\t  class B {
         |\t\t\t 1 match
         |\t\t\t\tcase 11 =>
         |\t\t\t\t\t  $CARET
         |\t\t\t\tcase 22 =>
         |\t  }
         |}
         |""".stripMargin
    )
    finally {
      indentOptions.USE_TAB_CHARACTER = USE_TAB_CHARACTER_BEFORE
      indentOptions.TAB_SIZE = TAB_SIZE_BEFORE
      indentOptions.INDENT_SIZE = INDENT_SIZE_BEFORE
    }
  }
}
