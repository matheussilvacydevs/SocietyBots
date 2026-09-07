const express = require('express');

const router = express.Router();

const FIREBASE_API_KEY = process.env.FIREBASE_API_KEY;

if (!FIREBASE_API_KEY) {
    throw new Error('FIREBASE_API_KEY não configurada.');
}

const FIREBASE_BASE =
    'https://identitytoolkit.googleapis.com/v1';

function firebaseUrl(endpoint) {
    return `${FIREBASE_BASE}/${endpoint}?key=${encodeURIComponent(FIREBASE_API_KEY)}`;
}

async function firebaseRequest(endpoint, body) {

    const response = await fetch(
        firebaseUrl(endpoint),
        {
            method: 'POST',

            headers: {
                'Content-Type': 'application/json'
            },

            body: JSON.stringify(body)
        }
    );

    const data = await response.json()
        .catch(() => ({}));

    if (!response.ok) {

        const firebaseCode =
            data?.error?.message ||
            `HTTP_${response.status}`;

        const error =
            new Error(firebaseCode);

        error.firebaseCode =
            firebaseCode;

        error.status =
            response.status;

        throw error;
    }

    return data;
}

function traduzirErroFirebase(code) {

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
            'Login com e-mail e senha ainda não está habilitado no Firebase.',

        WEAK_PASSWORD:
            'A senha é muito fraca.',

        MISSING_PASSWORD:
            'Informe sua senha.',

        MISSING_EMAIL:
            'Informe seu e-mail.'
    };

    return (
        errors[normalized] ||
        'Não foi possível concluir a solicitação.'
    );
}

function validarEmail(email) {

    return (
        typeof email === 'string' &&
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(
            email.trim()
        )
    );
}

function sanitizarUsuario(data) {

    return {
        uid:
            data.localId || null,

        email:
            data.email || null,

        displayName:
            data.displayName || null,

        emailVerified:
            Boolean(data.emailVerified)
    };
}

/* ============================================================
 * CADASTRO
 * ============================================================ */

router.post(
    '/register',
    async (req, res) => {

        try {

            const {
                name,
                email,
                password
            } = req.body || {};

            if (
                typeof name !== 'string' ||
                name.trim().length < 2
            ) {

                return res.status(400).json({
                    ok: false,
                    error: 'Informe seu nome.'
                });
            }

            if (!validarEmail(email)) {

                return res.status(400).json({
                    ok: false,
                    error: 'Informe um e-mail válido.'
                });
            }

            if (
                typeof password !== 'string' ||
                password.length < 6
            ) {

                return res.status(400).json({
                    ok: false,
                    error:
                        'A senha precisa ter pelo menos 6 caracteres.'
                });
            }

            const created =
                await firebaseRequest(
                    'accounts:signUp',
                    {
                        email:
                            email.trim().toLowerCase(),

                        password,

                        returnSecureToken: true
                    }
                );

            /*
             * Atualiza displayName depois de criar.
             */

            const updated =
                await firebaseRequest(
                    'accounts:update',
                    {
                        idToken:
                            created.idToken,

                        displayName:
                            name.trim(),

                        returnSecureToken: true
                    }
                );

            return res.status(201).json({

                ok: true,

                message:
                    'Conta criada com sucesso.',

                user:
                    sanitizarUsuario(updated),

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
            });

        } catch (error) {

            console.error(
                '[AUTH][REGISTER]',
                error.firebaseCode ||
                error.message
            );

            return res.status(400).json({
                ok: false,
                error:
                    traduzirErroFirebase(
                        error.firebaseCode
                    )
            });
        }
    }
);

/* ============================================================
 * LOGIN
 * ============================================================ */

router.post(
    '/login',
    async (req, res) => {

        try {

            const {
                email,
                password
            } = req.body || {};

            if (!validarEmail(email)) {

                return res.status(400).json({
                    ok: false,
                    error:
                        'Informe um e-mail válido.'
                });
            }

            if (
                typeof password !== 'string' ||
                !password
            ) {

                return res.status(400).json({
                    ok: false,
                    error:
                        'Informe sua senha.'
                });
            }

            const data =
                await firebaseRequest(
                    'accounts:signInWithPassword',
                    {
                        email:
                            email.trim().toLowerCase(),

                        password,

                        returnSecureToken:
                            true
                    }
                );

            return res.json({

                ok: true,

                message:
                    'Login realizado com sucesso.',

                user:
                    sanitizarUsuario(data),

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
            });

        } catch (error) {

            console.error(
                '[AUTH][LOGIN]',
                error.firebaseCode ||
                error.message
            );

            return res.status(401).json({
                ok: false,
                error:
                    traduzirErroFirebase(
                        error.firebaseCode
                    )
            });
        }
    }
);

/* ============================================================
 * REDEFINIÇÃO DE SENHA
 * ============================================================ */

router.post(
    '/forgot-password',
    async (req, res) => {

        try {

            const {
                email
            } = req.body || {};

            if (!validarEmail(email)) {

                return res.status(400).json({
                    ok: false,
                    error:
                        'Informe um e-mail válido.'
                });
            }

            await firebaseRequest(
                'accounts:sendOobCode',
                {
                    requestType:
                        'PASSWORD_RESET',

                    email:
                        email.trim().toLowerCase()
                }
            );

            /*
             * Resposta genérica de propósito.
             * Evita revelar se determinado e-mail
             * possui conta cadastrada.
             */

            return res.json({

                ok: true,

                message:
                    'Se esse e-mail estiver cadastrado, você receberá as instruções para redefinir sua senha.'
            });

        } catch (error) {

            /*
             * EMAIL_NOT_FOUND também retorna sucesso genérico.
             */

            if (
                error.firebaseCode ===
                'EMAIL_NOT_FOUND'
            ) {

                return res.json({

                    ok: true,

                    message:
                        'Se esse e-mail estiver cadastrado, você receberá as instruções para redefinir sua senha.'
                });
            }

            console.error(
                '[AUTH][RESET]',
                error.firebaseCode ||
                error.message
            );

            return res.status(400).json({
                ok: false,
                error:
                    traduzirErroFirebase(
                        error.firebaseCode
                    )
            });
        }
    }
);

/* ============================================================
 * USUÁRIO ATUAL
 * ============================================================ */

router.get(
    '/me',
    async (req, res) => {

        try {

            const authorization =
                req.headers.authorization || '';

            const match =
                authorization.match(
                    /^Bearer\s+(.+)$/i
                );

            if (!match) {

                return res.status(401).json({
                    ok: false,
                    error:
                        'Token de autenticação ausente.'
                });
            }

            const idToken =
                match[1];

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

                return res.status(401).json({
                    ok: false,
                    error:
                        'Sessão inválida.'
                });
            }

            return res.json({

                ok: true,

                user:
                    sanitizarUsuario(user)
            });

        } catch (error) {

            return res.status(401).json({
                ok: false,
                error:
                    'Sua sessão expirou ou não é válida.'
            });
        }
    }
);

module.exports = router;
