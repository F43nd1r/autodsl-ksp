package com.faendir.kotlin.autodsl.parameter

import com.cesarferreira.pluralize.singularize
import com.cesarferreira.pluralize.utils.Plurality
import com.faendir.kotlin.autodsl.AutoDsl
import com.faendir.kotlin.autodsl.AutoDslSingular
import com.faendir.kotlin.autodsl.SourceInfoResolver
import com.faendir.kotlin.autodsl.toRawType
import com.squareup.kotlinpoet.asClassName
import kotlin.reflect.KClass

class ParameterFactory<A, T : A, C : A, P : A>(private val resolver: SourceInfoResolver<A, T, C, P>) {
    private val setType = Set::class.asClassName()
    private val listType = List::class.asClassName()
    private val collectionType = Collection::class.asClassName()
    private val iterableType = Iterable::class.asClassName()

    fun getParameters(constructor: C): List<Parameter> = resolver.run {
        return constructor.getParameters().withIndex().map { (index, parameter) ->
            val type = parameter.getTypeName()
            val rawType = type.toRawType()
            when {
                rawType == setType && parameter.hasAnnotatedTypeArgument(AutoDsl::class) -> {
                    NestedDslSetParameter(type, parameter.getName(), parameter.hasDefault(), findSingular(parameter, index), index)
                }
                (rawType == listType || rawType == collectionType || rawType == iterableType) && parameter.hasAnnotatedTypeArgument(AutoDsl::class) -> {
                    NestedDslListParameter(type, parameter.getName(), parameter.hasDefault(), findSingular(parameter, index), index)
                }
                parameter.getTypeDeclaration()?.hasAnnotation(AutoDsl::class) == true -> {
                    NestedDslParameter(type, parameter.getName(), parameter.hasDefault(), index)
                }
                else -> {
                    StandardParameter(type, parameter.getName(), parameter.hasDefault(), index)
                }
            }
        }
    }

    private fun P.hasAnnotatedTypeArgument(annotation: KClass<out Annotation>): Boolean = resolver.run {
        return getTypeArguments().firstOrNull()?.hasAnnotation(annotation) ?: false
    }

    private fun findSingular(parameter: P, index: Int): String = resolver.run {
        when {
            parameter.hasAnnotation(AutoDslSingular::class) -> parameter.getAnnotationProperty(AutoDslSingular::class, AutoDslSingular::value)
            parameter.getName().length < 3 -> parameter.getName()
            else -> parameter.getName().singularize(Plurality.CouldBeEither)
        } ?: "var$index"
    }
}