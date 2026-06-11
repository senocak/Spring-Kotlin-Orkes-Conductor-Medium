package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class ReleaseInventoryWorker : Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "release_inventory"

    override fun execute(task: Task): TaskResult {
        val orderId: String = task.requiredString(key = "orderId") ?: return failed(reason = "Missing 'orderId' input")
        val reservationId: String = task.optionalString(key = "reservationId", defaultValue = "unknown")
        val reason: String = task.optionalString(key = "reason", defaultValue = "Fulfillment cancelled")
        log.info("release_inventory: orderId=$orderId reservationId=$reservationId reason=$reason")
        return completed(
            task = task,
            output = mapOf(
                "inventoryReleased" to true,
                "reason" to reason
            )
        )
    }
}
