# PostgreSQL 18 + pg_bigm image for DTS Wiki.
# Design: sprint-6-202610 F0/design/06 S2.2, decision W-ADR-8 (assets/search-spike.md).
# pg_bigm is compiled from source (pinned commit) because no official PG18 image
# ships it. Verified 2026-09-26: builds against postgres:18 (PG 18.6, trixie).

ARG PG_MAJOR=18
ARG PG_IMAGE=postgres:18.4
FROM ${PG_IMAGE} AS builder
ARG PG_MAJOR=18
RUN apt-get update && apt-get install -y --no-install-recommends \
      gcc make postgresql-server-dev-${PG_MAJOR} libicu-dev git ca-certificates \
 && rm -rf /var/lib/apt/lists/*
# Pinned to the commit validated by the F0/T03 spike (branch master HEAD at spike time).
ARG PGBIGM_REF=8c0a691b9e99c1f83d71ebd1bc066f0140fd1850
RUN git clone https://github.com/pgbigm/pg_bigm.git /tmp/pg_bigm \
 && cd /tmp/pg_bigm && git checkout "${PGBIGM_REF}" \
 && export PATH="/usr/lib/postgresql/${PG_MAJOR}/bin:${PATH}" \
 && hash -r && command -v pg_config && pg_config --version \
 && make USE_PGXS=1 PG_CONFIG="/usr/lib/postgresql/${PG_MAJOR}/bin/pg_config" -j"$(nproc)" \
 && make USE_PGXS=1 PG_CONFIG="/usr/lib/postgresql/${PG_MAJOR}/bin/pg_config" install \
 && ls "/usr/lib/postgresql/${PG_MAJOR}/lib/pg_bigm.so" \
 && rm -rf /tmp/pg_bigm

FROM ${PG_IMAGE}
ARG PG_MAJOR=18
COPY --from=builder /usr/lib/postgresql/${PG_MAJOR}/lib/pg_bigm.so \
                    /usr/lib/postgresql/${PG_MAJOR}/lib/
COPY --from=builder /usr/share/postgresql/${PG_MAJOR}/extension/pg_bigm* \
                    /usr/share/postgresql/${PG_MAJOR}/extension/
# NOTE: JIT bitcode (*.bc) installed by `make install` is intentionally not copied;
# the extension works without it.
RUN ls /usr/lib/postgresql/${PG_MAJOR}/lib/pg_bigm.so \
 && ls /usr/share/postgresql/${PG_MAJOR}/extension/pg_bigm.control
