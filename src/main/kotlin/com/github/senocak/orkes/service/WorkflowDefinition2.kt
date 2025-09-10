package com.github.senocak.orkes.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.senocak.orkes.logger
import com.github.senocak.orkes.model.SimpleTaskDefinition
import com.github.senocak.orkes.model.TaskDefinitionDto
import com.github.senocak.orkes.model.WorkflowDefinition
import com.github.senocak.orkes.model.WorkflowTask
import org.slf4j.Logger
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException

@Service
class WorkflowDefinition2(
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate
){
    private val log: Logger by logger()
    private val TASK_DEF_PATH = "/metadata/taskdefs"
    private val WORKFLOW_DEF_PATH = "/metadata/workflow"
    private val headers: HttpHeaders = HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON }

    @Throws(exceptionClasses = [IOException::class])
    fun loadWorkflowsAndTasks(url: String) {
        val taskList: MutableList<Task> = arrayListOf()
        objectMapper
            .readTree(javaClass.classLoader.getResource("task.json"))
            .map { objectMapper.treeToValue(it, SimpleTaskDefinition::class.java) }
            .forEach { node: SimpleTaskDefinition ->
                taskList.add(element = Task(name = node.name, type = "SIMPLE", jsonNode = node))
            }

        val workflowTasks: MutableList<WorkflowDefinition> = arrayListOf()
        val workflowTaskNames: MutableList<Task> = arrayListOf()
        objectMapper
            .readTree(javaClass.classLoader.getResource("workflow.json"))
            .map { objectMapper.treeToValue(it, WorkflowDefinition::class.java) }
            .forEach { jsonNode: WorkflowDefinition ->
                workflowTasks.add(jsonNode)
                jsonNode.tasks.forEach { node: WorkflowTask ->
                    workflowTaskNames.add(element = Task(name = node.name, type = node.type, jsonNode = node))
                }
            }

        workflowTaskNames.forEach { workflowTask: Task ->
            if (workflowTask.type == "SIMPLE" && !workflowTaskNames.any { wtn: Task -> taskList.any { tl: Task -> tl.name == wtn.name } }) {
                val message = "Workflow Task scan failed. Be careful about :$workflowTask"
                log.info(message)
                throw IllegalArgumentException(message)
            }
        }

        val tasks: ResponseEntity<List<TaskDefinitionDto>> = restTemplate.exchange(url + TASK_DEF_PATH, HttpMethod.GET, null,
            object : ParameterizedTypeReference<List<TaskDefinitionDto>>() {})
        if (tasks.statusCode.is2xxSuccessful && tasks.body != null) {
            tasks.body!!.forEach { conductorTask: TaskDefinitionDto ->
                val taskIterator: MutableIterator<Task> = taskList.iterator()
                while (taskIterator.hasNext()) {
                    val task: Task = taskIterator.next()
                    if (conductorTask.name == task.name) {
                        restTemplate.put(url + TASK_DEF_PATH, HttpEntity(task.jsonNode, headers))
                        taskIterator.remove()
                    }
                }
            }
            val list: ArrayList<Any> = arrayListOf()
            taskList.forEach { task: Task -> list.add(task.jsonNode!!) }
            if (list.isNotEmpty()) {
                restTemplate.postForObject(url + TASK_DEF_PATH, HttpEntity(list, headers), Void::class.java)
            }
        }

        val list: ArrayList<WorkflowDefinition> = arrayListOf()
        workflowTasks.forEach { value: WorkflowDefinition -> list.add(value) }
        restTemplate.put(url + WORKFLOW_DEF_PATH, HttpEntity(list, headers))
    }

    data class Task(var name: String, var type: String, var jsonNode: Any? = null)
}
