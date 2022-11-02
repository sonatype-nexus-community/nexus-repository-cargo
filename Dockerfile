# declaration of NEXUS_VERSION must appear before first FROM command
# see: https://docs.docker.com/engine/reference/builder/#understand-how-arg-and-from-interact
ARG NEXUS_VERSION=latest

FROM maven:3-jdk-8-alpine AS build

COPY . /nexus-repository-cargo/
# requires BUILDKIT, caches .m2 between builds.  Use --no-cache to build from scratch
RUN --mount=type=cache,target=/root/.m2 cd /nexus-repository-cargo/; \
    mvn clean package -PbuildKar;

FROM sonatype/nexus3:$NEXUS_VERSION

ARG DEPLOY_DIR=/opt/sonatype/nexus/deploy/
USER root
COPY --from=build /nexus-repository-cargo/target/nexus-repository-cargo-*-bundle.kar ${DEPLOY_DIR}
# Uncomment the next line to enable nexus console.  Useful for debugging.
# RUN sed -e 's/\(-Dkaraf.startLocalConsole=\)false/\1true/g' /opt/sonatype/nexus/bin/nexus.vmoptions
USER nexus
