#!/usr/bin/env bash
# Restore into one disposable, isolated container; never write to the running Wiki DB.
set -euo pipefail
[[ $# == 1 ]] || { echo 'Usage: restore-check.sh <backup-dir>' >&2; exit 2; }
backup_dir="$(cd "$1" && pwd)"
(cd "$backup_dir" && sha256sum -c SHA256SUMS)
db_image="$(cat "$backup_dir/db-image-id.txt")"
[[ "$db_image" =~ ^sha256:[a-f0-9]{64}$ ]] || { echo 'Invalid database image ID.' >&2; exit 1; }
docker image inspect "$db_image" >/dev/null
restore_tmp="$(mktemp -d "${TMPDIR:-/tmp}/dts-wiki-restore.XXXXXX")"
container=""
cleanup() {
  if [[ -n "$container" ]]; then docker rm -f -v "$container" >/dev/null; fi
  rm -rf -- "$restore_tmp"
}
trap cleanup EXIT
python3 - "$backup_dir/attachments.tar.gz" "$restore_tmp" <<'PY'
import hashlib, pathlib, sys, tarfile
with tarfile.open(sys.argv[1], 'r:gz') as archive:
    for member in archive:
        path = pathlib.PurePosixPath(member.name)
        if path.is_absolute() or '..' in path.parts or path.parts[0] != 'attachments' or not (member.isdir() or member.isfile()):
            raise SystemExit('Unsafe blob archive member')
    archive.extractall(sys.argv[2], filter='data')
for path in pathlib.Path(sys.argv[2], 'attachments').rglob('*'):
    if path.is_file():
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        # BlobStore stores the SHA-256 as the filename, optionally under prefix directories.
        if path.name != digest:
            raise SystemExit('Blob digest mismatch: ' + path.name)
print('Restored blob archive and verified every content digest.')
PY
container="$(docker run -d --pull=never --network none -e POSTGRES_DB=wiki -e POSTGRES_USER=wiki -e POSTGRES_HOST_AUTH_METHOD=trust "$db_image")"
ready=false
for attempt in {1..30}; do
  if docker exec "$container" pg_isready -U wiki -d wiki >/dev/null 2>&1; then ready=true; break; fi
  sleep 1
done
[[ "$ready" == true ]] || { echo 'Disposable PostgreSQL did not become ready.' >&2; exit 1; }
docker exec -i "$container" pg_restore -U wiki -d wiki --exit-on-error --no-owner --no-acl < "$backup_dir/wiki.dump"
docker exec "$container" psql -U wiki -d wiki -Atc 'SELECT (SELECT count(*) FROM page),(SELECT count(*) FROM page_version),(SELECT count(*) FROM attachment),(SELECT count(*) FROM databasechangelog)' > "$restore_tmp/counts.txt"
cmp "$backup_dir/counts.txt" "$restore_tmp/counts.txt"
docker exec "$container" psql -U wiki -d wiki -Atc "SELECT extname FROM pg_extension WHERE extname='pg_bigm'" | grep -Fx pg_bigm
docker exec "$container" psql -U wiki -d wiki -Atc 'SELECT sha256 FROM attachment' > "$restore_tmp/blob-refs.txt"
python3 - "$restore_tmp" <<'PY'
import pathlib, sys
root = pathlib.Path(sys.argv[1])
files = {p.name for p in (root / 'attachments').rglob('*') if p.is_file()}
missing = set((root / 'blob-refs.txt').read_text().splitlines()) - files
if missing:
    raise SystemExit('Missing attachment blobs: ' + str(len(missing)))
print('Database counts, pg_bigm and attachment references passed restore verification.')
PY
