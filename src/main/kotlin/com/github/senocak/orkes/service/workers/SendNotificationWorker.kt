package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service(value = "SendNotification")
class SendNotificationWorker: Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "send_notification"

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
}
