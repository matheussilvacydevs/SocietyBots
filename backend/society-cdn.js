'use strict';

const path =
    require('path');


require('dotenv').config({
    path:
        path.join(
            __dirname,
            '.env'
        )
});


const PUBLIC_BASE =
    'https://android-studio.cloudpaniel.com.br';


const UPLOAD_URL =
    `${PUBLIC_BASE}/api/society/cdn/upload`;


const MAX_BYTES =
    80 * 1024 * 1024;


function bridgeKey() {

    return String(
        process.env.SOCIETY_CDN_BRIDGE_KEY ||
        ''
    ).trim();
}


async function uploadArchive(
    buffer,
    filename,
    mimetype
) {

    const key =
        bridgeKey();


    if (
        !key
    ) {

        throw new Error(
            'SOCIETY_CDN_BRIDGE_KEY_NOT_CONFIGURED'
        );
    }


    if (
        !Buffer.isBuffer(
            buffer
        ) ||
        buffer.length ===
            0
    ) {

        throw new Error(
            'Arquivo vazio.'
        );
    }


    if (
        buffer.length >
        MAX_BYTES
    ) {

        throw new Error(
            'Arquivo maior que 80 MB.'
        );
    }


    const controller =
        new AbortController();


    const timeout =
        setTimeout(
            () =>
                controller.abort(),
            120000
        );


    try {

        const response =
            await fetch(
                UPLOAD_URL,
                {
                    method:
                        'POST',

                    headers: {
                        'Content-Type':
                            mimetype ||
                            'application/octet-stream',

                        'X-File-Name':
                            encodeURIComponent(
                                filename ||
                                'arquivo.bin'
                            ),

                        'X-Society-Cdn-Key':
                            key,

                        'Content-Length':
                            String(
                                buffer.length
                            )
                    },

                    body:
                        buffer,

                    signal:
                        controller.signal
                }
            );


        const raw =
            await response.text();


        let data;


        try {

            data =
                JSON.parse(
                    raw
                );

        } catch {

            throw new Error(
                `SOCIETY_CDN_UPLOAD_HTTP_${response.status}: ${raw.slice(0, 300)}`
            );
        }


        if (
            !response.ok ||
            data.ok ===
                false
        ) {

            throw new Error(
                data.error ||
                `SOCIETY_CDN_UPLOAD_HTTP_${response.status}`
            );
        }


        if (
            !data.url ||
            !data.id
        ) {

            throw new Error(
                'Society CDN não retornou URL/ID.'
            );
        }


        return {
            url:
                data.url,

            id:
                data.id,

            size:
                data.size,

            sha256:
                data.sha256,

            expiresAt:
                data.expiresAt
        };

    } finally {

        clearTimeout(
            timeout
        );
    }
}


async function downloadArchive(
    reference
) {

    const url =
        typeof reference ===
            'string'
            ? reference
            : reference?.url;


    if (
        !url
    ) {

        throw new Error(
            'Society CDN não forneceu URL para download.'
        );
    }


    const parsed =
        new URL(
            url
        );


    if (
        parsed.protocol !==
        'https:'
    ) {

        throw new Error(
            'URL da Society CDN não usa HTTPS.'
        );
    }


    if (
        parsed.hostname !==
        'android-studio.cloudpaniel.com.br'
    ) {

        throw new Error(
            'Host inesperado na URL da Society CDN.'
        );
    }


    const controller =
        new AbortController();


    const timeout =
        setTimeout(
            () =>
                controller.abort(),
            120000
        );


    try {

        const response =
            await fetch(
                parsed,
                {
                    signal:
                        controller.signal,

                    redirect:
                        'error'
                }
            );


        if (
            !response.ok
        ) {

            const raw =
                await response.text();


            throw new Error(
                `SOCIETY_CDN_DOWNLOAD_HTTP_${response.status}: ${raw.slice(0, 300)}`
            );
        }


        const declaredLength =
            Number(
                response.headers.get(
                    'content-length'
                ) ||
                0
            );


        if (
            declaredLength >
            MAX_BYTES
        ) {

            throw new Error(
                'Arquivo da Society CDN excede 80 MB.'
            );
        }


        const chunks =
            [];


        let total =
            0;


        for await (
            const chunk
            of response.body
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

                throw new Error(
                    'Download excedeu 80 MB.'
                );
            }


            chunks.push(
                buffer
            );
        }


        return Buffer.concat(
            chunks
        );

    } finally {

        clearTimeout(
            timeout
        );
    }
}


module.exports = {
    uploadArchive,
    downloadArchive,
    MAX_ARCHIVE_BYTES:
        MAX_BYTES
};
