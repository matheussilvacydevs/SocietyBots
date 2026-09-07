package opnet.fsocietydevs.devbots

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest


private const val SOCIETY_VERSION_URL =
    "https://android-studio.cloudpaniel.com.br/api/society/app/version"


internal data class SocietyAppUpdate(
    val versionCode: Int,
    val versionName: String,
    val minSupportedVersionCode: Int,
    val forceUpdate: Boolean,
    val apkUrl: String,
    val sha256: String,
    val size: Long,
    val changelog: List<String>
)


private suspend fun societyCheckUpdate():
    SocietyAppUpdate? =
    withContext(
        Dispatchers.IO
    ) {

        val connection =
            (
                URL(
                    SOCIETY_VERSION_URL
                )
                    .openConnection()
                as HttpURLConnection
            )


        try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                12_000

            connection.readTimeout =
                15_000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )


            val status =
                connection.responseCode


            if (
                status !in
                200..299
            ) {
                return@withContext null
            }


            val text =
                connection
                    .inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }


            val json =
                JSONObject(
                    text
                )


            if (
                !json.optBoolean(
                    "ok",
                    false
                )
            ) {
                return@withContext null
            }


            val changelogArray =
                json.optJSONArray(
                    "changelog"
                )


            val changelog =
                buildList {

                    if (
                        changelogArray !=
                        null
                    ) {

                        for (
                            index
                            in 0 until
                                changelogArray.length()
                        ) {

                            val value =
                                changelogArray
                                    .optString(
                                        index
                                    )
                                    .trim()


                            if (
                                value.isNotBlank()
                            ) {
                                add(
                                    value
                                )
                            }
                        }
                    }
                }


            SocietyAppUpdate(
                versionCode =
                    json.getInt(
                        "versionCode"
                    ),

                versionName =
                    json.getString(
                        "versionName"
                    ),

                minSupportedVersionCode =
                    json.optInt(
                        "minSupportedVersionCode",
                        1
                    ),

                forceUpdate =
                    json.optBoolean(
                        "forceUpdate",
                        false
                    ),

                apkUrl =
                    json.getString(
                        "apkUrl"
                    ),

                sha256 =
                    json.getString(
                        "sha256"
                    )
                        .lowercase(),

                size =
                    json.optLong(
                        "size",
                        0L
                    ),

                changelog =
                    changelog
            )

        } catch (
            _: Exception
        ) {

            null

        } finally {

            connection.disconnect()
        }
    }


private fun societySha256(
    file: File
): String {

    val digest =
        MessageDigest.getInstance(
            "SHA-256"
        )


    file.inputStream()
        .buffered()
        .use {
            input ->

            val buffer =
                ByteArray(
                    1024 * 1024
                )


            while (true) {

                val read =
                    input.read(
                        buffer
                    )


                if (
                    read <=
                    0
                ) {
                    break
                }


                digest.update(
                    buffer,
                    0,
                    read
                )
            }
        }


    return digest
        .digest()
        .joinToString(
            ""
        ) {
            "%02x".format(
                it
            )
        }
}


private suspend fun societyDownloadUpdate(
    context: Context,
    update: SocietyAppUpdate,
    onProgress: (Float) -> Unit
): File =
    withContext(
        Dispatchers.IO
    ) {

        val updateDir =
            File(
                context.cacheDir,
                "updates"
            )


        if (
            !updateDir.exists()
        ) {

            updateDir.mkdirs()
        }


        updateDir.listFiles()
            ?.forEach {
                file ->

                runCatching {
                    file.delete()
                }
            }


        val target =
            File(
                updateDir,
                "SocietyBots-${update.versionCode}.apk"
            )


        val connection =
            (
                URL(
                    update.apkUrl
                )
                    .openConnection()
                as HttpURLConnection
            )


        try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                15_000

            connection.readTimeout =
                120_000

            connection.instanceFollowRedirects =
                true

            connection.useCaches =
                false

            connection.setRequestProperty(
                "Cache-Control",
                "no-cache, no-store"
            )

            connection.setRequestProperty(
                "Pragma",
                "no-cache"
            )


            val status =
                connection.responseCode


            if (
                status !in
                200..299
            ) {

                throw IllegalStateException(
                    "A Cloud respondeu HTTP $status."
                )
            }


            val expectedSize =
                connection.contentLengthLong
                    .takeIf {
                        it >
                        0L
                    }
                    ?: update.size


            var downloaded =
                0L


            connection
                .inputStream
                .buffered()
                .use {
                    input ->

                    target
                        .outputStream()
                        .buffered()
                        .use {
                            output ->

                            val buffer =
                                ByteArray(
                                    128 * 1024
                                )


                            while (true) {

                                val read =
                                    input.read(
                                        buffer
                                    )


                                if (
                                    read <=
                                    0
                                ) {
                                    break
                                }


                                output.write(
                                    buffer,
                                    0,
                                    read
                                )


                                downloaded +=
                                    read


                                if (
                                    expectedSize >
                                    0L
                                ) {

                                    onProgress(
                                        (
                                            downloaded
                                                .toDouble() /
                                            expectedSize
                                                .toDouble()
                                        )
                                            .toFloat()
                                            .coerceIn(
                                                0f,
                                                1f
                                            )
                                    )
                                }
                            }


                            output.flush()
                        }
                }


            if (
                !target.exists() ||
                target.length() <=
                    0L
            ) {

                throw IllegalStateException(
                    "O APK baixado está vazio."
                )
            }


            val actualHash =
                societySha256(
                    target
                )


            if (
                !actualHash.equals(
                    update.sha256,
                    ignoreCase =
                        true
                )
            ) {

                target.delete()


                throw SecurityException(
                    "O SHA-256 do APK não corresponde ao publicado pela Cloud."
                )
            }


            /*
             * Segunda proteção:
             *
             * Não basta o metadata dizer que este arquivo é
             * a versão X. O próprio Android abre o APK e
             * confere packageName + versionCode antes da
             * instalação.
             *
             * Isso impede exatamente o caso:
             *
             * metadata = 1.1.1 / code 6
             * APK      = 1.1.0 / code 5
             */
            val archiveInfo =
                context.packageManager
                    .getPackageArchiveInfo(
                        target.absolutePath,
                        0
                    )


            if (
                archiveInfo ==
                null
            ) {

                target.delete()


                throw SecurityException(
                    "O arquivo recebido não é um APK Android válido."
                )
            }


            if (
                archiveInfo.packageName !=
                context.packageName
            ) {

                target.delete()


                throw SecurityException(
                    "O APK recebido pertence a outro aplicativo."
                )
            }


            val downloadedVersionCode =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.P
                ) {

                    archiveInfo.longVersionCode

                } else {

                    @Suppress("DEPRECATION")
                    archiveInfo.versionCode
                        .toLong()
                }


            if (
                downloadedVersionCode !=
                update.versionCode
                    .toLong()
            ) {

                target.delete()


                throw SecurityException(
                    "DOWNLOAD_VERSION_MISMATCH: " +
                    "a Cloud anunciou ${update.versionCode}, " +
                    "mas o APK recebido é $downloadedVersionCode."
                )
            }


            onProgress(
                1f
            )


            target

        } finally {

            connection.disconnect()
        }
    }


private fun societyInstallApk(
    context: Context,
    apk: File
) {

    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )


    val intent =
        Intent(
            Intent.ACTION_VIEW
        )
            .setDataAndType(
                uri,
                "application/vnd.android.package-archive"
            )
            .addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )


    if (
        context !is
        Activity
    ) {

        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
        )
    }


    context.startActivity(
        intent
    )
}


@Composable
internal fun SocietyUpdateHost(
    colors: SocietyColors
) {

    val context =
        LocalContext.current


    var update by remember {
        mutableStateOf<SocietyAppUpdate?>(
            null
        )
    }


    var checking by remember {
        mutableStateOf(
            true
        )
    }


    var downloading by remember {
        mutableStateOf(
            false
        )
    }


    var progress by remember {
        mutableFloatStateOf(
            0f
        )
    }


    var downloadedApk by remember {
        mutableStateOf<File?>(
            null
        )
    }


    var error by remember {
        mutableStateOf<String?>(
            null
        )
    }


    val settingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {

            val apk =
                downloadedApk


            if (
                apk !=
                    null &&
                (
                    Build.VERSION.SDK_INT <
                        Build.VERSION_CODES.O ||
                    context
                        .packageManager
                        .canRequestPackageInstalls()
                )
            ) {

                runCatching {

                    societyInstallApk(
                        context,
                        apk
                    )

                }.onFailure {

                    error =
                        it.message
                            ?: "Não foi possível abrir o instalador."
                }

            } else {

                error =
                    "Autorize o Society Bots a instalar atualizações e tente novamente."
            }
        }


    fun install(
        apk: File
    ) {

        downloadedApk =
            apk


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O &&
            !context
                .packageManager
                .canRequestPackageInstalls()
        ) {

            val intent =
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(
                        "package:${context.packageName}"
                    )
                )


            settingsLauncher.launch(
                intent
            )


            return
        }


        runCatching {

            societyInstallApk(
                context,
                apk
            )

        }.onFailure {

            error =
                it.message
                    ?: "Não foi possível abrir o instalador."
        }
    }


    LaunchedEffect(
        Unit
    ) {

        val remote =
            societyCheckUpdate()


        checking =
            false


        if (
            remote !=
                null &&
            remote.versionCode >
                BuildConfig.VERSION_CODE
        ) {

            update =
                remote
        }
    }


    val current =
        update


    if (
        current !=
        null
    ) {

        val mandatory =
            current.forceUpdate ||
            BuildConfig.VERSION_CODE <
                current.minSupportedVersionCode


        AlertDialog(
            onDismissRequest = {

                if (
                    !mandatory &&
                    !downloading
                ) {

                    update =
                        null
                }
            },

            title = {

                Text(
                    if (
                        mandatory
                    )
                        "Atualização necessária"
                    else
                        "Nova atualização disponível",

                    color =
                        colors.text,

                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    Text(
                        "Society Bots ${current.versionName}",
                        color =
                            colors.primarySoft,

                        fontWeight =
                            FontWeight.Bold
                    )


                    if (
                        current.changelog
                            .isNotEmpty()
                    ) {

                        Text(
                            current.changelog
                                .joinToString(
                                    separator =
                                        "\n"
                                ) {
                                    "• $it"
                                },

                            color =
                                colors.secondary
                        )
                    }


                    if (
                        downloading
                    ) {

                        Spacer(
                            Modifier.height(
                                4.dp
                            )
                        )


                        LinearProgressIndicator(
                            progress = {
                                progress
                                    .coerceIn(
                                        0f,
                                        1f
                                    )
                            },

                            modifier =
                                Modifier.fillMaxWidth()
                        )


                        Text(
                            "${(progress * 100).toInt()}%",

                            color =
                                colors.secondary
                        )
                    }


                    if (
                        error !=
                        null
                    ) {

                        Text(
                            error ?: "",

                            color =
                                androidx.compose.material3
                                    .MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    enabled =
                        !downloading,

                    onClick = {

                        val ready =
                            downloadedApk


                        if (
                            ready !=
                                null &&
                            ready.exists()
                        ) {

                            install(
                                ready
                            )

                        } else {

                            downloading =
                                true

                            error =
                                null

                            progress =
                                0f


                            (
                                context
                                as? androidx.activity.ComponentActivity
                            )?.let {
                                activity ->

                                activity
                                    .lifecycleScope
                                    .launch {

                                        try {

                                            val apk =
                                                societyDownloadUpdate(
                                                    context =
                                                        context,

                                                    update =
                                                        current,

                                                    onProgress = {
                                                        value ->

                                                        progress =
                                                            value
                                                    }
                                                )


                                            downloadedApk =
                                                apk


                                            downloading =
                                                false


                                            install(
                                                apk
                                            )

                                        } catch (
                                            exception: Exception
                                        ) {

                                            downloading =
                                                false


                                            error =
                                                exception.message
                                                    ?: "Falha ao baixar a atualização."
                                        }
                                    }
                            }
                        }
                    }
                ) {

                    Text(
                        if (
                            downloadedApk !=
                                null
                        )
                            "Instalar"
                        else
                            "Atualizar",

                        color =
                            colors.primarySoft
                    )
                }
            },

            dismissButton = {

                if (
                    !mandatory
                ) {

                    TextButton(
                        enabled =
                            !downloading,

                        onClick = {
                            update =
                                null
                        }
                    ) {

                        Text(
                            "Depois",
                            color =
                                colors.secondary
                        )
                    }
                }
            },

            containerColor =
                colors.surface
        )
    }


    /*
     * Mantido para deixar explícito que a checagem inicial
     * é assíncrona e não bloqueia a interface.
     */
    if (
        checking
    ) {
        Spacer(
            Modifier
                .height(
                    0.dp
                )
                .padding(
                    0.dp
                )
        )
    }
}
