/**
 * author Pascal Knueppel <br>
 * created at: 02.11.2019 - 22:19 <br>
 * <br>
 */
module de.captaingoldfish.scim.sdk.common {

  requires static lombok;

  requires org.slf4j;
  requires org.apache.commons.lang3;
  requires com.fasterxml.jackson.databind;
  requires java.ws.rs;
  requires java.activation;

  exports de.captaingoldfish.scim.sdk.common.constants;
  exports de.captaingoldfish.scim.sdk.common.constants.enums;
  exports de.captaingoldfish.scim.sdk.common.exceptions;
  exports de.captaingoldfish.scim.sdk.common.request;
  exports de.captaingoldfish.scim.sdk.common.resources;
  exports de.captaingoldfish.scim.sdk.common.response;
  exports de.captaingoldfish.scim.sdk.common.schemas;
  exports de.captaingoldfish.scim.sdk.common.utils;
  exports de.captaingoldfish.scim.sdk.common.resources.base;
  exports de.captaingoldfish.scim.sdk.common.resources.complex;
  exports de.captaingoldfish.scim.sdk.common.resources.multicomplex;
  exports de.captaingoldfish.scim.sdk.common.etag;

  opens de.captaingoldfish.scim.sdk.common.meta;
  opens de.captaingoldfish.scim.sdk.common.request;
  opens de.captaingoldfish.scim.sdk.common.resourcetypes;
  opens de.captaingoldfish.scim.sdk.common.response;
  opens de.captaingoldfish.scim.sdk.common.schemas;
}
