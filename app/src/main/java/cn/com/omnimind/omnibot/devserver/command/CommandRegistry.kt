package cn.com.omnimind.omnibot.devserver.command

import cn.com.omnimind.omnibot.devserver.contract.Command
import cn.com.omnimind.omnibot.devserver.contract.CommandInfo
import fi.iki.elonen.NanoHTTPD
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

data class CommandRoute(
    val path: String,
    val commandName: String,
    val description: String,
    val argNames: List<String>,
    val responseType: KClass<*>,
    val operationId: String,
    val handler: suspend (NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response,
)

object CommandRegistry {
    fun collect(commandTargets: List<Any>): List<CommandRoute> {
        val routes = mutableListOf<CommandRoute>()

        commandTargets.forEach { target ->
            target::class
                .declaredMemberFunctions
                .filter { it.findAnnotation<CommandInfo>() != null }
                .forEach { fn ->
                    val ann = fn.findAnnotation<CommandInfo>()!!
                    fn.isAccessible = true
                    val isSessionParam = fn.parameters.size == 2
                    val handler: suspend (NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response = { session ->
                        if (isSessionParam) {
                            fn.callSuspend(target, session) as NanoHTTPD.Response
                        } else {
                            fn.callSuspend(target) as NanoHTTPD.Response
                        }
                    }

                    routes +=
                        CommandRoute(
                            path = "/${ann.name}",
                            commandName = ann.name,
                            description = ann.description,
                            argNames = ann.argNames.toList(),
                            responseType = ann.responseType,
                            operationId = fn.name,
                            handler = handler,
                        )
                }
        }

        val duplicatePaths =
            routes
                .groupBy { it.path }
                .filterValues { it.size > 1 }
                .keys
        if (duplicatePaths.isNotEmpty()) {
            throw IllegalStateException("Duplicate command paths found: $duplicatePaths")
        }

        return routes
    }

    fun toRouteMap(routes: List<CommandRoute>): Map<String, suspend (NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response> =
        routes.associate { it.path to it.handler }

    fun toCommandList(routes: List<CommandRoute>): List<Command> =
        routes.map {
            Command(
                name = it.commandName,
                description = it.description,
                argNames = it.argNames,
            )
        }
}
