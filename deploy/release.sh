#!/usr/bin/env bash
# Prepare locally; deployment is a separate, explicit operation on the v2 instance.
set -euo pipefail
cd "$(dirname "$0")/.."
usage() { echo 'Usage: deploy/release.sh --prepare <tag> <new-bundle-dir> | --deploy <bundle-dir> <ssh-target>' >&2; exit 2; }
case "${1:-}" in
  --prepare)
    [[ $# == 3 ]] || usage
    app_tag="$2"; bundle="$3"
    [[ "$app_tag" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$ ]] || usage
    [[ ! -e "$bundle" ]] || { echo 'Bundle directory must be new.' >&2; exit 1; }
    export JAVA_HOME="${JAVA_HOME:-$HOME/.sdkman/candidates/java/25.0.4-tem}"
    export PATH="$JAVA_HOME/bin:$PATH"
    ./build.sh verify
    app_base="${WIKI_APP_BASE_IMAGE:-eclipse-temurin:25-jre-noble}"
    docker image inspect "$app_base" >/dev/null
    docker image inspect dts-wiki-db:18-bigm >/dev/null
    docker build --pull=false --build-arg "APP_BASE=$app_base" -f src/main/docker/app.Dockerfile -t "dts-wiki-app:$app_tag" .
    umask 077
    mkdir -p -- "$bundle"
    bundle="$(cd "$bundle" && pwd)"
    docker save "dts-wiki-app:$app_tag" dts-wiki-db:18-bigm | gzip > "$bundle/images.tar.gz"
    cp deploy/compose.yml deploy/.env.example deploy/backup.sh deploy/restore-check.sh "$bundle/"
    printf 'services:\n  wiki-app:\n    image: dts-wiki-app:%s\n  wiki-db:\n    image: dts-wiki-db:18-bigm\n' "$app_tag" > "$bundle/compose.override.yml"
    printf 'APP_TAG=%s\nDB_TAG=18-bigm\nSOURCE_COMMIT=%s\n' "$app_tag" "$(git rev-parse HEAD)" > "$bundle/release.properties"
    docker image inspect --format '{{.Id}}' "dts-wiki-app:$app_tag" dts-wiki-db:18-bigm > "$bundle/image-ids.txt"
    (cd "$bundle" && sha256sum images.tar.gz compose.yml compose.override.yml .env.example backup.sh restore-check.sh release.properties image-ids.txt > SHA256SUMS)
    echo "Prepared local release bundle: $bundle"
    ;;
  --deploy)
    [[ $# == 3 ]] || usage
    bundle="$(cd "$2" && pwd)"; ssh_target="$3"
    [[ "$ssh_target" =~ ^[a-zA-Z0-9_.@-]+$ ]] || usage
    (cd "$bundle" && sha256sum -c SHA256SUMS)
    app_tag="$(sed -n 's/^APP_TAG=//p' "$bundle/release.properties")"
    [[ "$app_tag" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$ ]] || usage
    ssh "$ssh_target" 'test -f /data/dts-wiki-v2/.env && test -f /data/dts-wiki-v2/secrets/content.key && test -f /data/dts-wiki-v2/secrets/known_hosts'
    ssh "$ssh_target" "mkdir -p /data/dts-wiki-v2/releases && test ! -e /data/dts-wiki-v2/releases/$app_tag"
    scp -r "$bundle" "$ssh_target:/data/dts-wiki-v2/releases/$app_tag"
    ssh "$ssh_target" "cd /data/dts-wiki-v2/releases/$app_tag && sha256sum -c SHA256SUMS && gunzip -c images.tar.gz | docker load"
    ssh "$ssh_target" "cp /data/dts-wiki-v2/releases/$app_tag/compose.yml /data/dts-wiki-v2/compose.yml && cp /data/dts-wiki-v2/releases/$app_tag/compose.override.yml /data/dts-wiki-v2/compose.override.yml && cd /data/dts-wiki-v2 && APP_TAG=$app_tag DB_TAG=18-bigm docker compose config --quiet && APP_TAG=$app_tag DB_TAG=18-bigm docker compose up -d --pull never && APP_TAG=$app_tag DB_TAG=18-bigm docker compose ps"
    ;;
  *) usage ;;
esac
