package cn.com.omnimind.omnibot.devserver.openapi

import cn.com.omnimind.omnibot.devserver.command.CommandRoute
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenApiDocumentBuilderTest {
    @Test
    fun `build should include get operation and query parameters`() {
        val builder = OpenApiDocumentBuilder()
        val response = builder.build(listOf(route(name = "clickCoordinate", args = listOf("x", "y"))))
        val body = response.data.bufferedReader().use { it.readText() }
        val root = JSONObject(body)

        val getOp = root.getJSONObject("paths").getJSONObject("/clickCoordinate").getJSONObject("get")
        val parameters = getOp.getJSONArray("parameters")

        assertEquals("Click route", getOp.getString("summary"))
        assertEquals(2, parameters.length())
        assertEquals("x", parameters.getJSONObject(0).getString("name"))
        assertEquals("query", parameters.getJSONObject(0).getString("in"))
    }

    @Test
    fun `build should fallback data schema when service method is missing`() {
        val builder = OpenApiDocumentBuilder()
        val response = builder.build(listOf(route(name = "unknownCommand", args = emptyList())))
        val body = response.data.bufferedReader().use { it.readText() }
        val root = JSONObject(body)

        val schema =
            root
                .getJSONObject("paths")
                .getJSONObject("/unknownCommand")
                .getJSONObject("get")
                .getJSONObject("responses")
                .getJSONObject("200")
                .getJSONObject("content")
                .getJSONObject("application/json")
                .getJSONObject("schema")
                .getJSONObject("properties")
                .getJSONObject("data")

        assertEquals("object", schema.getString("type"))
        assertTrue(schema.getString("description").contains("Could not determine data schema"))
    }

    private fun route(
        name: String,
        args: List<String>,
    ): CommandRoute =
        CommandRoute(
            path = "/$name",
            commandName = name,
            description = "Click route",
            argNames = args,
            responseType = Unit::class,
            operationId = "op_$name",
            handler = {
                NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/json",
                    "{}",
                )
            },
        )
}
