package com.github.aymanizz.ktor_i18n

import io.ktor.application.*
import io.ktor.request.acceptLanguage
import io.ktor.util.AttributeKey
import java.util.*

/**
 * Internationalization support feature for Ktor.
 *
 * @see Configuration for how to configure this feature
 */
class I18n private constructor(configuration: Configuration): MessageResolver by configuration.messageResolver {
    val supportedLocales = configuration.supportedLocales
    val defaultLocale = configuration.defaultLocale ?: supportedLocales.first()

    init { Locale.setDefault(defaultLocale) }

    /**
     * The configuration for the internationalization feature.
     *
     * [supportedLocales] represents the supported locales for the application, this list is used in the resolution
     * of the current locale. must be initialized and not empty.
     *
     * [defaultLocale]. Used as a fallback when the request locale is not supported. Defaults to the first
     * locale in [supportedLocales].
     *
     * [messageResolver] instance of MessageResolver used in message resolution. Defaults to
     * [ResourceBundleMessageResolver].
     *
     * @see MessageResolver
     */
    class Configuration {
        lateinit var supportedLocales: List<Locale>
        var defaultLocale: Locale? = null
        var messageResolver: MessageResolver = ResourceBundleMessageResolver()
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, I18n> {
        override val key = AttributeKey<I18n>("I18n")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): I18n {
            val configuration = Configuration().apply { configure() }.apply {
                require(supportedLocales.isNotEmpty()) { "supported locales must not be empty" }
                require(defaultLocale == null || defaultLocale!! in supportedLocales) {
                    "default locale must be one of the supported locales"
                }
            }
            return I18n(configuration)
        }
    }
}


val Application.i18n get() = feature(I18n)

private val CallLocaleKey = AttributeKey<Locale>("CallLocale")

/**
 * The locale for the current call, from the accept language header.
 *
 * If there is no supported locale that matches the request accept language locales, the the default locale is returned.
 */
val ApplicationCall.locale get() = attributes.computeIfAbsent(CallLocaleKey) {
    val i18n = application.i18n
    val acceptLocales = request.acceptLanguage()
    val ranges = if (acceptLocales != null) Locale.LanguageRange.parse(acceptLocales) else listOf()
    Locale.filter(ranges, i18n.supportedLocales).firstOrNull()
            ?: Locale.lookup(ranges, i18n.supportedLocales)
            ?: i18n.defaultLocale
}

/**
 * Helper function for resolving a message using the call locale as returned from [ApplicationCall.locale].
 */
fun ApplicationCall.t(keys: Iterable<String>, vararg args: Any = arrayOf()) =
        application.i18n.t(locale, keys, *args)