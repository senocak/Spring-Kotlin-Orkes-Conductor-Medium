package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import java.util.UUID
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class ProcessPaymentWorker : Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "process_payment"

    override fun execute(task: Task): TaskResult {
        val orderId: String = task.requiredString(key = "orderId") ?: return failed(reason = "Missing 'orderId' input")
        val customerId: String = task.requiredString(key = "customerId") ?: return failed(reason = "Missing 'customerId' input")
        val totalAmount: Double = task.numberInput(key = "totalAmount") ?: return failed(reason = "Missing or invalid 'totalAmount' input")
        val paymentMethod: String = task.optionalString(key = "paymentMethod", defaultValue = "CARD")
        val paymentDeclined: Boolean = paymentMethod.equals(other = "DECLINED", ignoreCase = true) ||
                paymentMethod.equals("FAIL", ignoreCase = true) ||
                paymentMethod.equals("INVALID_CARD", ignoreCase = true)

        val paymentStatus: String = if (paymentDeclined) "DECLINED" else "APPROVED"
        val transactionId: String? = if (paymentStatus == "APPROVED") "txn-${UUID.randomUUID()}" else null
        val reason: String = if (paymentStatus == "APPROVED") "Payment authorized" else "Payment authorization declined"

        log.info("process_payment: orderId=$orderId customerId=$customerId amount=$totalAmount status=$paymentStatus")
        return completed(
            task = task,
            output = mapOf(
                "paymentStatus" to paymentStatus,
                "transactionId" to transactionId,
                "reason" to reason
            )
        )
    }
}
