package de.captaingoldfish.scim.sdk.translator.classbuilder.constructor;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.translator.utils.SharedMethods;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 29.01.2022
 */
@RequiredArgsConstructor
public class ConstructorParameter
{

  private final String type;

  private final String name;

  public ConstructorParameter(SchemaAttribute schemaAttribute)
  {
    this.name = SharedMethods.getAttributeName(schemaAttribute);
    this.type = SharedMethods.getSetterMethodParameterType(schemaAttribute);
  }

  @Override
  public String toString()
  {
    return String.format("%s %s", type, name);
  }
}
