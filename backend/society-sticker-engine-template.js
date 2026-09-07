'use strict';

module.exports = String.raw`
'use strict';

const {
    execFile
} = require('child_process');

const fs =
    require('fs');

const fsp =
    fs.promises;

const path =
    require('path');

const webp =
    require('node-webpmux');


const STICKER_MAX_BYTES =
    480 * 1024;

const FFMPEG_TIMEOUT_MS =
    25000;

const TMP_DIR =
    path.join(
        __dirname,
        '..',
        'tmp_stickers'
    );


if (
    !fs.existsSync(
        TMP_DIR
    )
) {

    fs.mkdirSync(
        TMP_DIR,
        {
            recursive: true
        }
    );
}


function detectarExtensaoBuffer(
    buffer
) {

    if (
        !buffer ||
        buffer.length < 12
    ) {
        return '';
    }


    if (
        buffer.toString(
            'ascii',
            0,
            4
        ) === 'RIFF' &&
        buffer.toString(
            'ascii',
            8,
            12
        ) === 'WEBP'
    ) {
        return '.webp';
    }


    if (
        buffer.toString(
            'ascii',
            4,
            8
        ) === 'ftyp'
    ) {
        return '.mp4';
    }


    if (
        buffer.toString(
            'ascii',
            0,
            3
        ) === 'GIF'
    ) {
        return '.gif';
    }


    return '';
}


function rodarFFmpeg(
    inputPath,
    outputPath,
    isVideo,
    maxSeconds,
    qualidade,
    fps = 12
) {

    return new Promise(
        (
            resolve,
            reject
        ) => {

            const filtro =
                'scale=512:512:' +
                'force_original_aspect_ratio=increase,' +
                'crop=512:512:' +
                '(in_w-512)/2:' +
                '(in_h-512)*2/10';


            const args =
                isVideo
                    ? [
                        '-i',
                        inputPath,

                        '-t',
                        String(
                            maxSeconds
                        ),

                        '-vcodec',
                        'libwebp',

                        '-vf',
                        'fps=' +
                            String(fps) +
                            ',' +
                            filtro,

                        '-loop',
                        '0',

                        '-preset',
                        'default',

                        '-an',

                        '-fps_mode',
                        'passthrough',

                        '-q:v',
                        String(
                            qualidade
                        ),

                        '-compression_level',
                        '6',

                        outputPath,

                        '-y'
                    ]
                    : [
                        '-i',
                        inputPath,

                        '-vcodec',
                        'libwebp',

                        '-vf',
                        filtro,

                        '-q:v',
                        String(
                            qualidade
                        ),

                        '-compression_level',
                        '6',

                        outputPath,

                        '-y'
                    ];


            execFile(
                'ffmpeg',
                args,
                {
                    timeout:
                        FFMPEG_TIMEOUT_MS
                },
                error => {

                    if (
                        error
                    ) {
                        reject(
                            error
                        );
                        return;
                    }


                    resolve();
                }
            );
        }
    );
}


async function converterParaSticker(
    buffer,
    isVideo,
    maxSeconds = 8,
    fps = 12
) {

    const time =
        Date.now() +
        '_' +
        Math.floor(
            Math.random() *
            100000
        );


    const extensao =
        detectarExtensaoBuffer(
            buffer
        );


    const inputPath =
        path.join(
            TMP_DIR,
            'in_' +
                time +
                extensao
        );


    await fsp.writeFile(
        inputPath,
        buffer
    );


    const arquivosGerados =
        [];


    const outputPath =
        tag =>
            path.join(
                TMP_DIR,
                'out_' +
                    time +
                    '_' +
                    tag +
                    '.webp'
            );


    const limpar =
        async () => {

            await fsp.unlink(
                inputPath
            ).catch(
                () => {}
            );


            for (
                const file
                of arquivosGerados
            ) {

                await fsp.unlink(
                    file
                ).catch(
                    () => {}
                );
            }
        };


    try {

        let qMin =
            1;

        let qMax =
            isVideo
                ? 60
                : 90;

        let melhorBuffer =
            null;

        let melhorQualidade =
            null;


        while (
            qMax -
            qMin >
            3
        ) {

            const qTeste =
                Math.round(
                    (
                        qMin +
                        qMax
                    ) /
                    2
                );


            const out =
                outputPath(
                    qTeste
                );


            arquivosGerados.push(
                out
            );


            await rodarFFmpeg(
                inputPath,
                out,
                isVideo,
                maxSeconds,
                qTeste,
                fps
            );


            const tamanho =
                (
                    await fsp.stat(
                        out
                    )
                ).size;


            if (
                tamanho <=
                STICKER_MAX_BYTES
            ) {

                if (
                    melhorQualidade ===
                        null ||
                    qTeste >
                        melhorQualidade
                ) {

                    melhorBuffer =
                        await fsp.readFile(
                            out
                        );

                    melhorQualidade =
                        qTeste;
                }


                qMin =
                    qTeste;

            } else {

                qMax =
                    qTeste;
            }
        }


        if (
            !melhorBuffer
        ) {

            const out =
                outputPath(
                    'piso'
                );


            arquivosGerados.push(
                out
            );


            await rodarFFmpeg(
                inputPath,
                out,
                isVideo,
                maxSeconds,
                qMin,
                fps
            );


            const tamanho =
                (
                    await fsp.stat(
                        out
                    )
                ).size;


            if (
                tamanho <=
                STICKER_MAX_BYTES
            ) {

                melhorBuffer =
                    await fsp.readFile(
                        out
                    );
            }
        }


        await limpar();

        return melhorBuffer;

    } catch (
        error
    ) {

        await limpar();

        throw error;
    }
}


function construirDadosExif(
    packname,
    author
) {

    const jsonStr =
        JSON.stringify({
            'sticker-pack-id':
                'society-bots-v1',

            'sticker-pack-name':
                packname ||
                'Society Bots',

            'sticker-pack-publisher':
                author ||
                'Society Bots',

            emojis: [
                '✨'
            ]
        });


    const exifHeader =
        Buffer.from([
            0x49, 0x49, 0x2A, 0x00,
            0x08, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x41, 0x57,
            0x07, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x16, 0x00,
            0x00, 0x00
        ]);


    const jsonBuffer =
        Buffer.from(
            jsonStr,
            'utf-8'
        );


    const exifObj =
        Buffer.alloc(
            exifHeader.length +
            jsonBuffer.length
        );


    exifHeader.copy(
        exifObj,
        0
    );


    exifObj.writeUInt32LE(
        jsonBuffer.length,
        14
    );


    jsonBuffer.copy(
        exifObj,
        exifHeader.length
    );


    return exifObj;
}


async function addExifToWebp(
    webpBuffer,
    packname,
    author
) {

    if (
        !webpBuffer
    ) {

        return null;
    }


    try {

        const img =
            new webp.Image();


        await img.load(
            webpBuffer
        );


        img.exif =
            construirDadosExif(
                packname,
                author
            );


        return await img.save(
            null
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][EXIF]',
            error.message
        );


        return webpBuffer;
    }
}


module.exports = {
    converterParaSticker,
    addExifToWebp
};
`;
