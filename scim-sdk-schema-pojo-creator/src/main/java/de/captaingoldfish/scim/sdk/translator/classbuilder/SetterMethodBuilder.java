package de.captaingoldfish.scim.sdk.translator.classbuilder;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 02.05.2021
 */
public class SetterMethodBuilder
{

  @Getter
  private String setterCall;

  protected String generateSimpleSetterMethod(SchemaAttribute schemaAttribute)
  {
    final String capitalizedAttributeName = StringUtils.capitalize(schemaAttribute.getName());
    String setterMethodParameterType = getSetterMethodParameterType(schemaAttribute);
    String setterMethodCall = getSetterMethodCall(schemaAttribute);
    String attributeName = schemaAttribute.isMultiValued() ? schemaAttribute.getName() + "List"
      : schemaAttribute.getName();
    String javadoc = StringUtils.isBlank(schemaAttribute.getDescription()) ? ""
      : String.format("  /** %s */\n", schemaAttribute.getDescription());
    this.setterCall = String.format("set%s(%s);", capitalizedAttributeName, attributeName);
    // @formatter:off
    return String.format("%s  public void set%s(%s %s)\n" +
                         "  {\n " +
                         "    %s\n" +
                         "  }\n",
                         javadoc,
                         capitalizedAttributeName,
                         setterMethodParameterType,
                         attributeName, 
                         setterMethodCall);
    // @formatter:on
  }

  private String getSetterMethodCall(SchemaAttribute schemaAttribute)
  {
    String attributeTypeForSetter = Type.DATE_TIME.equals(schemaAttribute.getType()) && !schemaAttribute.isMultiValued()
      ? "DateTime" : "";
    String fieldName = schemaAttribute.getName().replaceAll("([A-Z])", "_$1").toUpperCase(Locale.ROOT);
    String attributeName = schemaAttribute.isMultiValued() ? schemaAttribute.getName() + "List"
      : schemaAttribute.getName();
    String methodNameSuffix = getMethodNameSuffix(schemaAttribute);
    return String.format("set%sAttribute%s(FieldNames.%s, %s);",
                         attributeTypeForSetter,
                         methodNameSuffix,
                         fieldName,
                         attributeName);
  }

  private String getMethodNameSuffix(SchemaAttribute schemaAttribute)
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

  private String getSetterMethodParameterType(SchemaAttribute schemaAttribute)
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
}
