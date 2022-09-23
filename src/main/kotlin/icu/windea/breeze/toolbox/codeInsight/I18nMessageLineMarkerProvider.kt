package icu.windea.breeze.toolbox.codeInsight

import com.intellij.codeInsight.daemon.*
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.properties.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.progress.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.util.Function
import icons.*
import icu.windea.breeze.toolbox.*
import org.jetbrains.uast.*
import java.util.*

class I18nMessageLineMarkerProvider : LineMarkerProviderDescriptor() {
	override fun getName() = BreezeBundle.message("gutterIcon.i18nMessage")
	
	override fun getIcon() = BreezeIcons.Gutter.I18nMessage
	
	override fun getLineMarkerInfo(element: PsiElement) = null
	
	override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
		//可能会有重复，需要过滤
		val expressions = elements.mapNotNullTo(mutableSetOf()) { element ->
			element.toUElementOfType<ULiteralExpression>()
		}
		for(expression in expressions) {
			ProgressManager.checkCanceled()
			val locationElement = expression.sourcePsi?.firstLeafOrSelf() ?: continue
			if(!expression.isI18nProperty()) continue
			//用户语言区域的本地化文本需要置顶
			val properties = expression.getI18nProperties()
				.sortedBy { it.propertiesFile?.locale == Locale.getDefault() }
			if(properties.isEmpty()) continue
			val propertyName = properties.first().name
			var appendHr = false
			val tooltipText = buildString {
				append("<code>$propertyName</code>")
				append("<br><br>")
				for(property in properties) {
					val value = property.value ?: continue
					val handledValue = value.handleHtmlI18nPropertyValue()
					if(appendHr) {
						append("<hr>")
					} else {
						appendHr = true
					}
					val file = property.propertiesFile
					if(file != null){
						append("<font color='#808080'><code>(${file.name})</code></font>")
					}
					append("<p>")
					append(handledValue)
					append("</p>")
				}
			}
			val lineMarkerInfo = NavigationGutterIconBuilder.create(icon)
				.setTooltipText(tooltipText)
				.setTargets(properties.filterIsInstance<PsiElement>())
				.setAlignment(GutterIconRenderer.Alignment.RIGHT)
				.setNamer { BreezeBundle.message("gutterIcon.i18nMessage") }
				.createLineMarkerInfo(locationElement)
			result.add(lineMarkerInfo)
		}
	}
}