package cn.com.omnimind.omnibot.devserver.openapi

import cn.com.omnimind.omnibot.OmniOperatorService
import cn.com.omnimind.omnibot.api.BaseOperatorResult
import cn.com.omnimind.omnibot.devserver.command.CommandRoute
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

class OpenApiDocumentBuilder {
    private val serviceMethods by lazy {
        OmniOperatorService.Companion::class.declaredFunctions.associateBy { it.name }
    }

    fun build(commandRoutes: List<CommandRoute>): NanoHTTPD.Response {
        val root = JSONObject()
        root.put("openapi", "3.0.0")
        root.put("info", buildInfo())

        val componentSchemas = JSONObject()
        val paths = JSONObject()

        commandRoutes.forEach { route ->
            val pathItem = JSONObject()
            pathItem.put("get", buildGetOperation(route, componentSchemas))
            paths.put(route.path, pathItem)
        }

        root.put("paths", paths)
        if (componentSchemas.length() > 0) {
            root.put("components", JSONObject().put("schemas", componentSchemas))
        }

        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            root.toString(2),
        )
    }

    private fun buildInfo(): JSONObject =
        JSONObject()
            .put("title", "Omni DevServer API")
            .put("version", "1.0.0")
            .put("description", "API for controlling the Omni application.")

    private fun buildGetOperation(
        route: CommandRoute,
        componentSchemas: JSONObject,
    ): JSONObject {
        val operation = JSONObject()
        operation.put("summary", route.description)
        operation.put("operationId", route.operationId)

        val parameters = buildQueryParameters(route.argNames)
        if (parameters.length() > 0) {
            operation.put("parameters", parameters)
        }

        operation.put("responses", buildResponses(route.commandName, componentSchemas))
        return operation
    }

    private fun buildQueryParameters(argNames: List<String>): JSONArray {
        val parameters = JSONArray()
        argNames.forEach { argName ->
            parameters.put(
                JSONObject()
                    .put("name", argName)
                    .put("in", "query")
                    .put("required", true)
                    .put("schema", JSONObject().put("type", "string")),
            )
        }
        return parameters
    }

    private fun buildResponses(
        commandName: String,
        componentSchemas: JSONObject,
    ): JSONObject {
        val responses = JSONObject()
        val schema = buildResponseSchema(commandName, componentSchemas)
        val content = JSONObject().put("application/json", JSONObject().put("schema", schema))

        responses.put(
            "200",
            JSONObject()
                .put("description", "Successful operation")
                .put("content", content),
        )
        responses.put(
            "400",
            JSONObject()
                .put("description", "Invalid input or error")
                .put("content", content),
        )
        return responses
    }

    private fun buildResponseSchema(
        commandName: String,
        componentSchemas: JSONObject,
    ): JSONObject {
        val schema = JSONObject().put("type", "object")
        val responseProperties = JSONObject()
        responseProperties.put("success", JSONObject().put("type", "boolean"))
        responseProperties.put("message", JSONObject().put("type", "string"))

        val requiredResponseProps = mutableListOf("success", "message")

        val payloadSchema = resolvePayloadSchema(commandName, componentSchemas)
        if (payloadSchema != null) {
            responseProperties.put("data", payloadSchema)
            if (payloadSchema.optString("\$ref").isNotBlank()) {
                requiredResponseProps.add("data")
            }
        }

        schema.put("properties", responseProperties)
        if (requiredResponseProps.isNotEmpty()) {
            schema.put("required", JSONArray(requiredResponseProps))
        }
        return schema
    }

    private fun resolvePayloadSchema(
        commandName: String,
        componentSchemas: JSONObject,
    ): JSONObject? {
        val serviceMethod = serviceMethods[commandName]
            ?: return fallbackDataSchema("Could not determine data schema for $commandName.")

        val returnType = serviceMethod.returnType
        if (returnType.jvmErasure != BaseOperatorResult::class) {
            return fallbackDataSchema("Service method $commandName does not return BaseResult structure.")
        }

        val payloadType = returnType.arguments.firstOrNull()?.type ?: return null
        if (payloadType.jvmErasure == Unit::class) {
            return null
        }

        val payloadKClass = payloadType.classifier as? KClass<*>
            ?: return fallbackDataSchema("Payload type complex or unknown for $commandName.")

        val payloadSchemaName = payloadKClass.simpleName ?: "UnknownPayload"
        generatePayloadDataClassSchema(payloadKClass, componentSchemas)
        return JSONObject().put("\$ref", "#/components/schemas/$payloadSchemaName")
    }

    private fun fallbackDataSchema(description: String): JSONObject =
        JSONObject()
            .put("type", "object")
            .put("description", description)

    private fun getJsonType(type: KType): JSONObject {
        val cls = type.classifier as? KClass<*> ?: return JSONObject().put("type", "object")

        return when (cls) {
            String::class -> JSONObject().put("type", "string")
            Int::class -> JSONObject().put("type", "integer").put("format", "int32")
            Long::class -> JSONObject().put("type", "integer").put("format", "int64")
            Float::class -> JSONObject().put("type", "number").put("format", "float")
            Double::class -> JSONObject().put("type", "number").put("format", "double")
            Boolean::class -> JSONObject().put("type", "boolean")
            List::class, MutableList::class, ArrayList::class -> {
                val elementType = type.arguments.firstOrNull()?.type
                val itemSchema = elementType?.let { getJsonType(it) } ?: JSONObject().put("type", "object")
                JSONObject().put("type", "array").put("items", itemSchema)
            }

            Unit::class -> JSONObject()
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
            return componentSchemas.getJSONObject(schemaName)
        }

        val schema = JSONObject().put("type", "object")
        val properties = JSONObject()
        val required = mutableListOf<String>()

        if (kClass.isData) {
            kClass.primaryConstructor?.parameters?.forEach { param ->
                val name = param.name ?: return@forEach
                val type = param.type
                properties.put(name, getJsonType(type))
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
}
