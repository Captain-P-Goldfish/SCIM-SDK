FROM jboss/keycloak

# keycloak settings
ENV KEYCLOAK_USER=admin
ENV KEYCLOAK_PASSWORD=admin

# database settings
ENV DB_VENDOR=mysql
ENV DB_PORT=3306
ENV DB_USER=root
ENV DB_PASSWORD=123456
ENV DB_DATABASE=keycloak

ARG VOLUME_DIR=/opt/jboss/mnt
ARG DEPLOYMENT_DIR=/opt/jboss/keycloak/standalone/deployments

RUN mkdir ${VOLUME_DIR}

# Copy the freshly built artifact
COPY maven/ ${DEPLOYMENT_DIR}

ENTRYPOINT /opt/jboss/tools/docker-entrypoint.sh
