package dev.composehighlight.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.composehighlight.engine.HighlightEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Microbenchmarks for the full [HighlightEngine] pipeline.
 *
 * Measures the end-to-end cost of a single highlight call: JS execution in the hidden
 * WebView, JSON-unescaping the result, and returning the raw HTML string.
 *
 * The WebView is initialized once in [@Before] to avoid counting cold-start overhead —
 * use [HighlightEngineWarmupBenchmark] for measuring first-call latency if needed.
 *
 * Note: these benchmarks include async IPC overhead (instrumentation thread → Main thread
 * → WebView → back), so individual iterations are slower than [ThemeParserBenchmark] and
 * [HtmlToAnnotatedStringBenchmark]. The value is tracking regressions in JS execution speed.
 *
 * Run with:
 *   ./gradlew :compose-highlight:connectedAndroidTest -P android.testInstrumentationRunnerArguments.class=dev.composehighlight.benchmark.HighlightEngineBenchmark
 */
@RunWith(AndroidJUnit4::class)
class HighlightEngineBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var engine: HighlightEngine

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        engine = HighlightEngine(context)
        // Pre-warm WebView so benchmark iterations measure steady-state performance
        runBlocking { engine.initialize() }
    }

    @After
    fun tearDown() {
        engine.destroy()
    }

    @Test
    fun highlightPythonToHtml() = benchmarkRule.measureRepeated {
        runBlocking {
            engine.highlightToHtml(PYTHON_CODE, "python").getOrThrow()
        }
    }

    @Test
    fun highlightKotlinToHtml() = benchmarkRule.measureRepeated {
        runBlocking {
            engine.highlightToHtml(KOTLIN_CODE, "kotlin").getOrThrow()
        }
    }

    @Test
    fun highlightSqlToHtml() = benchmarkRule.measureRepeated {
        runBlocking {
            engine.highlightToHtml(SQL_CODE, "sql").getOrThrow()
        }
    }

    @Test
    fun highlightLargeKotlinWeatherAppToHtml() = benchmarkRule.measureRepeated {
        runBlocking {
            engine.highlightToHtml(WEATHER_APP_KOTLIN_CODE, "kotlin").getOrThrow()
        }
    }

    @Test
    fun highlightLargeTypeScriptZodCoreToHtml() = benchmarkRule.measureRepeated {
        runBlocking {
            engine.highlightToHtml(ZOD_CORE_TS_CODE, "typescript").getOrThrow()
        }
    }

    companion object {
        private val PYTHON_CODE = """
            def fibonacci(n: int) -> int:
                # Returns the nth Fibonacci number
                if n <= 1:
                    return n
                a, b = 0, 1
                for _ in range(n - 1):
                    a, b = b, a + b
                return b

            result = fibonacci(10)
            print(f"Result: {result}")
        """.trimIndent()

        private val KOTLIN_CODE = """
            data class User(val name: String, val age: Int)

            fun List<User>.filterAdults(minAge: Int): List<User> =
                filter { it.age >= minAge }

            val users = listOf(
                User("Alice", 30),
                User("Bob", 25),
            )
            val adults = users.filterAdults(18)
            println(adults)
        """.trimIndent()

        private val SQL_CODE = """
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
        """.trimIndent()

        // Large real-world Kotlin file (~150 lines) from ZacSweers/metro weather sample.
        // Used to benchmark JS highlighting performance on a substantial, realistic input.
        // Source: https://github.com/ZacSweers/metro/blob/main/samples/weather-app/src/commonMain/kotlin/dev/zacsweers/metro/sample/weather/WeatherApp.kt
        private val WEATHER_APP_KOTLIN_CODE = """
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
                        location = LocationInfo(name = location.name, region = location.region, country = location.country),
                        current = CurrentWeatherInfo(
                          temperature = weather.current.temperature,
                          humidity = weather.current.humidity,
                          windSpeed = weather.current.windSpeed,
                          description = getWeatherDescription(weather.current.weatherCode),
                        ),
                        hourlyForecast = weather.hourly.time.zip(
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
                    cellStyle { border = true; alignment = TextAlignment.MiddleCenter }
                    header { row { cell("Time"); cell("Temperature"); cell("Conditions") } }
                    forecast.take(24).forEach { hour ->
                      val localTime = hour.time.toLocalDateTime(TimeZone.currentSystemDefault())
                      val timeStr = "${'$'}{localTime.hour.toString().padStart(2, '0')}:${'$'}{localTime.minute.toString().padStart(2, '0')}"
                      row { cell(timeStr); cell("${'$'}{hour.temperature}°C"); cell(hour.description) }
                    }
                  }.toString()
              }
            }

            data class WeatherInfo(val location: LocationInfo, val current: CurrentWeatherInfo, val hourlyForecast: List<HourlyForecastInfo>)
            data class LocationInfo(val name: String, val region: String?, val country: String)
            data class CurrentWeatherInfo(val temperature: Double, val humidity: Double, val windSpeed: Double, val description: String)
            data class HourlyForecastInfo(val time: Instant, val temperature: Double, val description: String)
        """.trimIndent()

        // Large real-world TypeScript file (~200 lines) from colinhacks/zod v4 core.
        // Used to benchmark JS highlighting performance on a TypeScript-heavy input with
        // generics, decorators, and complex type expressions.
        // Source: https://github.com/colinhacks/zod/blob/main/packages/zod/src/v4/core/core.ts
        private val ZOD_CORE_TS_CODE = """
            import type * as errors from "./errors.js";
            import type * as schemas from "./schemas.js";
            import type { Class } from "./util.js";

            type ZodTrait = { _zod: { def: any; [k: string]: any } };
            export interface ${'$'}constructor<T extends ZodTrait, D = T["_zod"]["def"]> {
              new (def: D): T;
              init(inst: T, def: D): asserts inst is T;
            }

            /** A special constant with type `never` */
            export const NEVER: never = Object.freeze({ status: "aborted" }) as never;

            export function ${'$'}constructor<T extends ZodTrait, D = T["_zod"]["def"]>(
              name: string,
              initializer: (inst: T, def: D) => void,
              params?: { Parent?: typeof Class }
            ): ${'$'}constructor<T, D> {
              function init(inst: T, def: D) {
                if (!inst._zod) {
                  Object.defineProperty(inst, "_zod", {
                    value: { def, constr: _, traits: new Set() },
                    enumerable: false,
                  });
                }
                if (inst._zod.traits.has(name)) return;
                inst._zod.traits.add(name);
                initializer(inst, def);
                const proto = _.prototype;
                const keys = Object.keys(proto);
                for (let i = 0; i < keys.length; i++) {
                  const k = keys[i]!;
                  if (!(k in inst)) (inst as any)[k] = proto[k].bind(inst);
                }
              }

              const Parent = params?.Parent ?? Object;
              class Definition extends Parent {}
              Object.defineProperty(Definition, "name", { value: name });

              function _(this: any, def: D) {
                const inst = params?.Parent ? new Definition() : this;
                init(inst, def);
                inst._zod.deferred ??= [];
                for (const fn of inst._zod.deferred) fn();
                return inst;
              }

              Object.defineProperty(_, "init", { value: init });
              Object.defineProperty(_, Symbol.hasInstance, {
                value: (inst: any) => {
                  if (params?.Parent && inst instanceof params.Parent) return true;
                  return inst?._zod?.traits?.has(name);
                },
              });
              Object.defineProperty(_, "name", { value: name });
              return _ as any;
            }

            export const ${'$'}brand: unique symbol = Symbol("zod_brand");
            export type ${'$'}brand<T extends string | number | symbol = string | number | symbol> = {
              [${'$'}brand]: { [k in T]: true };
            };

            export type ${'$'}ZodBranded<
              T extends schemas.SomeType,
              Brand extends string | number | symbol,
              Dir extends "in" | "out" | "inout" = "out",
            > = T & (Dir extends "inout"
              ? { _zod: { input: input<T> & ${'$'}brand<Brand>; output: output<T> & ${'$'}brand<Brand> } }
              : Dir extends "in"
                ? { _zod: { input: input<T> & ${'$'}brand<Brand> } }
                : { _zod: { output: output<T> & ${'$'}brand<Brand> } });

            export type ${'$'}ZodNarrow<T extends schemas.SomeType, Out> = T & { _zod: { output: Out } };

            export class ${'$'}ZodAsyncError extends Error {
              constructor() {
                super("Encountered Promise during synchronous parse. Use .parseAsync() instead.");
              }
            }

            export class ${'$'}ZodEncodeError extends Error {
              constructor(name: string) {
                super("Encountered unidirectional transform during encode: " + name);
                this.name = "ZodEncodeError";
              }
            }

            export type input<T> = T extends { _zod: { input: any } } ? T["_zod"]["input"] : unknown;
            export type output<T> = T extends { _zod: { output: any } } ? T["_zod"]["output"] : unknown;
            export type { output as infer };

            export interface ${'$'}ZodConfig {
              customError?: errors.${'$'}ZodErrorMap | undefined;
              localeError?: errors.${'$'}ZodErrorMap | undefined;
              jitless?: boolean | undefined;
            }

            export const globalConfig: ${'$'}ZodConfig = {};

            export function config(newConfig?: Partial<${'$'}ZodConfig>): ${'$'}ZodConfig {
              if (newConfig) Object.assign(globalConfig, newConfig);
              return globalConfig;
            }
        """.trimIndent()
    }
}
