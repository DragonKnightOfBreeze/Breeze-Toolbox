package icu.windea.breeze.toolbox.codeInsight

import com.intellij.codeInsight.editorActions.enter.*
import com.intellij.lang.properties.PropertiesUtil
import com.intellij.lang.properties.parsing.*
import com.intellij.lang.properties.psi.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.actionSystem.*
import com.intellij.openapi.util.*
import com.intellij.psi.*

class EnterInPropertiesFileHandler : EnterHandlerDelegateAdapter() {
	override fun preprocessEnter(file: PsiFile, editor: Editor, caretOffsetRef: Ref<Int>, caretAdvance: Ref<Int>,
		dataContext: DataContext, originalHandler: EditorActionHandler?): EnterHandlerDelegate.Result {
		if(file is PropertiesFile) {
			val caretOffset = caretOffsetRef.get()
			val document = editor.document
			PsiDocumentManager.getInstance(file.getProject()).commitDocument(document)
			val psiAtOffset = file.findElementAt(caretOffset)
			handleEnterInPropertiesFile(editor, document, psiAtOffset, caretOffset)
			return EnterHandlerDelegate.Result.Stop
		}
		return EnterHandlerDelegate.Result.Continue
	}
	
	private fun handleEnterInPropertiesFile(editor: Editor, document: Document, psiAtOffset: PsiElement?, caretOffset: Int) {
		val text = document.text
		var line = text.substring(0, caretOffset)
		val i = line.lastIndexOf('\n')
		if(i > 0) line = line.substring(i)
		val elementType = psiAtOffset?.node?.elementType
		val toInsert: String = when {
			PropertiesUtil.isUnescapedBackSlashAtTheEnd(line) -> "\n"
			elementType === PropertiesTokenTypes.VALUE_CHARACTERS -> {
				if(text[caretOffset] == ' ' || text[caretOffset] == '\t') {
					// escape the whitespace on the next line like "\ "
					"\\\n\\"
				} else {
					"\\\n"
				}
			}
			elementType === PropertiesTokenTypes.END_OF_LINE_COMMENT && "#!".indexOf(document.text[caretOffset]) == -1 -> "\n#"
			else -> "\n"
		}
		document.insertString(caretOffset, toInsert)
		editor.caretModel.moveToOffset(caretOffset + toInsert.length)
		editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
		editor.selectionModel.removeSelection()
	}
}