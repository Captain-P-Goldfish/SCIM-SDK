/**
 * <br>
 * <br>
 * created at: 12.05.2020
 * @author Pascal Knüppel
 */
module de.captaingoldfish.scim.sdk.client {

  requires static lombok;
  requires static org.mapstruct.processor;

  requires de.captaingoldfish.scim.sdk.common;

  requires org.apache.commons.lang3;
  requires org.apache.httpcomponents.httpclient;
  requires org.apache.httpcomponents.httpcore;
  requires org.apache.commons.io;

  requires org.bouncycastle.provider;

  requires transitive com.fasterxml.jackson.databind;

  /* for unit testing only */
  requires static jdk.httpserver;

}
