package icu.windea.breeze.toolbox

import com.intellij.codeInspection.i18n.*
import com.intellij.codeInspection.i18n.folding.*
import com.intellij.lang.properties.*
import com.intellij.lang.properties.psi.*
import com.intellij.openapi.util.*
import com.intellij.openapi.util.text.*
import com.intellij.psi.*
import com.intellij.ui.*
import org.jetbrains.uast.*

fun String.handleTruncatedI18nPropertyValue(): String {
	val index = indexOf("\\n")
	val suffix = if(index == -1) "" else "..."
	val truncatedValue = if(index == -1) this else substring(0, index)
	return StringUtil.escapeXmlEntities(truncatedValue.replace("\\\n", "")) + suffix
}

fun String.handleHtmlI18nPropertyValue(): String {
	return StringUtil.escapeXmlEntities(replace("\\\n", "")).replace("\\n", "<br>")
}

private val NULL_PROPERTY: IProperty? = PropertyFoldingBuilder.NULL

//com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder.isI18nProperty(org.jetbrains.uast.ULiteralExpression)
fun ULiteralExpression.isI18nProperty(): Boolean {
	if(!isString) return false
	val sourcePsi = sourcePsi ?: return false
	val cachedProperty = sourcePsi.getUserData(BreezeKeys.i18nPropertyKey)
	if(cachedProperty == NULL_PROPERTY) return false
	if(cachedProperty != null) return true
	val isI18n = JavaI18nUtil.mustBePropertyKey(this, null)
	if(!isI18n) sourcePsi.putUserData(BreezeKeys.i18nPropertyKey, PropertyFoldingBuilder.NULL)
	return isI18n
}

//com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder.getI18nProperty
fun ULiteralExpression.getI18nProperty(): IProperty? {
	val literal = this
	val sourcePsi = sourcePsi ?: return null
	val property = sourcePsi.getUserData(BreezeKeys.i18nPropertyKey) as Property?
	if(property === PropertyFoldingBuilder.NULL) return null
	if(property != null && literal.isValidI18nKey(property)) return property
	if(literal.isI18nProperty()) {
		val references = literal.injectedReferences
		for(reference in references) {
			if(reference is PsiPolyVariantReference) {
				val results = reference.multiResolve(false)
				for(result in results) {
					val element = result.element
					if(element is IProperty) {
						sourcePsi.putUserData(BreezeKeys.i18nPropertyKey, element as IProperty)
						return element
					}
				}
			} else {
				val element = reference.resolve()
				if(element is IProperty) {
					sourcePsi.putUserData(BreezeKeys.i18nPropertyKey, element as IProperty)
					return element
				}
			}
		}
	}
	return null
}

//fileName -> property
fun ULiteralExpression.getI18nProperties(): Set<IProperty> {
	val literal = this
	if(literal.isI18nProperty()) {
		val properties = mutableSetOf<IProperty>()
		val references = literal.injectedReferences
		for(reference in references) {
			if(reference is PsiPolyVariantReference) {
				val results = reference.multiResolve(false)
				for(result in results) {
					val element = result.element
					if(element is IProperty) {
						properties.add(element)
					}
				}
			} else {
				val element = reference.resolve()
				if(element is IProperty) {
					properties.add(element)
				}
			}
		}
		return properties
	}
	return emptySet()
}

fun ULiteralExpression.isValidI18nKey(property: Property): Boolean {
	if(!property.isValid) return false
	val result = evaluate()
	return if(result !is String) false else StringUtil.unquoteString(result) == property.key
}

//com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder.checkLiteral
fun UCallExpression.isValidI18nStringMethod(literalExpression: ULiteralExpression): Boolean {
	val sourceLiteralExpression = literalExpression.sourcePsi ?: return false
	val args = valueArguments
	if(args[0].sourcePsi == sourceLiteralExpression) {
		sourcePsi ?: return false
		val count = JavaI18nUtil.getPropertyValueParamsMaxCount(literalExpression)
		if(args.size == 1 + count) return true //matched message argument size -> ok enoungth
		//if(args.size == 1 + count) {
		//	val ok = args.drop(1).all { arg -> arg is UReferenceExpression || arg.evaluate() != null }
		//	if(ok) return true
		//}
	}
	return false
}

//com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder.format
fun UCallExpression.formatI18nString(text: String): String {
	val args = valueArguments
	//val callSourcePsi = sourcePsi
	if(args.isNotEmpty()) {
		val literalExpression = args[0]
		if(literalExpression is ULiteralExpression && literalExpression.isI18nProperty()) {
			val count = JavaI18nUtil.getPropertyValueParamsMaxCount(literalExpression)
			if(args.size == count + 1) {
				var resultText = text
				val replacementPositions = mutableListOf<Couple<Int>>()
				for(i in 1..count) {
					val arg = args[i]
					var value = arg.evaluate()
					if(value == null) {
						if(arg is UReferenceExpression) {
							val sourcePsi = arg.sourcePsi
							value = "{${sourcePsi?.text ?: "<error>"}}"
						} else {
							value = "{${sourcePsi?.text ?: "<error>"}}"
							//return callSourcePsi?.text ?: "<error>"
						}
					}
					resultText = replacePlaceholder(text, "{" + (i - 1) + "}", value.toString(), replacementPositions)
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

//TODO 考虑自动换行过长的本地化文本？
fun getI18nMessageTooltip(properties: List<IProperty>, propertyName: String?): String {
	var appendHr = false
	return buildString {
		append("<html><body>")
		for(property in properties) {
			val value = property.value ?: return "# unresolved"
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
		append("</body></html>")
	}
}