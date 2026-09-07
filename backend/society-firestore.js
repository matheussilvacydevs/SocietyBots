'use strict';

const PROJECT_ID =
    process.env.FIREBASE_PROJECT_ID ||
    'cobrancasmart-8b868';

const BASE =
    `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents`;


/* ============================================================
 * FIRESTORE VALUE
 * ============================================================ */

function encodeValue(value) {

    if (value === null) {
        return {
            nullValue: null
        };
    }

    if (typeof value === 'boolean') {
        return {
            booleanValue: value
        };
    }

    if (
        typeof value === 'number' &&
        Number.isInteger(value)
    ) {
        return {
            integerValue:
                String(value)
        };
    }

    if (typeof value === 'number') {
        return {
            doubleValue:
                value
        };
    }

    if (Array.isArray(value)) {

        return {
            arrayValue: {
                values:
                    value.map(
                        encodeValue
                    )
            }
        };
    }

    if (
        typeof value === 'object' &&
        value !== null
    ) {

        return {
            mapValue: {
                fields:
                    encodeFields(
                        value
                    )
            }
        };
    }

    return {
        stringValue:
            String(
                value ?? ''
            )
    };
}


function encodeFields(object) {

    const fields = {};

    for (
        const [key, value]
        of Object.entries(
            object || {}
        )
    ) {

        if (value === undefined) {
            continue;
        }

        fields[key] =
            encodeValue(
                value
            );
    }

    return fields;
}


/* ============================================================
 * FIRESTORE → JS
 * ============================================================ */

function decodeValue(value) {

    if (!value) {
        return null;
    }

    if (
        Object.prototype
            .hasOwnProperty
            .call(
                value,
                'stringValue'
            )
    ) {

        return value.stringValue;
    }

    if (
        Object.prototype
            .hasOwnProperty
            .call(
                value,
                'integerValue'
            )
    ) {

        return Number(
            value.integerValue
        );
    }

    if (
        Object.prototype
            .hasOwnProperty
            .call(
                value,
                'doubleValue'
            )
    ) {

        return Number(
            value.doubleValue
        );
    }

    if (
        Object.prototype
            .hasOwnProperty
            .call(
                value,
                'booleanValue'
            )
    ) {

        return Boolean(
            value.booleanValue
        );
    }

    if (
        Object.prototype
            .hasOwnProperty
            .call(
                value,
                'nullValue'
            )
    ) {

        return null;
    }

    if (value.arrayValue) {

        return (
            value.arrayValue
                .values ||
            []
        ).map(
            decodeValue
        );
    }

    if (value.mapValue) {

        return decodeFields(
            value.mapValue
                .fields ||
            {}
        );
    }

    return null;
}


function decodeFields(fields) {

    const output = {};

    for (
        const [key, value]
        of Object.entries(
            fields || {}
        )
    ) {

        output[key] =
            decodeValue(
                value
            );
    }

    return output;
}


/* ============================================================
 * REQUEST
 * ============================================================ */

async function request(
    path,
    {
        method = 'GET',
        idToken,
        body = null
    } = {}
) {

    if (!idToken) {

        throw new Error(
            'AUTH_REQUIRED'
        );
    }


    const response =
        await fetch(
            `${BASE}/${path}`,
            {
                method,

                headers: {
                    Authorization:
                        `Bearer ${idToken}`,

                    'Content-Type':
                        'application/json'
                },

                body:
                    body === null
                        ? undefined
                        : JSON.stringify(
                            body
                        )
            }
        );


    const raw =
        await response.text();


    let data = {};

    try {

        data =
            raw
                ? JSON.parse(raw)
                : {};

    } catch {

        data = {};
    }


    if (!response.ok) {

        const error =
            new Error(
                data?.error?.message ||
                `FIRESTORE_HTTP_${response.status}`
            );

        error.statusCode =
            response.status;

        error.firestore =
            data;

        throw error;
    }


    return data;
}


/* ============================================================
 * PROFILE
 * ============================================================ */

async function getProfile(
    uid,
    idToken
) {

    try {

        const doc =
            await request(
                `societyUsers/${encodeURIComponent(uid)}`,
                {
                    idToken
                }
            );


        return decodeFields(
            doc.fields ||
            {}
        );

    } catch (
        error
    ) {

        if (
            error.statusCode ===
            404
        ) {

            return null;
        }

        throw error;
    }
}


async function saveProfile(
    uid,
    idToken,
    profile
) {

    const url =
        `societyUsers/${encodeURIComponent(uid)}`;


    return request(
        url,
        {
            method:
                'PATCH',

            idToken,

            body: {
                fields:
                    encodeFields(
                        profile
                    )
            }
        }
    );
}


/* ============================================================
 * BOTS
 * ============================================================ */

async function listBots(
    uid,
    idToken
) {

    const data =
        await request(
            `societyUsers/${encodeURIComponent(uid)}/bots`,
            {
                idToken
            }
        );


    return (
        data.documents ||
        []
    ).map(
        document => {

            const fields =
                decodeFields(
                    document.fields ||
                    {}
                );


            return {
                id:
                    document.name
                        .split('/')
                        .pop(),

                ...fields
            };
        }
    );
}


async function saveBot(
    uid,
    botId,
    idToken,
    bot
) {

    return request(
        `societyUsers/${encodeURIComponent(uid)}/bots/${encodeURIComponent(botId)}`,
        {
            method:
                'PATCH',

            idToken,

            body: {
                fields:
                    encodeFields(
                        bot
                    )
            }
        }
    );
}


async function deleteBot(
    uid,
    botId,
    idToken
) {

    return request(
        `societyUsers/${encodeURIComponent(uid)}/bots/${encodeURIComponent(botId)}`,
        {
            method:
                'DELETE',

            idToken
        }
    );
}



/* ============================================================
 * SOCIETY AI - CHATS
 * ============================================================ */

async function listAiChats(
    uid,
    idToken
) {

    const data =
        await request(
            `societyUsers/${encodeURIComponent(uid)}/aiChats`,
            {
                idToken
            }
        );


    return (
        data.documents ||
        []
    )
        .map(
            document => {

                const fields =
                    decodeFields(
                        document.fields ||
                        {}
                    );


                return {
                    id:
                        document.name
                            .split('/')
                            .pop(),

                    ...fields
                };
            }
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
}


async function saveAiChat(
    uid,
    chatId,
    idToken,
    chat
) {

    return request(
        `societyUsers/${encodeURIComponent(uid)}/aiChats/${encodeURIComponent(chatId)}`,
        {
            method:
                'PATCH',

            idToken,

            body: {
                fields:
                    encodeFields(
                        chat
                    )
            }
        }
    );
}


async function deleteAiChat(
    uid,
    chatId,
    idToken
) {

    return request(
        `societyUsers/${encodeURIComponent(uid)}/aiChats/${encodeURIComponent(chatId)}`,
        {
            method:
                'DELETE',

            idToken
        }
    );
}

module.exports = {
    getProfile,
    saveProfile,
    listBots,
    saveBot,
    deleteBot,
    listAiChats,
    saveAiChat,
    deleteAiChat
};
