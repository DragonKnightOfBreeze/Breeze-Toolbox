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
			buildPropertyDefinition(property)
		}
	}
	
	private fun getPropertyDoc(property: Property): String {
		return buildString {
			buildPropertyDefinition(property)
			buildPropertyContent(property)
		}
	}
	
	private fun StringBuilder.buildPropertyDefinition(property: Property) {
		val fileName = property.containingFile?.name
		val key = property.name ?: anonymousString
		definition {
			if(fileName != null) {
				grayed {
					append("[").append(fileName.escapeXml()).append("]")
				}
				append("<br>")
			}
			append(key.escapeXml())
		}
	}
	
	private fun StringBuilder.buildPropertyContent(property: Property) {
		val value = property.value
		if(value != null) {
			content {
				append(value.handleHtmlI18nPropertyValue())
			}
		}
	}
}