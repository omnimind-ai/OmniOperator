@file:Suppress("UNUSED")

package cn.com.omnimind.omnibot

import cn.com.omnimind.omnibot.api.BaseOperatorResult
import cn.com.omnimind.omnibot.api.CaptureScreenshotImageResult
import cn.com.omnimind.omnibot.api.CaptureScreenshotXmlResult
import cn.com.omnimind.omnibot.api.ClickCoordinateResult
import cn.com.omnimind.omnibot.api.ClickNodeResult
import cn.com.omnimind.omnibot.api.CopyToClipboardResult
import cn.com.omnimind.omnibot.api.GetMetadataResult
import cn.com.omnimind.omnibot.api.GoBackResult
import cn.com.omnimind.omnibot.api.GoHomeResult
import cn.com.omnimind.omnibot.api.InjectTextByIMEResult
import cn.com.omnimind.omnibot.api.InputTextResult
import cn.com.omnimind.omnibot.api.InputTextToFocusedNodeResult
import cn.com.omnimind.omnibot.api.LaunchApplicationResult
import cn.com.omnimind.omnibot.api.ListInstalledApplicationsResult
import cn.com.omnimind.omnibot.api.LongClickCoordinateResult
import cn.com.omnimind.omnibot.api.LongClickNodeResult
import cn.com.omnimind.omnibot.api.PushMessageToBotResult
import cn.com.omnimind.omnibot.api.RequireUserChoiceResult
import cn.com.omnimind.omnibot.api.RequireUserConfirmationResult
import cn.com.omnimind.omnibot.api.ScrollCoordinateResult
import cn.com.omnimind.omnibot.api.ScrollNodeResult
import cn.com.omnimind.omnibot.api.ShowMessageResult
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLConnection
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CommandInfo(
    val name: String,
    val description: String,
    val argNames: Array<String>,
    val responseType: KClass<*>,
)

data class Command(
    val name: String,
    val description: String,
    val argNames: List<String>,
)

class OmniDevServer(
    port: Int,
) : NanoHTTPD(port) {
    private val routeHandlers: Map<String, suspend (IHTTPSession) -> Response> =
        buildRouteHandlers()

    private var lastScreenshotTimestamp: Long = 0L
    private var lastXmlTimestamp: Long = 0L

    @CommandInfo(
        "captureScreenshotImage",
        "Capture a screenshot as base64 encoded JPEG.",
        [],
        CaptureScreenshotImageResult::class,
    )
    private suspend fun handleCaptureScreenshotImageRequest(session: IHTTPSession): Response {
        val res = OmniOperatorService.captureScreenshotImage()
        val record = session.parameters["record"]?.firstOrNull()?.toBoolean() ?: true
        if (record) {
            lastScreenshotTimestamp = System.currentTimeMillis()
        }
        return handleResult(res)
    }


    @CommandInfo(
        "captureScreenshotXml",
        "Capture a screenshot UI as XML.",
        [],
        CaptureScreenshotXmlResult::class,
    )
    private suspend fun handleCaptureScreenshotXmlRequest(session: IHTTPSession): Response {
        val res = OmniOperatorService.captureScreenshotXml()
        val record = session.parameters["record"]?.firstOrNull()?.toBoolean() ?: true
        if (record) {
            lastXmlTimestamp = System.currentTimeMillis()
        }
        return handleResult(res)
    }

    @CommandInfo(
        "getMetadata",
        "Get the package name and activity name.",
        [],
        GetMetadataResult::class,
    )
    private suspend fun handleGetMetadata(): Response {
        val res = OmniOperatorService.getMetadata()
        return handleResult(res)
    }

    @CommandInfo("clickNode", "Click on a node.", ["nodeId"], ClickNodeResult::class)
    private suspend fun handleClickNodeRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val nodeId = params["nodeId"]?.firstOrNull()

        return if (nodeId != null) {
            val res = OmniOperatorService.clickNode(nodeId)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid node id")
        }
    }

    @CommandInfo("longClickNode", "Long click on a node.", ["nodeId"], LongClickNodeResult::class)
    private suspend fun handleLongClickNodeRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val nodeId = params["nodeId"]?.firstOrNull()

        return if (nodeId != null) {
            val res = OmniOperatorService.longClickNode(nodeId)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid node id")
        }
    }

    @CommandInfo(
        "scrollNode",
        "Scroll a node in a direction (forward, backward).",
        ["nodeId", "direction"],
        ScrollNodeResult::class,
    )
    private suspend fun handleScrollNodeRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val nodeId = params["nodeId"]?.firstOrNull()
        val direction = params["direction"]?.firstOrNull()

        return if (nodeId != null && direction != null) {
            val res = OmniOperatorService.scrollNode(nodeId, direction)
            handleResult(res)
        } else {
            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "text/plain",
                "Invalid node id or invalid direction",
            )
        }
    }

    @CommandInfo(
        "inputText",
        "Input text on an editable node and submit.",
        ["nodeId", "text"],
        InputTextResult::class,
    )
    private suspend fun handleInputTextRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val nodeId = params["nodeId"]?.firstOrNull()
        val text = params["text"]?.firstOrNull()

        return if (nodeId != null && text != null) {
            val res = OmniOperatorService.inputText(nodeId, text)
            handleResult(res)
        } else {
            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "text/plain",
                "Invalid node id or empty text",
            )
        }
    }

    @CommandInfo(
        "inputTextToFocusedNode",
        "Input text to the currently focused node and submit.",
        ["text"],
        InputTextToFocusedNodeResult::class,
    )
    private suspend fun handleInputTextToFocusedNodeRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val text = params["text"]?.firstOrNull()

        return if (text != null) {
            val res = OmniOperatorService.inputTextToFocusedNode(text)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Empty text")
        }
    }

    @CommandInfo(
        "copyToClipboard",
        "Copy text to the clipboard.",
        ["text"],
        CopyToClipboardResult::class,
    )
    private suspend fun copyToClipboard(session: IHTTPSession): Response {
        val params = session.parameters
        val text = params["text"]?.firstOrNull()

        return if (text != null) {
            val res = OmniOperatorService.copyToClipboard(text)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Empty text")
        }
    }

    @CommandInfo(
        "injectTextByIME",
        "Use OmniIME to input text  (not fully complete yet, there might be some bugs QwQ)",
        ["text"],
        InjectTextByIMEResult::class,
    )
    private suspend fun injectTextByIME(session: IHTTPSession): Response {
        val params = session.parameters
        val text = params["text"]?.firstOrNull()

        return if (text != null) {
            val res = OmniOperatorService.injectTextByIME(text)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Empty text")
        }
    }

    @CommandInfo(
        "clickCoordinate",
        "Click on a coordinate.",
        ["x", "y"],
        ClickCoordinateResult::class,
    )
    private suspend fun handleClickCoordinateRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val x = params["x"]?.firstOrNull()?.toFloatOrNull()
        val y = params["y"]?.firstOrNull()?.toFloatOrNull()

        return if (x != null && y != null) {
            val res = OmniOperatorService.clickCoordinate(x, y)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid coordinates")
        }
    }

    @CommandInfo(
        "longClickCoordinate",
        "Long click on a coordinate.",
        ["x", "y"],
        LongClickCoordinateResult::class,
    )
    private suspend fun handleLongClickCoordinateRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val x = params["x"]?.firstOrNull()?.toFloatOrNull()
        val y = params["y"]?.firstOrNull()?.toFloatOrNull()

        return if (x != null && y != null) {
            val res = OmniOperatorService.longClickCoordinate(x, y)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid coordinates")
        }
    }

    @CommandInfo(
        "launchApplication",
        "Launch an application by package name.",
        ["packageName"],
        LaunchApplicationResult::class,
    )
    private suspend fun handleLaunchApplicationRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val packageName = params["packageName"]?.firstOrNull()

        return if (packageName != null) {
            val res = OmniOperatorService.launchApplication(packageName)
            handleResult(res)
        } else {
            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "text/plain",
                "Missing package name",
            )
        }
    }

    @CommandInfo(
        "requireUserConfirmation",
        "Require user confirmation with a prompt.",
        ["prompt"],
        RequireUserConfirmationResult::class,
    )
    private suspend fun handleRequireUserConfirmationRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val prompt = params["prompt"]?.firstOrNull()

        return if (prompt != null) {
            val res = OmniOperatorService.requireUserConfirmation(prompt)
            handleResult(res)
        } else {
            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "text/plain",
                "Missing prompt",
            )
        }
    }

    @CommandInfo(
        "requireUserChoice",
        "Require user to choose from a list of options, dividing by semicolon(;).",
        ["prompt", "options"],
        RequireUserChoiceResult::class,
    )
    private suspend fun handleRequireUserChoiceRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val prompt = params["prompt"]?.firstOrNull()
        val options = params["options"]?.firstOrNull()?.split(";")?.map { it.trim() }

        return if (prompt != null && !options.isNullOrEmpty()) {
            val res = OmniOperatorService.requireUserChoice(prompt, options)
            handleResult(res)
        } else {
            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "text/plain",
                "Missing prompt or options",
            )
        }
    }

    @CommandInfo(
        "listInstalledApplications",
        "List all installed applications.",
        [],
        ListInstalledApplicationsResult::class,
    )
    private suspend fun handleListInstalledApplicationsRequest(): Response {
        val res = OmniOperatorService.listInstalledApplications()
        return handleResult(res)
    }

    @CommandInfo(
        "scrollCoordinate",
        "Scroll on a coordinate in a direction (up, down, left, right).",
        ["x", "y", "direction", "distance"],
        ScrollCoordinateResult::class,
    )
    private suspend fun handleScrollCoordinateRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val x = params["x"]?.firstOrNull()?.toFloatOrNull()
        val y = params["y"]?.firstOrNull()?.toFloatOrNull()
        val direction = params["direction"]?.firstOrNull()
        val distance = params["distance"]?.firstOrNull()?.toFloatOrNull()

        return if (x != null && y != null && direction != null && distance != null) {
            val res = OmniOperatorService.scrollCoordinate(x, y, direction, distance)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid parameters")
        }
    }

    @CommandInfo("goHome", "Go to the home screen.", [], GoHomeResult::class)
    private suspend fun handleGoHomeRequest(): Response {
        val res = OmniOperatorService.goHome()
        return handleResult(res)
    }

    @CommandInfo("goBack", "Go back to the previous screen.", [], GoBackResult::class)
    private suspend fun handleGoBackRequest(): Response {
        val res = OmniOperatorService.goBack()
        return handleResult(res)
    }

    @CommandInfo(
        "pushMessageToBot",
        "Push a message to the bot with optional suggestions.",
        ["message", "suggestionTitle", "suggestions"],
        PushMessageToBotResult::class,
    )
    private suspend fun handlePushMessageToBotRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val message = params["message"]?.firstOrNull()
        val suggestionTitle = params["suggestionTitle"]?.firstOrNull()
        val suggestions = params["suggestions"]?.firstOrNull()?.split(";")?.map { it.trim() }

        return if (message != null) {
            val res = OmniOperatorService.pushMessageToBot(message, suggestionTitle, suggestions)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing message")
        }
    }

    @CommandInfo(
        "showMessage",
        "Push a message to the user.",
        ["title", "content"],
        ShowMessageResult::class,
    )
    private suspend fun handleShowMessageRequest(session: IHTTPSession): Response {
        val params = session.parameters
        val title = params["title"]?.firstOrNull()
        val content = params["content"]?.firstOrNull()

        return if (title != null && content != null) {
            val res = OmniOperatorService.showMessage(title, content)
            handleResult(res)
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing title or content")
        }
    }

    private fun handleGetTimestampsRequest(): Response {
        val timestamps =
            mapOf(
                "screenshot" to lastScreenshotTimestamp,
                "xml" to lastXmlTimestamp,
            )
        val json = Gson().toJson(timestamps)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun buildRouteHandlers(): Map<String, suspend (IHTTPSession) -> Response> {
        val handlers = mutableMapOf<String, suspend (IHTTPSession) -> Response>()

        this::class
            .declaredMemberFunctions
            .filter { it.findAnnotation<CommandInfo>() != null }
            .forEach { fn ->
                val ann = fn.findAnnotation<CommandInfo>()!!
                fn.isAccessible = true
                val isSessionParam = fn.parameters.size == 2 // [this, session] vs [this]
                val handler: suspend (IHTTPSession) -> Response = { session ->
                    if (isSessionParam) {
                        fn.callSuspend(this, session) as Response
                    } else {
                        fn.callSuspend(this) as Response
                    }
                }
                handlers["/${ann.name}"] = handler
            }

        return handlers
    }

    override fun serve(session: IHTTPSession): Response =
        runBlocking {
            return@runBlocking when (session.uri) {
                "/" -> handleRootRequest()
                "/commands" -> handleCommandsRequest()
                "/openapi.json" -> handleOpenApiJsonRequest()
                "/redoc" -> handleRedocRequest()
                "/client" -> handleClientRequest()
                "/timestamps" -> handleGetTimestampsRequest()
                "/health" ->
                    newFixedLengthResponse(
                        Response.Status.OK,
                        "text/plain",
                        BuildConfig.VERSION_NAME,
                    )

                else -> {
                    when {
                        session.uri.startsWith("/static/") -> handleStaticFileRequest(session)
                        routeHandlers.containsKey(session.uri) ->
                            routeHandlers[session.uri]!!.invoke(
                                session,
                            )

                        else -> handleNotFound()
                    }
                }
            }
        }

    private fun handleNotFound(): Response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")

    private fun handleCommandsRequest(): Response {
        val commands =
            this::class
                .declaredMemberFunctions
                .filter { it.findAnnotation<CommandInfo>() != null }
                .mapNotNull { fn ->
                    fn.findAnnotation<CommandInfo>()?.let { ann ->
                        Command(
                            name = ann.name,
                            description = ann.description,
                            argNames = ann.argNames.toList(),
                        )
                    }
                }

        val json = Gson().toJson(commands)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun handleRootRequest(): Response {
        val context = MyApplication.getContext()
        val inputStream = context.resources.openRawResource(R.raw.index)
        val htmlContent = inputStream.bufferedReader().use { it.readText() }
        return newFixedLengthResponse(htmlContent)
    }

    private fun handleRedocRequest(): Response {
        val context = MyApplication.getContext()
        val inputStream = context.resources.openRawResource(R.raw.redoc)
        val htmlContent = inputStream.bufferedReader().use { it.readText() }
        return newFixedLengthResponse(htmlContent)
    }

    private fun handleClientRequest(): Response {
        val context = MyApplication.getContext()
        val inputStream = context.resources.openRawResource(R.raw.client)
        val htmlContent = inputStream.bufferedReader().use { it.readText() }
        return newFixedLengthResponse(htmlContent)
    }

    private fun handleStaticFileRequest(session: IHTTPSession): Response {
        val filePath = session.uri.removePrefix("/")
        val context = MyApplication.getContext()
        val inputStream = context.assets.open(filePath)
        return newFixedLengthResponse(
            Response.Status.OK,
            URLConnection.guessContentTypeFromName(filePath) ?: "application/octet-stream",
            inputStream,
            inputStream.available().toLong(),
        )
    }

    private fun getJsonType(type: KType): JSONObject {
        val cls =
            type.classifier as? KClass<*> ?: return JSONObject().put(
                "type",
                "object",
            ) // Default for unknown

        return when (cls) {
            String::class -> JSONObject().put("type", "string")
            Int::class -> JSONObject().put("type", "integer").put("format", "int32")
            Long::class -> JSONObject().put("type", "integer").put("format", "int64")
            Float::class -> JSONObject().put("type", "number").put("format", "float")
            Double::class -> JSONObject().put("type", "number").put("format", "double")
            Boolean::class -> JSONObject().put("type", "boolean")
            List::class, MutableList::class, ArrayList::class -> { // Handle more list types
                val elementType = type.arguments.firstOrNull()?.type
                val itemSchema =
                    if (elementType != null) {
                        getJsonType(elementType)
                    } else {
                        JSONObject().put(
                            "type",
                            "object",
                        )
                    }
                JSONObject().put("type", "array").put("items", itemSchema)
            }

            Unit::class -> JSONObject() // Represent Unit as an empty schema, effectively omitting it where it's a direct type
            else -> {
                if (cls.isData) {
                    JSONObject().put("\$ref", "#/components/schemas/${cls.simpleName}")
                } else {
                    JSONObject()
                        .put("type", "object")
                        .put("description", "Complex object: ${cls.simpleName}")
                }
            }
        }
    }

    private fun generatePayloadDataClassSchema(
        kClass: KClass<*>,
        componentSchemas: JSONObject,
    ): JSONObject {
        val schemaName = kClass.simpleName ?: "UnknownPayloadType"
        if (componentSchemas.has(schemaName)) {
            return componentSchemas.getJSONObject(schemaName) // Already generated
        }

        val schema = JSONObject().put("type", "object")
        val properties = JSONObject()
        val required = mutableListOf<String>()

        if (kClass.isData) {
            kClass.primaryConstructor?.parameters?.forEach { param ->
                val name = param.name ?: return@forEach
                val type = param.type
                properties.put(name, getJsonType(type)) // Use getJsonType for property types
                if (!type.isMarkedNullable) {
                    required.add(name)
                }
            }
        } else {
            schema.put("description", "Schema for non-data class: ${kClass.simpleName}")
        }

        schema.put("properties", properties)
        if (required.isNotEmpty()) {
            schema.put("required", JSONArray(required))
        }
        componentSchemas.put(schemaName, schema)
        return schema
    }

    private fun handleOpenApiJsonRequest(): Response {
        val root = JSONObject()
        root.put("openapi", "3.0.0")

        val info = JSONObject()
        info.put("title", "Omni DevServer API")
        info.put("version", "1.0.0") // Increment version
        info.put("description", "API for controlling the Omni application.")
        root.put("info", info)

        val componentSchemas = JSONObject()
        val paths = JSONObject()

        val devServerHandlerFunctions =
            this::class
                .declaredMemberFunctions
                .filter { it.findAnnotation<CommandInfo>() != null }

        for (devServerHandlerFn in devServerHandlerFunctions) {
            val ann = devServerHandlerFn.findAnnotation<CommandInfo>()!!
            val pathItem = JSONObject()
            val getOp = JSONObject()
            getOp.put("summary", ann.description)
            getOp.put("operationId", devServerHandlerFn.name)

            val parameters = JSONArray()
            for (argName in ann.argNames) {
                parameters.put(
                    JSONObject().apply {
                        put("name", argName)
                        put("in", "query")
                        put("required", true)
                        put("schema", JSONObject().put("type", "string"))
                    },
                )
            }
            if (parameters.length() > 0) {
                getOp.put("parameters", parameters)
            }

            val responses = JSONObject()
            val okResponse = JSONObject()
            okResponse.put("description", "Successful operation")

            val responseSchema = JSONObject().put("type", "object")
            val responseProperties = JSONObject()
            responseProperties.put("success", JSONObject().put("type", "boolean"))
            responseProperties.put("message", JSONObject().put("type", "string"))

            val requiredResponseProps = mutableListOf("success", "message")

            val serviceMethodName = ann.name
            val serviceMethod =
                OmniOperatorService.Companion::class.declaredFunctions.find { it.name == serviceMethodName }

            if (serviceMethod == null) {
                responseProperties.put(
                    "data",
                    JSONObject()
                        .put("type", "object")
                        .put("description", "Could not determine data schema for ${ann.name}."),
                )
            } else {
                val serviceMethodReturnType: KType = serviceMethod.returnType

                if (serviceMethodReturnType.classifier == BaseOperatorResult::class) {
                    val typeArgumentKType = serviceMethodReturnType.arguments.firstOrNull()?.type
                    if (typeArgumentKType != null && typeArgumentKType.classifier != Unit::class) {
                        val payloadKClass = typeArgumentKType.classifier as? KClass<*>
                        if (payloadKClass != null) {
                            val payloadSchemaName = payloadKClass.simpleName ?: "UnknownPayload"
                            generatePayloadDataClassSchema(payloadKClass, componentSchemas)
                            responseProperties.put(
                                "data",
                                JSONObject().put("\$ref", "#/components/schemas/$payloadSchemaName"),
                            )
                            requiredResponseProps.add("data")
                        } else {
                            responseProperties.put(
                                "data",
                                JSONObject().put("type", "object").put(
                                    "description",
                                    "Payload type complex or unknown for ${ann.name}.",
                                ),
                            )
                        }
                    }
                } else {
                    responseProperties.put(
                        "data",
                        JSONObject()
                            .put(
                                "type",
                                "object",
                            ).put(
                                "description",
                                "Service method ${ann.name} does not return BaseResult structure.",
                            ),
                    )
                }
            }

            responseSchema.put("properties", responseProperties)
            if (requiredResponseProps.isNotEmpty()) {
                responseSchema.put("required", JSONArray(requiredResponseProps))
            }

            okResponse.put(
                "content",
                JSONObject().put("application/json", JSONObject().put("schema", responseSchema)),
            )
            responses.put("200", okResponse)

            val badRequestResponse =
                JSONObject()
                    .put("description", "Invalid input or error")
                    .put(
                        "content",
                        JSONObject().put(
                            "application/json",
                            JSONObject().put("schema", responseSchema),
                        ),
                    )
            responses.put("400", badRequestResponse)

            getOp.put("responses", responses)
            pathItem.put("get", getOp)
            paths.put("/${ann.name}", pathItem)
        }

        root.put("paths", paths)
        if (componentSchemas.length() > 0) {
            root.put("components", JSONObject().put("schemas", componentSchemas))
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", root.toString(2))
    }

    private fun <TData> handleResult(
        result: BaseOperatorResult<TData>,
        status: Response.Status = Response.Status.OK,
    ): Response {
        val responseMap = LinkedHashMap<String, Any?>()
        responseMap["success"] = result.success
        responseMap["message"] = result.message

        if (result.data != null && result.data !is Unit) {
            responseMap["data"] = result.data
        }

        return newFixedLengthResponse(
            status,
            "application/json",
            Gson().toJson(responseMap),
        )
    }
}
