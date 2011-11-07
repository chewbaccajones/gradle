/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.util;


import static org.gradle.util.GUtil.*

public class GUtilTest extends spock.lang.Specification {
    
    def convertStringToCamelCase() {
        expect:
        toCamelCase(null) == null
        toCamelCase("") == ""
        toCamelCase("word") == "Word"
        toCamelCase("twoWords") == "TwoWords"
        toCamelCase("TwoWords") == "TwoWords"
        toCamelCase("two-words") == "TwoWords"
        toCamelCase("two.words") == "TwoWords"
        toCamelCase("two words") == "TwoWords"
        toCamelCase("two Words") == "TwoWords"
        toCamelCase("Two Words") == "TwoWords"
        toCamelCase(" Two  \t words\n") == "TwoWords"
        toCamelCase("four or so Words") == "FourOrSoWords"
        toCamelCase("trailing-") == "Trailing"
        toCamelCase("ABC") == "ABC"
        toCamelCase(".") == ""
        toCamelCase("-") == ""
    }

    
    def convertStringToConstantName() {
        expect:
        toConstant(null) == null
        toConstant("") == ""
        toConstant("word") == "WORD"
        toConstant("twoWords") == "TWO_WORDS"
        toConstant("TwoWords") == "TWO_WORDS"
        toConstant("two-words") == "TWO_WORDS"
        toConstant("TWO_WORDS") == "TWO_WORDS"
        toConstant("two.words") == "TWO_WORDS"
        toConstant("two words") == "TWO_WORDS"
        toConstant("two Words") == "TWO_WORDS"
        toConstant("Two Words") == "TWO_WORDS"
        toConstant(" Two  \t words\n") == "TWO_WORDS"
        toConstant("four or so Words") == "FOUR_OR_SO_WORDS"
        toConstant("trailing-") == "TRAILING"
        toConstant("a") == "A"
        toConstant("A") == "A"
        toConstant("aB") == "A_B"
        toConstant("ABC") == "ABC"
        toConstant("ABCThing") == "ABC_THING"
        toConstant("ABC Thing") == "ABC_THING"
        toConstant(".") == ""
        toConstant("-") == ""
    }

    def convertStringToWords() {
        expect:
        toWords(null) == null
        toWords("") == ""
        toWords("word") == "word"
        toWords("twoWords") == "two words"
        toWords("TwoWords") == "two words"
        toWords("two words") == "two words"
        toWords("Two Words") == "two words"
        toWords(" Two  \t words\n") == "two words"
        toWords("two_words") == "two words"
        toWords("two.words") == "two words"
        toWords("two,words") == "two words"
        toWords("trailing-") == "trailing"
        toWords("a") == "a"
        toWords("aB") == "a b"
        toWords("ABC") == "abc"
        toWords("ABCThing") == "abc thing"
        toWords("ABC Thing") == "abc thing"
        toWords(".") == ""
        toWords("_") == ""
    }

    def "normalizes to collection"() {
        expect:
        normalize(null) == []
        normalize(10) == [10]
        normalize("a") == ["a"]

        List list = [2, 2, "three"] as List
        normalize(list) == [2, 2, "three"]

        Object[] array = [1, 1, "three"]
        normalize(array) == [1, 1, "three"]

        normalize([list, array]) == [2, 2, "three", 1, 1, "three"]
    }
}