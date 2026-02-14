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
        val x = CommandRequest.floatParam(session, "x") ?: return CommandRequest.badRequest("Invalid coordinates")
        val y = CommandRequest.floatParam(session, "y") ?: return CommandRequest.badRequest("Invalid coordinates")
        val res = OmniOperatorService.clickCoordinate(x, y)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "longClickCoordinate",
        "Long click on a coordinate.",
        ["x", "y"],
        LongClickCoordinateResult::class,
    )
    private suspend fun handleLongClickCoordinateRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val x = CommandRequest.floatParam(session, "x") ?: return CommandRequest.badRequest("Invalid coordinates")
        val y = CommandRequest.floatParam(session, "y") ?: return CommandRequest.badRequest("Invalid coordinates")
        val res = OmniOperatorService.longClickCoordinate(x, y)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "scrollCoordinate",
        "Scroll on a coordinate in a direction (up, down, left, right).",
        ["x", "y", "direction", "distance"],
        ScrollCoordinateResult::class,
    )
    private suspend fun handleScrollCoordinateRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val x = CommandRequest.floatParam(session, "x") ?: return CommandRequest.badRequest("Invalid parameters")
        val y = CommandRequest.floatParam(session, "y") ?: return CommandRequest.badRequest("Invalid parameters")
        val direction =
            CommandRequest.stringParam(session, "direction")
                ?: return CommandRequest.badRequest("Invalid parameters")
        val distance =
            CommandRequest.floatParam(session, "distance")
                ?: return CommandRequest.badRequest("Invalid parameters")
        val res = OmniOperatorService.scrollCoordinate(x, y, direction, distance)
        return CommandResultWriter.handleResult(res)
    }
}
