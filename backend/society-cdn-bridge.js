'use strict';

const fs =
    require('fs');

const path =
    require('path');

const crypto =
    require('crypto');


require('dotenv').config({
    path:
        path.join(
            __dirname,
            '.env'
        )
});


const STORE =
    path.join(
        __dirname,
        'data',
        'import-cdn'
    );


const MAX_BYTES =
    80 * 1024 * 1024;


const TTL_MS =
    15 * 60 * 1000;


const PUBLIC_BASE =
    'https://android-studio.cloudpaniel.com.br';


fs.mkdirSync(
    STORE,
    {
        recursive:
            true
    }
);


function sendJson(
    res,
    status,
    body
) {

    if (
        res.headersSent
    ) {
        return;
    }


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
            body
        )
    );
}


function safeEqual(
    a,
    b
) {

    const aa =
        Buffer.from(
            String(
                a || ''
            )
        );


    const bb =
        Buffer.from(
            String(
                b || ''
            )
        );


    if (
        aa.length === 0 ||
        aa.length !==
            bb.length
    ) {
        return false;
    }


    return crypto.timingSafeEqual(
        aa,
        bb
    );
}


function internalKey() {

    return String(
        process.env.SOCIETY_CDN_BRIDGE_KEY ||
        ''
    ).trim();
}


function safeFilename(
    value
) {

    let name =
        String(
            value ||
            'arquivo.bin'
        );


    try {

        name =
            decodeURIComponent(
                name
            );

    } catch {}


    name =
        path.basename(
            name
        );


    name =
        name.replace(
            /[^\p{L}\p{N}._() -]+/gu,
            '_'
        );


    return (
        name.slice(
            0,
            140
        ) ||
        'arquivo.bin'
    );
}


function filePath(
    id
) {

    return path.join(
        STORE,
        `${id}.bin`
    );
}


function metaPath(
    id
) {

    return path.join(
        STORE,
        `${id}.json`
    );
}


function validId(
    id
) {

    return /^[a-f0-9]{32}$/i.test(
        String(
            id ||
            ''
        )
    );
}


function removeEntry(
    id
) {

    try {

        fs.rmSync(
            filePath(
                id
            ),
            {
                force:
                    true
            }
        );

    } catch {}


    try {

        fs.rmSync(
            metaPath(
                id
            ),
            {
                force:
                    true
            }
        );

    } catch {}
}


function cleanupExpired() {

    let files;


    try {

        files =
            fs.readdirSync(
                STORE
            );

    } catch {

        return;
    }


    const now =
        Date.now();


    for (
        const name
        of files
    ) {

        if (
            !name.endsWith(
                '.json'
            )
        ) {
            continue;
        }


        const metaFile =
            path.join(
                STORE,
                name
            );


        try {

            const meta =
                JSON.parse(
                    fs.readFileSync(
                        metaFile,
                        'utf8'
                    )
                );


            if (
                Number(
                    meta.expiresAt ||
                    0
                ) <
                now
            ) {

                removeEntry(
                    meta.id
                );
            }

        } catch {

            try {

                fs.rmSync(
                    metaFile,
                    {
                        force:
                            true
                    }
                );

            } catch {}
        }
    }
}


async function readLimited(
    req
) {

    const chunks =
        [];


    let total =
        0;


    for await (
        const chunk
        of req
    ) {

        const buffer =
            Buffer.from(
                chunk
            );


        total +=
            buffer.length;


        if (
            total >
            MAX_BYTES
        ) {

            const error =
                new Error(
                    'Arquivo maior que 80 MB.'
                );


            error.statusCode =
                413;


            throw error;
        }


        chunks.push(
            buffer
        );
    }


    if (
        total ===
        0
    ) {

        const error =
            new Error(
                'Arquivo vazio.'
            );


        error.statusCode =
            400;


        throw error;
    }


    return Buffer.concat(
        chunks
    );
}


async function upload(
    req,
    res
) {

    try {

        cleanupExpired();


        if (
            req.method !==
            'POST'
        ) {

            sendJson(
                res,
                405,
                {
                    ok: false,
                    error:
                        'METHOD_NOT_ALLOWED'
                }
            );

            return;
        }


        const configured =
            internalKey();


        if (
            !configured
        ) {

            sendJson(
                res,
                500,
                {
                    ok: false,
                    error:
                        'SOCIETY_CDN_KEY_NOT_CONFIGURED'
                }
            );

            return;
        }


        const received =
            String(
                req.headers[
                    'x-society-cdn-key'
                ] ||
                ''
            );


        if (
            !safeEqual(
                configured,
                received
            )
        ) {

            sendJson(
                res,
                401,
                {
                    ok: false,
                    error:
                        'INVALID_CDN_KEY'
                }
            );

            return;
        }


        const buffer =
            await readLimited(
                req
            );


        const id =
            crypto
                .randomBytes(
                    16
                )
                .toString(
                    'hex'
                );


        const token =
            crypto
                .randomBytes(
                    32
                )
                .toString(
                    'hex'
                );


        const filename =
            safeFilename(
                req.headers[
                    'x-file-name'
                ]
            );


        const mimetype =
            String(
                req.headers[
                    'content-type'
                ] ||
                'application/octet-stream'
            )
                .split(
                    ';'
                )[0]
                .trim();


        const expiresAt =
            Date.now()
            + TTL_MS;


        const sha256 =
            crypto
                .createHash(
                    'sha256'
                )
                .update(
                    buffer
                )
                .digest(
                    'hex'
                );


        fs.writeFileSync(
            filePath(
                id
            ),
            buffer
        );


        fs.writeFileSync(
            metaPath(
                id
            ),
            JSON.stringify(
                {
                    id,
                    token,
                    filename,
                    mimetype,
                    size:
                        buffer.length,

                    sha256,
                    createdAt:
                        Date.now(),

                    expiresAt
                },
                null,
                2
            )
        );


        const url =
            `${PUBLIC_BASE}/api/society/cdn/file/${id}?token=${encodeURIComponent(token)}`;


        sendJson(
            res,
            200,
            {
                ok: true,
                id,
                url,
                size:
                    buffer.length,

                sha256,
                expiresAt
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][CDN BRIDGE][UPLOAD]',
            error.message
        );


        sendJson(
            res,
            Number(
                error.statusCode ||
                500
            ),
            {
                ok: false,
                error:
                    error.message ||
                    'Falha no upload.'
            }
        );
    }
}


async function download(
    req,
    res
) {

    try {

        cleanupExpired();


        if (
            req.method !==
            'GET'
        ) {

            sendJson(
                res,
                405,
                {
                    ok: false,
                    error:
                        'METHOD_NOT_ALLOWED'
                }
            );

            return;
        }


        const parsed =
            new URL(
                req.url,
                'http://localhost'
            );


        const prefix =
            '/api/society/cdn/file/';


        const id =
            decodeURIComponent(
                parsed.pathname.slice(
                    prefix.length
                )
            );


        if (
            !validId(
                id
            )
        ) {

            sendJson(
                res,
                404,
                {
                    ok: false,
                    error:
                        'NOT_FOUND'
                }
            );

            return;
        }


        const metadataPath =
            metaPath(
                id
            );


        const binaryPath =
            filePath(
                id
            );


        if (
            !fs.existsSync(
                metadataPath
            ) ||
            !fs.existsSync(
                binaryPath
            )
        ) {

            sendJson(
                res,
                404,
                {
                    ok: false,
                    error:
                        'NOT_FOUND'
                }
            );

            return;
        }


        const meta =
            JSON.parse(
                fs.readFileSync(
                    metadataPath,
                    'utf8'
                )
            );


        if (
            Number(
                meta.expiresAt ||
                0
            ) <
            Date.now()
        ) {

            removeEntry(
                id
            );


            sendJson(
                res,
                410,
                {
                    ok: false,
                    error:
                        'LINK_EXPIRED'
                }
            );

            return;
        }


        const token =
            parsed.searchParams.get(
                'token'
            );


        if (
            !safeEqual(
                meta.token,
                token
            )
        ) {

            sendJson(
                res,
                403,
                {
                    ok: false,
                    error:
                        'INVALID_DOWNLOAD_TOKEN'
                }
            );

            return;
        }


        const stat =
            fs.statSync(
                binaryPath
            );


        res.statusCode =
            200;


        res.setHeader(
            'Content-Type',
            meta.mimetype ||
                'application/octet-stream'
        );


        res.setHeader(
            'Content-Length',
            String(
                stat.size
            )
        );


        res.setHeader(
            'Content-Disposition',
            `attachment; filename="${String(
                meta.filename ||
                'arquivo.bin'
            ).replace(
                /"/g,
                ''
            )}"`
        );


        res.setHeader(
            'Cache-Control',
            'private, no-store'
        );


        res.setHeader(
            'X-Society-Sha256',
            meta.sha256
        );


        const stream =
            fs.createReadStream(
                binaryPath
            );


        stream.on(
            'error',
            error => {

                console.error(
                    '[SOCIETY][CDN BRIDGE][STREAM]',
                    error.message
                );


                if (
                    !res.headersSent
                ) {

                    sendJson(
                        res,
                        500,
                        {
                            ok: false,
                            error:
                                'STREAM_ERROR'
                        }
                    );

                } else {

                    res.destroy(
                        error
                    );
                }
            }
        );


        stream.pipe(
            res
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][CDN BRIDGE][DOWNLOAD]',
            error.message
        );


        sendJson(
            res,
            500,
            {
                ok: false,
                error:
                    'Falha ao baixar arquivo.'
            }
        );
    }
}


module.exports = {
    upload,
    download
};
