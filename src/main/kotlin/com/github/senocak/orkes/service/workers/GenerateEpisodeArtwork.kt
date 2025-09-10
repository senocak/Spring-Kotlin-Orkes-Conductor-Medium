package com.github.senocak.orkes.service.workers

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.worker.Worker
import com.netflix.conductor.common.metadata.tasks.Task
import com.netflix.conductor.common.metadata.tasks.TaskResult
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class GenerateEpisodeArtwork: Worker {
    private val log: Logger by logger()

    override fun getTaskDefName(): String = "generate_episode_artwork"

    override fun execute(task: Task): TaskResult {
        val movieId: String = task.inputData["movieId"] as String
        log.info("GENERATE EPISODE ARTWORK : Movie ID : $movieId")
        return TaskResult.complete()
    }
}