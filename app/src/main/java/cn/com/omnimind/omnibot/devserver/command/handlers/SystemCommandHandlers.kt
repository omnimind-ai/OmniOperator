package cn.com.omnimind.omnibot.devserver.command.handlers

import cn.com.omnimind.omnibot.OmniOperatorService
import cn.com.omnimind.omnibot.api.GoBackResult
import cn.com.omnimind.omnibot.api.GoHomeResult
import cn.com.omnimind.omnibot.api.LaunchApplicationResult
import cn.com.omnimind.omnibot.api.ListInstalledApplicationsResult
import cn.com.omnimind.omnibot.devserver.command.CommandRequest
import cn.com.omnimind.omnibot.devserver.command.CommandResultWriter
import cn.com.omnimind.omnibot.devserver.contract.CommandInfo
import fi.iki.elonen.NanoHTTPD

class SystemCommandHandlers {
    @CommandInfo(
        "launchApplication",
        "Launch an application by package name.",
        ["packageName"],
        LaunchApplicationResult::class,
    )
    private suspend fun handleLaunchApplicationRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val packageName =
            CommandRequest.stringParam(session, "packageName")
                ?: return CommandRequest.badRequest("Missing package name")
        val res = OmniOperatorService.launchApplication(packageName)
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo(
        "listInstalledApplications",
        "List all installed applications.",
        [],
        ListInstalledApplicationsResult::class,
    )
    private suspend fun handleListInstalledApplicationsRequest(): NanoHTTPD.Response {
        val res = OmniOperatorService.listInstalledApplications()
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo("goHome", "Go to the home screen.", [], GoHomeResult::class)
    private suspend fun handleGoHomeRequest(): NanoHTTPD.Response {
        val res = OmniOperatorService.goHome()
        return CommandResultWriter.handleResult(res)
    }

    @CommandInfo("goBack", "Go back to the previous screen.", [], GoBackResult::class)
    private suspend fun handleGoBackRequest(): NanoHTTPD.Response {
        val res = OmniOperatorService.goBack()
        return CommandResultWriter.handleResult(res)
    }
}
