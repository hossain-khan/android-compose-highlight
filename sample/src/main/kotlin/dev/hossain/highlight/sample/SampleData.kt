package dev.hossain.highlight.sample

import dev.hossain.highlight.engine.HighlightTheme

/** A single syntax-highlighting demo sample with its highlight.js [language] identifier and [code] string. */
internal data class CodeSample(
    val language: String,
    val code: String,
)

/**
 * Collection of code samples used to showcase syntax highlighting across multiple languages.
 *
 * Each [CodeSample] pairs a highlight.js language identifier with the corresponding source code
 * string, passed directly to [dev.hossain.highlight.ui.SyntaxHighlightedCode].
 *
 * The list intentionally covers a range of use-cases:
 * - Short, focused snippets (Python, Kotlin, JavaScript, Java, SQL, JSON, XML)
 * - A large, real-world Kotlin file (WeatherApp) to stress-test rendering performance
 * - An empty string edge case to verify graceful fallback
 */
internal val SAMPLES =
    listOf(
        CodeSample(
            language = "python",
            code =
                """
def fibonacci(n: int) -> int:
    # Returns the nth Fibonacci number
    if n <= 1:
        return n
    a, b = 0, 1
    for _ in range(n - 1):
        a, b = b, a + b
    return b

# Edge case: special chars  \t \n ' "
result = fibonacci(10)
print(f"Result: {result}")
                """.trimIndent(),
        ),
        CodeSample(
            language = "kotlin",
            code =
                """
data class User(val name: String, val age: Int)

fun List<User>.filter(minAge: Int): List<User> =
    filter { it.age >= minAge }

// Unicode: héllo wörld 🌍
val users = listOf(
    User("Alice", 30),
    User("Bob", 25),
)

val adults = users.filter(18)
println(adults)
                """.trimIndent(),
        ),
        CodeSample(
            language = "javascript",
            code =
                """
async function fetchUser(id) {
    const response = await fetch(`/api/users/${'$'}{id}`);
    if (!response.ok) {
        throw new Error(`HTTP error: ${'$'}{response.status}`);
    }
    return response.json();
}

// Backslash path: C:\Users\test
const path = 'C:\\Users\\test\\file.txt';
                """.trimIndent(),
        ),
        CodeSample(
            language = "java",
            code =
                """
public class BinarySearch {
    public static int search(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) return mid;
            if (arr[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }
}
                """.trimIndent(),
        ),
        CodeSample(
            language = "sql",
            code =
                """
SELECT
    u.id,
    u.name,
    COUNT(o.id) AS order_count,
    SUM(o.total) AS revenue
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
WHERE u.created_at >= '2024-01-01'
GROUP BY u.id, u.name
HAVING order_count > 0
ORDER BY revenue DESC
LIMIT 10;
                """.trimIndent(),
        ),
        CodeSample(
            language = "json",
            code =
                """
{
    "name": "compose-highlight",
    "version": "0.1.0",
    "dependencies": {
        "highlight.js": "^11.11.1"
    },
    "keywords": ["android", "compose", "syntax-highlight"],
    "unicode": "héllo 🌍",
    "escapes": "line1\nline2\ttabbed"
}
                """.trimIndent(),
        ),
        CodeSample(
            language = "xml",
            code =
                """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Compose Highlight</string>
    <style name="Theme.App" parent="Theme.Material3.DayNight">
        <item name="colorPrimary">@color/purple_500</item>
    </style>
</resources>
                """.trimIndent(),
        ),
        // Large real-world Kotlin file — WeatherApp from ZacSweers/metro samples.
        // Source: https://github.com/ZacSweers/metro/blob/main/samples/weather-app/src/commonMain/kotlin/dev/zacsweers/metro/sample/weather/WeatherApp.kt
        CodeSample(
            language = "kotlin (large — WeatherApp)",
            code =
                """
// Copyright (C) 2025 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.metro.sample.weather

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import dev.zacsweers.metro.Inject
import kotlin.time.Instant
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Inject
class WeatherApp(private val repository: WeatherRepository) {
  suspend operator fun invoke(query: String, log: (String, isError: Boolean) -> Unit) {
    byLocation(query)
      .onSuccess { weather ->
        val location = weather.location
        val message = buildString {
          appendLine("Weather for ${'$'}{location.name}, ${'$'}{location.region ?: location.country}:")

          val current = weather.current
          appendLine("\nCurrent conditions:")
          appendLine("Temperature: ${'$'}{current.temperature}°C")
          appendLine("Humidity: ${'$'}{current.humidity}%")
          appendLine("Wind Speed: ${'$'}{current.windSpeed} km/h")
          appendLine("Description: ${'$'}{current.description}")

          appendLine("\nHourly forecast:")

          val hourlyTable = formatHourlyForecast(weather.hourlyForecast)
          appendLine(hourlyTable)
        }
        log(message, false)
      }
      .onFailure { error -> log("Error fetching weather: ${'$'}{error.message}", true) }
  }

  private suspend fun byLocation(query: String): Result<WeatherInfo> = coroutineScope {
    try {
      val locations = repository.searchLocation(query).getOrThrow()
      if (locations.isEmpty()) {
        Result.failure(NoSuchElementException("Location not found: ${'$'}query"))
      } else {
        val location = locations.first()
        val weather = repository.getWeather(location.latitude, location.longitude).getOrThrow()

        Result.success(
          WeatherInfo(
            location =
              LocationInfo(
                name = location.name,
                region = location.region,
                country = location.country,
              ),
            current =
              CurrentWeatherInfo(
                temperature = weather.current.temperature,
                humidity = weather.current.humidity,
                windSpeed = weather.current.windSpeed,
                description = getWeatherDescription(weather.current.weatherCode),
              ),
            hourlyForecast =
              weather.hourly.time.zip(
                weather.hourly.temperatures.zip(weather.hourly.weatherCodes)
              ) { time, (temp, code) ->
                HourlyForecastInfo(
                  time = LocalDateTime.parse(time).toInstant(TimeZone.UTC),
                  temperature = temp,
                  description = getWeatherDescription(code),
                )
              },
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun getWeatherDescription(code: Int): String =
    when (code) {
      0 -> "Clear sky"
      1, 2, 3 -> "Partly cloudy"
      45, 48 -> "Foggy"
      51, 53, 55 -> "Drizzle"
      61, 63, 65 -> "Rain"
      71, 73, 75 -> "Snow"
      77 -> "Snow grains"
      80, 81, 82 -> "Rain showers"
      85, 86 -> "Snow showers"
      95 -> "Thunderstorm"
      96, 99 -> "Thunderstorm with hail"
      else -> "Unknown"
    }

  private fun formatHourlyForecast(forecast: List<HourlyForecastInfo>): String {
    return table {
        cellStyle {
          border = true
          alignment = TextAlignment.MiddleCenter
        }

        header {
          row {
            cell("Time") { alignment = TextAlignment.MiddleCenter }
            cell("Temperature")
            cell("Conditions") { alignment = TextAlignment.MiddleCenter }
          }
        }

        forecast.take(24).forEach { hour ->
          val localTime = hour.time.toLocalDateTime(TimeZone.currentSystemDefault())
          val timeStr =
            "${'$'}{localTime.hour.toString().padStart(2, '0')}:${'$'}{
          localTime.minute.toString().padStart(2, '0')
        }"

          row {
            cell(timeStr)
            cell("${'$'}{hour.temperature}°C")
            cell(hour.description)
          }
        }
      }
      .toString()
  }
}

data class WeatherInfo(
  val location: LocationInfo,
  val current: CurrentWeatherInfo,
  val hourlyForecast: List<HourlyForecastInfo>,
)

data class LocationInfo(val name: String, val region: String?, val country: String)

data class CurrentWeatherInfo(
  val temperature: Double,
  val humidity: Double,
  val windSpeed: Double,
  val description: String,
)

data class HourlyForecastInfo(val time: Instant, val temperature: Double, val description: String)
                """.trimIndent(),
        ),
        CodeSample(language = "plaintext", code = ""), // empty edge case
    )

/** A named pair of light/dark [HighlightTheme]s for the theme picker. */
internal data class ThemePair(
    val name: String,
    val light: HighlightTheme,
    val dark: HighlightTheme,
)
