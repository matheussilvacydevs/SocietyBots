'use strict';

const fs = require('fs');
const fsp = fs.promises;
const path = require('path');

const STICKER_ENGINE_SOURCE =
    require('./society-sticker-engine-template');

const RUNTIME_SOURCE = String.raw`
'use strict';

const fs = require('fs');
const fsp = fs.promises;
const path = require('path');

const STATUS_FILE =
    path.join(
        __dirname,
        '..',
        'data',
        'status.json'
    );

const COMMANDS_FILE =
    path.join(
        __dirname,
        '..',
        'data',
        'custom-commands.json'
    );

const LOG_FILE =
    path.join(
        __dirname,
        '..',
        'data',
        'society.log'
    );


async function readJson(
    file,
    fallback
) {

    try {

        return JSON.parse(
            await fsp.readFile(
                file,
                'utf8'
            )
        );

    } catch {

        return fallback;
    }
}


async function writeJson(
    file,
    value
) {

    await fsp.mkdir(
        path.dirname(file),
        {
            recursive: true
        }
    );

    const tmp =
        file +
        '.tmp-' +
        process.pid;

    await fsp.writeFile(
        tmp,
        JSON.stringify(
            value,
            null,
            2
        ) + '\n'
    );

    await fsp.rename(
        tmp,
        file
    );
}


async function log(
    message
) {

    const line =
        new Date()
            .toISOString() +
        ' ' +
        String(message) +
        '\n';

    await fsp.mkdir(
        path.dirname(LOG_FILE),
        {
            recursive: true
        }
    );

    await fsp.appendFile(
        LOG_FILE,
        line
    ).catch(
        () => {}
    );
}


async function setStatus(
    status
) {

    await writeJson(
        STATUS_FILE,
        {
            status:
                String(status),

            pid:
                process.pid,

            updatedAt:
                Date.now()
        }
    );

    await log(
        '[STATUS] ' +
        status
    );
}


async function bufferFromStream(
    stream
) {

    const chunks = [];

    for await (
        const chunk
        of stream
    ) {

        chunks.push(
            Buffer.from(
                chunk
            )
        );
    }

    return Buffer.concat(
        chunks
    );
}


function commandText(
    msg
) {

    return (
        msg.message
            ?.conversation ||

        msg.message
            ?.extendedTextMessage
            ?.text ||

        msg.message
            ?.imageMessage
            ?.caption ||

        msg.message
            ?.videoMessage
            ?.caption ||

        ''
    );
}


async function quotedImage(
    baileys,
    msg
) {

    const direct =
        msg.message
            ?.imageMessage;

    if (
        direct &&
        typeof baileys
            .downloadContentFromMessage ===
            'function'
    ) {

        const stream =
            await baileys
                .downloadContentFromMessage(
                    direct,
                    'image'
                );

        return bufferFromStream(
            stream
        );
    }


    const quoted =
        msg.message
            ?.extendedTextMessage
            ?.contextInfo
            ?.quotedMessage;

    const image =
        quoted
            ?.imageMessage;

    if (
        !image ||
        typeof baileys
            .downloadContentFromMessage !==
            'function'
    ) {

        return null;
    }


    const stream =
        await baileys
            .downloadContentFromMessage(
                image,
                'image'
            );

    return bufferFromStream(
        stream
    );
}


async function runSticker(
    ctx
) {

    const stickerEngine =
        require(
            './society-sticker-engine'
        );


    const message =
        ctx.msg.message ||
        {};


    const quoted =
        message
            ?.extendedTextMessage
            ?.contextInfo
            ?.quotedMessage;


    const directImage =
        message
            ?.imageMessage;


    const directVideo =
        message
            ?.videoMessage;


    const quotedImage =
        quoted
            ?.imageMessage;


    const quotedVideo =
        quoted
            ?.videoMessage;


    const mediaMessage =
        directImage ||
        directVideo ||
        quotedImage ||
        quotedVideo;


    const isVideo =
        Boolean(
            directVideo ||
            quotedVideo
        );


    if (
        !mediaMessage
    ) {

        await ctx.sock
            .sendMessage(
                ctx.jid,
                {
                    text:
                        '🖼️ Envie uma foto/vídeo com ' +
                        ctx.prefix +
                        ctx.command.name +
                        ' ou responda uma mídia com o comando.'
                },
                {
                    quoted:
                        ctx.msg
                }
            );


        return true;
    }


    try {

        await ctx.sock
            .sendMessage(
                ctx.jid,
                {
                    react: {
                        text:
                            '⏳',

                        key:
                            ctx.msg.key
                    }
                }
            );


        let buffer;


        if (
            typeof ctx.baileys
                .downloadMediaMessage ===
            'function'
        ) {

            const source =
                (
                    quotedImage ||
                    quotedVideo
                )
                    ? {
                        message:
                            quoted
                    }
                    : ctx.msg;


            buffer =
                await ctx.baileys
                    .downloadMediaMessage(
                        source,
                        'buffer',
                        {},
                        {
                            reuploadRequest:
                                ctx.sock
                                    .updateMediaMessage
                        }
                    );

        } else {

            const type =
                isVideo
                    ? 'video'
                    : 'image';


            const stream =
                await ctx.baileys
                    .downloadContentFromMessage(
                        mediaMessage,
                        type
                    );


            buffer =
                await bufferFromStream(
                    stream
                );
        }


        const stickerRaw =
            await stickerEngine
                .converterParaSticker(
                    buffer,
                    isVideo,
                    18
                );


        if (
            !stickerRaw
        ) {

            throw new Error(
                'STICKER_TOO_LARGE'
            );
        }


        const author =
            ctx.msg.pushName ||
            'Usuário';


        const finalSticker =
            await stickerEngine
                .addExifToWebp(
                    stickerRaw,
                    ctx.configName ||
                    'Society Bot',
                    author
                );


        await ctx.sock
            .sendMessage(
                ctx.jid,
                {
                    sticker:
                        finalSticker
                },
                {
                    quoted:
                        ctx.msg
                }
            );


        await ctx.sock
            .sendMessage(
                ctx.jid,
                {
                    react: {
                        text:
                            '✅',

                        key:
                            ctx.msg.key
                    }
                }
            );


        await log(
            '[COMMAND] ' +
            ctx.prefix +
            ctx.command.name +
            ' sticker enviado'
        );


        return true;

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][STICKER]',
            error.message
        );


        await ctx.sock
            .sendMessage(
                ctx.jid,
                {
                    react: {
                        text:
                            '❌',

                        key:
                            ctx.msg.key
                    }
                }
            )
            .catch(
                () => {}
            );


        await ctx.sock
            .sendMessage(
                ctx.jid,
                {
                    text:
                        '💥 Falha ao converter em figurinha.'
                },
                {
                    quoted:
                        ctx.msg
                }
            );


        return true;
    }
}


async function runText(
    ctx
) {

    const response =
        String(
            ctx.command.response ||
            ''
        ).trim();


    if (!response) {
        return false;
    }


    await ctx.sock
        .sendMessage(
            ctx.jid,
            {
                text:
                    response
            },
            {
                quoted:
                    ctx.msg
            }
        );


    await log(
        '[COMMAND] ' +
        ctx.prefix +
        ctx.command.name
    );


    return true;
}


async function handle({
    sock,
    msg,
    jid,
    config,
    baileys
}) {

    const text =
        commandText(
            msg
        );


    const prefix =
        config.prefix ||
        '/';


    if (
        !text.startsWith(
            prefix
        )
    ) {

        return false;
    }


    const withoutPrefix =
        text
            .slice(
                prefix.length
            )
            .trim();


    if (!withoutPrefix) {
        return false;
    }


    const parts =
        withoutPrefix
            .split(
                /\s+/
            );


    const name =
        String(
            parts.shift() ||
            ''
        ).toLowerCase();


    const commands =
        await readJson(
            COMMANDS_FILE,
            []
        );


    const command =
        commands.find(
            item =>
                item.enabled !== false &&
                String(
                    item.name ||
                    ''
                ).toLowerCase() ===
                name
        );


    if (!command) {
        return false;
    }


    const type =
        String(
            command.type ||
            command.action ||
            'text'
        ).toLowerCase();


    const ctx = {
        sock,
        msg,
        jid,
        prefix,
        command,
        args:
            parts,
        baileys,

        configName:
            config.name ||
            'Society Bot'
    };


    if (
        type ===
        'sticker' ||
        type ===
        'sticker-image'
    ) {

        return runSticker(
            ctx
        );
    }


    return runText(
        ctx
    );
}


module.exports = {
    handle,
    setStatus,
    log
};
`;


async function install(
    dir
) {

    const indexFile =
        path.join(
            dir,
            'index.js'
        );

    const runtimeFile =
        path.join(
            dir,
            'src',
            'society-runtime.js'
        );


    const stickerEngineFile =
        path.join(
            dir,
            'src',
            'society-sticker-engine.js'
        );


    await fsp.mkdir(
        path.dirname(
            runtimeFile
        ),
        {
            recursive: true
        }
    );


    await fsp.writeFile(
        runtimeFile,
        RUNTIME_SOURCE,
        'utf8'
    );


    await fsp.writeFile(
        stickerEngineFile,
        STICKER_ENGINE_SOURCE,
        'utf8'
    );


    let source =
        await fsp.readFile(
            indexFile,
            'utf8'
        );


    if (
        !source.includes(
            "require('./src/society-runtime')"
        )
    ) {

        const anchor =
            "const handler =\n  require('./src/command-handler');";


        if (
            !source.includes(
                anchor
            )
        ) {

            throw new Error(
                'RUNTIME_HANDLER_ANCHOR_NOT_FOUND'
            );
        }


        source =
            source.replace(
                anchor,
                anchor +
                "\n\nconst societyRuntime =\n" +
                "  require('./src/society-runtime');"
            );
    }


    if (
        !source.includes(
            '/* SOCIETY STATUS OPEN */'
        )
    ) {

        const anchor =
`        console.log(
          '__SOCIETY_CONNECTED__'
        );`;


        source =
            source.replace(
                anchor,
                anchor +
`
        /* SOCIETY STATUS OPEN */
        await societyRuntime
          .setStatus(
            'connected'
          );
`
            );
    }


    if (
        !source.includes(
            '/* SOCIETY STATUS CLOSE */'
        )
    ) {

        const anchor =
`        console.log(
          '__SOCIETY_DISCONNECTED__ ' +
          String(status || '')
        );`;


        source =
            source.replace(
                anchor,
                anchor +
`
        /* SOCIETY STATUS CLOSE */
        await societyRuntime
          .setStatus(
            loggedOut
              ? 'disconnected'
              : 'connecting'
          );
`
            );
    }


    if (
        !source.includes(
            '/* SOCIETY CUSTOM COMMANDS */'
        )
    ) {

        const anchor =
`      if (
        !text.startsWith(
          config.prefix
        )
      ) {
        return;
      }`;


        if (
            !source.includes(
                anchor
            )
        ) {

            throw new Error(
                'RUNTIME_COMMAND_ANCHOR_NOT_FOUND'
            );
        }


        source =
            source.replace(
                anchor,
`      /* SOCIETY CUSTOM COMMANDS */
      const handled =
        await societyRuntime
          .handle({
            sock,
            msg,
            jid,
            config,
            baileys
          });

      if (handled) {
        return;
      }


` + anchor
            );
    }


    await fsp.writeFile(
        indexFile,
        source,
        'utf8'
    );


    return true;
}


module.exports = {
    install
};
