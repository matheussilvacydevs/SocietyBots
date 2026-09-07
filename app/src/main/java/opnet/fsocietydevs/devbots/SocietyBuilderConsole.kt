package opnet.fsocietydevs.devbots

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private data class ConsoleLine(
    val id: Long,
    val type: String,
    val text: String
)


private fun qrBitmap(
    content: String,
    size: Int = 720
): Bitmap? {

    if (content.isBlank()) {
        return null
    }

    return try {

        val matrix =
            QRCodeWriter()
                .encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
                )

        val bitmap =
            Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.RGB_565
            )

        for (x in 0 until size) {

            for (y in 0 until size) {

                bitmap.setPixel(
                    x,
                    y,
                    if (matrix[x, y])
                        android.graphics.Color.BLACK
                    else
                        android.graphics.Color.WHITE
                )
            }
        }

        bitmap

    } catch (
        _: Exception
    ) {

        null
    }
}


@Composable
internal fun SocietyBuilderConsolePage(
    colors: SocietyColors,
    draft: BotDraft,
    uid: String,
    onCancel: () -> Unit,
    onBotReady: (String) -> Unit
) {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val listState =
        rememberLazyListState()


    var session by remember {
        mutableStateOf<BuilderSession?>(null)
    }

    var consoleLines by remember {
        mutableStateOf(
            emptyList<ConsoleLine>()
        )
    }

    var lastEventId by remember {
        mutableIntStateOf(0)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var sendingAnswer by remember {
        mutableStateOf(false)
    }

    var buildStarted by remember {
        mutableStateOf(false)
    }

    var fatalError by remember {
        mutableStateOf<String?>(null)
    }

    var textAnswer by remember {
        mutableStateOf("")
    }

    var multiAnswer by remember {
        mutableStateOf(
            emptySet<String>()
        )
    }

    var latestQr by remember {
        mutableStateOf<String?>(null)
    }

    var pairingCode by remember {
        mutableStateOf<String?>(null)
    }

    var readyDelivered by remember {
        mutableStateOf(false)
    }


    fun appendLine(
        type: String,
        text: String
    ) {

        if (text.isBlank()) {
            return
        }

        consoleLines =
            consoleLines +
                ConsoleLine(
                    id =
                        System.nanoTime(),

                    type =
                        type,

                    text =
                        text
                )
    }


    /*
     * Inicia a entrevista uma única vez.
     */
    LaunchedEffect(Unit) {

        try {

            appendLine(
                "system",
                "Inicializando Society AI..."
            )

            val created =
                SocietyBuilderApi.start(
                    uid =
                        uid.ifBlank {
                            "local-user"
                        },

                    name =
                        draft.name.ifBlank {
                            "Meu Bot"
                        },

                    owner =
                        draft.owner,

                    library =
                        draft.library,

                    style =
                        draft.style,

                    features =
                        draft.features
                )

            session =
                created

            appendLine(
                "assistant",
                "Olá. Vamos construir ${draft.name.ifBlank { "seu bot" }}."
            )

            created.question
                ?.let {

                    appendLine(
                        "assistant",
                        it.title
                    )

                    if (
                        it.text.isNotBlank()
                    ) {

                        appendLine(
                            "assistant",
                            it.text
                        )
                    }
                }

        } catch (
            error: Exception
        ) {

            fatalError =
                error.message
                    ?: "Não foi possível iniciar o Builder."

            appendLine(
                "error",
                fatalError!!
            )

        } finally {

            loading =
                false
        }
    }


    /*
     * Polling dos logs da Cloud.
     */
    LaunchedEffect(
        session?.id
    ) {

        val id =
            session?.id
                ?: return@LaunchedEffect

        while (true) {

            try {

                val result =
                    SocietyBuilderApi.events(
                        sessionId =
                            id,

                        after =
                            lastEventId
                    )

                session =
                    result.first

                result.second
                    .forEach { event ->

                        if (
                            event.id >
                            lastEventId
                        ) {

                            lastEventId =
                                event.id
                        }


                        when (
                            event.type
                        ) {

                            "qr" -> {

                                latestQr =
                                    event.qr

                                pairingCode =
                                    null

                                appendLine(
                                    "assistant",
                                    "QR Code recebido. Escaneie pelo WhatsApp."
                                )
                            }


                            "pairing" -> {

                                pairingCode =
                                    event.code

                                latestQr =
                                    null

                                appendLine(
                                    "assistant",
                                    "Código de pareamento gerado."
                                )
                            }


                            "connected" -> {

                                appendLine(
                                    "success",
                                    "✓ WhatsApp conectado com sucesso."
                                )

                                appendLine(
                                    "success",
                                    "✓ Sessão salva."
                                )

                                appendLine(
                                    "success",
                                    "✓ Bot pronto."
                                )
                            }


                            "assistant" -> {

                                /*
                                 * Evita duplicar perguntas que
                                 * já atualizamos imediatamente
                                 * após cada resposta.
                                 */
                                if (
                                    !consoleLines.any {
                                        it.text ==
                                            event.message
                                    }
                                ) {

                                    appendLine(
                                        "assistant",
                                        event.message
                                    )
                                }
                            }


                            "user" -> {

                                if (
                                    !consoleLines.any {
                                        it.text ==
                                            "> ${event.message}"
                                    }
                                ) {

                                    appendLine(
                                        "user",
                                        "> ${event.message}"
                                    )
                                }
                            }


                            "error" -> {

                                appendLine(
                                    "error",
                                    event.message
                                )
                            }


                            "console" -> {

                                appendLine(
                                    "console",
                                    event.message
                                )
                            }


                            else -> {

                                appendLine(
                                    "system",
                                    event.message
                                )
                            }
                        }
                    }


                val current =
                    result.first

                if (
                    current.status ==
                        "connected" &&
                    !readyDelivered
                ) {

                    readyDelivered =
                        true

                    val botId =
                        current.botId

                    if (
                        !botId.isNullOrBlank()
                    ) {

                        onBotReady(
                            botId
                        )
                    }
                }

            } catch (
                _: Exception
            ) {

                /*
                 * Uma falha temporária não mata o console.
                 */
            }

            delay(
                900
            )
        }
    }


    /*
     * Sempre acompanha a saída mais recente.
     */
    LaunchedEffect(
        consoleLines.size,
        latestQr,
        pairingCode
    ) {

        delay(
            80
        )

        if (
            consoleLines.isNotEmpty()
        ) {

            try {

                listState.animateScrollToItem(
                    consoleLines.lastIndex
                )

            } catch (
                _: Exception
            ) {}
        }
    }


    fun submitAnswer(
        value: Any,
        humanLabel: String
    ) {

        val current =
            session
                ?: return

        if (sendingAnswer) {
            return
        }

        sendingAnswer =
            true

        appendLine(
            "user",
            "> $humanLabel"
        )


        scope.launch {

            try {

                val updated =
                    SocietyBuilderApi.answer(
                        sessionId =
                            current.id,

                        value =
                            value
                    )

                session =
                    updated

                textAnswer =
                    ""

                multiAnswer =
                    emptySet()


                updated.question
                    ?.let { question ->

                        appendLine(
                            "assistant",
                            question.title
                        )

                        if (
                            question.text
                                .isNotBlank()
                        ) {

                            appendLine(
                                "assistant",
                                question.text
                            )
                        }
                    }

            } catch (
                error: Exception
            ) {

                appendLine(
                    "error",
                    error.message
                        ?: "Não foi possível enviar a resposta."
                )

            } finally {

                sendingAnswer =
                    false
            }
        }
    }


    fun confirmAndBuild() {

        val current =
            session
                ?: return

        if (
            sendingAnswer ||
            buildStarted
        ) {
            return
        }

        sendingAnswer =
            true

        appendLine(
            "user",
            "> Construir bot"
        )


        scope.launch {

            try {

                val confirmed =
                    SocietyBuilderApi.answer(
                        sessionId =
                            current.id,

                        value =
                            "confirm"
                    )

                session =
                    confirmed

                appendLine(
                    "assistant",
                    "Configuração aprovada."
                )

                appendLine(
                    "assistant",
                    "Iniciando construção na Cloud..."
                )


                val building =
                    SocietyBuilderApi.build(
                        sessionId =
                            confirmed.id
                    )

                session =
                    building

                buildStarted =
                    true

            } catch (
                error: Exception
            ) {

                appendLine(
                    "error",
                    error.message
                        ?: "Não foi possível iniciar a construção."
                )

            } finally {

                sendingAnswer =
                    false
            }
        }
    }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    colors.background
                )
    ) {

        /*
         * CABEÇALHO
         */
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            18.dp,

                        vertical =
                            14.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                color =
                    colors.surface,

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                modifier =
                    Modifier
                        .size(
                            44.dp
                        )
                        .clickable {
                            onCancel()
                        }
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "←",

                        color =
                            colors.text,

                        fontSize =
                            22.sp
                    )
                }
            }


            Spacer(
                Modifier.width(
                    13.dp
                )
            )


            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    "Society AI Console",

                    color =
                        colors.text,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    when (
                        session?.status
                    ) {

                        "interview" ->
                            "Configurando bot"

                        "ready" ->
                            "Pronto para construir"

                        "building" ->
                            "Construindo"

                        "running" ->
                            "Aguardando WhatsApp"

                        "connected" ->
                            "Online"

                        "error" ->
                            "Erro"

                        else ->
                            "Inicializando"
                    },

                    color =
                        if (
                            session?.status ==
                            "connected"
                        ) {
                            Color(
                                0xFF7CEAA2
                            )
                        } else {
                            colors.secondary
                        },

                    fontSize =
                        11.sp
                )
            }


            Surface(
                color =
                    colors.primary.copy(
                        alpha =
                            0.16f
                    ),

                shape =
                    RoundedCornerShape(
                        50.dp
                    )
            ) {

                Text(
                    if (
                        session?.status ==
                        "connected"
                    )
                        "● ONLINE"
                    else
                        "● AI",

                    color =
                        if (
                            session?.status ==
                            "connected"
                        )
                            Color(
                                0xFF72E79A
                            )
                        else
                            colors.primarySoft,

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier.padding(
                            horizontal =
                                10.dp,

                            vertical =
                                6.dp
                        )
                )
            }
        }


        HorizontalDivider(
            color =
                colors.border.copy(
                    alpha =
                        0.55f
                )
        )


        /*
         * TERMINAL
         */
        Surface(
            color =
                Color(
                    0xFF08080D
                ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(
                        1f
                    )
        ) {

            LazyColumn(
                state =
                    listState,

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal =
                                17.dp,

                            vertical =
                                16.dp
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        7.dp
                    )
            ) {

                item {

                    Text(
                        "society@cloud:~$",

                        color =
                            colors.primarySoft,

                        fontFamily =
                            FontFamily.Monospace,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            13.sp
                    )


                    Spacer(
                        Modifier.height(
                            6.dp
                        )
                    )
                }


                items(
                    items =
                        consoleLines,

                    key = {
                        it.id
                    }
                ) { line ->

                    Text(
                        text =
                            line.text,

                        color =
                            when (
                                line.type
                            ) {

                                "assistant" ->
                                    Color(
                                        0xFFD6A4FF
                                    )

                                "user" ->
                                    Color(
                                        0xFFF2F2F4
                                    )

                                "error" ->
                                    Color(
                                        0xFFFF808A
                                    )

                                "success" ->
                                    Color(
                                        0xFF7CEAA2
                                    )

                                "console" ->
                                    Color(
                                        0xFFAEB4C2
                                    )

                                else ->
                                    Color(
                                        0xFF828795
                                    )
                            },

                        fontFamily =
                            FontFamily.Monospace,

                        fontSize =
                            12.sp,

                        lineHeight =
                            18.sp
                    )
                }


                if (
                    latestQr !=
                    null
                ) {

                    item {

                        Spacer(
                            Modifier.height(
                                12.dp
                            )
                        )


                        val bitmap =
                            remember(
                                latestQr
                            ) {
                                latestQr
                                    ?.let {
                                        qrBitmap(
                                            it
                                        )
                                    }
                            }


                        bitmap
                            ?.let {

                                Surface(
                                    color =
                                        Color.White,

                                    shape =
                                        RoundedCornerShape(
                                            20.dp
                                        ),

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal =
                                                    16.dp
                                            )
                                ) {

                                    Image(
                                        bitmap =
                                            it.asImageBitmap(),

                                        contentDescription =
                                            "QR Code WhatsApp",

                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(
                                                    1f
                                                )
                                                .padding(
                                                    14.dp
                                                )
                                    )
                                }
                            }


                        Spacer(
                            Modifier.height(
                                10.dp
                            )
                        )


                        Text(
                            "WhatsApp → Dispositivos conectados → Conectar dispositivo",

                            color =
                                Color(
                                    0xFFAEB4C2
                                ),

                            textAlign =
                                TextAlign.Center,

                            fontFamily =
                                FontFamily.Monospace,

                            fontSize =
                                11.sp,

                            modifier =
                                Modifier.fillMaxWidth()
                        )
                    }
                }


                if (
                    pairingCode !=
                    null
                ) {

                    item {

                        Spacer(
                            Modifier.height(
                                13.dp
                            )
                        )


                        Surface(
                            color =
                                Color(
                                    0xFF15121D
                                ),

                            shape =
                                RoundedCornerShape(
                                    20.dp
                                ),

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        colors.primary,
                                        RoundedCornerShape(
                                            20.dp
                                        )
                                    )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        20.dp
                                    ),

                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(
                                    "CÓDIGO DE PAREAMENTO",

                                    color =
                                        Color(
                                            0xFF9FA4B1
                                        ),

                                    fontSize =
                                        10.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Spacer(
                                    Modifier.height(
                                        10.dp
                                    )
                                )


                                Text(
                                    pairingCode.orEmpty(),

                                    color =
                                        colors.primarySoft,

                                    fontFamily =
                                        FontFamily.Monospace,

                                    fontSize =
                                        27.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Spacer(
                                    Modifier.height(
                                        12.dp
                                    )
                                )


                                TextButton(
                                    onClick = {

                                        val clipboard =
                                            context
                                                .getSystemService(
                                                    Context.CLIPBOARD_SERVICE
                                                ) as
                                                ClipboardManager

                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText(
                                                "Código Society",
                                                pairingCode
                                            )
                                        )
                                    }
                                ) {

                                    Text(
                                        "Copiar código",

                                        color =
                                            colors.primarySoft
                                    )
                                }
                            }
                        }
                    }
                }


                if (
                    session?.status ==
                    "connected"
                ) {

                    item {

                        Spacer(
                            Modifier.height(
                                15.dp
                            )
                        )


                        Text(
                            "society@cloud:~$ bot online_",

                            color =
                                Color(
                                    0xFF7CEAA2
                                ),

                            fontFamily =
                                FontFamily.Monospace,

                            fontSize =
                                12.sp
                        )
                    }
                }
            }
        }


        /*
         * AÇÕES DO CONSOLE
         */
        Surface(
            color =
                Color(
                    0xFF0D0D13
                ),

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(
                modifier =
                    Modifier.padding(
                        horizontal =
                            15.dp,

                        vertical =
                            10.dp
                    ),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                Surface(
                    color =
                        colors.surfaceAlt,

                    shape =
                        RoundedCornerShape(
                            15.dp
                        ),

                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .clickable {

                                val text =
                                    consoleLines
                                        .joinToString(
                                            "\n"
                                        ) {
                                            it.text
                                        }


                                val clipboard =
                                    context
                                        .getSystemService(
                                            Context.CLIPBOARD_SERVICE
                                        ) as
                                        ClipboardManager


                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "Society AI Console",
                                        text
                                    )
                                )
                            }
                ) {

                    Text(
                        "⧉  Copiar",

                        color =
                            colors.primarySoft,

                        textAlign =
                            TextAlign.Center,

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        modifier =
                            Modifier.padding(
                                vertical =
                                    12.dp
                            )
                    )
                }


                Surface(
                    color =
                        colors.surfaceAlt,

                    shape =
                        RoundedCornerShape(
                            15.dp
                        ),

                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .clickable {

                                /*
                                 * Limpa somente a tela.
                                 * Não apaga a sessão nem o bot.
                                 */
                                consoleLines =
                                    emptyList()
                            }
                ) {

                    Text(
                        "⌫  Limpar",

                        color =
                            colors.secondary,

                        textAlign =
                            TextAlign.Center,

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        modifier =
                            Modifier.padding(
                                vertical =
                                    12.dp
                            )
                    )
                }
            }
        }


        /*
         * ÁREA INTERATIVA
         */
        Surface(
            color =
                colors.surface,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            15.dp
                        )
            ) {

                if (
                    loading
                ) {

                    LinearProgressIndicator(
                        modifier =
                            Modifier.fillMaxWidth(),

                        color =
                            colors.primary,

                        trackColor =
                            colors.border
                    )
                }


                fatalError
                    ?.let {

                        Text(
                            it,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,

                            fontSize =
                                12.sp
                        )
                    }


                val question =
                    session?.question


                AnimatedVisibility(
                    visible =
                        question !=
                        null &&
                        session?.status ==
                        "interview"
                ) {

                    question
                        ?.let { q ->

                            Column {

                                Text(
                                    q.title,

                                    color =
                                        colors.text,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        14.sp
                                )


                                if (
                                    q.text.isNotBlank()
                                ) {

                                    Spacer(
                                        Modifier.height(
                                            3.dp
                                        )
                                    )


                                    Text(
                                        q.text,

                                        color =
                                            colors.secondary,

                                        fontSize =
                                            11.sp
                                    )
                                }


                                Spacer(
                                    Modifier.height(
                                        12.dp
                                    )
                                )


                                when (
                                    q.type
                                ) {

                                    "choice" -> {

                                        q.options
                                            .forEach { option ->

                                                ConsoleChoiceButton(
                                                    colors =
                                                        colors,

                                                    text =
                                                        option.label,

                                                    enabled =
                                                        !sendingAnswer
                                                ) {

                                                    submitAnswer(
                                                        value =
                                                            option.id,

                                                        humanLabel =
                                                            option.label
                                                    )
                                                }


                                                Spacer(
                                                    Modifier.height(
                                                        7.dp
                                                    )
                                                )
                                            }
                                    }


                                    "text" -> {

                                        OutlinedTextField(
                                            value =
                                                textAnswer,

                                            onValueChange = {
                                                textAnswer =
                                                    it.take(
                                                        32
                                                    )
                                            },

                                            singleLine =
                                                true,

                                            placeholder = {
                                                Text(
                                                    q.placeholder
                                                        ?: ""
                                                )
                                            },

                                            modifier =
                                                Modifier.fillMaxWidth()
                                        )


                                        Spacer(
                                            Modifier.height(
                                                9.dp
                                            )
                                        )


                                        Button(
                                            onClick = {

                                                if (
                                                    textAnswer
                                                        .isNotBlank()
                                                ) {

                                                    submitAnswer(
                                                        value =
                                                            textAnswer,

                                                        humanLabel =
                                                            textAnswer
                                                    )
                                                }
                                            },

                                            enabled =
                                                textAnswer
                                                    .isNotBlank() &&
                                                !sendingAnswer,

                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {

                                            Text(
                                                "Continuar"
                                            )
                                        }
                                    }


                                    "multi" -> {

                                        q.options
                                            .chunked(
                                                2
                                            )
                                            .forEach { row ->

                                                Row(
                                                    modifier =
                                                        Modifier.fillMaxWidth(),

                                                    horizontalArrangement =
                                                        Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                ) {

                                                    row.forEach { option ->

                                                        val selected =
                                                            option.id in
                                                                multiAnswer

                                                        Surface(
                                                            color =
                                                                if (
                                                                    selected
                                                                )
                                                                    colors.primary.copy(
                                                                        alpha =
                                                                            0.2f
                                                                    )
                                                                else
                                                                    colors.surfaceAlt,

                                                            shape =
                                                                RoundedCornerShape(
                                                                    15.dp
                                                                ),

                                                            modifier =
                                                                Modifier
                                                                    .weight(
                                                                        1f
                                                                    )
                                                                    .border(
                                                                        1.dp,
                                                                        if (
                                                                            selected
                                                                        )
                                                                            colors.primary
                                                                        else
                                                                            colors.border,
                                                                        RoundedCornerShape(
                                                                            15.dp
                                                                        )
                                                                    )
                                                                    .clickable {

                                                                        multiAnswer =
                                                                            if (
                                                                                selected
                                                                            ) {
                                                                                multiAnswer -
                                                                                    option.id
                                                                            } else {
                                                                                multiAnswer +
                                                                                    option.id
                                                                            }
                                                                    }
                                                        ) {

                                                            Text(
                                                                if (
                                                                    selected
                                                                )
                                                                    "✓ ${option.label}"
                                                                else
                                                                    option.label,

                                                                color =
                                                                    if (
                                                                        selected
                                                                    )
                                                                        colors.primarySoft
                                                                    else
                                                                        colors.text,

                                                                fontSize =
                                                                    12.sp,

                                                                modifier =
                                                                    Modifier.padding(
                                                                        12.dp
                                                                    )
                                                            )
                                                        }
                                                    }


                                                    if (
                                                        row.size ==
                                                        1
                                                    ) {

                                                        Spacer(
                                                            Modifier.weight(
                                                                1f
                                                            )
                                                        )
                                                    }
                                                }


                                                Spacer(
                                                    Modifier.height(
                                                        8.dp
                                                    )
                                                )
                                            }


                                        Button(
                                            onClick = {

                                                submitAnswer(
                                                    value =
                                                        multiAnswer,

                                                    humanLabel =
                                                        if (
                                                            multiAnswer
                                                                .isEmpty()
                                                        )
                                                            "Nenhum recurso adicional"
                                                        else
                                                            multiAnswer
                                                                .joinToString(
                                                                    ", "
                                                                )
                                                )
                                            },

                                            enabled =
                                                !sendingAnswer,

                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {

                                            Text(
                                                "Continuar"
                                            )
                                        }
                                    }


                                    "confirm" -> {

                                        Button(
                                            onClick = {
                                                confirmAndBuild()
                                            },

                                            enabled =
                                                !sendingAnswer &&
                                                !buildStarted,

                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {

                                            Text(
                                                if (
                                                    sendingAnswer
                                                )
                                                    "Preparando..."
                                                else
                                                    "Construir bot"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }


                if (
                    session?.status ==
                        "building" ||
                    session?.status ==
                        "running"
                ) {

                    LinearProgressIndicator(
                        modifier =
                            Modifier.fillMaxWidth(),

                        color =
                            colors.primary,

                        trackColor =
                            colors.border
                    )


                    Spacer(
                        Modifier.height(
                            7.dp
                        )
                    )


                    Text(
                        if (
                            session?.status ==
                            "building"
                        )
                            "A Cloud está montando seu bot..."
                        else
                            "Bot iniciado. Aguardando autenticação do WhatsApp...",

                        color =
                            colors.secondary,

                        fontSize =
                            11.sp
                    )
                }


                if (
                    session?.status ==
                    "connected"
                ) {

                    Surface(
                        color =
                            Color(
                                0xFF12351F
                            ),

                        shape =
                            RoundedCornerShape(
                                17.dp
                            ),

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "✓ Bot criado e conectado",

                            color =
                                Color(
                                    0xFF8EF1AF
                                ),

                            textAlign =
                                TextAlign.Center,

                            fontWeight =
                                FontWeight.Bold,

                            modifier =
                                Modifier.padding(
                                    14.dp
                                )
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ConsoleChoiceButton(
    colors: SocietyColors,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Surface(
        color =
            colors.surfaceAlt,

        shape =
            RoundedCornerShape(
                16.dp
            ),

        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    colors.border,
                    RoundedCornerShape(
                        16.dp
                    )
                )
                .clickable(
                    enabled =
                        enabled
                ) {
                    onClick()
                }
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal =
                        15.dp,

                    vertical =
                        13.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                "›",

                color =
                    colors.primarySoft,

                fontSize =
                    21.sp
            )


            Spacer(
                Modifier.width(
                    9.dp
                )
            )


            Text(
                text,

                color =
                    colors.text,

                fontWeight =
                    FontWeight.SemiBold,

                fontSize =
                    13.sp
            )
        }
    }
}
