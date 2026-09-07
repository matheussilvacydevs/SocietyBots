package opnet.fsocietydevs.devbots

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL


private const val SOCIETY_IMPORT_URL =
    "https://android-studio.cloudpaniel.com.br/api/society/account/bots/import"


internal data class SocietyImportedBot(
    val id: String,
    val name: String,
    val library: String
)


private fun archiveName(
    context: Context,
    uri: Uri
): String {

    var result =
        "bot.zip"


    context.contentResolver
        .query(
            uri,
            null,
            null,
            null,
            null
        )
        ?.use {
            cursor ->

            val index =
                cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )


            if (
                index >=
                    0 &&
                cursor.moveToFirst()
            ) {

                result =
                    cursor.getString(
                        index
                    )
                        ?: result
            }
        }


    return result
}


private fun supportedArchive(
    filename: String
): Boolean {

    val lower =
        filename.lowercase()


    return (
        lower.endsWith(
            ".zip"
        ) ||
        lower.endsWith(
            ".tar.gz"
        ) ||
        lower.endsWith(
            ".tgz"
        ) ||
        lower.endsWith(
            ".tar"
        )
    )
}


internal object SocietyBotImportApi {

    suspend fun importBot(
        context: Context,
        prefs: SharedPreferences,
        uri: Uri
    ): SocietyImportedBot =
        withContext(
            Dispatchers.IO
        ) {

            val filename =
                archiveName(
                    context,
                    uri
                )


            if (
                !supportedArchive(
                    filename
                )
            ) {

                throw Exception(
                    "Escolha um arquivo .zip, .tar.gz, .tgz ou .tar."
                )
            }


            var token =
                SocietySession.validToken(
                    prefs
                )


            var response =
                request(
                    context =
                        context,

                    uri =
                        uri,

                    filename =
                        filename,

                    token =
                        token
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
                        context =
                            context,

                        uri =
                            uri,

                        filename =
                            filename,

                        token =
                            token
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
                        "Não foi possível importar o bot."
                    )
                )
            }


            val bot =
                json.optJSONObject(
                    "bot"
                )
                    ?: throw Exception(
                        "A Cloud não retornou o bot importado."
                    )


            SocietyImportedBot(
                id =
                    bot.optString(
                        "id"
                    ),

                name =
                    bot.optString(
                        "name",
                        "Bot importado"
                    ),

                library =
                    bot.optString(
                        "library"
                    )
            )
        }


    private fun request(
        context: Context,
        uri: Uri,
        filename: String,
        token: String
    ): Pair<Int, JSONObject> {

        val connection =
            URL(
                SOCIETY_IMPORT_URL
            )
                .openConnection()
                as HttpURLConnection


        try {

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                30000

            connection.readTimeout =
                240000

            connection.doOutput =
                true

            connection.setChunkedStreamingMode(
                256 *
                    1024
            )


            connection.setRequestProperty(
                "Accept",
                "application/json"
            )


            connection.setRequestProperty(
                "Content-Type",
                "application/octet-stream"
            )


            connection.setRequestProperty(
                "Authorization",
                "Bearer $token"
            )


            connection.setRequestProperty(
                "X-File-Name",
                URLEncoder.encode(
                    filename,
                    "UTF-8"
                )
            )


            connection.outputStream
                .use {
                    output ->

                    val input =
                        context.contentResolver
                            .openInputStream(
                                uri
                            )
                            ?: throw Exception(
                                "Não consegui abrir o arquivo."
                            )


                    input.use {

                        val buffer =
                            ByteArray(
                                64 *
                                    1024
                            )


                        while (
                            true
                        ) {

                            val count =
                                it.read(
                                    buffer
                                )


                            if (
                                count <
                                    0
                            ) {
                                break
                            }


                            output.write(
                                buffer,
                                0,
                                count
                            )
                        }
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


@Composable
internal fun SocietyImportBotButton(
    prefs: SharedPreferences,
    colors: SocietyColors
) {

    val context =
        androidx.compose.ui.platform
            .LocalContext.current


    val scope =
        rememberCoroutineScope()


    var importing by remember {
        mutableStateOf(
            false
        )
    }


    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) {
            uri ->

            if (
                uri ==
                    null
            ) {
                return@rememberLauncherForActivityResult
            }


            scope.launch {

                importing =
                    true


                try {

                    val result =
                        SocietyBotImportApi
                            .importBot(
                                context =
                                    context,

                                prefs =
                                    prefs,

                                uri =
                                    uri
                            )


                    Toast.makeText(
                        context,
                        "${result.name} importado com sucesso.",
                        Toast.LENGTH_LONG
                    ).show()


                    /*
                     * O bot já está no Firestore.
                     * Recriamos a Activity para o bootstrap
                     * buscar a lista nova imediatamente.
                     */
                    (
                        context as?
                            Activity
                    )?.recreate()

                } catch (
                    error: Exception
                ) {

                    Toast.makeText(
                        context,
                        error.message
                            ?: "Não foi possível importar o bot.",
                        Toast.LENGTH_LONG
                    ).show()

                } finally {

                    importing =
                        false
                }
            }
        }


    Surface(
        color =
            colors.primary.copy(
                alpha =
                    0.12f
            ),

        shape =
            RoundedCornerShape(
                22.dp
            ),

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled =
                        !importing
                ) {

                    launcher.launch(
                        arrayOf(
                            "application/zip",
                            "application/x-zip-compressed",
                            "application/gzip",
                            "application/x-gzip",
                            "application/x-tar",
                            "application/octet-stream"
                        )
                    )
                }
    ) {

        Column(
            modifier =
                Modifier.padding(
                    horizontal =
                        20.dp,

                    vertical =
                        16.dp
                )
        ) {

            if (
                importing
            ) {

                CircularProgressIndicator(
                    color =
                        colors.primarySoft
                )


                Text(
                    "Enviando → CDN → Cloud...",
                    color =
                        colors.text,

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier.padding(
                            top =
                                9.dp
                        )
                )

            } else {

                Text(
                    "⇧  Importar bot",
                    color =
                        colors.primarySoft,

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    "ZIP, TAR.GZ, TGZ ou TAR",
                    color =
                        colors.muted,

                    fontSize =
                        11.sp,

                    modifier =
                        Modifier.padding(
                            top =
                                3.dp
                        )
                )
            }
        }
    }
}
