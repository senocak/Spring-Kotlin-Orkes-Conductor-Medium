package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class SendOrderNotificationWorker : Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "send_order_notification"

    override fun execute(task: Task): TaskResult {
        val orderId: String = task.requiredString(key = "orderId") ?: return failed(reason = "Missing 'orderId' input")
        val customerId: String = task.requiredString(key = "customerId") ?: return failed(reason = "Missing 'customerId' input")
        val channel: String = task.optionalString(key = "channel", defaultValue = "EMAIL")
        val status: String = task.optionalString(key = "status", defaultValue = "UPDATED")
        val message: String = task.optionalString(key = "message", defaultValue = "Order $orderId status changed to $status")
        log.info("send_order_notification: orderId=$orderId customerId=$customerId channel=$channel status=$status message=$message")
        return completed(
            task = task,
            output = mapOf(
                "notified" to true,
                "channel" to channel,
                "message" to message
            )
        )
    }
}
