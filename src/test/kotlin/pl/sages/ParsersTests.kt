package pl.sages

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ParsersTests : StringSpec({

    /*"should parse key value pair" {
        keyValuePair("username:jan") shouldBe mapOf("username" to "jan")
    }

    "should parse many key value pairs" {
        keyValuePair("username:jan\nmarek:nowak") shouldBe
                mapOf("username" to "jan", "marek" to "nowak")
    }*/

    /*
    co jeśli pojawią się kolejne wymagania np:
        - pary jako wartości
        - linie z komentarzem
        - inne separatory
        - wyrażenia, które trzeba interpretować w specyficzmy sposób
     Logika naszego parsera stanie się zawiła i skomplikowana.
     Jakimś pomysłem mogą wydawać się wyrażenia regularne, ale to zły pomysł :)

     Źeby rozwiązać problem w efektywny sposób wykorzytamy funkcje i możliwośc ich kompozycji
     Możemy założyć (chociaż to uproszeczenie), że parser to prosta funkcja
     (String) -> Result<T> gdzie Result będzie reprezentowało dwie potencjalne wartości
     sukces albo porażkę
    */

    "should parse a prefix" {
        val parser = prefix("-")
        parser("-jan") shouldBe Result.Success("-", "jan")
        parser("jan") shouldBe Result.Failure("-", "jan")
    }

    "should parse an integer" {
        integer("12jan") shouldBe Result.Success(12, "jan")
        integer("jan") shouldBe Result.Failure("an integer", "jan")
    }

    "should parse a whitespace" {
        whitespace("  jan") shouldBe Result.Success("  ", "jan")
        whitespace("jan") shouldBe Result.Failure("", "jan")
    }

    "should parse a sequence" {
        val parser = sequence(prefix("-"), ::integer)
        parser("-123") shouldBe Result.Success(Pair("-", 123), "")
        parser("123") shouldBe Result.Failure("-", "123")
        prefix("-").then(::integer)("-123") shouldBe Result.Success(Pair("-", 123), "")
    }

    "should parse using one of the parsers" {
        val parser = oneOf(prefix("a"), prefix("b"))
        parser("ab") shouldBe Result.Success("a", "b")
        parser("cd") shouldBe Result.Failure("a or b", "cd")
        prefix("a").or(prefix("b"))("ab") shouldBe Result.Success("a", "b")
    }

    "should map result value" {
        integer("11").map { it % 2 == 0 } shouldBe Result.Success(false, "")
    }

    "should skip first parser" {
        val parser = ::integer.before(prefix("a"))
        parser("1a") shouldBe Result.Success("a", "")
    }

    "should skip following parser" {
        val parser = prefix("a").followedBy(::integer)
        parser("a1") shouldBe Result.Success("a", "")
    }

    "should parse many elements" {
        val parser = prefix("a").many()
        parser("aa") shouldBe Result.Success(listOf("a", "a"), "")
    }

    "should parse many separated elements" {
        val parser = ::integer.separatedBy(prefix(","))
        parser("1,2") shouldBe Result.Success(listOf(1, 2), "")
        parser("1,a") shouldBe Result.Failure("an integer", "a")
        parser("a") shouldBe Result.Success(emptyList<Int>(), "a")
    }

    "should parse array of integers" {
        val letKeyword = prefix("let").then(::whitespace.many())
        val variableName = prefixWhile { it.isLetter() }.followedBy(::whitespace.many())
        val assigment = prefix("=").followedBy(::whitespace.many())
        val numbersSeparatedByComma = ::integer.separatedBy(sequence(prefix(","), ::whitespace.many()))
        val numbersArray = prefix("[").before(numbersSeparatedByComma).followedBy(prefix("]"))
        val parser = letKeyword.before(variableName).followedBy(assigment).then(numbersArray)

        when (val result = parser("let  ab = [1, 2,  3,4]")) {
            is Result.Success -> print(result)
            is Result.Failure -> print(result.expected)
        }
    }

})
