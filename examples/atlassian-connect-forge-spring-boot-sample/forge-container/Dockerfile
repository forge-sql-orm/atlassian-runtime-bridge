# Build from repository root (see docker-compose.yml / manifest.yml tunnel context).
# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY bridge-common bridge-common
COPY bridge-connect-container bridge-connect-container
COPY examples/atlassian-connect-forge-spring-boot-sample examples/atlassian-connect-forge-spring-boot-sample

# Install parent POM only (reactor lists bridge-forge-connect; do not copy it into the image).
RUN mvn -B -q install -N \
    -DskipTests -Dspotbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true \
    -Dspotless.skip=true -Djacoco.skip=true -DinstallGitHooks.skip=true \
    && mvn -B -q install -f bridge-common/pom.xml \
    -DskipTests -Dspotbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true \
    -Dspotless.skip=true -Djacoco.skip=true -DinstallGitHooks.skip=true \
    && mvn -B -q install -f bridge-connect-container/pom.xml \
    -DskipTests -Dspotbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true \
    -Dspotless.skip=true -Djacoco.skip=true -DinstallGitHooks.skip=true \
    && mvn -B -q package -f examples/atlassian-connect-forge-spring-boot-sample/pom.xml \
    -pl forge-container -am \
    -DskipTests -Dspotbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true \
    -Dspotless.skip=true -Djacoco.skip=true -DinstallGitHooks.skip=true

FROM eclipse-temurin:21-jre-jammy

ARG ENABLE_DEBUG=false
RUN useradd -u 1000 -r -s /bin/false appuser

WORKDIR /opt/service
COPY --from=builder /workspace/examples/atlassian-connect-forge-spring-boot-sample/forge-container/target/sample-forge-container-*.jar /opt/service/service.jar

ENV FORGE_EGRESS_PROXY_URL=http://proxy-sidecar:7072

EXPOSE 8080
EXPOSE 8000
USER appuser

# Optional remote debug: docker run -e ENABLE_DEBUG=true ...
ENTRYPOINT ["sh", "-c", "exec java -XX:InitialRAMPercentage=15 -XX:MaxRAMPercentage=35 ${ENABLE_DEBUG:+ -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000} -jar /opt/service/service.jar"]
