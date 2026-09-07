package opnet.fsocietydevs.devbots

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


private const val BOT_CONTROL_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"


internal object SocietyBotControlApi {

    suspend fun status(
        uid: String,
        botId: String
    ): String =
        withContext(
            Dispatchers.IO
        ) {

            request(
                "GET",
                "$BOT_CONTROL_BASE/bots/status" +
                    "?uid=${enc(uid)}" +
                    "&botId=${enc(botId)}"
            )
                .optString(
                    "status",
                    "disconnected"
                )
        }


    suspend fun logs(
        uid: String,
        botId: String
    ): List<String> =
        withContext(
            Dispatchers.IO
        ) {

            val json =
                request(
                    "GET",
                    "$BOT_CONTROL_BASE/bots/logs" +
                        "?uid=${enc(uid)}" +
                        "&botId=${enc(botId)}"
                )


            val array =
                json.optJSONArray(
                    "logs"
                )


            buildList {

                if (
                    array !=
                    null
                ) {

                    for (
                        i in
                        0 until array.length()
                    ) {

                        add(
                            array.optString(
                                i
                            )
                        )
                    }
                }
            }
        }


    suspend fun start(
        uid: String,
        botId: String
    ) =
        control(
            "start",
            uid,
            botId
        )


    suspend fun stop(
        uid: String,
        botId: String
    ) =
        control(
            "stop",
            uid,
            botId
        )


    suspend fun restart(
        uid: String,
        botId: String
    ) =
        control(
            "restart",
            uid,
            botId
        )


    private suspend fun control(
        action: String,
        uid: String,
        botId: String
    ) =
        withContext(
            Dispatchers.IO
        ) {

            request(
                "POST",
                "$BOT_CONTROL_BASE/bots/$action",
                JSONObject()
                    .put(
                        "uid",
                        uid
                    )
                    .put(
                        "botId",
                        botId
                    )
            )
        }


    private fun request(
        method: String,
        url: String,
        body: JSONObject? = null
    ): JSONObject {

        val connection =
            URL(url)
                .openConnection()
                as HttpURLConnection


        try {

            connection.requestMethod =
                method

            connection.connectTimeout =
                15000

            connection.readTimeout =
                30000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )


            if (
                body !=
                null
            ) {

                connection.doOutput =
                    true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )


                connection.outputStream
                    .bufferedWriter()
                    .use {

                        it.write(
                            body.toString()
                        )
                    }
            }


            val status =
                connection.responseCode


            val raw =
                (
                    if (
                        status in
                        200..299
                    )
                        connection.inputStream
                    else
                        connection.errorStream
                )
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    .orEmpty()


            val json =
                if (
                    raw.isBlank()
                )
                    JSONObject()
                else
                    JSONObject(
                        raw
                    )


            if (
                status !in
                200..299
            ) {

                throw IllegalStateException(
                    json.optString(
                        "error",
                        "Falha ao controlar o bot."
                    )
                )
            }


            return json

        } finally {

            connection.disconnect()
        }
    }


    private fun enc(
        value: String
    ): String =
        URLEncoder.encode(
            value,
            "UTF-8"
        )
}
