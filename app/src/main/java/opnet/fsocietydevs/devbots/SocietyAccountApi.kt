package opnet.fsocietydevs.devbots

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


private const val ACCOUNT_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"


internal data class RemoteSocietyBot(
    val id: String,
    val name: String,
    val owner: String,
    val architecture: String,
    val library: String,
    val authMode: String,
    val prefix: String,
    val database: String,
    val features: Set<String>,
    val style: String,
    val status: String,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)


internal data class SocietyRemoteProfile(
    val displayName: String,
    val email: String,
    val avatarUrl: String
)


internal data class SocietyBootstrap(
    val uid: String,
    val profile: SocietyRemoteProfile,
    val bots: List<RemoteSocietyBot>
)


internal object SocietyAccountApi {

    suspend fun upsertBot(
        idToken: String,
        id: String,
        draft: BotDraft,
        archived: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) =
        withContext(
            Dispatchers.IO
        ) {

            val features =
                JSONArray()

            draft.features.forEach {
                features.put(it)
            }


            val body =
                JSONObject()
                    .put(
                        "id",
                        id
                    )
                    .put(
                        "serverBotId",
                        id
                    )
                    .put(
                        "name",
                        draft.name
                    )
                    .put(
                        "owner",
                        draft.owner
                    )
                    .put(
                        "architecture",
                        "command-handler"
                    )
                    .put(
                        "library",
                        draft.library
                    )
                    .put(
                        "authMode",
                        "pairing"
                    )
                    .put(
                        "prefix",
                        "/"
                    )
                    .put(
                        "database",
                        "json"
                    )
                    .put(
                        "features",
                        features
                    )
                    .put(
                        "style",
                        draft.style
                    )
                    .put(
                        "archived",
                        archived
                    )
                    .put(
                        "createdAt",
                        createdAt
                    )


            accountWrite(
                idToken =
                    idToken,

                endpoint =
                    "/account/bots/upsert",

                body =
                    body
            )
        }


    suspend fun deleteBot(
        idToken: String,
        botId: String
    ) =
        withContext(
            Dispatchers.IO
        ) {

            accountWrite(
                idToken =
                    idToken,

                endpoint =
                    "/account/bots/delete",

                body =
                    JSONObject()
                        .put(
                            "id",
                            botId
                        )
            )
        }


    private fun accountWrite(
        idToken: String,
        endpoint: String,
        body: JSONObject
    ): JSONObject {

        val connection =
            URL(
                ACCOUNT_BASE +
                    endpoint
            )
                .openConnection()
                as HttpURLConnection


        try {

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                15000

            connection.readTimeout =
                30000

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
                "Bearer $idToken"
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

                throw SocietyApiException(
                    message =
                        json.optString(
                            "error",
                            "Não foi possível salvar na Cloud."
                        ),

                    statusCode =
                        status
                )
            }


            return json

        } finally {

            connection.disconnect()
        }
    }




    suspend fun bootstrap(
        idToken: String
    ): SocietyBootstrap =
        withContext(
            Dispatchers.IO
        ) {

            val connection =
                URL(
                    "$ACCOUNT_BASE/account/bootstrap"
                )
                    .openConnection()
                    as HttpURLConnection


            try {

                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    30000


                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )


                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $idToken"
                )


                val status =
                    connection.responseCode


                val stream =
                    if (
                        status in
                        200..299
                    )
                        connection.inputStream
                    else
                        connection.errorStream


                val raw =
                    stream
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
                        JSONObject(raw)


                if (
                    status !in
                    200..299
                ) {

                    throw SocietyApiException(
                        message =
                            json.optString(
                                "error",
                                "Não foi possível sincronizar sua conta."
                            ),

                        statusCode =
                            status
                    )
                }


                parseBootstrap(
                    json
                )

            } finally {

                connection.disconnect()
            }
        }


    private fun parseBootstrap(
        json: JSONObject
    ): SocietyBootstrap {

        val profileJson =
            json.optJSONObject(
                "profile"
            ) ?: JSONObject()


        val botsJson =
            json.optJSONArray(
                "bots"
            ) ?: JSONArray()


        val bots =
            buildList {

                for (
                    i in
                    0 until
                    botsJson.length()
                ) {

                    /*
                     * Um registro legado/malformado não pode
                     * derrubar a restauração de todos os bots.
                     */
                    val bot =
                        botsJson
                            .optJSONObject(
                                i
                            )
                            ?: continue


                    val featureArray =
                        bot.optJSONArray(
                            "features"
                        ) ?: JSONArray()


                    val features =
                        buildSet {

                            for (
                                j in
                                0 until
                                featureArray.length()
                            ) {

                                val value =
                                    featureArray
                                        .optString(
                                            j
                                        )

                                if (
                                    value.isNotBlank()
                                ) {
                                    add(
                                        value
                                    )
                                }
                            }
                        }


                    add(
                        RemoteSocietyBot(
                            id =
                                bot.optString(
                                    "id"
                                ),

                            name =
                                bot.optString(
                                    "name"
                                ),

                            owner =
                                bot.optString(
                                    "owner"
                                ),

                            architecture =
                                bot.optString(
                                    "architecture"
                                ),

                            library =
                                bot.optString(
                                    "library"
                                ),

                            authMode =
                                bot.optString(
                                    "authMode"
                                ),

                            prefix =
                                bot.optString(
                                    "prefix",
                                    "/"
                                ),

                            database =
                                bot.optString(
                                    "database",
                                    "json"
                                ),

                            features =
                                features,

                            style =
                                bot.optString(
                                    "style"
                                ),

                            status =
                                bot.optString(
                                    "status",
                                    "disconnected"
                                ),

                            archived =
                                bot.optBoolean(
                                    "archived",
                                    false
                                ),

                            createdAt =
                                bot.optLong(
                                    "createdAt",
                                    System.currentTimeMillis()
                                ),

                            updatedAt =
                                bot.optLong(
                                    "updatedAt",
                                    0L
                                )
                        )
                    )
                }
            }


        return SocietyBootstrap(
            uid =
                json
                    .optJSONObject(
                        "user"
                    )
                    ?.optString(
                        "uid"
                    )
                    .orEmpty(),

            profile =
                SocietyRemoteProfile(
                    displayName =
                        profileJson.optString(
                            "displayName"
                        ),

                    email =
                        profileJson.optString(
                            "email"
                        ),

                    avatarUrl =
                        profileJson.optString(
                            "avatarUrl"
                        )
                ),

            bots =
                bots
        )
    }
}
