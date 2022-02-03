package de.captaingoldfish.scim.sdk.translator.classbuilder.constructor;

import java.util.List;
import java.util.stream.Collectors;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;


/**
 * @author Pascal Knueppel
 * @since 29.01.2022
 */
public class ConstructorParameterBuilder
{

  public static List<ConstructorParameter> getConstructorParams(Schema schema, List<Schema> schemaExtensions)
  {
    List<ConstructorParameter> constructorParameterList = schema.getAttributes()
                                                                .stream()
                                                                .map(ConstructorParameter::new)
                                                                .collect(Collectors.toList());

    return constructorParameterList;
  }
}
