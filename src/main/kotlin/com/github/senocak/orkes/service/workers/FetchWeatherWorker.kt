package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service(value = "FetchWeather")
class FetchWeatherWorker(
    private val restTemplate: RestTemplate
): Worker {
    private val log: Logger by logger()
    private val API_URL = "http://api.weatherstack.com/current"

    override fun getTaskDefName(): String = "fetch_weather"

    override fun execute(task: Task): TaskResult {
        val location: String? = (task.inputData["location"] as? String) ?: "Istanbul,TR"
        if (location.isNullOrBlank()) {
            return TaskResult.failed().also { it.reasonForIncompletion = "Missing 'location' input" }
        }
        return try {
            val response: WeatherResponse? = restTemplate.getForObject("$API_URL?access_key=46aa57b9789c0b758c19e10e06fdea04&query=$location",
                WeatherResponse::class.java)
            log.info("Weather information for $location: $response")

            val tr = TaskResult(task)
            tr.outputData = mapOf(
                "location" to location,
                "weather" to response
            )
            tr.status = TaskResult.Status.COMPLETED
            tr
        } catch (e: Exception) {
            log.error("Failed to fetch weather for location=$location", e)
            TaskResult.failed().also { it.reasonForIncompletion = e.message ?: "Unknown error" }
        }
    }
}

data class WeatherResponse(
    val request: Request? = null,
    val location: Location? = null,
    val current: Current? = null,
    val success: Boolean? = null,
    val error: Error? = null,
)

data class Error(
    val code: Int,
    val type: String,
    val info: String,
)

data class Request(
    val type: String,
    val query: String,
    val language: String,
    val unit: String
)

data class Location(
    val name: String,
    val country: String,
    val region: String,
    val lat: String,
    val lon: String,
    val timezone_id: String,
    val localtime: String,
    val localtime_epoch: Long,
    val utc_offset: String
)

data class Current(
    val observation_time: String,
    val temperature: Int,
    val weather_code: Int,
    val weather_icons: List<String>,
    val weather_descriptions: List<String>,
    val wind_speed: Int,
    val wind_degree: Int,
    val wind_dir: String,
    val pressure: Int,
    val precip: Int,
    val humidity: Int,
    val cloudcover: Int,
    val feelslike: Int,
    val uv_index: Int,
    val visibility: Int,
    val is_day: String
)
