package opnet.fsocietydevs.devbots

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID


private const val SOCIETY_AI_CHATS_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"


internal data class SocietyAiStoredMessage(
    val role: String,
    val text: String,
    val provider: String = "",
    val time: String = ""
)


internal data class SocietyAiStoredChat(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<SocietyAiStoredMessage>
)


internal object SocietyAiChatsApi {

    suspend fun list(
        prefs: SharedPreferences
    ): List<SocietyAiStoredChat> =
        withContext(
            Dispatchers.IO
        ) {

            val json =
                requestWithSession(
                    prefs =
                        prefs,

                    method =
                        "GET",

                    endpoint =
                        "/account/ai/chats"
                )


            val array =
                json.optJSONArray(
                    "chats"
                )
                    ?: JSONArray()


            val result =
                mutableListOf<SocietyAiStoredChat>()


            for (
                i in
                0 until
                array.length()
            ) {

                result.add(
                    parseChat(
                        array.getJSONObject(
                            i
                        )
                    )
                )
            }


            result.sortedByDescending {
                it.updatedAt
            }
        }


    suspend fun save(
        prefs: SharedPreferences,
        chat: SocietyAiStoredChat
    ): SocietyAiStoredChat =
        withContext(
            Dispatchers.IO
        ) {

            val messages =
                JSONArray()


            chat.messages.forEach {
                message ->

                messages.put(
                    JSONObject()
                        .put(
                            "role",
                            message.role
                        )
                        .put(
                            "text",
                            message.text
                        )
                        .put(
                            "provider",
                            message.provider
                        )
                        .put(
                            "time",
                            message.time
                        )
                )
            }


            val json =
                requestWithSession(
                    prefs =
                        prefs,

                    method =
                        "POST",

                    endpoint =
                        "/account/ai/chats/save",

                    body =
                        JSONObject()
                            .put(
                                "id",
                                chat.id
                            )
                            .put(
                                "title",
                                chat.title
                            )
                            .put(
                                "createdAt",
                                chat.createdAt
                            )
                            .put(
                                "messages",
                                messages
                            )
                )


            parseChat(
                json.optJSONObject(
                    "chat"
                )
                    ?: throw Exception(
                        "A Cloud retornou um chat inválido."
                    )
            )
        }


    suspend fun delete(
        prefs: SharedPreferences,
        chatId: String
    ) =
        withContext(
            Dispatchers.IO
        ) {

            requestWithSession(
                prefs =
                    prefs,

                method =
                    "POST",

                endpoint =
                    "/account/ai/chats/delete",

                body =
                    JSONObject()
                        .put(
                            "id",
                            chatId
                        )
            )
        }


    suspend fun saveGeneratedCommandChat(
        prefs: SharedPreferences,
        commandName: String,
        prompt: String,
        answer: String
    ) {

        val now =
            System.currentTimeMillis()


        val clean =
            commandName
                .trim()
                .removePrefix(
                    "/"
                )


        save(
            prefs =
                prefs,

            chat =
                SocietyAiStoredChat(
                    id =
                        UUID.randomUUID()
                            .toString(),

                    title =
                        if (
                            clean.isBlank()
                        )
                            "Comando criado com IA"
                        else
                            "Comando /$clean",

                    createdAt =
                        now,

                    updatedAt =
                        now,

                    messages =
                        listOf(
                            SocietyAiStoredMessage(
                                role =
                                    "user",

                                text =
                                    prompt,

                                time =
                                    societyAiMessageStamp()
                            ),

                            SocietyAiStoredMessage(
                                role =
                                    "assistant",

                                text =
                                    answer,

                                time =
                                    societyAiMessageStamp()
                            )
                        )
                )
        )
    }


    private fun parseChat(
        json: JSONObject
    ): SocietyAiStoredChat {

        val messagesJson =
            json.optJSONArray(
                "messages"
            )
                ?: JSONArray()


        val messages =
            mutableListOf<SocietyAiStoredMessage>()


        for (
            i in
            0 until
            messagesJson.length()
        ) {

            val item =
                messagesJson
                    .getJSONObject(
                        i
                    )


            messages.add(
                SocietyAiStoredMessage(
                    role =
                        item.optString(
                            "role",
                            "user"
                        ),

                    text =
                        item.optString(
                            "text"
                        ),

                    provider =
                        item.optString(
                            "provider"
                        ),

                    time =
                        item.optString(
                            "time"
                        )
                )
            )
        }


        return SocietyAiStoredChat(
            id =
                json.optString(
                    "id"
                ),

            title =
                json.optString(
                    "title",
                    "Nova conversa"
                ),

            createdAt =
                json.optLong(
                    "createdAt",
                    System.currentTimeMillis()
                ),

            updatedAt =
                json.optLong(
                    "updatedAt",
                    System.currentTimeMillis()
                ),

            messages =
                messages
        )
    }


    private suspend fun requestWithSession(
        prefs: SharedPreferences,
        method: String,
        endpoint: String,
        body: JSONObject? = null
    ): JSONObject {

        var token =
            SocietySession.validToken(
                prefs
            )


        var result =
            request(
                token,
                method,
                endpoint,
                body
            )


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
                    token,
                    method,
                    endpoint,
                    body
                )
        }


        if (
            result.first !in
            200..299
        ) {

            throw Exception(
                result.second
                    .optString(
                        "error",
                        "Não foi possível sincronizar o chat."
                    )
            )
        }


        return result.second
    }


    private fun request(
        token: String,
        method: String,
        endpoint: String,
        body: JSONObject?
    ): Pair<Int, JSONObject> {

        val connection =
            URL(
                SOCIETY_AI_CHATS_BASE +
                    endpoint
            )
                .openConnection()
                as HttpURLConnection


        try {

            connection.requestMethod =
                method

            connection.connectTimeout =
                20000

            connection.readTimeout =
                60000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Authorization",
                "Bearer $token"
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
