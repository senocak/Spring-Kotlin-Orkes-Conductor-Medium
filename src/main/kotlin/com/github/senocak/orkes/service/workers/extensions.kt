package com.github.senocak.orkes.service.workers

import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult

fun Task.requiredString(key: String): String? =
    (inputData[key] as? String)?.takeIf { it.isNotBlank() }

fun Task.optionalString(key: String, defaultValue: String): String =
    (inputData[key] as? String)?.takeIf { it.isNotBlank() } ?: defaultValue

fun Task.numberInput(key: String): Double? =
    when (val value = inputData[key]) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

fun Task.itemsInput(): List<Any> =
    (inputData["items"] as? List<*>)?.filterNotNull() ?: emptyList()

fun completed(task: Task, output: Map<String, Any?>): TaskResult =
    TaskResult(task).also { it: TaskResult ->
        it.outputData = output
        it.status = TaskResult.Status.COMPLETED
    }

fun failed(reason: String): TaskResult =
    TaskResult.failed().also { it.reasonForIncompletion = reason }
