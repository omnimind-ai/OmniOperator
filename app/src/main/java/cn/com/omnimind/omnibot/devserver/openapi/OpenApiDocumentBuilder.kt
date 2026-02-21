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

class OpenApiDocumentBuilder {
    private fun getJsonType(type: KType): JSONObject {
        val cls =
            type.classifier as? KClass<*> ?: return JSONObject().put(
                "type",
                "object",
            )

        return when (cls) {
            String::class -> JSONObject().put("type", "string")
            Int::class -> JSONObject().put("type", "integer").put("format", "int32")
            Long::class -> JSONObject().put("type", "integer").put("format", "int64")
            Float::class -> JSONObject().put("type", "number").put("format", "float")
            Double::class -> JSONObject().put("type", "number").put("format", "double")
            Boolean::class -> JSONObject().put("type", "boolean")
            List::class, MutableList::class, ArrayList::class -> {
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

    fun build(commandRoutes: List<CommandRoute>): NanoHTTPD.Response {
        val root = JSONObject()
        root.put("openapi", "3.0.0")

        val info = JSONObject()
        info.put("title", "Omni DevServer API")
        info.put("version", "1.0.0")
        info.put("description", "API for controlling the Omni application.")
        root.put("info", info)

        val componentSchemas = JSONObject()
        val paths = JSONObject()

        for (route in commandRoutes) {
            val pathItem = JSONObject()
            val getOp = JSONObject()
            getOp.put("summary", route.description)
            getOp.put("operationId", route.operationId)

            val parameters = JSONArray()
            for (argName in route.argNames) {
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

            val serviceMethodName = route.commandName
            val serviceMethod =
                OmniOperatorService.Companion::class.declaredFunctions.find { it.name == serviceMethodName }

            if (serviceMethod == null) {
                responseProperties.put(
                    "data",
                    JSONObject()
                        .put("type", "object")
                        .put("description", "Could not determine data schema for ${route.commandName}."),
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
                                    "Payload type complex or unknown for ${route.commandName}.",
                                ),
                            )
                        }
                    }
                } else {
                    responseProperties.put(
                        "data",
                        JSONObject().put("type", "object").put(
                            "description",
                            "Service method ${route.commandName} does not return BaseResult structure.",
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
}
