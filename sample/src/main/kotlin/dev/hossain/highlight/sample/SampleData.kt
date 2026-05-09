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
 * - Systems / low-level languages (Rust, C++, Go) showcasing templates, lifetimes, goroutines
 * - Typed languages (TypeScript, Swift, C#) showcasing generics, protocols, LINQ, async/await
 * - Shell script (Bash) showcasing variables, functions, case statements
 * - Stylesheet (CSS) showcasing selectors, variables, media queries, keyframes
 * - A large, real-world Kotlin file (WeatherApp) to stress-test rendering performance
 * - An empty string edge case to verify graceful fallback
 */
internal val CODE_SAMPLES =
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
    "string": "Hello, World!",
    "emptyString": "",
    "unicodeString": "héllo 🌍 日本語",
    "escapedString": "line1\nline2\ttabbed \"quoted\" and backslash \\",
    "integer": 42,
    "negativeInteger": -7,
    "float": 3.14159,
    "negativeFloat": -0.001,
    "scientificNotation": 1.5e10,
    "largeNumber": 9007199254740991,
    "booleanTrue": true,
    "booleanFalse": false,
    "nullValue": null,
    "emptyArray": [],
    "stringArray": ["android", "compose", "kotlin"],
    "numberArray": [1, 2, 3, 4, 5],
    "mixedArray": [1, "two", true, null, 3.0],
    "nestedArray": [[1, 2], [3, 4], [5, 6]],
    "emptyObject": {},
    "nestedObject": {
        "id": 101,
        "name": "compose-highlight",
        "version": "0.3.0",
        "stable": true,
        "deprecated": null,
        "tags": ["library", "ui", "syntax"],
        "metadata": {
            "author": "hossain-khan",
            "license": "Apache-2.0",
            "stars": 42
        }
    },
    "arrayOfObjects": [
        { "lang": "kotlin", "highlight": true },
        { "lang": "python", "highlight": true },
        { "lang": "cobol", "highlight": false }
    ]
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
        CodeSample(
            language = "typescript",
            code =
                """
interface Repository<T extends { id: number }> {
  findById(id: number): Promise<T | null>;
  save(entity: Omit<T, 'id'>): Promise<T>;
  delete(id: number): Promise<void>;
}

type ApiResponse<T> = {
  data: T;
  status: number;
  message?: string;
};

async function fetchWithRetry<T>(
  url: string,
  retries = 3,
): Promise<ApiResponse<T>> {
  for (let attempt = 0; attempt < retries; attempt++) {
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${'$'}{res.status}: ${'$'}{res.statusText}`);
      const data: T = await res.json();
      return { data, status: res.status };
    } catch (err) {
      if (attempt === retries - 1) throw err;
      await new Promise(r => setTimeout(r, 2 ** attempt * 100));
    }
  }
  throw new Error('unreachable');
}
                """.trimIndent(),
        ),
        CodeSample(
            language = "rust",
            code =
                """
use std::collections::HashMap;

#[derive(Debug, Clone)]
pub struct LruCache<V> {
    store: HashMap<String, V>,
    capacity: usize,
}

impl<V> LruCache<V> {
    pub fn new(capacity: usize) -> Self {
        Self { store: HashMap::with_capacity(capacity), capacity }
    }

    pub fn get(&self, key: &str) -> Option<&V> {
        self.store.get(key)
    }

    /// Inserts a value; returns the old value if the key existed.
    pub fn insert(&mut self, key: String, value: V) -> Result<Option<V>, &'static str> {
        if self.store.len() >= self.capacity && !self.store.contains_key(&key) {
            return Err("cache full — eviction not implemented");
        }
        Ok(self.store.insert(key, value))
    }
}

fn main() {
    let mut cache: LruCache<i32> = LruCache::new(4);
    cache.insert("answer".to_owned(), 42).unwrap();
    match cache.get("answer") {
        Some(v) => println!("Found: {v}"),
        None    => println!("Cache miss"),
    }
}
                """.trimIndent(),
        ),
        CodeSample(
            language = "go",
            code =
                """
package main

import (
    "context"
    "fmt"
    "sync"
    "time"
)

type Result struct {
    URL      string
    Duration time.Duration
    Err      error
}

func fetchAll(ctx context.Context, urls []string) []Result {
    var mu sync.Mutex
    results := make([]Result, 0, len(urls))
    var wg sync.WaitGroup

    for _, url := range urls {
        wg.Add(1)
        go func(u string) {
            defer wg.Done()
            start := time.Now()
            select {
            case <-ctx.Done():
                mu.Lock()
                results = append(results, Result{URL: u, Err: ctx.Err()})
                mu.Unlock()
            case <-time.After(50 * time.Millisecond):
                mu.Lock()
                results = append(results, Result{URL: u, Duration: time.Since(start)})
                mu.Unlock()
            }
        }(url)
    }
    wg.Wait()
    return results
}

func main() {
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()
    urls := []string{"https://example.com", "https://golang.org", "https://pkg.go.dev"}
    for _, r := range fetchAll(ctx, urls) {
        if r.Err != nil {
            fmt.Printf("ERR  %s: %v\n", r.URL, r.Err)
        } else {
            fmt.Printf("OK   %s  (%v)\n", r.URL, r.Duration)
        }
    }
}
                """.trimIndent(),
        ),
        CodeSample(
            language = "swift",
            code =
                """
import Foundation

protocol Fetchable {
    associatedtype Resource: Decodable
    var baseURL: URL { get }
    func fetch(id: Int) async throws -> Resource
}

struct User: Decodable, Identifiable {
    let id: Int
    let name: String
    let email: String?
}

enum NetworkError: LocalizedError {
    case badStatus(Int)
    case decodingFailed(Error)

    var errorDescription: String? {
        switch self {
        case .badStatus(let code): return "Server returned HTTP \(code)"
        case .decodingFailed(let e): return "Decode error: \(e.localizedDescription)"
        }
    }
}

struct UserService: Fetchable {
    typealias Resource = User
    let baseURL = URL(string: "https://api.example.com")!

    func fetch(id: Int) async throws -> User {
        let url = baseURL.appendingPathComponent("users/\(id)")
        let (data, response) = try await URLSession.shared.data(from: url)
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode) else {
            throw NetworkError.badStatus((response as? HTTPURLResponse)?.statusCode ?? -1)
        }
        do {
            return try JSONDecoder().decode(User.self, from: data)
        } catch {
            throw NetworkError.decodingFailed(error)
        }
    }
}
                """.trimIndent(),
        ),
        CodeSample(
            language = "cpp",
            code =
                """
#include <algorithm>
#include <iostream>
#include <memory>
#include <stdexcept>
#include <vector>

template <typename T>
class Stack {
public:
    void push(T value) { data_.push_back(std::move(value)); }

    T pop() {
        if (data_.empty()) throw std::underflow_error("Stack is empty");
        T top = std::move(data_.back());
        data_.pop_back();
        return top;
    }

    [[nodiscard]] bool empty() const noexcept { return data_.empty(); }
    [[nodiscard]] std::size_t size() const noexcept { return data_.size(); }

private:
    std::vector<T> data_;
};

int main() {
    auto s = std::make_unique<Stack<int>>();
    for (int i : {3, 1, 4, 1, 5, 9, 2, 6}) s->push(i);

    std::cout << "Size: " << s->size() << "\nPopping: ";
    while (!s->empty()) std::cout << s->pop() << ' ';
    std::cout << '\n';

    // Lambda + algorithm example
    std::vector<int> nums = {5, 3, 8, 1, 9, 2};
    std::sort(nums.begin(), nums.end(), [](int a, int b) { return a > b; });
    for (auto n : nums) std::cout << n << ' ';
    return 0;
}
                """.trimIndent(),
        ),
        CodeSample(
            language = "csharp",
            code =
                """
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

public record Product(int Id, string Name, decimal Price, int Stock);

public class ProductService(IEnumerable<Product> catalog)
{
    public IEnumerable<Product> Search(string query, decimal? maxPrice = null) =>
        catalog
            .Where(p => p.Name.Contains(query, StringComparison.OrdinalIgnoreCase))
            .Where(p => maxPrice is null || p.Price <= maxPrice)
            .OrderBy(p => p.Price);

    public async Task<Product?> FindCheapestAsync(string query)
    {
        await Task.Delay(10); // simulate DB round-trip
        return Search(query).FirstOrDefault();
    }
}

var catalog = new List<Product>
{
    new(1, "Kotlin Book",    29.99m, 100),
    new(2, "Rust in Action", 34.99m,  50),
    new(3, "Go Programming", 24.99m,  75),
};

var svc = new ProductService(catalog);
var hit = await svc.FindCheapestAsync("kotlin");
Console.WriteLine(hit is { } p
    ? ${'$'}"Found: {p.Name} at ${'$'}{p.Price:C}"
    : "Not found");
                """.trimIndent(),
        ),
        CodeSample(
            language = "bash",
            code =
                """
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="${'$'}(cd "${'$'}(dirname "${'$'}{BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="${'$'}{SCRIPT_DIR}/build"

log()  { printf '[%s] %s\n' "${'$'}(date +%T)" "${'$'}*"; }
fail() { log "ERROR: ${'$'}*" >&2; exit 1; }

setup() {
    mkdir -p "${'$'}BUILD_DIR"
    [[ -f "gradle.properties" ]] || fail "Not a Gradle project"
    log "Setup done → ${'$'}BUILD_DIR"
}

run_tests() {
    local filter="${'$'}{1:-}"
    if [[ -n "${'$'}filter" ]]; then
        ./gradlew test --tests "${'$'}filter"
    else
        ./gradlew test
    fi
    log "Tests complete ✓"
}

publish() {
    local version="${'$'}{1:?version required (e.g. 1.2.3)}"
    [[ "${'$'}version" =~ ^[0-9]+\.[0-9]+\.[0-9]+${'$'} ]] \
        || fail "Invalid semver: ${'$'}version"
    ./gradlew publishToMavenLocal -Pversion="${'$'}version"
    git tag "${'$'}version" && git push origin "${'$'}version"
    log "Published ${'$'}version"
}

case "${'$'}{1:-help}" in
    setup)   setup ;;
    test)    run_tests "${'$'}{2:-}" ;;
    publish) publish "${'$'}{2:-}" ;;
    clean)   rm -rf "${'$'}BUILD_DIR" && log "Cleaned." ;;
    *)       echo "Usage: ${'$'}0 {setup|test|publish <ver>|clean}" ;;
esac
                """.trimIndent(),
        ),
        CodeSample(
            language = "css",
            code =
                """
:root {
    --color-primary:  #6200ea;
    --color-surface:  #ffffff;
    --color-on-surface: #1c1b1f;
    --radius-md: 8px;
    --transition: 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

@media (prefers-color-scheme: dark) {
    :root {
        --color-primary:    #bb86fc;
        --color-surface:    #121212;
        --color-on-surface: #e6e1e5;
    }
}

.card {
    background: var(--color-surface);
    border-radius: var(--radius-md);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
    color: var(--color-on-surface);
    padding: 1.5rem;
    transition: transform var(--transition), box-shadow var(--transition);
}

.card:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.18);
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(6px); }
    to   { opacity: 1; transform: translateY(0);   }
}

.card--animated {
    animation: fadeIn 0.35s var(--transition) both;
}

.card__title {
    color: var(--color-primary);
    font-size: clamp(1rem, 2.5vw, 1.5rem);
    font-weight: 600;
    letter-spacing: 0.01em;
    margin: 0 0 0.5rem;
}
                """.trimIndent(),
        ),
        CodeSample(language = "plaintext", code = "This is plaintext.\nNothing to highlight."),
    )

/** A named pair of light/dark [HighlightTheme]s for the theme picker. */
internal data class ThemePair(
    val name: String,
    val light: HighlightTheme,
    val dark: HighlightTheme,
)
