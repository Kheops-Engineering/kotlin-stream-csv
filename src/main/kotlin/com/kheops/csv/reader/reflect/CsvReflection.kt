package com.kheops.csv.reader.reflect

import com.kheops.csv.CsvProperty
import com.kheops.csv.reader.Test
import java.lang.reflect.Field
import java.util.*
import kotlin.collections.HashMap
import kotlin.jvm.internal.Reflection
import kotlin.reflect.jvm.kotlinProperty

class CsvReflectionCreator<T>(private val target: Class<T>) {
    private val fieldTranslation: Map<String, InstantiationField>
    private val ignoreCaseTokenId: String = UUID.randomUUID().toString()

    init {
        fieldTranslation = target.declaredFields.map { createFieldMapping(it) }.toMap()
    }

    private fun createFieldMapping(field: Field): Pair<String, InstantiationField> {
        val annotation =
            field.getAnnotation(CsvProperty::class.java) ?: return field.name to toInstantiationField(field)
        if (annotation.ignoreCase) {
            return toIgnoreCaseToken(annotation.name) to toInstantiationField(field)
        }
        return annotation.name to toInstantiationField(field)
    }

    private fun toInstantiationField(field: Field): InstantiationField {
        Reflection.createKotlinClass(Test::class.java)
        return InstantiationField(
            field = field,
            property = field.kotlinProperty,
        )
    }

    private fun toIgnoreCaseToken(value: String): String {
        return ignoreCaseTokenId + value.toUpperCase()
    }

    fun createCsvInstance(csvHeadersValues: Map<String, String?>): InstantiationWithErrors<T> {
        val ignoreCaseCsvHeader = csvHeadersValues.map { toIgnoreCaseToken(it.key) to it.value }.toMap()

        val arguments = HashMap<InstantiationField, String?>()

        fieldTranslation.forEach {
            val targetClassField = it.value
            val csvFieldValue = ignoreCaseCsvHeader[it.key] ?: csvHeadersValues[it.key]
            arguments[targetClassField] = csvFieldValue
        }

        return createInstance(target = target, arguments = arguments)
    }
}