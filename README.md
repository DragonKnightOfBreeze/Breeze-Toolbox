# 概述

IDEA插件：自用工具箱。

# 功能

* [X] UAST - 对于第一个参数是本地化文本的key的方法引用，可以折叠并显示对应的本地化文本（覆盖IDEA的默认实现）
  * 显示实际上会读取到的处理后的本地化文本
  * 将会列出来自各个本地化文件的本地化文本
  * 实现：`icu.windea.btb.editor.I18nMessageFoldingBuilder`
  * 参考：`com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder`
* [X] UAST - 对于类型定义上注有`@I18nName`的变量引用，可以折叠并显示对应的本地化文本
  * 显示由`@I18nName`的`value`属性的值的对应名称的该类型的属性指定的本地化文本
  * 显示实际上会读取到的处理后的本地化文本
  * 实现：`icu.windea.btb.editor.I18nNameAnnotatedReferenceFoldingBuilder`
  * 参考：`com.intellij.codeInspection.i18n.folding.PropertyFoldingBuilder`
* [X] ~~UAST - 对于引用到本地化文本的地方，加上装订线图标，可以鼠标悬浮显示对应的本地化文本，可以鼠标点击导航到声明处~~
  * 显示实际上会读取到的处理后的本地化文本
  * 实现：`icu.windea.btb.editor.I18nMessageLineMarkerProvider`
* [X] UAST - 对于引用到本地化文本的地方，在消息码之后加上嵌入图标，可以鼠标悬浮显示对应的本地化文本，可以鼠标点击导航到声明处
  * 显示实际上会读取到的处理后的本地化文本
  * 实现：`icu.windea.breeze.toolbox.codeInsight.hints.I18nMessageHintsProvider`
* [X] Properties - 渲染属性的文档注释（覆盖IDEA的默认实现）
  * 显示实际上会读取到的处理后的本地化文本
  * 实现：`icu.windea.btb.editor.PropertiesDocumentationProvider`
* [X] Properties - 在本地化文本中换行时不自动缩进（覆盖IDEA的默认实现）
  * 实现：`icu.windea.btb.editor.EnterInPropertiesFileHandler`