package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import java.util.UUID
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class ScheduleDeliveryWorker : Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "schedule_delivery"

    override fun execute(task: Task): TaskResult {
        val orderId: String = task.requiredString(key = "orderId") ?: return failed(reason = "Missing 'orderId' input")
        val deliveryAddress: String = task.requiredString(key = "deliveryAddress") ?: return failed(reason = "Missing 'deliveryAddress' input")
        val priority: String = task.optionalString(key = "priority", defaultValue = "STANDARD")
        val etaMinutes: Int = if (priority.equals(other = "EXPRESS", ignoreCase = true)) 30 else 90
        val deliveryId = "del-${UUID.randomUUID()}"
        log.info("schedule_delivery: orderId=$orderId deliveryId=$deliveryId etaMinutes=$etaMinutes address=$deliveryAddress")
        return completed(
            task = task,
            output = mapOf(
                "deliveryId" to deliveryId,
                "etaMinutes" to etaMinutes,
                "courier" to "demo-courier"
            )
        )
    }
}
