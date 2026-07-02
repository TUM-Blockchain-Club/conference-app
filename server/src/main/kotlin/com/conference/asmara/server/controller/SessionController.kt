package com.conference.asmara.server.controller

import com.conference.asmara.common.api.Routes
import com.conference.asmara.common.model.Session
import com.conference.asmara.common.model.Speaker
import kotlinx.datetime.Instant
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SessionController {

    @GetMapping(Routes.SESSIONS)
    fun getSessions(): List<Session> = listOf(
        Session(
            id = "1",
            title = "Intro to Kotlin Multiplatform",
            description = "Getting started with KMP for mobile development",
            speakerIds = listOf("s1"),
            startTime = Instant.parse("2026-07-10T09:00:00Z"),
            endTime = Instant.parse("2026-07-10T10:00:00Z"),
            room = "Main Hall",
        ),
        Session(
            id = "2",
            title = "Server-Side Kotlin with Spring Boot",
            description = "Building production APIs with Spring Boot and Exposed",
            speakerIds = listOf("s2"),
            startTime = Instant.parse("2026-07-10T10:30:00Z"),
            endTime = Instant.parse("2026-07-10T11:30:00Z"),
            room = "Room A",
        ),
    )

    @GetMapping(Routes.SPEAKERS)
    fun getSpeakers(): List<Speaker> = listOf(
        Speaker(
            id = "s1",
            name = "Alice Chen",
            bio = "Mobile engineer specializing in cross-platform development",
        ),
        Speaker(
            id = "s2",
            name = "Bob Martinez",
            bio = "Backend architect with 10 years of JVM experience",
        ),
    )

    @PostMapping(Routes.SESSIONS)
    fun createSession(@RequestBody session: Session): Session =
        session.copy(title = "${session.title} [received]")
}
