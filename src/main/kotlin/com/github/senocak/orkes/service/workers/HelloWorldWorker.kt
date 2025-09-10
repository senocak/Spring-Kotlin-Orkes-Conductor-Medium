package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class HelloWorldWorker: Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "hello_world"

    override fun execute(task: Task): TaskResult {
        log.info("HELLO WORLD: ${task.inputData["name"]}")
        return TaskResult.complete()
    }
}