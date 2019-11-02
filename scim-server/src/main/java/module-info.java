/**
 * author Pascal Knueppel <br>
 * created at: 02.11.2019 - 22:43 <br>
 * <br>
 */
module de.golden.palace.scim.server {

  requires static lombok;

  requires de.golden.palace.scim.common;

  exports de.gold.scim.server.endpoints;
  exports de.gold.scim.server.endpoints.base;
  exports de.gold.scim.server.filter;
//  exports de.gold.scim.server.filter.antlr;
  exports de.gold.scim.server.response;
  exports de.gold.scim.server.schemas;


  requires org.slf4j;
  requires org.apache.commons.lang3;
  requires com.fasterxml.jackson.databind;
  requires org.antlr.antlr4.runtime;

}
