package com.github.senocak.orkes

import com.github.senocak.orkes.config.ConductorProperties
import com.github.senocak.orkes.service.WorkflowDefinition1
import com.github.senocak.orkes.service.WorkflowDefinition2
import com.netflix.conductor.client.http.WorkflowClient
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

fun main(args: Array<String>) {
    runApplication<SpringKotlinConductorApplication>(*args)
}

fun <R : Any> R.logger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger((if (javaClass.kotlin.isCompanion) javaClass.enclosingClass else javaClass).name)
}

@SpringBootApplication
@ConfigurationPropertiesScan
@RestController
@RequestMapping(value = ["/api/v1"])
class SpringKotlinConductorApplication(
    private val workflowDefinition1: WorkflowDefinition1,
    private val workflowDefinition2: WorkflowDefinition2,
    private val conductorProperties: ConductorProperties,
    private val workflowClient: WorkflowClient
) {
    @EventListener(value = [ApplicationReadyEvent::class])
    fun setup() {
        //workflowDefinition1.loadWorkflowsAndTasks(url = conductorProperties.url)
        workflowDefinition2.loadWorkflowsAndTasks(url = conductorProperties.url)
    }

    @PostMapping(value = ["/orders"])
    fun orderFulfillmentTrigger(@RequestBody request: OrderWorkflowRequest): String =
        triggerWorkflowByNameAndInput(workflowName = "order_fulfillment_workflow", input = request.toWorkflowInput())

    private fun triggerWorkflowByNameAndInput(workflowName: String, input: Map<String, Any?>): String =
        StartWorkflowRequest()
            .also { it: StartWorkflowRequest ->
                it.input = input
                it.withName(workflowName)
            }
            .run { workflowClient.startWorkflow(this) }
}

data class OrderWorkflowRequest(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val deliveryAddress: String,
    val paymentMethod: String = "CARD",
    val priority: String = "STANDARD",
    val notificationChannel: String = "EMAIL"
) {
    fun toWorkflowInput(): Map<String, Any?> =
        mapOf(
            "orderId" to orderId,
            "customerId" to customerId,
            "items" to items,
            "totalAmount" to totalAmount,
            "deliveryAddress" to deliveryAddress,
            "paymentMethod" to paymentMethod,
            "priority" to priority,
            "notificationChannel" to notificationChannel
        )
}

data class OrderItem(
    val sku: String,
    val quantity: Int,
    val price: Double,
    val available: Boolean = true
)
