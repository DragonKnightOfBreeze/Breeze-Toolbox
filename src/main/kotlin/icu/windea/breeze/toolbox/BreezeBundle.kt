package icu.windea.breeze.toolbox

import com.intellij.*
import org.jetbrains.annotations.PropertyKey

private const val bundleName = "messages.BreezeBundle"

object BreezeBundle: DynamicBundle(bundleName){
	fun message(@PropertyKey(resourceBundle = bundleName) key: String, vararg params: Any): String {
		return getMessage(key, *params)
	}
}