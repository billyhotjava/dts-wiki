#!/usr/bin/env bash
# Build a tested production jar using the installed, pinned toolchains.
set -euo pipefail
cd "$(dirname "$0")"
[[ "${1:-verify}" == verify ]] || { echo 'Usage: ./build.sh verify' >&2; exit 2; }
for command in java node pnpm docker python3; do
  command -v "$command" >/dev/null || { echo "Missing tool: $command" >&2; exit 1; }
done
java -version 2>&1 | head -1 | grep -Eq 'version "25[.]' || { echo 'Set JAVA_HOME and PATH to Java 25.' >&2; exit 1; }
[[ "$(node --version)" == v24.* ]] || { echo 'Node 24 is required.' >&2; exit 1; }
[[ "$(pnpm --version)" == 12.* ]] || { echo 'pnpm 12 is required.' >&2; exit 1; }
docker image inspect dts-wiki-db:18-bigm >/dev/null
docker image inspect testcontainers/ryuk:0.14.0 >/dev/null
pnpm --dir frontend install --frozen-lockfile
pnpm --dir frontend test
pnpm --dir frontend build
build_tmp="$(mktemp -d "${TMPDIR:-/tmp}/dts-wiki-build.XXXXXX")"
trap 'rm -rf -- "$build_tmp"' EXIT
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djava.io.tmpdir=$build_tmp"
./mvnw -B -ntp -Pprod clean verify "-Dlogging.file.name=$build_tmp/spring.log"
python3 - <<'PY'
from pathlib import Path
import zipfile
jar = next(Path('target').glob('dts-wiki-*.jar'))
with zipfile.ZipFile(jar) as archive:
    for asset in Path('frontend/dist').rglob('*'):
        if asset.is_file():
            bundled = 'BOOT-INF/classes/static/' + asset.relative_to('frontend/dist').as_posix()
            if archive.read(bundled) != asset.read_bytes():
                raise SystemExit('Bundled frontend mismatch: ' + bundled)
print('Production jar contains the verified frontend assets.')
PY
