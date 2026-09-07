'use strict';

const fs = require('fs');
const fsp = fs.promises;
const path = require('path');
const crypto = require('crypto');

const DATA_ROOT =
    path.join(
        __dirname,
        'data'
    );

const USER_ROOT =
    path.join(
        DATA_ROOT,
        'users'
    );

const IMAGE_ROOT =
    path.join(
        DATA_ROOT,
        'profile-images'
    );


function safeUid(uid) {

    return String(uid || '')
        .replace(
            /[^a-zA-Z0-9_-]/g,
            ''
        );
}


function userFile(uid) {

    const safe =
        safeUid(uid);

    if (!safe) {
        throw new Error(
            'INVALID_UID'
        );
    }

    return path.join(
        USER_ROOT,
        `${safe}.json`
    );
}


function defaultUser(uid) {

    return {
        uid,
        profile: {
            displayName: '',
            photoPath: null
        },
        settings: {
            darkMode: true,
            language: 'pt-BR'
        },
        bots: [],
        updatedAt:
            Date.now()
    };
}


async function ensureDirs() {

    await fsp.mkdir(
        USER_ROOT,
        {
            recursive: true
        }
    );

    await fsp.mkdir(
        IMAGE_ROOT,
        {
            recursive: true
        }
    );
}


async function loadUser(uid) {

    await ensureDirs();

    const file =
        userFile(uid);

    try {

        const raw =
            await fsp.readFile(
                file,
                'utf8'
            );

        return JSON.parse(
            raw
        );

    } catch (
        error
    ) {

        if (
            error.code !==
            'ENOENT'
        ) {
            throw error;
        }

        const data =
            defaultUser(uid);

        await saveUser(
            uid,
            data
        );

        return data;
    }
}


async function saveUser(
    uid,
    data
) {

    await ensureDirs();

    const file =
        userFile(uid);

    const tmp =
        `${file}.${process.pid}.tmp`;

    const normalized = {
        ...defaultUser(uid),
        ...data,
        uid,
        updatedAt:
            Date.now()
    };

    await fsp.writeFile(
        tmp,
        JSON.stringify(
            normalized,
            null,
            2
        ),
        {
            encoding: 'utf8',
            mode: 0o600
        }
    );

    await fsp.rename(
        tmp,
        file
    );

    return normalized;
}


async function updateUser(
    uid,
    patch
) {

    const current =
        await loadUser(uid);

    const next = {
        ...current,
        ...patch,

        profile: {
            ...current.profile,
            ...(patch.profile || {})
        },

        settings: {
            ...current.settings,
            ...(patch.settings || {})
        }
    };

    return saveUser(
        uid,
        next
    );
}


async function saveProfileImage(
    uid,
    mime,
    buffer
) {

    await ensureDirs();

    if (
        !Buffer.isBuffer(buffer) ||
        buffer.length === 0
    ) {
        throw new Error(
            'EMPTY_IMAGE'
        );
    }

    if (
        buffer.length >
        5 * 1024 * 1024
    ) {
        throw new Error(
            'IMAGE_TOO_LARGE'
        );
    }

    const ext =
        mime === 'image/png'
            ? 'png'
            : 'jpg';

    const name =
        `${safeUid(uid)}-${crypto
            .randomBytes(6)
            .toString('hex')}.${ext}`;

    const file =
        path.join(
            IMAGE_ROOT,
            name
        );

    await fsp.writeFile(
        file,
        buffer,
        {
            mode: 0o600
        }
    );

    return name;
}


function imageFile(name) {

    const safe =
        path.basename(
            String(name || '')
        );

    return path.join(
        IMAGE_ROOT,
        safe
    );
}


module.exports = {
    loadUser,
    saveUser,
    updateUser,
    saveProfileImage,
    imageFile
};
