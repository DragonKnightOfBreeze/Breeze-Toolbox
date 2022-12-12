package icu.windea.breeze.toolbox.codeInsight.hints

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.uast.UastMetaLanguage

@Suppress("UnstableApiUsage")
class I18nMessageHintsProviderFactory: InlayHintsProviderFactory {
	override fun getProvidersInfo(): List<ProviderInfo<out Any>> {
		return I18nMessageHintsProvider().let { p -> getLanguages().map { l -> ProviderInfo(l, p) } }
	}

	override fun getProvidersInfoForLanguage(language: Language): List<InlayHintsProvider<out Any>> {
		return I18nMessageHintsProvider().let { listOf(it) }
	}
	
	override fun getLanguages(): Iterable<Language> {
		return UastMetaLanguage.getRegisteredLanguages()
	}
}