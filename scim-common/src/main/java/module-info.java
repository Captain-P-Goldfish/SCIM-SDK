/**
 * author Pascal Knueppel <br>
 * created at: 02.11.2019 - 22:19 <br>
 * <br>
 */
module de.golden.palace.scim.common {

  requires static lombok;

  requires org.slf4j;
  requires org.apache.commons.lang3;
  requires com.fasterxml.jackson.databind;
  requires java.ws.rs;
  requires java.xml.bind;
  requires java.activation;

  exports de.gold.scim.common.constants;
  exports de.gold.scim.common.constants.enums;
  exports de.gold.scim.common.exceptions;
  exports de.gold.scim.common.request;
  exports de.gold.scim.common.resources;
  exports de.gold.scim.common.response;
  exports de.gold.scim.common.schemas;
  exports de.gold.scim.common.utils;
  exports de.gold.scim.common.resources.base;
  exports de.gold.scim.common.resources.complex;
  exports de.gold.scim.common.resources.multicomplex;

  opens de.gold.scim.common.meta;
  opens de.gold.scim.common.request;
  opens de.gold.scim.common.resourcetypes;
  opens de.gold.scim.common.response;
  opens de.gold.scim.common.schemas;
}
