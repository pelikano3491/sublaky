package org.sublaky.player

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleResolverTest {
    @Test
    fun resolvesExplicitLanguageTags() {
        assertEquals(Locale.ENGLISH, localeForLanguageTag("en"))
        assertEquals(Locale("es"), localeForLanguageTag("es"))
        assertEquals(Locale("es"), localeForLanguageTag("es-ES"))
    }

    @Test
    fun fallsBackToProvidedSystemLocaleWhenUnset() {
        val fallback = Locale.ENGLISH
        assertEquals(fallback, localeForLanguageTag(null, fallback))
        assertEquals(fallback, localeForLanguageTag("   ", fallback))
    }
}
