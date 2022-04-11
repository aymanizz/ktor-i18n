package com.github.aymanizz.ktori18n

import com.github.aymanizz.ktori18n.I18n.Configuration
import io.ktor.http.Cookie
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.plugins.origin
import io.ktor.server.request.acceptLanguage
import io.ktor.server.response.respondRedirect
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import java.util.Locale
import kotlin.collections.ArrayList

/**
 * Internationalization support feature for Ktor.
 *
 * @see Configuration for how to configure this feature
 */
class I18n private constructor(configuration: Configuration) : MessageResolver by configuration.messageResolver {
    val supportedLocales = configuration.supportedLocales

    val defaultLocale = configuration.defaultLocale ?: supportedLocales.first()

    val useOfCookie = configuration.useOfCookie

    val localeCookieName = configuration.cookieName

    val useOfRedirection = configuration.useOfRedirection

    val supportedPathPrefixes = "(${supportedLocales.joinToString("|", transform = { it.language })})".toRegex()

    val excludePredicates: List<(ApplicationCall) -> Boolean> = configuration.excludePredicates.toList()

    init {
        Locale.setDefault(defaultLocale)
    }

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
     * [useOfCookie] Whether to use cookie or not to resolve [Locale]
     * [cookieName] A sensible naming of the cookie
     *
     * [useOfRedirection] Whether to use redirection based on path language prefix
     *
     * [excludePredicates] The list of call predicates for redirect exclusion.
     * Any call matching any of the predicates will not be redirected by this feature.
     *
     * @see MessageResolver
     */
    class Configuration {
        lateinit var supportedLocales: List<Locale>

        var defaultLocale: Locale? = null

        var messageResolver: MessageResolver = ResourceBundleMessageResolver()

        var useOfCookie: Boolean = false

        var cookieName: String = "locale"

        var useOfRedirection: Boolean = false

        val excludePredicates: MutableList<(ApplicationCall) -> Boolean> = ArrayList()

        /**
         * Exclude calls with paths matching the [pathPrefixes] from being redirected with language prefix by this feature.
         */
        fun excludePrefixes(vararg pathPrefixes: String) {
            pathPrefixes.forEach { prefix ->
                exclude { call -> call.request.origin.uri.startsWith(prefix) }
            }
        }

        /**
         * Exclude calls matching the [predicate] from being redirected with language prefix by this feature.
         * @see io.ktor.features.HttpsRedirect for example of exclusions
         */
        fun exclude(predicate: (call: ApplicationCall) -> Boolean) {
            excludePredicates.add(predicate)
        }
    }

    private suspend fun intercept(ctx: PipelineContext<Unit, ApplicationCall>) {
        val call = ctx.context
        val language = call.locale.language

        val uri = call.request.origin.uri.trimStart('/').trimEnd('/').split('/')
        if (!uri.first().matches(supportedPathPrefixes) &&
            excludePredicates.none { predicate -> predicate(call) }
        ) {
            val toRedirect =
                mutableListOf<String>(language, *uri.toTypedArray()).joinToString("/", prefix = "/").trimEnd('/')
            call.respondRedirect(toRedirect)
            ctx.finish()
        }
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, I18n> {

        override val key = AttributeKey<I18n>("I18n")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): I18n {
            val configuration = Configuration().apply { configure() }.apply {
                require(supportedLocales.isNotEmpty()) { "supported locales must not be empty" }
                require(defaultLocale == null || defaultLocale!! in supportedLocales) {
                    "default locale must be one of the supported locales"
                }
            }

            val feature = I18n(configuration)

            if (configuration.useOfRedirection) {
                pipeline.intercept(Plugins) { feature.intercept(this) }
            }

            return feature
        }
    }
}

val Application.i18n
    get() = this.plugin(I18n)

private val CallLocaleKey = AttributeKey<Locale>("CallLocale")

/**
 * The locale for the current call, based on accept language header, then from the optional language cookie and finally from
 * the language prefix of the request path.
 *
 * If there is no supported locale that matches the request accept language locales, the the default locale is returned.
 */
val ApplicationCall.locale
    get() = attributes.computeIfAbsent(CallLocaleKey) locale@{
        val i18n = application.i18n

        fun writeCookie(locale: Locale) {
            response.cookies.append(Cookie(i18n.localeCookieName, locale.language, maxAge = 60 * 60))
        }

        fun readCookie(): String? {
            return request.cookies[i18n.localeCookieName]
        }

        if (i18n.useOfRedirection) {
            val uri = request.origin.uri.trimStart('/').trimEnd('/').split('/')
            val languagePrefix = uri.first()
            if (languagePrefix.matches(i18n.supportedPathPrefixes)) {
                val locale = Locale.forLanguageTag(languagePrefix)
                if (i18n.useOfCookie && languagePrefix != readCookie()) {
                    writeCookie(locale)
                }
                return@locale locale
            }
        }

        if (i18n.useOfCookie) {
            val cookieLocale = readCookie()
            if (cookieLocale != null) {
                return@locale Locale.forLanguageTag(cookieLocale)
            }
        }

        val acceptLocales = request.acceptLanguage()
        val ranges = if (acceptLocales != null) Locale.LanguageRange.parse(acceptLocales) else listOf()
        val locale = Locale.filter(ranges, i18n.supportedLocales).firstOrNull()
            ?: Locale.lookup(ranges, i18n.supportedLocales)
            ?: i18n.defaultLocale

        if (i18n.useOfCookie) {
            writeCookie(locale)
        }

        return@locale locale
    }

/**
 * Helper function for resolving a message using the call locale as returned from [ApplicationCall.locale].
 */
fun ApplicationCall.t(keys: Iterable<String>, vararg args: Any = arrayOf()) =
    application.i18n.t(locale, keys, *args)
