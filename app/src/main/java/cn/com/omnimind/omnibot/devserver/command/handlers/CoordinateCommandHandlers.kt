package cn.com.omnimind.omnibot.devserver.command.handlers

import cn.com.omnimind.omnibot.OmniOperatorService
import cn.com.omnimind.omnibot.api.ClickCoordinateResult
import cn.com.omnimind.omnibot.api.LongClickCoordinateResult
import cn.com.omnimind.omnibot.api.ScrollCoordinateResult
import cn.com.omnimind.omnibot.devserver.command.CommandRequest
import cn.com.omnimind.omnibot.devserver.command.CommandResultWriter
import cn.com.omnimind.omnibot.devserver.contract.CommandInfo
import fi.iki.elonen.NanoHTTPD

class CoordinateCommandHandlers {
    @CommandInfo(
        "clickCoordinate",
        "Click on a coordinate.",
        ["x", "y"],
        ClickCoordinateResult::class,
    )
    private suspend fun handleClickCoordinateRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val x = CommandRequest.floatParam(session, "x")
        val y = CommandRequest.floatParam(session, "y")

        return if (x != null && y != null) {
            val res = OmniOperatorService.clickCoordinate(x, y)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Invalid coordinates")
        }
    }

    @CommandInfo(
        "longClickCoordinate",
        "Long click on a coordinate.",
        ["x", "y"],
        LongClickCoordinateResult::class,
    )
    private suspend fun handleLongClickCoordinateRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val x = CommandRequest.floatParam(session, "x")
        val y = CommandRequest.floatParam(session, "y")

        return if (x != null && y != null) {
            val res = OmniOperatorService.longClickCoordinate(x, y)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Invalid coordinates")
        }
    }

    @CommandInfo(
        "scrollCoordinate",
        "Scroll on a coordinate in a direction (up, down, left, right).",
        ["x", "y", "direction", "distance"],
        ScrollCoordinateResult::class,
    )
    private suspend fun handleScrollCoordinateRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val x = CommandRequest.floatParam(session, "x")
        val y = CommandRequest.floatParam(session, "y")
        val direction = CommandRequest.stringParam(session, "direction")
        val distance = CommandRequest.floatParam(session, "distance")

        return if (x != null && y != null && direction != null && distance != null) {
            val res = OmniOperatorService.scrollCoordinate(x, y, direction, distance)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Invalid parameters")
        }
    }
}
