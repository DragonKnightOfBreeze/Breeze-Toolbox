package icu.windea.breeze.toolbox.codeInsight.hints

import com.intellij.codeInsight.hint.*
import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.*
import com.intellij.codeInsight.navigation.*
import com.intellij.lang.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.*
import com.intellij.uast.*
import com.intellij.ui.*
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.*
import icons.*
import icu.windea.breeze.toolbox.*
import org.jetbrains.kotlin.idea.codeInsight.hints.*
import org.jetbrains.uast.*
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.*

/**
 * UAST - 对于引用到本地化文本的地方，在消息码之后加上嵌入图标，可以鼠标悬浮显示对应的本地化文本，可以鼠标点击导航到声明处
 * * 显示实际上会读取到的处理后的本地化文本
 */
@Suppress("UnstableApiUsage")
class I18nMessageHintsProvider : InlayHintsProvider<I18nMessageHintsProvider.Settings> {
	companion object {
		private val settingsKey = SettingsKey<Settings>("breeze.toolbox.i18nMessage")
	}
	
	data class Settings(
		var pinTooltip: Boolean = true
	)
	
	private val uastMetaLanguage = UastMetaLanguage.findInstance(UastMetaLanguage::class.java)
	
	override val key: SettingsKey<Settings> = settingsKey
	override val name: String = BreezeBundle.message("hint.i18nMessage.name")
	override val description: String = BreezeBundle.message("hint.i18nMessage.description")
	override val previewText: String? = null
	
	override fun isLanguageSupported(language: Language): Boolean {
		return uastMetaLanguage.matchesLanguage(language)
	}
	
	override fun createSettings(): Settings {
		return Settings()
	}
	
	override fun createConfigurable(settings: Settings): ImmediateConfigurable {
		return object : ImmediateConfigurable {
			override fun createComponent(listener: ChangeListener): JComponent = panel {
				row {
					checkBox(BreezeBundle.message("hint.i18nMessage.settings.pinTooltip"))
						.bindSelected(settings::pinTooltip)
				}
			}
		}
	}
	
	override fun getCollectorFor(file: PsiFile, editor: Editor, settings: Settings, sink: InlayHintsSink): InlayHintsCollector {
		val offsets = mutableSetOf<Int>()
		return object : FactoryInlayHintsCollector(editor) {
			override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
				return factory.collect(element, file, editor, settings, sink, offsets)
			}
		}
	}
	
	private fun PresentationFactory.collect(element: PsiElement, file: PsiFile, editor: Editor, settings: Settings, sink: InlayHintsSink, offsets: MutableSet<Int>): Boolean {
		val expression = element.toUElementOfType<ULiteralExpression>() ?: return true
		val sourcePsi = expression.sourcePsi ?: return true
		val locationElement = getLocationElement(sourcePsi) ?: return true
		val offset = locationElement.textRange.endOffset
		if(!offsets.add(offset)) return true //不要在同一偏移处重复添加内嵌提示
		if(!expression.isI18nProperty()) return true
		//用户语言区域的本地化文本需要置顶
		val properties = expression.getI18nProperties()
			.sortedBy { it.propertiesFile?.locale == Locale.getDefault() }
		if(properties.isEmpty()) return true
		val elements = properties.filterIsInstance<PsiElement>().toTypedArray()
		val propertyName = properties.first().name
		val tooltip = getI18nMessageTooltip(properties, propertyName)
		var hint: LightweightHint? = null
		val project = file.project
		val presentation = icon(BreezeIcons.Gutter.I18nMessage)
			.let {
				onHover(it, object : InlayPresentationFactory.HoverListener {
					override fun onHover(event: MouseEvent, translated: Point) {
						if(hint?.isVisible != true) {
							//使用自定义的方法显示提示 - 可以保持显示，以便进行参照和复制提示文本
							hint = showTooltip(editor, settings, event, tooltip)
						}
					}
					
					override fun onHoverFinished() {
				
					}
				})
			}
			.let {
				referenceOnHover(it) { _, _ ->
					val popup = NavigationUtil.getPsiElementPopup(elements, BreezeBundle.message("hint.i18nMessage.popup.title"))
					popup.showInBestPositionFor(editor)
				}
			}
			.let { roundWithBackgroundAndSmallInset(it) }
			.let {
				MenuOnClickPresentation(it, project) {
					listOf(
						PinI18nMessageTooltipAction(settings),
						UnpinI18nMessageTooltipAction(settings),
						InlayProviderDisablingAction(name, file.language, project, key),
						ShowInlayHintsSettings(key)
					)
				}
			}
		sink.addInlineElement(offset, false, presentation, false)
		return true
	}
	
	private fun getLocationElement(sourcePsi: PsiElement): PsiElement? {
		if(sourcePsi is LeafPsiElement) return null //sourcePsi不应该是叶子元素
		val text = sourcePsi.text
		val c1 = text.getOrNull(text.length - 1)
		val c2 = text.getOrNull(text.length - 2)
		if((c1 == '"' || c1 == '\'') && c2 != '\\') {
			return sourcePsi //sourcePsi包含用于括起字符串的引号，直接返回
		} else {
			val nextSibling = sourcePsi.nextSibling ?: return null //不期望的结果
			if(nextSibling !is LeafPsiElement) return null //用于括起字符串的引号应当是叶子元素
			if(nextSibling.nextSibling != null) return null //用于括起字符串的引号应当没有下一个兄弟元素
			val quoteText = nextSibling.text
			if(quoteText.all { it == '"' } || quoteText.all { it == '\'' }) return sourcePsi.parent //nextSibling是期望的引号元素
			return null
		}
	}
	
	private fun showTooltip(editor: Editor, settings: Settings, e: MouseEvent, @NlsContexts.HintText text: String): LightweightHint {
		val hint = run {
			val label = HintUtil.createInformationLabel(text)
			label.border = JBUI.Borders.empty(6, 6, 5, 6)
			LightweightHint(label)
		}
		
		val constraint = HintManager.ABOVE
		
		val point = run {
			val pointOnEditor = locationAt(e, editor.contentComponent)
			val p = HintManagerImpl.getHintPosition(hint, editor, editor.xyToVisualPosition(pointOnEditor), constraint)
			p.x = e.xOnScreen - editor.contentComponent.topLevelAncestor.locationOnScreen.x
			p
		}
		
		val hintHint = HintManagerImpl.createHintHint(editor, point, hint, constraint)
		hintHint.isShowImmediately = true
		if(settings.pinTooltip){
			hintHint.isExplicitClose = true
			hintHint.isContentActive = true
		}
		val flags = HintManager.HIDE_BY_ESCAPE or HintManager.UPDATE_BY_SCROLLING
		HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, point, flags, 0, false, hintHint)
		return hint
	}
	
	private fun locationAt(e: MouseEvent, component: Component): Point {
		val pointOnScreen = component.locationOnScreen
		return Point(e.xOnScreen - pointOnScreen.x, e.yOnScreen - pointOnScreen.y)
	}
	
	class PinI18nMessageTooltipAction(
		private val settings: Settings
	) : AnAction(BreezeBundle.message("action.hints.pinI18nMessageTooltip.text")) {
		override fun getActionUpdateThread() = ActionUpdateThread.BGT
		
		override fun update(e: AnActionEvent) {
			e.presentation.isEnabledAndVisible = !settings.pinTooltip
		}
		
		override fun actionPerformed(e: AnActionEvent) {
			settings.pinTooltip = true
		}
	}
	
	class UnpinI18nMessageTooltipAction(
		private val settings: Settings
	) : AnAction(BreezeBundle.message("action.hints.unpinI18nMessageTooltip.text")) {
		override fun getActionUpdateThread() = ActionUpdateThread.EDT
		
		override fun update(e: AnActionEvent) {
			e.presentation.isEnabledAndVisible = settings.pinTooltip
		}
		
		override fun actionPerformed(e: AnActionEvent) {
			settings.pinTooltip = false
		}
	}
}