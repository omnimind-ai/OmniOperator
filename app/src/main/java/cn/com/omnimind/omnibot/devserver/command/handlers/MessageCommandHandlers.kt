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
        val prompt = CommandRequest.stringParam(session, "prompt") ?: return CommandRequest.badRequest("Missing prompt")
        val res = OmniOperatorService.requireUserConfirmation(prompt)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "requireUserChoice",
        "Require user to choose from a list of options, dividing by semicolon(;).",
        ["prompt", "options"],
        RequireUserChoiceResult::class,
    )
    private suspend fun handleRequireUserChoiceRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val prompt =
            CommandRequest.stringParam(session, "prompt")
                ?: return CommandRequest.badRequest("Missing prompt or options")
        val options =
            CommandRequest.semicolonListParam(session, "options")
                ?.takeIf { it.isNotEmpty() }
                ?: return CommandRequest.badRequest("Missing prompt or options")
        val res = OmniOperatorService.requireUserChoice(prompt, options)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "pushMessageToBot",
        "Push a message to the bot with optional suggestions.",
        ["message", "suggestionTitle", "suggestions"],
        PushMessageToBotResult::class,
    )
    private suspend fun handlePushMessageToBotRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val message = CommandRequest.stringParam(session, "message") ?: return CommandRequest.badRequest("Missing message")
        val suggestionTitle = CommandRequest.stringParam(session, "suggestionTitle")
        val suggestions = CommandRequest.semicolonListParam(session, "suggestions")
        val res = OmniOperatorService.pushMessageToBot(message, suggestionTitle, suggestions)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "showMessage",
        "Push a message to the user.",
        ["title", "content"],
        ShowMessageResult::class,
    )
    private suspend fun handleShowMessageRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val title =
            CommandRequest.stringParam(session, "title")
                ?: return CommandRequest.badRequest("Missing title or content")
        val content =
            CommandRequest.stringParam(session, "content")
                ?: return CommandRequest.badRequest("Missing title or content")
        val res = OmniOperatorService.showMessage(title, content)
        return CommandResultWriter.handleResult(res)
    }
}
