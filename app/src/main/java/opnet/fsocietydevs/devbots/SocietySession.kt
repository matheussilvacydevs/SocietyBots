package opnet.fsocietydevs.devbots

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


private const val SESSION_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"


internal object SocietySession {

    suspend fun validToken(
        prefs: SharedPreferences,
        forceRefresh: Boolean = false
    ): String =
        withContext(
            Dispatchers.IO
        ) {

            val current =
                prefs.getString(
                    "auth_id_token",
                    ""
                ).orEmpty()

            val refresh =
                prefs.getString(
                    "auth_refresh_token",
                    ""
                ).orEmpty()

            val expiresIn =
                prefs.getLong(
                    "auth_expires_in",
                    0L
                )

            val savedAt =
                prefs.getLong(
                    "auth_saved_at",
                    0L
                )

            val now =
                System.currentTimeMillis()

            val expiresAt =
                savedAt +
                    (
                        expiresIn *
                        1000L
                    )

            val stillValid =
                current.isNotBlank() &&
                expiresAt >
                    now +
                    120000L


            if (
                !forceRefresh &&
                stillValid
            ) {
                return@withContext current
            }


            if (
                refresh.isBlank()
            ) {

                if (
                    current.isNotBlank() &&
                    !forceRefresh
                ) {
                    return@withContext current
                }

                throw Exception(
                    "Sua sessão expirou. Entre novamente."
                )
            }


            refreshToken(
                prefs,
                refresh
            )
        }


    private fun refreshToken(
        prefs: SharedPreferences,
        refreshToken: String
    ): String {

        val connection =
            URL(
                "$SESSION_BASE/auth/refresh"
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


            val body =
                JSONObject()
                    .put(
                        "refreshToken",
                        refreshToken
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

                    throw Exception(
                        "A Cloud respondeu em formato inválido."
                    )
                }


            if (
                status !in
                200..299
            ) {

                throw Exception(
                    json.optString(
                        "error",
                        "Não foi possível renovar sua sessão."
                    )
                )
            }


            val auth =
                json.optJSONObject(
                    "auth"
                )
                    ?: throw Exception(
                        "Resposta de sessão inválida."
                    )


            val newToken =
                auth.optString(
                    "idToken"
                )


            if (
                newToken.isBlank()
            ) {

                throw Exception(
                    "A Cloud não retornou um novo token."
                )
            }


            prefs.edit()
                .putString(
                    "auth_id_token",
                    newToken
                )
                .putString(
                    "auth_refresh_token",
                    auth.optString(
                        "refreshToken",
                        refreshToken
                    )
                )
                .putLong(
                    "auth_expires_in",
                    auth.optLong(
                        "expiresIn",
                        3600L
                    )
                )
                .putLong(
                    "auth_saved_at",
                    System.currentTimeMillis()
                )
                .apply()


            return newToken

        } finally {

            connection.disconnect()
        }
    }
}
