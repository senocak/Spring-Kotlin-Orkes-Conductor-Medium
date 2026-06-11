package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ReserveInventoryWorker : Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "reserve_inventory"

    override fun execute(task: Task): TaskResult {
        val orderId: String = task.requiredString("orderId") ?: return failed("Missing 'orderId' input")
        val items: List<Any> = task.itemsInput()
        val unavailableItems: List<Any> = items.filter { item: Any? ->
            val itemMap: Map<*, *>? = item as? Map<*, *>
            itemMap?.get("available") == false || itemMap?.get("sku")?.toString()?.equals("OUT_OF_STOCK", ignoreCase = true) == true
        }
        if (unavailableItems.isNotEmpty()) {
            return failed(reason = "Inventory unavailable for orderId=$orderId items=$unavailableItems")
        }
        val reservationId = "res-${UUID.randomUUID()}"
        log.info("reserve_inventory: orderId=$orderId reservationId=$reservationId itemCount=${items.size}")
        return completed(
            task = task,
            output = mapOf(
                "inventoryReserved" to true,
                "reservationId" to reservationId,
                "unavailableItems" to emptyList<Any>()
            )
        )
    }
}


