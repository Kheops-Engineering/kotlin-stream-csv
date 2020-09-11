package com.kheops.csv.reader.types

import com.kheops.csv.reader.CsvError
import com.kheops.csv.reader.CsvErrorType
import com.kheops.csv.reader.CsvParsingException
import com.kheops.csv.reader.reflect.CsvReflectionCreator
import com.kheops.csv.reader.reflect.InstantiationError
import com.kheops.csv.reader.reflect.converters.ConversionSettings
import java.util.stream.Stream


data class TypedCsvReader<T>(
    val targetClass: Class<T>,
    val listSeparator: String = ",",
    private val csvReflectionCreator: CsvReflectionCreator<T> = CsvReflectionCreator(targetClass),
    private val reader: HeaderCsvReader = HeaderCsvReader()
) {
    private val conversionSettings = ConversionSettings(
        listSeparator = listSeparator
    )

    fun read(lines: Stream<String>): Stream<TypedCsvLine<T>> {
        return readHeaderLines(reader.read(lines))
    }

    fun withSeparator(separator: String): TypedCsvReader<T> {
        return copy(reader = reader.withSeparator(separator))
    }

    fun withDelimiter(delimiter: String): TypedCsvReader<T> {
        return copy(reader = reader.withDelimiter(delimiter))
    }

    fun withEscapeCharacter(character: String): TypedCsvReader<T> {
        return copy(reader = reader.withEscapeCharacter(character))
    }

    fun withTrimEntries(trim: Boolean): TypedCsvReader<T> {
        return copy(reader = reader.withTrimEntries(trim))
    }

    fun withSkipEmptyLines(skip: Boolean): TypedCsvReader<T> {
        return copy(reader = reader.withSkipEmptyLines(skip))
    }

    fun withEmptyStringsAsNull(emptyAsNulls: Boolean): TypedCsvReader<T> {
        return copy(reader = reader.withEmptyStringsAsNull(emptyAsNulls))
    }

    fun withListSeparator(listSeparator: String): TypedCsvReader<T> {
        return copy(listSeparator = listSeparator)
    }

    fun readHeaderLines(lines: Stream<HeaderCsvLine>): Stream<TypedCsvLine<T>> {
        return lines.map {
            val mappedInstance = csvReflectionCreator.createCsvInstance(
                csvHeadersValues = it.values, settings = conversionSettings
            )
            TypedCsvLine(
                result = mappedInstance.result,
                errors = mappedInstance.errors.map { error -> mapInstantiationError(error) },
                line = it.line
            )
        }
    }

    private fun mapInstantiationError(error: InstantiationError): CsvError {
        return CsvError(
            csvField = error.originalField,
            classField = error.field,
            type = CsvErrorType.valueOf(error.type.toString()),
            cause = error.cause
        )
    }
}

data class TypedCsvLine<T>(
    val result: T?,
    val line: Int,
    val errors: List<CsvError>? = null
) {
    val hasErrors: Boolean get() = !errors.isNullOrEmpty()

    fun getResultOrThrow(): T {
        return if (hasErrors) throw CsvParsingException(
            message = "error while parsing CSV file",
            errors = errors!!,
            line = line
        ) else result ?: error("unexpected null result for an entity with no error")
    }
}