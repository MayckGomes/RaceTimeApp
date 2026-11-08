package mayckgomes.com.racetimeapp.domain.weekend

import java.time.Clock
import java.time.LocalDate
import java.time.Period

fun driverDisplayName(givenName: String, familyName: String, fallback: String): String =
    listOf(givenName.trim(), familyName.trim())
        .filter(String::isNotEmpty)
        .joinToString(" ")
        .ifBlank { fallback }

fun driverBirthDate(value: String, clock: Clock): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()?.takeIf { !it.isAfter(LocalDate.now(clock)) }

fun driverAge(value: String, clock: Clock): Int? =
    driverBirthDate(value, clock)?.let { Period.between(it, LocalDate.now(clock)).years }

fun driverInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return listOfNotNull(words.firstOrNull(), words.lastOrNull()?.takeIf { words.size > 1 })
        .joinToString("") { it.take(1).uppercase(java.util.Locale.ROOT) }
}
