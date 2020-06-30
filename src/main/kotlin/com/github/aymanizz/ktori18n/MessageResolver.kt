package com.github.aymanizz.ktori18n

import org.jetbrains.annotations.PropertyKey
import java.text.Format
import java.text.MessageFormat
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap

/**
 * Used to obtain a message and format it.
 *
 * Defines the strategy used in resolving messages according to the localization and key generator provided.
 *
 * The extra arguments are used in formatting the message. The semantics for formatting are defined by subclasses.
 *
 * @see ResourceBundleMessageResolver
 */
interface MessageResolver {
    fun t(locale: Locale, keyGenerator: KeyGenerator, vararg args: Any = arrayOf()): String
}

/**
 * Any iterable of strings can be used as the key generator.
 */
typealias KeyGenerator = Iterable<String>

/**
 * Base class for key generators that generate keys from components.
 *
 * Generation of the keys starts with a base key constructed from the concatenation of the components using
 * the delimiter as a separator. Each iteration removes the last component from the key until there are no
 * more components.
 *
 * For example:
 *  for the three components: `key1`, `key2`, `key3`,
 *  the keys generated are inorder: `key1.key2.key3`, `key1.key2`, `key1`
 *  assuming the delimiter is `"."`
 */
abstract class DelimitedKeyGenerator(
    private vararg val components: String?,
    private val delimiter: String = "."
) : KeyGenerator {
    override fun iterator() = iterator {
        val components = components.filterNotNull()
        for (count in components.size downTo 1) {
            yield(components.subList(0, count).joinToString(separator = delimiter))
        }
    }
}

@Suppress("InvalidBundleOrProperty")
const val DEFAULT_RESOURCE_BUNDLE = "i18n.Messages"

/**
 * The default key generator based on the [DelimitedKeyGenerator].
 *
 * This class provides basic support for default lookup keys with an optional count component.
 *
 * @param baseKey The base key for the lookup, this is the first component of the key.
 * @param count The second component of the key, semantically this is the number of items.
 */
class R(
    @PropertyKey(resourceBundle = DEFAULT_RESOURCE_BUNDLE)
    baseKey: String,
    count: Int? = null
) : DelimitedKeyGenerator(baseKey, count?.toString())

private typealias FormatCacheKey = Pair<String, Locale>

/**
 * A [MessageResolver] that uses [ResourceBundle] for message resolution and [MessageFormat] for message formatting.
 *
 * By default the resource bundle used by this class is i18n.Messages.
 *
 * For each key generated by the key generator, the resolution for the message is delegated to the resource bundle.
 * If the resource bundle fails to resolve a key, the next key is attempted again until there are no more keys.
 *
 * The default resource bundle control for this class supports UTF-8 property files.
 *
 * For customizing the resource bundle and message format instances used by this class, subclass it and override the
 * appropriate methods.
 *
 * @see ResourceBundle for the resolution mechanism used for a single key.
 * @see MessageFormat for the formatting semantics.
 */
open class ResourceBundleMessageResolver(
    /** The base name for the resource bundle */
    private val baseName: String = DEFAULT_RESOURCE_BUNDLE,
    /** The resource bundle control. */
    private val control: ResourceBundle.Control = UTF8Control()
) : MessageResolver {
    private val formats = ConcurrentHashMap<FormatCacheKey, Format>()

    final override fun t(locale: Locale, keyGenerator: KeyGenerator, vararg args: Any): String {
        var exception: MissingResourceException? = null
        if (!keyGenerator.iterator().hasNext()) {
            throw IllegalStateException("at least one key must be present")
        }

        for (key in keyGenerator) {
            try {
                return translate(locale, key, *args)
            } catch (e: MissingResourceException) {
                if (exception == null) {
                    exception = e
                }
            }
        }
        throw exception!!
    }

    /**
     * Gets the resource bundle used for resolving the message
     */
    protected open fun getBundle(locale: Locale): ResourceBundle =
        ResourceBundle.getBundle(baseName, locale, control)

    /**
     * Gets the format instance used for formatting messages
     *
     * The [Format] instances returned from this function are cached.
     */
    protected open fun getMessageFormat(format: String, locale: Locale): Format =
        MessageFormat(format, locale)

    private fun translate(locale: Locale, key: String, vararg args: Any): String {
        val bundle = getBundle(locale)
        val string = bundle.getString(key)
        return args.takeUnless { it.isEmpty() }?.let {
            formats.computeIfAbsent(FormatCacheKey(string, bundle.locale)) {
                getMessageFormat(string, bundle.locale)
            }.format(it)
        } ?: string
    }
}
