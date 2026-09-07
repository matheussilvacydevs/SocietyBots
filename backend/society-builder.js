'use strict';

const fs = require('fs');
const fsp = fs.promises;
const path = require('path');
const crypto = require('crypto');
const { spawn } = require('child_process');

const ROOT =
    '/home/server/society-bots';

const sessions =
    new Map();

const processes =
    new Map();


function randomId(prefix = '') {

    return (
        prefix +
        crypto
            .randomBytes(8)
            .toString('hex')
    );
}


function normalizeWhatsAppNumber(
    value
) {

    let number =
        String(value || '')
            .replace(
                /\D/g,
                ''
            );


    /*
     * Brasil:
     * DDD + número possui normalmente 10 ou 11 dígitos.
     * Para o WhatsApp/Baileys precisamos do DDI 55.
     */
    if (
        number.length === 10 ||
        number.length === 11
    ) {

        number =
            '55' +
            number;
    }


    if (
        number.length < 12 ||
        number.length > 15
    ) {

        throw new Error(
            'INVALID_PAIRING_NUMBER'
        );
    }


    return number;
}


function safeName(value) {

    return String(value || '')
        .trim()
        .replace(
            /[^a-zA-Z0-9_-]+/g,
            '-'
        )
        .replace(
            /^-+|-+$/g,
            ''
        )
        .slice(0, 48)
        || 'bot';
}


function emit(
    session,
    type,
    message,
    extra = {}
) {

    const event = {
        id:
            session.events.length + 1,

        at:
            Date.now(),

        type,
        message,
        ...extra
    };

    session.events.push(
        event
    );

    if (
        session.events.length >
        500
    ) {
        session.events.splice(
            0,
            session.events.length -
            500
        );
    }

    return event;
}


function questionForStep(
    session
) {

    const step =
        session.step;

    const questions = {

        architecture: {
            key:
                'architecture',

            title:
                'Como você quer estruturar o bot?',

            text:
                'Escolha a arquitetura principal.',

            type:
                'choice',

            options: [
                {
                    id:
                        'switch-case',

                    label:
                        'Switch / Case'
                },
                {
                    id:
                        'command-handler',

                    label:
                        'Command Handler'
                },
                {
                    id:
                        'plugins',

                    label:
                        'Plugins'
                },
                {
                    id:
                        'modular',

                    label:
                        'Modular por pastas'
                }
            ]
        },


        library: {
            key:
                'library',

            title:
                'Qual Baileys deseja usar?',

            text:
                'Escolha a implementação de conexão com WhatsApp.',

            type:
                'choice',

            options: [
                {
                    id:
                        '@systemzero/baileys',

                    label:
                        '@systemzero/baileys'
                },
                {
                    id:
                        '@itsliaaa/baileys',

                    label:
                        '@itsliaaa/baileys'
                },
                {
                    id:
                        '@whiskeysockets/baileys',

                    label:
                        'WhiskeySockets/Baileys'
                }
            ]
        },


        auth: {
            key:
                'authMode',

            title:
                'Como deseja conectar o WhatsApp?',

            text:
                'Você pode usar QR Code ou código de pareamento.',

            type:
                'choice',

            options: [
                {
                    id:
                        'qr',

                    label:
                        'QR Code'
                },
                {
                    id:
                        'pairing',

                    label:
                        'Código de pareamento / PIN'
                }
            ]
        },


        prefix: {
            key:
                'prefix',

            title:
                'Qual prefixo o bot deverá usar?',

            text:
                'Exemplo: .menu, !menu ou /menu',

            type:
                'text',

            placeholder:
                '.'
        },


        database: {
            key:
                'database',

            title:
                'Qual armazenamento deseja usar?',

            text:
                'Você pode começar simples e trocar depois.',

            type:
                'choice',

            options: [
                {
                    id:
                        'json',

                    label:
                        'JSON local'
                },
                {
                    id:
                        'sqlite',

                    label:
                        'SQLite'
                },
                {
                    id:
                        'none',

                    label:
                        'Sem banco por enquanto'
                }
            ]
        },


        features: {
            key:
                'features',

            title:
                'Quais recursos deseja preparar?',

            text:
                'Selecione quantos quiser.',

            type:
                'multi',

            options: [
                {
                    id:
                        'menu',

                    label:
                        'Menu'
                },
                {
                    id:
                        'owner',

                    label:
                        'Comandos de dono'
                },
                {
                    id:
                        'moderation',

                    label:
                        'Moderação'
                },
                {
                    id:
                        'welcome',

                    label:
                        'Boas-vindas'
                },
                {
                    id:
                        'antilink',

                    label:
                        'Anti-link'
                },
                {
                    id:
                        'stickers',

                    label:
                        'Stickers'
                },
                {
                    id:
                        'downloads',

                    label:
                        'Downloads'
                },
                {
                    id:
                        'ai',

                    label:
                        'IA'
                }
            ]
        },


        confirm: {
            key:
                'confirm',

            title:
                'Tudo pronto para construir.',

            text:
                'Revise a configuração e confirme.',

            type:
                'confirm'
        }
    };

    return (
        questions[step] ||
        null
    );
}


function nextStep(
    session,
    current
) {

    const order = [
        'architecture',
        'library',
        'auth',
        'prefix',
        'database',
        'features',
        'confirm'
    ];


    let index =
        order.indexOf(
            current
        ) + 1;


    while (
        index <
        order.length
    ) {

        const candidate =
            order[index];


        if (
            candidate ===
            'confirm'
        ) {
            return candidate;
        }


        if (
            !Object.prototype
                .hasOwnProperty
                .call(
                    session.answers,
                    candidate
                )
        ) {

            return candidate;
        }


        index++;
    }


    return 'done';
}


function createSession({
    uid,
    name,
    owner,
    library = '',
    style = '',
    features = []
}) {

    const id =
        randomId(
            'build_'
        );

    const session = {
        id,
        uid,
        name:
            String(name || '')
                .trim()
                .slice(0, 48),

        owner:
            String(owner || '')
                .trim(),

        step:
            'architecture',

        answers: {
            ...(library
                ? {
                    library
                }
                : {}),

            ...(style
                ? {
                    style
                }
                : {}),

            ...(Array.isArray(features)
                ? {
                    features
                }
                : {})
        },

        events:
            [],

        status:
            'interview',

        botId:
            null,

        createdAt:
            Date.now()
    };

    emit(
        session,
        'assistant',
        `Olá! Vamos montar ${session.name || 'seu bot'} do jeito que você quiser.`
    );

    sessions.set(
        id,
        session
    );

    return session;
}


function getSession(id) {

    return (
        sessions.get(
            String(id || '')
        ) ||
        null
    );
}


function answerSession(
    session,
    value
) {

    if (
        !session ||
        session.status !==
        'interview'
    ) {
        throw new Error(
            'SESSION_NOT_INTERVIEW'
        );
    }

    const question =
        questionForStep(
            session
        );

    if (!question) {
        throw new Error(
            'INVALID_STEP'
        );
    }

    if (
        question.type ===
        'confirm'
    ) {

        if (
            value !== true &&
            value !== 'yes' &&
            value !== 'confirm'
        ) {
            throw new Error(
                'CONFIRM_REQUIRED'
            );
        }

        session.step =
            'done';

        session.status =
            'ready';

        emit(
            session,
            'assistant',
            'Configuração confirmada. Posso começar a construir.'
        );

        return session;
    }


    session.answers[
        question.key
    ] = value;

    emit(
        session,
        'user',
        Array.isArray(value)
            ? value.join(', ')
            : String(value)
    );

    session.step =
        nextStep(
            session,
            session.step
        );

    const next =
        questionForStep(
            session
        );

    if (next) {

        emit(
            session,
            'assistant',
            next.title,
            {
                question:
                    next
            }
        );
    }

    return session;
}


function packageNameFor(
    library
) {

    /*
     * @systemzero/baileys foi bloqueado por segurança.
     * Não instalar nem executar essa fork.
     */
    if (
        library ===
        '@systemzero/baileys'
    ) {

        const error =
            new Error(
                'BLOCKED_UNSAFE_BAILEYS'
            );

        error.userMessage =
            'Esta biblioteca foi bloqueada por segurança. Escolha @itsliaaa/baileys ou WhiskeySockets/Baileys.';

        throw error;
    }

    if (
        library ===
        '@itsliaaa/baileys'
    ) {
        return '@itsliaaa/baileys';
    }

    return '@whiskeysockets/baileys';
}


async function writeJson(
    file,
    data
) {

    await fsp.writeFile(
        file,
        JSON.stringify(
            data,
            null,
            2
        ) + '\n',
        'utf8'
    );
}


function commandHandlerSource() {

    return `
const commands = new Map();

function register(name, fn) {
  commands.set(name.toLowerCase(), fn);
}

async function run(name, ctx) {
  const command = commands.get(
    String(name || '').toLowerCase()
  );

  if (!command) return false;

  await command(ctx);
  return true;
}

module.exports = {
  register,
  run
};
`.trim() + '\n';
}


function menuCommandSource() {

    return `
module.exports = function registerMenu({
  register
}) {
  register('menu', async ({
    sock,
    jid,
    prefix
  }) => {
    await sock.sendMessage(
      jid,
      {
        text:
          '🤖 Society Bot\\n\\n' +
          prefix + 'menu\\n' +
          prefix + 'ping\\n'
      }
    );
  });
};
`.trim() + '\n';
}


function pingCommandSource() {

    return `
module.exports = function registerPing({
  register
}) {
  register('ping', async ({
    sock,
    jid
  }) => {
    await sock.sendMessage(
      jid,
      {
        text: '🏓 Pong!'
      }
    );
  });
};
`.trim() + '\n';
}


function switchCaseSource() {

    return `
async function handleCommand({
  sock,
  jid,
  command,
  prefix
}) {

  switch (
    String(command || '')
      .toLowerCase()
  ) {

    case 'ping':
      await sock.sendMessage(
        jid,
        {
          text: '🏓 Pong!'
        }
      );
      break;

    case 'menu':
      await sock.sendMessage(
        jid,
        {
          text:
            '🤖 Society Bot\\n\\n' +
            prefix + 'menu\\n' +
            prefix + 'ping'
        }
      );
      break;

    default:
      break;
  }
}

module.exports = {
  handleCommand
};
`.trim() + '\n';
}


function indexSource({
    architecture,
    authMode,
    prefix,
    owner
}) {

    const handlerImport =
        architecture ===
        'switch-case'
            ? `
const {
  handleCommand
} = require('./src/switch-handler');
`
            : `
const handler =
  require('./src/command-handler');

require('./commands/menu')(
  handler
);

require('./commands/ping')(
  handler
);
`;

    const commandRun =
        architecture ===
        'switch-case'
            ? `
      await handleCommand({
        sock,
        jid,
        command,
        prefix: config.prefix
      });
`
            : `
      await handler.run(
        command,
        {
          sock,
          jid,
          prefix:
            config.prefix
        }
      );
`;

    return `
const fs =
  require('fs');

const path =
  require('path');

const P =
  require('pino');

const config =
  require('./config.json');

const BAILEYS_PACKAGE =
  config.library;

const baileys =
  require(
    BAILEYS_PACKAGE
  );

const {
  default:
    makeWASocket,
  useMultiFileAuthState,
  DisconnectReason,
  fetchLatestBaileysVersion
} = baileys;

${handlerImport}

const AUTH_DIR =
  path.join(
    __dirname,
    'auth'
  );


async function start() {

  const {
    state,
    saveCreds
  } =
    await useMultiFileAuthState(
      AUTH_DIR
    );

  const versionResult =
    await fetchLatestBaileysVersion()
      .catch(() => ({
        version:
          undefined
      }));

  const sock =
    makeWASocket({
      auth:
        state,

      version:
        versionResult.version,

      printQRInTerminal:
        false,

      auth:
        state,

      markOnlineOnConnect:
        true,

      version:
        versionResult.version,

      browser: [
        'Mac OS',
        'Safari',
        '10.15.7'
      ],

      logger:
        P({
          level:
            'silent'
        })
    });


  /*
   * Pairing code precisa de alguns segundos para
   * o websocket do WhatsApp terminar o handshake.
   */
  if (
    config.authMode ===
      'pairing' &&
    !state.creds.registered
  ) {

    let number =
      String(
        config.owner || ''
      ).replace(
        /\D/g,
        ''
      );


    /*
     * Se o usuário informar apenas DDD + número,
     * adicionamos automaticamente o DDI brasileiro.
     *
     * Exemplos:
     * 92993855157
     *     ↓
     * 5592993855157
     */
    if (
      number.length === 10 ||
      number.length === 11
    ) {

      number =
        '55' +
        number;
    }


    /*
     * Número internacional E.164.
     * Aceitamos 12 a 15 dígitos.
     */
    if (
      number.length < 12 ||
      number.length > 15
    ) {

      console.log(
        '__SOCIETY_ERROR__ INVALID_PAIRING_NUMBER'
      );

      number = '';
    }


    if (!number) {

      console.log(
        '__SOCIETY_ERROR__ OWNER_NUMBER_REQUIRED'
      );

    } else {

      setTimeout(
        async () => {

          try {

            console.log(
              '__SOCIETY_INFO__ REQUESTING_PAIRING ' +
              '***' +
              number.slice(-4)
            );

            const code =
              await sock
                .requestPairingCode(
                  number
                );

            console.log(
              '__SOCIETY_PAIRING__ ' +
              code
            );

          } catch (
            error
          ) {

            console.log(
              '__SOCIETY_ERROR__ PAIRING ' +
              (
                error?.message ||
                String(error)
              )
            );
          }

        },
        3000
      );
    }
  }


  sock.ev.on(
    'creds.update',
    saveCreds
  );


  sock.ev.on(
    'connection.update',
    async update => {

      const {
        connection,
        lastDisconnect,
        qr
      } =
        update;


      if (
        qr &&
        config.authMode ===
          'qr'
      ) {

        console.log(
          '__SOCIETY_QR__ ' +
          qr
        );
      }


      if (
        connection ===
        'open'
      ) {

        console.log(
          '__SOCIETY_CONNECTED__'
        );
      }


      if (
        connection ===
        'close'
      ) {

        const status =
          lastDisconnect
            ?.error
            ?.output
            ?.statusCode;

        const loggedOut =
          status ===
            DisconnectReason
              .loggedOut ||
          status ===
            401;

        console.log(
          '__SOCIETY_DISCONNECTED__ ' +
          String(status || '')
        );


        if (loggedOut) {

          console.log(
            '__SOCIETY_AUTH_RESET_REQUIRED__'
          );

          return;
        }


        setTimeout(
          start,
          3000
        );
      }
    }
  );


  sock.ev.on(
    'messages.upsert',
    async event => {

      const msg =
        event.messages?.[0];

      if (
        !msg ||
        !msg.message
      ) {
        return;
      }

      const jid =
        msg.key.remoteJid;

      const text =
        msg.message
          ?.conversation ||
        msg.message
          ?.extendedTextMessage
          ?.text ||
        '';

      if (
        !text.startsWith(
          config.prefix
        )
      ) {
        return;
      }

      const command =
        text
          .slice(
            config.prefix.length
          )
          .trim()
          .split(/\\s+/)[0];

${commandRun}
    }
  );
}


start()
  .catch(
    error => {

      console.error(
        '__SOCIETY_FATAL__',
        error
      );

      process.exit(1);
    }
  );
`.trim() + '\n';
}


async function buildFiles(
    session
) {

    const botId =
        randomId(
            safeName(
                session.name
            ) + '_'
        );

    session.botId =
        botId;

    const dir =
        path.join(
            ROOT,
            safeName(
                session.uid
            ),
            botId
        );

    await fsp.mkdir(
        dir,
        {
            recursive:
                true
        }
    );

    await fsp.mkdir(
        path.join(
            dir,
            'src'
        ),
        {
            recursive:
                true
        }
    );

    await fsp.mkdir(
        path.join(
            dir,
            'commands'
        ),
        {
            recursive:
                true
        }
    );

    await fsp.mkdir(
        path.join(
            dir,
            'auth'
        ),
        {
            recursive:
                true
        }
    );


    const library =
        packageNameFor(
            session.answers.library
        );

    const packageJson = {
        name:
            safeName(
                session.name
            ).toLowerCase(),

        version:
            '1.0.0',

        private:
            true,

        main:
            'index.js',

        scripts: {
            start:
                'node index.js'
        },

        dependencies: {
            [library]:
                'latest',

            pino:
                '^9.0.0'
        }
    };


    if (
        session.answers.database ===
        'sqlite'
    ) {
        packageJson.dependencies[
            'better-sqlite3'
        ] =
            '^11.0.0';
    }


    await writeJson(
        path.join(
            dir,
            'package.json'
        ),
        packageJson
    );


    await writeJson(
        path.join(
            dir,
            'config.json'
        ),
        {
            name:
                session.name,

            owner:
                session.owner,

            architecture:
                session.answers
                    .architecture,

            library,

            authMode:
                session.answers
                    .authMode,

            prefix:
                String(
                    session.answers
                        .prefix ||
                    '.'
                ).slice(
                    0,
                    4
                ),

            database:
                session.answers
                    .database,

            features:
                session.answers
                    .features ||
                []
        }
    );


    await fsp.writeFile(
        path.join(
            dir,
            'index.js'
        ),
        indexSource({
            architecture:
                session.answers
                    .architecture,

            authMode:
                session.answers
                    .authMode,

            prefix:
                session.answers
                    .prefix,

            owner:
                session.owner
        }),
        'utf8'
    );


    if (
        session.answers
            .architecture ===
        'switch-case'
    ) {

        await fsp.writeFile(
            path.join(
                dir,
                'src',
                'switch-handler.js'
            ),
            switchCaseSource(),
            'utf8'
        );

    } else {

        await fsp.writeFile(
            path.join(
                dir,
                'src',
                'command-handler.js'
            ),
            commandHandlerSource(),
            'utf8'
        );

        await fsp.writeFile(
            path.join(
                dir,
                'commands',
                'menu.js'
            ),
            menuCommandSource(),
            'utf8'
        );

        await fsp.writeFile(
            path.join(
                dir,
                'commands',
                'ping.js'
            ),
            pingCommandSource(),
            'utf8'
        );
    }


    return {
        botId,
        dir
    };
}


function runCommand(
    session,
    command,
    args,
    cwd
) {

    return new Promise(
        (resolve, reject) => {

            emit(
                session,
                'console',
                `$ ${command} ${args.join(' ')}`
            );

            const child =
                spawn(
                    command,
                    args,
                    {
                        cwd,
                        env:
                            process.env
                    }
                );

            child.stdout.on(
                'data',
                chunk => {

                    const text =
                        chunk.toString();

                    for (
                        const line
                        of text.split(
                            /\r?\n/
                        )
                    ) {

                        if (
                            line.trim()
                        ) {
                            emit(
                                session,
                                'console',
                                line
                            );
                        }
                    }
                }
            );


            child.stderr.on(
                'data',
                chunk => {

                    const text =
                        chunk.toString();

                    for (
                        const line
                        of text.split(
                            /\r?\n/
                        )
                    ) {

                        if (
                            line.trim()
                        ) {
                            emit(
                                session,
                                'console',
                                line
                            );
                        }
                    }
                }
            );


            child.on(
                'error',
                reject
            );


            child.on(
                'exit',
                code => {

                    if (code === 0) {

                        resolve();

                    } else {

                        reject(
                            new Error(
                                `${command} saiu com código ${code}`
                            )
                        );
                    }
                }
            );
        }
    );
}


async function startBotProcess(
    session,
    dir
) {

    const existing =
        processes.get(
            session.botId
        );

    if (existing) {

        try {
            existing.kill();
        } catch {}
    }


    const child =
        spawn(
            'node',
            [
                'index.js'
            ],
            {
                cwd:
                    dir,

                env:
                    process.env
            }
        );


    processes.set(
        session.botId,
        child
    );


    session.status =
        'running';


    function consume(
        chunk,
        stream
    ) {

        const text =
            chunk.toString();

        for (
            const line
            of text.split(
                /\r?\n/
            )
        ) {

            if (
                !line.trim()
            ) {
                continue;
            }


            if (
                line.startsWith(
                    '__SOCIETY_QR__ '
                )
            ) {

                emit(
                    session,
                    'qr',
                    'QR recebido.',
                    {
                        qr:
                            line.slice(
                                15
                            )
                    }
                );

                continue;
            }


            if (
                line.startsWith(
                    '__SOCIETY_PAIRING__ '
                )
            ) {

                emit(
                    session,
                    'pairing',
                    'Código de pareamento recebido.',
                    {
                        code:
                            line.slice(
                                20
                            ).trim()
                    }
                );

                continue;
            }


            if (
                line.startsWith(
                    '__SOCIETY_CONNECTED__'
                )
            ) {

                session.status =
                    'connected';

                emit(
                    session,
                    'connected',
                    'WhatsApp conectado com sucesso.'
                );

                continue;
            }


            emit(
                session,
                stream,
                line
            );
        }
    }


    child.stdout.on(
        'data',
        chunk =>
            consume(
                chunk,
                'console'
            )
    );


    child.stderr.on(
        'data',
        chunk =>
            consume(
                chunk,
                'error'
            )
    );


    child.on(
        'exit',
        code => {

            processes.delete(
                session.botId
            );

            if (
                session.status !==
                'connected'
            ) {
                session.status =
                    'stopped';
            }

            emit(
                session,
                'system',
                `Processo encerrado (${code}).`
            );
        }
    );
}


async function build(
    session
) {

    if (
        session.status !==
        'ready'
    ) {
        throw new Error(
            'SESSION_NOT_READY'
        );
    }


    session.status =
        'building';

    emit(
        session,
        'assistant',
        'Criando estrutura do projeto...'
    );


    const {
        dir
    } =
        await buildFiles(
            session
        );


    emit(
        session,
        'assistant',
        'Estrutura criada.'
    );


    emit(
        session,
        'assistant',
        'Instalando dependências...'
    );


    await runCommand(
        session,
        'npm',
        [
            'install',
            '--no-audit',
            '--no-fund'
        ],
        dir
    );


    emit(
        session,
        'assistant',
        'Dependências instaladas.'
    );


    emit(
        session,
        'assistant',
        'Iniciando o bot...'
    );


    await startBotProcess(
        session,
        dir
    );


    return session;
}


function eventsSince(
    session,
    after = 0
) {

    return session.events
        .filter(
            event =>
                event.id >
                Number(after || 0)
        );
}


function sessionView(
    session
) {

    return {
        id:
            session.id,

        name:
            session.name,

        owner:
            session.owner,

        step:
            session.step,

        status:
            session.status,

        botId:
            session.botId,

        answers:
            session.answers,

        question:
            questionForStep(
                session
            )
    };
}


module.exports = {
    createSession,
    getSession,
    answerSession,
    build,
    eventsSince,
    sessionView
};
