package icu.windea.breeze.toolbox.codeInsight.markers

import com.intellij.codeInsight.daemon.*
import com.intellij.codeInsight.navigation.*
import com.intellij.openapi.progress.*
import com.intellij.psi.*
import com.intellij.ui.*
import icons.*
import icu.windea.breeze.toolbox.*
import icu.windea.breeze.toolbox.codeInsight.*
import org.jetbrains.uast.*
import java.util.*

/**
 * UAST - 对于引用到本地化文本的地方，加上装订线图标，可以鼠标悬浮显示对应的本地化文本，可以鼠标点击导航到声明处
 * * 显示实际上会读取到的处理后的本地化文本
 */
@Deprecated("DEPRECATED")
class I18nMessageLineMarkerProvider : LineMarkerProviderDescriptor() {
	override fun isEnabledByDefault() = false
	
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
			val tooltipText = getI18nMessageTooltip(properties, propertyName)
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