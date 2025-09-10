package com.github.senocak.orkes.model

data class SimpleTaskDefinitions(
    val list: List<SimpleTaskDefinition>
): BaseDto()

data class SimpleTaskDefinition(
    val name: String,
    val inputKeys: List<String> = emptyList(),
    val retryCount: Int? = null,
    val type: String? = null
): BaseDto()

data class TaskDefinitionDto(
    val ownerApp: String? = null,
    val createTime: Long? = null,
    val updateTime: Long? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val accessPolicy: AccessPolicyDto? = null,
    val name: String,
    val description: String? = null,
    val retryCount: Int? = null,
    val timeoutSeconds: Int? = null,
    val inputKeys: List<String> = emptyList(),
    val outputKeys: List<String> = emptyList(),
    val timeoutPolicy: String? = null,
    val retryLogic: String? = null,
    val retryDelaySeconds: Int? = null,
    val responseTimeoutSeconds: Int? = null,
    val concurrentExecLimit: Int? = null,
    val inputTemplate: Map<String, Any>? = null,
    val rateLimitPerFrequency: Int? = null,
    val rateLimitFrequencyInSeconds: Int? = null,
    val isolationGroupId: String? = null,
    val executionNameSpace: String? = null,
    val ownerEmail: String? = null,
    val pollTimeoutSeconds: Int? = null,
    val backoffScaleFactor: Int? = null,
): BaseDto()

data class AccessPolicyDto(
    val rules: Map<String, Any>? = null
): BaseDto()
