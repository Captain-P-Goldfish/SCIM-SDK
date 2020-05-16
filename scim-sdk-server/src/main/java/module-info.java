/**
 * author Pascal Knueppel <br>
 * created at: 02.11.2019 - 22:43 <br>
 * <br>
 */
module de.captaingoldfish.scim.sdk.server {

  requires static lombok;

  requires de.captaingoldfish.scim.sdk.common;

  exports de.captaingoldfish.scim.sdk.server.endpoints;
  exports de.captaingoldfish.scim.sdk.server.endpoints.base;
  exports de.captaingoldfish.scim.sdk.server.endpoints.authorize;
  exports de.captaingoldfish.scim.sdk.server.filter;
  exports de.captaingoldfish.scim.sdk.server.response;
  exports de.captaingoldfish.scim.sdk.server.schemas;
  exports de.captaingoldfish.scim.sdk.server.schemas.custom;

  requires org.slf4j;
  requires org.apache.commons.lang3;
  requires com.fasterxml.jackson.databind;
  requires org.antlr.antlr4.runtime;

}
