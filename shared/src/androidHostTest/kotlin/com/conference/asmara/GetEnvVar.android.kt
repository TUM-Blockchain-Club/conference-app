package com.conference.asmara

actual fun getEnvVar(name: String): String? = System.getenv(name)
