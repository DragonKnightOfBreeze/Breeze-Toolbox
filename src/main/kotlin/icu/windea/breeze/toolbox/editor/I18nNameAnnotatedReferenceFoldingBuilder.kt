package icu.windea.breeze.toolbox.editor

import com.intellij.lang.*
import com.intellij.lang.folding.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.progress.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import icu.windea.breeze.toolbox.*
import icu.windea.breeze.toolbox.settings.*
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.*
import java.util.*

//com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder

//TODO 可自定义作用对象和标记注解

/**
 * UAST - 对于类型定义上注有`@I18nName`的变量引用，可以折叠并显示对应的本地化文本
 * * 显示由`@I18nName`的`value`属性的值的对应名称的该类型的属性指定的本地化文本
 * * 显示实际上会读取到的处理后的本地化文本
 */
class I18nNameAnnotatedReferenceFoldingBuilder : FoldingBuilderEx() {
	override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
		if(root !is PsiFile || quick || !isFoldingOn()) return emptyArray()
		val result = mutableListOf<FoldingDescriptor>()
		val visitor = object : PsiRecursiveElementWalkingVisitor() {
			override fun visitElement(element: PsiElement) {
				ProgressManager.checkCanceled()
				checkElement(element, result)
				super.visitElement(element)
			}
		}
		root.accept(visitor)
		return result.toTypedArray()
	}
	
	private fun isFoldingOn(): Boolean {
		return BreezeCodeFoldingSettings.getInstance().collapseI18nNameAnnotated
	}
	
	private fun checkElement(element: PsiElement, result: MutableList<FoldingDescriptor>) {
		val expression = element.toUElementOfExpectedTypes(UQualifiedReferenceExpression::class.java) ?: return
		val expressionParent = expression.uastParent ?: return
		if(expressionParent is UReferenceExpression || expressionParent is UImportStatement) return //跳过要忽略的referenceExpression
		val expressionType = expression.getExpressionType() ?: return //得到expression的类型
		val expressionClass = PsiTypesUtil.getPsiClass(expressionType) ?: return //得到expression的返回类型的class
		val annotation = getI18nNameAnnotation(expressionClass) ?: return //得到@I18nName
		val annotationValue = annotation.findAttributeValue("value") ?: return //得到@I18nName的value属性的值
		val propertyName = annotationValue.text.unquote()
		val resolvedExpression = expression.resolve() ?: return
		val literal = getI18nNameLiteralExpression(resolvedExpression, propertyName) ?: return
		val elementToFold = expression.sourcePsi ?: return
		checkLiteral(literal, result, elementToFold)
	}
	
	private fun getI18nNameAnnotation(expressionClass: PsiClass): PsiAnnotation? {
		return expressionClass.annotations.find {
			it.qualifiedName.let { qn -> qn != null && qn.substringAfterLast('.', qn) == "I18nName" }
		}
	}
	
	private fun getI18nNameLiteralExpression(resolvedExpression: PsiElement, propertyName: String): ULiteralExpression? {
		var result: ULiteralExpression? = null
		val expressionToVisit = when {
			resolvedExpression is KtLightMember<*> -> resolvedExpression.kotlinOrigin
			else -> resolvedExpression.originalElement //未验证，可能不需要使用原始元素
		} ?: return null
		//从resolvedExpression中找到name属性对应的expression中的第一个literalExpression，视为i18nKey
		expressionToVisit.accept(object : PsiRecursiveElementWalkingVisitor() {
			override fun visitElement(element: PsiElement) {
				val binaryExpression = element.toUElementOfExpectedTypes(UBinaryExpression::class.java)
				if(binaryExpression != null) {
					if((binaryExpression.leftOperand as? USimpleNameReferenceExpression)?.identifier == propertyName) {
						binaryExpression.rightOperand.accept(object : AbstractUastVisitor() {
							override fun visitLiteralExpression(node: ULiteralExpression): Boolean {
								result = node
								return true
							}
						})
					}
				}
				super.visitElement(element)
			}
		})
		return result
	}
	
	private fun checkLiteral(expression: ULiteralExpression, result: MutableList<FoldingDescriptor>, elementToFold: PsiElement) {
		expression.sourcePsi ?: return
		if(!expression.isI18nProperty()) return
		val property = expression.getI18nProperty()
		val set = HashSet<Any>()
		set.add(property ?: PsiModificationTracker.MODIFICATION_COUNT)
		val text = property?.value?.handleHtmlI18nPropertyValue()
		val parent = expression.uastParent
		when {
			//如果无法解析本地化文本
			text == null -> {
				val placeholder = expression.evaluateString().toString().quote()
				result.add(createFoldingDescriptor(elementToFold, placeholder, set))
			}
			//如果作为本地化文本的引用的字符串是方法参数，需要将其中的索引占位符替换为上下文表达式占位符
			parent is UCallExpression && parent.isValidI18nStringMethod(expression) -> {
				val placeholder = parent.formatI18nString(text).quote()
				result.add(createFoldingDescriptor(elementToFold, placeholder, set))
			}
			//否则直接使用处理后的本地化文本即可
			else -> {
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

