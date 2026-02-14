package cn.com.omnimind.omnibot.devserver.command.handlers

import cn.com.omnimind.omnibot.OmniOperatorService
import cn.com.omnimind.omnibot.api.ClickNodeResult
import cn.com.omnimind.omnibot.api.CopyToClipboardResult
import cn.com.omnimind.omnibot.api.InjectTextByIMEResult
import cn.com.omnimind.omnibot.api.InputTextResult
import cn.com.omnimind.omnibot.api.InputTextToFocusedNodeResult
import cn.com.omnimind.omnibot.api.LongClickNodeResult
import cn.com.omnimind.omnibot.api.ScrollNodeResult
import cn.com.omnimind.omnibot.devserver.command.CommandRequest
import cn.com.omnimind.omnibot.devserver.command.CommandResultWriter
import cn.com.omnimind.omnibot.devserver.contract.CommandInfo
import fi.iki.elonen.NanoHTTPD

class NodeCommandHandlers {
    @CommandInfo("clickNode", "Click on a node.", ["nodeId"], ClickNodeResult::class)
    private suspend fun handleClickNodeRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val nodeId = CommandRequest.stringParam(session, "nodeId") ?: return CommandRequest.badRequest("Invalid node id")
        val res = OmniOperatorService.clickNode(nodeId)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo("longClickNode", "Long click on a node.", ["nodeId"], LongClickNodeResult::class)
    private suspend fun handleLongClickNodeRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val nodeId = CommandRequest.stringParam(session, "nodeId") ?: return CommandRequest.badRequest("Invalid node id")
        val res = OmniOperatorService.longClickNode(nodeId)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "scrollNode",
        "Scroll a node in a direction (forward, backward).",
        ["nodeId", "direction"],
        ScrollNodeResult::class,
    )
    private suspend fun handleScrollNodeRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val nodeId =
            CommandRequest.stringParam(session, "nodeId")
                ?: return CommandRequest.badRequest("Invalid node id or invalid direction")
        val direction =
            CommandRequest.stringParam(session, "direction")
                ?: return CommandRequest.badRequest("Invalid node id or invalid direction")
        val res = OmniOperatorService.scrollNode(nodeId, direction)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "inputText",
        "Input text on an editable node and submit.",
        ["nodeId", "text"],
        InputTextResult::class,
    )
    private suspend fun handleInputTextRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val nodeId =
            CommandRequest.stringParam(session, "nodeId")
                ?: return CommandRequest.badRequest("Invalid node id or empty text")
        val text =
            CommandRequest.stringParam(session, "text")
                ?: return CommandRequest.badRequest("Invalid node id or empty text")
        val res = OmniOperatorService.inputText(nodeId, text)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "inputTextToFocusedNode",
        "Input text to the currently focused node and submit.",
        ["text"],
        InputTextToFocusedNodeResult::class,
    )
    private suspend fun handleInputTextToFocusedNodeRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val text = CommandRequest.stringParam(session, "text") ?: return CommandRequest.badRequest("Empty text")
        val res = OmniOperatorService.inputTextToFocusedNode(text)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "copyToClipboard",
        "Copy text to the clipboard.",
        ["text"],
        CopyToClipboardResult::class,
    )
    private suspend fun copyToClipboard(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val text = CommandRequest.stringParam(session, "text") ?: return CommandRequest.badRequest("Empty text")
        val res = OmniOperatorService.copyToClipboard(text)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "injectTextByIME",
        "Use OmniIME to input text  (not fully complete yet, there might be some bugs QwQ)",
        ["text"],
        InjectTextByIMEResult::class,
    )
    private suspend fun injectTextByIME(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val text = CommandRequest.stringParam(session, "text") ?: return CommandRequest.badRequest("Empty text")
        val res = OmniOperatorService.injectTextByIME(text)
        return CommandResultWriter.handleResult(res)
    }
}
