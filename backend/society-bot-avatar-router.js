'use strict';

require('dotenv').config();

const fs =
    require(
        'fs'
    );

const path =
    require(
        'path'
    );

const crypto =
    require(
        'crypto'
    );

const societyFirestore =
    require(
        './society-firestore'
    );


const ROOT =
    path.join(
        __dirname,
        'data',
        'bot-media'
    );


function sendJson(
    res,
    status,
    data
) {

    res.statusCode =
        status;

    res.setHeader(
        'Content-Type',
        'application/json; charset=utf-8'
    );

    res.setHeader(
        'Cache-Control',
        'no-store'
    );

    res.end(
        JSON.stringify(
            data
        )
    );
}


function bearer(
    req
) {

    const raw =
        String(
            req.headers.authorization ||
            ''
        );


    if (
        !raw.startsWith(
            'Bearer '
        )
    ) {

        throw Object.assign(
            new Error(
                'AUTH_REQUIRED'
            ),
            {
                statusCode:
                    401
            }
        );
    }


    return raw.slice(
        7
    ).trim();
}


async function firebaseUser(
    idToken
) {

    const key =
        process.env.FIREBASE_API_KEY;


    const response =
        await fetch(
            'https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=' +
                encodeURIComponent(
                    key
                ),
            {
                method:
                    'POST',

                headers: {
                    'Content-Type':
                        'application/json'
                },

                body:
                    JSON.stringify({
                        idToken
                    })
            }
        );


    const data =
        await response.json()
            .catch(
                () => ({})
            );


    if (
        !response.ok ||
        !data.users?.[0]?.localId
    ) {

        throw Object.assign(
            new Error(
                'INVALID_SESSION'
            ),
            {
                statusCode:
                    401
            }
        );
    }


    return data.users[0];
}


async function readBody(
    req
) {

    let raw =
        '';


    for await (
        const chunk
        of req
    ) {

        raw +=
            chunk.toString(
                'utf8'
            );


        if (
            raw.length >
            2 *
            1024 *
            1024
        ) {

            throw Object.assign(
                new Error(
                    'PHOTO_TOO_LARGE'
                ),
                {
                    statusCode:
                        413
                }
            );
        }
    }


    return raw.trim()
        ? JSON.parse(
            raw
        )
        : {};
}


function botFile(
    uid,
    botId
) {

    const owner =
        crypto
            .createHash(
                'sha256'
            )
            .update(
                String(
                    uid
                )
            )
            .digest(
                'hex'
            );


    const bot =
        crypto
            .createHash(
                'sha256'
            )
            .update(
                String(
                    botId
                )
            )
            .digest(
                'hex'
            );


    const directory =
        path.join(
            ROOT,
            owner
        );


    fs.mkdirSync(
        directory,
        {
            recursive:
                true
        }
    );


    return path.join(
        directory,
        `${bot}.jpg`
    );
}


async function ownedBot(
    uid,
    botId,
    idToken
) {

    let bots =
        [];


    try {

        bots =
            await societyFirestore
                .listBots(
                    uid,
                    idToken
                );

    } catch (
        error
    ) {

        if (
            error.statusCode !==
            404
        ) {
            throw error;
        }
    }


    return bots.find(
        bot =>
            String(
                bot.id
            ) ===
            String(
                botId
            )
    );
}


module.exports =
async function societyBotAvatarRouter(
    req,
    res
) {

    try {

        const url =
            new URL(
                req.url,
                'http://localhost'
            );


        const idToken =
            bearer(
                req
            );


        const user =
            await firebaseUser(
                idToken
            );


        const uid =
            user.localId;


        if (
            req.method ===
            'GET'
        ) {

            const botId =
                String(
                    url.searchParams.get(
                        'botId'
                    ) ||
                    ''
                ).trim();


            if (!botId) {

                sendJson(
                    res,
                    400,
                    {
                        ok: false,
                        error:
                            'Bot inválido.'
                    }
                );

                return;
            }


            const bot =
                await ownedBot(
                    uid,
                    botId,
                    idToken
                );


            if (!bot) {

                sendJson(
                    res,
                    404,
                    {
                        ok: false,
                        error:
                            'Bot não encontrado.'
                    }
                );

                return;
            }


            const file =
                botFile(
                    uid,
                    botId
                );


            if (
                !fs.existsSync(
                    file
                )
            ) {

                sendJson(
                    res,
                    404,
                    {
                        ok: false,
                        error:
                            'Foto não configurada.'
                    }
                );

                return;
            }


            const bytes =
                fs.readFileSync(
                    file
                );


            res.statusCode =
                200;

            res.setHeader(
                'Content-Type',
                'image/jpeg'
            );

            res.setHeader(
                'Content-Length',
                bytes.length
            );

            res.setHeader(
                'Cache-Control',
                'no-store'
            );

            res.end(
                bytes
            );

            return;
        }


        if (
            req.method ===
            'POST'
        ) {

            const body =
                await readBody(
                    req
                );


            const botId =
                String(
                    body.botId ||
                    body.id ||
                    ''
                ).trim();


            const avatarBase64 =
                String(
                    body.avatarBase64 ||
                    ''
                ).trim();


            if (
                !botId ||
                !avatarBase64
            ) {

                sendJson(
                    res,
                    400,
                    {
                        ok: false,
                        error:
                            'Bot ou foto inválidos.'
                    }
                );

                return;
            }


            const bot =
                await ownedBot(
                    uid,
                    botId,
                    idToken
                );


            if (!bot) {

                sendJson(
                    res,
                    404,
                    {
                        ok: false,
                        error:
                            'Esse bot não pertence à sua conta.'
                    }
                );

                return;
            }


            const bytes =
                Buffer.from(
                    avatarBase64,
                    'base64'
                );


            if (
                !bytes.length ||
                bytes.length >
                900 *
                1024
            ) {

                sendJson(
                    res,
                    413,
                    {
                        ok: false,
                        error:
                            'A foto deve ter menos de 900 KB.'
                    }
                );

                return;
            }


            const file =
                botFile(
                    uid,
                    botId
                );


            fs.writeFileSync(
                file,
                bytes
            );


            /*
             * Metadata no Firestore.
             * A imagem em si fica na Cloud.
             */
            await societyFirestore
                .saveBot(
                    uid,
                    botId,
                    idToken,
                    {
                        ...bot,

                        avatarUpdatedAt:
                            Date.now()
                    }
                );


            sendJson(
                res,
                200,
                {
                    ok: true,
                    botId,
                    avatarUpdatedAt:
                        Date.now()
                }
            );

            return;
        }


        sendJson(
            res,
            405,
            {
                ok: false,
                error:
                    'Método não permitido.'
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][BOT AVATAR]',
            error.message
        );


        const status =
            Number(
                error.statusCode ||
                500
            );


        sendJson(
            res,
            status,
            {
                ok: false,

                error:
                    status ===
                    401
                        ? 'Sua sessão expirou. Entre novamente.'
                        : 'Não foi possível sincronizar a foto do bot.'
            }
        );
    }
};
