package opnet.fsocietydevs.devbots

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val BUILDER_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"


data class BuilderOption(
    val id: String,
    val label: String
)


data class BuilderQuestion(
    val key: String,
    val title: String,
    val text: String,
    val type: String,
    val placeholder: String?,
    val options: List<BuilderOption>
)


data class BuilderSession(
    val id: String,
    val name: String,
    val owner: String,
    val step: String,
    val status: String,
    val botId: String?,
    val question: BuilderQuestion?
)


data class BuilderEvent(
    val id: Int,
    val type: String,
    val message: String,
    val qr: String?,
    val code: String?
)


object SocietyBuilderApi {

    suspend fun start(
        uid: String,
        name: String,
        owner: String,
        library: String,
        style: String,
        features: Set<String>
    ): BuilderSession =
        withContext(
            Dispatchers.IO
        ) {

            val json =
                post(
                    "/builder/start",
                    JSONObject()
                        .put(
                            "uid",
                            uid
                        )
                        .put(
                            "name",
                            name
                        )
                        .put(
                            "owner",
                            owner
                        )
                        .put(
                            "library",
                            library
                        )
                        .put(
                            "style",
                            style
                        )
                        .put(
                            "features",
                            JSONArray().apply {

                                features.forEach {
                                    put(it)
                                }
                            }
                        )
                )

            parseSession(
                json.getJSONObject(
                    "session"
                )
            )
        }


    suspend fun answer(
        sessionId: String,
        value: Any
    ): BuilderSession =
        withContext(
            Dispatchers.IO
        ) {

            val body =
                JSONObject()
                    .put(
                        "sessionId",
                        sessionId
                    )


            when (value) {

                is Collection<*> -> {

                    val array =
                        JSONArray()

                    value.forEach {
                        array.put(
                            it
                        )
                    }

                    body.put(
                        "value",
                        array
                    )
                }

                else -> {
                    body.put(
                        "value",
                        value
                    )
                }
            }


            val json =
                post(
                    "/builder/answer",
                    body
                )


            parseSession(
                json.getJSONObject(
                    "session"
                )
            )
        }


    suspend fun build(
        sessionId: String
    ): BuilderSession =
        withContext(
            Dispatchers.IO
        ) {

            val json =
                post(
                    "/builder/build",
                    JSONObject()
                        .put(
                            "sessionId",
                            sessionId
                        )
                )


            parseSession(
                json.getJSONObject(
                    "session"
                )
            )
        }


    suspend fun events(
        sessionId: String,
        after: Int
    ): Pair<
        BuilderSession,
        List<BuilderEvent>
    > =
        withContext(
            Dispatchers.IO
        ) {

            val query =
                "?sessionId=" +
                URLEncoder.encode(
                    sessionId,
                    "UTF-8"
                ) +
                "&after=" +
                after


            val json =
                get(
                    "/builder/events" +
                    query
                )


            val session =
                parseSession(
                    json.getJSONObject(
                        "session"
                    )
                )


            val array =
                json.optJSONArray(
                    "events"
                ) ?: JSONArray()


            val events =
                buildList {

                    for (
                        i in
                        0 until
                        array.length()
                    ) {

                        val item =
                            array
                                .getJSONObject(
                                    i
                                )

                        add(
                            BuilderEvent(
                                id =
                                    item.optInt(
                                        "id"
                                    ),

                                type =
                                    item.optString(
                                        "type"
                                    ),

                                message =
                                    item.optString(
                                        "message"
                                    ),

                                qr =
                                    item.optString(
                                        "qr"
                                    ).takeIf {
                                        it.isNotBlank()
                                    },

                                code =
                                    item.optString(
                                        "code"
                                    ).takeIf {
                                        it.isNotBlank()
                                    }
                            )
                        )
                    }
                }


            session to
                events
        }


    private fun parseSession(
        json: JSONObject
    ): BuilderSession {

        return BuilderSession(
            id =
                json.optString(
                    "id"
                ),

            name =
                json.optString(
                    "name"
                ),

            owner =
                json.optString(
                    "owner"
                ),

            step =
                json.optString(
                    "step"
                ),

            status =
                json.optString(
                    "status"
                ),

            botId =
                json.optString(
                    "botId"
                ).takeIf {
                    it.isNotBlank()
                },

            question =
                json.optJSONObject(
                    "question"
                )?.let {
                    parseQuestion(
                        it
                    )
                }
        )
    }


    private fun parseQuestion(
        json: JSONObject
    ): BuilderQuestion {

        val optionsJson =
            json.optJSONArray(
                "options"
            ) ?: JSONArray()


        val options =
            buildList {

                for (
                    i in
                    0 until
                    optionsJson.length()
                ) {

                    val item =
                        optionsJson
                            .getJSONObject(
                                i
                            )

                    add(
                        BuilderOption(
                            id =
                                item.optString(
                                    "id"
                                ),

                            label =
                                item.optString(
                                    "label"
                                )
                        )
                    )
                }
            }


        return BuilderQuestion(
            key =
                json.optString(
                    "key"
                ),

            title =
                json.optString(
                    "title"
                ),

            text =
                json.optString(
                    "text"
                ),

            type =
                json.optString(
                    "type"
                ),

            placeholder =
                json.optString(
                    "placeholder"
                ).takeIf {
                    it.isNotBlank()
                },

            options =
                options
        )
    }


    private fun post(
        path: String,
        body: JSONObject
    ): JSONObject {

        return request(
            "POST",
            path,
            body
        )
    }


    private fun get(
        path: String
    ): JSONObject {

        return request(
            "GET",
            path,
            null
        )
    }


    private fun request(
        method: String,
        path: String,
        body: JSONObject?
    ): JSONObject {

        val connection =
            URL(
                BUILDER_BASE +
                path
            ).openConnection()
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
                body != null
            ) {

                connection.doOutput =
                    true

                connection
                    .setRequestProperty(
                        "Content-Type",
                        "application/json; charset=utf-8"
                    )

                connection.outputStream
                    .bufferedWriter(
                        Charsets.UTF_8
                    )
                    .use {
                        it.write(
                            body.toString()
                        )
                    }
            }


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
                            "Falha no Builder."
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
}
