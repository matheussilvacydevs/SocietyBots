'use strict';


const fs =
    require('fs');

const path =
    require('path');

require('dotenv').config({
    path:
        path.join(
            __dirname,
            '.env'
        )
});

const crypto =
    require('crypto');

const {
    execFile
} =
    require('child_process');

const {
    promisify
} =
    require('util');

const societyFirestore =
    require(
        './society-firestore'
    );

const {
    uploadArchive,
    downloadArchive,
    MAX_ARCHIVE_BYTES
} =
    require(
        './society-cdn'
    );


const execFileAsync =
    promisify(
        execFile
    );


const BOT_ROOT =
    '/home/server/society-bots';

const TEMP_ROOT =
    '/home/server/society-import-tmp';


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


function httpError(
    status,
    message
) {

    const error =
        new Error(
            message
        );

    error.statusCode =
        status;

    return error;
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

        throw httpError(
            401,
            'AUTH_REQUIRED'
        );
    }


    const token =
        raw.slice(
            7
        ).trim();


    if (!token) {

        throw httpError(
            401,
            'AUTH_REQUIRED'
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

        throw httpError(
            500,
            'Firebase não configurado.'
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


    const user =
        data.users?.[0];


    if (
        !response.ok ||
        !user?.localId
    ) {

        throw httpError(
            401,
            'INVALID_SESSION'
        );
    }


    return user;
}


async function readArchive(
    req
) {

    const chunks =
        [];

    let size =
        0;


    for await (
        const chunk
        of req
    ) {

        const buffer =
            Buffer.from(
                chunk
            );


        size +=
            buffer.length;


        if (
            size >
            MAX_ARCHIVE_BYTES
        ) {

            throw httpError(
                413,
                'O arquivo compactado pode ter no máximo 80 MB.'
            );
        }


        chunks.push(
            buffer
        );
    }


    if (
        size ===
        0
    ) {

        throw httpError(
            400,
            'O arquivo enviado está vazio.'
        );
    }


    return Buffer.concat(
        chunks
    );
}


function decodeFilename(
    value
) {

    const raw =
        String(
            value ||
            ''
        );


    try {

        return decodeURIComponent(
            raw
        );

    } catch {

        return raw;
    }
}


function safeFilename(
    name
) {

    return String(
        name ||
        'bot.zip'
    )
        .replace(
            /[^\p{L}\p{N}._-]+/gu,
            '_'
        )
        .slice(
            0,
            120
        );
}


function archiveType(
    filename
) {

    const lower =
        filename.toLowerCase();


    if (
        lower.endsWith(
            '.tar.gz'
        )
    ) {
        return {
            ext:
                '.tar.gz',

            mime:
                'application/gzip'
        };
    }


    if (
        lower.endsWith(
            '.tgz'
        )
    ) {
        return {
            ext:
                '.tgz',

            mime:
                'application/gzip'
        };
    }


    if (
        lower.endsWith(
            '.zip'
        )
    ) {
        return {
            ext:
                '.zip',

            mime:
                'application/zip'
        };
    }


    if (
        lower.endsWith(
            '.tar'
        )
    ) {
        return {
            ext:
                '.tar',

            mime:
                'application/x-tar'
        };
    }


    throw httpError(
        400,
        'Envie um .zip, .tar.gz, .tgz ou .tar.'
    );
}


function sha256(
    buffer
) {

    return crypto
        .createHash(
            'sha256'
        )
        .update(
            buffer
        )
        .digest(
            'hex'
        );
}


function slug(
    value
) {

    const result =
        String(
            value ||
            'bot-importado'
        )
            .normalize(
                'NFKD'
            )
            .replace(
                /[\u0300-\u036f]/g,
                ''
            )
            .toLowerCase()
            .replace(
                /[^a-z0-9_-]+/g,
                '-'
            )
            .replace(
                /^-+|-+$/g,
                ''
            )
            .slice(
                0,
                45
            );


    return result ||
        'bot-importado';
}


function backupDirectory(
    uid
) {

    const candidates = [
        `/home/server/backups/arlecchino/imported-bots/${uid}`,
        `/home/server/society-import-archives/${uid}`
    ];


    for (
        const directory
        of candidates
    ) {

        try {

            fs.mkdirSync(
                directory,
                {
                    recursive:
                        true
                }
            );


            fs.accessSync(
                directory,
                fs.constants.W_OK
            );


            return directory;

        } catch {}
    }


    throw httpError(
        500,
        'Não consegui criar o backup local do arquivo.'
    );
}


function findPackageJson(
    root
) {

    const found =
        [];


    function walk(
        current,
        depth
    ) {

        if (
            depth >
            4
        ) {
            return;
        }


        for (
            const entry
            of fs.readdirSync(
                current,
                {
                    withFileTypes:
                        true
                }
            )
        ) {

            if (
                entry.name ===
                    'node_modules' ||
                entry.name ===
                    '.git' ||
                entry.name ===
                    '__MACOSX'
            ) {
                continue;
            }


            const full =
                path.join(
                    current,
                    entry.name
                );


            if (
                entry.isFile() &&
                entry.name ===
                    'package.json'
            ) {

                found.push(
                    full
                );

                continue;
            }


            if (
                entry.isDirectory()
            ) {

                walk(
                    full,
                    depth + 1
                );
            }
        }
    }


    walk(
        root,
        0
    );


    found.sort(
        (a, b) =>
            a.split(
                path.sep
            ).length -
            b.split(
                path.sep
            ).length
    );


    return found[0] ||
        null;
}


function removeHeavyFolders(
    root
) {

    function walk(
        current
    ) {

        for (
            const entry
            of fs.readdirSync(
                current,
                {
                    withFileTypes:
                        true
                }
            )
        ) {

            const full =
                path.join(
                    current,
                    entry.name
                );


            if (
                entry.isDirectory() &&
                (
                    entry.name ===
                        'node_modules' ||
                    entry.name ===
                        '.git'
                )
            ) {

                fs.rmSync(
                    full,
                    {
                        recursive:
                            true,

                        force:
                            true
                    }
                );

                continue;
            }


            if (
                entry.isDirectory()
            ) {

                walk(
                    full
                );
            }
        }
    }


    walk(
        root
    );
}


function compareVersion(
    a,
    b
) {

    for (
        let index = 0;
        index < 3;
        index++
    ) {

        const av =
            Number(
                a[index] ||
                0
            );


        const bv =
            Number(
                b[index] ||
                0
            );


        if (
            av >
            bv
        ) {

            return 1;
        }


        if (
            av <
            bv
        ) {

            return -1;
        }
    }


    return 0;
}


function systemZeroVersionAllowed(
    spec
) {

    const value =
        String(
            spec ||
            ''
        )
            .trim();


    /*
     * Não aceitamos referências ambíguas como:
     *
     * latest
     * *
     * github:
     * git+
     *
     * Para SystemZero, o package.json precisa conter
     * uma versão/range explicitamente auditável.
     */
    if (
        !value ||
        value === '*' ||
        value.toLowerCase() ===
            'latest' ||
        value.startsWith(
            'git+'
        ) ||
        value.startsWith(
            'github:'
        ) ||
        /^https?:\/\//i.test(
            value
        )
    ) {

        return false;
    }


    const matches =
        [
            ...value.matchAll(
                /(\d+)\.(\d+)\.(\d+)/g
            )
        ];


    if (
        matches.length ===
        0
    ) {

        return false;
    }


    const minimumSafe =
        [
            1,
            1,
            4
        ];


    /*
     * Todos os números de versão explícitos encontrados
     * precisam ser >= 1.1.4.
     *
     * Exemplos:
     *
     * 1.1.4                  OK
     * ^1.1.4                 OK
     * >=1.1.4               OK
     * >=1.1.4 <2.0.0        OK
     *
     * 1.1.2                  BLOQUEADO
     * ^1.1.2                 BLOQUEADO
     * 1.1.4 || 1.1.2        BLOQUEADO
     */
    return matches.every(
        match => {

            const version =
                [
                    Number(
                        match[1]
                    ),

                    Number(
                        match[2]
                    ),

                    Number(
                        match[3]
                    )
                ];


            return (
                compareVersion(
                    version,
                    minimumSafe
                ) >=
                0
            );
        }
    );
}


function detectLibrary(
    pkg
) {

    const deps = {
        ...(
            pkg.dependencies ||
            {}
        ),

        ...(
            pkg.devDependencies ||
            {}
        )
    };


    const systemZero =
        deps[
            '@systemzero/baileys'
        ];


    if (
        systemZero
    ) {

        if (
            !systemZeroVersionAllowed(
                systemZero
            )
        ) {

            throw httpError(
                400,
                '@systemzero/baileys está liberado a partir da versão 1.1.4. Atualize a dependência do projeto.'
            );
        }


        return '@systemzero/baileys';
    }


    if (
        deps['@itsliaaa/baileys']
    ) {

        return '@itsliaaa/baileys';
    }


    if (
        deps['@whiskeysockets/baileys']
    ) {

        return '@whiskeysockets/baileys';
    }


    return 'Node.js';
}


function validateBotProject(
    projectRoot,
    pkg
) {

    const candidates = [
        pkg.main,
        'index.js',
        'botconfig.js',
        'server.js',
        'app.js',
        'src/index.js',
        'src/main.js'
    ]
        .filter(
            Boolean
        );


    const hasEntry =
        candidates.some(
            entry =>
                fs.existsSync(
                    path.join(
                        projectRoot,
                        entry
                    )
                )
        );


    const hasStart =
        Boolean(
            pkg.scripts?.start
        );


    if (
        !hasEntry &&
        !hasStart
    ) {

        throw httpError(
            400,
            'O arquivo possui package.json, mas não parece conter um bot Node executável.'
        );
    }
}


module.exports =
async function societyImportRouter(
    req,
    res
) {

    let jobDirectory =
        null;

    let targetDirectory =
        null;


    try {

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
                        'Método não permitido.'
                }
            );

            return;
        }


        const idToken =
            bearer(
                req
            );


        const user =
            await firebaseUser(
                idToken
            );

        if (
            !user.emailVerified
        ) {

            throw httpError(
                403,
                'Confirme seu e-mail para importar bots.'
            );
        }



        const uid =
            user.localId;


        const originalName =
            safeFilename(
                decodeFilename(
                    req.headers[
                        'x-file-name'
                    ]
                )
            );


        const type =
            archiveType(
                originalName
            );


        /*
         * 1. APK -> Cloud.
         */
        const uploadedBuffer =
            await readArchive(
                req
            );


        const originalHash =
            sha256(
                uploadedBuffer
            );


        /*
         * 2. Cloud -> CDN.
         */
        const cdn =
            await uploadArchive(
                uploadedBuffer,
                originalName,
                type.mime
            );


        /*
         * 3. CDN -> Cloud novamente.
         */
        const downloadedBuffer =
            await downloadArchive(
                cdn
            );


        const downloadedHash =
            sha256(
                downloadedBuffer
            );


        /*
         * O arquivo só segue se voltou idêntico.
         */
        if (
            downloadedHash !==
            originalHash
        ) {

            throw httpError(
                502,
                'O checksum do arquivo mudou durante o round-trip pela CDN.'
            );
        }


        /*
         * Backup permanente do compactado que voltou da CDN.
         */
        const backupRoot =
            backupDirectory(
                uid
            );


        const backupName =
            `${Date.now()}-${originalName}`;


        const archivePath =
            path.join(
                backupRoot,
                backupName
            );


        fs.writeFileSync(
            archivePath,
            downloadedBuffer
        );


        jobDirectory =
            path.join(
                TEMP_ROOT,
                crypto
                    .randomUUID()
            );


        const extractDirectory =
            path.join(
                jobDirectory,
                'extracted'
            );


        fs.mkdirSync(
            extractDirectory,
            {
                recursive:
                    true
            }
        );


        /*
         * 4. Extração segura.
         */
        await execFileAsync(
            'python3',
            [
                path.join(
                    __dirname,
                    'society-safe-extract.py'
                ),

                archivePath,
                extractDirectory
            ],
            {
                timeout:
                    180000,

                maxBuffer:
                    1024 *
                    1024
            }
        );


        /*
         * 5. Encontrar package.json principal.
         */
        const packagePath =
            findPackageJson(
                extractDirectory
            );


        if (!packagePath) {

            throw httpError(
                400,
                'Não encontrei package.json dentro do arquivo. Ele não parece ser um bot Node.'
            );
        }


        const projectRoot =
            path.dirname(
                packagePath
            );


        let pkg;


        try {

            pkg =
                JSON.parse(
                    fs.readFileSync(
                        packagePath,
                        'utf8'
                    )
                );

        } catch {

            throw httpError(
                400,
                'package.json inválido.'
            );
        }


        const library =
            detectLibrary(
                pkg
            );


        validateBotProject(
            projectRoot,
            pkg
        );


        /*
         * Não reaproveitamos node_modules recebido no ZIP.
         * Dependências serão preparadas posteriormente pelo Society.
         */
        removeHeavyFolders(
            projectRoot
        );


        const displayName =
            String(
                pkg.displayName ||
                pkg.name ||
                originalName
                    .replace(
                        /\.(tar\.gz|tgz|zip|tar)$/i,
                        ''
                    )
            )
                .trim()
                .slice(
                    0,
                    80
                ) ||
            'Bot importado';


        const botId =
            `${slug(displayName)}_${crypto
                .randomUUID()
                .replace(/-/g, '')
                .slice(0, 12)}`;


        const userBotRoot =
            path.join(
                BOT_ROOT,
                uid
            );


        fs.mkdirSync(
            userBotRoot,
            {
                recursive:
                    true
            }
        );


        targetDirectory =
            path.join(
                userBotRoot,
                botId
            );


        /*
         * 6. O código só entra na pasta oficial depois
         * de todas as validações.
         */
        fs.renameSync(
            projectRoot,
            targetDirectory
        );


        const now =
            Date.now();


        const bot = {
            id:
                botId,

            name:
                displayName,

            owner:
                uid,

            architecture:
                'imported-archive',

            library,

            authMode:
                'existing',

            prefix:
                '/',

            database:
                'existing',

            style:
                'imported',

            status:
                'disconnected',

            archived:
                false,

            createdAt:
                now,

            updatedAt:
                now,

            importedAt:
                now,

            archiveName:
                originalName,

            importSha256:
                originalHash
        };


        try {

            /*
             * 7. Registro site-to-site no Firestore.
             */
            await societyFirestore
                .saveBot(
                    uid,
                    botId,
                    idToken,
                    bot
                );

        } catch (
            error
        ) {

            /*
             * Não deixa bot órfão na pasta oficial.
             */
            if (
                targetDirectory &&
                fs.existsSync(
                    targetDirectory
                )
            ) {

                const failedRoot =
                    path.join(
                        backupRoot,
                        'failed'
                    );


                fs.mkdirSync(
                    failedRoot,
                    {
                        recursive:
                            true
                    }
                );


                fs.renameSync(
                    targetDirectory,
                    path.join(
                        failedRoot,
                        botId
                    )
                );


                targetDirectory =
                    null;
            }


            throw error;
        }


        if (
            jobDirectory &&
            fs.existsSync(
                jobDirectory
            )
        ) {

            fs.rmSync(
                jobDirectory,
                {
                    recursive:
                        true,

                    force:
                        true
                }
            );
        }


        sendJson(
            res,
            200,
            {
                ok: true,

                bot,

                import: {
                    archive:
                        backupName,

                    sha256:
                        originalHash,

                    cdnRoundTrip:
                        true
                }
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][IMPORT BOT]',
            error.message
        );


        if (
            jobDirectory &&
            fs.existsSync(
                jobDirectory
            )
        ) {

            try {

                fs.rmSync(
                    jobDirectory,
                    {
                        recursive:
                            true,

                        force:
                            true
                    }
                );

            } catch {}
        }


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
                        : error.message ||
                            'Não foi possível importar o bot.'
            }
        );
    }
};
