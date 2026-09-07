'use strict';

const societyBotImportRouter =
    require('./society-import-router');

const societyUpdateRouter =
    require('./society-update-router');


const societyCdnBridge =
    require('./society-cdn-bridge');




const societyAiChatsRouter =
    require('./society-ai-chats-router');

const societyBotAvatarRouter =
    require('./society-bot-avatar-router');



const societyAi = require('./society-ai');

const societyBotManager =
    require('./society-bot-manager');


const societyWhatsApp =
    require(
        './society-whatsapp'
    );


const path = require('path');
const fs = require('fs');
const fsp = fs.promises;

const societyFirestore =
    require('./society-firestore');

const societyBuilder =
    require('./society-builder');

require('dotenv').config({
    path: path.join(__dirname, '.env')
});

const FIREBASE_API_KEY =
    process.env.FIREBASE_API_KEY;

const FIREBASE_PROJECT_ID =
    process.env.FIREBASE_PROJECT_ID ||
    'cobrancasmart-8b868';

if (!FIREBASE_API_KEY) {
    throw new Error(
        'SocietyBots: FIREBASE_API_KEY não configurada.'
    );
}

const FIREBASE_BASE =
    'https://identitytoolkit.googleapis.com/v1';

const MAX_BODY_SIZE =
    256 * 1024;


/*
 * Proteção de login.
 *
 * 1–4 falhas: login continua permitido
 * 5–6 falhas: 60 segundos
 * 7–9 falhas: 5 minutos
 * 10+ falhas: 15 minutos
 *
 * Login bem-sucedido limpa o contador.
 */
const LOGIN_ATTEMPTS =
    new Map();

const LOGIN_ENTRY_TTL =
    24 * 60 * 60 * 1000;


/* ============================================================
 * RESPOSTAS
 * ============================================================ */

function json(res, status, data) {

    const body =
        JSON.stringify(data);

    res.writeHead(
        status,
        {
            'Content-Type':
                'application/json; charset=utf-8',

            'Content-Length':
                Buffer.byteLength(body),

            'Cache-Control':
                'no-store',

            'X-Content-Type-Options':
                'nosniff'
        }
    );

    res.end(body);
}


function methodNotAllowed(res) {

    return json(
        res,
        405,
        {
            ok: false,
            error:
                'Método não permitido.'
        }
    );
}


/* ============================================================
 * BODY JSON
 * ============================================================ */

function readJson(req) {

    return new Promise(
        (resolve, reject) => {

            let size = 0;

            const chunks = [];

            req.on(
                'data',
                chunk => {

                    size += chunk.length;

                    if (
                        size >
                        MAX_BODY_SIZE
                    ) {

                        reject(
                            new Error(
                                'PAYLOAD_TOO_LARGE'
                            )
                        );

                        req.destroy();

                        return;
                    }

                    chunks.push(chunk);
                }
            );


            req.on(
                'end',
                () => {

                    try {

                        const raw =
                            Buffer
                                .concat(chunks)
                                .toString(
                                    'utf8'
                                );

                        if (!raw.trim()) {

                            return resolve(
                                {}
                            );
                        }

                        resolve(
                            JSON.parse(raw)
                        );

                    } catch {

                        reject(
                            new Error(
                                'INVALID_JSON'
                            )
                        );
                    }
                }
            );


            req.on(
                'error',
                reject
            );
        }
    );
}


/* ============================================================
 * FIREBASE
 * ============================================================ */

function firebaseUrl(endpoint) {

    return (
        `${FIREBASE_BASE}/${endpoint}` +
        `?key=${encodeURIComponent(FIREBASE_API_KEY)}`
    );
}


async function firebaseRequest(
    endpoint,
    body
) {

    const response =
        await fetch(
            firebaseUrl(endpoint),
            {
                method: 'POST',

                headers: {
                    'Content-Type':
                        'application/json'
                },

                body:
                    JSON.stringify(body)
            }
        );


    const raw =
        await response.text();


    let data = {};

    try {
        data =
            JSON.parse(raw);
    } catch {}


    if (!response.ok) {

        const firebaseCode =
            data?.error?.message ||
            `HTTP_${response.status}`;

        const error =
            new Error(
                firebaseCode
            );

        error.firebaseCode =
            firebaseCode;

        throw error;
    }


    return data;
}


/* ============================================================
 * ERROS FIREBASE
 * ============================================================ */

function firebaseErrorMessage(code) {

    const normalized =
        String(code || '')
            .split(' : ')[0]
            .trim();


    const errors = {

        EMAIL_EXISTS:
            'Este e-mail já está cadastrado.',

        INVALID_EMAIL:
            'O endereço de e-mail é inválido.',

        EMAIL_NOT_FOUND:
            'Nenhuma conta foi encontrada com este e-mail.',

        INVALID_PASSWORD:
            'A senha informada está incorreta.',

        INVALID_LOGIN_CREDENTIALS:
            'E-mail ou senha incorretos.',

        USER_DISABLED:
            'Esta conta foi desativada.',

        TOO_MANY_ATTEMPTS_TRY_LATER:
            'Muitas tentativas. Aguarde alguns minutos e tente novamente.',

        OPERATION_NOT_ALLOWED:
            'Login com e-mail e senha não está habilitado no Firebase.',

        WEAK_PASSWORD:
            'A senha informada é muito fraca.',

        MISSING_PASSWORD:
            'Informe sua senha.',

        MISSING_EMAIL:
            'Informe seu e-mail.',

        INVALID_ID_TOKEN:
            'Sua sessão não é válida.',

        TOKEN_EXPIRED:
            'Sua sessão expirou.'
    };


    return (
        errors[normalized] ||
        'Não foi possível concluir a solicitação.'
    );
}


function validEmail(email) {

    return (
        typeof email ===
        'string' &&

        /^[^\s@]+@[^\s@]+\.[^\s@]+$/
            .test(
                email.trim()
            )
    );
}



/* ============================================================
 * SEGURANÇA DE SENHA / LOGIN
 * ============================================================ */

function registrationPasswordValid(
    password
) {

    if (
        typeof password !==
        'string'
    ) {
        return false;
    }

    if (
        password.length < 6 ||
        password.length > 64
    ) {
        return false;
    }

    /*
     * O usuário pediu uma política permissiva:
     * basta existir uma MAIÚSCULA OU um NÚMERO.
     */
    return (
        /[A-Z]/.test(password) ||
        /[0-9]/.test(password)
    );
}


function clientIp(req) {

    /*
     * Como existe Nginx na frente do Node,
     * priorizamos X-Forwarded-For.
     */
    const forwarded =
        String(
            req.headers[
                'x-forwarded-for'
            ] || ''
        )
            .split(',')[0]
            .trim();

    const realIp =
        String(
            req.headers[
                'x-real-ip'
            ] || ''
        ).trim();

    return (
        forwarded ||
        realIp ||
        req.socket?.remoteAddress ||
        'unknown'
    );
}


function loginAttemptKey(
    req,
    email
) {

    return (
        clientIp(req) +
        '|' +
        String(email || '')
            .trim()
            .toLowerCase()
    );
}


function loginCooldownMs(
    failures
) {

    if (failures >= 10) {
        return (
            15 *
            60 *
            1000
        );
    }

    if (failures >= 7) {
        return (
            5 *
            60 *
            1000
        );
    }

    if (failures >= 5) {
        return (
            60 *
            1000
        );
    }

    return 0;
}


function loginRateState(
    req,
    email
) {

    const key =
        loginAttemptKey(
            req,
            email
        );

    const entry =
        LOGIN_ATTEMPTS.get(
            key
        );

    if (!entry) {

        return {
            key,
            blocked: false,
            failures: 0,
            retryAfter: 0
        };
    }

    const now =
        Date.now();

    if (
        entry.blockedUntil &&
        entry.blockedUntil >
        now
    ) {

        return {
            key,
            blocked: true,
            failures:
                entry.failures,

            retryAfter:
                Math.max(
                    1,
                    Math.ceil(
                        (
                            entry.blockedUntil -
                            now
                        ) /
                        1000
                    )
                )
        };
    }

    return {
        key,
        blocked: false,
        failures:
            entry.failures || 0,
        retryAfter: 0
    };
}


function failedLogin(
    req,
    email
) {

    const key =
        loginAttemptKey(
            req,
            email
        );

    const previous =
        LOGIN_ATTEMPTS.get(
            key
        ) || {
            failures: 0
        };

    const failures =
        previous.failures +
        1;

    const cooldown =
        loginCooldownMs(
            failures
        );

    const entry = {
        failures,

        lastFailure:
            Date.now(),

        blockedUntil:
            cooldown
                ? Date.now() +
                    cooldown
                : 0
    };

    LOGIN_ATTEMPTS.set(
        key,
        entry
    );

    return {
        failures,

        retryAfter:
            cooldown
                ? Math.ceil(
                    cooldown /
                    1000
                )
                : 0
    };
}


function clearLoginFailures(
    req,
    email
) {

    LOGIN_ATTEMPTS.delete(
        loginAttemptKey(
            req,
            email
        )
    );
}


/*
 * Evita que o Map cresça indefinidamente.
 */
const loginCleanupTimer =
    setInterval(
        () => {

            const now =
                Date.now();

            for (
                const [
                    key,
                    entry
                ]
                of
                LOGIN_ATTEMPTS
            ) {

                const last =
                    entry.lastFailure ||
                    0;

                if (
                    now - last >
                    LOGIN_ENTRY_TTL
                ) {

                    LOGIN_ATTEMPTS
                        .delete(
                            key
                        );
                }
            }

        },
        60 * 60 * 1000
    );

loginCleanupTimer.unref?.();


function sanitizeUser(data) {

    return {

        uid:
            data.localId ||
            data.local_id ||
            null,

        email:
            data.email ||
            null,

        displayName:
            data.displayName ||
            null,

        emailVerified:
            Boolean(
                data.emailVerified
            )
    };
}


/* ============================================================
 * REGISTER
 * ============================================================ */

async function register(
    req,
    res
) {

    try {

        const body =
            await readJson(req);


        const name =
            String(
                body.name || ''
            ).trim();

        const email =
            String(
                body.email || ''
            )
                .trim()
                .toLowerCase();

        const password =
            String(
                body.password ||
                ''
            );


        if (name.length < 2) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'Informe seu nome.'
                }
            );
        }


        if (!validEmail(email)) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'Informe um e-mail válido.'
                }
            );
        }


        if (
            password.length < 6 ||
            password.length > 64
        ) {

            return json(
                res,
                400,
                {
                    ok: false,
                    code:
                        'PASSWORD_LENGTH',

                    error:
                        'A senha deve ter entre 6 e 64 caracteres.'
                }
            );
        }


        if (
            !registrationPasswordValid(
                password
            )
        ) {

            return json(
                res,
                400,
                {
                    ok: false,
                    code:
                        'PASSWORD_REQUIREMENT',

                    error:
                        'Use pelo menos uma letra maiúscula ou um número.'
                }
            );
        }


        const created =
            await firebaseRequest(
                'accounts:signUp',
                {
                    email,
                    password,
                    returnSecureToken:
                        true
                }
            );


        const updated =
            await firebaseRequest(
                'accounts:update',
                {
                    idToken:
                        created.idToken,

                    displayName:
                        name,

                    returnSecureToken:
                        true
                }
            );


        try {

            await firebaseRequest(
                'accounts:sendOobCode',
                {
                    requestType:
                        'VERIFY_EMAIL',

                    idToken:
                        updated.idToken ||
                        created.idToken
                }
            );

        } catch (
            verifyError
        ) {

            console.error(
                '[SOCIETY][VERIFY EMAIL]',
                verifyError.firebaseCode ||
                verifyError.message
            );
        }


        return json(
            res,
            201,
            {
                ok: true,

                message:
                    'Conta criada. Confirme seu e-mail para liberar a criação de bots.',

                user:
                    sanitizeUser(
                        updated
                    ),

                auth: {

                    idToken:
                        updated.idToken ||
                        created.idToken,

                    refreshToken:
                        updated.refreshToken ||
                        created.refreshToken,

                    expiresIn:
                        Number(
                            updated.expiresIn ||
                            created.expiresIn ||
                            3600
                        )
                }
            }
        );

    } catch (error) {

        console.error(
            '[SOCIETY][REGISTER]',
            error.firebaseCode ||
            error.message
        );


        if (
            error.message ===
            'INVALID_JSON'
        ) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'JSON inválido.'
                }
            );
        }


        return json(
            res,
            400,
            {
                ok: false,

                error:
                    firebaseErrorMessage(
                        error.firebaseCode
                    )
            }
        );
    }
}


/* ============================================================
 * LOGIN
 * ============================================================ */

async function login(
    req,
    res
) {

    let email = '';

    try {

        const body =
            await readJson(req);


        email =
            String(
                body.email || ''
            )
                .trim()
                .toLowerCase();

        const password =
            String(
                body.password ||
                ''
            );


        if (!validEmail(email)) {

            return json(
                res,
                400,
                {
                    ok: false,
                    code:
                        'INVALID_EMAIL',

                    error:
                        'Informe um e-mail válido.'
                }
            );
        }


        if (!password) {

            return json(
                res,
                400,
                {
                    ok: false,
                    code:
                        'MISSING_PASSWORD',

                    error:
                        'Informe sua senha.'
                }
            );
        }


        const rate =
            loginRateState(
                req,
                email
            );


        if (rate.blocked) {

            return json(
                res,
                429,
                {
                    ok: false,

                    code:
                        'LOGIN_RATE_LIMITED',

                    error:
                        `Muitas tentativas. Aguarde ${rate.retryAfter} segundos e tente novamente.`,

                    retryAfter:
                        rate.retryAfter,

                    resetSuggested:
                        true
                }
            );
        }


        const data =
            await firebaseRequest(
                'accounts:signInWithPassword',
                {
                    email,
                    password,

                    returnSecureToken:
                        true
                }
            );


        /*
         * Credenciais corretas:
         * zeramos as falhas.
         */
        clearLoginFailures(
            req,
            email
        );


        return json(
            res,
            200,
            {
                ok: true,

                message:
                    'Login realizado com sucesso.',

                user:
                    sanitizeUser(
                        data
                    ),

                auth: {

                    idToken:
                        data.idToken,

                    refreshToken:
                        data.refreshToken,

                    expiresIn:
                        Number(
                            data.expiresIn ||
                            3600
                        )
                }
            }
        );

    } catch (error) {

        const firebaseCode =
            String(
                error.firebaseCode ||
                ''
            )
                .split(' : ')[0]
                .trim();


        const credentialFailure =
            [
                'INVALID_PASSWORD',
                'INVALID_LOGIN_CREDENTIALS',
                'EMAIL_NOT_FOUND'
            ].includes(
                firebaseCode
            );


        if (credentialFailure) {

            const failed =
                failedLogin(
                    req,
                    email
                );


            console.warn(
                '[SOCIETY][LOGIN_FAIL]',
                {
                    ip:
                        clientIp(req),

                    failures:
                        failed.failures
                }
            );


            if (
                failed.retryAfter >
                0
            ) {

                return json(
                    res,
                    429,
                    {
                        ok: false,

                        code:
                            'LOGIN_RATE_LIMITED',

                        error:
                            `Muitas tentativas incorretas. Aguarde ${failed.retryAfter} segundos.`,

                        retryAfter:
                            failed.retryAfter,

                        resetSuggested:
                            true
                    }
                );
            }


            return json(
                res,
                401,
                {
                    ok: false,

                    code:
                        'INVALID_CREDENTIALS',

                    error:
                        'E-mail ou senha incorretos.',

                    failedAttempts:
                        failed.failures,

                    resetSuggested:
                        true
                }
            );
        }


        console.error(
            '[SOCIETY][LOGIN]',
            firebaseCode ||
            error.message
        );


        if (
            firebaseCode ===
            'TOO_MANY_ATTEMPTS_TRY_LATER'
        ) {

            return json(
                res,
                429,
                {
                    ok: false,

                    code:
                        'FIREBASE_RATE_LIMITED',

                    error:
                        'Muitas tentativas. Aguarde alguns minutos e tente novamente.',

                    resetSuggested:
                        true
                }
            );
        }


        return json(
            res,
            401,
            {
                ok: false,

                error:
                    firebaseErrorMessage(
                        error.firebaseCode
                    )
            }
        );
    }
}


/* ============================================================
 * PASSWORD RESET
 * ============================================================ */

async function forgotPassword(
    req,
    res
) {

    const genericMessage =
        'Se esse e-mail estiver cadastrado, você receberá as instruções para redefinir sua senha.';


    try {

        const body =
            await readJson(req);


        const email =
            String(
                body.email || ''
            )
                .trim()
                .toLowerCase();


        if (!validEmail(email)) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'Informe um e-mail válido.'
                }
            );
        }


        await firebaseRequest(
            'accounts:sendOobCode',
            {
                requestType:
                    'PASSWORD_RESET',

                email
            }
        );


        return json(
            res,
            200,
            {
                ok: true,
                message:
                    genericMessage
            }
        );

    } catch (error) {

        if (
            error.firebaseCode ===
            'EMAIL_NOT_FOUND'
        ) {

            return json(
                res,
                200,
                {
                    ok: true,
                    message:
                        genericMessage
                }
            );
        }


        console.error(
            '[SOCIETY][RESET]',
            error.firebaseCode ||
            error.message
        );


        return json(
            res,
            400,
            {
                ok: false,

                error:
                    firebaseErrorMessage(
                        error.firebaseCode
                    )
            }
        );
    }
}


/* ============================================================
 * RESEND EMAIL VERIFICATION
 * ============================================================ */

async function resendVerification(
    req,
    res
) {

    try {

        const authorization =
            String(
                req.headers.authorization ||
                ''
            );


        const match =
            authorization.match(
                /^Bearer\s+(.+)$/i
            );


        if (
            !match
        ) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Token de autenticação ausente.'
                }
            );
        }


        await firebaseRequest(
            'accounts:sendOobCode',
            {
                requestType:
                    'VERIFY_EMAIL',

                idToken:
                    match[1]
            }
        );


        return json(
            res,
            200,
            {
                ok: true,

                message:
                    'E-mail de verificação enviado.'
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][RESEND VERIFY]',
            error.firebaseCode ||
            error.message
        );


        return json(
            res,
            400,
            {
                ok: false,

                error:
                    firebaseErrorMessage(
                        error.firebaseCode
                    )
            }
        );
    }
}


/* ============================================================
 * ME
 * ============================================================ */

async function me(
    req,
    res
) {

    try {

        const authorization =
            String(
                req.headers
                    .authorization ||
                ''
            );


        const match =
            authorization.match(
                /^Bearer\s+(.+)$/i
            );


        if (!match) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Token de autenticação ausente.'
                }
            );
        }


        const data =
            await firebaseRequest(
                'accounts:lookup',
                {
                    idToken:
                        match[1]
                }
            );


        const user =
            data.users?.[0];


        if (!user) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Sessão inválida.'
                }
            );
        }


        return json(
            res,
            200,
            {
                ok: true,
                user:
                    sanitizeUser(user)
            }
        );

    } catch {

        return json(
            res,
            401,
            {
                ok: false,
                error:
                    'Sua sessão expirou ou não é válida.'
            }
        );
    }
}



/* ============================================================
 * VERIFIED EMAIL GUARD
 * ============================================================ */

async function requireVerifiedEmail(
    req,
    res
) {

    try {

        const authorization =
            String(
                req.headers.authorization ||
                ''
            );


        const match =
            authorization.match(
                /^Bearer\s+(.+)$/i
            );


        if (
            !match
        ) {

            json(
                res,
                401,
                {
                    ok: false,

                    code:
                        'AUTH_REQUIRED',

                    error:
                        'Entre novamente para continuar.'
                }
            );

            return false;
        }


        const data =
            await firebaseRequest(
                'accounts:lookup',
                {
                    idToken:
                        match[1]
                }
            );


        const user =
            data.users?.[0];


        if (
            !user
        ) {

            json(
                res,
                401,
                {
                    ok: false,

                    code:
                        'INVALID_SESSION',

                    error:
                        'Sua sessão expirou.'
                }
            );

            return false;
        }


        if (
            !user.emailVerified
        ) {

            json(
                res,
                403,
                {
                    ok: false,

                    code:
                        'EMAIL_NOT_VERIFIED',

                    error:
                        'Confirme seu e-mail para criar bots.'
                }
            );

            return false;
        }


        return true;

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][VERIFIED EMAIL GUARD]',
            error.message
        );


        json(
            res,
            401,
            {
                ok: false,

                code:
                    'INVALID_SESSION',

                error:
                    'Não foi possível validar sua sessão.'
            }
        );


        return false;
    }
}


/* ============================================================
 * BUILDER
 * ============================================================ */

async function builderStart(
    req,
    res
) {

    const body =
        await readJson(req);

    const name =
        String(
            body.name ||
            ''
        ).trim();

    const owner =
        String(
            body.owner ||
            ''
        ).trim();

    const uid =
        String(
            body.uid ||
            'local-user'
        ).trim();


    const library =
        String(
            body.library ||
            ''
        ).trim();


    const style =
        String(
            body.style ||
            ''
        ).trim();


    const features =
        Array.isArray(
            body.features
        )
            ? body.features
                .map(
                    item =>
                        String(item)
                            .trim()
                )
                .filter(Boolean)
            : [];


    if (
        name.length <
        2
    ) {

        return json(
            res,
            400,
            {
                ok: false,
                error:
                    'Informe o nome do bot.'
            }
        );
    }


    const session =
        societyBuilder
            .createSession({
                uid,
                name,
                owner,
                library,
                style,
                features
            });


    return json(
        res,
        201,
        {
            ok: true,

            session:
                societyBuilder
                    .sessionView(
                        session
                    )
        }
    );
}


async function builderAnswer(
    req,
    res
) {

    const body =
        await readJson(req);

    const session =
        societyBuilder
            .getSession(
                body.sessionId
            );


    if (!session) {

        return json(
            res,
            404,
            {
                ok: false,
                error:
                    'Sessão de criação não encontrada.'
            }
        );
    }


    try {

        societyBuilder
            .answerSession(
                session,
                body.value
            );


        return json(
            res,
            200,
            {
                ok: true,

                session:
                    societyBuilder
                        .sessionView(
                            session
                        )
            }
        );

    } catch (
        error
    ) {

        return json(
            res,
            400,
            {
                ok: false,
                error:
                    error.message
            }
        );
    }
}


async function builderBuild(
    req,
    res
) {

    const body =
        await readJson(req);

    const session =
        societyBuilder
            .getSession(
                body.sessionId
            );


    if (!session) {

        return json(
            res,
            404,
            {
                ok: false,
                error:
                    'Sessão não encontrada.'
            }
        );
    }


    /*
     * Não bloqueia a resposta esperando npm install.
     */
    societyBuilder
        .build(
            session
        )
        .catch(
            error => {

                console.error(
                    '[SOCIETY][BUILDER]',
                    error
                );

                session.status =
                    'error';

                session.events.push({
                    id:
                        session.events.length +
                        1,

                    at:
                        Date.now(),

                    type:
                        'error',

                    message:
                        error.message ||
                        'Erro ao construir bot.'
                });
            }
        );


    return json(
        res,
        202,
        {
            ok: true,
            message:
                'Construção iniciada.',

            session:
                societyBuilder
                    .sessionView(
                        session
                    )
        }
    );
}


async function builderEvents(
    req,
    res,
    url
) {

    const session =
        societyBuilder
            .getSession(
                url.searchParams
                    .get(
                        'sessionId'
                    )
            );


    if (!session) {

        return json(
            res,
            404,
            {
                ok: false,
                error:
                    'Sessão não encontrada.'
            }
        );
    }


    const after =
        Number(
            url.searchParams
                .get(
                    'after'
                ) ||
            0
        );


    return json(
        res,
        200,
        {
            ok: true,

            session:
                societyBuilder
                    .sessionView(
                        session
                    ),

            events:
                societyBuilder
                    .eventsSince(
                        session,
                        after
                    )
        }
    );
}



/* ============================================================
 * BOT STATUS / COMMANDS
 * ============================================================ */

async function botStatus(
    req,
    res,
    url
) {

    const uid =
        String(
            url.searchParams.get(
                'uid'
            ) || ''
        ).trim();

    const botId =
        String(
            url.searchParams.get(
                'botId'
            ) || ''
        ).trim();


    if (
        !uid ||
        !botId
    ) {

        return json(
            res,
            400,
            {
                ok: false,
                error:
                    'uid e botId são obrigatórios.'
            }
        );
    }


    const result =
        await societyBotManager
            .status(
                uid,
                botId
            );


    return json(
        res,
        200,
        {
            ok: true,
            status:
                result.status
        }
    );
}


async function botCommands(
    req,
    res,
    url
) {

    const uid =
        String(
            url.searchParams.get(
                'uid'
            ) || ''
        ).trim();

    const botId =
        String(
            url.searchParams.get(
                'botId'
            ) || ''
        ).trim();


    if (
        !uid ||
        !botId
    ) {

        return json(
            res,
            400,
            {
                ok: false,
                error:
                    'uid e botId são obrigatórios.'
            }
        );
    }


    try {

        const result =
            await societyBotManager
                .listCommands(
                    uid,
                    botId
                );


        return json(
            res,
            200,
            {
                ok: true,
                ...result
            }
        );

    } catch (
        error
    ) {

        if (
            error.message ===
            'BOT_NOT_FOUND'
        ) {

            return json(
                res,
                404,
                {
                    ok: false,
                    error:
                        'Bot não encontrado.'
                }
            );
        }


        throw error;
    }
}


async function createBotCommand(
    req,
    res
) {

    const body =
        await readJson(
            req
        );


    try {

        const command =
            await societyBotManager
                .createCommand(
                    String(
                        body.uid || ''
                    ).trim(),

                    String(
                        body.botId || ''
                    ).trim(),

                    {
                        name:
                            body.name,

                        description:
                            body.description,

                        response:
                            body.response,

                        type:
                            body.type
                    }
                );


        return json(
            res,
            201,
            {
                ok: true,
                command
            }
        );

    } catch (
        error
    ) {

        const errors = {

            BOT_NOT_FOUND:
                'Bot não encontrado.',

            INVALID_COMMAND_NAME:
                'Nome de comando inválido.',

            INVALID_RESPONSE:
                'Informe uma resposta para o comando.',

            RESERVED_COMMAND:
                'Esse comando é padrão do Society Bots.',

            COMMAND_EXISTS:
                'Já existe um comando com esse nome.'
        };


        return json(
            res,
            error.message ===
                'BOT_NOT_FOUND'
                ? 404
                : 400,
            {
                ok: false,
                error:
                    errors[
                        error.message
                    ] ||
                    'Não foi possível criar o comando.'
            }
        );
    }
}


async function deleteBotCommand(
    req,
    res
) {

    const body =
        await readJson(
            req
        );


    try {

        await societyBotManager
            .deleteCommand(
                String(
                    body.uid || ''
                ).trim(),

                String(
                    body.botId || ''
                ).trim(),

                String(
                    body.commandId || ''
                ).trim()
            );


        return json(
            res,
            200,
            {
                ok: true,
                message:
                    'Comando removido.'
            }
        );

    } catch (
        error
    ) {

        return json(
            res,
            error.message ===
                'BOT_NOT_FOUND'
                ? 404
                : 400,
            {
                ok: false,
                error:
                    error.message ===
                    'BOT_NOT_FOUND'
                        ? 'Bot não encontrado.'
                        : 'Não foi possível remover o comando.'
            }
        );
    }
}




async function botLogs(
    req,
    res,
    url
) {

    const uid =
        String(
            url.searchParams.get('uid') || ''
        ).trim();

    const botId =
        String(
            url.searchParams.get('botId') || ''
        ).trim();

    if (!uid || !botId) {
        return json(
            res,
            400,
            {
                ok: false,
                error: 'uid e botId são obrigatórios.'
            }
        );
    }

    try {

        const lines =
            await societyBotManager.logs(
                uid,
                botId,
                20
            );

        return json(
            res,
            200,
            {
                ok: true,
                logs: lines
            }
        );

    } catch (error) {

        return json(
            res,
            error.message === 'BOT_NOT_FOUND'
                ? 404
                : 500,
            {
                ok: false,
                error:
                    error.message === 'BOT_NOT_FOUND'
                        ? 'Bot não encontrado.'
                        : 'Não foi possível carregar os logs.'
            }
        );
    }
}


async function botControl(
    req,
    res,
    action
) {

    const body =
        await readJson(req);

    const uid =
        String(
            body.uid || ''
        ).trim();

    const botId =
        String(
            body.botId || ''
        ).trim();

    if (!uid || !botId) {
        return json(
            res,
            400,
            {
                ok: false,
                error: 'uid e botId são obrigatórios.'
            }
        );
    }

    try {

        let result;

        if (action === 'start') {
            result =
                await societyBotManager.startBot(
                    uid,
                    botId
                );
        } else if (action === 'stop') {
            result =
                await societyBotManager.stopBot(
                    uid,
                    botId
                );
        } else if (action === 'restart') {
            result =
                await societyBotManager.restartBot(
                    uid,
                    botId
                );
        } else {
            return json(
                res,
                400,
                {
                    ok: false,
                    error: 'Ação inválida.'
                }
            );
        }

        return json(
            res,
            200,
            {
                ok: true,
                action,
                result
            }
        );

    } catch (error) {

        return json(
            res,
            error.message === 'BOT_NOT_FOUND'
                ? 404
                : 500,
            {
                ok: false,
                error:
                    error.message === 'BOT_NOT_FOUND'
                        ? 'Bot não encontrado.'
                        : 'Falha ao controlar o bot.'
            }
        );
    }
}



/* ============================================================
 * SOCIETY WHATSAPP API
 * ============================================================ */

async function societyWhatsAppUid(
    req
) {

    const token =
        bearerToken(
            req
        );


    if (!token) {

        const error =
            new Error(
                'AUTH_REQUIRED'
            );


        error.statusCode =
            401;


        throw error;
    }


    const response =
        await firebaseUserFromToken(
            token
        );


    const user =
        response?.users?.[0] ||
        response?.user ||
        response;


    const uid =
        String(
            user?.localId ||
            user?.uid ||
            user?.user_id ||
            ''
        ).trim();


    if (!uid) {

        const error =
            new Error(
                'AUTH_REQUIRED'
            );


        error.statusCode =
            401;


        throw error;
    }


    return uid;
}


function societyWhatsAppError(
    res,
    error
) {

    const statusCode =
        error?.statusCode ||
        (
            error?.message ===
            'BOT_NOT_FOUND'
                ? 404
                : 500
        );


    return json(
        res,
        statusCode,
        {
            ok:
                false,

            error:
                error?.message ===
                'AUTH_REQUIRED'
                    ? 'Autenticação necessária.'
                    : (
                        error?.message ===
                        'BOT_NOT_FOUND'
                            ? 'Bot não encontrado.'
                            : 'Falha na sessão do WhatsApp.'
                    )
        }
    );
}


async function societyWhatsAppStatus(
    req,
    res,
    url
) {

    try {

        const uid =
            await societyWhatsAppUid(
                req
            );


        const botId =
            String(
                url.searchParams.get(
                    'botId'
                ) ||
                ''
            ).trim();


        if (!botId) {

            return json(
                res,
                400,
                {
                    ok:
                        false,

                    error:
                        'botId é obrigatório.'
                }
            );
        }


        const result =
            await societyWhatsApp.state(
                uid,
                botId
            );


        return json(
            res,
            200,
            {
                ok:
                    true,

                botId,

                ...result
            }
        );

    } catch (
        error
    ) {

        return societyWhatsAppError(
            res,
            error
        );
    }
}


async function societyWhatsAppLogs(
    req,
    res,
    url
) {

    try {

        const uid =
            await societyWhatsAppUid(
                req
            );


        const botId =
            String(
                url.searchParams.get(
                    'botId'
                ) ||
                ''
            ).trim();


        if (!botId) {

            return json(
                res,
                400,
                {
                    ok:
                        false,

                    error:
                        'botId é obrigatório.'
                }
            );
        }


        const result =
            await societyWhatsApp.logs(
                uid,
                botId,
                80
            );


        return json(
            res,
            200,
            {
                ok:
                    true,

                botId,

                logs:
                    result
            }
        );

    } catch (
        error
    ) {

        return societyWhatsAppError(
            res,
            error
        );
    }
}


async function societyWhatsAppControl(
    req,
    res,
    action
) {

    try {

        const uid =
            await societyWhatsAppUid(
                req
            );


        const body =
            await readJson(
                req
            );


        const botId =
            String(
                body?.botId ||
                ''
            ).trim();


        if (!botId) {

            return json(
                res,
                400,
                {
                    ok:
                        false,

                    error:
                        'botId é obrigatório.'
                }
            );
        }


        let result;


        if (
            action ===
            'connect'
        ) {

            result =
                await societyWhatsApp.connect(
                    uid,
                    botId
                );

        } else if (
            action ===
            'disconnect'
        ) {

            result =
                await societyWhatsApp.disconnect(
                    uid,
                    botId
                );

        } else if (
            action ===
            'restart'
        ) {

            result =
                await societyWhatsApp.restart(
                    uid,
                    botId
                );

        } else if (
            action ===
            'delete-session'
        ) {

            result =
                await societyWhatsApp.deleteSession(
                    uid,
                    botId
                );

        } else {

            return json(
                res,
                400,
                {
                    ok:
                        false,

                    error:
                        'Ação inválida.'
                }
            );
        }


        return json(
            res,
            200,
            {
                ok:
                    true,

                action,

                botId,

                result
            }
        );

    } catch (
        error
    ) {

        return societyWhatsAppError(
            res,
            error
        );
    }
}


/* ============================================================
 * ACCOUNT BOOTSTRAP
 * ============================================================ */

function bearerToken(req) {

    const authorization =
        String(
            req.headers.authorization ||
            ''
        );

    const match =
        authorization.match(
            /^Bearer\s+(.+)$/i
        );

    return (
        match?.[1] ||
        null
    );
}


async function firebaseUserFromToken(
    idToken
) {

    if (!idToken) {

        const error =
            new Error(
                'AUTH_REQUIRED'
            );

        error.statusCode =
            401;

        throw error;
    }


    const data =
        await firebaseRequest(
            'accounts:lookup',
            {
                idToken
            }
        );


    const user =
        data.users?.[0];


    if (!user) {

        const error =
            new Error(
                'INVALID_SESSION'
            );

        error.statusCode =
            401;

        throw error;
    }


    return user;
}


async function readJsonFile(
    file,
    fallback = {}
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


async function cloudBotsForUid(
    uid
) {

    const userDir =
        path.join(
            '/home/server/society-bots',
            uid
        );


    const names =
        await fsp.readdir(
            userDir
        ).catch(
            () => []
        );


    const bots = [];


    for (
        const directoryName
        of names
    ) {

        const dir =
            path.join(
                userDir,
                directoryName
            );


        const stat =
            await fsp.stat(
                dir
            ).catch(
                () => null
            );


        if (
            !stat ||
            !stat.isDirectory()
        ) {
            continue;
        }


        const config =
            await readJsonFile(
                path.join(
                    dir,
                    'config.json'
                ),
                null
            );


        if (!config) {
            continue;
        }


        const statusData =
            await readJsonFile(
                path.join(
                    dir,
                    'data',
                    'status.json'
                ),
                {
                    status:
                        'disconnected'
                }
            );


        bots.push({
            id:
                directoryName,

            serverBotId:
                directoryName,

            name:
                String(
                    config.name ||
                    directoryName
                ),

            owner:
                String(
                    config.owner ||
                    ''
                ),

            architecture:
                String(
                    config.architecture ||
                    ''
                ),

            library:
                String(
                    config.library ||
                    ''
                ),

            authMode:
                String(
                    config.authMode ||
                    ''
                ),

            prefix:
                String(
                    config.prefix ||
                    '/'
                ),

            database:
                String(
                    config.database ||
                    'json'
                ),

            features:
                Array.isArray(
                    config.features
                )
                    ? config.features
                    : [],

            style:
                String(
                    config.style ||
                    ''
                ),

            status:
                String(
                    statusData.status ||
                    'disconnected'
                ),

            archived:
                false,

            updatedAt:
                Date.now()
        });
    }


    return bots;
}




async function refreshFirebaseSession(
    req,
    res
) {

    try {

        const body =
            await readJson(
                req
            );


        const refreshToken =
            String(
                body.refreshToken ||
                ''
            ).trim();


        if (
            !refreshToken
        ) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'Refresh token ausente.'
                }
            );
        }


        const apiKey =
            process.env
                .FIREBASE_API_KEY;


        if (
            !apiKey
        ) {

            console.error(
                '[SOCIETY][REFRESH] FIREBASE_API_KEY ausente'
            );


            return json(
                res,
                500,
                {
                    ok: false,
                    error:
                        'Configuração Firebase indisponível.'
                }
            );
        }


        const params =
            new URLSearchParams();

        params.set(
            'grant_type',
            'refresh_token'
        );

        params.set(
            'refresh_token',
            refreshToken
        );


        const response =
            await fetch(
                'https://securetoken.googleapis.com/v1/token' +
                '?key=' +
                encodeURIComponent(
                    apiKey
                ),
                {
                    method:
                        'POST',

                    headers: {
                        'Content-Type':
                            'application/x-www-form-urlencoded'
                    },

                    body:
                        params.toString()
                }
            );


        const data =
            await response
                .json()
                .catch(
                    () => ({})
                );


        if (
            !response.ok ||
            !data.id_token
        ) {

            console.error(
                '[SOCIETY][REFRESH]',
                data?.error?.message ||
                response.status
            );


            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Sua sessão expirou. Entre novamente.'
                }
            );
        }


        return json(
            res,
            200,
            {
                ok: true,

                auth: {
                    idToken:
                        data.id_token,

                    refreshToken:
                        data.refresh_token ||
                        refreshToken,

                    expiresIn:
                        Number(
                            data.expires_in ||
                            3600
                        ),

                    uid:
                        data.user_id ||
                        ''
                }
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][REFRESH]',
            error.message
        );


        return json(
            res,
            500,
            {
                ok: false,
                error:
                    'Não foi possível renovar a sessão.'
            }
        );
    }
}



async function accountProfileUpdate(
    req,
    res
) {

    try {

        const idToken =
            bearerToken(
                req
            );


        const firebaseUser =
            await firebaseUserFromToken(
                idToken
            );


        const uid =
            firebaseUser.localId;


        const body =
            await readJson(
                req
            );


        let profile =
            await societyFirestore
                .getProfile(
                    uid,
                    idToken
                );


        profile =
            profile ||
            {};


        const displayName =
            String(
                body.displayName ??
                profile.displayName ??
                firebaseUser.displayName ??
                ''
            )
                .trim()
                .slice(
                    0,
                    80
                );


        let avatarUrl =
            String(
                profile.avatarUrl ||
                ''
            );


        const avatarBase64 =
            String(
                body.avatarBase64 ||
                ''
            ).trim();


        if (
            avatarBase64
        ) {

            const fsLocal =
                require(
                    'fs'
                );

            const pathLocal =
                require(
                    'path'
                );


            const buffer =
                Buffer.from(
                    avatarBase64,
                    'base64'
                );


            if (
                !buffer.length
            ) {

                return json(
                    res,
                    400,
                    {
                        ok: false,
                        error:
                            'A foto enviada é inválida.'
                    }
                );
            }


            if (
                buffer.length >
                900 * 1024
            ) {

                return json(
                    res,
                    413,
                    {
                        ok: false,
                        error:
                            'A foto do perfil ficou grande demais.'
                    }
                );
            }


            const safeUid =
                String(
                    uid
                ).replace(
                    /[^a-zA-Z0-9_-]/g,
                    '_'
                );


            const directory =
                pathLocal.join(
                    '/home/server/projetos/SocietyBots/backend/data/profile-media',
                    safeUid
                );


            fsLocal.mkdirSync(
                directory,
                {
                    recursive:
                        true
                }
            );


            const file =
                pathLocal.join(
                    directory,
                    'avatar.jpg'
                );


            fsLocal.writeFileSync(
                file,
                buffer
            );


            avatarUrl =
                'https://android-studio.cloudpaniel.com.br' +
                '/api/society/account/avatar' +
                '?uid=' +
                encodeURIComponent(
                    uid
                ) +
                '&v=' +
                Date.now();
        }


        const now =
            Date.now();


        const savedProfile = {
            ...profile,

            displayName,

            email:
                firebaseUser.email ||
                profile.email ||
                '',

            avatarUrl,

            createdAt:
                Number(
                    profile.createdAt ||
                    now
                ),

            updatedAt:
                now
        };


        await societyFirestore
            .saveProfile(
                uid,
                idToken,
                savedProfile
            );


        return json(
            res,
            200,
            {
                ok:
                    true,

                profile:
                    savedProfile
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][PROFILE UPDATE]',
            error.message
        );


        if (
            error.statusCode ===
                401 ||
            error.message ===
                'AUTH_REQUIRED' ||
            error.message ===
                'INVALID_SESSION'
        ) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Sua sessão expirou. Entre novamente.'
                }
            );
        }


        return json(
            res,
            500,
            {
                ok: false,
                error:
                    'Não foi possível salvar seu perfil.'
            }
        );
    }
}



function accountAvatar(
    req,
    res,
    url
) {

    try {

        const fsLocal =
            require(
                'fs'
            );

        const pathLocal =
            require(
                'path'
            );


        const uid =
            String(
                url.searchParams.get(
                    'uid'
                ) ||
                ''
            );


        if (
            !uid
        ) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'UID ausente.'
                }
            );
        }


        const safeUid =
            uid.replace(
                /[^a-zA-Z0-9_-]/g,
                '_'
            );


        const file =
            pathLocal.join(
                '/home/server/projetos/SocietyBots/backend/data/profile-media',
                safeUid,
                'avatar.jpg'
            );


        if (
            !fsLocal.existsSync(
                file
            )
        ) {

            return json(
                res,
                404,
                {
                    ok: false,
                    error:
                        'Foto não encontrada.'
                }
            );
        }


        const buffer =
            fsLocal.readFileSync(
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
            buffer.length
        );

        res.setHeader(
            'Cache-Control',
            'public, max-age=31536000, immutable'
        );

        res.setHeader(
            'X-Content-Type-Options',
            'nosniff'
        );

        res.end(
            buffer
        );

        return true;

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][AVATAR]',
            error.message
        );


        return json(
            res,
            500,
            {
                ok: false,
                error:
                    'Não foi possível carregar a foto.'
            }
        );
    }
}


async function accountBotUpsert(
    req,
    res
) {

    try {

        const idToken =
            bearerToken(req);

        const firebaseUser =
            await firebaseUserFromToken(
                idToken
            );

        const uid =
            firebaseUser.localId;

        const body =
            await readJson(req);

        const botId =
            String(
                body.id ||
                body.botId ||
                ''
            ).trim();


        if (!botId) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'ID do bot é obrigatório.'
                }
            );
        }


        /*
         * Nunca confiamos em UID enviado pelo cliente.
         * O dono vem exclusivamente do Bearer Firebase.
         */
        let existing =
            null;

        try {

            const bots =
                await societyFirestore
                    .listBots(
                        uid,
                        idToken
                    );

            existing =
                bots.find(
                    item =>
                        item.id ===
                        botId
                ) || null;

        } catch (error) {

            if (
                error.statusCode !==
                404
            ) {
                throw error;
            }
        }


        const now =
            Date.now();


        const bot = {
            ...(existing || {}),

            id:
                botId,

            serverBotId:
                String(
                    body.serverBotId ||
                    botId
                ),

            name:
                String(
                    body.name ||
                    existing?.name ||
                    ''
                ),

            owner:
                String(
                    body.owner ??
                    existing?.owner ??
                    ''
                ),

            architecture:
                String(
                    body.architecture ||
                    existing?.architecture ||
                    'command-handler'
                ),

            library:
                String(
                    body.library ||
                    existing?.library ||
                    ''
                ),

            authMode:
                String(
                    body.authMode ||
                    existing?.authMode ||
                    'pairing'
                ),

            prefix:
                String(
                    body.prefix ||
                    existing?.prefix ||
                    '/'
                ),

            database:
                String(
                    body.database ||
                    existing?.database ||
                    'json'
                ),

            features:
                Array.isArray(
                    body.features
                )
                    ? body.features
                    : Array.isArray(
                        existing?.features
                    )
                        ? existing.features
                        : [],

            style:
                String(
                    body.style ??
                    existing?.style ??
                    ''
                ),

            archived:
                typeof body.archived ===
                    'boolean'
                    ? body.archived
                    : Boolean(
                        existing?.archived
                    ),

            createdAt:
                Number(
                    existing?.createdAt ||
                    body.createdAt ||
                    now
                ),

            updatedAt:
                now
        };


        await societyFirestore
            .saveBot(
                uid,
                botId,
                idToken,
                bot
            );


        return json(
            res,
            200,
            {
                ok: true,
                message:
                    'Bot salvo na Cloud.',
                bot
            }
        );

    } catch (error) {

        console.error(
            '[SOCIETY][BOT UPSERT]',
            error.message
        );


        if (
            error.statusCode ===
                401 ||
            error.message ===
                'AUTH_REQUIRED' ||
            error.message ===
                'INVALID_SESSION'
        ) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Sua sessão expirou. Entre novamente.'
                }
            );
        }


        return json(
            res,
            500,
            {
                ok: false,
                error:
                    'Não foi possível salvar o bot na Cloud.'
            }
        );
    }
}


function backupDeletedBotDirectory(
    uid,
    botId
) {

    const fsLocal =
        require(
            'fs'
        );

    const pathLocal =
        require(
            'path'
        );


    const ownerRoot =
        pathLocal.resolve(
            '/home/server/society-bots',
            uid
        );


    const botPath =
        pathLocal.resolve(
            ownerRoot,
            botId
        );


    /*
     * Proteção contra path traversal.
     */
    if (
        pathLocal.dirname(
            botPath
        ) !==
        ownerRoot
    ) {

        throw new Error(
            'BOT_PATH_INVALID'
        );
    }


    if (
        !fsLocal.existsSync(
            botPath
        )
    ) {

        return null;
    }


    const deletedRoot =
        pathLocal.join(
            ownerRoot,
            '.deleted'
        );


    fsLocal.mkdirSync(
        deletedRoot,
        {
            recursive:
                true
        }
    );


    const safeBotId =
        pathLocal
            .basename(
                botId
            )
            .replace(
                /[^a-zA-Z0-9_.-]/g,
                '_'
            );


    const backupName =
        String(
            Date.now()
        ) +
        '__' +
        safeBotId;


    const backupPath =
        pathLocal.join(
            deletedRoot,
            backupName
        );


    fsLocal.renameSync(
        botPath,
        backupPath
    );


    return {
        backupName,
        backupPath
    };
}



async function accountBotDelete(
    req,
    res
) {

    try {

        const idToken =
            bearerToken(
                req
            );


        const firebaseUser =
            await firebaseUserFromToken(
                idToken
            );


        const uid =
            firebaseUser.localId;


        const body =
            await readJson(
                req
            );


        const botId =
            String(
                body.id ||
                body.botId ||
                ''
            ).trim();


        if (
            !botId
        ) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'ID do bot é obrigatório.'
                }
            );
        }


        /*
         * Para o processo antes de mover
         * o diretório para backup.
         */
        let stopWarning =
            null;


        try {

            await societyBotManager
                .stopBot(
                    uid,
                    botId
                );

        } catch (
            error
        ) {

            stopWarning =
                error.message;

            console.warn(
                '[SOCIETY][BOT DELETE][STOP]',
                botId,
                error.message
            );
        }


        /*
         * Move o bot para:
         *
         * /home/server/society-bots/<uid>/.deleted/
         *
         * Não usamos rm -rf.
         */
        let backup =
            null;

        let backupWarning =
            null;


        try {

            backup =
                backupDeletedBotDirectory(
                    uid,
                    botId
                );

        } catch (
            error
        ) {

            backupWarning =
                error.message;

            console.error(
                '[SOCIETY][BOT DELETE][BACKUP]',
                botId,
                error.message
            );
        }


        /*
         * Firestore é a fonte oficial.
         * Se apagou aqui, não existe mais
         * para o aplicativo.
         */
        try {

            await societyFirestore
                .deleteBot(
                    uid,
                    botId,
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


        console.log(
            '[SOCIETY][BOT DELETE]',
            {
                uid,
                botId,
                backup:
                    backup
                        ?.backupName ||
                    null
            }
        );


        return json(
            res,
            200,
            {
                ok:
                    true,

                deleted:
                    true,

                botId,

                backup:
                    backup
                        ?.backupName ||
                    null,

                warning:
                    backupWarning ||
                    stopWarning ||
                    null
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][BOT DELETE]',
            error.message
        );


        if (
            error.statusCode ===
                401 ||
            error.message ===
                'AUTH_REQUIRED' ||
            error.message ===
                'INVALID_SESSION'
        ) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Sua sessão expirou. Entre novamente.'
                }
            );
        }


        return json(
            res,
            500,
            {
                ok: false,
                error:
                    'Não foi possível remover o bot da Cloud.'
            }
        );
    }
}



/* ============================================================
 * SOCIETY AI - CHATS DA CONTA
 * ============================================================ */

async function readSocietyAiChatJson(
    req
) {

    let raw = '';


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
            2 * 1024 * 1024
        ) {

            const error =
                new Error(
                    'PAYLOAD_TOO_LARGE'
                );

            error.statusCode =
                413;

            throw error;
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

        const error =
            new Error(
                'INVALID_JSON'
            );

        error.statusCode =
            400;

        throw error;
    }
}


async function accountAiChatsList(
    req,
    res
) {

    try {

        const idToken =
            bearerToken(
                req
            );


        const firebaseUser =
            await firebaseUserFromToken(
                idToken
            );


        const uid =
            firebaseUser.localId;


        const chats =
            await societyFirestore
                .listAiChats(
                    uid,
                    idToken
                );


        return json(
            res,
            200,
            {
                ok: true,
                chats
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][AI CHATS LIST]',
            error.message
        );


        if (
            error.statusCode === 401 ||
            error.message === 'AUTH_REQUIRED' ||
            error.message === 'INVALID_SESSION'
        ) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Sua sessão expirou. Entre novamente.'
                }
            );
        }


        return json(
            res,
            500,
            {
                ok: false,
                error:
                    'Não foi possível carregar seus chats.'
            }
        );
    }
}


async function accountAiChatSave(
    req,
    res
) {

    try {

        const idToken =
            bearerToken(
                req
            );


        const firebaseUser =
            await firebaseUserFromToken(
                idToken
            );


        const uid =
            firebaseUser.localId;


        const body =
            await readSocietyAiChatJson(
                req
            );


        const chatId =
            String(
                body.id ||
                body.chatId ||
                ''
            )
                .trim();


        if (
            !chatId
        ) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'ID do chat é obrigatório.'
                }
            );
        }


        let messages =
            Array.isArray(
                body.messages
            )
                ? body.messages
                : [];


        /*
         * Mantém o documento muito abaixo do
         * limite de 1 MiB do Firestore.
         */
        messages =
            messages
                .slice(
                    -60
                )
                .map(
                    item => ({

                        role:
                            String(
                                item?.role ||
                                'user'
                            ) ===
                            'assistant'
                                ? 'assistant'
                                : 'user',

                        text:
                            String(
                                item?.text ||
                                ''
                            )
                                .slice(
                                    0,
                                    6000
                                ),

                        provider:
                            String(
                                item?.provider ||
                                ''
                            )
                                .slice(
                                    0,
                                    120
                                ),

                        time:
                            String(
                                item?.time ||
                                ''
                            )
                                .slice(
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


        if (
            !title
        ) {

            title =
                'Nova conversa';
        }


        const now =
            Date.now();


        const chat = {

            id:
                chatId,

            title,

            createdAt:
                Number(
                    body.createdAt ||
                    now
                ),

            updatedAt:
                now,

            messages
        };


        await societyFirestore
            .saveAiChat(
                uid,
                chatId,
                idToken,
                chat
            );


        return json(
            res,
            200,
            {
                ok: true,
                chat
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][AI CHAT SAVE]',
            error.message
        );


        if (
            error.statusCode === 400 ||
            error.message === 'INVALID_JSON'
        ) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'JSON inválido.'
                }
            );
        }


        if (
            error.statusCode === 413 ||
            error.message === 'PAYLOAD_TOO_LARGE'
        ) {

            return json(
                res,
                413,
                {
                    ok: false,
                    error:
                        'Conversa grande demais.'
                }
            );
        }


        if (
            error.statusCode === 401 ||
            error.message === 'AUTH_REQUIRED' ||
            error.message === 'INVALID_SESSION'
        ) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Sua sessão expirou. Entre novamente.'
                }
            );
        }


        return json(
            res,
            500,
            {
                ok: false,
                error:
                    'Não foi possível salvar a conversa.'
            }
        );
    }
}


async function accountAiChatDelete(
    req,
    res
) {

    try {

        const idToken =
            bearerToken(
                req
            );


        const firebaseUser =
            await firebaseUserFromToken(
                idToken
            );


        const uid =
            firebaseUser.localId;


        const body =
            await readSocietyAiChatJson(
                req
            );


        const chatId =
            String(
                body.id ||
                body.chatId ||
                ''
            )
                .trim();


        if (
            !chatId
        ) {

            return json(
                res,
                400,
                {
                    ok: false,
                    error:
                        'ID do chat é obrigatório.'
                }
            );
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


        return json(
            res,
            200,
            {
                ok: true
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][AI CHAT DELETE]',
            error.message
        );


        if (
            error.statusCode === 401 ||
            error.message === 'AUTH_REQUIRED' ||
            error.message === 'INVALID_SESSION'
        ) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Sua sessão expirou. Entre novamente.'
                }
            );
        }


        return json(
            res,
            500,
            {
                ok: false,
                error:
                    'Não foi possível excluir a conversa.'
            }
        );
    }
}


async function accountBootstrap(
    req,
    res
) {

    try {

        const idToken =
            bearerToken(
                req
            );


        const firebaseUser =
            await firebaseUserFromToken(
                idToken
            );


        const uid =
            firebaseUser.localId;


        /*
         * PERFIL
         */
        let profile =
            await societyFirestore
                .getProfile(
                    uid,
                    idToken
                );


        if (!profile) {

            profile = {
                displayName:
                    firebaseUser.displayName ||
                    '',

                email:
                    firebaseUser.email ||
                    '',

                avatarUrl:
                    '',

                createdAt:
                    Date.now(),

                updatedAt:
                    Date.now()
            };


            await societyFirestore
                .saveProfile(
                    uid,
                    idToken,
                    profile
                );
        }


        /*
         * FIRESTORE É A FONTE DA VERDADE.
         *
         * Não recriamos mais automaticamente bots
         * existentes no filesystem.
         *
         * Isso impede que um bot apagado no app
         * "ressuscite" no próximo login.
         */
        let firestoreBots = [];

        try {

            firestoreBots =
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


        /*
         * O filesystem agora serve SOMENTE para
         * informações operacionais/status.
         */
        const cloudBots =
            await cloudBotsForUid(
                uid
            );


        /*
         * Sempre preferimos status real da Cloud.
         */
        const cloudById =
            new Map(
                cloudBots.map(
                    bot => [
                        bot.id,
                        bot
                    ]
                )
            );


        const bots =
            firestoreBots.map(
                bot => {

                    const cloud =
                        cloudById.get(
                            bot.id
                        );


                    return cloud
                        ? {
                            ...bot,

                            /*
                             * Apenas informação operacional vem
                             * do filesystem/runtime.
                             */
                            status:
                                String(
                                    cloud.status ||
                                    'disconnected'
                                ),

                            serverBotId:
                                String(
                                    cloud.serverBotId ||
                                    bot.serverBotId ||
                                    bot.id
                                )
                        }
                        : {
                            ...bot,
                            status:
                                'disconnected'
                        };
                }
            );


        return json(
            res,
            200,
            {
                ok: true,

                user: {
                    uid,

                    email:
                        firebaseUser.email ||
                        '',

                    displayName:
                        firebaseUser.displayName ||
                        profile.displayName ||
                        '',

                    emailVerified:
                        Boolean(
                            firebaseUser.emailVerified
                        )
                },

                profile,

                bots
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][BOOTSTRAP]',
            error.message
        );


        if (
            error.statusCode ===
            401 ||
            error.message ===
            'AUTH_REQUIRED' ||
            error.message ===
            'INVALID_SESSION'
        ) {

            return json(
                res,
                401,
                {
                    ok: false,
                    error:
                        'Sua sessão expirou. Entre novamente.'
                }
            );
        }


        return json(
            res,
            500,
            {
                ok: false,

                error:
                    'Não foi possível sincronizar sua conta.'
            }
        );
    }
}


/* ============================================================
 * ROUTER NATIVO
 * ============================================================ */

function handle(
    req,
    res,
    parsedUrl = null
) {

    const url =
        parsedUrl ||
        new URL(
            req.url,
            'http://127.0.0.1'
        );


    const pathname =
        url.pathname;


    if (
        !pathname.startsWith(
            '/api/society'
        )
    ) {

        return false;
    }


    /*
     * Retornamos true imediatamente para avisar
     * terminal-server.js que essa requisição pertence
     * ao SocietyBots.
     */

    (async () => {

        try {

            /* SOCIETY_APP_UPDATE_ROUTER */
            if (
                pathname ===
                    '/api/society/app/version'
            ) {

                await societyUpdateRouter
                    .version(
                        req,
                        res
                    );

                return;
            }


            if (
                pathname ===
                    '/api/society/app/apk'
            ) {

                await societyUpdateRouter
                    .apk(
                        req,
                        res
                    );

                return;
            }


            if (
                pathname ===
                '/api/society/health'
            ) {

                if (
                    req.method !==
                    'GET'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }


                return json(
                    res,
                    200,
                    {
                        ok: true,

                        service:
                            'Society Bots API',

                        firebaseProject:
                            FIREBASE_PROJECT_ID
                    }
                );
            }


            if (
                pathname ===
                '/api/society/teste'
            ) {

                return json(
                    res,
                    200,
                    {
                        ok: true,

                        message:
                            'SocietyBots funcionando pela porta principal.'
                    }
                );
            }


            if (
                pathname ===
                    '/api/society/auth/resend-verification'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {

                    return methodNotAllowed(
                        res
                    );
                }


                return resendVerification(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/builder/start'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                if (
                    !await requireVerifiedEmail(
                        req,
                        res
                    )
                ) {
                    return;
                }


                return builderStart(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/builder/answer'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                if (
                    !await requireVerifiedEmail(
                        req,
                        res
                    )
                ) {
                    return;
                }


                return builderAnswer(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/builder/build'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                if (
                    !await requireVerifiedEmail(
                        req,
                        res
                    )
                ) {
                    return;
                }


                return builderBuild(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/builder/events'
            ) {

                if (
                    req.method !==
                    'GET'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                return builderEvents(
                    req,
                    res,
                    url
                );
            }


            if (
                pathname ===
                '/api/society/ai/chat'
            ) {

                societyAi
                    .handleChat(
                        req,
                        res
                    );

                return true;
            }


            if (
                pathname ===
                '/api/society/ai/command-draft'
            ) {

                societyAi
                    .handleCommandDraft(
                        req,
                        res
                    );

                return true;
            }


            if (
                pathname.startsWith(
                    '/api/society/ai/media/'
                )
            ) {

                societyAi
                    .serveMedia(
                        req,
                        res,
                        pathname
                    );

                return true;
            }


            if (
                pathname ===
                '/api/society/auth/refresh'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {

                    json(
                        res,
                        405,
                        {
                            ok: false,
                            error:
                                'Método não permitido.'
                        }
                    );

                    return true;
                }


                refreshFirebaseSession(
                    req,
                    res
                );

                return true;
            }


            if (
                pathname ===
                '/api/society/account/profile'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {

                    json(
                        res,
                        405,
                        {
                            ok: false,
                            error:
                                'Método não permitido.'
                        }
                    );

                    return true;
                }


                accountProfileUpdate(
                    req,
                    res
                );

                return true;
            }


            if (
                pathname ===
                '/api/society/account/avatar'
            ) {

                if (
                    req.method !==
                    'GET'
                ) {

                    json(
                        res,
                        405,
                        {
                            ok: false,
                            error:
                                'Método não permitido.'
                        }
                    );

                    return true;
                }


                accountAvatar(
                    req,
                    res,
                    url
                );

                return true;
            }


            if (
                pathname ===
                '/api/society/account/bots/upsert'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }


                return accountBotUpsert(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/account/bots/delete'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }


                return accountBotDelete(
                    req,
                    res
                );
            }



            if (
                pathname ===
                    '/api/society/account/ai/chats'
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
                                'Método não permitido.'
                        }
                    );
                }


                return accountAiChatsList(
                    req,
                    res
                );
            }


            if (
                pathname ===
                    '/api/society/account/ai/chats/save'
            ) {

                if (
                    req.method !==
                        'POST'
                ) {

                    return json(
                        res,
                        405,
                        {
                            ok: false,
                            error:
                                'Método não permitido.'
                        }
                    );
                }


                return accountAiChatSave(
                    req,
                    res
                );
            }


            if (
                pathname ===
                    '/api/society/account/ai/chats/delete'
            ) {

                if (
                    req.method !==
                        'POST'
                ) {

                    return json(
                        res,
                        405,
                        {
                            ok: false,
                            error:
                                'Método não permitido.'
                        }
                    );
                }


                return accountAiChatDelete(
                    req,
                    res
                );
            }




            /* SOCIETY_CDN_BRIDGE_ROUTER */
            if (
                pathname ===
                    '/api/society/cdn/upload'
            ) {

                societyCdnBridge
                    .upload(
                        req,
                        res
                    )
                    .catch(
                        error => {

                            console.error(
                                '[SOCIETY][CDN UPLOAD ROUTER]',
                                error
                            );

                            if (
                                !res.headersSent
                            ) {

                                res.statusCode =
                                    500;

                                res.setHeader(
                                    'Content-Type',
                                    'application/json; charset=utf-8'
                                );

                                res.end(
                                    JSON.stringify({
                                        ok: false,
                                        error:
                                            'Falha interna na Society CDN.'
                                    })
                                );
                            }
                        }
                    );

                return true;
            }


            if (
                pathname.startsWith(
                    '/api/society/cdn/file/'
                )
            ) {

                societyCdnBridge
                    .download(
                        req,
                        res
                    )
                    .catch(
                        error => {

                            console.error(
                                '[SOCIETY][CDN DOWNLOAD ROUTER]',
                                error
                            );

                            if (
                                !res.headersSent
                            ) {

                                res.statusCode =
                                    500;

                                res.setHeader(
                                    'Content-Type',
                                    'application/json; charset=utf-8'
                                );

                                res.end(
                                    JSON.stringify({
                                        ok: false,
                                        error:
                                            'Falha interna na Society CDN.'
                                    })
                                );
                            }
                        }
                    );

                return true;
            }


            /* SOCIETY_BOT_IMPORT_ROUTER */
            if (
                pathname ===
                    '/api/society/account/bots/import'
            ) {

                societyBotImportRouter(
                    req,
                    res
                ).catch(
                    error => {

                        console.error(
                            '[SOCIETY][IMPORT ROUTER]',
                            error
                        );

                        if (
                            !res.headersSent
                        ) {

                            res.statusCode =
                                500;

                            res.setHeader(
                                'Content-Type',
                                'application/json; charset=utf-8'
                            );

                            res.end(
                                JSON.stringify({
                                    ok: false,
                                    error:
                                        'Falha interna ao importar bot.'
                                })
                            );
                        }
                    }
                );

                return true;
            }


            /* SOCIETY_BOT_AVATAR_ROUTER */
            if (
                pathname ===
                    '/api/society/account/bots/avatar'
            ) {

                societyBotAvatarRouter(
                    req,
                    res
                ).catch(
                    error => {

                        console.error(
                            '[SOCIETY][BOT AVATAR ROUTER]',
                            error
                        );

                        if (
                            !res.headersSent
                        ) {

                            res.statusCode =
                                500;

                            res.setHeader(
                                'Content-Type',
                                'application/json; charset=utf-8'
                            );

                            res.end(
                                JSON.stringify({
                                    ok: false,
                                    error:
                                        'Falha interna na foto do bot.'
                                })
                            );
                        }
                    }
                );

                return true;
            }


            if (
                pathname ===
                '/api/society/account/bootstrap'
            ) {

                if (
                    req.method !==
                    'GET'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }


                return accountBootstrap(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/auth/register'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                return register(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/auth/login'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                return login(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/auth/forgot-password'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                return forgotPassword(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/auth/me'
            ) {

                if (
                    req.method !==
                    'GET'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                return me(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/whatsapp/status'
            ) {

                if (
                    req.method !==
                    'GET'
                ) {

                    return methodNotAllowed(
                        res
                    );
                }


                return societyWhatsAppStatus(
                    req,
                    res,
                    url
                );
            }


            if (
                pathname ===
                '/api/society/whatsapp/logs'
            ) {

                if (
                    req.method !==
                    'GET'
                ) {

                    return methodNotAllowed(
                        res
                    );
                }


                return societyWhatsAppLogs(
                    req,
                    res,
                    url
                );
            }


            if (
                pathname ===
                '/api/society/whatsapp/connect'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {

                    return methodNotAllowed(
                        res
                    );
                }


                return societyWhatsAppControl(
                    req,
                    res,
                    'connect'
                );
            }


            if (
                pathname ===
                '/api/society/whatsapp/disconnect'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {

                    return methodNotAllowed(
                        res
                    );
                }


                return societyWhatsAppControl(
                    req,
                    res,
                    'disconnect'
                );
            }


            if (
                pathname ===
                '/api/society/whatsapp/restart'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {

                    return methodNotAllowed(
                        res
                    );
                }


                return societyWhatsAppControl(
                    req,
                    res,
                    'restart'
                );
            }


            if (
                pathname ===
                '/api/society/whatsapp/delete-session'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {

                    return methodNotAllowed(
                        res
                    );
                }


                return societyWhatsAppControl(
                    req,
                    res,
                    'delete-session'
                );
            }


            if (
                pathname ===
                '/api/society/bots/status'
            ) {

                if (
                    req.method !==
                    'GET'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                return botStatus(
                    req,
                    res,
                    url
                );
            }


            if (
                pathname ===
                '/api/society/bots/logs'
            ) {

                if (req.method !== 'GET') {
                    return methodNotAllowed(res);
                }

                return botLogs(
                    req,
                    res,
                    url
                );
            }


            if (
                pathname ===
                '/api/society/bots/start'
            ) {

                if (req.method !== 'POST') {
                    return methodNotAllowed(res);
                }

                return botControl(
                    req,
                    res,
                    'start'
                );
            }


            if (
                pathname ===
                '/api/society/bots/stop'
            ) {

                if (req.method !== 'POST') {
                    return methodNotAllowed(res);
                }

                return botControl(
                    req,
                    res,
                    'stop'
                );
            }


            if (
                pathname ===
                '/api/society/bots/restart'
            ) {

                if (req.method !== 'POST') {
                    return methodNotAllowed(res);
                }

                return botControl(
                    req,
                    res,
                    'restart'
                );
            }


            if (
                pathname ===
                '/api/society/bots/commands'
            ) {

                if (
                    req.method !==
                    'GET'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                return botCommands(
                    req,
                    res,
                    url
                );
            }


            if (
                pathname ===
                '/api/society/bots/commands/create'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                return createBotCommand(
                    req,
                    res
                );
            }


            if (
                pathname ===
                '/api/society/bots/commands/delete'
            ) {

                if (
                    req.method !==
                    'POST'
                ) {
                    return methodNotAllowed(
                        res
                    );
                }

                return deleteBotCommand(
                    req,
                    res
                );
            }


            return json(
                res,
                404,
                {
                    ok: false,
                    error:
                        'Endpoint SocietyBots não encontrado.'
                }
            );

        } catch (error) {

            console.error(
                '[SOCIETY API]',
                error
            );


            if (
                !res.headersSent
            ) {

                return json(
                    res,
                    500,
                    {
                        ok: false,
                        error:
                            'Erro interno da API SocietyBots.'
                    }
                );
            }


            try {
                res.end();
            } catch {}
        }

    })();


    return true;
}


module.exports = {
    handle
};
