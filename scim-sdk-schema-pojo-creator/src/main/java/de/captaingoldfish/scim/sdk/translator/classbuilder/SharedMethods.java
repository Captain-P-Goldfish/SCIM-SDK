package de.captaingoldfish.scim.sdk.translator.classbuilder;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * @author Pascal Knueppel
 * @since 02.05.2021
 */
class SharedMethods
{

  protected static String getReturnTypeAttribute(SchemaAttribute schemaAttribute)
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
}
