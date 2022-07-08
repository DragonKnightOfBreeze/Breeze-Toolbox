package icu.windea.breeze.toolbox

import com.intellij.codeInspection.i18n.*
import com.intellij.codeInspection.i18n.folding.*
import com.intellij.lang.properties.*
import com.intellij.openapi.util.*
import com.intellij.openapi.util.text.*
import com.intellij.util.*
import org.jetbrains.annotations.*
import org.jetbrains.uast.*
import javax.swing.*

//region Stdlib Extensions
fun String.quote(): String {
	return '"' + this + '"'
}

fun String.unquote(): String {
	return if(length >= 2 && startsWith('"') && endsWith('"')) substring(1, length - 1) else this
}

fun Icon.resize(width: Int, height: Int = width): Icon {
	return IconUtil.toSize(this, width, height)
}
//endregion

//region Project Extensions
val locationClass = BreezeBundle::class.java

fun String.handleTruncatedI18nPropertyValue():String{
	val index = indexOf("\\n")
	val suffix = if(index == -1) "" else "..."
	val truncatedValue = if(index == -1) this else substring(0, index)
	return StringUtil.escapeXmlEntities(truncatedValue.replace("\\\n","")) + suffix
}

fun String.handleHtmlI18nPropertyValue(): String {
	return StringUtil.escapeXmlEntities(replace("\\\n", "")).replace("\\n", "<br>")
}

fun ULiteralExpression.isI18nProperty(): Boolean {
	return PropertyFoldingBuilder.isI18nProperty(this)
}

fun ULiteralExpression.getI18nProperty(): IProperty? {
	return PropertyFoldingBuilder.getI18nProperty(this)
}

fun UCallExpression.isValidI18nStringMethod(literalExpression: ULiteralExpression): Boolean {
	val sourceLiteralExpression = literalExpression.sourcePsi ?: return false
	val args = valueArguments
	if(args[0].sourcePsi == sourceLiteralExpression) {
		sourcePsi ?: return false
		val count = JavaI18nUtil.getPropertyValueParamsMaxCount(literalExpression)
		if(args.size == 1 + count) {
			val ok = args.all { arg -> arg.evaluate() != null || arg is UReferenceExpression }
			if(ok) return true
		}
	}
	return false
}

//com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder.format
fun UCallExpression.formatI18nString(text: String): String {
	val args = valueArguments
	val callSourcePsi = sourcePsi
	if(args.isNotEmpty()){
		val literalExpression = args[0]
		if(literalExpression is ULiteralExpression && literalExpression.isI18nProperty()){
			val count = JavaI18nUtil.getPropertyValueParamsMaxCount(literalExpression)
			if(args.size == count + 1){
				var resultText = text
				val replacementPositions = mutableListOf<Couple<Int>>()
				for(i in 1..count){
					val arg = args[i]
					var value = arg.evaluate()
					if(value == null){
						if(arg is UReferenceExpression){
							val sourcePsi = arg.sourcePsi
							value = "{${sourcePsi?.text?:"<error>"}}"
						}else{
							return callSourcePsi?.text ?: "<error>"
						}
					}
					resultText = replacePlaceholder(text, "{" + (i-1) + "}", value.toString(), replacementPositions)
				}
				return resultText
			}
		}
	}
	return text
}

//com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder.replacePlaceholder
private fun replacePlaceholder(text: String, placeholder: String, replacement: String, replacementPositions: MutableList<Couple<Int>>): String {
	var resultText = text
	var curPos = 0
	do {
		val placeholderPos = resultText.indexOf(placeholder, curPos)
		if(placeholderPos < 0) break
		resultText = resultText.substring(0, placeholderPos) + replacement + resultText.substring(placeholderPos + placeholder.length)
		val it = replacementPositions.listIterator()
		var diff = 0
		while(it.hasNext()) {
			val next = it.next()
			if(next.second > placeholderPos) {
				it.previous()
				break
			}
			diff = next.second - next.first
		}
		it.add(Couple.of(placeholderPos - diff, placeholderPos))
		it.add(Couple.of(placeholderPos - diff + placeholder.length, placeholderPos + replacement.length))
		while(it.hasNext()) {
			val next = it.next()
			it.set(Couple.of(next.first, next.second + replacement.length - placeholder.length))
		}
		curPos = placeholderPos + replacement.length
	} while(true)
	return resultText
}
//endregion