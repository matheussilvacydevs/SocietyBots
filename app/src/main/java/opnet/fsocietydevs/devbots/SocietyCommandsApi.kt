package opnet.fsocietydevs.devbots

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


private const val COMMANDS_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"


internal data class SocietyRemoteCommand(
    val id: String,
    val name: String,
    val description: String,
    val response: String,
    val enabled: Boolean,
    val builtin: Boolean
)


internal object SocietyCommandsApi {

    suspend fun listCommands(
        uid: String,
        botId: String
    ): List<SocietyRemoteCommand> =
        withContext(
            Dispatchers.IO
        ) {

            val url =
                "$COMMANDS_BASE/bots/commands" +
                "?uid=${enc(uid)}" +
                "&botId=${enc(botId)}"


            val json =
                request(
                    method =
                        "GET",

                    url =
                        url
                )


            buildList {

                val defaults =
                    json.optJSONArray(
                        "defaults"
                    )


                if (
                    defaults !=
                    null
                ) {

                    for (
                        i in
                        0 until
                        defaults.length()
                    ) {

                        add(
                            parseCommand(
                                defaults
                                    .getJSONObject(
                                        i
                                    )
                            )
                        )
                    }
                }


                val custom =
                    json.optJSONArray(
                        "custom"
                    )


                if (
                    custom !=
                    null
                ) {

                    for (
                        i in
                        0 until
                        custom.length()
                    ) {

                        add(
                            parseCommand(
                                custom
                                    .getJSONObject(
                                        i
                                    )
                            )
                        )
                    }
                }
            }
        }


    suspend fun createCommand(
        uid: String,
        botId: String,
        name: String,
        description: String,
        response: String,
        type: String = "text"
    ) =
        withContext(
            Dispatchers.IO
        ) {

            request(
                method =
                    "POST",

                url =
                    "$COMMANDS_BASE/bots/commands/create",

                body =
                    JSONObject()
                        .put(
                            "uid",
                            uid
                        )
                        .put(
                            "botId",
                            botId
                        )
                        .put(
                            "name",
                            name
                        )
                        .put(
                            "description",
                            description
                        )
                        .put(
                            "response",
                            response
                        )
                        .put(
                            "type",
                            type
                        )
            )
        }


    suspend fun deleteCommand(
        uid: String,
        botId: String,
        commandId: String
    ) =
        withContext(
            Dispatchers.IO
        ) {

            request(
                method =
                    "POST",

                url =
                    "$COMMANDS_BASE/bots/commands/delete",

                body =
                    JSONObject()
                        .put(
                            "uid",
                            uid
                        )
                        .put(
                            "botId",
                            botId
                        )
                        .put(
                            "commandId",
                            commandId
                        )
            )
        }


    private fun parseCommand(
        json: JSONObject
    ): SocietyRemoteCommand {

        return SocietyRemoteCommand(
            id =
                json.optString(
                    "id"
                ),

            name =
                json.optString(
                    "name"
                ),

            description =
                json.optString(
                    "description"
                ),

            response =
                json.optString(
                    "response"
                ),

            enabled =
                json.optBoolean(
                    "enabled",
                    true
                ),

            builtin =
                json.optBoolean(
                    "builtin",
                    false
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
                        "Erro ao acessar comandos."
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
