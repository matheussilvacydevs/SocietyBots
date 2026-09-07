'use strict';

const fs = require('fs');
const fsp = fs.promises;
const path = require('path');
const crypto = require('crypto');
const {
    spawn
} = require('child_process');

const runtimeInstaller =
    require('./society-runtime-installer');

const ROOT =
    '/home/server/society-bots';


function safe(
    value
) {

    return String(
        value ||
        ''
    ).replace(
        /[^a-zA-Z0-9_-]/g,
        ''
    );
}


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
    data
) {

    await fsp.mkdir(
        path.dirname(file),
        {
            recursive: true
        }
    );


    const tmp =
        `${file}.tmp-${process.pid}`;


    await fsp.writeFile(
        tmp,
        JSON.stringify(
            data,
            null,
            2
        ) + '\n'
    );


    await fsp.rename(
        tmp,
        file
    );
}


async function findBot(
    uid,
    botId
) {

    const userDir =
        path.join(
            ROOT,
            safe(uid)
        );


    const names =
        await fsp.readdir(
            userDir
        ).catch(
            () => []
        );


    const exact =
        names.find(
            name =>
                name === botId
        );


    if (exact) {

        return path.join(
            userDir,
            exact
        );
    }


    const suffix =
        String(
            botId ||
            ''
        )
            .split('_')
            .pop();


    const found =
        names.find(
            name =>
                name.endsWith(
                    '_' +
                    suffix
                )
        );


    return found
        ? path.join(
            userDir,
            found
        )
        : null;
}


async function ensureBotData(
    dir
) {

    const dataDir =
        path.join(
            dir,
            'data'
        );


    await fsp.mkdir(
        dataDir,
        {
            recursive: true
        }
    );


    const commands =
        path.join(
            dataDir,
            'custom-commands.json'
        );


    try {

        await fsp.access(
            commands
        );

    } catch {

        await writeJson(
            commands,
            []
        );
    }


    await runtimeInstaller
        .install(
            dir
        )
        .catch(
            error => {

                console.error(
                    '[SOCIETY][RUNTIME]',
                    error.message
                );
            }
        );
}


async function procPid(
    dir
) {

    const ids =
        await fsp.readdir(
            '/proc'
        ).catch(
            () => []
        );


    const target =
        path.join(
            dir,
            'index.js'
        );


    for (
        const id
        of ids
    ) {

        if (
            !/^\d+$/
                .test(id)
        ) {
            continue;
        }


        try {

            const raw =
                await fsp.readFile(
                    `/proc/${id}/cmdline`
                );


            const cmd =
                raw
                    .toString()
                    .replace(
                        /\0/g,
                        ' '
                    );


            if (
                cmd.includes(
                    target
                ) ||
                (
                    cmd.includes(
                        'node index.js'
                    ) &&
                    await cwdOfPid(
                        id
                    ) === dir
                )
            ) {

                return Number(
                    id
                );
            }

        } catch {
        }
    }


    return null;
}


async function cwdOfPid(
    pid
) {

    try {

        return await fsp.readlink(
            `/proc/${pid}/cwd`
        );

    } catch {

        return '';
    }
}


async function status(
    uid,
    botId
) {

    const dir =
        await findBot(
            uid,
            botId
        );


    if (!dir) {

        return {
            status:
                'disconnected'
        };
    }


    await ensureBotData(
        dir
    );


    const data =
        await readJson(
            path.join(
                dir,
                'data',
                'status.json'
            ),
            {}
        );


    const pid =
        await procPid(
            dir
        );


    if (!pid) {

        return {
            status:
                'disconnected',

            pid:
                null
        };
    }


    return {
        status:
            data.status ||
            'connecting',

        pid
    };
}


async function setStatus(
    uid,
    botId,
    value
) {

    const dir =
        await findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    await writeJson(
        path.join(
            dir,
            'data',
            'status.json'
        ),
        {
            status:
                value,

            updatedAt:
                Date.now()
        }
    );


    return true;
}


async function startBot(
    uid,
    botId
) {

    const dir =
        await findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    await ensureBotData(
        dir
    );


    const existing =
        await procPid(
            dir
        );


    if (existing) {

        return {
            pid:
                existing,

            alreadyRunning:
                true
        };
    }


    await setStatus(
        uid,
        botId,
        'connecting'
    );


    const logFile =
        path.join(
            dir,
            'data',
            'process.log'
        );


    const fd =
        fs.openSync(
            logFile,
            'a'
        );


    const child =
        spawn(
            process.execPath,
            [
                path.join(
                    dir,
                    'index.js'
                )
            ],
            {
                cwd:
                    dir,

                detached:
                    true,

                stdio: [
                    'ignore',
                    fd,
                    fd
                ]
            }
        );


    child.unref();

    fs.closeSync(
        fd
    );


    await writeJson(
        path.join(
            dir,
            'data',
            'process.json'
        ),
        {
            pid:
                child.pid,

            startedAt:
                Date.now()
        }
    );


    return {
        pid:
            child.pid,

        alreadyRunning:
            false
    };
}


async function stopBot(
    uid,
    botId
) {

    const dir =
        await findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    const pid =
        await procPid(
            dir
        );


    if (pid) {

        try {

            process.kill(
                pid,
                'SIGTERM'
            );

        } catch {
        }
    }


    await setStatus(
        uid,
        botId,
        'disconnected'
    );


    return {
        stopped:
            Boolean(pid)
    };
}


async function restartBot(
    uid,
    botId
) {

    await stopBot(
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


    return startBot(
        uid,
        botId
    );
}


async function logs(
    uid,
    botId,
    limit = 30
) {

    const dir =
        await findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    const files = [
        path.join(
            dir,
            'data',
            'society.log'
        ),

        path.join(
            dir,
            'data',
            'process.log'
        )
    ];


    const lines = [];


    for (
        const file
        of files
    ) {

        try {

            const raw =
                await fsp.readFile(
                    file,
                    'utf8'
                );


            lines.push(
                ...raw
                    .split(/\r?\n/)
                    .filter(Boolean)
            );

        } catch {
        }
    }


    return lines
        .slice(
            -Math.max(
                1,
                Math.min(
                    Number(limit) ||
                    30,
                    200
                )
            )
        );
}


async function listCommands(
    uid,
    botId
) {

    const dir =
        await findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    await ensureBotData(
        dir
    );


    const config =
        await readJson(
            path.join(
                dir,
                'config.json'
            ),
            {}
        );


    const custom =
        await readJson(
            path.join(
                dir,
                'data',
                'custom-commands.json'
            ),
            []
        );


    return {
        prefix:
            config.prefix ||
            '/',

        defaults: [
            {
                id:
                    'default-menu',

                name:
                    'menu',

                description:
                    'Exibe o menu principal.',

                enabled:
                    true,

                builtin:
                    true,

                type:
                    'builtin'
            },

            {
                id:
                    'default-ping',

                name:
                    'ping',

                description:
                    'Testa se o bot está online.',

                enabled:
                    true,

                builtin:
                    true,

                type:
                    'builtin'
            }
        ],

        custom
    };
}


async function createCommand(
    uid,
    botId,
    input
) {

    const dir =
        await findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    await ensureBotData(
        dir
    );


    const name =
        String(
            input.name ||
            ''
        )
            .trim()
            .toLowerCase()
            .replace(
                /^[./!#]+/,
                ''
            )
            .replace(
                /[^a-z0-9_-]/g,
                ''
            );


    const type =
        String(
            input.type ||
            'text'
        )
            .trim()
            .toLowerCase();


    const response =
        String(
            input.response ||
            ''
        ).trim();


    if (
        name.length < 1 ||
        name.length > 32
    ) {

        throw new Error(
            'INVALID_COMMAND_NAME'
        );
    }


    if (
        name === 'menu' ||
        name === 'ping'
    ) {

        throw new Error(
            'RESERVED_COMMAND'
        );
    }


    if (
        type === 'text' &&
        !response
    ) {

        throw new Error(
            'INVALID_RESPONSE'
        );
    }


    const file =
        path.join(
            dir,
            'data',
            'custom-commands.json'
        );


    const commands =
        await readJson(
            file,
            []
        );


    if (
        commands.some(
            item =>
                item.name ===
                name
        )
    ) {

        throw new Error(
            'COMMAND_EXISTS'
        );
    }


    const command = {
        id:
            crypto
                .randomBytes(8)
                .toString('hex'),

        name,

        description:
            String(
                input.description ||
                ''
            ).trim(),

        response,

        type,

        enabled:
            true,

        builtin:
            false,

        createdAt:
            Date.now()
    };


    commands.push(
        command
    );


    await writeJson(
        file,
        commands
    );


    return command;
}


async function deleteCommand(
    uid,
    botId,
    commandId
) {

    const dir =
        await findBot(
            uid,
            botId
        );


    if (!dir) {

        throw new Error(
            'BOT_NOT_FOUND'
        );
    }


    const file =
        path.join(
            dir,
            'data',
            'custom-commands.json'
        );


    const commands =
        await readJson(
            file,
            []
        );


    await writeJson(
        file,
        commands.filter(
            item =>
                item.id !==
                commandId
        )
    );


    return true;
}


module.exports = {
    findBot,
    status,
    setStatus,
    startBot,
    stopBot,
    restartBot,
    logs,
    listCommands,
    createCommand,
    deleteCommand
};
