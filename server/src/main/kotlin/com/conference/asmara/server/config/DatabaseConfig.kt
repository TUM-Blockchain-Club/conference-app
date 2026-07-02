package com.conference.asmara.server.config

import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Configuration

@Configuration
@ImportAutoConfiguration(ExposedAutoConfiguration::class)
class DatabaseConfig
