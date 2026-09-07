'use strict';

require('dotenv').config();

const societyFirestore =
    require(
        './society-firestore'
    );


function sendJson(
    res,
    status,
    data
) {

    const body =
        JSON.stringify(
            data
        );


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
        body
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


    const token =
        raw.slice(
            7
        ).trim();


    if (!token) {

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


    return token;
}


async function firebaseUser(
    idToken
) {

    const apiKey =
        process.env.FIREBASE_API_KEY;


    if (!apiKey) {

        throw new Error(
            'FIREBASE_API_KEY_NOT_CONFIGURED'
        );
    }


    const response =
        await fetch(
            'https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=' +
                encodeURIComponent(
                    apiKey
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
        !Array.isArray(
            data.users
        ) ||
        !data.users[0]?.localId
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
            1024 *
            1024
        ) {

            throw Object.assign(
                new Error(
                    'PAYLOAD_TOO_LARGE'
                ),
                {
                    statusCode:
                        413
                }
            );
        }
    }


    if (
        !raw.trim()
    ) {
        return {};
    }


    try {

        return JSON.parse(
            raw
        );

    } catch {

        throw Object.assign(
            new Error(
                'INVALID_JSON'
            ),
            {
                statusCode:
                    400
            }
        );
    }
}


function normalizeMessages(
    input
) {

    if (
        !Array.isArray(
            input
        )
    ) {
        return [];
    }


    return input
        .slice(
            -60
        )
        .map(
            item => ({
                role:
                    item?.role ===
                    'assistant'
                        ? 'assistant'
                        : 'user',

                text:
                    String(
                        item?.text ||
                        ''
                    ).slice(
                        0,
                        6000
                    ),

                provider:
                    String(
                        item?.provider ||
                        ''
                    ).slice(
                        0,
                        100
                    ),

                time:
                    String(
                        item?.time ||
                        ''
                    ).slice(
                        0,
                        20
                    )
            })
        )
        .filter(
            item =>
                item.text
                    .trim()
        );
}


function decodeStoredChat(
    chat
) {

    let messages =
        [];


    try {

        messages =
            JSON.parse(
                String(
                    chat.messagesJson ||
                    '[]'
                )
            );


        if (
            !Array.isArray(
                messages
            )
        ) {
            messages =
                [];
        }

    } catch {

        messages =
            [];
    }


    const result = {
        ...chat,
        messages
    };


    delete result.messagesJson;


    return result;
}


module.exports =
async function societyAiChatsRouter(
    req,
    res
) {

    try {

        const url =
            new URL(
                req.url,
                'http://localhost'
            );


        const pathname =
            url.pathname;


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
            pathname ===
                '/api/society/account/ai/chats' &&
            req.method ===
                'GET'
        ) {

            let chats =
                [];


            try {

                chats =
                    await societyFirestore
                        .listAiChats(
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


            chats =
                chats
                    .map(
                        decodeStoredChat
                    )
                    .sort(
                        (a, b) =>
                            Number(
                                b.updatedAt ||
                                0
                            ) -
                            Number(
                                a.updatedAt ||
                                0
                            )
                    );


            sendJson(
                res,
                200,
                {
                    ok: true,
                    chats
                }
            );

            return;
        }


        if (
            pathname ===
                '/api/society/account/ai/chats/save' &&
            req.method ===
                'POST'
        ) {

            const body =
                await readBody(
                    req
                );


            const chatId =
                String(
                    body.id ||
                    body.chatId ||
                    ''
                ).trim();


            if (!chatId) {

                sendJson(
                    res,
                    400,
                    {
                        ok: false,
                        error:
                            'ID do chat é obrigatório.'
                    }
                );

                return;
            }


            const messages =
                normalizeMessages(
                    body.messages
                );


            const firstUser =
                messages.find(
                    item =>
                        item.role ===
                        'user'
                );


            let title =
                String(
                    body.title ||
                    ''
                )
                    .replace(
                        /\s+/g,
                        ' '
                    )
                    .trim()
                    .slice(
                        0,
                        90
                    );


            if (
                !title ||
                title ===
                    'Novo chat'
            ) {

                title =
                    String(
                        firstUser?.text ||
                        'Nova conversa'
                    )
                        .replace(
                            /\s+/g,
                            ' '
                        )
                        .trim()
                        .slice(
                            0,
                            48
                        );
            }


            const now =
                Date.now();


            /*
             * Guardamos mensagens como JSON string.
             * Isso evita problemas futuros do encoder
             * REST do Firestore com arrays/mapas aninhados.
             */
            const stored = {
                id:
                    chatId,

                title:
                    title ||
                    'Nova conversa',

                createdAt:
                    Number(
                        body.createdAt ||
                        now
                    ),

                updatedAt:
                    now,

                messagesJson:
                    JSON.stringify(
                        messages
                    )
            };


            await societyFirestore
                .saveAiChat(
                    uid,
                    chatId,
                    idToken,
                    stored
                );


            sendJson(
                res,
                200,
                {
                    ok: true,

                    chat: {
                        id:
                            chatId,

                        title:
                            stored.title,

                        createdAt:
                            stored.createdAt,

                        updatedAt:
                            stored.updatedAt,

                        messages
                    }
                }
            );

            return;
        }


        if (
            pathname ===
                '/api/society/account/ai/chats/delete' &&
            req.method ===
                'POST'
        ) {

            const body =
                await readBody(
                    req
                );


            const chatId =
                String(
                    body.id ||
                    body.chatId ||
                    ''
                ).trim();


            if (!chatId) {

                sendJson(
                    res,
                    400,
                    {
                        ok: false,
                        error:
                            'ID do chat é obrigatório.'
                    }
                );

                return;
            }


            try {

                await societyFirestore
                    .deleteAiChat(
                        uid,
                        chatId,
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


            sendJson(
                res,
                200,
                {
                    ok: true
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
            '[SOCIETY][AI CHATS]',
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
                        : 'Não foi possível sincronizar os chats.'
            }
        );
    }
};
