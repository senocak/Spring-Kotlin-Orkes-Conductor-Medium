package com.github.senocak.orkes.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.senocak.orkes.logger
import org.slf4j.Logger
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException

@Service
class WorkflowDefinition1(
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate
){
    private val log: Logger by logger()
    private val TASK_DEF_PATH = "/metadata/taskdefs"
    private val WORKFLOW_DEF_PATH = "/metadata/workflow"
    private val workflows: MutableList<JsonNode> = ArrayList()
    private val headers: HttpHeaders = HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON }

    @Throws(IOException::class)
    fun loadWorkflowsAndTasks(url: String) {
        val taskList = readTasks()
        val workflowTaskNames = readTaskNameFromWorkflow()
        checkTasksInWorkflow(taskList = taskList, workflowsTaskNames = workflowTaskNames)
        defineTasksInConductorServer(url = url, taskList = taskList)
        defineWorkflowsInConductorServer(url = url)
    }

    private fun defineWorkflowsInConductorServer(url: String) {
        val arrayNode = ArrayNode(JsonNodeFactory.instance)
        workflows.forEach { value: JsonNode? -> arrayNode.add(value) }
        restTemplate.put(url + WORKFLOW_DEF_PATH, HttpEntity(arrayNode, headers))
    }

    private fun defineTasksInConductorServer(url: String, taskList: MutableList<Task>) {
        val tasks = restTemplate.getForEntity(url + TASK_DEF_PATH, String::class.java)
        if (tasks.statusCode.is2xxSuccessful && tasks.body != null) {
            val conductorTasks = objectMapper.readTree(tasks.body) as ArrayNode
            conductorTasks.forEach { jsonNode: JsonNode ->
                val taskIterator = taskList.iterator()
                while (taskIterator.hasNext()) {
                    val task = taskIterator.next()
                    if (jsonNode["name"].textValue() == task.name) {
                        restTemplate.put(url + TASK_DEF_PATH, HttpEntity(task.jsonNode, headers))
                        taskIterator.remove()
                    }
                }
            }
            val arrayNode = ArrayNode(JsonNodeFactory.instance)
            taskList.forEach { task: Task -> arrayNode.add(task.jsonNode) }
            if (!arrayNode.isEmpty) {
                val httpEntity = HttpEntity(arrayNode, headers)
                //objectMapper.writeValueAsString(arrayNode)
                restTemplate.postForObject(url + TASK_DEF_PATH, httpEntity, Void::class.java)
            }
        }
    }

    private fun checkTasksInWorkflow(taskList: List<Task>, workflowsTaskNames: List<Task>) {
        workflowsTaskNames.forEach { workflowTask ->
            if (workflowTask.type == "SIMPLE" &&
                !workflowsTaskNames.any { wtn -> taskList.any { tl -> tl.name == wtn.name } }) {
                val message = "Workflow Task scan failed. Be careful about :$workflowTask"
                println(message = message)
                throw IllegalArgumentException(message)
            }
        }
    }

    private fun readTasks(): MutableList<Task> {
        val taskNames: MutableList<Task> = arrayListOf()
        val jsonNode = objectMapper.readTree(javaClass.classLoader.getResource("task.json")) as ArrayNode
        jsonNode.forEach { node: JsonNode ->
            taskNames.add(element = Task(name = node["name"].textValue(), type = "SIMPLE", jsonNode = node))
        }
        return taskNames
    }

    private fun readTaskNameFromWorkflow(): List<Task> {
        val arrayNode = objectMapper.readTree(javaClass.classLoader.getResource("workflow.json")) as ArrayNode
        val taskNames: MutableList<Task> = arrayListOf()
        arrayNode.elements().forEachRemaining { jsonNode: JsonNode ->
            workflows.add(element = jsonNode)
            val lArrayNode = jsonNode["tasks"] as ArrayNode
            lArrayNode.forEach { node: JsonNode ->
                taskNames.add(element = Task(name = node["name"].textValue(),
                    type = node["type"].textValue(), jsonNode = node))
            }
        }
        return taskNames
    }

    data class Task(var name: String, var type: String, var jsonNode: JsonNode)
}
