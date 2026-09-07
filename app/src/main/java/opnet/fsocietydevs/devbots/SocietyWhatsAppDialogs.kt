package opnet.fsocietydevs.devbots

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter


private val SocietyWaDialogBackground =
    Color(0xFF0D0C14)

private val SocietyWaDialogBorder =
    Color(0xFF4A246E)

private val SocietyWaDialogPurple =
    Color(0xFFA357FF)

private val SocietyWaDialogMuted =
    Color(0xFFA9A4B8)


@Composable
internal fun SocietyWhatsAppDialogs(
    qrBot: SocietyWhatsAppBotUi?,
    logsTitle: String?,
    logs: List<String>?,
    onDismissQr: () -> Unit,
    onDismissLogs: () -> Unit
) {

    if (qrBot != null) {

        SocietyWhatsAppQrDialog(
            bot = qrBot,
            onDismiss = onDismissQr
        )
    }


    if (
        logsTitle != null &&
        logs != null
    ) {

        SocietyWhatsAppLogsDialog(
            title = logsTitle,
            lines = logs,
            onDismiss = onDismissLogs
        )
    }
}


@Composable
private fun SocietyWhatsAppQrDialog(
    bot: SocietyWhatsAppBotUi,
    onDismiss: () -> Unit
) {

    val clipboard =
        LocalClipboardManager.current


    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SocietyWaDialogBackground,

        title = {

            Text(
                text =
                    when {

                        !bot.pairingCode.isNullOrBlank() ->
                            "Código de pareamento"

                        !bot.qr.isNullOrBlank() ->
                            "Conectar ${bot.name}"

                        else ->
                            "Aguardando conexão"
                    },

                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                when {

                    !bot.pairingCode.isNullOrBlank() -> {

                        Text(
                            text =
                                "No WhatsApp, abra Aparelhos conectados e escolha conectar com número de telefone.",

                            color = SocietyWaDialogMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )


                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )


                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFF171121),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        1.dp,
                                        SocietyWaDialogBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(22.dp),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text =
                                    bot.pairingCode ?: "",

                                color =
                                    SocietyWaDialogPurple,

                                fontSize =
                                    28.sp,

                                fontWeight =
                                    FontWeight.ExtraBold,

                                letterSpacing =
                                    3.sp
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )


                        TextButton(
                            onClick = {

                                clipboard.setText(
                                    AnnotatedString(
                                        bot.pairingCode ?: ""
                                    )
                                )
                            }
                        ) {

                            Text(
                                text = "Copiar código",
                                color = SocietyWaDialogPurple
                            )
                        }
                    }


                    !bot.qr.isNullOrBlank() -> {

                        Text(
                            text =
                                "WhatsApp → Aparelhos conectados → Conectar um aparelho",

                            color =
                                SocietyWaDialogMuted,

                            fontSize =
                                13.sp,

                            textAlign =
                                TextAlign.Center
                        )


                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )


                        val bitmap =
                            remember(bot.qr) {

                                createQrBitmap(
                                    bot.qr ?: ""
                                )
                            }


                        if (bitmap != null) {

                            Box(
                                modifier =
                                    Modifier
                                        .background(
                                            Color.White,
                                            RoundedCornerShape(18.dp)
                                        )
                                        .padding(12.dp)
                            ) {

                                Image(
                                    bitmap =
                                        bitmap.asImageBitmap(),

                                    contentDescription =
                                        "QR Code do WhatsApp",

                                    modifier =
                                        Modifier.size(260.dp)
                                )
                            }

                        } else {

                            Text(
                                text =
                                    "Não foi possível renderizar o QR.",

                                color =
                                    Color(0xFFFF667F)
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )


                        Text(
                            text =
                                "O QR pode expirar. Se isso acontecer, feche esta janela e tente conectar novamente.",

                            color =
                                SocietyWaDialogMuted,

                            fontSize =
                                12.sp,

                            textAlign =
                                TextAlign.Center
                        )
                    }


                    else -> {

                        Text(
                            text =
                                when (bot.status) {

                                    "connecting" ->
                                        "A sessão foi iniciada. Aguarde alguns segundos enquanto a Cloud recebe o QR ou código de pareamento."

                                    "logged_out" ->
                                        "A sessão anterior expirou. Inicie uma nova conexão."

                                    else ->
                                        "Nenhum QR ou código está disponível neste momento."
                                },

                            color =
                                SocietyWaDialogMuted,

                            textAlign =
                                TextAlign.Center
                        )
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    text = "Fechar",
                    color = SocietyWaDialogPurple
                )
            }
        }
    )
}


@Composable
private fun SocietyWhatsAppLogsDialog(
    title: String,
    lines: List<String>,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SocietyWaDialogBackground,

        title = {

            Text(
                text = "Logs • $title",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = 180.dp,
                            max = 440.dp
                        )
                        .background(
                            Color(0xFF07070C),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            SocietyWaDialogBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(14.dp)
            ) {

                if (lines.isEmpty()) {

                    Text(
                        text = "Nenhum log disponível.",
                        color = SocietyWaDialogMuted
                    )

                } else {

                    lines.forEach {
                        line ->

                        Text(
                            text = line,
                            color = Color(0xFFD8D3E2),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(5.dp)
                        )
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    text = "Fechar",
                    color = SocietyWaDialogPurple
                )
            }
        }
    )
}


private fun createQrBitmap(
    content: String
): Bitmap? {

    if (content.isBlank()) {
        return null
    }


    return try {

        val matrix =
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                720,
                720
            )


        val width =
            matrix.width

        val height =
            matrix.height

        val pixels =
            IntArray(
                width * height
            )


        for (y in 0 until height) {

            for (x in 0 until width) {

                pixels[
                    y * width + x
                ] =
                    if (matrix[x, y]) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
            }
        }


        Bitmap.createBitmap(
            pixels,
            width,
            height,
            Bitmap.Config.RGB_565
        )

    } catch (
        _: Exception
    ) {

        null
    }
}
