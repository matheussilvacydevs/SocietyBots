'use strict';

const fs =
    require('fs');

const fsp =
    fs.promises;

const path =
    require('path');

const manager =
    require(
        './society-bot-manager'
    );


const MAX_LOG_BYTES =
    512 * 1024;


/*
 * Lê apenas o final do process.log.
 */
async function tailFile(
    file,
    maxBytes = MAX_LOG_BYTES
) {

    try {

        const stat =
            await fsp.stat(
                file
            );


        if (
            !stat.size
        ) {

            return '';
        }


        const start =
            Math.max(
                0,
                stat.size -
                    maxBytes
            );


        const length =
            stat.size -
            start;


        const handle =
            await fsp.open(
                file,
                'r'
            );


        try {

            const buffer =
                Buffer.alloc(
                    length
                );


            await handle.read(
                buffer,
                0,
                length,
                start
            );


            return buffer.toString(
                'utf8'
            );

        } finally {

            await handle.close();
        }

    } catch {

        return '';
    }
}


/*
 * Analisa os marcadores que o próprio bot
 * Baileys já escreve no process.log.
 */
function parseState(
    raw,
    running,
    pid
) {

    let lines =
        String(
            raw ||
            ''
        ).split(
            /\r?\n/
        );


    /*
     * Quando existir __SOCIETY_RUN__, eventos
     * anteriores não devem afetar a sessão atual.
     */
    let lastRun =
        -1;


    for (
        let i = 0;
        i < lines.length;
        i++
    ) {

        if (
            lines[i]
                .startsWith(
                    '__SOCIETY_RUN__ '
                )
        ) {

            lastRun =
                i;
        }
    }


    if (
        lastRun >=
        0
    ) {

        lines =
            lines.slice(
                lastRun + 1
            );
    }


    let status =
        running
            ? 'connecting'
            : 'disconnected';


    let qr =
        null;


    let pairingCode =
        null;


    let lastEvent =
        null;


    for (
        const sourceLine
        of lines
    ) {

        const line =
            String(
                sourceLine ||
                ''
            ).trim();


        if (!line) {
            continue;
        }


        if (
            line.startsWith(
                '__SOCIETY_QR__ '
            )
        ) {

            qr =
                line
                    .slice(
                        '__SOCIETY_QR__ '.length
                    )
                    .trim();


            pairingCode =
                null;


            status =
                'qr';


            lastEvent =
                'qr';


            continue;
        }


        if (
            line.startsWith(
                '__SOCIETY_PAIRING__ '
            )
        ) {

            pairingCode =
                line
                    .slice(
                        '__SOCIETY_PAIRING__ '.length
                    )
                    .trim();


            qr =
                null;


            status =
                'pairing';


            lastEvent =
                'pairing';


            continue;
        }


        if (
            line.startsWith(
                '__SOCIETY_CONNECTED__'
            )
        ) {

            status =
                'connected';


            qr =
                null;


            pairingCode =
                null;


            lastEvent =
                'connected';


            continue;
        }


        if (
            line.startsWith(
                '__SOCIETY_AUTH_RESET_REQUIRED__'
            )
        ) {

            status =
                'logged_out';


            qr =
                null;


            pairingCode =
                null;


            lastEvent =
                'logged_out';


            continue;
        }


        if (
            line.startsWith(
                '__SOCIETY_DISCONNECTED__'
            )
        ) {

            status =
                running
                    ? 'connecting'
                    : 'disconnected';


            qr =
                null;


            pairingCode =
                null;


            lastEvent =
                'disconnected';


            continue;
        }


        if (
            line.startsWith(
                '__SOCIETY_MANAGER_STOPPED__'
            )
        ) {

            status =
                'disconnected';


            qr =
                null;


            pairingCode =
                null;


            lastEvent =
                'stopped';


            continue;
        }


        if (
            line.startsWith(
                '__SOCIETY_SESSION_DELETED__'
            )
        ) {

            status =
                'disconnected';


            qr =
                null;


            pairingCode =
                null;


            lastEvent =
                'session_deleted';
        }
    }


    /*
     * Segurança visual:
     * sem processo não existe sessão online.
     */
    if (
        !running
    ) {

        status =
            'disconnected';


        qr =
            null;


        pairingCode =
            null;
    }


    return {
        status,

        connected:
            status ===
            'connected',

        running,

        pid:
            pid ||
            null,

        qr,

        pairingCode,

        needsQr:
            status ===
                'qr' ||
            status ===
                'pairing' ||
            status ===
                'logged_out',

        lastEvent
    };
}


async function state(
    uid,
    botId
) {

    const dir =
        await manager.findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    /*
     * Reutiliza o manager original apenas
     * para descobrir processo/status.
     */
    const base =
        await manager.status(
            uid,
            botId
        );


    const pid =
        base?.pid ||
        null;


    const running =
        Boolean(
            pid
        );


    const processLog =
        path.join(
            dir,
            'data',
            'process.log'
        );


    const raw =
        await tailFile(
            processLog
        );


    const result =
        parseState(
            raw,
            running,
            pid
        );


    /*
     * Bots antigos podem não ter marcadores ainda.
     * Nesse caso preservamos um connected já conhecido.
     */
    if (
        running &&
        result.status ===
            'connecting' &&
        base?.status ===
            'connected'
    ) {

        result.status =
            'connected';


        result.connected =
            true;
    }


    return result;
}


async function markRun(
    uid,
    botId
) {

    const dir =
        await manager.findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    const dataDir =
        path.join(
            dir,
            'data'
        );


    await fsp.mkdir(
        dataDir,
        {
            recursive:
                true
        }
    );


    await fsp.appendFile(
        path.join(
            dataDir,
            'process.log'
        ),
        '\n__SOCIETY_RUN__ ' +
        Date.now() +
        '\n'
    );
}


async function connect(
    uid,
    botId
) {

    /*
     * Marcador precisa ser escrito antes de iniciar
     * para separar os logs desta execução.
     */
    await markRun(
        uid,
        botId
    );


    const result =
        await manager.startBot(
            uid,
            botId
        );


    return {
        result,

        state:
            await state(
                uid,
                botId
            )
    };
}


async function disconnect(
    uid,
    botId
) {

    const result =
        await manager.stopBot(
            uid,
            botId
        );


    const dir =
        await manager.findBot(
            uid,
            botId
        );


    if (
        dir
    ) {

        await fsp.appendFile(
            path.join(
                dir,
                'data',
                'process.log'
            ),
            '\n__SOCIETY_MANAGER_STOPPED__ ' +
            Date.now() +
            '\n'
        ).catch(
            () => {}
        );
    }


    return {
        result,

        state:
            await state(
                uid,
                botId
            )
    };
}


async function restart(
    uid,
    botId
) {

    await disconnect(
        uid,
        botId
    );


    await new Promise(
        resolve =>
            setTimeout(
                resolve,
                1500
            )
    );


    return connect(
        uid,
        botId
    );
}


/*
 * Apaga SOMENTE auth/.
 * Nunca remove a pasta do bot.
 */
async function deleteSession(
    uid,
    botId
) {

    const dir =
        await manager.findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    await manager.stopBot(
        uid,
        botId
    );


    await new Promise(
        resolve =>
            setTimeout(
                resolve,
                800
            )
    );


    const userDir =
        path.dirname(
            dir
        );


    const realUserDir =
        await fsp.realpath(
            userDir
        );


    const realBotDir =
        await fsp.realpath(
            dir
        );


    if (
        path.dirname(
            realBotDir
        ) !==
        realUserDir
    ) {

        throw new Error(
            'INVALID_BOT_PATH'
        );
    }


    const authDir =
        path.join(
            realBotDir,
            'auth'
        );


    await fsp.rm(
        authDir,
        {
            recursive:
                true,

            force:
                true
        }
    );


    await fsp.mkdir(
        authDir,
        {
            recursive:
                true
        }
    );


    const dataDir =
        path.join(
            realBotDir,
            'data'
        );


    await fsp.mkdir(
        dataDir,
        {
            recursive:
                true
        }
    );


    await fsp.appendFile(
        path.join(
            dataDir,
            'process.log'
        ),
        '\n__SOCIETY_SESSION_DELETED__ ' +
        Date.now() +
        '\n'
    );


    await manager.setStatus(
        uid,
        botId,
        'disconnected'
    );


    return {
        deleted:
            true,

        status:
            'disconnected'
    };
}


async function logs(
    uid,
    botId,
    limit = 80
) {

    return manager.logs(
        uid,
        botId,
        limit
    );
}


module.exports = {
    state,
    connect,
    disconnect,
    restart,
    deleteSession,
    logs
};
