package opnet.fsocietydevs.devbots

import android.content.Intent
import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

private const val PREFS = "devbots_preferences"

/*
 * Troque pelos seus links quando o site estiver pronto.
 */
private const val TERMS_URL =
    "https://example.com/termos"

private const val PRIVACY_URL =
    "https://example.com/privacidade"

internal data class SocietyColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val primarySoft: Color,
    val text: Color,
    val secondary: Color,
    val muted: Color,
    val border: Color
)

private val Dark = SocietyColors(
    background = Color(0xFF07070B),
    surface = Color(0xFF111116),
    surfaceAlt = Color(0xFF19131F),
    primary = Color(0xFF9148D4),
    primarySoft = Color(0xFFC178E8),
    text = Color(0xFFF7F5F8),
    secondary = Color(0xFFA6A0AA),
    muted = Color(0xFF706B74),
    border = Color(0xFF2D2932)
)

private val Light = SocietyColors(
    background = Color(0xFFF5F1F7),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEEE7F2),
    primary = Color(0xFF7538A7),
    primarySoft = Color(0xFF955DBD),
    text = Color(0xFF201A24),
    secondary = Color(0xFF6C6371),
    muted = Color(0xFF918794),
    border = Color(0xFFDCD3E0)
)

private enum class Screen {
    BOT_NAME,
    OWNER,
    LIBRARY,
    FEATURES,
    STYLE,
    EULA,
    AUTH,
    FORGOT_PASSWORD,
    AI_PROFILE,
    HOME
}

private enum class AuthMode {
    LOGIN,
    REGISTER
}

internal data class BotDraft(
    val name: String = "",
    val owner: String = "",
    val library: String = "",
    val features: Set<String> = emptySet(),
    val style: String = ""
)


private data class SocietyBotRecord(
    val id: String,
    val draft: BotDraft,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "disconnected"
)


private fun loadSocietyBots(
    prefs: android.content.SharedPreferences
): List<SocietyBotRecord> {

    val raw =
        prefs.getString(
            "society_bots_v1",
            null
        )

    if (!raw.isNullOrBlank()) {

        try {

            val array =
                org.json.JSONArray(
                    raw
                )

            val result =
                mutableListOf<SocietyBotRecord>()

            for (
                i in
                0 until array.length()
            ) {

                val item =
                    array.getJSONObject(
                        i
                    )

                val featuresJson =
                    item.optJSONArray(
                        "features"
                    )

                val features =
                    mutableSetOf<String>()

                if (
                    featuresJson !=
                    null
                ) {

                    for (
                        j in
                        0 until
                        featuresJson.length()
                    ) {

                        features +=
                            featuresJson
                                .optString(j)
                    }
                }

                result +=
                    SocietyBotRecord(
                        id =
                            item.optString(
                                "id"
                            ),

                        draft =
                            BotDraft(
                                name =
                                    item.optString(
                                        "name"
                                    ),

                                owner =
                                    item.optString(
                                        "owner"
                                    ),

                                library =
                                    item.optString(
                                        "library"
                                    ),

                                features =
                                    features,

                                style =
                                    item.optString(
                                        "style"
                                    )
                            ),

                        archived =
                            item.optBoolean(
                                "archived",
                                false
                            ),

                        createdAt =
                            item.optLong(
                                "createdAt",
                                System.currentTimeMillis()
                            )
                    )
            }

            return result

        } catch (
            _: Exception
        ) {}
    }


    /*
     * Migração automática do bot antigo.
     */
    if (
        prefs.getBoolean(
            "created_bot_exists",
            false
        )
    ) {

        val old =
            SocietyBotRecord(
                id =
                    UUID.randomUUID()
                        .toString(),

                draft =
                    BotDraft(
                        name =
                            prefs.getString(
                                "created_bot_name",
                                ""
                            ) ?: "",

                        owner =
                            prefs.getString(
                                "created_bot_owner",
                                ""
                            ) ?: "",

                        library =
                            prefs.getString(
                                "created_bot_library",
                                ""
                            ) ?: "",

                        features =
                            prefs.getStringSet(
                                "created_bot_features",
                                emptySet()
                            ) ?: emptySet(),

                        style =
                            prefs.getString(
                                "created_bot_style",
                                ""
                            ) ?: ""
                    )
            )

        return listOf(
            old
        )
    }


    return emptyList()
}


private fun saveSocietyBots(
    prefs: android.content.SharedPreferences,
    bots: List<SocietyBotRecord>
) {

    val array =
        org.json.JSONArray()

    bots.forEach { bot ->

        val features =
            org.json.JSONArray()

        bot.draft.features
            .forEach {
                features.put(
                    it
                )
            }

        array.put(
            org.json.JSONObject()
                .put(
                    "id",
                    bot.id
                )
                .put(
                    "name",
                    bot.draft.name
                )
                .put(
                    "owner",
                    bot.draft.owner
                )
                .put(
                    "library",
                    bot.draft.library
                )
                .put(
                    "features",
                    features
                )
                .put(
                    "style",
                    bot.draft.style
                )
                .put(
                    "archived",
                    bot.archived
                )
                .put(
                    "createdAt",
                    bot.createdAt
                )
        )
    }

    prefs.edit()
        .putString(
            "society_bots_v1",
            array.toString()
        )
        .apply()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs =
            getSharedPreferences(PREFS, MODE_PRIVATE)

        setContent {

            var darkMode by remember {
                mutableStateOf(
                    prefs.getBoolean("dark_mode", true)
                )
            }

            val colors =
                if (darkMode) Dark else Light


            SocietyUpdateHost(
                colors =
                    colors
            )

            var languageCode by remember {
                mutableStateOf(
                    prefs.getString(
                        "language_code",
                        detectLanguage()
                    ) ?: "pt-BR"
                )
            }

            var draft by remember {
                mutableStateOf(
                    BotDraft(
                        name = prefs.getString("bot_name", "") ?: "",
                        owner = prefs.getString("owner_number", "") ?: "",
                        library = prefs.getString("library", "") ?: "",
                        features =
                            prefs.getStringSet(
                                "features",
                                emptySet()
                            ) ?: emptySet(),
                        style = prefs.getString("bot_style", "") ?: ""
                    )
                )
            }

            val onboardingDone =
                prefs.getBoolean("onboarding_complete", false)

            val eulaAccepted =
                prefs.getBoolean("eula_accepted_v1", false)

            val authenticated =
                !prefs.getString(
                    "auth_id_token",
                    null
                ).isNullOrBlank()

            val justRegistered =
                prefs.getBoolean(
                    "just_registered",
                    false
                )

            var screen by remember {
                mutableStateOf(
                    when {
                        !onboardingDone ->
                            Screen.BOT_NAME

                        !eulaAccepted ->
                            Screen.EULA

                        !authenticated ->
                            Screen.AUTH

                        justRegistered ->
                            Screen.AI_PROFILE

                        else ->
                            Screen.HOME
                    }
                )
            }

            var authMode by remember {
                mutableStateOf(AuthMode.LOGIN)
            }

            MaterialTheme(
                colorScheme =
                    if (darkMode) {
                        darkColorScheme(
                            primary = colors.primary,
                            background = colors.background,
                            surface = colors.surface,
                            onBackground = colors.text,
                            onSurface = colors.text
                        )
                    } else {
                        lightColorScheme(
                            primary = colors.primary,
                            background = colors.background,
                            surface = colors.surface,
                            onBackground = colors.text,
                            onSurface = colors.text
                        )
                    }
            ) {

                when (screen) {

                    Screen.BOT_NAME ->
                        BotNameScreen(
                            colors = colors,
                            darkMode = darkMode,

                            onToggleTheme = {
                                darkMode = !darkMode

                                prefs.edit()
                                    .putBoolean(
                                        "dark_mode",
                                        darkMode
                                    )
                                    .apply()
                            },

                            onSkip = {
                                prefs.edit()
                                    .putBoolean(
                                        "onboarding_complete",
                                        true
                                    )
                                    .apply()

                                screen = Screen.EULA
                            },

                            value = draft.name,

                            onChange = {
                                draft =
                                    draft.copy(
                                        name = it
                                    )
                            },

                            onNext = {
                                prefs.edit()
                                    .putString(
                                        "bot_name",
                                        draft.name
                                    )
                                    .apply()

                                screen =
                                    Screen.OWNER
                            }
                        )

                    Screen.OWNER ->
                        OwnerScreen(
                            colors,
                            draft.owner,
                            {
                                draft =
                                    draft.copy(
                                        owner = it
                                    )
                            },
                            {
                                prefs.edit()
                                    .putString(
                                        "owner_number",
                                        draft.owner
                                    )
                                    .apply()

                                screen =
                                    Screen.LIBRARY
                            }
                        )

                    Screen.LIBRARY ->
                        LibraryScreen(
                            colors,
                            draft.library,
                            {
                                draft =
                                    draft.copy(
                                        library = it
                                    )
                            },
                            {
                                prefs.edit()
                                    .putString(
                                        "library",
                                        draft.library
                                    )
                                    .apply()

                                screen =
                                    Screen.FEATURES
                            }
                        )

                    Screen.FEATURES ->
                        FeaturesScreen(
                            colors,
                            draft.features,
                            {
                                draft =
                                    draft.copy(
                                        features = it
                                    )
                            },
                            {
                                prefs.edit()
                                    .putStringSet(
                                        "features",
                                        draft.features
                                    )
                                    .apply()

                                screen =
                                    Screen.STYLE
                            }
                        )

                    Screen.STYLE ->
                        StyleScreen(
                            colors,
                            draft.style,
                            {
                                draft =
                                    draft.copy(
                                        style = it
                                    )
                            },
                            {
                                prefs.edit()
                                    .putString(
                                        "bot_style",
                                        draft.style
                                    )
                                    .putBoolean(
                                        "onboarding_complete",
                                        true
                                    )
                                    .apply()

                                screen =
                                    Screen.EULA
                            }
                        )

                    Screen.EULA ->
                        EulaScreen(
                            colors = colors,
                            darkMode = darkMode,
                            languageCode = languageCode,

                            onLanguageChange = {
                                languageCode = it

                                prefs.edit()
                                    .putString(
                                        "language_code",
                                        it
                                    )
                                    .apply()
                            },

                            onToggleTheme = {
                                darkMode = !darkMode

                                prefs.edit()
                                    .putBoolean(
                                        "dark_mode",
                                        darkMode
                                    )
                                    .apply()
                            },

                            onContinue = {
                                prefs.edit()
                                    .putBoolean(
                                        "eula_accepted_v1",
                                        true
                                    )
                                    .apply()

                                screen =
                                    Screen.AUTH
                            }
                        )

                    Screen.AUTH ->
                        AuthScreen(
                            colors = colors,
                            mode = authMode,

                            onModeChange = {
                                authMode = it
                            },

                            onForgot = {
                                screen =
                                    Screen.FORGOT_PASSWORD
                            },

                            onSuccess = {

                                if (
                                    authMode ==
                                    AuthMode.REGISTER
                                ) {

                                    prefs.edit()
                                        .putBoolean(
                                            "just_registered",
                                            true
                                        )
                                        .putBoolean(
                                            "ai_profile_generated",
                                            false
                                        )
                                        .apply()

                                    screen =
                                        Screen.AI_PROFILE

                                } else {

                                    prefs.edit()
                                        .remove(
                                            "just_registered"
                                        )
                                        .apply()

                                    screen =
                                        Screen.HOME
                                }
                            }
                        )

                    Screen.FORGOT_PASSWORD ->
                        ForgotPasswordScreen(
                            colors = colors,

                            onBack = {
                                screen =
                                    Screen.AUTH
                            }
                        )

                    Screen.AI_PROFILE ->
                        AiProfileScreen(
                            colors,
                            draft
                        ) {
                            prefs.edit()
                                .putBoolean(
                                    "ai_profile_generated",
                                    true
                                )
                                .putBoolean(
                                    "just_registered",
                                    false
                                )
                                .putBoolean(
                                    "initial_bot_draft_ready",
                                    draft.name.isNotBlank() ||
                                    draft.owner.isNotBlank() ||
                                    draft.library.isNotBlank() ||
                                    draft.features.isNotEmpty() ||
                                    draft.style.isNotBlank()
                                )
                                .apply()

                            screen =
                                Screen.HOME
                        }

                    Screen.HOME ->
                        HomeScreen(
                            colors = colors,
                            draft = draft,
                            darkMode = darkMode,

                            onToggleTheme = {
                                darkMode =
                                    !darkMode

                                prefs.edit()
                                    .putBoolean(
                                        "dark_mode",
                                        darkMode
                                    )
                                    .apply()
                            },

                            onLogout = {

                                prefs.edit()
                                    .remove(
                                        "auth_id_token"
                                    )
                                    .remove(
                                        "auth_refresh_token"
                                    )
                                    .remove(
                                        "auth_expires_in"
                                    )
                                    .remove(
                                        "auth_saved_at"
                                    )
                                    .remove(
                                        "auth_uid"
                                    )
                                    .remove(
                                        "auth_email"
                                    )
                                    .remove(
                                        "auth_display_name"
                                    )
                                    .remove(
                                        "auth_email_verified"
                                    )
                                    .remove(
                                        "just_registered"
                                    )
                                    .apply()

                                authMode =
                                    AuthMode.LOGIN

                                screen =
                                    Screen.AUTH
                            }
                        )
                }
            }
        }
    }

    private fun detectLanguage(): String {

        val language =
            Locale.getDefault()
                .language
                .lowercase()

        return when (language) {
            "pt" -> "pt-BR"
            "es" -> "es-ES"
            "fr" -> "fr-FR"
            "de" -> "de-DE"
            "it" -> "it-IT"
            "ja" -> "ja-JP"
            "ko" -> "ko-KR"
            "zh" -> "zh-CN"
            "ru" -> "ru-RU"
            "ar" -> "ar-SA"
            "hi" -> "hi-IN"
            "tr" -> "tr-TR"
            "nl" -> "nl-NL"
            "pl" -> "pl-PL"
            "id" -> "id-ID"
            "vi" -> "vi-VN"
            "th" -> "th-TH"
            "sv" -> "sv-SE"
            "uk" -> "uk-UA"
            else -> "en-US"
        }
    }
}

/* ============================================================
   COMPONENTES
   ============================================================ */

@Composable
private fun BasePage(
    colors: SocietyColors,
    progress: Float? = null,
    keyboardAware: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {

    val pageScrollState =
        rememberScrollState()

    val pageModifier =
        if (keyboardAware) {

            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(
                    pageScrollState
                )
                .imePadding()
                .padding(
                    horizontal = 24.dp
                )

        } else {

            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = 24.dp
                )
        }

    Column(
        modifier =
            pageModifier
    ) {

        if (progress != null) {

            Spacer(
                Modifier.height(18.dp)
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = colors.primarySoft,
                trackColor = colors.surfaceAlt
            )
        }

        content()
    }
}

@Composable
private fun ThemeAndSkip(
    colors: SocietyColors,
    darkMode: Boolean,
    onTheme: () -> Unit,
    onSkip: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),

        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(28.dp),

            modifier = Modifier.clickable {
                onTheme()
            }
        ) {

            Row(
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (darkMode)
                        "🌙  ✦"
                    else
                        "☀️  ☁️",

                    fontSize = 17.sp
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    text = if (darkMode)
                        "Escuro"
                    else
                        "Claro",

                    color = colors.primarySoft,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(22.dp),

            modifier = Modifier.clickable {
                onSkip()
            }
        ) {

            Text(
                text = "Pular",
                color = colors.primarySoft,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,

                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 9.dp
                )
            )
        }
    }
}



@Composable
private fun PrimaryButton(
    colors: SocietyColors,
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape =
            RoundedCornerShape(22.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    colors.primary,
                contentColor =
                    Color.White
            )
    ) {

        Text(
            title,
            fontSize = 17.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun ChoiceCard(
    colors: SocietyColors,
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected)
                    colors.surfaceAlt
                else
                    colors.surface,
                RoundedCornerShape(20.dp)
            )
            .border(
                if (selected)
                    2.dp
                else
                    1.dp,
                if (selected)
                    colors.primary
                else
                    colors.border,
                RoundedCornerShape(20.dp)
            )
            .clickable {
                onClick()
            }
            .padding(19.dp)
    ) {

        Text(
            title,
            color = colors.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        if (subtitle != null) {

            Spacer(
                Modifier.height(7.dp)
            )

            Text(
                subtitle,
                color =
                    colors.secondary,
                fontSize =
                    13.sp,
                lineHeight =
                    19.sp
            )
        }
    }
}

/* ============================================================
   BOT NAME
   ============================================================ */

@Composable
private fun BotNameScreen(
    colors: SocietyColors,
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    onSkip: () -> Unit,
    value: String,
    onChange: (String) -> Unit,
    onNext: () -> Unit
) {

    var focused by remember {
        mutableStateOf(false)
    }

    val inputScale by animateFloatAsState(
        targetValue = if (focused) 1.015f else 1f,
        label = "botNameInputScale"
    )

    val titleScale by animateFloatAsState(
        targetValue = if (value.isNotBlank()) 1.018f else 1f,
        label = "botNameTitleScale"
    )

    BasePage(
        colors = colors,
        progress = .15f
    ) {

        ThemeAndSkip(
            colors = colors,
            darkMode = darkMode,
            onTheme = onToggleTheme,
            onSkip = onSkip
        )

        Spacer(
            Modifier.height(42.dp)
        )

        Text(
            text = "Vamos montar seu bot.",
            color = colors.text,
            fontSize = 31.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.scale(titleScale)
        )

        Spacer(
            Modifier.height(13.dp)
        )

        Text(
            text = "Primeiro escolha um nome. Depois a IA poderá usar essa identidade para personalizar seu projeto.",
            color = colors.secondary,
            fontSize = 16.sp,
            lineHeight = 23.sp
        )

        Spacer(
            Modifier.height(62.dp)
        )

        OutlinedTextField(
            value = value,

            onValueChange = { newValue ->

                onChange(
                    newValue
                        .replace("\n", "")
                        .replace("\r", "")
                        .take(32)
                )
            },

            singleLine = true,
            maxLines = 1,

            label = {
                Text("Nome do bot")
            },

            placeholder = {
                Text("Ex.: Arlecchino")
            },

            leadingIcon = {

                Text(
                    text = "✦",
                    color = if (focused)
                        colors.primarySoft
                    else
                        colors.secondary,
                    fontSize = 20.sp
                )
            },

            supportingText = {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    Text(
                        text = "${value.length}/32",
                        color = colors.muted,
                        fontSize = 11.sp
                    )
                }
            },

            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),

            keyboardActions = KeyboardActions(
                onDone = {

                    if (value.trim().length >= 2) {
                        onNext()
                    }
                }
            ),

            colors = OutlinedTextFieldDefaults.colors(

                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,

                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,

                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.border,

                focusedLabelColor = colors.primarySoft,
                unfocusedLabelColor = colors.secondary,

                cursorColor = colors.primarySoft
            ),

            shape = RoundedCornerShape(20.dp),

            modifier = Modifier
                .fillMaxWidth()
                .scale(inputScale)
                .onFocusChanged {
                    focused = it.isFocused
                }
        )

        if (value.isNotBlank()) {

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                text = "✨ ${value.trim()} já está ganhando uma identidade.",
                color = colors.primarySoft,
                fontSize = 13.sp
            )
        }

        Spacer(
            Modifier.weight(1f)
        )

        PrimaryButton(
            colors = colors,
            title = "Continuar",
            enabled = value.trim().length >= 2,
            onClick = onNext
        )

        Spacer(
            Modifier.height(22.dp)
        )
    }
}



/* ============================================================
   OWNER
   ============================================================ */

@Composable
private fun OwnerScreen(
    colors: SocietyColors,
    value: String,
    onChange: (String) -> Unit,
    onNext: () -> Unit
) {

    BasePage(
        colors,
        .30f
    ) {

        Spacer(
            Modifier.height(60.dp)
        )

        Text(
            "Qual é o número do dono?",
            color = colors.text,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            "Use código do país + DDD + número.",
            color = colors.secondary
        )

        Spacer(
            Modifier.height(60.dp)
        )

        OutlinedTextField(
            value = value,

            onValueChange = {
                onChange(
                    it.filter { c ->
                        c.isDigit()
                    }
                )
            },

            singleLine = true,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Phone,
                    imeAction =
                        ImeAction.Done
                ),

            label = {
                Text("Número do dono")
            },

            placeholder = {
                Text("5592999999999")
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.weight(1f)
        )

        PrimaryButton(
            colors,
            "Continuar",
            value.length >= 10,
            onNext
        )

        Spacer(
            Modifier.height(22.dp)
        )
    }
}

/* ============================================================
   LIBRARY
   ============================================================ */

@Composable
private fun LibraryScreen(
    colors: SocietyColors,
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {

    BasePage(
        colors,
        .48f
    ) {

        Spacer(
            Modifier.height(44.dp)
        )

        Text(
            "Escolha o motor do seu bot",
            color = colors.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            "Qual implementação do Baileys você prefere?",
            color = colors.secondary,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(30.dp)
        )

        ChoiceCard(
            colors,
            "@systemzero/baileys  ⭐",
            "Nossa recomendação para experiências mais interativas.",
            selected == "systemzero"
        ) {
            onSelect("systemzero")
        }

        Spacer(
            Modifier.height(12.dp)
        )

        ChoiceCard(
            colors,
            "@itsliaaa/baileys",
            "Muito conhecida e querida por criadores de bots.",
            selected == "itsliaaa"
        ) {
            onSelect("itsliaaa")
        }

        Spacer(
            Modifier.height(12.dp)
        )

        ChoiceCard(
            colors,
            "WhiskeySockets/Baileys",
            "A implementação tradicional do ecossistema.",
            selected == "whiskeysockets"
        ) {
            onSelect("whiskeysockets")
        }

        Spacer(
            Modifier.weight(1f)
        )

        PrimaryButton(
            colors,
            "Continuar",
            selected.isNotEmpty(),
            onNext
        )

        Spacer(
            Modifier.height(22.dp)
        )
    }
}

/* ============================================================
   FEATURES
   ============================================================ */

@Composable
private fun FeaturesScreen(
    colors: SocietyColors,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
    onNext: () -> Unit
) {

    val options =
        listOf(
            "IA integrada",
            "Botões",
            "Carrosséis",
            "Menus interativos",
            "Administração de grupos",
            "Anti-link / moderação",
            "Downloads",
            "Stickers",
            "Jogos",
            "Sistema de níveis",
            "Comandos do dono"
        )

    BasePage(
        colors,
        .66f
    ) {

        Spacer(
            Modifier.height(34.dp)
        )

        Text(
            "Quais recursos você quer?",
            color = colors.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            "Escolha quantos quiser.",
            color = colors.secondary
        )

        Spacer(
            Modifier.height(22.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {

            options.forEach {

                val active =
                    selected.contains(it)

                ChoiceCard(
                    colors,
                    it,
                    selected = active
                ) {
                    onChange(
                        if (active)
                            selected - it
                        else
                            selected + it
                    )
                }

                Spacer(
                    Modifier.height(10.dp)
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        PrimaryButton(
            colors,
            "Continuar",
            selected.isNotEmpty(),
            onNext
        )

        Spacer(
            Modifier.height(18.dp)
        )
    }
}

/* ============================================================
   STYLE
   ============================================================ */

@Composable
private fun StyleScreen(
    colors: SocietyColors,
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {

    BasePage(
        colors,
        .82f
    ) {

        Spacer(
            Modifier.height(45.dp)
        )

        Text(
            "Qual será a personalidade dele?",
            color = colors.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(30.dp)
        )

        listOf(
            "Profissional" to
                "Objetivo, organizado e elegante.",

            "Divertido" to
                "Descontraído, espontâneo e expressivo.",

            "Equilibrado" to
                "Adapta o comportamento ao contexto.",

            "Criado pela IA" to
                "A IA define uma personalidade baseada nas suas escolhas."
        ).forEach { item ->

            ChoiceCard(
                colors,
                item.first,
                item.second,
                selected == item.first
            ) {
                onSelect(
                    item.first
                )
            }

            Spacer(
                Modifier.height(12.dp)
            )
        }

        Spacer(
            Modifier.weight(1f)
        )

        PrimaryButton(
            colors,
            "Prosseguir",
            selected.isNotEmpty(),
            onNext
        )

        Spacer(
            Modifier.height(22.dp)
        )
    }
}

/* ============================================================
   EULA BONITO
   ============================================================ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EulaScreen(
    colors: SocietyColors,
    darkMode: Boolean,
    languageCode: String,
    onLanguageChange: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onContinue: () -> Unit
) {

    val context = LocalContext.current

    val lang =
        languageByCode(languageCode)

    var languageOpen by remember {
        mutableStateOf(false)
    }

    BasePage(colors) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp),

            horizontalArrangement =
                Arrangement.End
        ) {

            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(26.dp),

                modifier = Modifier.clickable {
                    onToggleTheme()
                }
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 13.dp,
                        vertical = 9.dp
                    ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = if (darkMode)
                            "🌙  ✦"
                        else
                            "☀️  ☁️",

                        fontSize = 15.sp
                    )

                    Spacer(
                        Modifier.width(7.dp)
                    )

                    Text(
                        text = if (darkMode)
                            "Escuro"
                        else
                            "Claro",

                        color = colors.primarySoft,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(
            Modifier.height(1.dp)
        )

        Image(
            painter =
                painterResource(
                    R.drawable.splash
                ),

            contentDescription = null,

            contentScale =
                ContentScale.Fit,

            modifier = Modifier
                .size(165.dp)
                .align(
                    Alignment.CenterHorizontally
                )
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            text = lang.welcome,

            color = colors.text,

            fontSize = 27.sp,

            lineHeight = 30.sp,

            fontWeight =
                FontWeight.Bold,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(7.dp)
        )

        Text(
            text = lang.subtitle,

            color = colors.secondary,

            fontSize = 14.sp,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(15.dp)
        )

        Row(
            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = lang.privacy,

                color =
                    colors.primarySoft,

                fontSize =
                    12.5.sp,

                fontWeight =
                    FontWeight.SemiBold,

                modifier =
                    Modifier.clickable {

                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    PRIVACY_URL
                                )
                            )
                        )
                    }
            )

            Text(
                text = "   •   ",
                color = colors.muted,
                fontSize = 11.sp
            )

            Text(
                text = lang.terms,

                color =
                    colors.primarySoft,

                fontSize =
                    12.5.sp,

                fontWeight =
                    FontWeight.SemiBold,

                modifier =
                    Modifier.clickable {

                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    TERMS_URL
                                )
                            )
                        )
                    }
            )
        }

        Spacer(
            Modifier.height(16.dp)
        )

        Surface(
            color =
                colors.surface,

            shape =
                RoundedCornerShape(24.dp),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = colors.border,
                        shape =
                            RoundedCornerShape(
                                24.dp
                            )
                    )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 19.dp,
                        vertical = 17.dp
                    )
            ) {

                Text(
                    text =
                        lang.beforeContinue,

                    color =
                        colors.text,

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    text =
                        lang.legalText,

                    color =
                        colors.secondary,

                    fontSize =
                        12.5.sp,

                    lineHeight =
                        18.5.sp
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                Text(
                    text =
                        "Fsociety Devs",

                    color =
                        colors.primarySoft,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        13.sp
                )
            }
        }

        Spacer(
            Modifier.weight(1f)
        )

        Surface(
            color =
                colors.surfaceAlt,

            shape =
                RoundedCornerShape(
                    26.dp
                ),

            modifier =
                Modifier
                    .align(
                        Alignment.CenterHorizontally
                    )
                    .clickable {

                        languageOpen =
                            true
                    }
        ) {

            Row(
                modifier =
                    Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 11.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        lang.flag,

                    fontSize =
                        19.sp
                )

                Spacer(
                    Modifier.width(9.dp)
                )

                Text(
                    text =
                        lang.label,

                    color =
                        colors.primarySoft,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        14.sp
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    text = "⌄",

                    color =
                        colors.primarySoft
                )
            }
        }

        Spacer(
            Modifier.height(16.dp)
        )

        PrimaryButton(
            colors = colors,
            title = lang.continueText,
            onClick = onContinue
        )

        Spacer(
            Modifier.height(10.dp)
        )

        Text(
            text =
                lang.afterText,

            color =
                colors.secondary,

            fontSize =
                11.5.sp,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(11.dp)
        )

        Text(
            text =
                "F A S O C I E T Y   D E V S",

            color =
                colors.muted,

            fontSize =
                9.sp,

            letterSpacing =
                1.2.sp,

            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        )

        Spacer(
            Modifier.height(12.dp)
        )
    }


    /*
     * ========================================================
     * NOVO SELETOR DE IDIOMA
     * ========================================================
     *
     * Em vez daquele DropdownMenu gigante,
     * usamos um painel inferior.
     */

    if (languageOpen) {

        ModalBottomSheet(
            onDismissRequest = {
                languageOpen = false
            },

            containerColor =
                colors.surface,

            contentColor =
                colors.text,

            dragHandle = {

                Box(
                    modifier =
                        Modifier
                            .padding(
                                top = 10.dp,
                                bottom = 4.dp
                            )
                            .width(42.dp)
                            .height(4.dp)
                            .background(
                                colors.border,
                                RoundedCornerShape(
                                    50
                                )
                            )
                )
            }
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = 20.dp
                        )
            ) {

                Text(
                    text =
                        "Idioma do aplicativo",

                    color =
                        colors.text,

                    fontSize =
                        22.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    text =
                        "Escolha o idioma que deseja usar.",

                    color =
                        colors.secondary,

                    fontSize =
                        13.sp
                )

                Spacer(
                    Modifier.height(16.dp)
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                max = 500.dp
                            )
                            .verticalScroll(
                                rememberScrollState()
                            )
                ) {

                    APP_LANGUAGES
                        .forEach { item ->

                            val selected =
                                item.code ==
                                languageCode

                            Surface(
                                color =
                                    if (selected)
                                        colors.surfaceAlt
                                    else
                                        Color.Transparent,

                                shape =
                                    RoundedCornerShape(
                                        17.dp
                                    ),

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {

                                            onLanguageChange(
                                                item.code
                                            )

                                            languageOpen =
                                                false
                                        }
                            ) {

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 15.dp,
                                                vertical = 13.dp
                                            ),

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(
                                        text =
                                            item.flag,

                                        fontSize =
                                            22.sp
                                    )

                                    Spacer(
                                        Modifier.width(
                                            13.dp
                                        )
                                    )

                                    Text(
                                        text =
                                            item.label,

                                        color =
                                            if (selected)
                                                colors.primarySoft
                                            else
                                                colors.text,

                                        fontSize =
                                            15.sp,

                                        fontWeight =
                                            if (selected)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Medium,

                                        modifier =
                                            Modifier.weight(
                                                1f
                                            )
                                    )

                                    if (selected) {

                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(
                                                        25.dp
                                                    )
                                                    .background(
                                                        colors.primary,
                                                        CircleShape
                                                    ),

                                            contentAlignment =
                                                Alignment.Center
                                        ) {

                                            Text(
                                                text =
                                                    "✓",

                                                color =
                                                    Color.White,

                                                fontSize =
                                                    14.sp,

                                                fontWeight =
                                                    FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(
                                Modifier.height(
                                    4.dp
                                )
                            )
                        }
                }

                Spacer(
                    Modifier.height(15.dp)
                )
            }
        }
    }
}




private fun registrationPasswordValid(
    password: String
): Boolean {

    return (
        password.length in 6..64 &&
        (
            password.any { it.isUpperCase() } ||
            password.any { it.isDigit() }
        )
    )
}


private fun generateStrongPassword(
    length: Int = 20
): String {

    val random = SecureRandom()

    val upper =
        "ABCDEFGHJKLMNPQRSTUVWXYZ"

    val lower =
        "abcdefghijkmnopqrstuvwxyz"

    val numbers =
        "23456789"

    val symbols =
        "!@#$%&*+-_=?."

    val all =
        upper + lower + numbers + symbols

    val size =
        length.coerceIn(12, 64)

    val result =
        mutableListOf(
            upper[random.nextInt(upper.length)],
            lower[random.nextInt(lower.length)],
            numbers[random.nextInt(numbers.length)],
            symbols[random.nextInt(symbols.length)]
        )

    while (result.size < size) {
        result +=
            all[
                random.nextInt(all.length)
            ]
    }

    for (i in result.lastIndex downTo 1) {
        val j =
            random.nextInt(i + 1)

        val tmp =
            result[i]

        result[i] =
            result[j]

        result[j] =
            tmp
    }

    return result.joinToString("")
}


@Composable
private fun PasswordRequirement(
    checked: Boolean,
    text: String,
    colors: SocietyColors
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            if (checked) "●" else "○",
            color =
                if (checked)
                    colors.primarySoft
                else
                    colors.secondary,
            fontSize = 18.sp
        )

        Spacer(
            Modifier.width(7.dp)
        )

        Text(
            text,
            color =
                if (checked)
                    colors.text
                else
                    colors.secondary,
            fontSize = 13.sp
        )
    }
}


@Composable
private fun PasswordSecurityPanel(
    password: String,
    colors: SocietyColors
) {

    val lengthOk =
        password.length >= 8

    val upper =
        password.any {
            it.isUpperCase()
        }

    val lower =
        password.any {
            it.isLowerCase()
        }

    val number =
        password.any {
            it.isDigit()
        }

    val symbol =
        password.any {
            !it.isLetterOrDigit() &&
            !it.isWhitespace()
        }

    val noSpaces =
        password.isNotEmpty() &&
        password.none {
            it.isWhitespace()
        }

    val checks =
        listOf(
            lengthOk,
            upper,
            lower,
            number,
            symbol,
            noSpaces
        )

    val strength =
        if (password.isEmpty()) {
            0f
        } else {
            checks.count { it } /
                checks.size.toFloat()
        }

    val animated by
        animateFloatAsState(
            targetValue =
                strength,
            label =
                "password_strength"
        )

    val label =
        when {
            password.isEmpty() ->
                "Digite uma senha"

            strength < 0.34f ->
                "Senha fraca"

            strength < 0.67f ->
                "Senha razoável"

            strength < 1f ->
                "Senha forte"

            else ->
                "Senha muito forte"
        }

    Column(
        Modifier.fillMaxWidth()
    ) {

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                "Segurança da senha",
                color =
                    colors.secondary,
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                label,
                color =
                    if (strength >= 0.67f)
                        colors.primarySoft
                    else
                        colors.secondary,
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            Modifier.height(9.dp)
        )

        LinearProgressIndicator(
            progress = {
                animated
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(7.dp),
            color =
                colors.primary,
            trackColor =
                colors.border
        )

        Spacer(
            Modifier.height(14.dp)
        )

        Row(
            Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                PasswordRequirement(
                    lengthOk,
                    "8 caracteres",
                    colors
                )

                PasswordRequirement(
                    lower,
                    "Minúscula",
                    colors
                )

                PasswordRequirement(
                    symbol,
                    "Símbolo",
                    colors
                )
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                PasswordRequirement(
                    upper,
                    "Maiúscula",
                    colors
                )

                PasswordRequirement(
                    number,
                    "Número",
                    colors
                )

                PasswordRequirement(
                    noSpaces,
                    "Sem espaços",
                    colors
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            if (
                registrationPasswordValid(
                    password
                )
            )
                "✓ Requisito mínimo atendido"
            else
                "Obrigatório: 6–64 caracteres e uma maiúscula OU um número.",

            color =
                if (
                    registrationPasswordValid(
                        password
                    )
                )
                    colors.primarySoft
                else
                    colors.secondary,

            fontSize =
                12.sp
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colors: SocietyColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    password: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null,
    keyboardType: KeyboardType =
        KeyboardType.Text,
    imeAction: ImeAction =
        ImeAction.Next,
    maxLength: Int? = null,
    isError: Boolean = false,
    onFocusChangedCallback: (Boolean) -> Unit = {}
) {

    var focused by remember {
        mutableStateOf(false)
    }

    val bringIntoViewRequester =
        remember {
            BringIntoViewRequester()
        }

    val focusScope =
        rememberCoroutineScope()

    val pulse =
        remember {
            Animatable(0f)
        }

    LaunchedEffect(
        value
    ) {

        if (
            focused &&
            value.isNotEmpty()
        ) {

            pulse.snapTo(
                1f
            )

            pulse.animateTo(
                targetValue =
                    0f,
                animationSpec =
                    tween(
                        durationMillis =
                            420
                    )
            )
        }
    }


    val borderColor by
        animateColorAsState(
            targetValue =
                when {

                    isError ->
                        MaterialTheme
                            .colorScheme
                            .error

                    focused ->
                        colors.primarySoft

                    value.isNotEmpty() ->
                        colors.primary

                    else ->
                        colors.border
                },

            animationSpec =
                tween(
                    220
                ),

            label =
                "auth_border"
        )


    val backgroundColor by
        animateColorAsState(
            targetValue =
                if (focused) {

                    colors.surfaceAlt.copy(
                        alpha =
                            0.62f +
                            (
                                pulse.value *
                                0.16f
                            )
                    )

                } else {

                    colors.surface.copy(
                        alpha =
                            0.93f
                    )
                },

            animationSpec =
                tween(
                    180
                ),

            label =
                "auth_background"
        )


    Surface(
        color =
            backgroundColor,

        shape =
            RoundedCornerShape(
                19.dp
            ),

        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width =
                        if (
                            focused ||
                            value.isNotEmpty()
                        )
                            1.6.dp
                        else
                            1.dp,

                    color =
                        borderColor.copy(
                            alpha =
                                if (
                                    focused
                                )
                                    0.95f
                                else
                                    0.72f
                        ),

                    shape =
                        RoundedCornerShape(
                            19.dp
                        )
                )
    ) {

        OutlinedTextField(
            value =
                value,

            onValueChange = {

                val accepted =
                    maxLength
                        ?.let { max ->
                            it.take(max)
                        }
                        ?: it

                onValueChange(
                    accepted
                )
            },

            enabled =
                enabled,

            singleLine =
                true,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        keyboardType,

                    imeAction =
                        imeAction
                ),

            visualTransformation =
                if (
                    password &&
                    !passwordVisible
                ) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },

            trailingIcon =
                if (
                    password &&
                    onPasswordVisibilityChange !=
                    null
                ) {

                    {

                        IconButton(
                            onClick =
                                onPasswordVisibilityChange
                        ) {

                            Icon(
                                imageVector =
                                    if (
                                        passwordVisible
                                    )
                                        Icons.Filled.VisibilityOff
                                    else
                                        Icons.Filled.Visibility,

                                contentDescription =
                                    if (
                                        passwordVisible
                                    )
                                        "Ocultar senha"
                                    else
                                        "Mostrar senha",

                                tint =
                                    if (focused)
                                        colors.primarySoft
                                    else
                                        colors.secondary
                            )
                        }
                    }

                } else {
                    null
                },

            label = {

                Text(
                    label,

                    color =
                        if (focused)
                            colors.primarySoft
                        else
                            colors.secondary
                )
            },

            colors =
                OutlinedTextFieldDefaults
                    .colors(

                        focusedBorderColor =
                            Color.Transparent,

                        unfocusedBorderColor =
                            Color.Transparent,

                        disabledBorderColor =
                            Color.Transparent,

                        errorBorderColor =
                            Color.Transparent,

                        focusedContainerColor =
                            Color.Transparent,

                        unfocusedContainerColor =
                            Color.Transparent,

                        errorContainerColor =
                            Color.Transparent
                    ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(
                        bringIntoViewRequester
                    )
                    .onFocusChanged {

                        focused =
                            it.isFocused

                        onFocusChangedCallback(
                            it.isFocused
                        )

                        if (
                            it.isFocused
                        ) {

                            focusScope.launch {

                                /*
                                 * Espera o teclado terminar
                                 * de abrir antes de rolar.
                                 */
                                delay(
                                    280
                                )

                                try {

                                    bringIntoViewRequester
                                        .bringIntoView()

                                } catch (
                                    _: Exception
                                ) {}
                            }
                        }
                    }
                    .padding(
                        horizontal =
                            3.dp,

                        vertical =
                            2.dp
                    )
        )
    }
}


/* ============================================================
   LOGIN
   ============================================================ */

@Composable
private fun AuthScreen(
    colors: SocietyColors,
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    onForgot: () -> Unit,
    onSuccess: () -> Unit
) {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val prefs =
        remember(context) {
            context.getSharedPreferences(
                PREFS,
                android.content.Context.MODE_PRIVATE
            )
        }


    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var confirmVisible by remember {
        mutableStateOf(false)
    }

    var nameFocused by remember {
        mutableStateOf(false)
    }

    var emailFocused by remember {
        mutableStateOf(false)
    }

    var passwordFocused by remember {
        mutableStateOf(false)
    }

    var confirmFocused by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var showResetSuggestion by remember {
        mutableStateOf(false)
    }


    val showPasswordTools =
        mode ==
            AuthMode.REGISTER &&
        !nameFocused &&
        !emailFocused &&
        (
            passwordFocused ||
            confirmFocused
        )


    BasePage(
        colors = colors,
        keyboardAware = true
    ) {

        Spacer(
            Modifier.height(
                if (
                    mode ==
                    AuthMode.LOGIN
                )
                    74.dp
                else
                    30.dp
            )
        )


        Image(
            painter =
                painterResource(
                    R.drawable.splash
                ),

            contentDescription =
                null,

            modifier =
                Modifier
                    .size(
                        if (
                            mode ==
                            AuthMode.LOGIN
                        )
                            96.dp
                        else
                            82.dp
                    )
                    .align(
                        Alignment.CenterHorizontally
                    )
        )


        Spacer(
            Modifier.height(
                21.dp
            )
        )


        Text(
            if (
                mode ==
                AuthMode.LOGIN
            )
                "Bem-vindo de volta"
            else
                "Crie sua conta",

            color =
                colors.text,

            fontSize =
                if (
                    mode ==
                    AuthMode.LOGIN
                )
                    31.sp
                else
                    29.sp,

            fontWeight =
                FontWeight.Bold,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.fillMaxWidth()
        )


        Spacer(
            Modifier.height(
                8.dp
            )
        )


        Text(
            if (
                mode ==
                AuthMode.LOGIN
            )
                "Entre para continuar seu projeto."
            else
                "Sua conta mantém seus bots e configurações sincronizados.",

            color =
                colors.secondary,

            textAlign =
                TextAlign.Center,

            lineHeight =
                21.sp,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            18.dp
                    )
        )


        Spacer(
            Modifier.height(
                if (
                    mode ==
                    AuthMode.LOGIN
                )
                    29.dp
                else
                    23.dp
            )
        )


        Surface(
            color =
                colors.surface,

            shape =
                RoundedCornerShape(
                    30.dp
                ),

            tonalElevation =
                1.dp,

            modifier =
                Modifier
                    .fillMaxWidth(
                        0.94f
                    )
                    .align(
                        Alignment.CenterHorizontally
                    )
        ) {

            Column(
                modifier =
                    Modifier
                        .padding(
                            horizontal =
                                21.dp,

                            vertical =
                                23.dp
                        )
            ) {


                if (
                    mode ==
                    AuthMode.REGISTER
                ) {

                    AnimatedAuthField(
                        value =
                            name,

                        onValueChange = {
                            name = it
                            errorMessage =
                                null
                        },

                        label =
                            "Nome",

                        colors =
                            colors,

                        enabled =
                            !loading,

                        onFocusChangedCallback = {

                            nameFocused =
                                it

                            if (it) {

                                emailFocused =
                                    false

                                passwordFocused =
                                    false

                                confirmFocused =
                                    false
                            }
                        }
                    )


                    Spacer(
                        Modifier.height(
                            14.dp
                        )
                    )
                }


                AnimatedAuthField(
                    value =
                        email,

                    onValueChange = {
                        email = it

                        errorMessage =
                            null

                        showResetSuggestion =
                            false
                    },

                    label =
                        "E-mail",

                    colors =
                        colors,

                    enabled =
                        !loading,

                    keyboardType =
                        KeyboardType.Email,

                    imeAction =
                        ImeAction.Next,

                    onFocusChangedCallback = {

                        emailFocused =
                            it

                        if (it) {

                            nameFocused =
                                false

                            passwordFocused =
                                false

                            confirmFocused =
                                false
                        }
                    }
                )


                Spacer(
                    Modifier.height(
                        14.dp
                    )
                )


                AnimatedAuthField(
                    value =
                        password,

                    onValueChange = {

                        password =
                            if (
                                mode ==
                                AuthMode.REGISTER
                            )
                                it.take(64)
                            else
                                it

                        errorMessage =
                            null

                        showResetSuggestion =
                            false
                    },

                    label =
                        "Senha",

                    colors =
                        colors,

                    enabled =
                        !loading,

                    password =
                        true,

                    passwordVisible =
                        passwordVisible,

                    onPasswordVisibilityChange = {
                        passwordVisible =
                            !passwordVisible
                    },

                    keyboardType =
                        KeyboardType.Password,

                    imeAction =
                        if (
                            mode ==
                            AuthMode.REGISTER
                        )
                            ImeAction.Next
                        else
                            ImeAction.Done,

                    maxLength =
                        if (
                            mode ==
                            AuthMode.REGISTER
                        )
                            64
                        else
                            null,

                    onFocusChangedCallback = {

                        passwordFocused =
                            it

                        if (it) {

                            nameFocused =
                                false

                            emailFocused =
                                false
                        }
                    }
                )


                if (
                    mode ==
                    AuthMode.LOGIN
                ) {

                    Spacer(
                        Modifier.height(
                            13.dp
                        )
                    )


                    Text(
                        "Esqueci minha senha",

                        color =
                            colors.primarySoft,

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        modifier =
                            Modifier
                                .align(
                                    Alignment.End
                                )
                                .padding(
                                    end =
                                        4.dp
                                )
                                .clickable(
                                    enabled =
                                        !loading
                                ) {
                                    onForgot()
                                }
                    )
                }


                AnimatedVisibility(
                    visible =
                        showPasswordTools,

                    enter =
                        fadeIn(
                            tween(220)
                        ) +
                        expandVertically(
                            tween(280)
                        ),

                    exit =
                        fadeOut(
                            tween(150)
                        ) +
                        shrinkVertically(
                            tween(220)
                        )
                ) {

                    Column {

                        Spacer(
                            Modifier.height(
                                12.dp
                            )
                        )


                        AnimatedAuthField(
                            value =
                                confirmPassword,

                            onValueChange = {
                                confirmPassword =
                                    it.take(
                                        64
                                    )

                                errorMessage =
                                    null
                            },

                            label =
                                "Confirmar senha",

                            colors =
                                colors,

                            enabled =
                                !loading,

                            password =
                                true,

                            passwordVisible =
                                confirmVisible,

                            onPasswordVisibilityChange = {
                                confirmVisible =
                                    !confirmVisible
                            },

                            keyboardType =
                                KeyboardType.Password,

                            imeAction =
                                ImeAction.Done,

                            maxLength =
                                64,

                            isError =
                                confirmPassword
                                    .isNotEmpty() &&
                                confirmPassword !=
                                    password,

                            onFocusChangedCallback = {

                                confirmFocused =
                                    it

                                if (it) {

                                    nameFocused =
                                        false

                                    emailFocused =
                                        false
                                }
                            }
                        )


                        if (
                            confirmPassword
                                .isNotEmpty()
                        ) {

                            Spacer(
                                Modifier.height(
                                    7.dp
                                )
                            )


                            Text(
                                if (
                                    confirmPassword ==
                                    password
                                )
                                    "✓ As senhas coincidem"
                                else
                                    "As senhas não coincidem",

                                color =
                                    if (
                                        confirmPassword ==
                                        password
                                    )
                                        colors.primarySoft
                                    else
                                        MaterialTheme
                                            .colorScheme
                                            .error,

                                fontSize =
                                    12.sp,

                                modifier =
                                    Modifier.padding(
                                        start =
                                            5.dp
                                    )
                            )
                        }


                        Spacer(
                            Modifier.height(
                                12.dp
                            )
                        )


                        Text(
                            "${password.length}/64",

                            color =
                                colors.muted,

                            fontSize =
                                11.sp,

                            modifier =
                                Modifier.align(
                                    Alignment.End
                                )
                        )


                        Spacer(
                            Modifier.height(
                                13.dp
                            )
                        )


                        PasswordSecurityPanel(
                            password =
                                password,

                            colors =
                                colors
                        )


                        Spacer(
                            Modifier.height(
                                17.dp
                            )
                        )


                        Surface(
                            color =
                                colors.surfaceAlt,

                            shape =
                                RoundedCornerShape(
                                    18.dp
                                ),

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        colors.primary.copy(
                                            alpha =
                                                0.45f
                                        ),
                                        RoundedCornerShape(
                                            18.dp
                                        )
                                    )
                                    .clickable(
                                        enabled =
                                            !loading
                                    ) {

                                        val generated =
                                            generateStrongPassword(
                                                20
                                            )

                                        password =
                                            generated

                                        confirmPassword =
                                            generated

                                        passwordVisible =
                                            true

                                        confirmVisible =
                                            true

                                        errorMessage =
                                            null
                                    }
                        ) {

                            Row(
                                modifier =
                                    Modifier.padding(
                                        horizontal =
                                            16.dp,

                                        vertical =
                                            15.dp
                                    ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    "⚡",
                                    fontSize =
                                        22.sp
                                )

                                Spacer(
                                    Modifier.width(
                                        10.dp
                                    )
                                )

                                Column {

                                    Text(
                                        "Gerar senha forte",

                                        color =
                                            colors.primarySoft,

                                        fontWeight =
                                            FontWeight.Bold,

                                        fontSize =
                                            15.sp
                                    )

                                    Spacer(
                                        Modifier.height(
                                            2.dp
                                        )
                                    )

                                    Text(
                                        "Senha segura de 20 caracteres",

                                        color =
                                            colors.secondary,

                                        fontSize =
                                            11.sp
                                    )
                                }
                            }
                        }
                    }
                }


                errorMessage
                    ?.let {

                        Spacer(
                            Modifier.height(
                                15.dp
                            )
                        )


                        Text(
                            it,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,

                            fontSize =
                                13.sp,

                            lineHeight =
                                18.sp
                        )
                    }


                if (
                    mode ==
                    AuthMode.LOGIN &&
                    showResetSuggestion
                ) {

                    Spacer(
                        Modifier.height(
                            11.dp
                        )
                    )


                    Row(
                        modifier =
                            Modifier
                                .align(
                                    Alignment.CenterHorizontally
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            "Esqueceu sua senha? ",

                            color =
                                colors.secondary,

                            fontSize =
                                13.sp
                        )


                        Text(
                            "Redefina-a aqui",

                            color =
                                colors.primarySoft,

                            fontSize =
                                13.sp,

                            fontWeight =
                                FontWeight.Bold,

                            modifier =
                                Modifier.clickable {
                                    onForgot()
                                }
                        )
                    }
                }


                Spacer(
                    Modifier.height(
                        23.dp
                    )
                )


                val registerValid =
                    name.trim()
                        .length >=
                        2 &&
                    email.isNotBlank() &&
                    registrationPasswordValid(
                        password
                    ) &&
                    password ==
                        confirmPassword


                val loginValid =
                    email.isNotBlank() &&
                    password.isNotEmpty()


                PrimaryButton(
                    colors,

                    if (loading) {

                        if (
                            mode ==
                            AuthMode.LOGIN
                        )
                            "Entrando..."
                        else
                            "Criando conta..."

                    } else {

                        if (
                            mode ==
                            AuthMode.LOGIN
                        )
                            "Entrar"
                        else
                            "Criar conta"
                    },

                    (
                        if (
                            mode ==
                            AuthMode.LOGIN
                        )
                            loginValid
                        else
                            registerValid
                    ) &&
                    !loading
                ) {

                    loading =
                        true

                    errorMessage =
                        null

                    showResetSuggestion =
                        false


                    scope.launch {

                        try {

                            val result =
                                if (
                                    mode ==
                                    AuthMode.LOGIN
                                ) {

                                    SocietyApi.login(
                                        email =
                                            email,

                                        password =
                                            password
                                    )

                                } else {

                                    SocietyApi.register(
                                        name =
                                            name,

                                        email =
                                            email,

                                        password =
                                            password
                                    )
                                }


                            prefs.edit()
                                .putString(
                                    "auth_id_token",
                                    result.auth.idToken
                                )
                                .putString(
                                    "auth_refresh_token",
                                    result.auth.refreshToken
                                )
                                .putLong(
                                    "auth_expires_in",
                                    result.auth.expiresIn
                                )
                                .putLong(
                                    "auth_saved_at",
                                    System.currentTimeMillis()
                                )
                                .putString(
                                    "auth_uid",
                                    result.user.uid
                                )
                                .putString(
                                    "auth_email",
                                    result.user.email
                                )
                                .putString(
                                    "auth_display_name",
                                    result.user.displayName
                                )
                                .putBoolean(
                                    "auth_email_verified",
                                    result.user.emailVerified
                                )
                                .remove(
                                    "authenticated_demo"
                                )
                                .apply()


                            Toast.makeText(
                                context,
                                result.message,
                                Toast.LENGTH_SHORT
                            ).show()


                            onSuccess()

                        } catch (
                            error:
                                SocietyApiException
                        ) {

                            errorMessage =
                                error.message
                                    ?: "Não foi possível autenticar."


                            if (
                                mode ==
                                AuthMode.LOGIN &&
                                (
                                    error.resetSuggested ||
                                    error.code ==
                                        "INVALID_CREDENTIALS" ||
                                    error.statusCode ==
                                        401 ||
                                    error.statusCode ==
                                        429
                                )
                            ) {

                                showResetSuggestion =
                                    true
                            }

                        } catch (
                            error:
                                Exception
                        ) {

                            errorMessage =
                                "Não foi possível conectar ao servidor."

                        } finally {

                            loading =
                                false
                        }
                    }
                }
            }
        }


        Spacer(
            Modifier.height(
                21.dp
            )
        )


        Row(
            modifier =
                Modifier
                    .align(
                        Alignment.CenterHorizontally
                    )
                    .padding(
                        horizontal =
                            18.dp
                    )
        ) {

            Text(
                if (
                    mode ==
                    AuthMode.LOGIN
                )
                    "Ainda não tem uma conta? "
                else
                    "Já possui uma conta? ",

                color =
                    colors.secondary
            )


            Text(
                if (
                    mode ==
                    AuthMode.LOGIN
                )
                    "Criar conta"
                else
                    "Entrar",

                color =
                    colors.primarySoft,

                fontWeight =
                    FontWeight.Bold,

                modifier =
                    Modifier.clickable(
                        enabled =
                            !loading
                    ) {

                        password =
                            ""

                        confirmPassword =
                            ""

                        errorMessage =
                            null

                        passwordFocused =
                            false

                        confirmFocused =
                            false

                        showResetSuggestion =
                            false


                        onModeChange(
                            if (
                                mode ==
                                AuthMode.LOGIN
                            )
                                AuthMode.REGISTER
                            else
                                AuthMode.LOGIN
                        )
                    }
            )
        }


        Spacer(
            Modifier.height(
                28.dp
            )
        )
    }
}

/* ============================================================
   RECUPERAÇÃO
   ============================================================ */

@Composable
private fun ForgotPasswordScreen(
    colors: SocietyColors,
    onBack: () -> Unit
) {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var email by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var isError by remember {
        mutableStateOf(false)
    }

    BasePage(colors) {

        Spacer(
            Modifier.height(55.dp)
        )

        Text(
            "Recuperar acesso",
            color =
                colors.text,
            fontSize =
                30.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(9.dp)
        )

        Text(
            "Informe o e-mail da sua conta para receber as instruções de recuperação.",
            color =
                colors.secondary,
            lineHeight =
                22.sp
        )

        Spacer(
            Modifier.height(35.dp)
        )

        Surface(
            color =
                colors.surface,
            shape =
                RoundedCornerShape(
                    24.dp
                ),
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(20.dp)
            ) {

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        message = null
                    },
                    enabled =
                        !loading,
                    singleLine =
                        true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Email,
                            imeAction =
                                ImeAction.Done
                        ),
                    label = {
                        Text("E-mail")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                message
                    ?.let {
                        Spacer(
                            Modifier.height(
                                15.dp
                            )
                        )

                        Text(
                            it,
                            color =
                                if (isError)
                                    MaterialTheme
                                        .colorScheme
                                        .error
                                else
                                    colors.primarySoft,
                            fontSize =
                                13.sp,
                            lineHeight =
                                19.sp
                        )
                    }

                Spacer(
                    Modifier.height(22.dp)
                )

                PrimaryButton(
                    colors,
                    if (loading)
                        "Enviando..."
                    else
                        "Enviar recuperação",
                    email.isNotBlank() &&
                    !loading
                ) {
                    loading = true
                    message = null
                    isError = false

                    scope.launch {
                        try {
                            message =
                                SocietyApi
                                    .forgotPassword(
                                        email
                                    )

                            isError = false

                        } catch (
                            e: SocietyApiException
                        ) {
                            message =
                                e.message
                                    ?: "Não foi possível enviar."
                            isError = true

                        } catch (
                            e: Exception
                        ) {
                            message =
                                "Não foi possível conectar ao servidor."
                            isError = true

                        } finally {
                            loading = false
                        }
                    }
                }
            }
        }

        Spacer(
            Modifier.height(24.dp)
        )

        Text(
            "← Voltar para o login",
            color =
                colors.primarySoft,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier
                    .align(
                        Alignment.CenterHorizontally
                    )
                    .clickable(
                        enabled =
                            !loading
                    ) {
                        onBack()
                    }
        )
    }
}

/* ============================================================
   IA
   ============================================================ */

@Composable
private fun AiProfileScreen(
    colors: SocietyColors,
    draft: BotDraft,
    onGenerate: () -> Unit
) {

    BasePage(
        colors,
        1f
    ) {

        Spacer(
            Modifier.height(45.dp)
        )

        Text(
            "Agora a IA assume daqui ✨",
            color =
                colors.text,
            fontSize =
                29.sp,
            fontWeight =
                FontWeight.Bold,
            textAlign =
                TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(11.dp)
        )

        Text(
            "Vamos usar suas escolhas para criar a configuração inicial do bot.",
            color =
                colors.secondary,
            textAlign =
                TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(30.dp)
        )

        Surface(
            color =
                colors.surface,
            shape =
                RoundedCornerShape(24.dp),
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(22.dp)
            ) {

                ProfileLine(
                    colors,
                    "BOT",
                    draft.name.ifBlank {
                        "Personalização pulada"
                    }
                )

                ProfileLine(
                    colors,
                    "DONO",
                    draft.owner.ifBlank {
                        "Não definido"
                    }
                )

                ProfileLine(
                    colors,
                    "BIBLIOTECA",
                    when (
                        draft.library
                    ) {
                        "systemzero" ->
                            "@systemzero/baileys"

                        "itsliaaa" ->
                            "@itsliaaa/baileys"

                        "whiskeysockets" ->
                            "WhiskeySockets/Baileys"

                        else ->
                            "A IA escolherá"
                    }
                )

                ProfileLine(
                    colors,
                    "ESTILO",
                    draft.style.ifBlank {
                        "A IA escolherá"
                    }
                )
            }
        }

        Spacer(
            Modifier.weight(1f)
        )

        PrimaryButton(
            colors,
            "Criar perfil com IA",
            onClick =
                onGenerate
        )

        Spacer(
            Modifier.height(22.dp)
        )
    }
}

@Composable
private fun ProfileLine(
    colors: SocietyColors,
    title: String,
    value: String
) {

    Text(
        title,
        color =
            colors.primarySoft,
        fontSize =
            11.sp,
        fontWeight =
            FontWeight.Bold
    )

    Spacer(
        Modifier.height(4.dp)
    )

    Text(
        value,
        color =
            colors.text,
        fontWeight =
            FontWeight.SemiBold,
        fontSize =
            17.sp
    )

    Spacer(
        Modifier.height(18.dp)
    )
}

/* ============================================================
   HOME
   ============================================================ */

private enum class HomeSection {
    HOME,
    CREATE_BOT,
    BUILDER_CONSOLE,
    BOTS,
    MANAGE_BOT,
    COMMANDS,
    WHATSAPP,
    AI,
    SETTINGS,
    SECURITY,
    ABOUT
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    colors: SocietyColors,
    draft: BotDraft,
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {

    val context =
        LocalContext.current

    val prefs =
        remember(context) {
            context.getSharedPreferences(
                PREFS,
                android.content.Context.MODE_PRIVATE
            )
        }

    val scope =
        rememberCoroutineScope()

    var emailVerified by remember {
        mutableStateOf(
            prefs.getBoolean(
                "auth_email_verified",
                false
            )
        )
    }

    val drawerState =
        rememberDrawerState(
            initialValue =
                DrawerValue.Closed
        )

    var section by remember {
        mutableStateOf(
            HomeSection.HOME
        )
    }

    var showProfile by remember {
        mutableStateOf(false)
    }

    var showLogoutConfirm by remember {
        mutableStateOf(false)
    }

    var bots by remember {
        mutableStateOf(
            loadSocietyBots(
                prefs
            )
        )
    }

    var managedBotId by remember {
        mutableStateOf<String?>(
            null
        )
    }


    var cloudSyncDone by remember {
        mutableStateOf(false)
    }

    var cloudSyncLoading by remember {
        mutableStateOf(false)
    }


    /*
     * Token como chave do efeito:
     * - cold start: sincroniza
     * - login: sincroniza
     * - troca de sessão: sincroniza novamente
     */
    val cloudAuthIdToken =
        prefs.getString(
            "auth_id_token",
            null
        )


    LaunchedEffect(
        cloudAuthIdToken
    ) {

        val idToken =
            cloudAuthIdToken


        if (
            !idToken.isNullOrBlank()
        ) {

            cloudSyncLoading =
                true


            try {

                val validToken =
                    SocietySession
                        .validToken(
                            prefs
                        )

                try {

                    val currentUser =
                        SocietyApi.me(
                            validToken
                        )

                    emailVerified =
                        currentUser.emailVerified

                    prefs.edit()
                        .putBoolean(
                            "auth_email_verified",
                            currentUser.emailVerified
                        )
                        .apply()

                } catch (
                    _: Exception
                ) {
                    /*
                     * Bootstrap continua mesmo se a consulta
                     * específica do estado do e-mail falhar.
                     */
                }

                val remote =
                    SocietyAccountApi
                        .bootstrap(
                            validToken
                        )


                val restored =
                    remote.bots
                        .map { bot ->

                            SocietyBotRecord(
                                id =
                                    bot.id,

                                draft =
                                    BotDraft(
                                        name =
                                            bot.name,

                                        owner =
                                            bot.owner,

                                        library =
                                            bot.library,

                                        features =
                                            bot.features,

                                        style =
                                            bot.style
                                    ),

                                archived =
                                    bot.archived,

                                createdAt =
                                    bot.createdAt,

                                status =
                                    bot.status
                            )
                        }


                bots =
                    restored


                if (
                    remote.uid
                        .isNotBlank()
                ) {

                    prefs.edit()
                        .putString(
                            "auth_uid",
                            remote.uid
                        )
                        .apply()
                }


                saveSocietyBots(
                    prefs,
                    restored
                )


                val first =
                    restored
                        .firstOrNull {
                            !it.archived
                        }


                if (
                    remote.profile
                        .displayName
                        .isNotBlank()
                ) {


                    prefs.edit()
                        .putString(
                            "auth_display_name",
                            remote.profile.displayName
                        )
                        .apply()
                }


                if (
                    remote.profile
                        .avatarUrl
                        .isNotBlank()
                ) {

                    prefs.edit()
                        .putString(
                            "profile_photo_uri",
                            remote.profile.avatarUrl
                        )
                        .apply()
                }


                cloudSyncDone =
                    true

            } catch (
                error: Exception
            ) {

                /*
                 * Mantém o cache local, mas NÃO esconde
                 * mais falhas da sincronização.
                 */
                cloudSyncDone =
                    false


                android.util.Log.e(
                    "SocietyCloudSync",
                    "Falha ao restaurar bots da Cloud",
                    error
                )


                android.widget.Toast.makeText(
                    context,
                    "Falha ao sincronizar bots: " +
                        (
                            error.message
                                ?: error.javaClass.simpleName
                        ),
                    android.widget.Toast.LENGTH_LONG
                ).show()

            } finally {

                cloudSyncLoading =
                    false
            }
        }
    }


    LaunchedEffect(
        bots.map {
            it.id
        }
    ) {

        val uid =
            prefs.getString(
                "auth_uid",
                ""
            ).orEmpty()


        if (
            uid.isNotBlank()
        ) {

            while (
                true
            ) {

                val refreshed =
                    bots.map {
                        bot ->

                        try {

                            val status =
                                SocietyBotControlApi
                                    .status(
                                        uid,
                                        bot.id
                                    )


                            bot.copy(
                                status =
                                    status
                            )

                        } catch (
                            _: Exception
                        ) {

                            bot
                        }
                    }


                if (
                    refreshed !=
                    bots
                ) {

                    bots =
                        refreshed


                    saveSocietyBots(
                        prefs,
                        refreshed
                    )
                }


                kotlinx.coroutines.delay(
                    5000
                )
            }
        }
    }


    val activeBots =
        bots.filter {
            !it.archived
        }

    val primaryBot =
        activeBots
            .maxByOrNull {
                it.createdAt
            }

    var hasCreatedBot by remember {
        mutableStateOf(
            activeBots.isNotEmpty()
        )
    }

    var createdBot by remember {
        mutableStateOf(
            primaryBot?.draft
                ?: BotDraft()
        )
    }

    var pendingBot by remember {
        mutableStateOf<BotDraft?>(
            null
        )
    }

    var showExitConfirm by remember {
        mutableStateOf(false)
    }

    var showNameEditor by remember {
        mutableStateOf(false)
    }

    var profilePhotoUri by remember {
        mutableStateOf(
            prefs.getString(
                "profile_photo_uri",
                null
            )
        )
    }

    var accountName by remember {
        mutableStateOf(
            prefs.getString(
                "auth_display_name",
                null
            )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Usuário Society"
        )
    }


    val accountNameBase =
        accountName


    val accountEmail =
        prefs.getString(
            "auth_email",
            null
        )
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Conta conectada"


    val initials =
        accountNameBase
            .trim()
            .split(
                Regex("\\s+")
            )
            .filter {
                it.isNotBlank()
            }
            .take(2)
            .mapNotNull {
                it.firstOrNull()
            }
            .joinToString("")
            .uppercase()
            .ifBlank {
                "SB"
            }


    val photoLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (
                uri != null
            ) {

                try {

                    context
                        .contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                } catch (
                    _: Exception
                ) {}


                /*
                 * Mostra a foto imediatamente no aparelho.
                 */
                profilePhotoUri =
                    uri.toString()


                prefs.edit()
                    .putString(
                        "profile_photo_uri",
                        uri.toString()
                    )
                    .apply()


                /*
                 * Também envia a foto para a Cloud.
                 */
                scope.launch {

                    try {

                        val remote =
                            SocietyProfileApi
                                .updateProfile(
                                    context =
                                        context,

                                    prefs =
                                        prefs,

                                    displayName =
                                        accountName,

                                    avatarUri =
                                        uri
                                )


                        if (
                            remote.avatarUrl
                                .isNotBlank()
                        ) {

                            profilePhotoUri =
                                remote.avatarUrl


                            prefs.edit()
                                .putString(
                                    "profile_photo_uri",
                                    remote.avatarUrl
                                )
                                .apply()
                        }


                        if (
                            remote.displayName
                                .isNotBlank()
                        ) {

                            accountName =
                                remote.displayName


                            prefs.edit()
                                .putString(
                                    "auth_display_name",
                                    remote.displayName
                                )
                                .apply()
                        }

                    } catch (
                        error: Exception
                    ) {

                        Toast.makeText(
                            context,
                            "A foto foi alterada, mas não sincronizou com a Cloud: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }


    /*
     * Quando o bootstrap terminar, atualizamos
     * nome e foto a partir da Cloud.
     */
    LaunchedEffect(
        cloudSyncDone
    ) {

        if (
            cloudSyncDone
        ) {

            accountName =
                prefs.getString(
                    "auth_display_name",
                    accountName
                )
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: accountName


            profilePhotoUri =
                prefs.getString(
                    "profile_photo_uri",
                    profilePhotoUri
                )
                    ?: profilePhotoUri
        }
    }


    /*
     * Alteração de nome também passa a ficar
     * sincronizada com a conta Society.
     */
    LaunchedEffect(
        accountName,
        cloudSyncDone
    ) {

        if (
            cloudSyncDone &&
            accountName.isNotBlank() &&
            accountName !=
                "Usuário Society"
        ) {

            kotlinx.coroutines.delay(
                700
            )


            try {

                val remote =
                    SocietyProfileApi
                        .updateProfile(
                            context =
                                context,

                            prefs =
                                prefs,

                            displayName =
                                accountName,

                            avatarUri =
                                null
                        )


                if (
                    remote.displayName
                        .isNotBlank()
                ) {

                    prefs.edit()
                        .putString(
                            "auth_display_name",
                            remote.displayName
                        )
                        .apply()
                }


                if (
                    remote.avatarUrl
                        .isNotBlank()
                ) {

                    profilePhotoUri =
                        remote.avatarUrl


                    prefs.edit()
                        .putString(
                            "profile_photo_uri",
                            remote.avatarUrl
                        )
                        .apply()
                }

            } catch (
                _: Exception
            ) {
                /*
                 * O nome permanece local se a Cloud
                 * estiver temporariamente indisponível.
                 */
            }
        }
    }

    fun navigateTo(
        target: HomeSection
    ) {

        if (
            !emailVerified &&
            (
                target ==
                    HomeSection.CREATE_BOT ||
                target ==
                    HomeSection.BUILDER_CONSOLE
            )
        ) {

            Toast.makeText(
                context,
                "Confirme seu e-mail para criar bots.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        section =
            target

        scope.launch {
            drawerState.close()
        }
    }


    BackHandler(
        enabled = true
    ) {

        when {

            showProfile -> {
                showProfile =
                    false
            }

            drawerState.isOpen -> {

                scope.launch {
                    drawerState.close()
                }
            }

            showNameEditor -> {
                showNameEditor =
                    false
            }

            section ==
                HomeSection.BUILDER_CONSOLE -> {

                section =
                    HomeSection.CREATE_BOT
            }

            section ==
                HomeSection.CREATE_BOT -> {

                pendingBot =
                    null

                section =
                    HomeSection.BOTS
            }

            section !=
                HomeSection.HOME -> {

                section =
                    HomeSection.HOME
            }

            showExitConfirm -> {

                (
                    context as?
                    Activity
                )?.finish()
            }

            else -> {

                showExitConfirm =
                    true
            }
        }
    }


    ModalNavigationDrawer(
        drawerState =
            drawerState,

        gesturesEnabled =
            true,

        drawerContent = {

            ModalDrawerSheet(
                drawerContainerColor =
                    colors.surface,

                drawerContentColor =
                    colors.text,

                modifier =
                    Modifier.fillMaxWidth(
                        0.86f
                    )
            ) {

                Spacer(
                    Modifier.height(
                        15.dp
                    )
                )


                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    20.dp,

                                vertical =
                                    16.dp
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
                                18.dp
                            ),

                        modifier =
                            Modifier.size(
                                52.dp
                            )
                    ) {

                        Image(
                            painter =
                                painterResource(
                                    R.drawable.splash
                                ),

                            contentDescription =
                                null,

                            contentScale =
                                ContentScale.Fit,

                            modifier =
                                Modifier.padding(
                                    6.dp
                                )
                        )
                    }


                    Spacer(
                        Modifier.width(
                            14.dp
                        )
                    )


                    Column {

                        Text(
                            "Society Bots",

                            color =
                                colors.text,

                            fontSize =
                                21.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(
                            "Painel de controle",

                            color =
                                colors.secondary,

                            fontSize =
                                12.sp
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


                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )


                SocietyDrawerItem(
                    selected =
                        section ==
                        HomeSection.HOME,

                    icon =
                        "⌂",

                    title =
                        "Início",

                    colors =
                        colors
                ) {
                    navigateTo(
                        HomeSection.HOME
                    )
                }


                SocietyDrawerItem(
                    selected =
                        section ==
                        HomeSection.BOTS,

                    icon =
                        "🤖",

                    title =
                        "Meus bots",

                    colors =
                        colors
                ) {
                    navigateTo(
                        HomeSection.BOTS
                    )
                }


                SocietyDrawerItem(
                    selected =
                        section ==
                        HomeSection.COMMANDS,

                    icon =
                        "⚡",

                    title =
                        "Comandos",

                    colors =
                        colors
                ) {
                    navigateTo(
                        HomeSection.COMMANDS
                    )
                }


                SocietyDrawerItem(
                    selected =
                        section ==
                        HomeSection.WHATSAPP,

                    icon =
                        "◉",

                    title =
                        "WhatsApp",

                    colors =
                        colors
                ) {
                    navigateTo(
                        HomeSection.WHATSAPP
                    )
                }


                SocietyDrawerItem(
                    selected =
                        section ==
                        HomeSection.AI,

                    icon =
                        "✦",

                    title =
                        "Inteligência artificial",

                    colors =
                        colors
                ) {
                    navigateTo(
                        HomeSection.AI
                    )
                }


                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )


                Text(
                    "Configuração",

                    color =
                        colors.muted,

                    fontSize =
                        12.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    modifier =
                        Modifier.padding(
                            horizontal =
                                25.dp,

                            vertical =
                                8.dp
                        )
                )


                SocietyDrawerItem(
                    selected =
                        section ==
                        HomeSection.SETTINGS,

                    icon =
                        "⚙",

                    title =
                        "Configurações",

                    colors =
                        colors
                ) {
                    navigateTo(
                        HomeSection.SETTINGS
                    )
                }


                SocietyDrawerItem(
                    selected =
                        section ==
                        HomeSection.SECURITY,

                    icon =
                        "◆",

                    title =
                        "Conta e segurança",

                    colors =
                        colors
                ) {
                    navigateTo(
                        HomeSection.SECURITY
                    )
                }


                SocietyDrawerItem(
                    selected =
                        section ==
                        HomeSection.ABOUT,

                    icon =
                        "ⓘ",

                    title =
                        "Sobre o Society Bots",

                    colors =
                        colors
                ) {
                    navigateTo(
                        HomeSection.ABOUT
                    )
                }


                Spacer(
                    Modifier.weight(
                        1f
                    )
                )


                HorizontalDivider(
                    color =
                        colors.border.copy(
                            alpha =
                                0.5f
                        )
                )


                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                showProfile =
                                    true
                            }
                            .padding(
                                19.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    SocietyAvatar(
                        initials =
                            initials,

                        colors =
                            colors,

                        size =
                            45.dp,
                        photoUri =
                            profilePhotoUri
                    )


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
                            accountName,

                            color =
                                colors.text,

                            fontSize =
                                14.sp,

                            fontWeight =
                                FontWeight.Bold,

                            maxLines =
                                1
                        )


                        Text(
                            accountEmail,

                            color =
                                colors.secondary,

                            fontSize =
                                11.sp,

                            maxLines =
                                1
                        )
                    }


                    Text(
                        "›",

                        color =
                            colors.secondary,

                        fontSize =
                            24.sp
                    )
                }
            }
        }
    ) {

        BasePage(
            colors = colors
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top =
                                16.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    color =
                        colors.surface,

                    shape =
                        CircleShape,

                    modifier =
                        Modifier
                            .size(
                                46.dp
                            )
                            .clickable {

                                scope.launch {
                                    drawerState.open()
                                }
                            }
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            "☰",

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


                Text(
                    homeSectionTitle(
                        section
                    ),

                    color =
                        colors.text,

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                SocietyAvatar(
                    initials =
                        initials,

                    colors =
                        colors,

                    size =
                        46.dp,

                    photoUri =
                        profilePhotoUri,

                    modifier =
                        Modifier.clickable {
                            showProfile =
                                true
                        }
                )
            }


            Spacer(
                Modifier.height(
                    18.dp
                )
            )


            if (
                !emailVerified
            ) {

                SocietyEmailVerificationBanner(
                    colors =
                        colors,

                    onResend = {

                        scope.launch {

                            try {

                                val token =
                                    SocietySession
                                        .validToken(
                                            prefs,
                                            forceRefresh =
                                                true
                                        )

                                val message =
                                    SocietyApi
                                        .resendVerification(
                                            token
                                        )

                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()

                            } catch (
                                error: Exception
                            ) {

                                Toast.makeText(
                                    context,
                                    error.message
                                        ?: "Não foi possível reenviar o e-mail.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },

                    onCheck = {

                        scope.launch {

                            try {

                                val token =
                                    SocietySession
                                        .validToken(
                                            prefs,
                                            forceRefresh =
                                                true
                                        )

                                val user =
                                    SocietyApi.me(
                                        token
                                    )

                                emailVerified =
                                    user.emailVerified

                                prefs.edit()
                                    .putBoolean(
                                        "auth_email_verified",
                                        user.emailVerified
                                    )
                                    .apply()

                                Toast.makeText(
                                    context,
                                    if (
                                        user.emailVerified
                                    )
                                        "E-mail verificado. Society Bots liberado."
                                    else
                                        "Seu e-mail ainda não foi verificado.",
                                    Toast.LENGTH_LONG
                                ).show()

                            } catch (
                                error: Exception
                            ) {

                                Toast.makeText(
                                    context,
                                    error.message
                                        ?: "Não foi possível verificar o estado da conta.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )
            }


            when (
                section
            ) {

                HomeSection.HOME ->
                    SocietyHomeDashboard(
                        colors =
                            colors,

                        draft =
                            if (
                                hasCreatedBot
                            )
                                createdBot
                            else
                                draft,

                        hasBot =
                            hasCreatedBot,

                        onNavigate = {
                            navigateTo(
                                it
                            )
                        }
                    )


                HomeSection.CREATE_BOT ->

                    if (
                        !emailVerified
                    ) {

                        LaunchedEffect(
                            Unit
                        ) {

                            Toast.makeText(
                                context,
                                "Confirme seu e-mail para criar bots.",
                                Toast.LENGTH_LONG
                            ).show()

                            section =
                                HomeSection.HOME
                        }

                    } else
                    SocietyCreateBotPage(
                        colors =
                            colors,

                        initialDraft =
                            pendingBot
                                ?: draft,

                        onCancel = {

                            pendingBot =
                                null

                            section =
                                HomeSection.BOTS
                        },

                        onCreated = {
                            newBot ->

                            /*
                             * NÃO adicionamos à lista ainda.
                             * O bot precisa passar pelo Builder.
                             */
                            pendingBot =
                                newBot

                            section =
                                HomeSection.BUILDER_CONSOLE
                        }
                    )


                HomeSection.BUILDER_CONSOLE -> {

                    val botToBuild =
                        pendingBot

                    if (
                        botToBuild ==
                        null
                    ) {

                        LaunchedEffect(
                            Unit
                        ) {
                            section =
                                HomeSection.BOTS
                        }

                    } else {

                        SocietyBuilderConsolePage(
                            colors =
                                colors,

                            draft =
                                botToBuild,

                            uid =
                                prefs.getString(
                                    "auth_uid",
                                    ""
                                ) ?: "",

                            onCancel = {

                                section =
                                    HomeSection.CREATE_BOT
                            },

                            onBotReady = {
                                remoteBotId ->

                                /*
                                 * Só entra em Meus Bots
                                 * depois de conectado.
                                 */
                                if (
                                    bots.none {
                                        it.id ==
                                            remoteBotId
                                    }
                                ) {

                                    val record =
                                        SocietyBotRecord(
                                            id =
                                                remoteBotId,

                                            draft =
                                                botToBuild
                                        )

                                    bots =
                                        bots +
                                            record

                                    saveSocietyBots(
                                        prefs,
                                        bots
                                    )

                                    /*
                                     * Persistência real:
                                     * APK/cache não é fonte da verdade.
                                     */
                                    val idToken =
                                        prefs.getString(
                                            "auth_id_token",
                                            null
                                        )

                                    if (
                                        !idToken.isNullOrBlank()
                                    ) {

                                        scope.launch {

                                            try {

                                                SocietyAccountApi
                                                    .upsertBot(
                                                        idToken =
                                                            idToken,

                                                        id =
                                                            record.id,

                                                        draft =
                                                            record.draft,

                                                        archived =
                                                            record.archived,

                                                        createdAt =
                                                            record.createdAt
                                                    )

                                            } catch (
                                                error: Exception
                                            ) {

                                                Toast.makeText(
                                                    context,
                                                    "Bot criado, mas a sincronização com a Cloud falhou: ${error.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }

                                    createdBot =
                                        botToBuild

                                    hasCreatedBot =
                                        true
                                }


                                pendingBot =
                                    null

                                Toast.makeText(
                                    context,
                                    "${botToBuild.name} criado e conectado.",
                                    Toast.LENGTH_LONG
                                ).show()


                                section =
                                    HomeSection.HOME
                            }
                        )
                    }
                }


                HomeSection.BOTS ->
                    SocietyBotsPage(
                        colors =
                            colors,

                        prefs =
                            prefs,

                        bots =
                            bots,

                        onCreate = {
                            navigateTo(
                                HomeSection.CREATE_BOT
                            )
                        },

                        onOpen = {
                            bot ->

                            managedBotId =
                                bot.id

                            section =
                                HomeSection.MANAGE_BOT
                        },

                        onUpdate = {
                            updated ->

                            bots =
                                bots.map {
                                    if (
                                        it.id ==
                                        updated.id
                                    )
                                        updated
                                    else
                                        it
                                }

                            saveSocietyBots(
                                prefs,
                                bots
                            )

                            scope.launch {

                                val idToken =
                                    prefs.getString(
                                        "auth_id_token",
                                        null
                                    )

                                if (
                                    !idToken.isNullOrBlank()
                                ) {

                                    try {

                                        SocietyAccountApi
                                            .upsertBot(
                                                idToken =
                                                    idToken,

                                                id =
                                                    updated.id,

                                                draft =
                                                    updated.draft,

                                                archived =
                                                    updated.archived,

                                                createdAt =
                                                    updated.createdAt
                                            )

                                    } catch (
                                        error: Exception
                                    ) {

                                        Toast.makeText(
                                            context,
                                            "Não foi possível sincronizar a alteração com a Cloud.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }

                            val newest =
                                bots
                                    .filter {
                                        !it.archived
                                    }
                                    .maxByOrNull {
                                        it.createdAt
                                    }

                            hasCreatedBot =
                                newest !=
                                    null

                            createdBot =
                                newest
                                    ?.draft
                                    ?: BotDraft()
                        },

                        onDelete = {
                            removed ->

                            bots =
                                bots.filterNot {
                                    it.id ==
                                    removed.id
                                }

                            saveSocietyBots(
                                prefs,
                                bots
                            )

                            scope.launch {

                                val idToken =
                                    prefs.getString(
                                        "auth_id_token",
                                        null
                                    )

                                if (
                                    !idToken.isNullOrBlank()
                                ) {

                                    try {

                                        SocietyAccountApi
                                            .deleteBot(
                                                idToken =
                                                    idToken,

                                                botId =
                                                    removed.id
                                            )

                                    } catch (
                                        error: Exception
                                    ) {

                                        Toast.makeText(
                                            context,
                                            "O bot saiu do aparelho, mas a remoção da Cloud falhou.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }

                            val newest =
                                bots
                                    .filter {
                                        !it.archived
                                    }
                                    .maxByOrNull {
                                        it.createdAt
                                    }

                            hasCreatedBot =
                                newest !=
                                    null

                            createdBot =
                                newest
                                    ?.draft
                                    ?: BotDraft()
                        }
                    )


                HomeSection.MANAGE_BOT ->
                    SocietyManageBotPage(
                        colors =
                            colors,

                        prefs =
                            prefs,

                        bot =
                            bots.firstOrNull {
                                it.id ==
                                    managedBotId
                            },

                        onCommands = {
                            section =
                                HomeSection.COMMANDS
                        },

                        onBotChanged = {
                            changed ->

                            bots =
                                bots.map {
                                    current ->

                                    if (
                                        current.id ==
                                        changed.id
                                    )
                                        changed
                                    else
                                        current
                                }

                            saveSocietyBots(
                                prefs,
                                bots
                            )
                        }
                    )


                HomeSection.COMMANDS ->
                    SocietyCommandsPage(
                        colors =
                            colors,

                        prefs =
                            prefs,

                        bot =
                            bots.firstOrNull {
                                it.id ==
                                    managedBotId
                            }
                                ?: activeBots
                                    .firstOrNull()
                    )



                HomeSection.WHATSAPP -> {

                    val whatsappScope =
                        androidx.compose.runtime
                            .rememberCoroutineScope()


                    val whatsappStates =
                        remember {
                            androidx.compose.runtime
                                .mutableStateMapOf<
                                    String,
                                    SocietyWhatsAppRemoteState
                                >()
                        }


                    val whatsappQrDialogBot =
                        remember {
                            mutableStateOf<SocietyWhatsAppBotUi?>(
                                null
                            )
                        }


                    val whatsappLogsTitle =
                        remember {
                            mutableStateOf<String?>(
                                null
                            )
                        }


                    val whatsappLogsLines =
                        remember {
                            mutableStateOf<List<String>?>(
                                null
                            )
                        }


                    androidx.compose.runtime.LaunchedEffect(
                        bots.map {
                            it.id
                        }
                    ) {

                        while (
                            true
                        ) {

                            bots.forEach {
                                bot ->

                                try {

                                    whatsappStates[
                                        bot.id
                                    ] =
                                        SocietyWhatsAppApi
                                            .status(
                                                prefs =
                                                    prefs,

                                                botId =
                                                    bot.id
                                            )

                                } catch (
                                    _: Exception
                                ) {
                                }
                            }


                            kotlinx.coroutines.delay(
                                3000
                            )
                        }
                    }


                    fun refreshWhatsApp(
                        botId: String
                    ) {

                        whatsappScope.launch {

                            try {

                                whatsappStates[
                                    botId
                                ] =
                                    SocietyWhatsAppApi
                                        .status(
                                            prefs =
                                                prefs,

                                            botId =
                                                botId
                                        )

                            } catch (
                                _: Exception
                            ) {
                            }
                        }
                    }


                    val whatsappBots =
                        bots.map {
                            bot ->

                            val remote =
                                whatsappStates[
                                    bot.id
                                ]


                            val status =
                                remote
                                    ?.status
                                    ?: "loading"


                            val connected =
                                remote
                                    ?.connected
                                    ?: false


                            val statusText =
                                when (
                                    status
                                ) {

                                    "connected" ->
                                        "Sessão ativa"

                                    "connecting" ->
                                        "Conectando..."

                                    "qr" ->
                                        "QR disponível"

                                    "pairing" ->
                                        "Código disponível"

                                    "logged_out" ->
                                        "Sessão expirada"

                                    "loading" ->
                                        "Consultando..."

                                    else ->
                                        "Sem sessão"
                                }


                            val detailText =
                                when (
                                    status
                                ) {

                                    "connected" ->
                                        "WhatsApp conectado"

                                    "connecting" ->
                                        "Aguardando conexão com o WhatsApp"

                                    "qr" ->
                                        "Escaneie o QR para conectar"

                                    "pairing" ->
                                        "Use o código de pareamento"

                                    "logged_out" ->
                                        "Faça um novo pareamento"

                                    "loading" ->
                                        "Consultando a Cloud..."

                                    else ->
                                        "Toque para conectar ao WhatsApp"
                                }


                            SocietyWhatsAppBotUi(
                                id =
                                    bot.id,

                                name =
                                    bot.draft.name,

                                connected =
                                    connected,

                                statusText =
                                    statusText,

                                detailText =
                                    detailText,

                                needsQr =
                                    remote
                                        ?.needsQr
                                        ?: false,

                                status =
                                    status,

                                qr =
                                    remote
                                        ?.qr,

                                pairingCode =
                                    remote
                                        ?.pairingCode
                            )
                        }


                    SocietyWhatsAppBotsPage(
                        bots =
                            whatsappBots,

                        onOpenBot = {
                            bot ->

                            managedBotId =
                                bot.id


                            Toast.makeText(
                                context,
                                bot.statusText,
                                Toast.LENGTH_SHORT
                            ).show()
                        },

                        onLogs = {
                            bot ->

                            whatsappScope.launch {

                                try {

                                    val lines =
                                        SocietyWhatsAppApi
                                            .logs(
                                                prefs =
                                                    prefs,

                                                botId =
                                                    bot.id
                                            )


                                    whatsappLogsTitle.value =
                                        bot.name


                                    whatsappLogsLines.value =
                                        lines

                                } catch (
                                    error: Exception
                                ) {

                                    Toast.makeText(
                                        context,
                                        error.message
                                            ?: "Falha ao carregar logs.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },

                        onConnect = {
                            bot ->

                            whatsappScope.launch {

                                try {

                                    SocietyWhatsAppApi
                                        .connect(
                                            prefs =
                                                prefs,

                                            botId =
                                                bot.id
                                        )


                                    Toast.makeText(
                                        context,
                                        "Conectando ${bot.name}...",
                                        Toast.LENGTH_SHORT
                                    ).show()


                                    kotlinx.coroutines.delay(
                                        1200
                                    )


                                    refreshWhatsApp(
                                        bot.id
                                    )

                                } catch (
                                    error: Exception
                                ) {

                                    Toast.makeText(
                                        context,
                                        error.message
                                            ?: "Falha ao conectar.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },

                        onShowQr = {
                            bot ->

                            whatsappQrDialogBot.value =
                                bot


                            refreshWhatsApp(
                                bot.id
                            )
                        },


                        onDisconnect = {
                            bot ->

                            whatsappScope.launch {

                                try {

                                    SocietyWhatsAppApi
                                        .disconnect(
                                            prefs =
                                                prefs,

                                            botId =
                                                bot.id
                                        )


                                    refreshWhatsApp(
                                        bot.id
                                    )


                                    Toast.makeText(
                                        context,
                                        "${bot.name} desconectado.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } catch (
                                    error: Exception
                                ) {

                                    Toast.makeText(
                                        context,
                                        error.message
                                            ?: "Falha ao desconectar.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },

                        onRestartSession = {
                            bot ->

                            whatsappScope.launch {

                                try {

                                    SocietyWhatsAppApi
                                        .restart(
                                            prefs =
                                                prefs,

                                            botId =
                                                bot.id
                                        )


                                    Toast.makeText(
                                        context,
                                        "Reiniciando ${bot.name}...",
                                        Toast.LENGTH_SHORT
                                    ).show()


                                    kotlinx.coroutines.delay(
                                        1200
                                    )


                                    refreshWhatsApp(
                                        bot.id
                                    )

                                } catch (
                                    error: Exception
                                ) {

                                    Toast.makeText(
                                        context,
                                        error.message
                                            ?: "Falha ao reiniciar.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },

                        onDeleteSession = {
                            bot ->

                            whatsappScope.launch {

                                try {

                                    SocietyWhatsAppApi
                                        .deleteSession(
                                            prefs =
                                                prefs,

                                            botId =
                                                bot.id
                                        )


                                    refreshWhatsApp(
                                        bot.id
                                    )


                                    Toast.makeText(
                                        context,
                                        "Sessão de ${bot.name} excluída.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } catch (
                                    error: Exception
                                ) {

                                    Toast.makeText(
                                        context,
                                        error.message
                                            ?: "Falha ao excluir sessão.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )


                    SocietyWhatsAppDialogs(
                        qrBot =
                            whatsappQrDialogBot.value,

                        logsTitle =
                            whatsappLogsTitle.value,

                        logs =
                            whatsappLogsLines.value,

                        onDismissQr = {

                            whatsappQrDialogBot.value =
                                null
                        },

                        onDismissLogs = {

                            whatsappLogsTitle.value =
                                null

                            whatsappLogsLines.value =
                                null
                        }
                    )
                }

                HomeSection.AI ->
                    SocietyAiPage(
                        colors =
                            colors,

                        prefs =
                            prefs
                    )


                HomeSection.SETTINGS ->
                    SocietySettingsPage(
                        colors =
                            colors,

                        darkMode =
                            darkMode,

                        onToggleTheme =
                            onToggleTheme,

                        onSecurity = {
                            section =
                                HomeSection.SECURITY
                        },

                        onAbout = {
                            section =
                                HomeSection.ABOUT
                        }
                    )


                HomeSection.SECURITY ->
                    SocietySecurityPage(
                        colors =
                            colors,

                        accountName =
                            accountName,

                        accountEmail =
                            accountEmail,

                        initials =
                            initials,

                        onLogout = {
                            showLogoutConfirm =
                                true
                        }
                    )


                HomeSection.ABOUT ->
                    SocietyAboutPage(
                        colors =
                            colors
                    )
            }
        }
    }


    if (
        showProfile
    ) {

        ModalBottomSheet(
            onDismissRequest = {
                showProfile =
                    false
            },

            containerColor =
                colors.background,

            contentColor =
                colors.text
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                20.dp
                        )
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        "Conta Society",

                        color =
                            colors.text,

                        fontSize =
                            20.sp,

                        fontWeight =
                            FontWeight.Bold,

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )


                    TextButton(
                        onClick = {
                            showProfile =
                                false
                        }
                    ) {

                        Text(
                            "Fechar",

                            color =
                                colors.primarySoft
                        )
                    }
                }


                Spacer(
                    Modifier.height(
                        15.dp
                    )
                )


                Surface(
                    color =
                        colors.surface,

                    shape =
                        RoundedCornerShape(
                            28.dp
                        ),

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                21.dp
                            )
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            SocietyAvatar(
                                initials =
                                    initials,

                                colors =
                                    colors,

                                size =
                                    72.dp,
                                photoUri =
                                    profilePhotoUri
                            )


                            Spacer(
                                Modifier.width(
                                    16.dp
                                )
                            )


                            Column(
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            ) {

                                Text(
                                    accountName,

                                    color =
                                        colors.text,

                                    fontSize =
                                        20.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Spacer(
                                    Modifier.height(
                                        3.dp
                                    )
                                )


                                Text(
                                    accountEmail,

                                    color =
                                        colors.secondary,

                                    fontSize =
                                        13.sp
                                )


                                Spacer(
                                    Modifier.height(
                                        7.dp
                                    )
                                )


                                Surface(
                                    color =
                                        colors.primary.copy(
                                            alpha =
                                                0.16f
                                        ),

                                    shape =
                                        RoundedCornerShape(
                                            40.dp
                                        )
                                ) {

                                    Text(
                                        "Society Member",

                                        color =
                                            colors.primarySoft,

                                        fontSize =
                                            11.sp,

                                        fontWeight =
                                            FontWeight.Bold,

                                        modifier =
                                            Modifier.padding(
                                                horizontal =
                                                    11.dp,

                                                vertical =
                                                    5.dp
                                            )
                                    )
                                }
                            }
                        }
                    }
                }


                Spacer(
                    Modifier.height(
                        14.dp
                    )
                )


                SocietyProfileAction(
                    colors =
                        colors,

                    icon =
                        "📷",

                    title =
                        "Alterar foto",

                    subtitle =
                        "Escolher uma imagem da galeria"
                ) {

                    photoLauncher.launch(
                        arrayOf(
                            "image/*"
                        )
                    )
                }


                SocietyProfileAction(
                    colors =
                        colors,

                    icon =
                        "✎",

                    title =
                        "Nome e sobrenome",

                    subtitle =
                        accountNameBase
                ) {

                    showNameEditor =
                        true
                }


                SocietyProfileAction(
                    colors =
                        colors,

                    icon =
                        "✉",

                    title =
                        "E-mail",

                    subtitle =
                        accountEmail
                ) {

                    section =
                        HomeSection.SECURITY

                    showProfile =
                        false
                }


                SocietyProfileAction(
                    colors =
                        colors,

                    icon =
                        "🔑",

                    title =
                        "Alterar senha",

                    subtitle =
                        "Enviar redefinição para seu e-mail"
                ) {

                    scope.launch {

                        try {

                            SocietyApi
                                .forgotPassword(
                                    accountEmail
                                )

                            Toast.makeText(
                                context,
                                "Enviamos as instruções de redefinição para seu e-mail.",
                                Toast.LENGTH_LONG
                            ).show()

                        } catch (
                            error: Exception
                        ) {

                            Toast.makeText(
                                context,
                                error.message
                                    ?: "Não foi possível enviar a redefinição.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }


                SocietyProfileAction(
                    colors =
                        colors,

                    icon =
                        "🛡",

                    title =
                        "Verificação em duas etapas",

                    subtitle =
                        "Configurar proteção adicional"
                ) {

                    Toast.makeText(
                        context,
                        "O fluxo MFA será conectado ao Firebase na próxima etapa de segurança.",
                        Toast.LENGTH_LONG
                    ).show()
                }


                SocietyProfileAction(
                    colors =
                        colors,

                    icon =
                        "↪",

                    title =
                        "Sair da conta",

                    subtitle =
                        "Encerrar sessão neste dispositivo",

                    danger =
                        true
                ) {

                    showProfile =
                        false

                    showLogoutConfirm =
                        true
                }


                Spacer(
                    Modifier.height(
                        34.dp
                    )
                )
            }
        }
    }


    if (
        showNameEditor
    ) {

        var editedName by remember(
            showNameEditor
        ) {
            mutableStateOf(
                accountNameBase
            )
        }


        AlertDialog(
            onDismissRequest = {
                showNameEditor =
                    false
            },

            title = {
                Text(
                    "Nome e sobrenome"
                )
            },

            text = {

                OutlinedTextField(
                    value =
                        editedName,

                    onValueChange = {
                        editedName =
                            it.take(60)
                    },

                    singleLine =
                        true,

                    label = {
                        Text(
                            "Nome completo"
                        )
                    }
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val clean =
                            editedName
                                .trim()

                        if (
                            clean.length >= 2
                        ) {

                            accountName =
                                clean

                            prefs.edit()
                                .putString(
                                    "auth_display_name",
                                    clean
                                )
                                .apply()

                            showNameEditor =
                                false
                        }
                    }
                ) {

                    Text(
                        "Salvar",
                        color =
                            colors.primarySoft
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showNameEditor =
                            false
                    }
                ) {

                    Text(
                        "Cancelar"
                    )
                }
            }
        )
    }


    AnimatedVisibility(
        visible =
            showExitConfirm,

        enter =
            fadeIn(
                tween(180)
            ),

        exit =
            fadeOut(
                tween(150)
            )
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize(),

            contentAlignment =
                Alignment.BottomCenter
        ) {

            Surface(
                color =
                    colors.surface,

                shape =
                    RoundedCornerShape(
                        22.dp
                    ),

                shadowElevation =
                    10.dp,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                18.dp,

                            vertical =
                                18.dp
                        )
                        .navigationBarsPadding()
                        .border(
                            1.dp,
                            colors.border,
                            RoundedCornerShape(
                                22.dp
                            )
                        )
            ) {

                Row(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                17.dp,

                            vertical =
                                15.dp
                        ),

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
                            "Deseja sair?",

                            color =
                                colors.text,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                15.sp
                        )

                        Text(
                            "Pressione voltar novamente ou escolha uma opção.",

                            color =
                                colors.secondary,

                            fontSize =
                                11.sp
                        )
                    }


                    TextButton(
                        onClick = {
                            showExitConfirm =
                                false
                        }
                    ) {

                        Text(
                            "Não",
                            color =
                                colors.secondary
                        )
                    }


                    TextButton(
                        onClick = {

                            (
                                context as?
                                Activity
                            )?.finish()
                        }
                    ) {

                        Text(
                            "Sim",
                            color =
                                colors.primarySoft,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }


    if (
        showLogoutConfirm
    ) {

        AlertDialog(
            onDismissRequest = {
                showLogoutConfirm =
                    false
            },

            title = {
                Text(
                    "Sair da conta?"
                )
            },

            text = {
                Text(
                    "Sua sessão será encerrada neste dispositivo. As configurações locais do bot serão mantidas."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showLogoutConfirm =
                            false

                        onLogout()
                    }
                ) {

                    Text(
                        "Sair",

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showLogoutConfirm =
                            false
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


private fun homeSectionTitle(
    section: HomeSection
): String {

    return when (
        section
    ) {

        HomeSection.HOME ->
            "Início"

        HomeSection.CREATE_BOT ->
            "Criar bot"

        HomeSection.BUILDER_CONSOLE ->
            "Society AI"

        HomeSection.BOTS ->
            "Meus bots"

        HomeSection.MANAGE_BOT ->
            "Gerenciar bot"

        HomeSection.COMMANDS ->
            "Comandos"

        HomeSection.WHATSAPP ->
            "WhatsApp"

        HomeSection.AI ->
            "Inteligência artificial"

        HomeSection.SETTINGS ->
            "Configurações"

        HomeSection.SECURITY ->
            "Conta e segurança"

        HomeSection.ABOUT ->
            "Sobre"
    }
}


@Composable
private fun SocietyAvatar(
    initials: String,
    colors: SocietyColors,
    size: Dp,
    modifier: Modifier = Modifier,
    photoUri: String? = null
) {

    Surface(
        color =
            colors.primary.copy(
                alpha = 0.18f
            ),

        shape =
            CircleShape,

        modifier =
            modifier
                .size(size)
                .border(
                    width = 2.dp,
                    color =
                        colors.primary.copy(
                            alpha = 0.72f
                        ),
                    shape = CircleShape
                )
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            if (
                !photoUri.isNullOrBlank()
            ) {

                AsyncImage(
                    model =
                        photoUri,

                    contentDescription =
                        "Foto do perfil",

                    contentScale =
                        ContentScale.Crop,

                    modifier =
                        Modifier.fillMaxSize()
                )

            } else {

                Text(
                    initials,

                    color =
                        colors.primarySoft,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        if (
                            size >= 65.dp
                        )
                            22.sp
                        else
                            14.sp
                )
            }
        }
    }
}


@Composable
private fun SocietyDrawerItem(
    selected: Boolean,
    icon: String,
    title: String,
    colors: SocietyColors,
    onClick: () -> Unit
) {

    val background by
        animateColorAsState(
            targetValue =
                if (
                    selected
                )
                    colors.primary.copy(
                        alpha =
                            0.19f
                    )
                else
                    Color.Transparent,

            label =
                "drawer_item"
        )


    Surface(
        color =
            background,

        shape =
            RoundedCornerShape(
                30.dp
            ),

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        12.dp,

                    vertical =
                        2.dp
                )
                .clickable {
                    onClick()
                }
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal =
                        18.dp,

                    vertical =
                        14.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                icon,

                color =
                    if (selected)
                        colors.primarySoft
                    else
                        colors.text,

                fontSize =
                    20.sp,

                modifier =
                    Modifier.width(
                        34.dp
                    )
            )


            Spacer(
                Modifier.width(
                    9.dp
                )
            )


            Text(
                title,

                color =
                    if (selected)
                        colors.primarySoft
                    else
                        colors.text,

                fontSize =
                    15.sp,

                fontWeight =
                    if (selected)
                        FontWeight.Bold
                    else
                        FontWeight.Medium
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SocietyBotsPage(
    colors: SocietyColors,
    prefs: android.content.SharedPreferences,
    bots: List<SocietyBotRecord>,
    onCreate: () -> Unit,
    onOpen: (SocietyBotRecord) -> Unit,
    onUpdate: (SocietyBotRecord) -> Unit,
    onDelete: (SocietyBotRecord) -> Unit
) {

    var selected by remember {
        mutableStateOf(
            emptySet<String>()
        )
    }

    var menuBotId by remember {
        mutableStateOf<String?>(null)
    }

    var renameBot by remember {
        mutableStateOf<SocietyBotRecord?>(null)
    }

    var deleteBot by remember {
        mutableStateOf<SocietyBotRecord?>(null)
    }


    val selectionMode =
        selected.isNotEmpty()


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    bottom =
                        32.dp
                )
    ) {

        if (selectionMode) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    "${selected.size} selecionado(s)",

                    color =
                        colors.text,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        18.sp,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                TextButton(
                    onClick = {

                        selected =
                            if (
                                selected.size ==
                                bots.size
                            ) {
                                emptySet()
                            } else {
                                bots
                                    .map {
                                        it.id
                                    }
                                    .toSet()
                            }
                    }
                ) {

                    Text(
                        if (
                            selected.size ==
                            bots.size
                        )
                            "Limpar"
                        else
                            "Selecionar tudo",

                        color =
                            colors.primarySoft
                    )
                }
            }


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                Surface(
                    color =
                        colors.surface,

                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),

                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .clickable {

                                bots
                                    .filter {
                                        it.id in
                                            selected
                                    }
                                    .forEach {

                                        onUpdate(
                                            it.copy(
                                                archived =
                                                    true
                                            )
                                        )
                                    }

                                selected =
                                    emptySet()
                            }
                ) {

                    Text(
                        "Arquivar",

                        color =
                            colors.text,

                        textAlign =
                            TextAlign.Center,

                        modifier =
                            Modifier.padding(
                                14.dp
                            )
                    )
                }


                Surface(
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                            .copy(
                                alpha =
                                    0.13f
                            ),

                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),

                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .clickable {

                                bots
                                    .filter {
                                        it.id in
                                            selected
                                    }
                                    .forEach {
                                        onDelete(
                                            it
                                        )
                                    }

                                selected =
                                    emptySet()
                            }
                ) {

                    Text(
                        "Excluir",

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

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


        if (
            !selectionMode
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
                        "Seus bots",

                        color =
                            colors.text,

                        fontSize =
                            27.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(
                        if (bots.isEmpty())
                            "Você ainda não criou nenhum bot."
                        else
                            "${bots.size} bot(s) no Society Bots",

                        color =
                            colors.secondary,

                        fontSize =
                            13.sp
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
                                48.dp
                            )
                            .clickable {
                                onCreate()
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
                                27.sp,

                            fontWeight =
                                FontWeight.Medium
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


        SocietyImportBotButton(
            prefs =
                prefs,

            colors =
                colors
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        if (
            bots.isEmpty()
        ) {

            Surface(
                color =
                    colors.surface,

                shape =
                    RoundedCornerShape(
                        28.dp
                    ),

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            26.dp
                        ),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        "🤖",

                        fontSize =
                            54.sp
                    )


                    Spacer(
                        Modifier.height(
                            14.dp
                        )
                    )


                    Text(
                        "Nenhum bot ainda",

                        color =
                            colors.text,

                        fontSize =
                            21.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        Modifier.height(
                            5.dp
                        )
                    )


                    Text(
                        "Crie seu primeiro bot e ele aparecerá aqui.",

                        color =
                            colors.secondary,

                        textAlign =
                            TextAlign.Center
                    )


                    Spacer(
                        Modifier.height(
                            20.dp
                        )
                    )


                    PrimaryButton(
                        colors,
                        "Criar bot",
                        true,
                        onCreate
                    )
                }
            }

            return@Column
        }


        bots.forEach { bot ->

            val selectedBot =
                bot.id in
                    selected


            Box {

                Surface(
                    color =
                        if (selectedBot)
                            colors.primary.copy(
                                alpha =
                                    0.14f
                            )
                        else
                            colors.surface,

                    shape =
                        RoundedCornerShape(
                            25.dp
                        ),

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(
                                width =
                                    if (selectedBot)
                                        1.5.dp
                                    else
                                        1.dp,

                                color =
                                    if (selectedBot)
                                        colors.primary
                                    else
                                        colors.border,

                                shape =
                                    RoundedCornerShape(
                                        25.dp
                                    )
                            )
                            .combinedClickable(

                                onClick = {

                                    if (
                                        selectionMode
                                    ) {

                                        selected =
                                            if (
                                                selectedBot
                                            ) {
                                                selected -
                                                    bot.id
                                            } else {
                                                selected +
                                                    bot.id
                                            }

                                    } else {

                                        onOpen(
                                            bot
                                        )
                                    }
                                },

                                onLongClick = {

                                    selected =
                                        selected +
                                        bot.id
                                }
                            )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                19.dp
                            )
                    ) {

                        Row(
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
                                        17.dp
                                    ),

                                modifier =
                                    Modifier.size(
                                        52.dp
                                    )
                            ) {

                                Box(
                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Text(
                                        "🤖",

                                        fontSize =
                                            25.sp
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
                                    bot.draft.name,

                                    color =
                                        colors.text,

                                    fontSize =
                                        18.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Text(
                                    bot.draft.library
                                        .ifBlank {
                                            "Biblioteca não definida"
                                        },

                                    color =
                                        colors.secondary,

                                    fontSize =
                                        12.sp
                                )
                            }


                            if (
                                selectedBot
                            ) {

                                Text(
                                    "✓",

                                    color =
                                        colors.primarySoft,

                                    fontSize =
                                        22.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                            } else {

                                Box {

                                    Text(
                                        "⋮",

                                        color =
                                            colors.secondary,

                                        fontSize =
                                            27.sp,

                                        modifier =
                                            Modifier
                                                .padding(
                                                    8.dp
                                                )
                                                .clickable {

                                                    menuBotId =
                                                        bot.id
                                                }
                                    )


                                    DropdownMenu(
                                        expanded =
                                            menuBotId ==
                                                bot.id,

                                        onDismissRequest = {
                                            menuBotId =
                                                null
                                        }
                                    ) {

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Gerenciar"
                                                )
                                            },

                                            onClick = {

                                                menuBotId =
                                                    null

                                                onOpen(
                                                    bot
                                                )
                                            }
                                        )


                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Renomear"
                                                )
                                            },

                                            onClick = {

                                                menuBotId =
                                                    null

                                                renameBot =
                                                    bot
                                            }
                                        )


                                        DropdownMenuItem(
                                            text = {

                                                Text(
                                                    if (
                                                        bot.archived
                                                    )
                                                        "Desarquivar"
                                                    else
                                                        "Arquivar"
                                                )
                                            },

                                            onClick = {

                                                menuBotId =
                                                    null

                                                onUpdate(
                                                    bot.copy(
                                                        archived =
                                                            !bot.archived
                                                    )
                                                )
                                            }
                                        )


                                        DropdownMenuItem(
                                            text = {

                                                Text(
                                                    "Excluir",

                                                    color =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .error
                                                )
                                            },

                                            onClick = {

                                                menuBotId =
                                                    null

                                                deleteBot =
                                                    bot
                                            }
                                        )
                                    }
                                }
                            }
                        }


                        if (
                            bot.archived
                        ) {

                            Spacer(
                                Modifier.height(
                                    13.dp
                                )
                            )


                            Surface(
                                color =
                                    colors.surfaceAlt,

                                shape =
                                    RoundedCornerShape(
                                        30.dp
                                    )
                            ) {

                                Text(
                                    "Arquivado",

                                    color =
                                        colors.secondary,

                                    fontSize =
                                        11.sp,

                                    modifier =
                                        Modifier.padding(
                                            horizontal =
                                                10.dp,

                                            vertical =
                                                5.dp
                                        )
                                )
                            }
                        }
                    }
                }
            }


            Spacer(
                Modifier.height(
                    12.dp
                )
            )
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
                    .clickable {
                        onCreate()
                    }
        ) {

            Text(
                "+  Criar outro bot",

                color =
                    colors.primarySoft,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.Center,

                modifier =
                    Modifier.padding(
                        17.dp
                    )
            )
        }
    }


    renameBot?.let { bot ->

        var value by remember(
            bot.id
        ) {
            mutableStateOf(
                bot.draft.name
            )
        }


        AlertDialog(
            onDismissRequest = {
                renameBot =
                    null
            },

            title = {
                Text(
                    "Renomear bot"
                )
            },

            text = {

                OutlinedTextField(
                    value =
                        value,

                    onValueChange = {
                        value =
                            it.take(
                                32
                            )
                    },

                    singleLine =
                        true,

                    label = {
                        Text(
                            "Nome"
                        )
                    }
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val newName =
                            value.trim()

                        if (
                            newName.length >=
                            2
                        ) {

                            onUpdate(
                                bot.copy(
                                    draft =
                                        bot.draft.copy(
                                            name =
                                                newName
                                        )
                                )
                            )

                            renameBot =
                                null
                        }
                    }
                ) {

                    Text(
                        "Salvar",
                        color =
                            colors.primarySoft
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        renameBot =
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


    deleteBot?.let { bot ->

        AlertDialog(
            onDismissRequest = {
                deleteBot =
                    null
            },

            title = {
                Text(
                    "Excluir ${bot.draft.name}?"
                )
            },

            text = {
                Text(
                    "Essa ação removerá o bot da lista. Quando conectarmos o Builder à Cloud, também poderemos remover os arquivos e a sessão dele."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        onDelete(
                            bot
                        )

                        deleteBot =
                            null
                    }
                ) {

                    Text(
                        "Excluir",
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deleteBot =
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


@Composable
private fun SocietyCreateBotPage(
    colors: SocietyColors,
    initialDraft: BotDraft,
    onCancel: () -> Unit,
    onCreated: (BotDraft) -> Unit
) {

    var name by remember {
        mutableStateOf(
            initialDraft.name
        )
    }

    var owner by remember {
        mutableStateOf(
            initialDraft.owner
        )
    }

    var library by remember {
        mutableStateOf(
            initialDraft.library
        )
    }

    var style by remember {
        mutableStateOf(
            initialDraft.style
        )
    }

    var features by remember {
        mutableStateOf(
            initialDraft.features
        )
    }


    val libraries =
        listOf(
            "@itsliaaa/baileys",
            "WhiskeySockets/Baileys"
        )


    val styles =
        listOf(
            "Profissional",
            "Divertido",
            "Minimalista",
            "Personalizado"
        )


    val availableFeatures =
        listOf(
            "Menus",
            "Moderação",
            "Boas-vindas",
            "Stickers",
            "Downloads",
            "IA"
        )


    val valid =
        name.trim()
            .length >= 2 &&
        owner.trim()
            .length >= 8 &&
        library.isNotBlank() &&
        style.isNotBlank()


    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    bottom =
                        34.dp
                )
    ) {

        Text(
            "Novo bot",

            color =
                colors.text,

            fontSize =
                28.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                6.dp
            )
        )


        Text(
            "Configure a base do seu bot. Você poderá alterar tudo depois.",

            color =
                colors.secondary,

            lineHeight =
                21.sp
        )


        Spacer(
            Modifier.height(
                27.dp
            )
        )


        SocietyCreateSection(
            colors =
                colors,

            title =
                "Identidade"
        ) {

            AnimatedAuthField(
                value =
                    name,

                onValueChange = {
                    name =
                        it.take(32)
                },

                label =
                    "Nome do bot",

                colors =
                    colors,

                imeAction =
                    ImeAction.Next
            )


            Spacer(
                Modifier.height(
                    14.dp
                )
            )


            AnimatedAuthField(
                value =
                    owner,

                onValueChange = {

                    owner =
                        it
                            .filter { char ->
                                char.isDigit() ||
                                char == '+'
                            }
                            .take(20)
                },

                label =
                    "Número do dono",

                colors =
                    colors,

                keyboardType =
                    KeyboardType.Phone,

                imeAction =
                    ImeAction.Done
            )


            Spacer(
                Modifier.height(
                    8.dp
                )
            )


            Text(
                "Use DDI + DDD + número. Ex.: +5592999999999",

                color =
                    colors.muted,

                fontSize =
                    11.sp
            )
        }


        Spacer(
            Modifier.height(
                20.dp
            )
        )


        SocietyCreateSection(
            colors =
                colors,

            title =
                "Biblioteca WhatsApp"
        ) {

            libraries.forEach {

                SocietyBotChoice(
                    colors =
                        colors,

                    title =
                        it,

                    selected =
                        library ==
                            it
                ) {
                    library =
                        it
                }


                Spacer(
                    Modifier.height(
                        9.dp
                    )
                )
            }
        }


        Spacer(
            Modifier.height(
                20.dp
            )
        )


        SocietyCreateSection(
            colors =
                colors,

            title =
                "Personalidade"
        ) {

            styles.forEach {

                SocietyBotChoice(
                    colors =
                        colors,

                    title =
                        it,

                    selected =
                        style ==
                            it
                ) {
                    style =
                        it
                }


                Spacer(
                    Modifier.height(
                        9.dp
                    )
                )
            }
        }


        Spacer(
            Modifier.height(
                20.dp
            )
        )


        SocietyCreateSection(
            colors =
                colors,

            title =
                "Recursos iniciais"
        ) {

            Text(
                "Escolha o que já deve vir preparado.",

                color =
                    colors.secondary,

                fontSize =
                    12.sp
            )


            Spacer(
                Modifier.height(
                    13.dp
                )
            )


            availableFeatures
                .chunked(2)
                .forEach { row ->

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {

                        row.forEach { feature ->

                            val selected =
                                feature in
                                    features


                            Surface(
                                color =
                                    if (selected)
                                        colors.primary.copy(
                                            alpha =
                                                0.18f
                                        )
                                    else
                                        colors.surfaceAlt,

                                shape =
                                    RoundedCornerShape(
                                        18.dp
                                    ),

                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                                        .border(
                                            width =
                                                1.dp,

                                            color =
                                                if (selected)
                                                    colors.primary
                                                else
                                                    colors.border,

                                            shape =
                                                RoundedCornerShape(
                                                    18.dp
                                                )
                                        )
                                        .clickable {

                                            features =
                                                if (selected) {

                                                    features -
                                                        feature

                                                } else {

                                                    features +
                                                        feature
                                                }
                                        }
                            ) {

                                Text(
                                    if (selected)
                                        "✓  $feature"
                                    else
                                        feature,

                                    color =
                                        if (selected)
                                            colors.primarySoft
                                        else
                                            colors.text,

                                    fontSize =
                                        13.sp,

                                    fontWeight =
                                        if (selected)
                                            FontWeight.Bold
                                        else
                                            FontWeight.Medium,

                                    modifier =
                                        Modifier.padding(
                                            horizontal =
                                                13.dp,

                                            vertical =
                                                14.dp
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
                            10.dp
                        )
                    )
                }
        }


        Spacer(
            Modifier.height(
                27.dp
            )
        )


        Surface(
            color =
                if (valid)
                    colors.primary
                else
                    colors.surfaceAlt,

            shape =
                RoundedCornerShape(
                    20.dp
                ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled =
                            valid
                    ) {

                        onCreated(
                            BotDraft(
                                name =
                                    name.trim(),

                                owner =
                                    owner.trim(),

                                library =
                                    library,

                                features =
                                    features,

                                style =
                                    style
                            )
                        )
                    }
        ) {

            Text(
                "Criar bot",

                color =
                    if (valid)
                        Color.White
                    else
                        colors.muted,

                textAlign =
                    TextAlign.Center,

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    16.sp,

                modifier =
                    Modifier.padding(
                        vertical =
                            17.dp
                    )
            )
        }


        Spacer(
            Modifier.height(
                10.dp
            )
        )


        TextButton(
            onClick =
                onCancel,

            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        ) {

            Text(
                "Cancelar",

                color =
                    colors.secondary
            )
        }
    }
}


@Composable
private fun SocietyCreateSection(
    colors: SocietyColors,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Surface(
        color =
            colors.surface,

        shape =
            RoundedCornerShape(
                25.dp
            ),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(
                title,

                color =
                    colors.text,

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(
                    15.dp
                )
            )


            content()
        }
    }
}


@Composable
private fun SocietyBotChoice(
    colors: SocietyColors,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        color =
            if (selected)
                colors.primary.copy(
                    alpha =
                        0.15f
                )
            else
                colors.surfaceAlt,

        shape =
            RoundedCornerShape(
                18.dp
            ),

        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width =
                        if (selected)
                            1.5.dp
                        else
                            1.dp,

                    color =
                        if (selected)
                            colors.primary
                        else
                            colors.border,

                    shape =
                        RoundedCornerShape(
                            18.dp
                        )
                )
                .clickable {
                    onClick()
                }
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal =
                        16.dp,

                    vertical =
                        15.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                if (selected)
                    "●"
                else
                    "○",

                color =
                    if (selected)
                        colors.primarySoft
                    else
                        colors.secondary,

                fontSize =
                    18.sp
            )


            Spacer(
                Modifier.width(
                    11.dp
                )
            )


            Text(
                title,

                color =
                    if (selected)
                        colors.primarySoft
                    else
                        colors.text,

                fontSize =
                    14.sp,

                fontWeight =
                    if (selected)
                        FontWeight.Bold
                    else
                        FontWeight.Medium
            )
        }
    }
}


@Composable
private fun SocietyEmailVerificationBanner(
    colors: SocietyColors,
    onResend: () -> Unit,
    onCheck: () -> Unit
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
            Modifier
                .fillMaxWidth()
                .border(
                    width =
                        1.dp,

                    color =
                        colors.primary.copy(
                            alpha =
                                0.32f
                        ),

                    shape =
                        RoundedCornerShape(
                            22.dp
                        )
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(
                "⚠ Confirme seu e-mail",

                color =
                    colors.text,

                fontSize =
                    17.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(
                    7.dp
                )
            )


            Text(
                "Verifique seu e-mail para liberar a criação, importação e conexão de bots.",

                color =
                    colors.secondary,

                fontSize =
                    13.sp,

                lineHeight =
                    19.sp
            )


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                TextButton(
                    onClick =
                        onResend
                ) {

                    Text(
                        "Reenviar e-mail",

                        color =
                            colors.primarySoft
                    )
                }


                TextButton(
                    onClick =
                        onCheck
                ) {

                    Text(
                        "Já verifiquei",

                        color =
                            colors.primarySoft,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
private fun SocietyHomeDashboard(
    colors: SocietyColors,
    draft: BotDraft,
    hasBot: Boolean,
    onNavigate: (HomeSection) -> Unit
) {

    val configuredDraft =
        !hasBot &&
        (
            draft.name.isNotBlank() ||
            draft.owner.isNotBlank() ||
            draft.library.isNotBlank() ||
            draft.features.isNotEmpty() ||
            draft.style.isNotBlank()
        )


    val botName =
        if (
            hasBot ||
            configuredDraft
        ) {
            draft.name.ifBlank {
                "Meu Bot"
            }
        } else {
            "Nenhum bot criado"
        }


    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
    ) {

        Spacer(
            Modifier.height(
                8.dp
            )
        )


        Text(
            "Olá 👋",

            color =
                colors.secondary,

            fontSize =
                14.sp
        )


        Text(
            if (
                hasBot
            )
                "$botName está pronto."
            else if (
                configuredDraft
            )
                "$botName está configurado."
            else
                "Crie seu primeiro bot.",

            color =
                colors.text,

            fontSize =
                28.sp,

            fontWeight =
                FontWeight.Bold,

            lineHeight =
                34.sp
        )


        Spacer(
            Modifier.height(
                22.dp
            )
        )


        Surface(
            color =
                colors.surface,

            shape =
                RoundedCornerShape(
                    28.dp
                ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        colors.primary.copy(
                            alpha =
                                0.4f
                        ),
                        RoundedCornerShape(
                            28.dp
                        )
                    )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        21.dp
                    )
            ) {

                Text(
                    if (
                        hasBot
                    )
                        "🤖  $botName"
                    else if (
                        configuredDraft
                    )
                        "✨  $botName"
                    else
                        "🤖  Nenhum bot ainda",

                    color =
                        colors.text,

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    Modifier.height(
                        7.dp
                    )
                )


                Text(
                    if (
                        hasBot ||
                        configuredDraft
                    ) {
                        draft.library.ifBlank {
                            "Configuração inicial pronta"
                        }
                    } else {
                        "Configure seu primeiro bot para começar."
                    },

                    color =
                        colors.secondary,

                    fontSize =
                        13.sp
                )


                Spacer(
                    Modifier.height(
                        17.dp
                    )
                )


                Surface(
                    color =
                        colors.primary,

                    shape =
                        RoundedCornerShape(
                            17.dp
                        ),

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate(
                                    if (hasBot)
                                        HomeSection.BOTS
                                    else
                                        HomeSection.CREATE_BOT
                                )
                            }
                ) {

                    Text(
                        if (hasBot)
                            "Gerenciar bot  →"
                        else
                            "Criar bot  →",

                        color =
                            Color.White,

                        fontWeight =
                            FontWeight.Bold,

                        textAlign =
                            TextAlign.Center,

                        modifier =
                            Modifier.padding(
                                15.dp
                            )
                    )
                }
            }
        }


        Spacer(
            Modifier.height(
                27.dp
            )
        )


        Text(
            "Acesso rápido",

            color =
                colors.text,

            fontSize =
                19.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                14.dp
            )
        )


        Row(
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            SocietyHomeShortcut(
                colors,
                "◉",
                "WhatsApp",
                Modifier.weight(1f)
            ) {
                onNavigate(
                    HomeSection.WHATSAPP
                )
            }


            SocietyHomeShortcut(
                colors,
                "⚡",
                "Comandos",
                Modifier.weight(1f)
            ) {
                onNavigate(
                    HomeSection.COMMANDS
                )
            }
        }


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        Row(
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            SocietyHomeShortcut(
                colors,
                "✦",
                "IA",
                Modifier.weight(1f)
            ) {
                onNavigate(
                    HomeSection.AI
                )
            }


            SocietyHomeShortcut(
                colors,
                "⚙",
                "Configurações",
                Modifier.weight(1f)
            ) {
                onNavigate(
                    HomeSection.SETTINGS
                )
            }
        }


        Spacer(
            Modifier.height(
                30.dp
            )
        )
    }
}


@Composable
private fun SocietyHomeShortcut(
    colors: SocietyColors,
    icon: String,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {

    Surface(
        color =
            colors.surface,

        shape =
            RoundedCornerShape(
                22.dp
            ),

        modifier =
            modifier.clickable {
                onClick()
            }
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Text(
                icon,

                color =
                    colors.primarySoft,

                fontSize =
                    24.sp
            )


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            Text(
                label,

                color =
                    colors.text,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


@Composable
private fun SocietySettingsPage(
    colors: SocietyColors,
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    onSecurity: () -> Unit,
    onAbout: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
    ) {

        Text(
            "Preferências",

            color =
                colors.secondary,

            fontSize =
                13.sp
        )


        Spacer(
            Modifier.height(
                14.dp
            )
        )


        SocietySettingsCard(
            colors =
                colors
        ) {

            SocietySwitchRow(
                colors =
                    colors,

                icon =
                    if (darkMode)
                        "🌙"
                    else
                        "☀",

                title =
                    "Tema escuro",

                subtitle =
                    "Alterar aparência do aplicativo",

                checked =
                    darkMode,

                onCheckedChange = {
                    onToggleTheme()
                }
            )
        }


        Spacer(
            Modifier.height(
                15.dp
            )
        )


        SocietySettingsCard(
            colors =
                colors
        ) {

            SocietySettingsAction(
                colors,
                "🤖",
                "Configuração dos bots",
                "Biblioteca, estilo e recursos"
            ) {}


            HorizontalDivider(
                color =
                    colors.border
            )


            SocietySettingsAction(
                colors,
                "◆",
                "Conta e segurança",
                "Sessão, senha e privacidade"
            ) {
                onSecurity()
            }


            HorizontalDivider(
                color =
                    colors.border
            )


            SocietySettingsAction(
                colors,
                "🔔",
                "Notificações",
                "Alertas e atividades do bot"
            ) {}


            HorizontalDivider(
                color =
                    colors.border
            )


            SocietySettingsAction(
                colors,
                "🌐",
                "Idioma",
                "Português (Brasil)"
            ) {}
        }


        Spacer(
            Modifier.height(
                15.dp
            )
        )


        SocietySettingsCard(
            colors =
                colors
        ) {

            SocietySettingsAction(
                colors,
                "ⓘ",
                "Sobre",
                "Versão, termos e informações"
            ) {
                onAbout()
            }
        }


        Spacer(
            Modifier.height(
                30.dp
            )
        )
    }
}


@Composable
private fun SocietySecurityPage(
    colors: SocietyColors,
    accountName: String,
    accountEmail: String,
    initials: String,
    onLogout: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
    ) {

        SocietyAvatar(
            initials,
            colors,
            74.dp,
            Modifier.align(
                Alignment.CenterHorizontally
            )
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        Text(
            accountName,

            color =
                colors.text,

            fontSize =
                20.sp,

            fontWeight =
                FontWeight.Bold,

            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        )


        Text(
            accountEmail,

            color =
                colors.secondary,

            fontSize =
                13.sp,

            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        )


        Spacer(
            Modifier.height(
                28.dp
            )
        )


        SocietySettingsCard(
            colors
        ) {

            SocietySettingsAction(
                colors,
                "✉",
                "E-mail",
                accountEmail
            ) {}


            HorizontalDivider(
                color =
                    colors.border
            )


            SocietySettingsAction(
                colors,
                "🔑",
                "Alterar senha",
                "Enviar redefinição por e-mail"
            ) {}


            HorizontalDivider(
                color =
                    colors.border
            )


            SocietySettingsAction(
                colors,
                "▣",
                "Sessões",
                "Gerenciar dispositivos conectados"
            ) {}
        }


        Spacer(
            Modifier.height(
                18.dp
            )
        )


        Surface(
            color =
                MaterialTheme
                    .colorScheme
                    .error
                    .copy(
                        alpha =
                            0.12f
                    ),

            shape =
                RoundedCornerShape(
                    20.dp
                ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onLogout()
                    }
        ) {

            Text(
                "Sair da conta",

                color =
                    MaterialTheme
                        .colorScheme
                        .error,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.Center,

                modifier =
                    Modifier.padding(
                        17.dp
                    )
            )
        }
    }
}


@Composable
private fun SocietyAboutPage(
    colors: SocietyColors
) {

    Column(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Image(
            painter =
                painterResource(
                    R.drawable.splash
                ),

            contentDescription =
                null,

            modifier =
                Modifier.size(
                    110.dp
                )
        )


        Text(
            "Society Bots",

            color =
                colors.text,

            fontSize =
                25.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                5.dp
            )
        )


        Text(
            "Versão 1.0.3",

            color =
                colors.secondary,

            fontSize =
                13.sp
        )


        Spacer(
            Modifier.height(
                25.dp
            )
        )


        Text(
            "Crie, configure e gerencie bots de WhatsApp diretamente pelo seu dispositivo.",

            color =
                colors.secondary,

            textAlign =
                TextAlign.Center,

            lineHeight =
                22.sp
        )
    }
}




@Composable
private fun SocietyManageBotPage(
    colors: SocietyColors,
    prefs: android.content.SharedPreferences,
    bot: SocietyBotRecord?,
    onCommands: () -> Unit,
    onBotChanged: (SocietyBotRecord) -> Unit
) {

    if (
        bot ==
        null
    ) {

        SocietySimplePage(
            colors =
                colors,

            icon =
                "🤖",

            title =
                "Bot não encontrado",

            subtitle =
                "Não foi possível carregar esse bot."
        )

        return
    }


    val scope =
        rememberCoroutineScope()


    val uid =
        prefs.getString(
            "auth_uid",
            ""
        ).orEmpty()


    var liveStatus by remember(
        bot.id
    ) {
        mutableStateOf(
            bot.status
        )
    }


    var logs by remember(
        bot.id
    ) {
        mutableStateOf<List<String>>(
            emptyList()
        )
    }


    var loadingAction by remember {
        mutableStateOf(
            false
        )
    }


    suspend fun refresh() {

        if (
            uid.isBlank()
        ) {
            return
        }


        try {

            liveStatus =
                SocietyBotControlApi
                    .status(
                        uid,
                        bot.id
                    )


            logs =
                SocietyBotControlApi
                    .logs(
                        uid,
                        bot.id
                    )


            if (
                liveStatus !=
                bot.status
            ) {

                onBotChanged(
                    bot.copy(
                        status =
                            liveStatus
                    )
                )
            }

        } catch (
            _: Exception
        ) {
        }
    }


    LaunchedEffect(
        bot.id,
        uid
    ) {

        while (
            true
        ) {

            refresh()

            kotlinx.coroutines.delay(
                5000
            )
        }
    }


    val normalized =
        liveStatus
            .trim()
            .lowercase()


    val statusLabel =
        when (
            normalized
        ) {

            "connected" ->
                "🟢 Conectado"

            "connecting",
            "reconnecting" ->
                "🟡 Conectando..."

            else ->
                "🔴 Não conectado"
        }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal =
                        22.dp,

                    vertical =
                        18.dp
                )
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                color =
                    colors.primary.copy(
                        alpha =
                            0.17f
                    ),

                shape =
                    RoundedCornerShape(
                        20.dp
                    ),

                modifier =
                    Modifier.size(
                        70.dp
                    )
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    SocietyEditableBotAvatar(
                            prefs = prefs,
                            botId = bot.id,
                            modifier = Modifier.fillMaxSize()
                        )
                }
            }


            Spacer(
                Modifier.width(
                    16.dp
                )
            )


            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    bot.draft.name,
                    color =
                        colors.text,
                    fontSize =
                        25.sp,
                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    Modifier.height(
                        4.dp
                    )
                )


                Text(
                    statusLabel,
                    color =
                        colors.text,
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(
                    Modifier.height(
                        4.dp
                    )
                )


                Text(
                    bot.draft.library,
                    color =
                        colors.muted,
                    fontSize =
                        13.sp
                )
            }
        }


        Spacer(
            Modifier.height(
                24.dp
            )
        )


        Text(
            "Atividade recente",
            color =
                colors.text,
            fontSize =
                20.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                10.dp
            )
        )


        Surface(
            color =
                colors.surface,

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
                        17.dp
                    )
            ) {

                if (
                    logs.isEmpty()
                ) {

                    Text(
                        "Nenhum log recente.",
                        color =
                            colors.muted,
                        fontSize =
                            13.sp
                    )

                } else {

                    logs
                        .takeLast(
                            5
                        )
                        .forEach {
                            line ->

                            Text(
                                line,
                                color =
                                    colors.muted,
                                fontSize =
                                    11.sp
                            )


                            Spacer(
                                Modifier.height(
                                    5.dp
                                )
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
            "Controle",
            color =
                colors.text,
            fontSize =
                20.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        Row(
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            Button(
                enabled =
                    !loadingAction,

                onClick = {

                    scope.launch {

                        loadingAction =
                            true

                        try {

                            SocietyBotControlApi
                                .start(
                                    uid,
                                    bot.id
                                )

                            kotlinx.coroutines.delay(
                                1200
                            )

                            refresh()

                        } finally {

                            loadingAction =
                                false
                        }
                    }
                },

                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    "▶ Iniciar"
                )
            }


            OutlinedButton(
                enabled =
                    !loadingAction,

                onClick = {

                    scope.launch {

                        loadingAction =
                            true

                        try {

                            SocietyBotControlApi
                                .restart(
                                    uid,
                                    bot.id
                                )

                            kotlinx.coroutines.delay(
                                1800
                            )

                            refresh()

                        } finally {

                            loadingAction =
                                false
                        }
                    }
                },

                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    "↻ Reiniciar"
                )
            }
        }


        Spacer(
            Modifier.height(
                8.dp
            )
        )


        OutlinedButton(
            enabled =
                !loadingAction,

            onClick = {

                scope.launch {

                    loadingAction =
                        true

                    try {

                        SocietyBotControlApi
                            .stop(
                                uid,
                                bot.id
                            )

                        kotlinx.coroutines.delay(
                            800
                        )

                        refresh()

                    } finally {

                        loadingAction =
                            false
                    }
                }
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "■ Parar"
            )
        }


        Spacer(
            Modifier.height(
                26.dp
            )
        )


        Text(
            "Configuração",
            color =
                colors.text,
            fontSize =
                20.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                10.dp
            )
        )


        SocietyManageInfo(
            colors,
            "Nome",
            bot.draft.name
        )


        SocietyManageInfo(
            colors,
            "Número do dono",
            bot.draft.owner
        )


        SocietyManageInfo(
            colors,
            "Biblioteca",
            bot.draft.library
        )


        SocietyManageInfo(
            colors,
            "ID",
            bot.id
        )


        Spacer(
            Modifier.height(
                20.dp
            )
        )


        Button(
            onClick =
                onCommands,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        58.dp
                    ),

            shape =
                RoundedCornerShape(
                    18.dp
                )
        ) {

            Text(
                "⚡ Gerenciar comandos",
                fontWeight =
                    FontWeight.Bold
            )
        }


        Spacer(
            Modifier.height(
                30.dp
            )
        )
    }
}


@Composable
private fun SocietyManageInfo(
    colors: SocietyColors,
    label: String,
    value: String
) {

    Surface(
        color =
            colors.surface,

        shape =
            RoundedCornerShape(
                17.dp
            ),

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        4.dp
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    15.dp
                )
        ) {

            Text(
                label,
                color =
                    colors.muted,
                fontSize =
                    11.sp,
                fontWeight =
                    FontWeight.SemiBold
            )


            Spacer(
                Modifier.height(
                    3.dp
                )
            )


            Text(
                value.ifBlank {
                    "Não informado"
                },
                color =
                    colors.text,
                fontSize =
                    14.sp
            )
        }
    }
}


@Composable
private fun SocietyCommandsPage(
    colors: SocietyColors,
    prefs: android.content.SharedPreferences,
    bot: SocietyBotRecord?
) {

    val scope =
        rememberCoroutineScope()

    var loading by remember(
        bot?.id
    ) {
        mutableStateOf(
            true
        )
    }

    var error by remember(
        bot?.id
    ) {
        mutableStateOf<String?>(
            null
        )
    }

    var commands by remember(
        bot?.id
    ) {
        mutableStateOf<List<SocietyRemoteCommand>>(
            emptyList()
        )
    }

    var showCreate by remember {
        mutableStateOf(
            false
        )
    }

    var commandName by remember {
        mutableStateOf(
            ""
        )
    }

    var commandDescription by remember {
        mutableStateOf(
            ""
        )
    }

    var commandResponse by remember {
        mutableStateOf(
            ""
        )
    }

    var commandType by remember {
        mutableStateOf(
            "text"
        )
    }


    var creating by remember {
        mutableStateOf(
            false
        )
    }


    val uid =
        prefs.getString(
            "auth_uid",
            ""
        ).orEmpty()


    val selectedBotId =
        bot?.id
            .orEmpty()


    val selectedBotName =
        bot?.draft
            ?.name
            .orEmpty()


    suspend fun reload() {

        if (
            selectedBotId.isBlank() ||
            uid.isBlank()
        ) {

            loading =
                false

            return
        }


        loading =
            true

        error =
            null


        try {

            commands =
                SocietyCommandsApi
                    .listCommands(
                        uid =
                            uid,

                        botId =
                            selectedBotId
                    )

        } catch (
            e: Exception
        ) {

            error =
                e.message
                    ?: "Não foi possível carregar os comandos."

        } finally {

            loading =
                false
        }
    }


    LaunchedEffect(
        bot?.id,
        uid
    ) {

        reload()
    }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal =
                        22.dp,

                    vertical =
                        18.dp
                )
    ) {

        if (
            bot ==
            null
        ) {

            Text(
                "⚡",
                fontSize =
                    48.sp
            )

            Spacer(
                Modifier.height(
                    18.dp
                )
            )

            Text(
                "Nenhum bot disponível",
                color =
                    colors.text,
                fontSize =
                    24.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(
                    8.dp
                )
            )

            Text(
                "Crie ou restaure um bot para gerenciar comandos.",
                color =
                    colors.muted
            )

            return@Column
        }


        Text(
            "⚡ Comandos",
            color =
                colors.text,
            fontSize =
                30.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                8.dp
            )
        )


        Text(
            "Gerencie os comandos do ${bot.draft.name}.",
            color =
                colors.muted,
            fontSize =
                15.sp
        )


        Spacer(
            Modifier.height(
                22.dp
            )
        )


        SocietyBotStatusCard(
            colors =
                colors,

            bot =
                bot ?: return@Column
        )


        Spacer(
            Modifier.height(
                26.dp
            )
        )


        Text(
            "Comandos disponíveis",
            color =
                colors.text,
            fontSize =
                20.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        if (
            loading
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            22.dp
                        ),

                    strokeWidth =
                        2.dp
                )

                Spacer(
                    Modifier.width(
                        12.dp
                    )
                )

                Text(
                    "Carregando comandos...",
                    color =
                        colors.muted
                )
            }

        } else if (
            error !=
            null
        ) {

            Text(
                error.orEmpty(),
                color =
                    MaterialTheme
                        .colorScheme
                        .error
            )

        } else {

            commands.forEach {
                command ->

                SocietyCommandCard(
                    colors =
                        colors,

                    command =
                        command,

                    onDelete = {

                        if (
                            command.builtin
                        ) {
                            return@SocietyCommandCard
                        }


                        scope.launch {

                            try {

                                SocietyCommandsApi
                                    .deleteCommand(
                                        uid =
                                            uid,

                                        botId =
                                            selectedBotId,

                                        commandId =
                                            command.id
                                    )

                                reload()

                            } catch (
                                e: Exception
                            ) {

                                error =
                                    e.message
                            }
                        }
                    }
                )


                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )
            }
        }


        Spacer(
            Modifier.height(
                14.dp
            )
        )


        Button(
            onClick = {

                commandName =
                    ""

                commandDescription =
                    ""

                commandResponse =
                    ""

                showCreate =
                    true
            },

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        58.dp
                    ),

            shape =
                RoundedCornerShape(
                    18.dp
                )
        ) {

            Text(
                "+ Novo comando",
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    16.sp
            )
        }


        Spacer(
            Modifier.height(
                30.dp
            )
        )
    }


    if (
        showCreate
    ) {

        AlertDialog(
            onDismissRequest = {

                if (
                    !creating
                ) {
                    showCreate =
                        false
                }
            },

            title = {

                Text(
                    "Novo comando"
                )
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    Text(
                        "Como esse comando funciona?",
                        color =
                            colors.text,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            15.sp
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        FilterChip(
                            selected =
                                commandType ==
                                    "text",

                            onClick = {
                                commandType =
                                    "text"
                            },

                            label = {
                                Text(
                                    "💬 Texto"
                                )
                            }
                        )

                        FilterChip(
                            selected =
                                commandType ==
                                    "sticker",

                            onClick = {
                                commandType =
                                    "sticker"

                                commandResponse =
                                    ""
                            },

                            label = {
                                Text(
                                    "🖼 Figurinha"
                                )
                            }
                        )
                    }

                    SocietyAiCommandAssistant(
                        colors =
                            colors,

                        prefs =
                            prefs,

                        currentType =
                            commandType,

                        onGenerated = {
                            generated ->

                            commandName =
                                generated.name
                                    .replace(
                                        "/",
                                        ""
                                    )
                                    .replace(
                                        " ",
                                        ""
                                    )
                                    .lowercase()

                            commandDescription =
                                generated.description

                            commandResponse =
                                generated.response

                            commandType =
                                if (
                                    generated.type ==
                                    "sticker"
                                )
                                    "sticker"
                                else
                                    "text"
                        }
                    )


                    OutlinedTextField(
                        value =
                            commandName,

                        onValueChange = {

                            commandName =
                                it
                                    .replace(
                                        "/",
                                        ""
                                    )
                                    .replace(
                                        " ",
                                        ""
                                    )
                                    .lowercase()
                        },

                        label = {
                            Text(
                                "Comando"
                            )
                        },

                        prefix = {
                            Text(
                                "/"
                            )
                        },

                        singleLine =
                            true,

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    OutlinedTextField(
                        value =
                            commandDescription,

                        onValueChange = {
                            commandDescription =
                                it
                        },

                        label = {
                            Text(
                                "Descrição"
                            )
                        },

                        singleLine =
                            true,

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    if (
                        commandType ==
                            "text"
                    ) {

                        OutlinedTextField(
                            value =
                                commandResponse,

                            onValueChange = {
                                commandResponse =
                                    it
                            },

                            label = {
                                Text(
                                    "Resposta do bot"
                                )
                            },

                            minLines =
                                3,

                            modifier =
                                Modifier.fillMaxWidth()
                        )

                    } else {

                        Surface(
                            color =
                                colors.surface,

                            shape =
                                RoundedCornerShape(
                                    16.dp
                                ),

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        14.dp
                                    )
                            ) {

                                Text(
                                    "🖼 Comando de figurinha",
                                    color =
                                        colors.text,
                                    fontWeight =
                                        FontWeight.Bold,
                                    fontSize =
                                        14.sp
                                )

                                Spacer(
                                    Modifier.height(
                                        5.dp
                                    )
                                )

                                Text(
                                    "O usuário pode enviar uma foto ou vídeo com o comando, ou responder uma mídia usando o comando. O Society converte automaticamente para figurinha.",
                                    color =
                                        colors.muted,
                                    fontSize =
                                        12.sp
                                )
                            }
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(
                    enabled =
                        !creating &&
                        commandName
                            .isNotBlank() &&
                        (
                            commandType !=
                                "text" ||
                            commandResponse
                                .isNotBlank()
                        ),

                    onClick = {

                        scope.launch {

                            creating =
                                true

                            error =
                                null


                            try {

                                SocietyCommandsApi
                                    .createCommand(
                                        uid =
                                            uid,

                                        botId =
                                            selectedBotId,

                                        name =
                                            commandName,

                                        description =
                                            commandDescription,

                                        response =
                                            commandResponse,

                                        type =
                                            commandType
                                    )


                                showCreate =
                                    false


                                reload()

                            } catch (
                                e: Exception
                            ) {

                                error =
                                    e.message
                                        ?: "Não foi possível criar o comando."

                            } finally {

                                creating =
                                    false
                            }
                        }
                    }
                ) {

                    Text(
                        if (
                            creating
                        )
                            "Criando..."
                        else
                            "Criar"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    enabled =
                        !creating,

                    onClick = {
                        showCreate =
                            false
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


@Composable
private fun SocietyBotStatusCard(
    colors: SocietyColors,
    bot: SocietyBotRecord
) {

    val normalized =
        bot.status
            .trim()
            .lowercase()


    val statusText =
        when (
            normalized
        ) {

            "connected" ->
                "Conectado"

            "connecting",
            "reconnecting" ->
                "Conectando..."

            else ->
                "Não conectado"
        }


    val dot =
        when (
            normalized
        ) {

            "connected" ->
                "🟢"

            "connecting",
            "reconnecting" ->
                "🟡"

            else ->
                "🔴"
        }


    Surface(
        color =
            colors.surface,

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
                            0.16f
                    ),

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                modifier =
                    Modifier.size(
                        56.dp
                    )
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "🤖",
                        fontSize =
                            26.sp
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
                    bot.draft.name,
                    color =
                        colors.text,
                    fontSize =
                        18.sp,
                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    Modifier.height(
                        3.dp
                    )
                )


                Text(
                    bot.draft.library,
                    color =
                        colors.muted,
                    fontSize =
                        14.sp
                )
            }


            Text(
                "$dot $statusText",
                color =
                    colors.text,
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}


@Composable
private fun SocietyCommandCard(
    colors: SocietyColors,
    command: SocietyRemoteCommand,
    onDelete: () -> Unit
) {

    Surface(
        color =
            colors.surface,

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
                    18.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    "⚡ /${command.name}",
                    color =
                        colors.text,
                    fontSize =
                        18.sp,
                    fontWeight =
                        FontWeight.Bold,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                if (
                    command.builtin
                ) {

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
                            "Padrão",
                            color =
                                colors.primarySoft,
                            fontSize =
                                12.sp,
                            fontWeight =
                                FontWeight.Bold,
                            modifier =
                                Modifier.padding(
                                    horizontal =
                                        10.dp,

                                    vertical =
                                        5.dp
                                )
                        )
                    }

                } else {

                    TextButton(
                        onClick =
                            onDelete
                    ) {

                        Text(
                            "Excluir"
                        )
                    }
                }
            }


            if (
                command.description
                    .isNotBlank()
            ) {

                Spacer(
                    Modifier.height(
                        6.dp
                    )
                )


                Text(
                    command.description,
                    color =
                        colors.muted,
                    fontSize =
                        14.sp
                )
            }


            if (
                !command.builtin &&
                command.response
                    .isNotBlank()
            ) {

                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )


                Text(
                    "Resposta",
                    color =
                        colors.muted,
                    fontSize =
                        12.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(
                    Modifier.height(
                        3.dp
                    )
                )


                Text(
                    command.response,
                    color =
                        colors.text,
                    fontSize =
                        14.sp
                )
            }
        }
    }
}


@Composable
private fun SocietySimplePage(
    colors: SocietyColors,
    icon: String,
    title: String,
    subtitle: String
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top =
                        55.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            icon,

            fontSize =
                52.sp
        )


        Spacer(
            Modifier.height(
                18.dp
            )
        )


        Text(
            title,

            color =
                colors.text,

            fontSize =
                24.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            Modifier.height(
                8.dp
            )
        )


        Text(
            subtitle,

            color =
                colors.secondary,

            textAlign =
                TextAlign.Center,

            lineHeight =
                22.sp
        )
    }
}


@Composable
private fun SocietySettingsCard(
    colors: SocietyColors,
    content: @Composable ColumnScope.() -> Unit
) {

    Surface(
        color =
            colors.surface,

        shape =
            RoundedCornerShape(
                24.dp
            ),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            content =
                content
        )
    }
}


@Composable
private fun SocietySettingsAction(
    colors: SocietyColors,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
                .padding(
                    17.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            icon,

            fontSize =
                20.sp,

            modifier =
                Modifier.width(
                    38.dp
                )
        )


        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                title,

                color =
                    colors.text,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                subtitle,

                color =
                    colors.secondary,

                fontSize =
                    11.sp
            )
        }


        Text(
            "›",

            color =
                colors.muted,

            fontSize =
                22.sp
        )
    }
}


@Composable
private fun SocietySwitchRow(
    colors: SocietyColors,
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier =
            Modifier.padding(
                17.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            icon,

            fontSize =
                20.sp,

            modifier =
                Modifier.width(
                    38.dp
                )
        )


        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                title,

                color =
                    colors.text,

                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                subtitle,

                color =
                    colors.secondary,

                fontSize =
                    11.sp
            )
        }


        Switch(
            checked =
                checked,

            onCheckedChange =
                onCheckedChange
        )
    }
}


@Composable
private fun SocietyProfileAction(
    colors: SocietyColors,
    icon: String,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {

    Surface(
        color =
            colors.surface,

        shape =
            RoundedCornerShape(
                21.dp
            ),

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        5.dp
                )
                .clickable {
                    onClick()
                }
    ) {

        Row(
            modifier =
                Modifier.padding(
                    17.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                icon,

                fontSize =
                    20.sp,

                modifier =
                    Modifier.width(
                        40.dp
                    )
            )


            Column {

                Text(
                    title,

                    color =
                        if (danger)
                            MaterialTheme
                                .colorScheme
                                .error
                        else
                            colors.text,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        14.sp
                )


                Text(
                    subtitle,

                    color =
                        colors.secondary,

                    fontSize =
                        11.sp
                )
            }
        }
    }
}

