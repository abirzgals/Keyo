package com.keyo

import org.junit.Assert.assertEquals
import org.junit.Test

class DictationLanguageTest {

    private val all = listOf("en", "ru", "lv")

    @Test fun russianHeardAsANeighbour_isRetriedAsRussian_notAsTheKeyboardLanguage() {
        for (d in listOf("ukrainian", "belarusian", "bulgarian", "serbian"))
            assertEquals(d, "ru", GroqApi.retryLanguage(d, "привіт як справи", all, fallback = "lv"))
    }

    @Test fun latvianHeardAsLithuanian_isRetriedAsLatvian() {
        assertEquals("lv", GroqApi.retryLanguage("lithuanian", "labas", all, fallback = "en"))
    }

    @Test fun unknownLanguage_withCyrillicText_isRussian() {
        assertEquals("ru", GroqApi.retryLanguage("mongolian", "сайн байна уу", all, fallback = "lv"))
    }

    @Test fun unknownLanguage_withLatinText_followsTheKeyboard() {
        assertEquals("lv", GroqApi.retryLanguage("spanish", "hola", all, fallback = "lv"))
    }

    @Test fun aNeighbourOfALanguageNotEnabled_doesNotWin() {
        assertEquals("lv", GroqApi.retryLanguage("ukrainian", "", listOf("en", "lv"), fallback = "lv"))
    }

    @Test fun fallbackNotAllowed_takesTheFirstAllowed() {
        assertEquals("en", GroqApi.retryLanguage("spanish", "hola", listOf("en", "lv"), fallback = "ru"))
    }
}
