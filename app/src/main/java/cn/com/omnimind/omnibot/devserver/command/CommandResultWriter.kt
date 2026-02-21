package cn.com.omnimind.omnibot.devserver.command

import cn.com.omnimind.omnibot.api.BaseOperatorResult
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD

object CommandResultWriter {
    fun <TData> handleResult(
        result: BaseOperatorResult<TData>,
        status: NanoHTTPD.Response.Status = NanoHTTPD.Response.Status.OK,
    ): NanoHTTPD.Response {
        val responseMap = LinkedHashMap<String, Any?>()
        responseMap["success"] = result.success
        responseMap["message"] = result.message

        if (result.data != null && result.data !is Unit) {
            responseMap["data"] = result.data
        }

        return NanoHTTPD.newFixedLengthResponse(
            status,
            "application/json",
            Gson().toJson(responseMap),
        )
    }
}
