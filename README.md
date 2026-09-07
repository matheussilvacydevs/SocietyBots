<div align="center">

# 🤖 Society Bots

### Plataforma Android para criação, gerenciamento e operação de bots

**Android • Kotlin • Jetpack Compose • Node.js • Firebase • Baileys • IA**

<br>

[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)](#)
[![Node.js](https://img.shields.io/badge/Backend-Node.js-339933?logo=node.js&logoColor=white)](#)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?logo=firebase&logoColor=black)](#)
[![Status](https://img.shields.io/badge/Status-Em%20desenvolvimento-8B2CFF)](#)

<br>

**Society Bots transforma o celular em um painel de controle para criar, administrar e operar bots hospedados na Cloud.**

</div>

---

# 📖 Sobre o projeto

O **Society Bots** é uma plataforma composta por um aplicativo Android e uma infraestrutura backend destinada à criação, gerenciamento e operação de bots.

A proposta é transformar tarefas que normalmente exigiriam terminal, SSH, edição manual de arquivos e gerenciamento direto de servidor em uma experiência visual pelo celular.

Pelo aplicativo, o usuário pode centralizar operações como:

- criação de bots;
- gerenciamento de projetos;
- autenticação de usuários;
- persistência em nuvem;
- gerenciamento de sessões do WhatsApp;
- visualização de QR Code;
- códigos de pareamento;
- logs dos processos;
- comandos dos bots;
- importação de projetos;
- comunicação com inteligência artificial;
- atualizações do aplicativo;
- gerenciamento remoto de processos executados na Cloud.

---

# 🧱 Arquitetura geral

```text
┌─────────────────────────────┐
│        Society Bots         │
│         Android App         │
│                             │
│ Kotlin + Jetpack Compose    │
└──────────────┬──────────────┘
               │
               │ HTTPS / JSON
               ▼
┌─────────────────────────────┐
│        Society API          │
│                             │
│ Node.js Backend             │
│ Firebase Auth               │
│ Firestore                   │
│ Bot Manager                 │
│ WhatsApp Manager            │
│ AI Gateway                  │
│ Import Engine               │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│         Bot Runtime         │
│                             │
│ Node.js                     │
│ Baileys                     │
│ Multi-file Auth             │
│ Process Logs                │
└─────────────────────────────┘
```

---

# ✨ Principais recursos

## 🔐 Autenticação

O aplicativo utiliza Firebase Authentication.

Recursos atuais:

- cadastro;
- login;
- logout;
- verificação de e-mail;
- renovação automática de token;
- persistência de sessão;
- identificação individual por UID;
- endpoints privados protegidos no backend.

As APIs privadas utilizam:

```http
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

O backend valida o token e determina o UID real do usuário no servidor.

---

# ☁️ Persistência em nuvem

Os dados do usuário podem ser persistidos no Firebase Firestore.

Estrutura conceitual:

```text
societyUsers/
└── UID_DO_USUARIO/
    ├── perfil
    ├── configurações
    │
    ├── bots/
    │   ├── bot-1
    │   ├── bot-2
    │   └── ...
    │
    └── aiChats/
        ├── conversa-1
        ├── conversa-2
        └── ...
```

---

# 🤖 Gerenciamento de bots

Cada bot pode possuir:

- identificador próprio;
- nome;
- projeto físico na Cloud;
- configurações;
- comandos;
- processo Node.js;
- logs;
- sessão do WhatsApp;
- dados de autenticação;
- status de execução.

---

# 💬 Central de WhatsApp

O app separa automaticamente os bots entre conectados e desconectados e acompanha estados como:

```text
connected
connecting
qr
pairing
logged_out
disconnected
```

Para bots conectados existem ações como abrir o bot, ver logs, reiniciar, desconectar e excluir somente a sessão. Para bots desconectados é possível conectar, visualizar QR, visualizar código de pareamento e remover credenciais antigas.

---

# 📱 QR Code e código de pareamento

O Android renderiza localmente o QR recebido da Cloud com ZXing:

```text
com.google.zxing:core:3.5.3
```

Fluxo:

```text
Conectar
   ↓
Backend inicia o bot
   ↓
Baileys gera QR / pairing
   ↓
Society API recebe o evento
   ↓
Android consulta o estado
   ↓
QR ou código aparece no app
```

---

# 📜 Logs e marcadores de runtime

O runtime pode emitir marcadores como:

```text
__SOCIETY_RUN__
__SOCIETY_QR__
__SOCIETY_PAIRING__
__SOCIETY_CONNECTED__
__SOCIETY_DISCONNECTED__
__SOCIETY_AUTH_RESET_REQUIRED__
__SOCIETY_SESSION_DELETED__
```

Esses eventos ajudam o backend a interpretar o estado real de cada sessão.

---

# 🧠 Inteligência Artificial

O Society Bots possui uma camada de IA no backend com suporte a chat, histórico, anexos, imagens, documentos, múltiplos provedores e fallback entre provedores.

```text
Android
   │
   ▼
Society AI API
   │
   ├── Provider A
   ├── Provider B
   └── Provider C
```

As chaves dos provedores ficam no backend, não no APK.

---

# 📦 Importação de projetos

A infraestrutura suporta importação de arquivos como:

```text
.zip
.tar
.tar.gz
.tgz
```

A extração segura inclui proteções contra path traversal, symlinks perigosos, device files, ZIP criptografado, arquivos excessivamente grandes e estruturas inválidas.

---

# 🔄 Sistema de atualização

O app consulta a versão disponível no backend:

```http
GET /api/society/app/version
```

Fluxo:

```text
Consulta versão
   ↓
Nova versão encontrada
   ↓
Download APK
   ↓
SHA-256
   ↓
FileProvider
   ↓
Instalador do Android
```

---

# 🔏 Assinatura permanente

As builds oficiais utilizam uma chave permanente de assinatura. Keystore, senhas e `signing.properties` ficam fora do repositório e são bloqueados pelo `.gitignore`.

---

# 🏗️ Stack

## Android

```text
Kotlin
Jetpack Compose
Material 3
AndroidX
Coroutines
Coil
ZXing
HttpURLConnection
FileProvider
```

Configuração atual:

```text
compileSdk: 35
targetSdk : 35
minSdk    : 26
JVM       : 17
```

## Backend

```text
Node.js
JavaScript
Firebase
Firestore
Firebase Authentication
Baileys
HTTP/JSON APIs
Process Management
File System
```

## Cloud

```text
Ubuntu / Debian
Node.js
Java
Android SDK
Gradle
CloudPanel
Reverse Proxy HTTPS
```

---

# 📁 Estrutura do projeto

```text
SocietyBots/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/opnet/fsocietydevs/devbots/
│       │   ├── MainActivity.kt
│       │   ├── SplashActivity.kt
│       │   ├── SocietyAccountApi.kt
│       │   ├── SocietyApi.kt
│       │   ├── SocietySession.kt
│       │   ├── SocietyAiApi.kt
│       │   ├── SocietyAiChatsApi.kt
│       │   ├── SocietyAiUi.kt
│       │   ├── SocietyBuilderApi.kt
│       │   ├── SocietyBuilderConsole.kt
│       │   ├── SocietyBotControlApi.kt
│       │   ├── SocietyBotImportUi.kt
│       │   ├── SocietyBotAvatar.kt
│       │   ├── SocietyCommandsApi.kt
│       │   ├── SocietyWhatsAppApi.kt
│       │   ├── SocietyWhatsAppUi.kt
│       │   ├── SocietyWhatsAppDialogs.kt
│       │   └── SocietyUpdateManager.kt
│       └── res/
├── backend/
│   ├── server.js
│   ├── society-api.js
│   ├── society-firestore.js
│   ├── society-ai.js
│   ├── society-ai-chats-router.js
│   ├── society-builder.js
│   ├── society-bot-manager.js
│   ├── society-whatsapp.js
│   ├── society-import-router.js
│   ├── society-safe-extract.py
│   ├── society-cdn.js
│   ├── society-cdn-bridge.js
│   ├── society-update-router.js
│   ├── society-app-version.json
│   └── package.json
├── gradle/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── README.md
```

---

# ⚙️ Clonando

```bash
git clone https://github.com/matheussilvacydevs/SocietyBots.git
cd SocietyBots
```

---

# 🔨 Build Android

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

APK gerado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build limpa:

```bash
./gradlew clean :app:assembleDebug
```

---

# 🖥️ Backend

```bash
cd backend
npm install
```

Configure o `.env` localmente com as variáveis necessárias. Nunca envie `.env`, keystores, tokens ou chaves privadas ao GitHub.

---

# 🌐 Principais categorias de API

```text
/api/society/account/
/api/society/account/ai/
/api/society/account/bots/
/api/society/whatsapp/
/api/society/app/
/api/society/cdn/
```

WhatsApp:

```http
GET  /api/society/whatsapp/status
GET  /api/society/whatsapp/logs
POST /api/society/whatsapp/connect
POST /api/society/whatsapp/disconnect
POST /api/society/whatsapp/restart
POST /api/society/whatsapp/delete-session
```

---

# 🛡️ Segurança

O repositório bloqueia arquivos como:

```text
.env
signing.properties
*.jks
*.keystore
*.pem
*.key
*.apk
*.aab
node_modules/
backend/data/
__pycache__/
*.pid
*.bak
*.backup
```

Endpoints privados devem derivar o UID do token autenticado, e não confiar em um UID arbitrário enviado pelo cliente.

---

# 🌿 Fluxo Git

```bash
git status
git add .
git commit -m "feat: descrição da alteração"
git push
```

Padrões sugeridos:

```text
feat: nova funcionalidade
fix: correção de bug
ui: alteração visual
backend: alteração de servidor
android: alteração do aplicativo
security: melhoria de segurança
refactor: reorganização
docs: documentação
release: nova versão
```

---

# 🚀 Histórico confirmado de versões

| VersionName | VersionCode | Estado |
|---|---:|---|
| 1.0.2 | 3 | Histórico |
| 1.0.3 | 4 | Histórico |
| 1.1.0 | 5 | Histórico |
| 1.1.1 | 6 | Histórico |
| 1.1.2 | 7 | Histórico |
| 1.1.3 | 8 | Atual |

Versão atual:

```text
VersionName: 1.1.3
VersionCode: 8
SHA-256: f46f3886732eb54c291dcf4e27a6ac1ea85f6413c069d434bfdb6f5bb187aea4
Tamanho: 18869936 bytes
```

---

# 📊 Estado atual

| Área | Estado |
|---|---|
| Android | ✅ Funcional |
| Jetpack Compose | ✅ |
| Login Firebase | ✅ |
| Verificação de e-mail | ✅ |
| Firestore | ✅ |
| Gerenciamento de bots | ✅ |
| IA | ✅ |
| Histórico de IA | ✅ |
| WhatsApp API | ✅ |
| QR Code | ✅ |
| Pairing Code | ✅ |
| Logs | ✅ |
| Reiniciar sessão | ✅ |
| Excluir sessão | ✅ |
| Updater | ✅ |
| Assinatura permanente | ✅ |
| Importação | 🧪 Beta |
| Refinamentos gerais | 🚧 Em desenvolvimento |

---

# 🗺️ Roadmap

- [ ] monitoramento de CPU e RAM por bot;
- [ ] métricas em tempo real;
- [ ] notificações de queda;
- [ ] reinício automático configurável;
- [ ] gerenciador avançado de arquivos;
- [ ] editor de código;
- [ ] terminal integrado;
- [ ] marketplace de templates;
- [ ] permissões por equipe;
- [ ] sistema de planos;
- [ ] backup automático de bots;
- [ ] restore completo;
- [ ] painel web;
- [ ] CI/CD;
- [ ] testes automatizados.

---

# 🎯 Objetivo

Transformar processos como SSH, terminal, gerenciamento de processos, logs, Baileys, Firebase, deploy e build em uma experiência simples pelo Android:

```text
Abrir aplicativo
       ↓
Escolher bot
       ↓
Executar ação
```

---

# ⚠️ Observações

O Society Bots está em desenvolvimento ativo. Integrações externas devem respeitar termos de serviço, políticas de privacidade, limites de API e legislação aplicável.

---

# 👨‍💻 Desenvolvimento

**Fsociety Devs**

GitHub: `matheussilvacydevs`

Repositório: `https://github.com/matheussilvacydevs/SocietyBots`

---

<div align="center">

# 💜 Society Bots

### Crie. Gerencie. Automatize.

**Uma infraestrutura de bots controlada diretamente pelo Android.**

</div>
