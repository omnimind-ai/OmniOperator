package cn.com.omnimind.omnibot.devserver.route

import cn.com.omnimind.omnibot.MyApplication
import cn.com.omnimind.omnibot.R
import fi.iki.elonen.NanoHTTPD
import java.net.URLConnection

class StaticContentHandler {
    fun handleRootRequest(): NanoHTTPD.Response {
        val context = MyApplication.getContext()
        val inputStream = context.resources.openRawResource(R.raw.index)
        val htmlContent = inputStream.bufferedReader().use { it.readText() }
        return NanoHTTPD.newFixedLengthResponse(htmlContent)
    }

    fun handleRedocRequest(): NanoHTTPD.Response {
        val context = MyApplication.getContext()
        val inputStream = context.resources.openRawResource(R.raw.redoc)
        val htmlContent = inputStream.bufferedReader().use { it.readText() }
        return NanoHTTPD.newFixedLengthResponse(htmlContent)
    }

    fun handleClientRequest(): NanoHTTPD.Response {
        val context = MyApplication.getContext()
        val inputStream = context.resources.openRawResource(R.raw.client)
        val htmlContent = inputStream.bufferedReader().use { it.readText() }
        return NanoHTTPD.newFixedLengthResponse(htmlContent)
    }

    fun handleStaticFileRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val filePath = session.uri.removePrefix("/")
        val context = MyApplication.getContext()
        val inputStream = context.assets.open(filePath)
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            URLConnection.guessContentTypeFromName(filePath) ?: "application/octet-stream",
            inputStream,
            inputStream.available().toLong(),
        )
    }
}
