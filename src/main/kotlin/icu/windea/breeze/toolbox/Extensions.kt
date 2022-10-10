package icu.windea.breeze.toolbox

import com.intellij.codeInsight.hint.*
import com.intellij.codeInspection.i18n.*
import com.intellij.codeInspection.i18n.folding.*
import com.intellij.lang.documentation.*
import com.intellij.lang.properties.*
import com.intellij.lang.properties.psi.*
import com.intellij.openapi.util.*
import com.intellij.openapi.util.text.*
import com.intellij.psi.*
import com.intellij.ui.*
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
val ANONYMOUS = "<anonymous>"

val SEPARATOR_COLOR = JBColor.namedColor("GutterTooltip.lineSeparatorColor", HintUtil.INFORMATION_BORDER_COLOR)

val locationClass = BreezeBundle::class.java
//endregion