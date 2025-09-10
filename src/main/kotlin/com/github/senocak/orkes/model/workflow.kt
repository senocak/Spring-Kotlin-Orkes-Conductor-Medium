package com.github.senocak.orkes.model

data class WorkflowDefinition(
    val name: String,
    val description: String? = null,
    val version: Int? = null,
    val schemaVersion: Int? = null,
    val inputParameters: List<String> = emptyList(),
    val tasks: List<WorkflowTask> = emptyList()
): BaseDto()

data class WorkflowTask(
    val name: String,
    val taskReferenceName: String,
    val inputParameters: Map<String, Any> = emptyMap(),
    val type: String,
    val caseValueParam: String? = null,
    val decisionCases: Map<String, List<WorkflowTask>>? = null,
    val defaultCase: List<WorkflowTask>? = null // optional in decision tasks
): BaseDto()