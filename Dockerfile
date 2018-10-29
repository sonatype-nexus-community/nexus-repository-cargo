ARG NEXUS_VERSION=3.12.0
ARG NEXUS_BUILD=01
ARG PLUGIN_GROUP=org.sonatype.nexus.plugins
ARG PLUGIN_ARTIFACT=nexus-repository-cargo
ARG PLUGIN_VERSION=0.0.1-SNAPSHOT

FROM maven:3-jdk-8-alpine AS build
ARG NEXUS_VERSION=3.12.0
ARG NEXUS_BUILD=01
ARG PLUGIN_GROUP=org.sonatype.nexus.plugins
ARG PLUGIN_ARTIFACT=nexus-repository-cargo
ARG PLUGIN_VERSION=0.0.1-SNAPSHOT
WORKDIR /${PLUGIN_ARTIFACT}/
COPY pom.xml .
RUN sed -i "s/3.12.0-01/${NEXUS_VERSION}-${NEXUS_BUILD}/g" pom.xml && \
    sed -i "s|<version>0.0.1</version>|<version>${PLUGIN_VERSION}</version>|g" pom.xml && \
    mvn -B -e -C dependency:resolve dependency:resolve-plugins

COPY src src
RUN mvn

FROM sonatype/nexus3:${NEXUS_VERSION}
ARG NEXUS_VERSION=3.12.0
ARG NEXUS_BUILD=01
ARG PLUGIN_GROUP=org.sonatype.nexus.plugins
ARG PLUGIN_ARTIFACT=nexus-repository-cargo
ARG PLUGIN_VERSION=0.0.1-SNAPSHOT
ARG PLUGIN_TARGET=/opt/sonatype/nexus/system/org/sonatype/nexus/plugins/${PLUGIN_ARTIFACT}/${PLUGIN_VERSION}/
USER root

WORKDIR ${PLUGIN_TARGET}
COPY --from=build /${PLUGIN_ARTIFACT}/target/${PLUGIN_ARTIFACT}-${PLUGIN_VERSION}.jar ${PLUGIN_TARGET}

WORKDIR /opt/sonatype/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/${NEXUS_VERSION}-${NEXUS_BUILD}/
RUN sed -i "s@nexus-repository-maven</feature>@nexus-repository-maven</feature>\n        <feature version=\"${PLUGIN_VERSION}\" prerequisite=\"false\" dependency=\"false\">${PLUGIN_ARTIFACT}</feature>@g" nexus-core-feature-${NEXUS_VERSION}-${NEXUS_BUILD}-features.xml; \
    sed -i "s@<feature name=\"nexus-repository-maven\"@<feature name=\"${PLUGIN_ARTIFACT}\" description=\"${PLUGIN_GROUP}:${PLUGIN_ARTIFACT}\" version=\"${PLUGIN_VERSION}\">\n        <details>${PLUGIN_GROUP}:${PLUGIN_ARTIFACT}</details>\n        <bundle>mvn:${PLUGIN_GROUP}/${PLUGIN_ARTIFACT}/${PLUGIN_VERSION}</bundle>\n    </feature>\n    <feature name=\"nexus-repository-maven\"@g" nexus-core-feature-${NEXUS_VERSION}-${NEXUS_BUILD}-features.xml;

USER nexus