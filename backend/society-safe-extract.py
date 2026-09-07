#!/usr/bin/env python3

import os
import stat
import sys
import tarfile
import zipfile
from pathlib import Path, PurePosixPath


MAX_FILES = 6000
MAX_TOTAL = 350 * 1024 * 1024
MAX_SINGLE = 160 * 1024 * 1024


def safe_parts(name):
    name = name.replace("\\", "/")

    if "\x00" in name:
        raise RuntimeError("Nome de arquivo inválido.")

    path = PurePosixPath(name)

    if path.is_absolute():
        raise RuntimeError(
            f"Caminho absoluto proibido: {name}"
        )

    parts = [
        p for p in path.parts
        if p not in ("", ".")
    ]

    if any(
        p == ".."
        for p in parts
    ):
        raise RuntimeError(
            f"Path traversal detectado: {name}"
        )

    return parts


def target_for(root, name):
    parts = safe_parts(name)

    target = (
        root.joinpath(*parts)
        .resolve()
    )

    root_resolved = root.resolve()

    if os.path.commonpath(
        [
            str(root_resolved),
            str(target)
        ]
    ) != str(root_resolved):
        raise RuntimeError(
            f"Arquivo tentou sair da pasta: {name}"
        )

    return target


def extract_zip(archive, root):
    count = 0
    total = 0
    seen = set()

    with zipfile.ZipFile(archive) as z:
        infos = z.infolist()

        if len(infos) > MAX_FILES:
            raise RuntimeError(
                "ZIP possui arquivos demais."
            )

        for info in infos:
            count += 1

            if info.flag_bits & 0x1:
                raise RuntimeError(
                    "ZIP criptografado não é aceito."
                )

            total += info.file_size

            if info.file_size > MAX_SINGLE:
                raise RuntimeError(
                    f"Arquivo interno grande demais: {info.filename}"
                )

            if total > MAX_TOTAL:
                raise RuntimeError(
                    "ZIP descompactaria para mais de 350 MB."
                )

            target = target_for(
                root,
                info.filename
            )

            key = str(target)

            if key in seen:
                raise RuntimeError(
                    f"Entrada duplicada: {info.filename}"
                )

            seen.add(key)

            mode = (
                info.external_attr >> 16
            )

            kind = stat.S_IFMT(mode)

            if kind == stat.S_IFLNK:
                raise RuntimeError(
                    f"Symlink proibido: {info.filename}"
                )

            if (
                kind
                and kind not in (
                    stat.S_IFREG,
                    stat.S_IFDIR
                )
            ):
                raise RuntimeError(
                    f"Tipo de arquivo proibido: {info.filename}"
                )

            if info.is_dir():
                target.mkdir(
                    parents=True,
                    exist_ok=True
                )
                continue

            target.parent.mkdir(
                parents=True,
                exist_ok=True
            )

            with z.open(info) as src:
                with open(target, "xb") as dst:
                    while True:
                        chunk = src.read(
                            1024 * 1024
                        )

                        if not chunk:
                            break

                        dst.write(chunk)

            os.chmod(
                target,
                0o755
                if mode & 0o111
                else 0o644
            )

    return count, total


def extract_tar(archive, root):
    count = 0
    total = 0
    seen = set()

    with tarfile.open(
        archive,
        mode="r:*"
    ) as tar:
        members = tar.getmembers()

        if len(members) > MAX_FILES:
            raise RuntimeError(
                "TAR possui arquivos demais."
            )

        for member in members:
            count += 1

            if (
                member.issym()
                or member.islnk()
                or member.isdev()
                or member.isfifo()
            ):
                raise RuntimeError(
                    f"Link/dispositivo proibido: {member.name}"
                )

            if not (
                member.isdir()
                or member.isfile()
            ):
                raise RuntimeError(
                    f"Tipo de entrada não suportado: {member.name}"
                )

            total += (
                member.size
                if member.isfile()
                else 0
            )

            if member.size > MAX_SINGLE:
                raise RuntimeError(
                    f"Arquivo interno grande demais: {member.name}"
                )

            if total > MAX_TOTAL:
                raise RuntimeError(
                    "TAR descompactaria para mais de 350 MB."
                )

            target = target_for(
                root,
                member.name
            )

            key = str(target)

            if key in seen:
                raise RuntimeError(
                    f"Entrada duplicada: {member.name}"
                )

            seen.add(key)

            if member.isdir():
                target.mkdir(
                    parents=True,
                    exist_ok=True
                )
                continue

            target.parent.mkdir(
                parents=True,
                exist_ok=True
            )

            src = tar.extractfile(
                member
            )

            if src is None:
                raise RuntimeError(
                    f"Não consegui ler: {member.name}"
                )

            with src:
                with open(target, "xb") as dst:
                    while True:
                        chunk = src.read(
                            1024 * 1024
                        )

                        if not chunk:
                            break

                        dst.write(chunk)

            os.chmod(
                target,
                0o755
                if member.mode & 0o111
                else 0o644
            )

    return count, total


def main():
    if len(sys.argv) != 3:
        raise SystemExit(
            "uso: extractor archive destination"
        )

    archive = Path(
        sys.argv[1]
    )

    destination = Path(
        sys.argv[2]
    )

    destination.mkdir(
        parents=True,
        exist_ok=True
    )

    lower = archive.name.lower()

    if lower.endswith(".zip"):
        count, total = extract_zip(
            archive,
            destination
        )

    elif (
        lower.endswith(".tar.gz")
        or lower.endswith(".tgz")
        or lower.endswith(".tar")
    ):
        count, total = extract_tar(
            archive,
            destination
        )

    else:
        raise RuntimeError(
            "Formato de arquivo não suportado."
        )

    print(
        f"OK files={count} bytes={total}"
    )


if __name__ == "__main__":
    main()
