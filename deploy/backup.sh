#!/usr/bin/env bash
# Run from the v2 instance directory. A short application pause makes blobs and DB consistent.
set -euo pipefail
[[ $# == 1 ]] || { echo 'Usage: backup.sh <new-backup-dir> (from the v2 instance directory)' >&2; exit 2; }
[[ -f compose.yml && -d attachments ]] || { echo 'Expected compose.yml and attachments/.' >&2; exit 1; }
backup_dir="$1"
[[ ! -e "$backup_dir" ]] || { echo 'Backup directory must be new.' >&2; exit 1; }
docker compose config --quiet
db_container="$(docker compose ps -q wiki-db)"
[[ -n "$db_container" ]] || { echo 'Wiki database is unavailable.' >&2; exit 1; }
app_container="$(docker compose ps -q wiki-app)"
resume_app=false
if [[ -n "$app_container" && "$(docker inspect -f '{{.State.Running}}' "$app_container")" == true ]]; then
  resume_app=true
  docker compose stop wiki-app
fi
cleanup() {
  if [[ "$resume_app" == true ]]; then docker compose start wiki-app >/dev/null; fi
}
trap cleanup EXIT
umask 077
mkdir -p -- "$backup_dir"
backup_dir="$(cd "$backup_dir" && pwd)"
docker compose exec -T wiki-db pg_dump -U wiki -d wiki --format=custom --no-owner --no-acl > "$backup_dir/wiki.dump"
docker compose exec -T wiki-db psql -U wiki -d wiki -Atc 'SELECT (SELECT count(*) FROM page),(SELECT count(*) FROM page_version),(SELECT count(*) FROM attachment),(SELECT count(*) FROM databasechangelog)' > "$backup_dir/counts.txt"
if [[ -n "$(find attachments -type l -print -quit)" ]]; then echo 'Symlinks are not allowed in the blob store.' >&2; exit 1; fi
tar -czf "$backup_dir/attachments.tar.gz" attachments
cp compose.yml "$backup_dir/compose.yml"
if [[ -f compose.override.yml ]]; then cp compose.override.yml "$backup_dir/compose.override.yml"; fi
docker inspect --format '{{.Image}}' "$db_container" > "$backup_dir/db-image-id.txt"
date -u +'%Y-%m-%dT%H:%M:%SZ' > "$backup_dir/captured-at.txt"
(cd "$backup_dir" && sha256sum wiki.dump attachments.tar.gz counts.txt compose*.yml db-image-id.txt captured-at.txt > SHA256SUMS)
echo "Completed consistent Wiki backup: $backup_dir"
