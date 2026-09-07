package opnet.fsocietydevs.devbots

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


internal fun societyAiMessageStamp(): String =
    SimpleDateFormat(
        "dd/MM/yyyy • HH:mm",
        Locale.getDefault()
    )
        .format(
            Date()
        )


private fun displayMessageStamp(
    value: String
): String {

    if (
        Regex(
            """^\d{2}:\d{2}$"""
        )
            .matches(
                value
            )
    ) {

        val today =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )
                .format(
                    Date()
                )


        return "$today • $value"
    }


    return value
        .ifBlank {
            societyAiMessageStamp()
        }
}


private fun automaticChatTitle(
    text: String
): String {

    val cleaned =
        text
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()


    return cleaned
        .take(
            48
        )
        .ifBlank {
            "Nova conversa"
        }
}


private data class PickedAttachment(
    val uri: Uri,
    val name: String,
    val mime: String
)


private fun attachmentName(
    context: Context,
    uri: Uri
): String {

    var result =
        "anexo"


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
                    cursor
                        .getString(
                            index
                        )
                        ?: result
            }
        }


    return result
}


private suspend fun prepareAttachment(
    context: Context,
    picked: PickedAttachment
): SocietyAiAttachment =
    withContext(
        Dispatchers.IO
    ) {

        val bytes =
            context.contentResolver
                .openInputStream(
                    picked.uri
                )
                ?.use {
                    it.readBytes()
                }
                ?: throw Exception(
                    "Não consegui ler o arquivo."
                )


        if (
            bytes.size >
            7 *
            1024 *
            1024
        ) {

            throw Exception(
                "O anexo pode ter no máximo 7 MB."
            )
        }


        SocietyAiAttachment(
            name =
                picked.name,

            mime =
                picked.mime,

            base64 =
                Base64.encodeToString(
                    bytes,
                    Base64.NO_WRAP
                )
        )
    }


@Composable
internal fun SocietyAiPage(
    colors: SocietyColors,
    prefs: SharedPreferences
) {

    val context =
        LocalContext.current


    val scope =
        rememberCoroutineScope()


    val listState =
        rememberLazyListState()


    var chats by remember {
        mutableStateOf(
            emptyList<SocietyAiStoredChat>()
        )
    }


    var currentChat by remember {
        mutableStateOf<SocietyAiStoredChat?>(
            null
        )
    }


    val messages =
        remember {
            mutableStateListOf<SocietyAiStoredMessage>()
        }


    var input by remember {
        mutableStateOf("")
    }


    var loading by remember {
        mutableStateOf(false)
    }


    var loadingChats by remember {
        mutableStateOf(true)
    }


    var attachment by remember {
        mutableStateOf<PickedAttachment?>(
            null
        )
    }


    var menuChat by remember {
        mutableStateOf<SocietyAiStoredChat?>(
            null
        )
    }


    var renameChat by remember {
        mutableStateOf<SocietyAiStoredChat?>(
            null
        )
    }


    var renameValue by remember {
        mutableStateOf("")
    }


    var deletingChat by remember {
        mutableStateOf<SocietyAiStoredChat?>(
            null
        )
    }


    fun putChat(
        chat: SocietyAiStoredChat
    ) {

        chats =
            (
                chats.filter {
                    it.id !=
                        chat.id
                } +
                    chat
            )
                .sortedByDescending {
                    it.updatedAt
                }
    }


    fun openChat(
        chat: SocietyAiStoredChat
    ) {

        currentChat =
            chat


        messages.clear()


        messages.addAll(
            chat.messages
        )
    }


    fun newChat() {

        val now =
            System.currentTimeMillis()


        currentChat =
            SocietyAiStoredChat(
                id =
                    UUID.randomUUID()
                        .toString(),

                title =
                    "Novo chat",

                createdAt =
                    now,

                updatedAt =
                    now,

                messages =
                    emptyList()
            )


        messages.clear()


        messages.add(
            SocietyAiStoredMessage(
                role =
                    "assistant",

                text =
                    "Oi 👋 Eu sou a Society AI. Posso ajudar com seu bot, coding, comandos, imagens e documentos.",

                time =
                    societyAiMessageStamp()
            )
        )
    }


    LaunchedEffect(
        Unit
    ) {

        loadingChats =
            true


        try {

            chats =
                SocietyAiChatsApi
                    .list(
                        prefs
                    )

        } catch (
            error: Exception
        ) {

            Toast.makeText(
                context,
                error.message
                    ?: "Não consegui carregar seus chats.",
                Toast.LENGTH_LONG
            ).show()

        } finally {

            loadingChats =
                false
        }
    }


    LaunchedEffect(
        messages.size,
        loading,
        currentChat?.id
    ) {

        if (
            currentChat !=
                null &&
            messages.isNotEmpty()
        ) {

            listState
                .animateScrollToItem(
                    messages.lastIndex
                )
        }
    }


    val imageLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            uri ->

            if (
                uri !=
                    null
            ) {

                attachment =
                    PickedAttachment(
                        uri =
                            uri,

                        name =
                            attachmentName(
                                context,
                                uri
                            ),

                        mime =
                            context.contentResolver
                                .getType(
                                    uri
                                )
                                ?: "image/jpeg"
                    )
            }
        }


    val documentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) {
            uri ->

            if (
                uri !=
                    null
            ) {

                attachment =
                    PickedAttachment(
                        uri =
                            uri,

                        name =
                            attachmentName(
                                context,
                                uri
                            ),

                        mime =
                            context.contentResolver
                                .getType(
                                    uri
                                )
                                ?: "application/octet-stream"
                    )
            }
        }


    val voiceLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {
            result ->

            if (
                result.resultCode ==
                    Activity.RESULT_OK
            ) {

                val spoken =
                    result.data
                        ?.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                        )
                        ?.firstOrNull()
                        .orEmpty()


                if (
                    spoken.isNotBlank()
                ) {

                    input =
                        if (
                            input.isBlank()
                        )
                            spoken
                        else
                            "$input $spoken"
                }
            }
        }


    fun send() {

        if (
            loading
        ) {
            return
        }


        val chat =
            currentChat
                ?: return


        val picked =
            attachment


        val prompt =
            input
                .trim()
                .ifBlank {

                    if (
                        picked !=
                            null
                    )
                        "Analise este anexo."
                    else
                        ""
                }


        if (
            prompt.isBlank()
        ) {
            return
        }


        val history =
            messages
                .takeLast(
                    16
                )
                .map {

                    SocietyAiHistoryItem(
                        role =
                            it.role,

                        content =
                            it.text
                    )
                }


        val visibleText =
            if (
                picked !=
                    null
            )
                prompt +
                    "\n📎 " +
                    picked.name
            else
                prompt


        messages.add(
            SocietyAiStoredMessage(
                role =
                    "user",

                text =
                    visibleText,

                time =
                    societyAiMessageStamp()
            )
        )


        var updated =
            chat.copy(
                title =
                    if (
                        chat.title ==
                            "Novo chat"
                    )
                        automaticChatTitle(
                            prompt
                        )
                    else
                        chat.title,

                updatedAt =
                    System.currentTimeMillis(),

                messages =
                    messages.toList()
            )


        currentChat =
            updated


        putChat(
            updated
        )


        input =
            ""


        attachment =
            null


        loading =
            true


        scope.launch {

            try {

                try {

                    updated =
                        SocietyAiChatsApi
                            .save(
                                prefs,
                                updated
                            )


                    currentChat =
                        updated


                    putChat(
                        updated
                    )

                } catch (
                    _: Exception
                ) {}


                val prepared =
                    if (
                        picked !=
                            null
                    )
                        prepareAttachment(
                            context,
                            picked
                        )
                    else
                        null


                val reply =
                    SocietyAiApi
                        .chat(
                            prefs =
                                prefs,

                            prompt =
                                prompt,

                            history =
                                history,

                            attachment =
                                prepared
                        )


                messages.add(
                    SocietyAiStoredMessage(
                        role =
                            "assistant",

                        text =
                            reply.answer,

                        provider =
                            reply.provider,

                        time =
                            societyAiMessageStamp()
                    )
                )


                updated =
                    updated.copy(
                        updatedAt =
                            System.currentTimeMillis(),

                        messages =
                            messages.toList()
                    )


                updated =
                    SocietyAiChatsApi
                        .save(
                            prefs,
                            updated
                        )


                currentChat =
                    updated


                putChat(
                    updated
                )

            } catch (
                error: Exception
            ) {

                messages.add(
                    SocietyAiStoredMessage(
                        role =
                            "assistant",

                        text =
                            error.message
                                ?: "Não consegui responder agora.",

                        time =
                            societyAiMessageStamp()
                    )
                )


                updated =
                    updated.copy(
                        updatedAt =
                            System.currentTimeMillis(),

                        messages =
                            messages.toList()
                    )


                currentChat =
                    updated


                putChat(
                    updated
                )


                try {

                    SocietyAiChatsApi
                        .save(
                            prefs,
                            updated
                        )

                } catch (
                    _: Exception
                ) {}

            } finally {

                loading =
                    false
            }
        }
    }


    if (
        currentChat ==
            null
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal =
                            12.dp
                    )
        ) {

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
                    Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier =
                        Modifier.padding(
                            18.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Surface(
                        color =
                            colors.primary.copy(
                                alpha =
                                    0.22f
                            ),

                        shape =
                            RoundedCornerShape(
                                18.dp
                            ),

                        modifier =
                            Modifier.size(
                                58.dp
                            )
                    ) {

                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                "✦",
                                color =
                                    colors.primarySoft,

                                fontSize =
                                    27.sp
                            )
                        }
                    }


                    Spacer(
                        Modifier.width(
                            14.dp
                        )
                    )


                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {

                        Text(
                            "Society AI",
                            color =
                                colors.text,

                            fontSize =
                                20.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(
                            "Assistente inteligente • chats sincronizados",
                            color =
                                colors.muted,

                            fontSize =
                                12.sp
                        )
                    }


                    Surface(
                        color =
                            colors.primary,

                        shape =
                            CircleShape,

                        modifier =
                            Modifier
                                .size(
                                    46.dp
                                )
                                .clickable {
                                    newChat()
                                }
                    ) {

                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                "+",
                                color =
                                    Color.White,

                                fontSize =
                                    27.sp
                            )
                        }
                    }
                }
            }


            Spacer(
                Modifier.height(
                    22.dp
                )
            )


            Text(
                "Conversas",
                color =
                    colors.text,

                fontSize =
                    21.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                "Toque para abrir • segure para renomear ou excluir",
                color =
                    colors.muted,

                fontSize =
                    11.sp
            )


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            if (
                loadingChats
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                30.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color =
                            colors.primarySoft
                    )
                }

            } else if (
                chats.isEmpty()
            ) {

                Surface(
                    color =
                        colors.surface,

                    shape =
                        RoundedCornerShape(
                            20.dp
                        ),

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                newChat()
                            }
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    ) {

                        Text(
                            "✦ Comece uma conversa",
                            color =
                                colors.text,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                16.sp
                        )


                        Spacer(
                            Modifier.height(
                                5.dp
                            )
                        )


                        Text(
                            "Seus chats aparecerão aqui e voltarão após você entrar novamente na conta.",
                            color =
                                colors.muted,

                            fontSize =
                                12.sp
                        )
                    }
                }

            } else {

                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        ),

                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    items(
                        chats,
                        key = {
                            it.id
                        }
                    ) {
                        chat ->

                        Surface(
                            color =
                                colors.surface,

                            shape =
                                RoundedCornerShape(
                                    18.dp
                                ),

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .pointerInput(
                                        chat.id
                                    ) {

                                        detectTapGestures(
                                            onTap = {

                                                openChat(
                                                    chat
                                                )
                                            },

                                            onLongPress = {

                                                menuChat =
                                                    chat
                                            }
                                        )
                                    }
                        ) {

                            Row(
                                modifier =
                                    Modifier.padding(
                                        15.dp
                                    ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Surface(
                                    color =
                                        colors.primary.copy(
                                            alpha =
                                                0.15f
                                        ),

                                    shape =
                                        RoundedCornerShape(
                                            14.dp
                                        ),

                                    modifier =
                                        Modifier.size(
                                            44.dp
                                        )
                                ) {

                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Text(
                                            "✦",
                                            color =
                                                colors.primarySoft,

                                            fontSize =
                                                20.sp
                                        )
                                    }
                                }


                                Spacer(
                                    Modifier.width(
                                        12.dp
                                    )
                                )


                                Column(
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                ) {

                                    Text(
                                        chat.title,
                                        color =
                                            colors.text,

                                        fontWeight =
                                            FontWeight.Bold,

                                        fontSize =
                                            15.sp,

                                        maxLines =
                                            1
                                    )


                                    Text(
                                        chat.messages
                                            .lastOrNull()
                                            ?.text
                                            ?.replace(
                                                "\n",
                                                " "
                                            )
                                            ?.take(
                                                65
                                            )
                                            ?: "Nova conversa",
                                        color =
                                            colors.muted,

                                        fontSize =
                                            11.sp,

                                        maxLines =
                                            1
                                    )
                                }


                                Text(
                                    "›",
                                    color =
                                        colors.primarySoft,

                                    fontSize =
                                        25.sp
                                )
                            }
                        }
                    }
                }
            }
        }

    } else {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(
                        horizontal =
                            10.dp
                    )
        ) {

            /*
             * VISUAL SOCIETY.
             * Não é mais cabeçalho estilo WhatsApp.
             */
            Surface(
                color =
                    colors.primary.copy(
                        alpha =
                            0.12f
                    ),

                shape =
                    RoundedCornerShape(
                        20.dp
                    ),

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier =
                        Modifier.padding(
                            12.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        "‹",
                        color =
                            colors.text,

                        fontSize =
                            31.sp,

                        modifier =
                            Modifier
                                .clickable {

                                    currentChat =
                                        null


                                    messages.clear()
                                }
                                .padding(
                                    horizontal =
                                        8.dp
                                )
                    )


                    Surface(
                        color =
                            colors.primary.copy(
                                alpha =
                                    0.22f
                            ),

                        shape =
                            RoundedCornerShape(
                                15.dp
                            ),

                        modifier =
                            Modifier.size(
                                48.dp
                            )
                    ) {

                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                "✦",
                                color =
                                    colors.primarySoft,

                                fontSize =
                                    24.sp
                            )
                        }
                    }


                    Spacer(
                        Modifier.width(
                            11.dp
                        )
                    )


                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {

                        Text(
                            currentChat
                                ?.title
                                ?: "Society AI",

                            color =
                                colors.text,

                            fontSize =
                                17.sp,

                            fontWeight =
                                FontWeight.Bold,

                            maxLines =
                                1
                        )


                        Text(
                            if (
                                loading
                            )
                                "Society AI está digitando..."
                            else
                                "Society AI • online",

                            color =
                                colors.muted,

                            fontSize =
                                11.sp
                        )
                    }


                    Text(
                        "⋮",
                        color =
                            colors.text,

                        fontSize =
                            25.sp,

                        modifier =
                            Modifier
                                .clickable {

                                    currentChat
                                        ?.let {
                                            menuChat =
                                                it
                                        }
                                }
                                .padding(
                                    8.dp
                                )
                    )
                }
            }


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            LazyColumn(
                state =
                    listState,

                verticalArrangement =
                    Arrangement.spacedBy(
                        7.dp
                    ),

                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .fillMaxWidth()
            ) {

                itemsIndexed(
                    messages
                ) {
                    _,
                    message ->

                    val mine =
                        message.role ==
                            "user"


                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            if (
                                mine
                            )
                                Arrangement.End
                            else
                                Arrangement.Start
                    ) {

                        /*
                         * BALÕES VOLTARAM AO VISUAL SOCIETY.
                         */
                        Surface(
                            color =
                                if (
                                    mine
                                )
                                    colors.primary.copy(
                                        alpha =
                                            0.20f
                                    )
                                else
                                    colors.surface,

                            shape =
                                RoundedCornerShape(
                                    topStart =
                                        18.dp,

                                    topEnd =
                                        18.dp,

                                    bottomStart =
                                        if (
                                            mine
                                        )
                                            18.dp
                                        else
                                            5.dp,

                                    bottomEnd =
                                        if (
                                            mine
                                        )
                                            5.dp
                                        else
                                            18.dp
                                ),

                            modifier =
                                Modifier.widthIn(
                                    max =
                                        325.dp
                                )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        horizontal =
                                            13.dp,

                                        vertical =
                                            9.dp
                                    )
                            ) {

                                if (
                                    !mine
                                ) {

                                    Text(
                                        "✦ Society AI",
                                        color =
                                            colors.primarySoft,

                                        fontSize =
                                            11.sp,

                                        fontWeight =
                                            FontWeight.Bold
                                    )


                                    Spacer(
                                        Modifier.height(
                                            4.dp
                                        )
                                    )
                                }


                                Text(
                                    message.text,
                                    color =
                                        colors.text,

                                    fontSize =
                                        14.sp,

                                    lineHeight =
                                        20.sp
                                )


                                Spacer(
                                    Modifier.height(
                                        5.dp
                                    )
                                )


                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.End,

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(
                                        displayMessageStamp(
                                            message.time
                                        ),

                                        color =
                                            colors.muted,

                                        fontSize =
                                            9.sp
                                    )


                                    if (
                                        mine
                                    ) {

                                        Spacer(
                                            Modifier.width(
                                                4.dp
                                            )
                                        )


                                        Text(
                                            "✓✓",
                                            color =
                                                colors.primarySoft,

                                            fontSize =
                                                9.sp
                                        )
                                    }
                                }


                                if (
                                    !mine &&
                                    message.provider
                                        .isNotBlank()
                                ) {

                                    Text(
                                        "via ${message.provider}",
                                        color =
                                            colors.muted.copy(
                                                alpha =
                                                    0.70f
                                            ),

                                        fontSize =
                                            8.sp
                                    )
                                }
                            }
                        }
                    }
                }


                if (
                    loading
                ) {

                    item {

                        Surface(
                            color =
                                colors.surface,

                            shape =
                                RoundedCornerShape(
                                    18.dp
                                )
                        ) {

                            Row(
                                modifier =
                                    Modifier.padding(
                                        horizontal =
                                            14.dp,

                                        vertical =
                                            10.dp
                                    ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                CircularProgressIndicator(
                                    color =
                                        colors.primarySoft,

                                    strokeWidth =
                                        2.dp,

                                    modifier =
                                        Modifier.size(
                                            16.dp
                                        )
                                )


                                Spacer(
                                    Modifier.width(
                                        8.dp
                                    )
                                )


                                Text(
                                    "Society AI está digitando...",
                                    color =
                                        colors.muted,

                                    fontSize =
                                        11.sp
                                )
                            }
                        }
                    }
                }
            }


            attachment
                ?.let {
                    selected ->

                    Surface(
                        color =
                            colors.surface,

                        shape =
                            RoundedCornerShape(
                                14.dp
                            ),

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    bottom =
                                        5.dp
                                )
                    ) {

                        Row(
                            modifier =
                                Modifier.padding(
                                    horizontal =
                                        12.dp,

                                    vertical =
                                        7.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                "📎 ${selected.name}",
                                color =
                                    colors.text,

                                fontSize =
                                    11.sp,

                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            )


                            Text(
                                "✕",
                                color =
                                    colors.text,

                                modifier =
                                    Modifier.clickable {
                                        attachment =
                                            null
                                    }
                            )
                        }
                    }
                }


            /*
             * SOMENTE A PARTE DE BAIXO
             * CONTINUA NO FORMATO WHATSAPP.
             */
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top =
                                4.dp,

                            bottom =
                                7.dp
                        ),

                verticalAlignment =
                    Alignment.Bottom
            ) {

                Surface(
                    color =
                        Color(
                            0xFF1B191F
                        ),

                    shape =
                        RoundedCornerShape(
                            27.dp
                        ),

                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,

                        modifier =
                            Modifier.padding(
                                horizontal =
                                    7.dp,

                                vertical =
                                    4.dp
                            )
                    ) {

                        Text(
                            "☺",
                            color =
                                colors.muted,

                            fontSize =
                                23.sp,

                            modifier =
                                Modifier.padding(
                                    6.dp
                                )
                        )


                        BasicTextField(
                            value =
                                input,

                            onValueChange = {
                                input =
                                    it
                            },

                            maxLines =
                                5,

                            textStyle =
                                TextStyle(
                                    color =
                                        colors.text,

                                    fontSize =
                                        15.sp
                                ),

                            keyboardOptions =
                                KeyboardOptions(
                                    imeAction =
                                        ImeAction.Send
                                ),

                            keyboardActions =
                                KeyboardActions(
                                    onSend = {
                                        send()
                                    }
                                ),

                            modifier =
                                Modifier.weight(
                                    1f
                                ),

                            decorationBox = {
                                inner ->

                                Box(
                                    modifier =
                                        Modifier.padding(
                                            vertical =
                                                9.dp
                                        )
                                ) {

                                    if (
                                        input.isBlank()
                                    ) {

                                        Text(
                                            "Mensagem",
                                            color =
                                                colors.muted,

                                            fontSize =
                                                15.sp
                                        )
                                    }


                                    inner()
                                }
                            }
                        )


                        Text(
                            "📎",
                            fontSize =
                                19.sp,

                            modifier =
                                Modifier
                                    .clickable {

                                        documentLauncher
                                            .launch(
                                                arrayOf(
                                                    "*/*"
                                                )
                                            )
                                    }
                                    .padding(
                                        7.dp
                                    )
                        )


                        Text(
                            "📷",
                            fontSize =
                                19.sp,

                            modifier =
                                Modifier
                                    .clickable {

                                        imageLauncher
                                            .launch(
                                                "image/*"
                                            )
                                    }
                                    .padding(
                                        7.dp
                                    )
                        )
                    }
                }


                Spacer(
                    Modifier.width(
                        7.dp
                    )
                )


                Surface(
                    color =
                        colors.primary,

                    shape =
                        CircleShape,

                    modifier =
                        Modifier
                            .size(
                                52.dp
                            )
                            .clickable {

                                if (
                                    input.isNotBlank() ||
                                    attachment !=
                                        null
                                ) {

                                    send()

                                } else {

                                    try {

                                        val intent =
                                            Intent(
                                                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                                            )
                                                .putExtra(
                                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                                )
                                                .putExtra(
                                                    RecognizerIntent.EXTRA_LANGUAGE,
                                                    Locale.getDefault()
                                                        .toLanguageTag()
                                                )


                                        voiceLauncher
                                            .launch(
                                                intent
                                            )

                                    } catch (
                                        _: Exception
                                    ) {

                                        Toast.makeText(
                                            context,
                                            "Reconhecimento de voz indisponível.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            if (
                                input.isNotBlank() ||
                                attachment !=
                                    null
                            )
                                "➤"
                            else
                                "🎙",

                            color =
                                Color.White,

                            fontSize =
                                20.sp
                        )
                    }
                }
            }
        }
    }


    if (
        menuChat !=
            null
    ) {

        val selected =
            menuChat!!


        AlertDialog(
            onDismissRequest = {
                menuChat =
                    null
            },

            title = {
                Text(
                    selected.title
                )
            },

            text = {

                Column {

                    TextButton(
                        onClick = {

                            renameChat =
                                selected


                            renameValue =
                                selected.title


                            menuChat =
                                null
                        }
                    ) {

                        Text(
                            "✏️ Renomear chat"
                        )
                    }


                    TextButton(
                        onClick = {

                            deletingChat =
                                selected


                            menuChat =
                                null
                        }
                    ) {

                        Text(
                            "🗑 Excluir chat"
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        menuChat =
                            null
                    }
                ) {

                    Text(
                        "Cancelar"
                    )
                }
            }
        )
    }


    if (
        renameChat !=
            null
    ) {

        AlertDialog(
            onDismissRequest = {
                renameChat =
                    null
            },

            title = {
                Text(
                    "Renomear chat"
                )
            },

            text = {

                OutlinedTextField(
                    value =
                        renameValue,

                    onValueChange = {
                        renameValue =
                            it
                    },

                    singleLine =
                        true,

                    label = {
                        Text(
                            "Título"
                        )
                    }
                )
            },

            confirmButton = {

                TextButton(
                    enabled =
                        renameValue
                            .isNotBlank(),

                    onClick = {

                        val target =
                            renameChat
                                ?: return@TextButton


                        scope.launch {

                            try {

                                val renamed =
                                    SocietyAiChatsApi
                                        .save(
                                            prefs,
                                            target.copy(
                                                title =
                                                    renameValue
                                                        .trim(),

                                                updatedAt =
                                                    System.currentTimeMillis()
                                            )
                                        )


                                putChat(
                                    renamed
                                )


                                if (
                                    currentChat?.id ==
                                        renamed.id
                                ) {

                                    currentChat =
                                        renamed
                                }


                                renameChat =
                                    null

                            } catch (
                                error: Exception
                            ) {

                                Toast.makeText(
                                    context,
                                    error.message
                                        ?: "Não consegui renomear o chat.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {

                    Text(
                        "Salvar"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        renameChat =
                            null
                    }
                ) {

                    Text(
                        "Cancelar"
                    )
                }
            }
        )
    }


    if (
        deletingChat !=
            null
    ) {

        AlertDialog(
            onDismissRequest = {
                deletingChat =
                    null
            },

            title = {
                Text(
                    "Excluir conversa?"
                )
            },

            text = {
                Text(
                    "Ela será removida da sua conta Society."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val target =
                            deletingChat
                                ?: return@TextButton


                        scope.launch {

                            try {

                                SocietyAiChatsApi
                                    .delete(
                                        prefs,
                                        target.id
                                    )


                                chats =
                                    chats.filter {
                                        it.id !=
                                            target.id
                                    }


                                if (
                                    currentChat?.id ==
                                        target.id
                                ) {

                                    currentChat =
                                        null


                                    messages.clear()
                                }


                                deletingChat =
                                    null

                            } catch (
                                error: Exception
                            ) {

                                Toast.makeText(
                                    context,
                                    error.message
                                        ?: "Não consegui excluir o chat.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {

                    Text(
                        "Excluir"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deletingChat =
                            null
                    }
                ) {

                    Text(
                        "Cancelar"
                    )
                }
            }
        )
    }
}


/* ============================================================
 * SOCIETY AI - CRIADOR DE COMANDOS
 * ============================================================ */

@Composable
internal fun SocietyAiCommandAssistant(
    colors: SocietyColors,
    prefs: SharedPreferences,
    currentType: String,
    onGenerated: (SocietyAiCommandDraft) -> Unit
) {

    val scope =
        rememberCoroutineScope()


    var prompt by remember {
        mutableStateOf("")
    }


    var generating by remember {
        mutableStateOf(false)
    }


    var error by remember {
        mutableStateOf<String?>(
            null
        )
    }


    Surface(
        color =
            colors.primary.copy(
                alpha =
                    0.11f
            ),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    9.dp
                )
        ) {

            Text(
                "✦ Society AI",
                color =
                    colors.primarySoft,

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    15.sp
            )


            Text(
                "Descreva o comando e eu preparo tudo para você revisar.",
                color =
                    colors.muted,

                fontSize =
                    11.sp
            )


            OutlinedTextField(
                value =
                    prompt,

                onValueChange = {
                    prompt =
                        it
                },

                placeholder = {
                    Text(
                        "Ex.: crie /bomdia que envie uma mensagem divertida..."
                    )
                },

                minLines =
                    2,

                maxLines =
                    5,

                modifier =
                    Modifier.fillMaxWidth()
            )


            error
                ?.let {

                    Text(
                        it,
                        color =
                            colors.primarySoft,

                        fontSize =
                            11.sp
                    )
                }


            Surface(
                color =
                    colors.primary,

                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled =
                                !generating &&
                                prompt
                                    .trim()
                                    .length >=
                                    4
                        ) {

                            val originalPrompt =
                                prompt.trim()


                            scope.launch {

                                generating =
                                    true


                                error =
                                    null


                                try {

                                    val generated =
                                        SocietyAiApi
                                            .commandDraft(
                                                prefs =
                                                    prefs,

                                                description =
                                                    originalPrompt,

                                                type =
                                                    currentType
                                            )


                                    onGenerated(
                                        generated
                                    )


                                    try {

                                        val answer =
                                            buildString {

                                                append(
                                                    "Criei o comando /"
                                                )

                                                append(
                                                    generated.name
                                                )

                                                append(
                                                    "."
                                                )


                                                if (
                                                    generated.description
                                                        .isNotBlank()
                                                ) {

                                                    append(
                                                        "\n\nDescrição: "
                                                    )

                                                    append(
                                                        generated.description
                                                    )
                                                }


                                                if (
                                                    generated.response
                                                        .isNotBlank()
                                                ) {

                                                    append(
                                                        "\n\nResposta:\n"
                                                    )

                                                    append(
                                                        generated.response
                                                    )
                                                }


                                                append(
                                                    "\n\nVocê pode continuar ajustando esse comando comigo."
                                                )
                                            }


                                        SocietyAiChatsApi
                                            .saveGeneratedCommandChat(
                                                prefs =
                                                    prefs,

                                                commandName =
                                                    generated.name,

                                                prompt =
                                                    originalPrompt,

                                                answer =
                                                    answer
                                            )

                                    } catch (
                                        _: Exception
                                    ) {}


                                    prompt =
                                        ""

                                } catch (
                                    exception: Exception
                                ) {

                                    error =
                                        exception.message
                                            ?: "Não foi possível gerar o comando."

                                } finally {

                                    generating =
                                        false
                                }
                            }
                        }
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center,

                    modifier =
                        Modifier.padding(
                            vertical =
                                13.dp
                        )
                ) {

                    if (
                        generating
                    ) {

                        CircularProgressIndicator(
                            color =
                                Color.White,

                            strokeWidth =
                                2.dp,

                            modifier =
                                Modifier.size(
                                    18.dp
                                )
                        )

                    } else {

                        Text(
                            "✨ Gerar comando com IA",
                            color =
                                Color.White,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
