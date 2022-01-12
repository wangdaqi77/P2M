package com.p2m.compiler.utils

import com.p2m.compiler.FILE_COMMENT
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.sun.tools.javac.code.Symbol
import java.lang.IllegalStateException
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import kotlin.reflect.KClass
import javax.lang.model.element.ExecutableElement

import javax.lang.model.element.AnnotationMirror

import javax.lang.model.element.TypeElement

import javax.lang.model.type.DeclaredType




/**
 * Returns the package name of a TypeElement.
 */
fun TypeElement.packageName() = enclosingElement.packageName()

fun Elements.getKDoc(element: Element): CodeBlock? {
    val sb = StringBuilder()
    getDocComment(element)
        ?.lineSequence()
        ?.map {
            it.replace(Regex("^\\s*(.*?)"), "")
        }
        ?.forEach {
            sb.append(it)
            sb.append("\n")
        }
    return if (sb.isNotEmpty()) {
        CodeBlock.of(sb.toString())
    } else {
        null
    }
}

private fun Element?.packageName(): String {
    return when (this) {
        is TypeElement -> packageName()
        is PackageElement -> qualifiedName.toString()
        else -> this?.enclosingElement?.packageName() ?: ""
    }
}

// to address kotlin internal method try to remove `$module_name_build_variant` from element info.
// ex: showCamera$sample_kotlin_debug → showCamera
internal fun String.trimDollarIfNeeded(): String {
    val index = indexOf("$")
    return if (index == -1) this else substring(0, index)
}

/**
 * Returns the simple name of an Element as a string.
 */
fun Element.simpleString() = this.simpleName.toString().trimDollarIfNeeded()

/**
 * ex: com.android.String.Inner → String.Inner
 */
fun TypeElement.simpleNames() = this.qualifiedName.removePrefix("${this.packageName()}.")

private fun TypeElement.className() = ClassName(packageName(), simpleNames().split("."))

fun Element.className(): ClassName {
    check(this is TypeElement) {
        "must is TypeElement"
    }
    return this.className()
}

/**
 * Returns the simple name of a TypeMirror as a string.
 */
fun TypeMirror.simpleString(): String {
    val toString: String = this.toString()
    val indexOfDot: Int = toString.lastIndexOf('.')
    return if (indexOfDot == -1) toString else toString.substring(indexOfDot + 1)
}

/**
 * Returns whether or not an Element is annotated with the provided Annotation class.
 */
fun <A : Annotation> Element.hasAnnotation(annotationType: Class<A>): Boolean =
    this.getAnnotation(annotationType) != null

/**
 * Returns whether a variable is nullable by inspecting its annotations.
 */
fun VariableElement.isNullable(): Boolean =
    this.annotationMirrors
        .map { it.annotationType.simpleString() }
        .toList()
        .contains("Nullable")

/**
 * Maps a variable to its TypeName, applying necessary transformations
 * for Java primitive types & mirroring the variable's nullability settings.
 */
@Suppress("DEPRECATION")
fun VariableElement.asPreparedType(): TypeName =
    this.asType()
        .asTypeName()
        .checkStringType()
        .checkParameterStringType()
        // .mapToNullableTypeIf(this.isNullable())



/**
 * Returns a list of enclosed elements annotated with the provided Annotations.
 */
fun <A : Annotation> Element.childElementsAnnotatedWith(annotationClass: Class<A>): List<ExecutableElement> =
    this.enclosedElements
        .filter { it.hasAnnotation(annotationClass) }
        .map { it as ExecutableElement }

fun FileSpec.Builder.addProperties(properties: List<PropertySpec>): FileSpec.Builder {
    properties.forEach { addProperty(it) }
    return this
}

fun FileSpec.Builder.addFunctions(functions: List<FunSpec>): FileSpec.Builder {
    functions.forEach { addFunction(it) }
    return this
}

fun FileSpec.Builder.addTypes(types: List<TypeSpec>): FileSpec.Builder {
    types.forEach { addType(it) }
    return this
}

/**
 * To avoid KotlinPoet bug that returns java.lang.String when type name is kotlin.String.
 * This method should be removed after addressing on KotlinPoet side.
 */
fun TypeName.checkStringType() =
    if (this.toString() == "java.lang.String") ClassName("kotlin", "String") else this

/**
 * Convert [java.lang.String] to [kotlin.String] in a parameter.
 * ref: https://github.com/permissions-dispatcher/PermissionsDispatcher/issues/427
 */
fun TypeName.checkParameterStringType(): TypeName {
    if (this is ParameterizedTypeName) {
        val typeArguments = this.typeArguments.map { it.checkStringType() }
        return this.rawType.parameterizedBy(*typeArguments.toTypedArray())
    }
    return this
}

/**
 * (a:A,b:B) -> a,b
 */
fun List<Symbol.VarSymbol>?.convertRealParamsForJava(): String {
    if (this.isNullOrEmpty()) return ""

    val sb = StringBuilder()
    this.forEachIndexed { index, varSymbol ->
        sb.append(varSymbol.name)
        if (index < this.size - 1) {
            sb.append(", ")
        }
    }
    return sb.toString()
}

/**
 * (a:A,b:B) -> a,b
 */
fun List<ParameterSpec>?.convertRealParamsForKotlin(): String {
    if (this.isNullOrEmpty()) return ""

    val sb = StringBuilder()
    this.forEachIndexed { index, parameterSpec ->
        sb.append(parameterSpec.name)
        if (index < this.size - 1) {
            sb.append(", ")
        }
    }
    return sb.toString()
}

fun FileSpec.Builder.addFileComment() = this.apply {
    addComment(FILE_COMMENT)
}

fun TypeElement.checkKotlinClass() {
    val metadata = getAnnotation(Metadata::class.java)
    check(metadata != null) {
        """
            Only supports kotlin class, please check class of $qualifiedName
        """.trimIndent()
    }
}

fun TypeElement.checkNotInnerClassForAnnotation(aClass: KClass<*>) {
    check(enclosingElement is Symbol.PackageSymbol) {
        """
            Class annotated by ${aClass.java.canonicalName} not support inner class, please check the class of ${qualifiedName}.
        """.trimIndent()
    }
}

fun TypeElement.checkNotHasInterfaceClassForAnnotation(aClass: KClass<*>) {

    check(interfaces.isEmpty()) {
        """
            Class annotated by ${aClass.java.simpleName} not support extends interface, please check the class of ${qualifiedName}.
        """.trimIndent()
    }
}

inline fun <reified T : Annotation> RoundEnvironment.getSingleTypeElementAnnotatedWith(logger: Logger, moduleName: String, clazz: Class<T>): Element? {
    val elements = getElementsAnnotatedWith(clazz)
    if (elements.size > 1) {
        logger.error(
            IllegalStateException("""
                Only one class use ${clazz.canonicalName} is allowed in $moduleName.
            """.trimIndent())
        )
    }
    if (elements.isEmpty()) return null
    return elements.first()
}

fun <T> findAnnotationValue(
    element: Element,
    annotationClass: String,
    valueName: String,
    expectedType: Class<T>
): T? {
    var ret: T? = null
    for (annotationMirror in element.annotationMirrors) {
        val annotationType = annotationMirror.annotationType
        val annotationElement = annotationType.asElement() as TypeElement
        if (annotationElement.qualifiedName.contentEquals(annotationClass)) {
            ret = extractValue(annotationMirror, valueName, expectedType)
            break
        }
    }
    return ret
}

private fun <T> extractValue(
    annotationMirror: AnnotationMirror,
    valueName: String,
    expectedType: Class<T>
): T? {
    val elementValues: Map<ExecutableElement, AnnotationValue> = HashMap(annotationMirror.elementValues)
    for ((executableElement, annotationValue) in elementValues) {
        if (executableElement.simpleName.contentEquals(valueName)) {
            val value = annotationValue.value
            return expectedType.cast(value)
        }
    }
    return null
}