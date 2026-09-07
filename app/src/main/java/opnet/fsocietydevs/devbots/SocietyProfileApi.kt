package opnet.fsocietydevs.devbots

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL


private const val PROFILE_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"


internal object SocietyProfileApi {

    suspend fun updateProfile(
        context: Context,
        prefs: SharedPreferences,
        displayName: String,
        avatarUri: Uri? = null
    ): SocietyRemoteProfile =
        withContext(
            Dispatchers.IO
        ) {

            val body =
                JSONObject()
                    .put(
                        "displayName",
                        displayName.trim()
                    )


            if (
                avatarUri !=
                null
            ) {

                body.put(
                    "avatarBase64",
                    prepareAvatar(
                        context,
                        avatarUri
                    )
                )

                body.put(
                    "avatarMime",
                    "image/jpeg"
                )
            }


            val json =
                postWithSession(
                    prefs =
                        prefs,

                    body =
                        body
                )


            val profile =
                json.optJSONObject(
                    "profile"
                ) ?: JSONObject()


            SocietyRemoteProfile(
                displayName =
                    profile.optString(
                        "displayName"
                    ),

                email =
                    profile.optString(
                        "email"
                    ),

                avatarUrl =
                    profile.optString(
                        "avatarUrl"
                    )
            )
        }


    private fun prepareAvatar(
        context: Context,
        uri: Uri
    ): String {

        val resolver =
            context.contentResolver


        val bounds =
            BitmapFactory.Options()
                .apply {
                    inJustDecodeBounds =
                        true
                }


        resolver
            .openInputStream(
                uri
            )
            ?.use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    bounds
                )
            }


        if (
            bounds.outWidth <=
                0 ||
            bounds.outHeight <=
                0
        ) {

            throw Exception(
                "Não consegui abrir essa imagem."
            )
        }


        var sample =
            1


        while (
            bounds.outWidth /
                sample >
                1280 ||
            bounds.outHeight /
                sample >
                1280
        ) {

            sample *=
                2
        }


        val options =
            BitmapFactory.Options()
                .apply {
                    inSampleSize =
                        sample
                }


        val original =
            resolver
                .openInputStream(
                    uri
                )
                ?.use {

                    BitmapFactory.decodeStream(
                        it,
                        null,
                        options
                    )
                }
                ?: throw Exception(
                    "Não consegui ler a foto."
                )


        val maxSide =
            640


        val scale =
            minOf(
                1f,
                maxSide.toFloat() /
                    maxOf(
                        original.width,
                        original.height
                    )
            )


        val targetWidth =
            maxOf(
                1,
                (
                    original.width *
                    scale
                ).toInt()
            )


        val targetHeight =
            maxOf(
                1,
                (
                    original.height *
                    scale
                ).toInt()
            )


        val resized =
            if (
                targetWidth !=
                    original.width ||
                targetHeight !=
                    original.height
            ) {

                Bitmap.createScaledBitmap(
                    original,
                    targetWidth,
                    targetHeight,
                    true
                )

            } else {

                original
            }


        fun compress(
            quality: Int
        ): ByteArray {

            val stream =
                ByteArrayOutputStream()


            resized.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                stream
            )


            return stream.toByteArray()
        }


        var bytes =
            compress(
                84
            )


        if (
            bytes.size >
            700 *
            1024
        ) {

            bytes =
                compress(
                    68
                )
        }


        if (
            resized !==
            original
        ) {

            resized.recycle()
        }


        original.recycle()


        if (
            bytes.size >
            850 *
            1024
        ) {

            throw Exception(
                "A foto ficou grande demais. Escolha outra imagem."
            )
        }


        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP
        )
    }


    private suspend fun postWithSession(
        prefs: SharedPreferences,
        body: JSONObject
    ): JSONObject {

        var token =
            SocietySession.validToken(
                prefs
            )


        var result =
            request(
                token,
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
                        "Não foi possível salvar o perfil."
                    )
            )
        }


        return result.second
    }


    private fun request(
        token: String,
        body: JSONObject
    ): Pair<Int, JSONObject> {

        val connection =
            URL(
                "$PROFILE_BASE/account/profile"
            )
                .openConnection()
                as HttpURLConnection


        try {

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                20000

            connection.readTimeout =
                60000

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
                            "A Cloud respondeu em formato inválido."
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
