package com.github.aymanizz.ktori18n

import io.ktor.application.install
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Locale
import kotlin.test.assertEquals

internal class I18nTests {
    @Test
    fun `throws when supported locales is not initialized`(): Unit = withTestApplication {
        assertThrows<UninitializedPropertyAccessException> { application.install(I18n) }
    }

    @Test
    fun `throws when supported locales is empty`(): Unit = withTestApplication {
        assertThrows<IllegalArgumentException> {
            application.install(I18n) { supportedLocales = listOf() }
        }
    }

    @Test
    fun `throws when default locale not in supported locales`(): Unit = withTestApplication {
        assertThrows<IllegalArgumentException> {
            application.install(I18n) {
                supportedLocales = listOf("en", "de").map { Locale.forLanguageTag(it) }
                defaultLocale = Locale.forLanguageTag("ar")
            }
        }
    }

    @Test
    fun `default locale defaults to the first supported locale`(): Unit = withTestApplication {
        val locales = listOf("en", "ar").map { Locale.forLanguageTag(it) }
        application.install(I18n) {
            supportedLocales = locales
        }
        assertEquals(locales[0], application.i18n.defaultLocale)
    }
}
