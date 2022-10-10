package icu.windea.breeze.toolbox.codeInsight.hints

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.*
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.designer.utils.Cursors
import com.intellij.lang.*
import com.intellij.lang.properties.*
import com.intellij.notification.Notifications
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.refactoring.suggested.*
import com.intellij.terminal.*
import com.intellij.uast.UastMetaLanguage
import com.intellij.ui.*
import com.intellij.ui.dsl.builder.*
import com.intellij.util.xml.ui.TooltipUtils
import icons.*
import icu.windea.breeze.toolbox.*
import org.jetbrains.kotlin.idea.codeInsight.hints.*
import org.jetbrains.plugins.notebooks.visualization.r.inlays.*
import org.jetbrains.uast.*
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.*

/**
 * UAST - 对于引用到本地化文本的地方，在消息码之后加上嵌入图标，可以鼠标悬浮显示对应的本地化文本，可以鼠标点击导航到声明处
 * * 显示实际上会读取到的处理后的本地化文本
 */
class I18nMessageHintsProvider : InlayHintsProvider<NoSettings> {
	companion object {
		private val settingsKey = SettingsKey<NoSettings>("breeze.toolbox.i18nMessage")
	}
	
	private val uastMetaLanguage = UastMetaLanguage.findInstance(UastMetaLanguage::class.java)
	
	override val key: SettingsKey<NoSettings> = settingsKey
	override val name: String = BreezeBundle.message("hint.i18nMessage.name")
	override val description: String? = BreezeBundle.message("hint.i18nMessage.description")
	override val previewText: String? get() = null
	
	override fun isLanguageSupported(language: Language): Boolean {
		return uastMetaLanguage.matchesLanguage(language)
	}
	
	override fun createSettings(): NoSettings {
		return NoSettings()
	}
	
	override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
		return object : ImmediateConfigurable {
			override fun createComponent(listener: ChangeListener): JComponent = panel {}
		}
	}
	
	override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector {
		return object : FactoryInlayHintsCollector(editor) {
			override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
				return factory.collect(element, file, editor, sink)
			}
		}
	}
	
	private fun PresentationFactory.collect(element: PsiElement, file: PsiFile, editor: Editor, sink: InlayHintsSink): Boolean {
		val expression = element.toUElementOfType<ULiteralExpression>() ?: return true
		val locationElement = expression.sourcePsi ?: return true
		if(!expression.isI18nProperty()) return true
		//用户语言区域的本地化文本需要置顶
		val properties = expression.getI18nProperties()
			.sortedBy { it.propertiesFile?.locale == Locale.getDefault() }
		val elements = properties.filterIsInstance<PsiElement>().toTypedArray()
		if(properties.isEmpty()) return true
		val propertyName = properties.first().name
		val tooltipText = getI18nMessageTooltipText(properties, propertyName)
		val offset = locationElement.textRange.endOffset
		val presentation =  icon(BreezeIcons.Gutter.I18nMessage)
			.let { withTooltip(tooltipText, it) }
			.let { referenceOnHover(it) { _, _ ->
				var popup = NavigationUtil.getPsiElementPopup(elements, BreezeBundle.message("hint.i18nMessage.popup.title"))
				popup.showInBestPositionFor(editor)
			} }
		val finalPresentation = presentation.toFinalPresentation(this, file, file.project)
		sink.addInlineElement(offset, false, finalPresentation, false)
		return true
	}
	
	private fun InlayPresentation.toFinalPresentation(factory: PresentationFactory, file: PsiFile, project: Project?): InlayPresentation {
		var presentation = factory.roundWithBackgroundAndSmallInset(this)
		if(project == null) return presentation
		presentation = MenuOnClickPresentation(presentation, project) {
			listOf(InlayProviderDisablingAction(name, file.language, project, key), ShowInlayHintsSettings(key))
		}
		return presentation
	}
}