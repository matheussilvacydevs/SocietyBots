'use strict';

const fs =
    require('fs');

const path =
    require('path');

const crypto =
    require('crypto');

const {
    execFileSync
} =
    require('child_process');


/* ============================================================
 * ENV LOCAL
 * ============================================================ */

function loadLocalEnv() {

    const file =
        path.join(
            __dirname,
            '.env'
        );


    if (
        !fs.existsSync(
            file
        )
    ) {
        return;
    }


    const raw =
        fs.readFileSync(
            file,
            'utf8'
        );


    for (
        const line
        of raw.split(/\r?\n/)
    ) {

        const trimmed =
            line.trim();


        if (
            !trimmed ||
            trimmed.startsWith(
                '#'
            )
        ) {
            continue;
        }


        const index =
            trimmed.indexOf(
                '='
            );


        if (
            index <=
            0
        ) {
            continue;
        }


        const key =
            trimmed
                .slice(
                    0,
                    index
                )
                .trim();


        const value =
            trimmed
                .slice(
                    index + 1
                )
                .trim();


        if (
            !process.env[key]
        ) {
            process.env[key] =
                value;
        }
    }
}


loadLocalEnv();


const FIREBASE_API_KEY =
    process.env.FIREBASE_API_KEY ||
    '';

const CLOUD_KEY =
    process.env.CLOUDPANIEL_AI_KEY ||
    '';

const ZONE_KEY =
    process.env.ZONE_API_KEY ||
    '';

const PUBLIC_BASE =
    process.env.SOCIETY_PUBLIC_BASE ||
    'https://android-studio.cloudpaniel.com.br';

const MEDIA_ROOT =
    path.join(
        '/home/server',
        'society-ai-media'
    );


fs.mkdirSync(
    MEDIA_ROOT,
    {
        recursive: true
    }
);


/* ============================================================
 * PROVEDORES
 * ============================================================ */

const PROVIDERS = [

    {
        nome:
            'CloudPaniel DeepSeek Visão',

        url:
            'https://api.cloudpaniel.com.br/api/ai/deepseek/visao',

        metodo:
            'POST',

        paramTexto:
            'prompt',

        paramImagem:
            'url',

        grupo:
            'cloud',

        authHeader:
            true,

        imagem:
            true
    },

    {
        nome:
            'CloudPaniel Gemini',

        url:
            'https://api.cloudpaniel.com.br/api/ai/gemini',

        metodo:
            'POST',

        paramTexto:
            'prompt',

        paramImagem:
            'url',

        grupo:
            'cloud',

        authHeader:
            true,

        imagem:
            true
    },


    {
        nome:
            'CloudPaniel Claude Haiku 4.5',

        url:
            'https://api.cloudpaniel.com.br/api/ai/claude-haiku-4-5',

        metodo:
            'GET',

        paramTexto:
            'prompt',

        paramApiKey:
            'Token',

        grupo:
            'cloud'
    },

    {
        nome:
            'CloudPaniel Claude Haiku 4.5 Max',

        url:
            'https://api.cloudpaniel.com.br/api/ai/claude-haiku-4-5-max',

        metodo:
            'GET',

        paramTexto:
            'prompt',

        paramApiKey:
            'Token',

        grupo:
            'cloud'
    },


    ...[
        [
            'CloudPaniel DeepSeek Expert',
            'deepseek/expert'
        ],

        [
            'CloudPaniel ChatGPT-5.5-Pro',
            'chatgpt-5.5-pro'
        ],

        [
            'CloudPaniel ChatGPT-5.5',
            'chatgpt-5.5'
        ],

        [
            'CloudPaniel DeepSeek',
            'deepseek'
        ],

        [
            'CloudPaniel DeepSeek Search',
            'deepseek/search'
        ],

        [
            'CloudPaniel ChatGPT-5.1-mini',
            'chatgpt-5.1-mini'
        ],

        [
            'CloudPaniel DeepSeek Thinking',
            'deepseek/thinking'
        ],

        [
            'CloudPaniel Freeai Chat',
            'gratuitos/freeai/chat'
        ],

        [
            'CloudPaniel Groq',
            'groq'
        ]
    ].map(
        item => ({
            nome:
                item[0],

            url:
                `https://api.cloudpaniel.com.br/api/ai/${item[1]}`,

            metodo:
                'POST',

            paramTexto:
                'prompt',

            grupo:
                'cloud',

            authHeader:
                true
        })
    ),


    ...[
        [
            'CloudPaniel GPT-5.6-Luna',
            'gpt-5.6-luna'
        ],

        [
            'CloudPaniel GPT-5.6-Terra',
            'gpt-5.6-terra'
        ],

        [
            'CloudPaniel GPT-5.6-Sol',
            'gpt-5.6-sol'
        ],

        [
            'CloudPaniel GPT-OSS-120B',
            'gpt-oss-120b'
        ],

        [
            'CloudPaniel GPT-OSS-20B',
            'gpt-oss-20b'
        ],

        [
            'CloudPaniel Perplexity',
            'perplexity'
        ]
    ].map(
        item => ({
            nome:
                item[0],

            url:
                `https://api.cloudpaniel.com.br/api/ai/${item[1]}`,

            metodo:
                'GET',

            paramTexto:
                'prompt',

            paramApiKey:
                'Token',

            grupo:
                'cloud'
        })
    ),


    {
        nome:
            'Zone GPT-OSS-120B',

        url:
            'https://zone.api.br/ai/gptoss',

        metodo:
            'GET',

        paramTexto:
            'text',

        paramApiKey:
            'apikey',

        grupo:
            'zone'
    },

    {
        nome:
            'Zone Claude Haiku 4.5',

        url:
            'https://zone.api.br/api/ia/claude-haiku',

        metodo:
            'GET',

        paramTexto:
            'text',

        paramApiKey:
            'apikey',

        grupo:
            'zone'
    },

    {
        nome:
            'Zone DeepSeek V4',

        url:
            'https://zone.api.br/api/V2/deepseek',

        metodo:
            'GET',

        paramTexto:
            'text',

        paramApiKey:
            'apikey',

        grupo:
            'zone'
    },

    {
        nome:
            'Zone GPT-5.6-Sol',

        url:
            'https://zone.api.br/api/ia/gpt-5-6-sol',

        metodo:
            'GET',

        paramTexto:
            'text',

        paramApiKey:
            'apikey',

        grupo:
            'zone'
    },

    {
        nome:
            'Zone Grok-4.5',

        url:
            'https://zone.api.br/api/ia/grok-4-5',

        metodo:
            'GET',

        paramTexto:
            'text',

        paramApiKey:
            'apikey',

        grupo:
            'zone'
    },

    {
        nome:
            'Zone Microsoft Copilot2',

        url:
            'https://zone.api.br/api/copilot2',

        metodo:
            'GET',

        paramTexto:
            'text',

        grupo:
            'public'
    }
];


/* ============================================================
 * HTTP
 * ============================================================ */

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

    res.end(
        JSON.stringify(
            body
        )
    );
}


async function readJson(
    req
) {

    const chunks =
        [];

    let size =
        0;

    const LIMIT =
        12 *
        1024 *
        1024;


    for await (
        const chunk
        of req
    ) {

        size +=
            chunk.length;


        if (
            size >
            LIMIT
        ) {
            const error =
                new Error(
                    'ATTACHMENT_TOO_LARGE'
                );

            error.statusCode =
                413;

            throw error;
        }


        chunks.push(
            chunk
        );
    }


    const raw =
        Buffer.concat(
            chunks
        )
            .toString(
                'utf8'
            );


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


/* ============================================================
 * AUTH
 * ============================================================ */

function bearer(
    req
) {

    const header =
        String(
            req.headers
                .authorization ||
            ''
        );


    if (
        !header
            .toLowerCase()
            .startsWith(
                'bearer '
            )
    ) {

        const error =
            new Error(
                'AUTH_REQUIRED'
            );

        error.statusCode =
            401;

        throw error;
    }


    return header
        .slice(
            7
        )
        .trim();
}


async function authUser(
    req
) {

    const idToken =
        bearer(
            req
        );


    if (
        !FIREBASE_API_KEY
    ) {

        const error =
            new Error(
                'FIREBASE_API_KEY ausente.'
            );

        error.statusCode =
            500;

        throw error;
    }


    const response =
        await fetch(
            'https://identitytoolkit.googleapis.com/v1/accounts:lookup' +
            '?key=' +
            encodeURIComponent(
                FIREBASE_API_KEY
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
        await response
            .json()
            .catch(
                () => ({})
            );


    const user =
        data.users?.[0];


    if (
        !response.ok ||
        !user?.localId
    ) {

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


/* ============================================================
 * FAILOVER
 * ============================================================ */

function providerKey(
    provider
) {

    if (
        provider.grupo ===
        'cloud'
    ) {
        return CLOUD_KEY;
    }


    if (
        provider.grupo ===
        'zone'
    ) {
        return ZONE_KEY;
    }


    return null;
}


function providerDisponivel(
    provider
) {

    if (
        provider.grupo ===
        'public'
    ) {
        return true;
    }


    return Boolean(
        providerKey(
            provider
        )
    );
}


function extrairTexto(
    data
) {

    const candidates = [
        data?.resposta,
        data?.response,
        data?.text,
        data?.result,
        data?.message,
        data?.content,
        data?.reply,
        data?.output,
        data?.answer,
        data?.resultado?.resposta,
        data?.data?.response,
        data?.data?.text,
        data?.choices?.[0]?.message?.content
    ];


    for (
        const candidate
        of candidates
    ) {

        if (
            typeof candidate ===
                'string' &&
            candidate.trim()
        ) {

            return candidate
                .replace(
                    /:::[\w-]+(\{[^}]*\})?\s*/g,
                    ''
                )
                .replace(
                    /:::/g,
                    ''
                )
                .trim();
        }
    }


    return null;
}


async function chamarProvider(
    provider,
    prompt,
    imageUrl =
        null
) {

    const key =
        providerKey(
            provider
        );


    const controller =
        new AbortController();


    const timer =
        setTimeout(
            () =>
                controller.abort(),
            30000
        );


    try {

        let url =
            provider.url;

        let init;


        if (
            provider.metodo ===
            'POST'
        ) {

            const body = {
                [
                    provider.paramTexto ||
                    'prompt'
                ]:
                    prompt
            };


            const headers = {
                'Content-Type':
                    'application/json'
            };


            if (
                provider.authHeader &&
                key
            ) {

                headers.Authorization =
                    `Bearer ${key}`;
            }


            if (
                provider.paramApiKey &&
                key
            ) {

                body[
                    provider.paramApiKey
                ] =
                    key;
            }


            if (
                imageUrl &&
                provider.imagem &&
                provider.paramImagem
            ) {

                body[
                    provider.paramImagem
                ] =
                    imageUrl;
            }


            init = {
                method:
                    'POST',

                headers,

                body:
                    JSON.stringify(
                        body
                    ),

                signal:
                    controller.signal
            };

        } else {

            const params =
                new URLSearchParams();


            params.set(
                provider.paramTexto ||
                'text',
                String(
                    prompt
                ).slice(
                    -6500
                )
            );


            if (
                provider.paramApiKey &&
                key
            ) {

                params.set(
                    provider.paramApiKey,
                    key
                );
            }


            if (
                imageUrl &&
                provider.imagem &&
                provider.paramImagem
            ) {

                params.set(
                    provider.paramImagem,
                    imageUrl
                );
            }


            url +=
                '?' +
                params.toString();


            init = {
                method:
                    'GET',

                signal:
                    controller.signal
            };
        }


        const response =
            await fetch(
                url,
                init
            );


        const raw =
            await response
                .text();


        let data;


        try {

            data =
                JSON.parse(
                    raw
                );

        } catch {

            data = {
                text:
                    raw
            };
        }


        if (
            !response.ok ||
            data?.ok ===
                false
        ) {

            const message =
                data?.error?.message ||
                data?.error ||
                `HTTP ${response.status}`;

            throw new Error(
                String(
                    message
                )
            );
        }


        const answer =
            extrairTexto(
                data
            );


        if (
            !answer
        ) {

            throw new Error(
                'Resposta vazia.'
            );
        }


        return answer;

    } finally {

        clearTimeout(
            timer
        );
    }
}


async function perguntar(
    prompt,
    imageUrl =
        null
) {

    let providers =
        PROVIDERS.filter(
            providerDisponivel
        );


    if (
        imageUrl
    ) {

        providers =
            providers.filter(
                provider =>
                    provider.imagem
            );
    }


    if (
        providers.length ===
        0
    ) {

        throw new Error(
            imageUrl
                ? 'Nenhum provedor de visão configurado.'
                : 'Nenhum provedor de IA configurado.'
        );
    }


    const errors =
        [];


    for (
        const provider
        of providers
    ) {

        try {

            const answer =
                await chamarProvider(
                    provider,
                    prompt,
                    imageUrl
                );


            console.log(
                `[SOCIETY][AI] resposta via ${provider.nome}`
            );


            return {
                answer,
                provider:
                    provider.nome
            };

        } catch (
            error
        ) {

            console.warn(
                `[SOCIETY][AI] ${provider.nome}: ${error.message}`
            );


            errors.push(
                `${provider.nome}: ${error.message}`
            );
        }
    }


    const error =
        new Error(
            'Todos os provedores disponíveis falharam.'
        );

    error.details =
        errors;

    throw error;
}


/* ============================================================
 * ATTACHMENTS
 * ============================================================ */

function safeExtension(
    name,
    mime
) {

    const ext =
        path.extname(
            name ||
            ''
        )
            .replace(
                /[^a-zA-Z0-9.]/g,
                ''
            )
            .slice(
                0,
                10
            );


    if (
        ext
    ) {
        return ext;
    }


    if (
        mime ===
        'image/png'
    ) {
        return '.png';
    }


    if (
        mime ===
        'image/webp'
    ) {
        return '.webp';
    }


    return '.jpg';
}


function decodeAttachment(
    attachment
) {

    if (
        !attachment ||
        typeof attachment !==
            'object'
    ) {
        return null;
    }


    const base64 =
        String(
            attachment.base64 ||
            ''
        );


    if (
        !base64
    ) {
        return null;
    }


    const buffer =
        Buffer.from(
            base64,
            'base64'
        );


    if (
        buffer.length >
        7 *
        1024 *
        1024
    ) {

        const error =
            new Error(
                'O anexo pode ter no máximo 7 MB.'
            );

        error.statusCode =
            413;

        throw error;
    }


    return {
        buffer,

        name:
            String(
                attachment.name ||
                'anexo'
            ),

        mime:
            String(
                attachment.mime ||
                'application/octet-stream'
            )
    };
}


function stripXml(
    raw
) {

    return String(
        raw
    )
        .replace(
            /<w:tab[^>]*\/>/g,
            '\t'
        )
        .replace(
            /<\/w:p>/g,
            '\n'
        )
        .replace(
            /<[^>]+>/g,
            ' '
        )
        .replace(
            /&amp;/g,
            '&'
        )
        .replace(
            /&lt;/g,
            '<'
        )
        .replace(
            /&gt;/g,
            '>'
        )
        .replace(
            /&quot;/g,
            '"'
        )
        .replace(
            /\s+\n/g,
            '\n'
        )
        .replace(
            /\n{3,}/g,
            '\n\n'
        )
        .trim();
}


function extractDocumentText(
    attachment
) {

    const {
        buffer,
        name,
        mime
    } =
        attachment;


    const lower =
        name.toLowerCase();


    const textual =
        mime.startsWith(
            'text/'
        ) ||
        /\.(txt|md|js|ts|kt|java|py|sh|json|xml|html|css|csv|yml|yaml)$/i
            .test(
                lower
            );


    if (
        textual
    ) {

        return buffer
            .toString(
                'utf8'
            )
            .slice(
                0,
                45000
            );
    }


    const tmp =
        path.join(
            MEDIA_ROOT,
            crypto.randomUUID() +
            path.extname(
                lower
            )
        );


    fs.writeFileSync(
        tmp,
        buffer
    );


    try {

        if (
            mime ===
                'application/pdf' ||
            lower.endsWith(
                '.pdf'
            )
        ) {

            const output =
                execFileSync(
                    'pdftotext',
                    [
                        tmp,
                        '-'
                    ],
                    {
                        encoding:
                            'utf8',

                        timeout:
                            15000,

                        maxBuffer:
                            6 *
                            1024 *
                            1024
                    }
                );


            return output
                .slice(
                    0,
                    45000
                );
        }


        if (
            lower.endsWith(
                '.docx'
            )
        ) {

            const xml =
                execFileSync(
                    'unzip',
                    [
                        '-p',
                        tmp,
                        'word/document.xml'
                    ],
                    {
                        encoding:
                            'utf8',

                        timeout:
                            15000,

                        maxBuffer:
                            6 *
                            1024 *
                            1024
                    }
                );


            return stripXml(
                xml
            )
                .slice(
                    0,
                    45000
                );
        }


        if (
            lower.endsWith(
                '.xlsx'
            )
        ) {

            const xml =
                execFileSync(
                    'unzip',
                    [
                        '-p',
                        tmp,
                        'xl/sharedStrings.xml'
                    ],
                    {
                        encoding:
                            'utf8',

                        timeout:
                            15000,

                        maxBuffer:
                            6 *
                            1024 *
                            1024
                    }
                );


            return stripXml(
                xml
            )
                .slice(
                    0,
                    45000
                );
        }


        throw new Error(
            'Formato de documento ainda não suportado.'
        );

    } finally {

        try {
            fs.unlinkSync(
                tmp
            );
        } catch {
            // ignore
        }
    }
}


function createPublicImage(
    attachment
) {

    const id =
        crypto.randomUUID();

    const ext =
        safeExtension(
            attachment.name,
            attachment.mime
        );

    const file =
        id +
        ext;

    const full =
        path.join(
            MEDIA_ROOT,
            file
        );


    fs.writeFileSync(
        full,
        attachment.buffer
    );


    return {
        file,
        full,

        url:
            `${PUBLIC_BASE}/api/society/ai/media/${encodeURIComponent(file)}`
    };
}


/* ============================================================
 * PROMPTS
 * ============================================================ */

function buildConversationPrompt(
    body,
    documentText =
        null
) {

    const history =
        Array.isArray(
            body.history
        )
            ? body.history
                .slice(
                    -12
                )
            : [];


    const lines =
        history
            .map(
                item => {

                    const role =
                        item?.role ===
                        'assistant'
                            ? 'ASSISTENTE'
                            : 'USUÁRIO';


                    return (
                        `${role}: ` +
                        String(
                            item?.content ||
                            ''
                        ).slice(
                            0,
                            5000
                        )
                    );
                }
            );


    let prompt =
        [
            'Você é a Society AI, assistente do aplicativo Society Bots.',
            'Responda em português brasileiro, de forma útil, clara e natural.',
            'Ajude com criação e gerenciamento de bots, programação e dúvidas gerais.',
            'Não invente que executou ações que não executou.',
            '',
            ...lines,
            '',
            'USUÁRIO:',
            String(
                body.prompt ||
                ''
            )
        ]
            .join(
                '\n'
            );


    if (
        documentText
    ) {

        prompt +=
            '\n\nDOCUMENTO ANEXADO:\n' +
            documentText;
    }


    return prompt;
}


function firstJsonObject(
    text
) {

    const start =
        text.indexOf(
            '{'
        );


    if (
        start ===
        -1
    ) {
        return null;
    }


    let depth =
        0;

    let string =
        false;

    let escaped =
        false;


    for (
        let i =
            start;
        i <
            text.length;
        i++
    ) {

        const ch =
            text[i];


        if (
            escaped
        ) {
            escaped =
                false;

            continue;
        }


        if (
            ch ===
                '\\' &&
            string
        ) {

            escaped =
                true;

            continue;
        }


        if (
            ch ===
            '"'
        ) {

            string =
                !string;

            continue;
        }


        if (
            string
        ) {
            continue;
        }


        if (
            ch ===
            '{'
        ) {
            depth++;
        }


        if (
            ch ===
            '}'
        ) {

            depth--;

            if (
                depth ===
                0
            ) {

                return text.slice(
                    start,
                    i + 1
                );
            }
        }
    }


    return null;
}


/* ============================================================
 * ROUTES
 * ============================================================ */

async function handleChat(
    req,
    res
) {

    try {

        if (
            req.method !==
            'POST'
        ) {

            return json(
                res,
                405,
                {
                    ok:
                        false,

                    error:
                        'Método não permitido.'
                }
            );
        }


        await authUser(
            req
        );


        const body =
            await readJson(
                req
            );


        const attachment =
            decodeAttachment(
                body.attachment
            );


        let documentText =
            null;

        let image =
            null;


        if (
            attachment
        ) {

            if (
                attachment.mime
                    .startsWith(
                        'audio/'
                    )
            ) {

                return json(
                    res,
                    415,
                    {
                        ok:
                            false,

                        error:
                            'Para voz, use o botão de microfone do chat. O áudio será transcrito no Android antes de chegar à IA.'
                    }
                );
            }


            if (
                attachment.mime
                    .startsWith(
                        'image/'
                    )
            ) {

                image =
                    createPublicImage(
                        attachment
                    );

            } else {

                documentText =
                    extractDocumentText(
                        attachment
                    );
            }
        }


        const prompt =
            buildConversationPrompt(
                body,
                documentText
            );


        if (
            !String(
                body.prompt ||
                ''
            ).trim() &&
            !attachment
        ) {

            return json(
                res,
                400,
                {
                    ok:
                        false,

                    error:
                        'Digite uma mensagem.'
                }
            );
        }


        try {

            const result =
                await perguntar(
                    prompt,
                    image?.url ||
                    null
                );


            return json(
                res,
                200,
                {
                    ok:
                        true,

                    answer:
                        result.answer,

                    provider:
                        result.provider
                }
            );

        } finally {

            if (
                image?.full
            ) {

                try {

                    fs.unlinkSync(
                        image.full
                    );

                } catch {
                    // ignore
                }
            }
        }

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][AI CHAT]',
            error.message
        );


        return json(
            res,
            error.statusCode ||
                500,
            {
                ok:
                    false,

                error:
                    error.message ||
                    'Não foi possível consultar a IA.'
            }
        );
    }
}


async function handleCommandDraft(
    req,
    res
) {

    try {

        if (
            req.method !==
            'POST'
        ) {

            return json(
                res,
                405,
                {
                    ok:
                        false,

                    error:
                        'Método não permitido.'
                }
            );
        }


        await authUser(
            req
        );


        const body =
            await readJson(
                req
            );


        const description =
            String(
                body.description ||
                ''
            ).trim();


        const currentType =
            body.type ===
            'sticker'
                ? 'sticker'
                : 'text';


        if (
            description.length <
            4
        ) {

            return json(
                res,
                400,
                {
                    ok:
                        false,

                    error:
                        'Descreva melhor o comando que deseja criar.'
                }
            );
        }


        const prompt =
            [
                'Você é o gerador de comandos do Society Bots.',
                '',
                'Crie os campos para um comando de WhatsApp.',
                '',
                'O usuário descreveu:',
                description,
                '',
                `Tipo atual: ${currentType}`,
                '',
                'Retorne SOMENTE JSON válido neste formato:',
                '{"name":"comando","description":"descrição curta","response":"resposta do bot","type":"text"}',
                '',
                'Regras:',
                '- name sem /, sem espaço, minúsculo.',
                '- type deve ser text ou sticker.',
                '- Se for sticker, response pode ser vazio.',
                '- Não use markdown.',
                '- Não escreva explicação fora do JSON.'
            ]
                .join(
                    '\n'
                );


        const result =
            await perguntar(
                prompt
            );


        const candidate =
            firstJsonObject(
                result.answer
            );


        if (
            !candidate
        ) {

            throw new Error(
                'A IA não retornou um comando estruturado.'
            );
        }


        const draft =
            JSON.parse(
                candidate
            );


        const name =
            String(
                draft.name ||
                ''
            )
                .replace(
                    /\//g,
                    ''
                )
                .replace(
                    /\s+/g,
                    ''
                )
                .toLowerCase()
                .slice(
                    0,
                    40
                );


        if (
            !name
        ) {

            throw new Error(
                'A IA não definiu o nome do comando.'
            );
        }


        return json(
            res,
            200,
            {
                ok:
                    true,

                provider:
                    result.provider,

                draft: {
                    name,

                    description:
                        String(
                            draft.description ||
                            ''
                        ).slice(
                            0,
                            180
                        ),

                    response:
                        String(
                            draft.response ||
                            ''
                        ).slice(
                            0,
                            8000
                        ),

                    type:
                        draft.type ===
                        'sticker'
                            ? 'sticker'
                            : 'text'
                }
            }
        );

    } catch (
        error
    ) {

        console.error(
            '[SOCIETY][AI COMMAND]',
            error.message
        );


        return json(
            res,
            error.statusCode ||
                500,
            {
                ok:
                    false,

                error:
                    error.message ||
                    'Não foi possível gerar o comando.'
            }
        );
    }
}


function serveMedia(
    req,
    res,
    pathname
) {

    try {

        if (
            req.method !==
            'GET'
        ) {

            res.statusCode =
                405;

            return res.end();
        }


        const name =
            decodeURIComponent(
                pathname
                    .split(
                        '/'
                    )
                    .pop() ||
                ''
            );


        if (
            !/^[a-f0-9-]+\.[a-zA-Z0-9]+$/
                .test(
                    name
                )
        ) {

            res.statusCode =
                404;

            return res.end();
        }


        const file =
            path.join(
                MEDIA_ROOT,
                name
            );


        if (
            !file.startsWith(
                MEDIA_ROOT +
                path.sep
            ) ||
            !fs.existsSync(
                file
            )
        ) {

            res.statusCode =
                404;

            return res.end();
        }


        const ext =
            path.extname(
                file
            )
                .toLowerCase();


        const mime =
            ext ===
                '.png'
                ? 'image/png'
                : ext ===
                    '.webp'
                    ? 'image/webp'
                    : 'image/jpeg';


        res.statusCode =
            200;

        res.setHeader(
            'Content-Type',
            mime
        );

        res.setHeader(
            'Cache-Control',
            'no-store'
        );


        fs.createReadStream(
            file
        )
            .pipe(
                res
            );

    } catch {

        res.statusCode =
            404;

        res.end();
    }
}


module.exports = {
    handleChat,
    handleCommandDraft,
    serveMedia
};
