package cn.com.omnimind.omnibot.devserver.command

import fi.iki.elonen.NanoHTTPD

object CommandRequest {
    fun badRequest(message: String): NanoHTTPD.Response =
        NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.BAD_REQUEST,
            "text/plain",
            message,
        )

    fun stringParam(session: NanoHTTPD.IHTTPSession, key: String): String? =
        session.parameters[key]?.firstOrNull()

    fun floatParam(session: NanoHTTPD.IHTTPSession, key: String): Float? =
        session.parameters[key]?.firstOrNull()?.toFloatOrNull()
}
