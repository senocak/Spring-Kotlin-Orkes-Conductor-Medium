package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class SetupEpisodesWorker: Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "setup_episodes"

    override fun execute(task: Task): TaskResult {
        val movieId: String = task.inputData["movieId"] as String
        log.info("Setup Episodes. Movie Id : $movieId")
        return TaskResult.complete()
    }
}