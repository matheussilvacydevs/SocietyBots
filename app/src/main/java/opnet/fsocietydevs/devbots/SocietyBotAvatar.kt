package opnet.fsocietydevs.devbots

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL


private const val BOT_AVATAR_BASE =
    "https://android-studio.cloudpaniel.com.br/api/society"


internal object SocietyBotAvatarApi {

    suspend fun load(
        prefs: SharedPreferences,
        botId: String
    ): ByteArray? =
        withContext(
            Dispatchers.IO
        ) {

            var token =
                SocietySession.validToken(
                    prefs
                )


            var result =
                download(
                    token,
                    botId
                )


            if (
                result.first ==
                401
            ) {

                token =
                    SocietySession.validToken(
                        prefs,
                        forceRefresh =
                            true
                    )


                result =
                    download(
                        token,
                        botId
                    )
            }


            if (
                result.first ==
                404
            ) {
                return@withContext null
            }


            if (
                result.first !in
                200..299
            ) {
                throw Exception(
                    "Não consegui carregar a foto do bot."
                )
            }


            result.second
        }


    suspend fun upload(
        prefs: SharedPreferences,
        botId: String,
        bytes: ByteArray
    ) =
        withContext(
            Dispatchers.IO
        ) {

            var token =
                SocietySession.validToken(
                    prefs
                )


            var status =
                send(
                    token,
                    botId,
                    bytes
                )


            if (
                status ==
                401
            ) {

                token =
                    SocietySession.validToken(
                        prefs,
                        forceRefresh =
                            true
                    )


                status =
                    send(
                        token,
                        botId,
                        bytes
                    )
            }


            if (
                status !in
                200..299
            ) {

                throw Exception(
                    "Não consegui salvar a foto do bot na Cloud."
                )
            }
        }


    private fun download(
        token: String,
        botId: String
    ): Pair<Int, ByteArray?> {

        val encoded =
            URLEncoder.encode(
                botId,
                "UTF-8"
            )


        val connection =
            URL(
                "$BOT_AVATAR_BASE/account/bots/avatar?botId=$encoded"
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
                "Authorization",
                "Bearer $token"
            )


            val status =
                connection.responseCode


            val bytes =
                if (
                    status in
                    200..299
                )
                    connection.inputStream
                        .use {
                            it.readBytes()
                        }
                else
                    null


            return Pair(
                status,
                bytes
            )

        } finally {

            connection.disconnect()
        }
    }


    private fun send(
        token: String,
        botId: String,
        bytes: ByteArray
    ): Int {

        val connection =
            URL(
                "$BOT_AVATAR_BASE/account/bots/avatar"
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
                "Authorization",
                "Bearer $token"
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )


            val body =
                JSONObject()
                    .put(
                        "botId",
                        botId
                    )
                    .put(
                        "avatarBase64",
                        Base64.encodeToString(
                            bytes,
                            Base64.NO_WRAP
                        )
                    )


            connection.outputStream
                .bufferedWriter()
                .use {
                    it.write(
                        body.toString()
                    )
                }


            return connection.responseCode

        } finally {

            connection.disconnect()
        }
    }
}


private suspend fun societyPrepareBotAvatar(
    context: Context,
    uri: Uri
): ByteArray =
    withContext(
        Dispatchers.IO
    ) {

        val originalBytes =
            context.contentResolver
                .openInputStream(
                    uri
                )
                ?.use {
                    it.readBytes()
                }
                ?: throw Exception(
                    "Não consegui abrir a imagem."
                )


        if (
            originalBytes.size >
            12 *
            1024 *
            1024
        ) {

            throw Exception(
                "Escolha uma imagem menor."
            )
        }


        val original =
            BitmapFactory.decodeByteArray(
                originalBytes,
                0,
                originalBytes.size
            )
                ?: throw Exception(
                    "Imagem inválida."
                )


        val maxSide =
            640f


        val scale =
            minOf(
                1f,
                maxSide /
                    maxOf(
                        original.width,
                        original.height
                    )
            )


        val width =
            maxOf(
                1,
                (
                    original.width *
                    scale
                ).toInt()
            )


        val height =
            maxOf(
                1,
                (
                    original.height *
                    scale
                ).toInt()
            )


        val resized =
            if (
                width !=
                    original.width ||
                height !=
                    original.height
            )
                Bitmap.createScaledBitmap(
                    original,
                    width,
                    height,
                    true
                )
            else
                original


        val output =
            ByteArrayOutputStream()


        resized.compress(
            Bitmap.CompressFormat.JPEG,
            82,
            output
        )


        if (
            resized !==
            original
        ) {
            resized.recycle()
        }


        original.recycle()


        val result =
            output.toByteArray()


        if (
            result.size >
            850 *
            1024
        ) {

            throw Exception(
                "A imagem ficou grande demais depois da compressão."
            )
        }


        result
    }


@Composable
internal fun SocietyEditableBotAvatar(
    prefs: SharedPreferences,
    botId: String,
    modifier: Modifier = Modifier
) {

    val context =
        androidx.compose.ui.platform
            .LocalContext.current


    val scope =
        rememberCoroutineScope()


    var bitmap by remember(
        botId
    ) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(
            null
        )
    }


    var loading by remember(
        botId
    ) {
        mutableStateOf(
            false
        )
    }


    LaunchedEffect(
        botId
    ) {

        if (
            botId.isBlank()
        ) {
            return@LaunchedEffect
        }


        loading =
            true


        try {

            val bytes =
                SocietyBotAvatarApi
                    .load(
                        prefs,
                        botId
                    )


            if (
                bytes !=
                null
            ) {

                bitmap =
                    BitmapFactory
                        .decodeByteArray(
                            bytes,
                            0,
                            bytes.size
                        )
                        ?.asImageBitmap()
            }

        } catch (
            _: Exception
        ) {

        } finally {

            loading =
                false
        }
    }


    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (
                uri ==
                null
            ) {
                return@rememberLauncherForActivityResult
            }


            try {

                context.contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

            } catch (
                _: Exception
            ) {}


            scope.launch {

                loading =
                    true


                try {

                    val bytes =
                        societyPrepareBotAvatar(
                            context,
                            uri
                        )


                    SocietyBotAvatarApi
                        .upload(
                            prefs,
                            botId,
                            bytes
                        )


                    bitmap =
                        BitmapFactory
                            .decodeByteArray(
                                bytes,
                                0,
                                bytes.size
                            )
                            ?.asImageBitmap()


                    Toast.makeText(
                        context,
                        "Foto do bot salva na Cloud.",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (
                    error: Exception
                ) {

                    Toast.makeText(
                        context,
                        error.message
                            ?: "Não consegui salvar a foto.",
                        Toast.LENGTH_LONG
                    ).show()

                } finally {

                    loading =
                        false
                }
            }
        }


    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(
                    enabled =
                        botId.isNotBlank() &&
                        !loading
                ) {

                    launcher.launch(
                        arrayOf(
                            "image/*"
                        )
                    )
                },

        contentAlignment =
            Alignment.Center
    ) {

        val image =
            bitmap


        if (
            image !=
            null
        ) {

            Image(
                bitmap =
                    image,

                contentDescription =
                    "Foto do bot",

                contentScale =
                    ContentScale.Crop,

                modifier =
                    Modifier.fillMaxSize()
            )

        } else {

            Text(
                if (
                    loading
                )
                    "…"
                else
                    "🤖",

                fontSize =
                    33.sp
            )
        }


        Surface(
            shape =
                CircleShape,

            modifier =
                Modifier
                    .align(
                        Alignment.BottomEnd
                    )
                    .padding(
                        3.dp
                    )
                    .size(
                        22.dp
                    )
        ) {

            Box(
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    "📷",
                    fontSize =
                        10.sp
                )
            }
        }
    }
}
