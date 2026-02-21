package cn.com.omnimind.omnibot.devserver.auth

import fi.iki.elonen.NanoHTTPD

object DevServerAuth {
    private val defaultExcludedPaths = setOf("/health")

    fun enforce(
        session: NanoHTTPD.IHTTPSession,
        apiKey: String?,
        excludedPaths: Set<String> = defaultExcludedPaths,
    ): NanoHTTPD.Response? {
        val key = apiKey
        if (key.isNullOrBlank() || session.uri in excludedPaths) {
            return null
        }

        val authHeader = session.headers["authorization"]
        if (authHeader == null || authHeader != "Bearer $key") {
            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.UNAUTHORIZED,
                "application/json",
                """{"success":false,"message":"Unauthorized"}""",
            )
        }

        return null
    }
}
