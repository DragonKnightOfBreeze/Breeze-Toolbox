package icu.windea.breeze.toolbox.editor

import com.intellij.lang.documentation.*
import com.intellij.lang.properties.psi.*
import com.intellij.psi.*
import icu.windea.breeze.*
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
			renderLocationString(this, property)
			renderPropertyKey(this, property)
			append(DocumentationMarkup.DEFINITION_END)
		}
	}
	
	private fun getPropertyDoc(property: Property): String {
		return buildString {
			append(DocumentationMarkup.DEFINITION_START)
			renderLocationString(this, property)
			renderPropertyKey(this, property)
			append(DocumentationMarkup.DEFINITION_END)
			append(DocumentationMarkup.CONTENT_START)
			renderPropertyValue(this, property)
			append(DocumentationMarkup.CONTENT_END)
		}
	}
	
	private fun renderLocationString(builder: StringBuilder, element: PsiElement) {
		val file = element.containingFile ?: return
		builder.append("[").append(file.name).append("]<br>")
	}
	
	private fun renderPropertyKey(builder: StringBuilder, property: Property) {
		val key = property.name ?: return
		builder.append("<b>").append(key).append("</b>")
	}
	
	private fun renderPropertyValue(builder: StringBuilder, property: Property) {
		val value = property.value ?: return
		builder.append(value.handleHtmlI18nPropertyValue())
	}
}