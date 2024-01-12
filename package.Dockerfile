ARG version
FROM --platform=$BUILDPLATFORM docker.taktik.be/icure/icure-kmehr-module:$version as builder

FROM docker.taktik.be/ubuntu-graalvm:21.0.1
ARG version
COPY --from=builder /build/standalone-$version.jar ./icure-kmehr-module-$version.jar
VOLUME /tmp
ENTRYPOINT ["/usr/bin/tini", "--", "bash", "-c", "java $JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED -jar ./icure-kmehr-module-*.jar $@"]
HEALTHCHECK --interval=150s --timeout=5m --retries=1 --start-period=10m CMD curl -f -s --retry 10 --max-time 2 --retry-delay 15 --retry-all-errors http://localhost:8080/actuator/health/liveness || (kill -s 15 -1 && (sleep 10; kill -s 9 -1))
