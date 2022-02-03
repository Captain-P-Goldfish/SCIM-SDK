package de.captaingoldfish.scim.sdk.translator.classbuilder.setter;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.translator.utils.SharedMethods;


/**
 * @author Pascal Knueppel
 * @since 02.05.2021
 */
public class SetterMethodBuilder
{

  public SetterMethod generateSimpleSetterMethod(SchemaAttribute schemaAttribute)
  {
    final String capitalizedAttributeName = StringUtils.capitalize(schemaAttribute.getName());
    String setterMethodParameterType = SharedMethods.getSetterMethodParameterType(schemaAttribute);
    String setterMethodCall = getSetterMethodCall(schemaAttribute);
    String attributeName = SharedMethods.getAttributeName(schemaAttribute);
    String javadoc = StringUtils.isBlank(schemaAttribute.getDescription()) ? ""
      : String.format("  /** %s */\n", schemaAttribute.getDescription());
    return new SetterMethod(javadoc, String.format("set%s", capitalizedAttributeName), setterMethodParameterType,
                            attributeName, setterMethodCall);
  }

  private String getSetterMethodCall(SchemaAttribute schemaAttribute)
  {
    String attributeTypeForSetter = Type.DATE_TIME.equals(schemaAttribute.getType()) && !schemaAttribute.isMultiValued()
      ? "DateTime" : "";
    String fieldName = schemaAttribute.getName().replaceAll("([A-Z])", "_$1").toUpperCase(Locale.ROOT);
    String attributeName = SharedMethods.getAttributeName(schemaAttribute);
    String methodNameSuffix = SharedMethods.getMethodNameSuffix(schemaAttribute);
    return String.format("set%sAttribute%s(FieldNames.%s, %s);",
                         attributeTypeForSetter,
                         methodNameSuffix,
                         fieldName,
                         attributeName);
  }

}
