package opnet.fsocietydevs.devbots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


internal data class SocietyWhatsAppBotUi(
    val id: String,
    val name: String,

    val connected: Boolean,

    val statusText: String =
        if (connected) {
            "Sessão ativa"
        } else {
            "Sem sessão"
        },

    val detailText: String =
        if (connected) {
            "WhatsApp conectado"
        } else {
            "Conecte para ativar este bot"
        },

    val needsQr: Boolean = false,

    val status: String =
        if (connected) {
            "connected"
        } else {
            "disconnected"
        },

    val qr: String? = null,

    val pairingCode: String? = null
)


private val WhatsAppBackground =
    Color(
        0xFF05050A
    )


private val WhatsAppCard =
    Color(
        0xFF0D0C14
    )


private val WhatsAppCardBorder =
    Color(
        0xFF38205C
    )


private val WhatsAppPurple =
    Color(
        0xFF8B2CFF
    )


private val WhatsAppPurple2 =
    Color(
        0xFF5D13D8
    )


private val WhatsAppGreen =
    Color(
        0xFF52F5A7
    )


private val WhatsAppGreenDark =
    Color(
        0xFF073923
    )


private val WhatsAppRed =
    Color(
        0xFFFF5574
    )


private val WhatsAppMuted =
    Color(
        0xFFA5A0B3
    )


@Composable
internal fun SocietyWhatsAppBotsPage(
    bots: List<SocietyWhatsAppBotUi>,

    onOpenBot: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onLogs: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onConnect: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onShowQr: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onDisconnect: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onRestartSession: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onDeleteSession: (
        SocietyWhatsAppBotUi
    ) -> Unit
) {

    val connected =
        bots.filter {
            it.connected
        }


    val disconnected =
        bots.filter {
            !it.connected
        }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    WhatsAppBackground
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal =
                        16.dp
                )
                .padding(
                    bottom =
                        32.dp
                )
    ) {

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        SocietyWhatsAppSummary(
            connected =
                connected.size,

            disconnected =
                disconnected.size
        )


        Spacer(
            modifier =
                Modifier.height(
                    26.dp
                )
        )


        SocietyWhatsAppSectionTitle(
            title =
                "Bots conectados",

            count =
                connected.size,

            color =
                WhatsAppGreen
        )


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        if (
            connected.isEmpty()
        ) {

            SocietyWhatsAppEmptyState(
                text =
                    "Nenhum bot conectado ao WhatsApp."
            )

        } else {

            connected.forEach {

                bot ->

                SocietyWhatsAppBotCard(
                    bot =
                        bot,

                    onOpenBot =
                        onOpenBot,

                    onLogs =
                        onLogs,

                    onConnect =
                        onConnect,

                    onShowQr =
                        onShowQr,

                    onDisconnect =
                        onDisconnect,

                    onRestartSession =
                        onRestartSession,

                    onDeleteSession =
                        onDeleteSession
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )


        SocietyWhatsAppSectionTitle(
            title =
                "Bots desconectados",

            count =
                disconnected.size,

            color =
                WhatsAppRed
        )


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        if (
            disconnected.isEmpty()
        ) {

            SocietyWhatsAppEmptyState(
                text =
                    "Todos os seus bots estão conectados."
            )

        } else {

            disconnected.forEach {

                bot ->

                SocietyWhatsAppBotCard(
                    bot =
                        bot,

                    onOpenBot =
                        onOpenBot,

                    onLogs =
                        onLogs,

                    onConnect =
                        onConnect,

                    onShowQr =
                        onShowQr,

                    onDisconnect =
                        onDisconnect,

                    onRestartSession =
                        onRestartSession,

                    onDeleteSession =
                        onDeleteSession
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )
            }
        }
    }
}



@Composable
private fun SocietyWhatsAppSummary(
    connected: Int,
    disconnected: Int
) {

    val shape =
        RoundedCornerShape(
            22.dp
        )


    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    shape
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF151020),
                            Color(0xFF0B0911)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF52237E),
                    shape = shape
                )
                .padding(
                    18.dp
                )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        "Visão geral",

                    color =
                        Color.White,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        "Status das sessões do WhatsApp",

                    color =
                        WhatsAppMuted,

                    fontSize =
                        12.sp
                )
            }


            Box(
                modifier =
                    Modifier
                        .size(
                            38.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            Color(
                                0xFF221331
                            )
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "◉",

                    color =
                        WhatsAppPurple,

                    fontSize =
                        18.sp
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            SocietyWhatsAppStatCard(
                modifier =
                    Modifier.weight(
                        1f
                    ),

                number =
                    connected,

                label =
                    "Conectados",

                description =
                    "Online e prontos",

                color =
                    WhatsAppGreen,

                background =
                    Color(
                        0xFF0B2119
                    )
            )


            SocietyWhatsAppStatCard(
                modifier =
                    Modifier.weight(
                        1f
                    ),

                number =
                    disconnected,

                label =
                    "Desconectados",

                description =
                    "Precisam conectar",

                color =
                    WhatsAppRed,

                background =
                    Color(
                        0xFF241018
                    )
            )
        }
    }
}


@Composable
private fun SocietyWhatsAppStatCard(
    modifier: Modifier,
    number: Int,
    label: String,
    description: String,
    color: Color,
    background: Color
) {

    val shape =
        RoundedCornerShape(
            18.dp
        )


    Column(
        modifier =
            modifier
                .clip(
                    shape
                )
                .background(
                    background
                )
                .border(
                    width =
                        1.dp,

                    color =
                        color.copy(
                            alpha =
                                0.22f
                        ),

                    shape =
                        shape
                )
                .padding(
                    horizontal =
                        14.dp,

                    vertical =
                        15.dp
                )
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            10.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            color
                        )
            )


            Spacer(
                modifier =
                    Modifier.width(
                        9.dp
                    )
            )


            Text(
                text =
                    number.toString(),

                color =
                    Color.White,

                fontSize =
                    28.sp,

                fontWeight =
                    FontWeight.ExtraBold,

                maxLines =
                    1
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )


        Text(
            text =
                label,

            color =
                Color.White,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.SemiBold,

            maxLines =
                1
        )


        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )


        Text(
            text =
                description,

            color =
                WhatsAppMuted,

            fontSize =
                11.sp,

            maxLines =
                1,

            overflow =
                TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SocietyWhatsAppCounter(
    modifier: Modifier,

    count: Int,

    title: String,

    subtitle: String,

    color: Color
) {

    Row(
        modifier =
            modifier,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        52.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        color.copy(
                            alpha =
                                0.15f
                        )
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            17.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            color
                        )
            )
        }


        Spacer(
            modifier =
                Modifier.width(
                    14.dp
                )
        )


        Column {

            Text(
                text =
                    count.toString(),

                color =
                    Color.White,

                fontSize =
                    28.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    title,

                color =
                    Color.White,

                fontSize =
                    17.sp
            )


            Text(
                text =
                    subtitle,

                color =
                    WhatsAppMuted,

                fontSize =
                    12.sp
            )
        }
    }
}



@Composable
private fun SocietyWhatsAppSectionTitle(
    title: String,
    count: Int,
    color: Color
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        2.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        9.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        color
                    )
        )


        Spacer(
            modifier =
                Modifier.width(
                    10.dp
                )
        )


        Text(
            text =
                title,

            modifier =
                Modifier.weight(
                    1f
                ),

            color =
                Color.White,

            fontSize =
                19.sp,

            fontWeight =
                FontWeight.Bold,

            maxLines =
                1
        )


        Box(
            modifier =
                Modifier
                    .clip(
                        RoundedCornerShape(
                            999.dp
                        )
                    )
                    .background(
                        Color(
                            0xFF15131B
                        )
                    )
                    .padding(
                        horizontal =
                            10.dp,

                        vertical =
                            5.dp
                    )
        ) {

            Text(
                text =
                    "$count ${if (count == 1) "bot" else "bots"}",

                color =
                    WhatsAppMuted,

                fontSize =
                    11.sp,

                fontWeight =
                    FontWeight.Medium,

                maxLines =
                    1
            )
        }
    }
}

@Composable
private fun SocietyWhatsAppEmptyState(
    text: String
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        18.dp
                    )
                )
                .background(
                    WhatsAppCard
                )
                .border(
                    width =
                        1.dp,

                    color =
                        WhatsAppCardBorder,

                    shape =
                        RoundedCornerShape(
                            18.dp
                        )
                )
                .padding(
                    22.dp
                )
    ) {

        Text(
            text =
                text,

            color =
                WhatsAppMuted,

            fontSize =
                14.sp
        )
    }
}



@Composable
private fun SocietyWhatsAppBotCard(
    bot: SocietyWhatsAppBotUi,

    onOpenBot: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onLogs: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onConnect: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onShowQr: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onDisconnect: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onRestartSession: (
        SocietyWhatsAppBotUi
    ) -> Unit,

    onDeleteSession: (
        SocietyWhatsAppBotUi
    ) -> Unit
) {

    var menuExpanded by
        remember {
            mutableStateOf(
                false
            )
        }


    var confirmDelete by
        remember {
            mutableStateOf(
                false
            )
        }


    val accent =
        if (
            bot.connected
        ) {
            WhatsAppGreen
        } else {
            WhatsAppPurple
        }


    val cardShape =
        RoundedCornerShape(
            22.dp
        )


    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    cardShape
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF100E16),
                            Color(0xFF09080D)
                        )
                    )
                )
                .border(
                    width =
                        1.dp,

                    color =
                        if (
                            bot.connected
                        ) {

                            WhatsAppGreen.copy(
                                alpha =
                                    0.30f
                            )

                        } else {

                            Color(
                                0xFF4B276C
                            )
                        },

                    shape =
                        cardShape
                )
                .padding(
                    16.dp
                )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            52.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                16.dp
                            )
                        )
                        .background(
                            if (
                                bot.connected
                            ) {
                                Color(0xFF102A20)
                            } else {
                                Color(0xFF22202A)
                            }
                        )
                        .border(
                            width =
                                1.dp,

                            color =
                                accent.copy(
                                    alpha =
                                        0.20f
                                ),

                            shape =
                                RoundedCornerShape(
                                    16.dp
                                )
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        if (
                            bot.connected
                        ) {
                            "●"
                        } else {
                            "◎"
                        },

                    color =
                        accent,

                    fontSize =
                        24.sp
                )
            }


            Spacer(
                modifier =
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
                    text =
                        bot.name,

                    color =
                        Color.White,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
                        )
                )


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    7.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    when (
                                        bot.status
                                    ) {
                                        "connected" ->
                                            WhatsAppGreen

                                        "connecting",
                                        "qr",
                                        "pairing" ->
                                            WhatsAppPurple

                                        else ->
                                            Color(
                                                0xFF8E899B
                                            )
                                    }
                                )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                7.dp
                            )
                    )


                    Text(
                        text =
                            bot.statusText,

                        color =
                            when (
                                bot.status
                            ) {

                                "connected" ->
                                    WhatsAppGreen

                                "connecting",
                                "qr",
                                "pairing" ->
                                    Color(
                                        0xFFC598FF
                                    )

                                else ->
                                    Color(
                                        0xFFB9B4C4
                                    )
                            },

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }


            Box {

                Text(
                    text =
                        "⋮",

                    modifier =
                        Modifier
                            .clip(
                                CircleShape
                            )
                            .clickable {
                                menuExpanded =
                                    true
                            }
                            .padding(
                                horizontal =
                                    12.dp,

                                vertical =
                                    8.dp
                            ),

                    color =
                        Color(
                            0xFFCBA8F5
                        ),

                    fontSize =
                        26.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                DropdownMenu(
                    expanded =
                        menuExpanded,

                    onDismissRequest = {
                        menuExpanded =
                            false
                    }
                ) {

                    if (
                        bot.connected
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Logs"
                                )
                            },

                            onClick = {
                                menuExpanded =
                                    false

                                onLogs(
                                    bot
                                )
                            }
                        )


                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Reiniciar sessão"
                                )
                            },

                            onClick = {
                                menuExpanded =
                                    false

                                onRestartSession(
                                    bot
                                )
                            }
                        )


                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Desconectar"
                                )
                            },

                            onClick = {
                                menuExpanded =
                                    false

                                onDisconnect(
                                    bot
                                )
                            }
                        )

                    } else {

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Conectar"
                                )
                            },

                            onClick = {
                                menuExpanded =
                                    false

                                onConnect(
                                    bot
                                )
                            }
                        )


                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Ver QR"
                                )
                            },

                            onClick = {
                                menuExpanded =
                                    false

                                onShowQr(
                                    bot
                                )
                            }
                        )
                    }


                    DropdownMenuItem(
                        text = {

                            Text(
                                text =
                                    "Excluir sessão",

                                color =
                                    WhatsAppRed
                            )
                        },

                        onClick = {
                            menuExpanded =
                                false

                            confirmDelete =
                                true
                        }
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        Text(
            text =
                bot.detailText,

            color =
                WhatsAppMuted,

            fontSize =
                12.sp,

            maxLines =
                2,

            overflow =
                TextOverflow.Ellipsis
        )


        Spacer(
            modifier =
                Modifier.height(
                    15.dp
                )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            if (
                bot.connected
            ) {

                Box(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .defaultMinSize(
                                minHeight =
                                    48.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    15.dp
                                )
                            )
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF6D16E8),
                                        Color(0xFF9A28FF)
                                    )
                                )
                            )
                            .clickable {
                                onOpenBot(
                                    bot
                                )
                            }
                            .padding(
                                horizontal =
                                    12.dp,

                                vertical =
                                    13.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            "Abrir bot",

                        color =
                            Color.White,

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold,

                        maxLines =
                            1
                    )
                }


                Box(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .defaultMinSize(
                                minHeight =
                                    48.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    15.dp
                                )
                            )
                            .border(
                                width =
                                    1.dp,

                                color =
                                    Color(
                                        0xFF393543
                                    ),

                                shape =
                                    RoundedCornerShape(
                                        15.dp
                                    )
                            )
                            .clickable {
                                onLogs(
                                    bot
                                )
                            }
                            .padding(
                                horizontal =
                                    12.dp,

                                vertical =
                                    13.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            "Logs",

                        color =
                            Color.White,

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        maxLines =
                            1
                    )
                }

            } else {

                Box(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .defaultMinSize(
                                minHeight =
                                    48.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    15.dp
                                )
                            )
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF6813E5),
                                        Color(0xFF9E24FF)
                                    )
                                )
                            )
                            .clickable {
                                onConnect(
                                    bot
                                )
                            }
                            .padding(
                                horizontal =
                                    10.dp,

                                vertical =
                                    13.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            if (
                                bot.status ==
                                "connecting"
                            ) {
                                "Conectando..."
                            } else {
                                "Conectar"
                            },

                        color =
                            Color.White,

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold,

                        maxLines =
                            1
                    )
                }


                Box(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .defaultMinSize(
                                minHeight =
                                    48.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    15.dp
                                )
                            )
                            .border(
                                width =
                                    1.dp,

                                color =
                                    if (
                                        bot.needsQr
                                    ) {

                                        WhatsAppPurple.copy(
                                            alpha =
                                                0.65f
                                        )

                                    } else {

                                        Color(
                                            0xFF393543
                                        )
                                    },

                                shape =
                                    RoundedCornerShape(
                                        15.dp
                                    )
                            )
                            .clickable {
                                onShowQr(
                                    bot
                                )
                            }
                            .padding(
                                horizontal =
                                    10.dp,

                                vertical =
                                    13.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            if (
                                !bot.pairingCode
                                    .isNullOrBlank()
                            ) {
                                "Ver código"
                            } else {
                                "Ver QR"
                            },

                        color =
                            Color.White,

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        maxLines =
                            1
                    )
                }
            }
        }
    }


    if (
        confirmDelete
    ) {

        AlertDialog(
            onDismissRequest = {
                confirmDelete =
                    false
            },

            containerColor =
                Color(
                    0xFF111018
                ),

            title = {

                Text(
                    text =
                        "Excluir sessão?",

                    color =
                        Color.White,

                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {

                Text(
                    text =
                        "Isso remove apenas a autenticação do WhatsApp de ${bot.name}. O projeto do bot continuará salvo.",

                    color =
                        WhatsAppMuted
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        confirmDelete =
                            false

                        onDeleteSession(
                            bot
                        )
                    }
                ) {

                    Text(
                        text =
                            "Excluir sessão",

                        color =
                            WhatsAppRed,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        confirmDelete =
                            false
                    }
                ) {

                    Text(
                        text =
                            "Cancelar",

                        color =
                            Color.White
                    )
                }
            }
        )
    }
}

@Composable
private fun SocietyWhatsAppIcon(
    connected: Boolean
) {

    Box(
        modifier =
            Modifier
                .size(
                    58.dp
                )
                .clip(
                    RoundedCornerShape(
                        16.dp
                    )
                )
                .background(
                    if (
                        connected
                    ) {

                        Color(
                            0xFF0A3929
                        )

                    } else {

                        Color(
                            0xFF292935
                        )
                    }
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                "◉",

            color =
                if (
                    connected
                ) {
                    WhatsAppGreen
                } else {
                    Color(
                        0xFFB7B6C8
                    )
                },

            fontSize =
                35.sp
        )
    }
}


@Composable
private fun SocietyWhatsAppStatus(
    connected: Boolean
) {

    val color =
        if (
            connected
        ) {

            WhatsAppGreen

        } else {

            Color(
                0xFFAAA9BD
            )
        }


    Row(
        modifier =
            Modifier
                .clip(
                    RoundedCornerShape(
                        50.dp
                    )
                )
                .background(
                    if (
                        connected
                    ) {

                        WhatsAppGreenDark

                    } else {

                        Color(
                            0xFF171721
                        )
                    }
                )
                .border(
                    width =
                        1.dp,

                    color =
                        if (
                            connected
                        ) {

                            Color(
                                0xFF08733F
                            )

                        } else {

                            Color(
                                0xFF454457
                            )
                        },

                    shape =
                        RoundedCornerShape(
                            50.dp
                        )
                )
                .padding(
                    horizontal =
                        12.dp,

                    vertical =
                        7.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        9.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        color
                    )
        )


        Spacer(
            modifier =
                Modifier.width(
                    7.dp
                )
        )


        Text(
            text =
                if (
                    connected
                ) {
                    "Conectado"
                } else {
                    "Desconectado"
                },

            color =
                color,

            fontWeight =
                FontWeight.SemiBold,

            fontSize =
                12.sp
        )
    }
}


@Composable
private fun SocietyWhatsAppPrimaryButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .height(
                    48.dp
                )
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            WhatsAppPurple2,
                            WhatsAppPurple
                        )
                    )
                )
                .clickable {
                    onClick()
                },

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                text,

            color =
                Color.White,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.SemiBold
        )
    }
}


@Composable
private fun SocietyWhatsAppSecondaryButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .height(
                    48.dp
                )
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(
                    Color(
                        0xFF111019
                    )
                )
                .border(
                    width =
                        1.dp,

                    color =
                        Color(
                            0xFF393646
                        ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                )
                .clickable {
                    onClick()
                },

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                text,

            color =
                Color.White,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.SemiBold
        )
    }
}


@Composable
private fun SocietyWhatsAppDangerButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .height(
                    48.dp
                )
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(
                    Color(
                        0xFF111019
                    )
                )
                .border(
                    width =
                        1.dp,

                    color =
                        Color(
                            0xFF393646
                        ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                )
                .clickable {
                    onClick()
                },

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                text,

            color =
                WhatsAppRed,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.SemiBold
        )
    }
}
