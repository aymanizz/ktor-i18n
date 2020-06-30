## Ktor I18n

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Latest Version](https://jitpack.io/v/aymanizz/ktor-i18n.svg)](https://jitpack.io/#aymanizz/ktor-i18n)
![CI](https://github.com/aymanizz/ktor-i18n/workflows/CI/badge.svg?branch=master&event=push)

Internationalization support for Ktor framework.

### Usage

#### Add The Dependency

add the dependency to your gradle build file:

```groovy
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation 'com.github.aymanizz:ktor-i18n:1.0.0'
}
```

#### Usage Example

For this example we will create a messages bundle with English and Arabic localizations.

Start by installing the feature in your application:
```kotlin
install(I18n) {
    supportedLocales = listOf("en", "ar").map(Locale::forLanguageTag)
}
```
Because the english locale is the first in the supported locales list, it will be considered the default/fallback locale.

Next, let's create `i18n/` folder inside the resources directory, and create two files: `Messages_en.properties` and
`Messages_ar.properties`, in this folder with the following content:

- inside Messages_en.properties
```properties
greeting=Hello!
```
- inside Messages_ar.properties
```properties
greeting=هلا!
```

We now have the most basic setup for the i18n feature, we can use it as follows:
```kotlin
routing {
    get("/greeting") {
        call.respondText(call.t(R("greeting")))
    }
}
```

Now that the route `/greeting` is set up, we can send a request and expect a response depending on the request's
`Accept-Language` header value.

For example, if the header value is `en-US` the response will be `Hello`, if it's `en;q=0.7,ar;q=0.9` then the response
will be `هلا`, and if it's `de` then the response will fallback to `Hello` because english is the default locale.

Now let's add more keys to our messages. This time we will use a key with a count, and also a placeholder:

- in Messages_en.properties
```properties
pieces=You have {0,number} pieces.
pieces.1=You have a single piece.
pieces.2=You have two pieces.
```
- in Messages_ar.properties
```properties
pieces=لديك {0,number} قطع
pieces.1=لديك قطعة واحدة
pieces.2=لديك قطعتان
```

And use it like so:
```kotlin
routing {
    get("/greeting") { /* ... */ }
    get("/pieces") {
        val count = call.parameters["count"]?.toIntOrNull() ?: 1
        call.respondText(call.t(R("pieces", count = count), count))
    }
}
```

Note that we have passed count twice, once to the `R` class constructor which generates the keys to lookup the
localization, this class will generate the keys `"pieces.$count"` and `"pieces"`, the localization provider will try
each key until one of them is present in the messages bundle, then the obtained message is formatted using `count` (the
second parameter passed to `t` call.) In the case of the `pieces.1` and `pieces.2` messages, the extra argument is
ignored. But in the case of `peices` message, the `{}` placeholder is replaced with the value of `count`.

Go ahead and try your application now with different `Accept-Language` header values and different `count` query
parameter values.

#### Different Key Generation Strategy

So far we have used the default strategy provided by the `R` key generator instances. However, one can implement any
other strategy to suit their needs. For example, here is how to add a gender key to our keys:
```kotlin
enum class Gender { Male, Female }

// implement a key generator
class MyKey(
        @PropertyKey(resourceBundle = "i18n.Messages")
        baseKey: String,
        count: Int? = null,
        gender: Gender? = null
) : DelimitedKeyGenerator(baseKey, count?.toString(), gender?.name?.toLowerCase())
```

Then we can use it in our code as before, in messages:
```properties
greeting=Hello!
greeting.female=Hi!
```

And the application code:
```kotlin
call.t(MyKey("greeting", gender=Gender.Female)) // result is Hi!
```

The key generator could be any iterable that produces strings, so you can customize the generation even further.

### Contribution

#### Development

To run the tests:
```
./gradlew test
```

To run the formatter:
```
./gradlew spotlessApply
```

#### Issues

If you encounter any issues with this feature, let us know by opening a new issue with the issue description.
Make sure that there are no similar issues already open.

#### Features

Before starting on development for a new feature please open a feature request issue for discussion.

### License

The project is licensed under the MIT License. For further details see [LICENSE](LICENSE).
