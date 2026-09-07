package opnet.fsocietydevs.devbots

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL


private const val SOCIETY_WHATSAPP_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society/whatsapp"


internal data class SocietyWhatsAppRemoteState(
    val status: String,
    val connected: Boolean,
    val running: Boolean,
    val pid: Long?,
    val qr: String?,
    val pairingCode: String?,
    val needsQr: Boolean,
    val lastEvent: String?
)


internal object SocietyWhatsAppApi {

    suspend fun status(
        prefs: SharedPreferences,
        botId: String
    ): SocietyWhatsAppRemoteState =
        withContext(
            Dispatchers.IO
        ) {

            val encoded =
                URLEncoder.encode(
                    botId,
                    "UTF-8"
                )


            val json =
                authenticated(
                    prefs = prefs,
                    method = "GET",
                    endpoint =
                        "/status?botId=$encoded"
                )


            parseState(
                json
            )
        }


    suspend fun connect(
        prefs: SharedPreferences,
        botId: String
    ) =
        control(
            prefs,
            botId,
            "connect"
        )


    suspend fun disconnect(
        prefs: SharedPreferences,
        botId: String
    ) =
        control(
            prefs,
            botId,
            "disconnect"
        )


    suspend fun restart(
        prefs: SharedPreferences,
        botId: String
    ) =
        control(
            prefs,
            botId,
            "restart"
        )


    suspend fun deleteSession(
        prefs: SharedPreferences,
        botId: String
    ) =
        control(
            prefs,
            botId,
            "delete-session"
        )


    suspend fun logs(
        prefs: SharedPreferences,
        botId: String
    ): List<String> =
        withContext(
            Dispatchers.IO
        ) {

            val encoded =
                URLEncoder.encode(
                    botId,
                    "UTF-8"
                )


            val json =
                authenticated(
                    prefs = prefs,
                    method = "GET",
                    endpoint =
                        "/logs?botId=$encoded"
                )


            val array =
                json.optJSONArray(
                    "logs"
                )


            if (
                array ==
                null
            ) {

                return@withContext emptyList()
            }


            buildList {

                for (
                    index in
                    0 until array.length()
                ) {

                    add(
                        array.optString(
                            index
                        )
                    )
                }
            }
        }


    private suspend fun control(
        prefs: SharedPreferences,
        botId: String,
        action: String
    ): JSONObject =
        withContext(
            Dispatchers.IO
        ) {

            authenticated(
                prefs = prefs,
                method = "POST",
                endpoint =
                    "/$action",
                body =
                    JSONObject()
                        .put(
                            "botId",
                            botId
                        )
            )
        }


    private suspend fun authenticated(
        prefs: SharedPreferences,
        method: String,
        endpoint: String,
        body: JSONObject? = null
    ): JSONObject {

        var token =
            SocietySession.validToken(
                prefs
            )


        var response =
            request(
                method = method,
                endpoint = endpoint,
                token = token,
                body = body
            )


        if (
            response.first ==
            401
        ) {

            token =
                SocietySession.validToken(
                    prefs =
                        prefs,

                    forceRefresh =
                        true
                )


            response =
                request(
                    method = method,
                    endpoint = endpoint,
                    token = token,
                    body = body
                )
        }


        val status =
            response.first


        val json =
            response.second


        if (
            status !in
            200..299
        ) {

            throw Exception(
                json.optString(
                    "error",
                    "Falha na comunicação com a sessão do WhatsApp."
                )
            )
        }


        return json
    }


    private fun request(
        method: String,
        endpoint: String,
        token: String,
        body: JSONObject?
    ): Pair<Int, JSONObject> {

        val connection =
            URL(
                SOCIETY_WHATSAPP_BASE +
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
                30000


            connection.setRequestProperty(
                "Accept",
                "application/json"
            )


            connection.setRequestProperty(
                "Authorization",
                "Bearer $token"
            )


            if (
                method ==
                "POST"
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
                        writer ->

                        writer.write(
                            (
                                body
                                    ?: JSONObject()
                            ).toString()
                        )
                    }
            }


            val status =
                connection.responseCode


            val stream =
                if (
                    status in
                    200..299
                ) {

                    connection.inputStream

                } else {

                    connection.errorStream
                }


            val raw =
                stream
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    .orEmpty()


            val json =
                try {

                    if (
                        raw.isBlank()
                    ) {

                        JSONObject()

                    } else {

                        JSONObject(
                            raw
                        )
                    }

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


    private fun parseState(
        json: JSONObject
    ): SocietyWhatsAppRemoteState {

        val pid =
            if (
                json.isNull(
                    "pid"
                )
            ) {

                null

            } else {

                json.optLong(
                    "pid"
                )
            }


        fun optional(
            key: String
        ): String? {

            val value =
                json.optString(
                    key
                ).trim()


            return value
                .takeIf {
                    it.isNotEmpty() &&
                    it != "null"
                }
        }


        return SocietyWhatsAppRemoteState(
            status =
                json.optString(
                    "status",
                    "disconnected"
                ),

            connected =
                json.optBoolean(
                    "connected",
                    false
                ),

            running =
                json.optBoolean(
                    "running",
                    false
                ),

            pid =
                pid,

            qr =
                optional(
                    "qr"
                ),

            pairingCode =
                optional(
                    "pairingCode"
                ),

            needsQr =
                json.optBoolean(
                    "needsQr",
                    false
                ),

            lastEvent =
                optional(
                    "lastEvent"
                )
        )
    }
}
