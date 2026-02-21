package cn.com.omnimind.omnibot.devserver.command.handlers

import cn.com.omnimind.omnibot.OmniOperatorService
import cn.com.omnimind.omnibot.api.CaptureScreenshotImageResult
import cn.com.omnimind.omnibot.api.CaptureScreenshotXmlResult
import cn.com.omnimind.omnibot.api.GetMetadataResult
import cn.com.omnimind.omnibot.devserver.command.CommandResultWriter
import cn.com.omnimind.omnibot.devserver.contract.CommandInfo
import fi.iki.elonen.NanoHTTPD

class ScreenshotCommandHandlers(
    private val markScreenshotTimestamp: () -> Unit,
    private val markXmlTimestamp: () -> Unit,
) {
    @CommandInfo(
        "captureScreenshotImage",
        "Capture a screenshot as base64 encoded JPEG.",
        [],
        CaptureScreenshotImageResult::class,
    )
    private suspend fun handleCaptureScreenshotImageRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val res = OmniOperatorService.captureScreenshotImage()
        val record = session.parameters["record"]?.firstOrNull()?.toBoolean() ?: true
        if (record) {
            markScreenshotTimestamp()
        }
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "captureScreenshotXml",
        "Capture a screenshot UI as XML.",
        [],
        CaptureScreenshotXmlResult::class,
    )
    private suspend fun handleCaptureScreenshotXmlRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val res = OmniOperatorService.captureScreenshotXml()
        val record = session.parameters["record"]?.firstOrNull()?.toBoolean() ?: true
        if (record) {
            markXmlTimestamp()
        }
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "getMetadata",
        "Get the package name and activity name.",
        [],
        GetMetadataResult::class,
    )
    private suspend fun handleGetMetadata(): NanoHTTPD.Response {
        val res = OmniOperatorService.getMetadata()
        return CommandResultWriter.handleResult(res)
    }
}
