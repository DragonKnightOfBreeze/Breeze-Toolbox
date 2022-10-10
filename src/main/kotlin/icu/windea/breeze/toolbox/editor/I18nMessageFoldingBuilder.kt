package icu.windea.breeze.toolbox.editor

import com.intellij.lang.*
import com.intellij.lang.folding.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.progress.*
import com.intellij.psi.*
import com.intellij.psi.jsp.*
import com.intellij.psi.util.*
import icu.windea.breeze.toolbox.*
import icu.windea.breeze.toolbox.settings.*
import org.jetbrains.uast.*
import java.util.*

//com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder

/**
 * UAST - 对于第一个参数是本地化文本的key的方法引用，可以折叠并显示对应的本地化文本（覆盖IDEA的默认实现）
 * * 显示实际上会读取到的处理后的本地化文本
 * * 将会列出来自各个本地化文件的本地化文本
 */
class I18nMessageFoldingBuilder : FoldingBuilderEx() {
	override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
		if(root !is PsiFile || quick || !isFoldingOn()) return emptyArray()
		val result = mutableListOf<FoldingDescriptor>()
		val hasJsp = root.viewProvider.languages.any { it is JspLanguage || it is JspxLanguage }
		val visitor = if(hasJsp) {
			object : JavaRecursiveElementWalkingVisitor() {
				override fun visitLiteralExpression(element: PsiLiteralExpression) {
					ProgressManager.checkCanceled()
					checkElement(element, result)
				}
			}
		}else {
			object : PsiRecursiveElementWalkingVisitor() {
				override fun visitElement(element: PsiElement) {
					ProgressManager.checkCanceled()
					checkElement(element, result)
					super.visitElement(element)
				}
			}
		}
		root.accept(visitor)
		return result.toTypedArray()
	}
	
	private fun isFoldingOn(): Boolean {
		return BreezeCodeFoldingSettings.getInstance().collapseI18nMessages
	}
	
	private fun checkElement(element: PsiElement, result: MutableList<FoldingDescriptor>) {
		val literal = element.toUElementOfExpectedTypes(ULiteralExpression::class.java) ?: return
		checkLiteral(literal, result)
	}
	
	private fun checkLiteral(expression: ULiteralExpression, result: MutableList<FoldingDescriptor>) {
		expression.sourcePsi ?: return
		if(!expression.isI18nProperty()) return
		val property = expression.getI18nProperty()
		val set = HashSet<Any>()
		set.add(property ?: PsiModificationTracker.MODIFICATION_COUNT)
		val text = property?.value?.handleTruncatedI18nPropertyValue()
		val parent = expression.uastParent
		when {
			//如果无法解析本地化文本
			text == null -> {
				val elementToFold = expression.sourcePsi ?: return
				val placeholder = expression.evaluateString().toString().quote()
				result.add(createFoldingDescriptor(elementToFold, placeholder, set))
			}
			//如果作为本地化文本的引用的字符串是方法参数，需要将其中的索引占位符替换为上下文表达式占位符
			parent is UCallExpression && parent.isValidI18nStringMethod(expression) -> {
				val callSourcePsi = parent.sourcePsi?: return
				val receiver = parent.receiver
				val receiverSourcePsi = receiver?.sourcePsi
				val elementToFold = receiverSourcePsi?.let { PsiTreeUtil.findCommonParent(callSourcePsi, it) } ?: callSourcePsi
				val placeholder = parent.formatI18nString(text).quote()
				result.add(createFoldingDescriptor(elementToFold, placeholder, set))
			}
			//否则直接使用处理后的本地化文本即可
			else -> {
				val elementToFold = expression.sourcePsi ?: return
				val placeholder = text.quote()
				result.add(createFoldingDescriptor(elementToFold, placeholder, set))
			}
		}
	}
	
	private fun createFoldingDescriptor(elementToFold: PsiElement, placeholder: String, set: HashSet<Any>): FoldingDescriptor {
		val node = Objects.requireNonNull(elementToFold.node)
		val textRange = elementToFold.textRange
		val placeholderText = placeholder
		val element = FoldingDescriptor(node, textRange, null, placeholderText, isFoldingOn(), set)
		return element
	}
	
	override fun getPlaceholderText(node: ASTNode): String? {
		return null
	}
	
	override fun isCollapsedByDefault(node: ASTNode): Boolean {
		return isFoldingOn()
	}
}