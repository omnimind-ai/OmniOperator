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
        val nodeId = CommandRequest.stringParam(session, "nodeId")

        return if (nodeId != null) {
            val res = OmniOperatorService.clickNode(nodeId)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Invalid node id")
        }
    }

    @CommandInfo("longClickNode", "Long click on a node.", ["nodeId"], LongClickNodeResult::class)
    private suspend fun handleLongClickNodeRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val nodeId = CommandRequest.stringParam(session, "nodeId")

        return if (nodeId != null) {
            val res = OmniOperatorService.longClickNode(nodeId)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Invalid node id")
        }
    }

    @CommandInfo(
        "scrollNode",
        "Scroll a node in a direction (forward, backward).",
        ["nodeId", "direction"],
        ScrollNodeResult::class,
    )
    private suspend fun handleScrollNodeRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val nodeId = CommandRequest.stringParam(session, "nodeId")
        val direction = CommandRequest.stringParam(session, "direction")

        return if (nodeId != null && direction != null) {
            val res = OmniOperatorService.scrollNode(nodeId, direction)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Invalid node id or invalid direction")
        }
    }

    @CommandInfo(
        "inputText",
        "Input text on an editable node and submit.",
        ["nodeId", "text"],
        InputTextResult::class,
    )
    private suspend fun handleInputTextRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val nodeId = CommandRequest.stringParam(session, "nodeId")
        val text = CommandRequest.stringParam(session, "text")

        return if (nodeId != null && text != null) {
            val res = OmniOperatorService.inputText(nodeId, text)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Invalid node id or empty text")
        }
    }

    @CommandInfo(
        "inputTextToFocusedNode",
        "Input text to the currently focused node and submit.",
        ["text"],
        InputTextToFocusedNodeResult::class,
    )
    private suspend fun handleInputTextToFocusedNodeRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val text = CommandRequest.stringParam(session, "text")

        return if (text != null) {
            val res = OmniOperatorService.inputTextToFocusedNode(text)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Empty text")
        }
    }

    @CommandInfo(
        "copyToClipboard",
        "Copy text to the clipboard.",
        ["text"],
        CopyToClipboardResult::class,
    )
    private suspend fun copyToClipboard(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val text = CommandRequest.stringParam(session, "text")

        return if (text != null) {
            val res = OmniOperatorService.copyToClipboard(text)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Empty text")
        }
    }

    @CommandInfo(
        "injectTextByIME",
        "Use OmniIME to input text  (not fully complete yet, there might be some bugs QwQ)",
        ["text"],
        InjectTextByIMEResult::class,
    )
    private suspend fun injectTextByIME(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val text = CommandRequest.stringParam(session, "text")

        return if (text != null) {
            val res = OmniOperatorService.injectTextByIME(text)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Empty text")
        }
    }
}
