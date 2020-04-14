package com.github.aymanizz.ktori18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class RTests {
    @Test
    @Suppress("InvalidBundleOrProperty")
    fun `generates keys correctly`() {
        assertAll({
            val r = R("key.subKey", count = 10)
            assertIterableEquals(listOf("key.subKey.10", "key.subKey"), r)
        }, {
            val r = R("key.subKey")
            assertIterableEquals(listOf("key.subKey"), r)
        })
    }
}

internal class ResourceBundleMessageResolverTests {
    private val resolver = ResourceBundleMessageResolver("TestBundle")
    private val testLocale = Locale.forLanguageTag("en")

    @Test
    fun `can resolve messages from a utf-8 encoded resource`() {
        val message = resolver.t(Locale.forLanguageTag("ar"), listOf("test"))
        assertEquals("رسالة اختبار", message)
    }

    @Test
    fun `resolves messages from specified bundle`() {
        val message = resolver.t(testLocale, listOf("bundleName"))
        assertEquals("test bundle", message)
    }

    @Test
    fun `resolves message for given locale`() {
        val key = listOf("test")
        assertAll({
            val message = resolver.t(Locale.forLanguageTag("en"), key)
            assertEquals("test message", message)
        }, {
            val message = resolver.t(Locale.forLanguageTag("ar"), key)
            assertEquals("رسالة اختبار", message)
        }, {
            val message = resolver.t(Locale.forLanguageTag("de"), key)
            assertEquals("test nachricht", message)
        })
    }

    @Test
    fun `resolves message for given key`() {
        assertAll({
            val message = resolver.t(testLocale, listOf("testKey1"))
            assertEquals("test key 1", message)
        }, {
            val message = resolver.t(testLocale, listOf("testKey2"))
            assertEquals("test key 2", message)
        })
    }

    @Test
    fun `resolves message for key with fallbacks`() {
        val message = resolver.t(testLocale, listOf("missingKey1", "missingKey2", "testKey1"))
        assertEquals("test key 1", message)
    }

    @Test
    fun `resolves to the first available key`() {
        val message = resolver.t(testLocale, listOf("testKey1", "testKey2"))
        assertEquals("test key 1", message)
    }

    @Test
    fun `resolves message for missing locale from fallback bundle`() {
        val message = resolver.t(Locale.forLanguageTag("fr"), listOf("defaultKey"))
        assertEquals("default key", message)
    }

    @Test
    fun `throws for missing key`() {
        assertThrows<MissingResourceException> {
            resolver.t(testLocale, listOf("missingKey"))
        }
    }

    @Test
    fun `formats message with extra arguments`() {
        val message = resolver.t(Locale.forLanguageTag("en"), listOf("formatKey"), "message", "formatted")
        assertEquals("this message is correctly formatted", message)
    }

    @Test
    fun `ignores extra arguments for messages with no placeholders`() {
        val message = resolver.t(Locale.forLanguageTag("en"), listOf("formatKey"), "message", "formatted", "ignored")
        assertEquals("this message is correctly formatted", message)
    }
}
