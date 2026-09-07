package opnet.fsocietydevs.devbots

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


private const val SOCIETY_AI_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"


internal data class SocietyAiHistoryItem(
    val role: String,
    val content: String
)


internal data class SocietyAiAttachment(
    val name: String,
    val mime: String,
    val base64: String
)


internal data class SocietyAiReply(
    val answer: String,
    val provider: String
)


internal data class SocietyAiCommandDraft(
    val name: String,
    val description: String,
    val response: String,
    val type: String,
    val provider: String
)


internal object SocietyAiApi {

    suspend fun chat(
        prefs: SharedPreferences,
        prompt: String,
        history: List<SocietyAiHistoryItem>,
        attachment: SocietyAiAttachment? = null
    ): SocietyAiReply =
        withContext(
            Dispatchers.IO
        ) {

            val historyJson =
                JSONArray()


            history.forEach {

                historyJson.put(
                    JSONObject()
                        .put(
                            "role",
                            it.role
                        )
                        .put(
                            "content",
                            it.content
                        )
                )
            }


            val body =
                JSONObject()
                    .put(
                        "prompt",
                        prompt
                    )
                    .put(
                        "history",
                        historyJson
                    )


            if (
                attachment !=
                null
            ) {

                body.put(
                    "attachment",
                    JSONObject()
                        .put(
                            "name",
                            attachment.name
                        )
                        .put(
                            "mime",
                            attachment.mime
                        )
                        .put(
                            "base64",
                            attachment.base64
                        )
                )
            }


            val json =
                postWithSession(
                    prefs =
                        prefs,

                    endpoint =
                        "/ai/chat",

                    body =
                        body
                )


            SocietyAiReply(
                answer =
                    json.optString(
                        "answer"
                    ),

                provider =
                    json.optString(
                        "provider"
                    )
            )
        }


    suspend fun commandDraft(
        prefs: SharedPreferences,
        description: String,
        type: String
    ): SocietyAiCommandDraft =
        withContext(
            Dispatchers.IO
        ) {

            val json =
                postWithSession(
                    prefs =
                        prefs,

                    endpoint =
                        "/ai/command-draft",

                    body =
                        JSONObject()
                            .put(
                                "description",
                                description
                            )
                            .put(
                                "type",
                                type
                            )
                )


            val draft =
                json.optJSONObject(
                    "draft"
                )
                    ?: JSONObject()


            SocietyAiCommandDraft(
                name =
                    draft.optString(
                        "name"
                    ),

                description =
                    draft.optString(
                        "description"
                    ),

                response =
                    draft.optString(
                        "response"
                    ),

                type =
                    draft.optString(
                        "type",
                        "text"
                    ),

                provider =
                    json.optString(
                        "provider"
                    )
            )
        }


    private suspend fun postWithSession(
        prefs: SharedPreferences,
        endpoint: String,
        body: JSONObject
    ): JSONObject {

        var token =
            SocietySession.validToken(
                prefs
            )


        var result =
            request(
                token =
                    token,

                endpoint =
                    endpoint,

                body =
                    body
            )


        /*
         * Mesmo que o cálculo local de expiração esteja errado,
         * um 401 força renovação e repete a chamada uma única vez.
         */
        if (
            result.first ==
            401
        ) {

            token =
                SocietySession.validToken(
                    prefs =
                        prefs,

                    forceRefresh =
                        true
                )


            result =
                request(
                    token =
                        token,

                    endpoint =
                        endpoint,

                    body =
                        body
                )
        }


        val status =
            result.first

        val json =
            result.second


        if (
            status !in
            200..299
        ) {

            throw SocietyApiException(
                message =
                    json.optString(
                        "error",
                        "Society AI indisponível."
                    ),

                statusCode =
                    status
            )
        }


        return json
    }


    private fun request(
        token: String,
        endpoint: String,
        body: JSONObject
    ): Pair<Int, JSONObject> {

        val connection =
            URL(
                SOCIETY_AI_BASE +
                    endpoint
            )
                .openConnection()
                as HttpURLConnection


        try {

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                20000

            connection.readTimeout =
                90000

            connection.doOutput =
                true


            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.setRequestProperty(
                "Authorization",
                "Bearer $token"
            )


            connection.outputStream
                .bufferedWriter()
                .use {
                    it.write(
                        body.toString()
                    )
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


            /*
             * Não deixa <!DOCTYPE ...> derrubar o app como JSON.
             */
            val json =
                try {

                    if (
                        raw.isBlank()
                    )
                        JSONObject()
                    else
                        JSONObject(
                            raw
                        )

                } catch (
                    _: Exception
                ) {

                    JSONObject()
                        .put(
                            "error",
                            "A Cloud respondeu em formato inválido (HTTP $status)."
                        )
                }


            return Pair(
                status,
                json
            )

        } finally {

            connection.disconnect()
        }
    }
}
