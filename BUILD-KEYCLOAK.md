#### Build app

`mvn install -DskipTests
`
#### Deploy

In directory _`scim-for-keycloak`_ run

`scim-for-keycloak>mvn deploy -DskipTests`

#### Copy to destination

`copy scim-for-keycloak-deployment\target\scim-for-keycloak.ear <KEYCLOAK_DIR>\standalone\deployments`

### Restart keycloak

### Buid docker
Unter `http://jenkins:8080/job/MID-Keycloak/`
