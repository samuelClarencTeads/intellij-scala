package org.jetbrains.plugins.scala
package findUsages

import java.{util as ju}

import com.intellij.openapi.application.ReadActionProcessor
import com.intellij.openapi.project.{IndexNotReadyException, Project}
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.cache.impl.id.{IdIndex, IdIndexEntry}
import com.intellij.psi.impl.search.PsiSearchHelperImpl
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.search.{GlobalSearchScope, TextOccurenceProcessor, UsageSearchContext}
import com.intellij.psi.{PsiElement, PsiReference}
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.{CommonProcessors, Processor, QueryExecutor}
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.finder.ScalaFilterScope
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil

/**
  * Nikolay.Tropin
  * 9/10/13
  */
class OperatorAndBacktickedSearcher extends QueryExecutor[PsiReference, ReferencesSearch.SearchParameters] {

  override def execute(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor[? >: PsiReference]): Boolean = {
    val elementToSearch = queryParameters.getElementToSearch

    val namesToProcess = inReadAction {
      import ScalaNamesUtil.{isBacktickedName, isOpCharacter}
      elementToSearch match {
        case named: ScNamedElement if named.isValid =>
          named.name match {
            case isBacktickedName(name) =>
              val tail = s"`$name`" :: Nil
              name match {
                case "" => tail
                case _ => name :: tail
              }
            case name if name.exists(isOpCharacter) => name :: Nil
            case _ => Nil
          }
        case _ => Nil
      }
    }

    val scope = inReadAction(ScalaFilterScope(queryParameters))
    namesToProcess.foreach { name =>
      val processor = new TextOccurenceProcessor {
        override def execute(element: PsiElement, offsetInElement: Int): Boolean = {
          val references = inReadAction(element.getReferences)
          for {
            reference <- references
            if reference.getRangeInElement.contains(offsetInElement)
          } inReadAction {
            if (reference.isReferenceTo(elementToSearch) || reference.resolve() == elementToSearch) {
              if (!consumer.process(reference)) return false
            }
          }

          true
        }
      }

      try {
        new ScalaPsiSearchHelper(queryParameters.getProject)
          .processElementsWithWord(processor, scope, name, UsageSearchContext.IN_CODE, true)
      } catch {
        case _: IndexNotReadyException =>
      }
    }

    true
  }

  private class ScalaPsiSearchHelper(project: Project)
    extends PsiSearchHelperImpl(project) {

    override def processCandidateFilesForText(scope: GlobalSearchScope,
                                              searchContext: Short,
                                              caseSensitively: Boolean,
                                              text: String,
                                              processor: Processor[? >: VirtualFile]): Boolean = {
      if (!ScalaNamesValidator.isIdentifier(text)) return true

      val entries = ju.Collections.singletonList(new IdIndexEntry(text, caseSensitively))
      val collectProcessor = new CommonProcessors.CollectProcessor[VirtualFile]
      val condition: Condition[Integer] = { (value: Integer) =>
        (value.intValue & searchContext) != 0
      }

      inReadAction {
        FileBasedIndex.getInstance.processFilesContainingAllKeys(IdIndex.NAME, entries, scope, condition, collectProcessor)
      }

      val readActionProcessor: ReadActionProcessor[VirtualFile] = { (virtualFile: VirtualFile) =>
        processor.process(virtualFile)
      }
      ContainerUtil.process(collectProcessor.getResults, readActionProcessor)
    }
  }

}
