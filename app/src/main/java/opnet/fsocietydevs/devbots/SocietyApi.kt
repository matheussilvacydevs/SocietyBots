package opnet.fsocietydevs.devbots

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val SOCIETY_API_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"

data class SocietyUser(
    val uid: String?,
    val email: String?,
    val displayName: String?,
    val emailVerified: Boolean
)

data class SocietyAuth(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

data class SocietyAuthResult(
    val message: String,
    val user: SocietyUser,
    val auth: SocietyAuth
)

class SocietyApiException(
    message: String,
    val statusCode: Int = 0,
    val code: String? = null,
    val resetSuggested: Boolean = false,
    val retryAfter: Int = 0
) : Exception(message)

object SocietyApi {

    suspend fun register(
        name: String,
        email: String,
        password: String
    ): SocietyAuthResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("name", name.trim())
                .put("email", email.trim().lowercase())
                .put("password", password)

            parseAuth(
                request(
                    "POST",
                    "/auth/register",
                    body
                )
            )
        }

    suspend fun login(
        email: String,
        password: String
    ): SocietyAuthResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("email", email.trim().lowercase())
                .put("password", password)

            parseAuth(
                request(
                    "POST",
                    "/auth/login",
                    body
                )
            )
        }

    suspend fun forgotPassword(
        email: String
    ): String =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("email", email.trim().lowercase())

            request(
                "POST",
                "/auth/forgot-password",
                body
            ).optString(
                "message",
                "Solicitação enviada."
            )
        }

    suspend fun resendVerification(
        idToken: String
    ): String =
        withContext(
            Dispatchers.IO
        ) {

            request(
                method =
                    "POST",

                path =
                    "/auth/resend-verification",

                bearerToken =
                    idToken
            )
                .optString(
                    "message",
                    "E-mail de verificação enviado."
                )
        }


    suspend fun me(
        idToken: String
    ): SocietyUser =
        withContext(Dispatchers.IO) {
            val json =
                request(
                    method = "GET",
                    path = "/auth/me",
                    bearerToken = idToken
                )

            parseUser(
                json.optJSONObject("user")
                    ?: JSONObject()
            )
        }

    private fun parseAuth(
        json: JSONObject
    ): SocietyAuthResult {

        val user =
            json.optJSONObject("user")
                ?: JSONObject()

        val auth =
            json.optJSONObject("auth")
                ?: throw SocietyApiException(
                    "Resposta de autenticação inválida."
                )

        val idToken =
            auth.optString("idToken")

        if (idToken.isBlank()) {
            throw SocietyApiException(
                "Sessão inválida retornada pelo servidor."
            )
        }

        return SocietyAuthResult(
            message =
                json.optString(
                    "message",
                    "Operação concluída."
                ),
            user =
                parseUser(user),
            auth =
                SocietyAuth(
                    idToken = idToken,
                    refreshToken =
                        auth.optString(
                            "refreshToken"
                        ),
                    expiresIn =
                        auth.optLong(
                            "expiresIn",
                            3600L
                        )
                )
        )
    }

    private fun parseUser(
        json: JSONObject
    ) =
        SocietyUser(
            uid =
                json.optString("uid")
                    .takeIf {
                        it.isNotBlank()
                    },
            email =
                json.optString("email")
                    .takeIf {
                        it.isNotBlank()
                    },
            displayName =
                json.optString(
                    "displayName"
                ).takeIf {
                    it.isNotBlank()
                },
            emailVerified =
                json.optBoolean(
                    "emailVerified",
                    false
                )
        )

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        bearerToken: String? = null
    ): JSONObject {

        val connection =
            URL(
                SOCIETY_API_BASE + path
            ).openConnection()
                as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )

            if (!bearerToken.isNullOrBlank()) {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $bearerToken"
                )
            }

            if (body != null) {
                connection.doOutput = true

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
                if (status in 200..299)
                    connection.inputStream
                else
                    connection.errorStream

            val raw =
                stream
                    ?.bufferedReader(
                        Charsets.UTF_8
                    )
                    ?.use {
                        it.readText()
                    }
                    .orEmpty()

            val json =
                try {
                    if (raw.isBlank())
                        JSONObject()
                    else
                        JSONObject(raw)
                } catch (_: Exception) {
                    JSONObject()
                }

            if (status !in 200..299) {
                throw SocietyApiException(
                    message =
                        json.optString(
                            "error",
                            "Falha na comunicação com a Society Bots."
                        ),

                    statusCode =
                        status,

                    code =
                        json.optString(
                            "code"
                        ).takeIf {
                            it.isNotBlank()
                        },

                    resetSuggested =
                        json.optBoolean(
                            "resetSuggested",
                            false
                        ),

                    retryAfter =
                        json.optInt(
                            "retryAfter",
                            0
                        )
                )
            }

            return json

        } catch (
            e: SocietyApiException
        ) {
            throw e

        } catch (
            e: Exception
        ) {
            throw SocietyApiException(
                e.message
                    ?: "Falha de conexão."
            )

        } finally {
            connection.disconnect()
        }
    }
}
