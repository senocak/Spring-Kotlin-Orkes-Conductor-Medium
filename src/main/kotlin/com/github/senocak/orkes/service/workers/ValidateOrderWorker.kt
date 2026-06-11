package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class ValidateOrderWorker : Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "validate_order"

    override fun execute(task: Task): TaskResult {
        val orderId: String = task.requiredString(key = "orderId") ?: return failed(reason = "Missing 'orderId' input")
        val customerId: String = task.requiredString(key = "customerId") ?: return failed(reason = "Missing 'customerId' input")
        val deliveryAddress: String = task.requiredString(key = "deliveryAddress") ?: return failed(reason = "Missing 'deliveryAddress' input")
        val totalAmount: Double = task.numberInput(key = "totalAmount") ?: return failed(reason = "Missing or invalid 'totalAmount' input")
        val items: List<Any> = task.itemsInput()
        if (items.isEmpty()) {
            return failed(reason = "Order must contain at least one item")
        }
        if (totalAmount <= 0.0) {
            return failed(reason = "Order totalAmount must be greater than zero")
        }
        log.info("validate_order: orderId=$orderId customerId=$customerId totalAmount=$totalAmount itemCount=${items.size}")
        return completed(
            task = task,
            output = mapOf(
                "orderId" to orderId,
                "customerId" to customerId,
                "items" to items,
                "itemCount" to items.size,
                "totalAmount" to totalAmount,
                "deliveryAddress" to deliveryAddress,
                "validated" to true
            )
        )
    }
}