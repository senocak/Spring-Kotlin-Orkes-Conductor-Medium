# Spring Kotlin Netflix/Orkes Conductor
Orkes Conductor is an open-source, cloud-native microservices and workflow orchestration platform. It was originally developed at Netflix as Netflix Conductor to help manage complex business processes and microservice interactions at scale. Later, Orkes (a company founded by the original creators of Conductor) built Orkes Conductor, which is a fully managed version with enterprise-grade features.

Here’s what it does in simple terms:

- 🔄 Workflow orchestration – It coordinates multiple services, APIs, and tasks into a defined workflow (like a flowchart of steps). 
- 🧩 Microservices integration – Each step in a workflow can call a microservice, run a script, trigger a human task, or execute a system operation. 
- 📊 Scalable and distributed – Built for high scale, it can run thousands (even millions) of workflows in parallel. 
- 🎛️ Event-driven – Supports event-based triggers and async communication. 
- ☁️ Cloud-native – Works on Kubernetes and integrates with cloud services, databases, queues, etc. 
- 🛠️ Developer-friendly – Provides SDKs for Java, Python, Go, etc., and APIs to define, monitor, and manage workflows.

This project demonstrates the integration of Spring Boot with Netflix/Orkes Conductor for workflow orchestration and provides a technical walk‑through of fetching weather data and sending notifications flow.

## Setup Server
Docker Compose file to run Conductor server is provided in `docker-compose.yml`. Start it with `docker-compose up -d`.

```sh
version: '3.8'
services:
  orkes_conductor:
    image: orkesio/orkes-conductor-community-standalone:1.1.12
    init: true
    ports:
      - "9090:8080"
      - "1234:5000"
```

## High‑Level Architecture: Weather ➜ Notification

- Conductor workflow: `weather_workflow` (workflow.json)
- Task 1 (SIMPLE): `fetch_weather` implemented by `FetchWeatherWorker`
- Task 2 (SIMPLE): `send_notification` implemented by `SendNotificationWorker`
- Data flow: workflow.input.location → FetchWeatherWorker.output (location, weather) → SendNotificationWorker.input

Sequence
1. Client triggers `weather_workflow` with an input `location` (e.g., "Istanbul,TR").
2. `FetchWeatherWorker` performs an HTTP GET to a public weather API and returns a structured `weather` object along with the `location`.
3. `SendNotificationWorker` formats a message from the `weather` payload and logs it (this is the stand‑in for sending emails/Slack/SMS etc.).

## Worker 1: FetchWeatherWorker.kt (fetch_weather)
Key points
- Registered as a Spring bean with `@Service("FetchWeather")` and implements `Worker`.
- `getTaskDefName()` returns `fetch_weather` (must match the Conductor task name).
- Reads an input parameter `location` from the Conductor task input. If missing/blank, the task fails with a reason.
- Uses Spring's `RestTemplate` to call the Weatherstack API (`/current`).
- Maps the HTTP response into `WeatherResponse` data classes (see file for structure: Request, Location, Current, Error).
- On success, sets `TaskResult.outputData` to:
  - `location`: the requested location string
  - `weather`: the entire parsed `WeatherResponse`
- On failure/exception, completes task with FAILED status and a meaningful `reasonForIncompletion`.

```kotlin
override fun execute(task: Task): TaskResult {
    val location: String? = (task.inputData["location"] as? String) ?: "Istanbul,TR"
    if (location.isNullOrBlank()) {
        return TaskResult.failed().also { it.reasonForIncompletion = "Missing 'location' input" }
    }
    return try {
        val response: WeatherResponse? = restTemplate.getForObject("http://api.weatherstack.com/current?access_key=$access_key&query=$location",
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
```

Note: Consider timeouts/retries at the HTTP client level and in Conductor’s task definition for resilience.

## Worker 2: SendNotificationWorker.kt (send_notification)
Key points
- `getTaskDefName()` returns `send_notification`.
- Expects these inputs (provided by workflow task input mapping):
  - `location`: String
  - `weather`: Map/JSON (the `WeatherResponse` from previous step)
  - `message` (optional): If absent, it constructs a default message using `current.temperature`.
- The worker logs the message and returns:
  - `notified`: true
  - `message`: the final message

```kotlin
override fun execute(task: Task): TaskResult {
    val location: String = task.inputData["location"] as String
    val current: LinkedHashMap<String, String>? = (task.inputData["weather"] as? LinkedHashMap<String, LinkedHashMap<String, String>>)?.get("current")
    val message: String = task.inputData["message"] as? String ?: "Weather update for ${location}: weather=${current?.get("temperature")}°C"
    log.info("send_notification: $message")
    val tr = TaskResult(task)
    tr.outputData = mapOf("notified" to true, "message" to message)
    tr.status = TaskResult.Status.COMPLETED
    return tr
}
```

Notes
- In production, replace the log call with integration to your notifier (email/SMS/Slack/Push).
- The worker tolerates absence of the explicit `message` by constructing one from the previous task’s output.

## Workflow Wiring (weather_workflow in workflow.json)
```json
{
  "name": "weather_workflow",
  "description": "Fetch current weather and send a notification",
  "version": 1,
  "schemaVersion": 2,
  "inputParameters": [
    "location"
  ],
  "tasks": [
    {
      "name": "fetch_weather",
      "taskReferenceName": "fetch_weather_ref",
      "inputParameters": {
        "location": "${workflow.input.location}"
      },
      "type": "SIMPLE"
    },
    {
      "name": "send_notification",
      "taskReferenceName": "send_notification_ref",
      "inputParameters": {
        "location": "${fetch_weather_ref.output.location}",
        "weather": "${fetch_weather_ref.output.weather}",
        "message": "Weather in ${fetch_weather_ref.output.location} is ${fetch_weather_ref.output.weather.current.temperature}°C"
      },
      "type": "SIMPLE"
    }
  ]
}
```

What to notice
- The second task reads its inputs directly from the first task’s outputs using Conductor's expression syntax.
- You can override `message` at runtime by passing a different mapping or changing task input.

## How to Run Just This Flow
1. Start Conductor server at http://localhost:9090 and run this Spring app.
2. Ensure the tasks are registered (Spring workers auto-register when using the Orkes client in this app setup).
3. Create the workflow definition by importing `workflow.json` into Conductor (UI or API).
4. Trigger the workflow with a location input.

Example request (via requests.http)
```http
GET http://localhost:8081/api/v1/weather/Istanbul,TR
```

Expected behavior
- Task fetch_weather completes with a `weather` payload (temperature etc.).
- Task send_notification logs a line containing the formatted message.

## Configuration (application.yml)
```yaml
conductor:
  url: http://localhost:9090/api/
  threadCount: 5
  timeOut: 10_000
```

## Tips for Production Hardening
- Externalize API access keys and base URLs (do not hardcode).
- Add HTTP client timeouts and retries; consider circuit breakers.
- Define Conductor task definitions with appropriate timeoutSeconds, retryCount, and backoff.
- Validate inputs early and return clear failure reasons to aid observability.
