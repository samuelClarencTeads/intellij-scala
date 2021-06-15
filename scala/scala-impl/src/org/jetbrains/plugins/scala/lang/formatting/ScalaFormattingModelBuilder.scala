package org.jetbrains.plugins.scala
package lang
package formatting

import com.intellij.formatting.*
import com.intellij.lang.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.{FormattingDocumentModelImpl, PsiBasedFormattingModel, FormatterUtil as PsiFormatterUtil}
import com.intellij.psi.impl.source.tree.TreeUtil
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.processors.ScalaFmtPreFormatProcessor

final class ScalaFormattingModelBuilder extends FormattingModelBuilder {

  import ScalaFormattingModelBuilder.*

  override def createModel(formattingContext: FormattingContext): FormattingModel = {
    val element = formattingContext.getPsiElement
    val styleSettings= formattingContext.getCodeStyleSettings

    Log.assertTrue(element.getNode != null, "AST should not be null for: " + element)

    if (styleSettings.getCustomSettings(classOf[settings.ScalaCodeStyleSettings]).USE_SCALAFMT_FORMATTER) {
      //preprocessing is done by this point, use this little side-effect to clean-up ranges synchronization
      // NOTE: looks like (only looks) this is not required?
      // I replaced this line directly in ScalaFmtPreFormatProcessor.process with rangesDeltaCache.remove(psiFile)
      //ScalaFmtPreFormatProcessor.clearRangesCache()
    }

    val file = element.getContainingFile
    val viewProvider = file.getViewProvider
    val containingFile = viewProvider.getPsi(viewProvider.getBaseLanguage)
    Log.assertTrue(containingFile != null, containingFile)

    val fileNode = file.getNode
    Log.assertTrue(fileNode != null, "AST should not be null for: " + containingFile)

    new ScalaFormattingModel(
      containingFile,
      new ScalaBlock(null, fileNode, null, null, Indent.getAbsoluteNoneIndent, null, styleSettings)
    )
  }

  override def getRangeAffectingIndent(file: PsiFile,
                                       offset: Int,
                                       elementAtOffset: ASTNode): TextRange =
    elementAtOffset.getTextRange
}

object ScalaFormattingModelBuilder {

  private val Log = Logger.getInstance(getClass)

  private final class ScalaFormattingModel(file: PsiFile, rootBlock: ScalaBlock)
    extends PsiBasedFormattingModel(file, rootBlock, FormattingDocumentModelImpl.createOn(file)) {

    import lexer.ScalaTokenTypes.WHITES_SPACES_FOR_FORMATTER_TOKEN_SET

    protected override def replaceWithPsiInLeaf(textRange: TextRange,
                                                whiteSpace: String,
                                                leafElement: ASTNode): String = leafElement.getElementType match {
      case elementType if !myCanModifyAllWhiteSpaces && WHITES_SPACES_FOR_FORMATTER_TOKEN_SET.contains(elementType) => null
      case _ =>
        val whiteSpaceToken = findWhiteSpaceToken(leafElement).getOrElse(TokenType.WHITE_SPACE)
        inWriteAction {
          PsiFormatterUtil.replaceWhiteSpace(whiteSpace, leafElement, whiteSpaceToken, textRange)
        }
        whiteSpace
    }

    private def findWhiteSpaceToken(node: ASTNode) =
      Option(TreeUtil.prevLeaf(node))
        .map(_.getElementType)
        .filter(WHITES_SPACES_FOR_FORMATTER_TOKEN_SET.contains)

  }

}