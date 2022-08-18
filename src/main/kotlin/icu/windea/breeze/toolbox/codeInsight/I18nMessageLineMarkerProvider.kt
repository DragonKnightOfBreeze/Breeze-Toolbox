package icu.windea.breeze.toolbox.codeInsight

import com.intellij.codeInsight.daemon.*
import com.intellij.lang.properties.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.progress.*
import com.intellij.psi.*
import icu.windea.breeze.toolbox.*
import org.jetbrains.uast.*

class I18nMessageLineMarkerProvider : LineMarkerProviderDescriptor() {
	override fun getName() = BreezeBundle.getMessage("gutterIcon.i18nMessage")
	
	override fun getIcon() = BreezeIcons.Gutter.I18nMessage
	
	override fun getLineMarkerInfo(element: PsiElement) = null
	
	override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
		//可能会有重复，需要过滤
		val expressions = elements.mapNotNullTo(mutableSetOf()) { element ->
			element.toUElementOfType<ULiteralExpression>()
		}
		for(expression in expressions) {
			ProgressManager.checkCanceled()
			val targetElement = expression.sourcePsi?.firstChild ?: continue
			if(!expression.isI18nProperty()) continue
			val property = expression.getI18nProperty() ?: continue
			val text = renderText(property) ?: continue
			val lineMarkerInfo = createLineMarkerInfo(targetElement, text, property)
			result.add(lineMarkerInfo)
		}
	}
	
	private fun renderText(property: IProperty): String?{
		val key = property.name ?: return null
		val value = property.value ?: return null
		return buildString {
			append("<b>").append(key).append("</b>")
			append("<br>")
			append(value.handleHtmlI18nPropertyValue())
		}
	}
	
	private fun createLineMarkerInfo(element: PsiElement, text: String, property: IProperty): LineMarkerInfo<PsiElement> {
		return LineMarkerInfo(
			element,
			element.textRange,
			icon,
			{ text },
			{ _, _ -> property.navigate(true) },
			GutterIconRenderer.Alignment.RIGHT,
			{ name }
		)
	}
}