package ai.amani.sample.domain.model

/**
 * A selectable country and the language the Amani UI SDK is started in for it.
 *
 * The entry screen shows only these (English) country names — there is no separate language
 * picker. Whatever country the user selects decides the `language` passed to the SDK, while the
 * token/server come from the scanned QR.
 */
data class Country(val displayName: String, val language: String)

val COUNTRIES: List<Country> = listOf(
    // Türkiye is the default selection, so it stays first.
    Country("Türkiye", "tr"),
    Country("Armenia", "hy"),
    Country("Brazil", "pt-br"),
    Country("Bulgaria", "bg"),
    Country("Czechia", "cs"),
    Country("Denmark", "da"),
    Country("France", "fr"),
    Country("Georgia", "ka"),
    Country("Germany", "de"),
    Country("Greece", "el"),
    Country("Hungary", "hu"),
    Country("Indonesia", "id"),
    Country("Italy", "it"),
    Country("Laos", "lo"),
    Country("Lithuania", "lt"),
    Country("Netherlands", "nl"),
    Country("Philippines", "fl"),
    Country("Poland", "pl"),
    Country("Portugal", "pt"),
    Country("Romania", "ro"),
    Country("Russia", "ru"),
    Country("Slovakia", "sk"),
    Country("Spain", "es"),
    Country("Thailand", "th"),
    Country("Ukraine", "uk"),
    Country("United Kingdom", "en"),
    Country("Vietnam", "vi")
)
