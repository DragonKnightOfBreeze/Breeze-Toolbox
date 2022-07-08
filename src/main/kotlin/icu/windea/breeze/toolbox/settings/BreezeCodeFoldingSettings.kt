package icu.windea.breeze.toolbox.settings

import com.intellij.openapi.application.*
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.*

@State(name = "BreezeCodeFoldingSettings", storages = [Storage("editor.xml")])
class BreezeCodeFoldingSettings : PersistentStateComponent<BreezeCodeFoldingSettings>{
	companion object{
		fun getInstance(): BreezeCodeFoldingSettings {
			return ApplicationManager.getApplication().getService(BreezeCodeFoldingSettings::class.java)
		}
	}
	
	@JvmField var collapseI18nMessages = true
	@JvmField var collapseI18nNameAnnotated = true
	
	override fun getState(): BreezeCodeFoldingSettings {
		return this
	}
	
	override fun loadState(state: BreezeCodeFoldingSettings) {
		XmlSerializerUtil.copyBean(state, this)
	}
}