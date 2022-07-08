package icu.windea.breeze.toolbox.settings

import com.intellij.application.options.editor.*
import com.intellij.openapi.options.*
import icu.windea.breeze.toolbox.*

class BreezeCodeFoldingOptionsProvider : BeanConfigurable<BreezeCodeFoldingSettings>(
	BreezeCodeFoldingSettings.getInstance(), BreezeBundle.getMessage("title")
), CodeFoldingOptionsProvider {
	init {
		val settings = instance!!
		checkBox(BreezeBundle.getMessage("folding.i18nNameAnnotated"), settings::collapseI18nNameAnnotated)
		checkBox(BreezeBundle.getMessage("folding.i18nMessages"), settings::collapseI18nMessages)
	}
}