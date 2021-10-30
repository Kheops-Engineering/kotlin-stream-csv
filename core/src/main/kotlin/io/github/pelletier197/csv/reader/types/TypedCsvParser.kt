package io.github.pelletier197.csv.reader.types

import io.github.pelletier197.csv.reader.CsvError
import io.github.pelletier197.csv.reader.CsvErrorType
import io.github.pelletier197.csv.reader.CsvParsingException
import io.github.pelletier197.csv.reader.reflect.CsvReflectionCreator
import io.github.pelletier197.csv.reader.reflect.InstantiationError
import io.github.pelletier197.csv.reader.reflect.converters.ConversionSettings
import io.github.pelletier197.csv.reader.reflect.converters.Converter
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.util.stream.Stream

data class TypedCsvReader<T>(
    private val targetClass: Class<T>,
    val listSeparator: Char = ',',
    private val creator: CsvReflectionCreator<T> = CsvReflectionCreator(targetClass),
    private val reader: HeaderCsvReader = HeaderCsvReader()
) {
    private val conversionSettings = ConversionSettings(
        listSeparator = listSeparator
    )

    fun withSeparator(separator: Char): TypedCsvReader<T> {
        return copy(reader = reader.withSeparator(separator))
    }

    fun withDelimiter(delimiter: Char): TypedCsvReader<T> {
        return copy(reader = reader.withDelimiter(delimiter))
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

    fun withHeader(header: List<String>): TypedCsvReader<T> {
        return copy(reader = reader.withHeader(header))
    }

    fun withListSeparator(listSeparator: Char): TypedCsvReader<T> {
        return copy(listSeparator = listSeparator)
    }

    fun withConverter(newConverter: Converter<*, *>): TypedCsvReader<T> {
        return copy(creator = creator.withConverter(newConverter))
    }

    fun withConverters(newConverters: List<Converter<*, *>>): TypedCsvReader<T> {
        return copy(creator = creator.withConverters(newConverters))
    }

    fun withClearedConverters(): TypedCsvReader<T> {
        return copy(creator = creator.withClearedConverters())
    }

    fun read(url: URL): Stream<TypedCsvLine<T>> {
        return readHeaderLines(reader.read(url))
    }

    fun read(input: InputStream): Stream<TypedCsvLine<T>> {
        return readHeaderLines(reader.read(input))
    }

    fun read(file: File): Stream<TypedCsvLine<T>> {
        return readHeaderLines(reader.read(file))
    }

    fun read(path: Path): Stream<TypedCsvLine<T>> {
        return readHeaderLines(reader.read(path))
    }

    fun read(lines: List<String>): Stream<TypedCsvLine<T>> {
        return readHeaderLines(reader.read(lines))
    }

    fun read(value: String): Stream<TypedCsvLine<T>> {
        return readHeaderLines(reader.read(value))
    }

    private fun readHeaderLines(lines: Stream<HeaderCsvLine>): Stream<TypedCsvLine<T>> {
        return lines.map {
            val mappedInstance = creator.createCsvInstance(
                csvHeadersValues = it.values,
                settings = conversionSettings
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
            providedValue = error.providedValue,
            type = CsvErrorType.valueOf(error.type.toString()),
            cause = error.cause
        )
    }
}

data class TypedCsvLine<T>(
    val result: T?,
    val line: Int,
    val errors: List<CsvError> = emptyList()
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()

    fun getResultOrThrow(): T {
        return if (hasErrors) throw CsvParsingException(
            message = "error while parsing CSV file",
            errors = errors,
            line = line
        ) else result ?: error("unexpected null result for an entity with no error")
    }
}
