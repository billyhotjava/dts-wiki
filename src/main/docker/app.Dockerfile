# DTS Wiki application image (Sprint-6 design 06 S2.1).
# Jib is not used because git sync needs git + ssh inside the container.
# Build on the dev machine (writes only here); ship to .50 via docker save/load.
ARG APP_BASE=eclipse-temurin:25-jre-noble
FROM ${APP_BASE}
RUN apt-get update && apt-get install -y --no-install-recommends git openssh-client tzdata \
 && rm -rf /var/lib/apt/lists/* && useradd -r -u 1001 -m wiki
ENV TZ=Asia/Shanghai JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=192m -Xss512k -XX:+ExitOnOutOfMemoryError"
COPY target/dts-wiki-*.jar /app/app.jar
USER wiki
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
