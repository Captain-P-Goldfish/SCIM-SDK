package de.captaingoldfish.scim.sdk.translator.utils;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * @author Pascal Knueppel
 * @since 02.05.2021
 */
public class SharedMethods
{

  public static String getReturnTypeAttribute(SchemaAttribute schemaAttribute)
  {
    Type type = schemaAttribute.getType();
    switch (type)
    {
      case BOOLEAN:
        return Boolean.class.getSimpleName();
      case DECIMAL:
        return Double.class.getSimpleName();
      case INTEGER:
        return Long.class.getSimpleName();
      case DATE_TIME:
        return Instant.class.getSimpleName();
      case COMPLEX:
        return StringUtils.capitalize(schemaAttribute.getName());
      default:
        return String.class.getSimpleName();
    }
  }

  public static String getSetterMethodParameterType(SchemaAttribute schemaAttribute)
  {
    String returnType = SharedMethods.getReturnTypeAttribute(schemaAttribute);
    if (schemaAttribute.isMultiValued())
    {
      return String.format("List<%s>", returnType);
    }
    else
    {
      return returnType;
    }
  }

  public static String getMethodNameSuffix(SchemaAttribute schemaAttribute)
  {
    if (schemaAttribute.isMultiValued())
    {
      return Type.COMPLEX.equals(schemaAttribute.getType()) ? "" : "List";
    }
    else
    {
      return "";
    }
  }

  public static String getAttributeName(SchemaAttribute schemaAttribute)
  {
    return schemaAttribute.isMultiValued() ? schemaAttribute.getName() + "List" : schemaAttribute.getName();
  }

  public static String getSchemaClassname(Schema schema)
  {
    return StringUtils.capitalize(schema.getName().orElse("Unknown"));
  }
}
