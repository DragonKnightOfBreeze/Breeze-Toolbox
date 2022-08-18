package icu.windea.breeze.toolbox.editor

import com.intellij.lang.documentation.*
import com.intellij.lang.properties.psi.*
import com.intellij.psi.*
import icu.windea.breeze.toolbox.*

class PropertiesDocumentationProvider : AbstractDocumentationProvider() {
	override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement?): String? {
		return when {
			element is Property -> getPropertyInfo(element)
			else -> null
		}
	}
	
	override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
		return when {
			element is Property -> getPropertyDoc(element)
			else -> null
		}
	}
	
	private fun getPropertyInfo(property: Property): String {
		return buildString {
			append(DocumentationMarkup.DEFINITION_START)
			this.renderLocationString(property)
			this.renderPropertyKey(property)
			append(DocumentationMarkup.DEFINITION_END)
		}
	}
	
	private fun getPropertyDoc(property: Property): String {
		return buildString {
			append(DocumentationMarkup.DEFINITION_START)
			renderLocationString(property)
			renderPropertyKey(property)
			append(DocumentationMarkup.DEFINITION_END)
			append(DocumentationMarkup.CONTENT_START)
			renderPropertyValue(property)
			append(DocumentationMarkup.CONTENT_END)
		}
	}
	
	private fun StringBuilder.renderLocationString(element: PsiElement) {
		val file = element.containingFile ?: return
		append("[").append(file.name).append("]<br>")
	}
	
	private fun StringBuilder.renderPropertyKey(property: Property) {
		val key = property.name ?: return
		append("<b>").append(key).append("</b>")
	}
	
	private fun StringBuilder.renderPropertyValue(property: Property) {
		val value = property.value ?: return
		append(value.handleHtmlI18nPropertyValue())
	}
}