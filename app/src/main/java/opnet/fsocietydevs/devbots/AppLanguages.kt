package opnet.fsocietydevs.devbots

data class AppLanguage(
    val code: String,
    val label: String,
    val flag: String,
    val welcome: String,
    val subtitle: String,
    val privacy: String,
    val terms: String,
    val beforeContinue: String,
    val legalText: String,
    val continueText: String,
    val afterText: String
)

val APP_LANGUAGES = listOf(

    AppLanguage(
        "pt-BR",
        "Português (Brasil)",
        "🇧🇷",
        "Bem-vindo(a) ao Society Bots",
        "Seu bot já está quase pronto.",
        "Política de Privacidade",
        "Termos de Serviço",
        "Antes de continuar",
        "Ao continuar, você confirma que leu e concorda com os termos que regem o uso do Society Bots. O aplicativo poderá integrar serviços de terceiros e provedores de automação de acordo com os recursos escolhidos por você.",
        "Concordar e continuar",
        "Depois disso você poderá entrar ou criar sua conta."
    ),

    AppLanguage(
        "en-US",
        "English",
        "🇺🇸",
        "Welcome to Society Bots",
        "Your bot is almost ready.",
        "Privacy Policy",
        "Terms of Service",
        "Before you continue",
        "By continuing, you confirm that you have read and agree to the terms governing the use of Society Bots. The app may integrate third-party services and automation providers according to the features you choose.",
        "Agree and continue",
        "After this you can sign in or create your account."
    ),

    AppLanguage(
        "es-ES",
        "Español",
        "🇪🇸",
        "Bienvenido(a) a Society Bots",
        "Tu bot está casi listo.",
        "Política de Privacidad",
        "Términos de Servicio",
        "Antes de continuar",
        "Al continuar, confirmas que has leído y aceptas los términos que regulan el uso de Society Bots. La aplicación puede integrar servicios de terceros según los recursos elegidos.",
        "Aceptar y continuar",
        "Después podrás iniciar sesión o crear tu cuenta."
    ),

    AppLanguage(
        "fr-FR",
        "Français",
        "🇫🇷",
        "Bienvenue sur Society Bots",
        "Votre bot est presque prêt.",
        "Politique de confidentialité",
        "Conditions d'utilisation",
        "Avant de continuer",
        "En continuant, vous confirmez avoir lu et accepté les conditions qui régissent l'utilisation de Society Bots.",
        "Accepter et continuer",
        "Vous pourrez ensuite vous connecter ou créer votre compte."
    ),

    AppLanguage(
        "de-DE",
        "Deutsch",
        "🇩🇪",
        "Willkommen bei Society Bots",
        "Dein Bot ist fast fertig.",
        "Datenschutzrichtlinie",
        "Nutzungsbedingungen",
        "Bevor du fortfährst",
        "Wenn du fortfährst, bestätigst du, dass du die Bedingungen für die Nutzung von Society Bots gelesen hast und ihnen zustimmst.",
        "Zustimmen und fortfahren",
        "Danach kannst du dich anmelden oder ein Konto erstellen."
    ),

    AppLanguage(
        "it-IT",
        "Italiano",
        "🇮🇹",
        "Benvenuto su Society Bots",
        "Il tuo bot è quasi pronto.",
        "Informativa sulla privacy",
        "Termini di servizio",
        "Prima di continuare",
        "Continuando, confermi di aver letto e accettato i termini che regolano l'utilizzo di Society Bots.",
        "Accetta e continua",
        "Successivamente potrai accedere o creare il tuo account."
    ),

    AppLanguage(
        "ja-JP",
        "日本語",
        "🇯🇵",
        "Society Bots へようこそ",
        "あなたのボットはもうすぐ完成です。",
        "プライバシーポリシー",
        "利用規約",
        "続行する前に",
        "続行することで、Society Bots の利用規約を読み、同意したことを確認します。",
        "同意して続行",
        "その後、ログインまたはアカウント作成ができます。"
    ),

    AppLanguage(
        "ko-KR",
        "한국어",
        "🇰🇷",
        "Society Bots에 오신 것을 환영합니다",
        "봇이 거의 준비되었습니다.",
        "개인정보 처리방침",
        "서비스 약관",
        "계속하기 전에",
        "계속하면 Society Bots 사용 약관을 읽고 동의했음을 확인합니다.",
        "동의하고 계속",
        "그 후 로그인하거나 계정을 만들 수 있습니다."
    ),

    AppLanguage(
        "zh-CN",
        "简体中文",
        "🇨🇳",
        "欢迎使用 Society Bots",
        "您的机器人即将准备完成。",
        "隐私政策",
        "服务条款",
        "继续之前",
        "继续即表示您确认已阅读并同意 Society Bots 的使用条款。",
        "同意并继续",
        "之后您可以登录或创建账户。"
    ),

    AppLanguage(
        "ru-RU",
        "Русский",
        "🇷🇺",
        "Добро пожаловать в Society Bots",
        "Ваш бот почти готов.",
        "Политика конфиденциальности",
        "Условия использования",
        "Перед продолжением",
        "Продолжая, вы подтверждаете, что прочитали и принимаете условия использования Society Bots.",
        "Принять и продолжить",
        "После этого вы сможете войти или создать аккаунт."
    ),

    AppLanguage(
        "ar-SA",
        "العربية",
        "🇸🇦",
        "مرحبًا بك في Society Bots",
        "روبوتك جاهز تقريبًا.",
        "سياسة الخصوصية",
        "شروط الخدمة",
        "قبل المتابعة",
        "بالمتابعة، فإنك تؤكد أنك قرأت ووافقت على شروط استخدام Society Bots.",
        "موافقة ومتابعة",
        "بعد ذلك يمكنك تسجيل الدخول أو إنشاء حساب."
    ),

    AppLanguage(
        "hi-IN",
        "हिन्दी",
        "🇮🇳",
        "Society Bots में आपका स्वागत है",
        "आपका बॉट लगभग तैयार है।",
        "गोपनीयता नीति",
        "सेवा की शर्तें",
        "आगे बढ़ने से पहले",
        "जारी रखते हुए आप पुष्टि करते हैं कि आपने Society Bots की उपयोग शर्तें पढ़ ली हैं और उनसे सहमत हैं।",
        "सहमत होकर जारी रखें",
        "इसके बाद आप लॉग इन या खाता बना सकते हैं।"
    ),

    AppLanguage(
        "tr-TR",
        "Türkçe",
        "🇹🇷",
        "Society Bots'a hoş geldiniz",
        "Botunuz neredeyse hazır.",
        "Gizlilik Politikası",
        "Hizmet Şartları",
        "Devam etmeden önce",
        "Devam ederek Society Bots kullanım koşullarını okuduğunuzu ve kabul ettiğinizi onaylarsınız.",
        "Kabul et ve devam et",
        "Daha sonra giriş yapabilir veya hesap oluşturabilirsiniz."
    ),

    AppLanguage(
        "nl-NL",
        "Nederlands",
        "🇳🇱",
        "Welkom bij Society Bots",
        "Je bot is bijna klaar.",
        "Privacybeleid",
        "Servicevoorwaarden",
        "Voordat je doorgaat",
        "Door verder te gaan bevestig je dat je de voorwaarden voor Society Bots hebt gelezen en geaccepteerd.",
        "Akkoord en doorgaan",
        "Daarna kun je inloggen of een account maken."
    ),

    AppLanguage(
        "pl-PL",
        "Polski",
        "🇵🇱",
        "Witamy w Society Bots",
        "Twój bot jest prawie gotowy.",
        "Polityka prywatności",
        "Warunki korzystania",
        "Przed kontynuowaniem",
        "Kontynuując, potwierdzasz przeczytanie i zaakceptowanie warunków korzystania z Society Bots.",
        "Zaakceptuj i kontynuuj",
        "Następnie możesz się zalogować lub utworzyć konto."
    ),

    AppLanguage(
        "id-ID",
        "Bahasa Indonesia",
        "🇮🇩",
        "Selamat datang di Society Bots",
        "Bot Anda hampir siap.",
        "Kebijakan Privasi",
        "Ketentuan Layanan",
        "Sebelum melanjutkan",
        "Dengan melanjutkan, Anda mengonfirmasi bahwa Anda telah membaca dan menyetujui ketentuan Society Bots.",
        "Setuju dan lanjutkan",
        "Setelah itu Anda dapat masuk atau membuat akun."
    ),

    AppLanguage(
        "vi-VN",
        "Tiếng Việt",
        "🇻🇳",
        "Chào mừng đến với Society Bots",
        "Bot của bạn gần hoàn tất.",
        "Chính sách bảo mật",
        "Điều khoản dịch vụ",
        "Trước khi tiếp tục",
        "Bằng cách tiếp tục, bạn xác nhận rằng bạn đã đọc và đồng ý với các điều khoản của Society Bots.",
        "Đồng ý và tiếp tục",
        "Sau đó bạn có thể đăng nhập hoặc tạo tài khoản."
    ),

    AppLanguage(
        "th-TH",
        "ไทย",
        "🇹🇭",
        "ยินดีต้อนรับสู่ Society Bots",
        "บอทของคุณเกือบพร้อมแล้ว",
        "นโยบายความเป็นส่วนตัว",
        "ข้อกำหนดในการให้บริการ",
        "���่อนดำเนินการต่อ",
        "เมื่อดำเนินการต่อ คุณยืนยันว่าได้อ่านและยอมรับข้อกำหนดของ Society Bots",
        "ยอมรับและดำเนินการต่อ",
        "หลังจากนั้นคุณสามารถเข้าสู่ระบบหรือสร้างบัญชีได้"
    ),

    AppLanguage(
        "sv-SE",
        "Svenska",
        "🇸🇪",
        "Välkommen till Society Bots",
        "Din bot är nästan klar.",
        "Integritetspolicy",
        "Användarvillkor",
        "Innan du fortsätter",
        "Genom att fortsätta bekräftar du att du har läst och accepterat villkoren för Society Bots.",
        "Godkänn och fortsätt",
        "Därefter kan du logga in eller skapa ett konto."
    ),

    AppLanguage(
        "uk-UA",
        "Українська",
        "🇺🇦",
        "Ласкаво просимо до Society Bots",
        "Ваш бот майже готовий.",
        "Політика конфіденційності",
        "Умови використання",
        "Перед продовженням",
        "Продовжуючи, ви підтверджуєте, що прочитали та приймаєте умови використання Society Bots.",
        "Прийняти та продовжити",
        "Після цього ви зможете увійти або створити акаунт."
    )
)

fun languageByCode(code: String): AppLanguage {
    return APP_LANGUAGES.firstOrNull {
        it.code == code
    } ?: APP_LANGUAGES.first()
}
