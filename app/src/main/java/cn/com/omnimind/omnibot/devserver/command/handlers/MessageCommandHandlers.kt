package cn.com.omnimind.omnibot.devserver.command.handlers

import cn.com.omnimind.omnibot.OmniOperatorService
import cn.com.omnimind.omnibot.api.PushMessageToBotResult
import cn.com.omnimind.omnibot.api.RequireUserChoiceResult
import cn.com.omnimind.omnibot.api.RequireUserConfirmationResult
import cn.com.omnimind.omnibot.api.ShowMessageResult
import cn.com.omnimind.omnibot.devserver.command.CommandRequest
import cn.com.omnimind.omnibot.devserver.command.CommandResultWriter
import cn.com.omnimind.omnibot.devserver.contract.CommandInfo
import fi.iki.elonen.NanoHTTPD

class MessageCommandHandlers {
    @CommandInfo(
        "requireUserConfirmation",
        "Require user confirmation with a prompt.",
        ["prompt"],
        RequireUserConfirmationResult::class,
    )
    private suspend fun handleRequireUserConfirmationRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val prompt = CommandRequest.stringParam(session, "prompt")

        return if (prompt != null) {
            val res = OmniOperatorService.requireUserConfirmation(prompt)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Missing prompt")
        }
    }

    @CommandInfo(
        "requireUserChoice",
        "Require user to choose from a list of options, dividing by semicolon(;).",
        ["prompt", "options"],
        RequireUserChoiceResult::class,
    )
    private suspend fun handleRequireUserChoiceRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val prompt = CommandRequest.stringParam(session, "prompt")
        val options = CommandRequest.stringParam(session, "options")?.split(";")?.map { it.trim() }

        return if (prompt != null && !options.isNullOrEmpty()) {
            val res = OmniOperatorService.requireUserChoice(prompt, options)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Missing prompt or options")
        }
    }

    @CommandInfo(
        "pushMessageToBot",
        "Push a message to the bot with optional suggestions.",
        ["message", "suggestionTitle", "suggestions"],
        PushMessageToBotResult::class,
    )
    private suspend fun handlePushMessageToBotRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val message = CommandRequest.stringParam(session, "message")
        val suggestionTitle = CommandRequest.stringParam(session, "suggestionTitle")
        val suggestions = CommandRequest.stringParam(session, "suggestions")?.split(";")?.map { it.trim() }

        return if (message != null) {
            val res = OmniOperatorService.pushMessageToBot(message, suggestionTitle, suggestions)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Missing message")
        }
    }

    @CommandInfo(
        "showMessage",
        "Push a message to the user.",
        ["title", "content"],
        ShowMessageResult::class,
    )
    private suspend fun handleShowMessageRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val title = CommandRequest.stringParam(session, "title")
        val content = CommandRequest.stringParam(session, "content")

        return if (title != null && content != null) {
            val res = OmniOperatorService.showMessage(title, content)
            CommandResultWriter.handleResult(res)
        } else {
            CommandRequest.badRequest("Missing title or content")
        }
    }
}
