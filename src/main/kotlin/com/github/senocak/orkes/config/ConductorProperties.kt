package com.github.senocak.orkes.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(value = "conductor")
class ConductorProperties {
    var url: String = "http://localhost:9090/api/"
    var threadCount: Int = 5
    var timeOut: Int = 10_000
}