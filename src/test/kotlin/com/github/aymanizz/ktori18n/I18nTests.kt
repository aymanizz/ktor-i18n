package com.github.aymanizz.ktori18n

import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Locale
import kotlin.test.assertEquals

internal class I18nTests {
    @Test
    fun `throws when supported locales is not initialized`() {
        assertThrows<UninitializedPropertyAccessException> {
            testApplication {
                install(I18n)
            }
        }
    }

    @Test
    fun `throws when supported locales is empty`() {
        assertThrows<IllegalArgumentException> {
            testApplication {
                install(I18n) { supportedLocales = listOf() }
            }
        }
    }

    @Test
    fun `throws when default locale not in supported locales`() {
        assertThrows<IllegalArgumentException> {
            testApplication {
                install(I18n) {
                    supportedLocales = listOf("en", "de").map { Locale.forLanguageTag(it) }
                    defaultLocale = Locale.forLanguageTag("ar")
                }
            }
        }
    }

    @Test
    fun `default locale defaults to the first supported locale`(): Unit = testApplication {
        val locales = listOf("en", "ar").map { Locale.forLanguageTag(it) }
        install(I18n) {
            supportedLocales = locales
        }
        this.application {
            assertEquals(locales[0], i18n.defaultLocale)
        }
    }
}
