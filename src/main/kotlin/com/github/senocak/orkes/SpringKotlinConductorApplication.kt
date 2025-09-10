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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
        workflowDefinition1.loadWorkflowsAndTasks(url = conductorProperties.url)
        workflowDefinition2.loadWorkflowsAndTasks(url = conductorProperties.url)
    }

    @GetMapping(value = ["/helloworld/{name}"])
    fun helloWorldTrigger(@PathVariable name: String): String =
        triggerWorkflowByNameAndInput(workflowName = "hello_world_workflow", input = mapOf("name" to name))

    @GetMapping(value = ["/movie/{movieType}/{movieId}"])
    fun movieTrigger(@PathVariable movieType: String, @PathVariable movieId: String): String =
        triggerWorkflowByNameAndInput(workflowName = "decision_workflow",
            input = mapOf("movieType" to movieType, "movieId" to movieId))

    @GetMapping(value = ["/weather/{location}"])
    fun weatherTrigger(@PathVariable location: String): String =
        triggerWorkflowByNameAndInput(workflowName = "weather_workflow", input = mapOf("location" to location))

    private fun triggerWorkflowByNameAndInput(workflowName: String, input: Map<String, Any?>): String =
        StartWorkflowRequest()
            .also {
                it.input = input
                it.withName(workflowName)
            }
            .run { workflowClient.startWorkflow(this) }
}
