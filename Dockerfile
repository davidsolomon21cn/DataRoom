FROM docker.io/eclipse-temurin:17-jdk AS release-unpack

RUN mkdir -p /opt/gcpaas
COPY RELEASE/dataRoom-*.zip /opt/gcpaas/
RUN set -eu; \
    set -- $(ls -t /opt/gcpaas/dataRoom-*.zip); \
    latest_zip="$1"; \
    test -n "${latest_zip}"; \
    cd /opt/gcpaas; \
    jar -xf "${latest_zip}"

FROM docker.io/eclipse-temurin:17-jre

COPY --from=release-unpack /opt/gcpaas/dataRoom /opt/gcpaas/dataRoom
WORKDIR /opt/gcpaas/dataRoom
ENV RUN_ENV=docker
ENTRYPOINT ["sh", "-c", "java -jar -Duser.timezone=GMT+8 dataRoomServer.jar"]
