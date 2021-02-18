package pl.sages

/*   "should parse key value pair"

fun keyValuePair(input: String): Map<String, String> {
    val parts = input.split(":")
    return mapOf(parts[0] to parts[1])
}
*/

/*   "should parse many key value pairs"

fun keyValuePair(input: String): Map<String, String> {
    return input.split("\n").map {
        val parts = it.split(":")
        parts[0] to parts[1]
    }.toMap()
}
*/

sealed class Result<out T> {
    data class Success<T>(val match: T, val remainder: String) : Result<T>()
    data class Failure(val expected: String, val remainder: String) : Result<Nothing>()

    fun <U> map(f: (T) -> U): Result<U> = when (this) {
        is Failure -> this
        is Success -> Success(f(match), remainder)
    }

    fun <U> flatMap(f: (T, String) -> Result<U>): Result<U> = when (this) {
        is Failure -> this
        is Success -> f(match, remainder)
    }

    fun mapExpected(f: (String) -> String): Result<T> = when (this) {
        is Success -> this
        is Failure -> Failure(f(expected), remainder)
    }
}

typealias Parser<T> = (String) -> Result<T>

/*
fun prefix(text: String): Parser<String> {
    return { input ->
        if (input.startsWith(text)) {
            Result.Success(text, input.substring(text.length))
        } else {
            Result.Failure(text, input)
        }
    }
}
*/

fun prefix(text: String): Parser<String> = { input ->
    if (input.startsWith(text)) {
        Result.Success(text, input.substring(text.length))
    } else {
        Result.Failure(text, input)
    }
}

fun integer(input: String): Result<Int> {
    val match = input.takeWhile { it.isDigit() }
    return if (match.isNotEmpty()) {
        Result.Success(match.toInt(), input.substring(match.length))
    } else {
        Result.Failure("an integer", input)
    }
}

fun whitespace(input: String): Result<String> {
    val match = input.takeWhile { it.isWhitespace() }
    return if (match.isNotEmpty()) {
        Result.Success(match, input.substring(match.length))
    } else {
        Result.Failure("", input)
    }
}

/*
fun <T1, T2> sequence(firstParser: Parser<T1>, secondParser: Parser<T2>): Parser<Pair<T1, T2>> = { input ->
    when (val firstResult = firstParser(input)) {
        is Result.Failure -> firstResult
        is Result.Success -> when (val secondResult = secondParser(firstResult.remainder)) {
            is Result.Failure -> secondResult
            is Result.Success -> Result.Success(Pair(firstResult.match, secondResult.match), secondResult.remainder)
        }
    }
}
*/

// With Result::map

/*
fun <T1, T2> sequence(firstParser: Parser<T1>, secondParser: Parser<T2>): Parser<Pair<T1, T2>> = { input ->
    when (val firstResult = firstParser(input)) {
        is Result.Failure -> firstResult
        is Result.Success -> secondParser(firstResult.remainder)
            .map { Pair(firstResult.match, it) }
    }
}
*/

// With Result::flatMap

fun <T1, T2> sequence(firstParser: Parser<T1>, secondParser: Parser<T2>): Parser<Pair<T1, T2>> = { input ->
    firstParser(input).flatMap { firstResult, rest ->
        secondParser(rest).map { Pair(firstResult, it) }
    }
}

infix fun <T1, T2> Parser<T1>.then(secondParser: Parser<T2>): Parser<Pair<T1, T2>> = sequence(this, secondParser)

fun <T> oneOf(firstParser: Parser<T>, secondParser: Parser<T>): Parser<T> = { input ->
    when (val firstResult = firstParser(input)) {
        is Result.Success -> firstResult
        is Result.Failure -> secondParser(input)
            .mapExpected { "${firstResult.expected} or $it" }
    }
}

infix fun <T> Parser<T>.or(secondParser: Parser<T>): Parser<T> = oneOf(this, secondParser)

fun <T, U> Parser<T>.map(f: (T) -> U): Parser<U> = { input ->
    this(input).map(f)
}

fun <X, T> Parser<X>.before(p: Parser<T>): Parser<T> = sequence(this, p).map { it.second }

fun <T, Y> Parser<T>.followedBy(y: Parser<Y>): Parser<T> = sequence(this, y).map { it.first }

fun <T> Parser<T>.many(): Parser<List<T>> = { input ->
    when (val result = this(input)) {
        is Result.Failure -> Result.Success(emptyList(), input)
        is Result.Success -> many()(result.remainder)
            .map { listOf(result.match) + it }
    }
}

fun <T, X> Parser<T>.separatedBy(separator: Parser<X>): Parser<List<T>> = { input ->
    fun parse(tail: String): Result<List<T>> =
        when (val separatorResult = separator(tail)) {
            is Result.Failure -> Result.Success(emptyList(), tail)
            is Result.Success -> when (val result = this(separatorResult.remainder)) {
                is Result.Failure -> result
                is Result.Success -> parse(result.remainder)
                    .map { listOf(result.match) + it }
            }
        }
    when (val result = this(input)) {
        is Result.Failure -> Result.Success(emptyList(), input)
        is Result.Success -> parse(result.remainder)
            .map { listOf(result.match) + it }
    }
}

fun prefixWhile(predicate: (Char) -> Boolean): Parser<String> = { input ->
    val match = input.takeWhile(predicate)
    if (match.isNotEmpty()) {
        Result.Success(match, input.substring(match.length))
    } else {
        Result.Failure("", input)
    }
}
