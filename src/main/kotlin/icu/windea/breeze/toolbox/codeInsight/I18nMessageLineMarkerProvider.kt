package icu.windea.breeze.toolbox.codeInsight

import com.intellij.codeInsight.daemon.*
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.properties.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.progress.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.ui.*
import com.intellij.util.Function
import icons.*
import icu.windea.breeze.toolbox.*
import org.jetbrains.uast.*
import java.util.*

private val SEPARATOR_COLOR = JBColor.namedColor("GutterTooltip.lineSeparatorColor", HintUtil.INFORMATION_BORDER_COLOR);

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
				for(property in properties) {
					val value = property.value ?: continue
					if(appendHr) {
						append("<br>")
					} else {
						appendHr = true
					}
					val file = property.propertiesFile
					if(file != null) {
						append("<p style='margin-bottom:2pt;border-bottom:thin solid #")
						append(ColorUtil.toHex(SEPARATOR_COLOR))
						append("'><code>")
						append(propertyName).append(" <font color='#909090'>(").append(file.name).append(")</font>")
						append("</code></p>")
					}
					val handledValue = value.handleHtmlI18nPropertyValue()
					append("<p>")
					append(handledValue)
					append("</p>")
				}
			}
			val lineMarkerInfo = NavigationGutterIconBuilder.create(icon)
				.setTooltipText(tooltipText)
				.setPopupTitle(BreezeBundle.message("gutterIcon.i18nMessage.tooltip.title"))
				.setTargets(properties.filterIsInstance<PsiElement>())
				.setNamer { BreezeBundle.message("gutterIcon.i18nMessage") }
				.createLineMarkerInfo(locationElement)
			result.add(lineMarkerInfo)
		}
	}
}