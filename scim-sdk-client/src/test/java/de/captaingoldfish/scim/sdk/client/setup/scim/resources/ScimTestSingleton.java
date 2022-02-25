package de.captaingoldfish.scim.sdk.client.setup.scim.resources;


import java.util.Collections;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import lombok.Builder;


/**
 * @author Pascal Knueppel
 * @since 25.02.2022
 */
public class ScimTestSingleton extends ResourceNode
{

  public ScimTestSingleton()
  {}

  @Builder
  public ScimTestSingleton(String id, String singletonAttribute, Meta meta)
  {
    setSchemas(Collections.singletonList(FieldNames.SCHEMA_ID));
    setId(id);
    setSingletonAttribute(singletonAttribute);
    setMeta(meta);
  }

  /** any string. */
  public Optional<String> getSingletonAttribute()
  {
    return getStringAttribute(FieldNames.SINGLETON_ATTRIBUTE);
  }

  /** any string. */
  public void setSingletonAttribute(String singletonAttribute)
  {
    setAttribute(FieldNames.SINGLETON_ATTRIBUTE, singletonAttribute);
  }


  public static class FieldNames
  {

    public static final String SCHEMA_ID = "urn:ietf:params:scim:schemas:core:2.0:TestSingleton";

    public static final String ID = "id";

    public static final String SINGLETON_ATTRIBUTE = "singletonAttribute";
  }
}
