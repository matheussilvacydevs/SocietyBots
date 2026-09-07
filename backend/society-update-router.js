'use strict';

const fs =
    require('fs');

const path =
    require('path');

const crypto =
    require('crypto');


const APK_PATH =
    '/home/server/backups/arlecchino/SocietyBots-debug.apk';


const VERSION_FILE =
    path.join(
        __dirname,
        'society-app-version.json'
    );


function json(
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

    res.setHeader(
        'X-Content-Type-Options',
        'nosniff'
    );

    res.end(
        JSON.stringify(
            body
        )
    );
}


function sha256File(
    filename
) {

    const hash =
        crypto.createHash(
            'sha256'
        );

    const fd =
        fs.openSync(
            filename,
            'r'
        );

    const buffer =
        Buffer.allocUnsafe(
            1024 * 1024
        );

    try {

        while (true) {

            const read =
                fs.readSync(
                    fd,
                    buffer,
                    0,
                    buffer.length,
                    null
                );

            if (
                read <=
                0
            ) {
                break;
            }

            hash.update(
                buffer.subarray(
                    0,
                    read
                )
            );
        }

    } finally {

        fs.closeSync(
            fd
        );
    }

    return hash.digest(
        'hex'
    );
}


function readVersion() {

    if (
        !fs.existsSync(
            VERSION_FILE
        )
    ) {

        return null;
    }

    try {

        return JSON.parse(
            fs.readFileSync(
                VERSION_FILE,
                'utf8'
            )
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][UPDATE] version.json inválido:',
            error.message
        );

        return null;
    }
}


async function version(
    req,
    res
) {

    if (
        req.method !==
        'GET'
    ) {

        return json(
            res,
            405,
            {
                ok: false,
                error:
                    'METHOD_NOT_ALLOWED'
            }
        );
    }


    const info =
        readVersion();


    if (
        !info
    ) {

        return json(
            res,
            503,
            {
                ok: false,
                error:
                    'UPDATE_METADATA_UNAVAILABLE'
            }
        );
    }


    if (
        !fs.existsSync(
            APK_PATH
        )
    ) {

        return json(
            res,
            503,
            {
                ok: false,
                error:
                    'APK_UNAVAILABLE'
            }
        );
    }


    const stat =
        fs.statSync(
            APK_PATH
        );


    return json(
        res,
        200,
        {
            ok: true,

            versionCode:
                Number(
                    info.versionCode
                ),

            versionName:
                String(
                    info.versionName
                ),

            minSupportedVersionCode:
                Number(
                    info.minSupportedVersionCode ||
                    1
                ),

            forceUpdate:
                Boolean(
                    info.forceUpdate
                ),

            apkUrl:
                'https://android-studio.cloudpaniel.com.br/api/society/app/apk',

            sha256:
                String(
                    info.sha256 ||
                    sha256File(
                        APK_PATH
                    )
                ),

            size:
                stat.size,

            changelog:
                Array.isArray(
                    info.changelog
                )
                    ? info.changelog
                    : []
        }
    );
}


async function apk(
    req,
    res
) {

    if (
        req.method !==
        'GET'
    ) {

        return json(
            res,
            405,
            {
                ok: false,
                error:
                    'METHOD_NOT_ALLOWED'
            }
        );
    }


    if (
        !fs.existsSync(
            APK_PATH
        )
    ) {

        return json(
            res,
            404,
            {
                ok: false,
                error:
                    'APK_NOT_FOUND'
            }
        );
    }


    const stat =
        fs.statSync(
            APK_PATH
        );


    res.statusCode =
        200;

    res.setHeader(
        'Content-Type',
        'application/vnd.android.package-archive'
    );

    res.setHeader(
        'Content-Length',
        String(
            stat.size
        )
    );

    res.setHeader(
        'Content-Disposition',
        'attachment; filename="SocietyBots.apk"'
    );

    res.setHeader(
        'Cache-Control',
        'no-store'
    );

    res.setHeader(
        'X-Content-Type-Options',
        'nosniff'
    );


    const stream =
        fs.createReadStream(
            APK_PATH
        );


    stream.on(
        'error',
        error => {

            console.error(
                '[SOCIETY][UPDATE][APK]',
                error
            );

            if (
                !res.headersSent
            ) {

                json(
                    res,
                    500,
                    {
                        ok: false,
                        error:
                            'APK_STREAM_ERROR'
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
}


module.exports = {
    version,
    apk
};
