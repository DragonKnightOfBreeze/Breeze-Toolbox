package icu.windea.breeze.toolbox

import com.intellij.codeInspection.i18n.*
import com.intellij.codeInspection.i18n.folding.*
import com.intellij.lang.documentation.*
import com.intellij.lang.properties.*
import com.intellij.lang.properties.psi.*
import com.intellij.openapi.util.*
import com.intellij.openapi.util.text.*
import com.intellij.psi.*
import com.intellij.util.*
import com.jetbrains.rd.util.*
import org.jetbrains.uast.*
import java.util.*
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

//region Intellij Extensions
inline fun StringBuilder.definition(block: StringBuilder.() -> Unit): StringBuilder {
	append(DocumentationMarkup.DEFINITION_START)
	block(this)
	append(DocumentationMarkup.DEFINITION_END)
	return this
}

inline fun StringBuilder.content(block: StringBuilder.() -> Unit): StringBuilder {
	append(DocumentationMarkup.CONTENT_START)
	block(this)
	append(DocumentationMarkup.CONTENT_END)
	return this
}

inline fun StringBuilder.sections(block: StringBuilder.() -> Unit): StringBuilder {
	append(DocumentationMarkup.SECTIONS_START)
	block(this)
	append(DocumentationMarkup.SECTIONS_END)
	return this
}

@Suppress("NOTHING_TO_INLINE")
inline fun StringBuilder.section(title: CharSequence, value: CharSequence): StringBuilder {
	append(DocumentationMarkup.SECTION_HEADER_START)
	append(title).append(" ")
	append(DocumentationMarkup.SECTION_SEPARATOR).append("<p>")
	append(value)
	append(DocumentationMarkup.SECTION_END)
	return this
}

inline fun StringBuilder.grayed(block: StringBuilder.() -> Unit): StringBuilder {
	append(DocumentationMarkup.GRAYED_START)
	block(this)
	append(DocumentationMarkup.GRAYED_END)
	return this
}

fun String.escapeXml() = if(this.isEmpty()) "" else StringUtil.escapeXmlEntities(this)

fun PsiElement.firstLeafOrSelf(): PsiElement? {
	var current = this
	while(true) {
		current = current.firstChild ?: return current
	}
}
//endregion

//region Project Extensions
val anonymousString = "<anonymous>"

val locationClass = BreezeBundle::class.java

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
	if(property != null && isValid(property, literal)) return property
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

private fun isValid(property: Property, literal: ULiteralExpression): Boolean {
	if(!property.isValid) return false
	val result = literal.evaluate()
	return if(result !is String) false else StringUtil.unquoteString(result) == property.key
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
							return callSourcePsi?.text ?: "<error>"
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
//endregion