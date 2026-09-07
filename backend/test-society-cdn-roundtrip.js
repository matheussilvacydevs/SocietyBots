'use strict';

const crypto =
    require('crypto');

const cdn =
    require(
        './society-cdn'
    );


(async () => {

    try {

        const original =
            Buffer.from(
                JSON.stringify({
                    app:
                        'Society Bots',

                    type:
                        'cdn-roundtrip',

                    nonce:
                        crypto.randomUUID(),

                    timestamp:
                        Date.now()
                }),
                'utf8'
            );


        const hashOriginal =
            crypto
                .createHash(
                    'sha256'
                )
                .update(
                    original
                )
                .digest(
                    'hex'
                );


        console.log(
            '📤 Cloud -> Society CDN...'
        );


        const uploaded =
            await cdn.uploadArchive(
                original,
                `probe-${Date.now()}.json`,
                'application/json'
            );


        console.log(
            '✅ Upload HTTPS concluído.'
        );


        console.log(
            '✅ URL temporária recebida.'
        );


        console.log(
            '📥 Society CDN URL -> Cloud...'
        );


        const downloaded =
            await cdn.downloadArchive(
                uploaded
            );


        const hashDownloaded =
            crypto
                .createHash(
                    'sha256'
                )
                .update(
                    downloaded
                )
                .digest(
                    'hex'
                );


        console.log(
            'Original:',
            original.length,
            'bytes'
        );


        console.log(
            'Retorno :',
            downloaded.length,
            'bytes'
        );


        if (
            hashOriginal !==
            hashDownloaded
        ) {

            throw new Error(
                'SHA256_DIFERENTE'
            );
        }


        console.log();
        console.log(
            '=============================================='
        );

        console.log(
            '✅ SOCIETY CDN ROUND-TRIP PERFEITO'
        );

        console.log(
            '✅ Cloud -> HTTPS CDN -> URL -> Cloud'
        );

        console.log(
            '✅ SHA-256 idêntico'
        );

        console.log(
            '=============================================='
        );

    } catch (
        error
    ) {

        console.error();
        console.error(
            '=============================================='
        );

        console.error(
            '❌ SOCIETY CDN TEST FAILED'
        );

        console.error(
            '=============================================='
        );

        console.error(
            error.stack ||
            error.message
        );

        process.exit(
            1
        );
    }

})();
