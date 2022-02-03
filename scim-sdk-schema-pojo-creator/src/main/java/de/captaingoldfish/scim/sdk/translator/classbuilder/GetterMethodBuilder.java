package de.captaingoldfish.scim.sdk.translator.classbuilder;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.translator.utils.SharedMethods;


/**
 * @author Pascal Knueppel
 * @since 29.04.2021
 */
public class GetterMethodBuilder
{

  protected String generateSimpleGetterMethod(SchemaAttribute schemaAttribute)
  {
    final String capitalizedAttributeName = StringUtils.capitalize(schemaAttribute.getName());
    String methodReturnType = getGetterMethodReturnType(schemaAttribute);
    String methodName = getGetterMethodCall(schemaAttribute);
    String javadoc = StringUtils.isBlank(schemaAttribute.getDescription()) ? ""
      : String.format("  /** %s */\n", schemaAttribute.getDescription());
    // @formatter:off
    return String.format("%s  public %s get%s()\n" +
                         "  {\n " +
                         "    return %s\n" +
                         "  }\n",
                         javadoc,
                         methodReturnType,
                         capitalizedAttributeName,
                         methodName);
    // @formatter:on
  }

  private String getGetterMethodReturnType(SchemaAttribute schemaAttribute)
  {
    String returnType = SharedMethods.getReturnTypeAttribute(schemaAttribute);
    if (schemaAttribute.isMultiValued())
    {
      if (Uniqueness.NONE.equals(schemaAttribute.getUniqueness()) || Type.COMPLEX.equals(schemaAttribute.getType()))
      {
        return String.format("List<%s>", returnType);
      }
      else
      {
        return String.format("Set<%s>", returnType);
      }
    }
    else
    {
      if (!schemaAttribute.isRequired())
      {
        returnType = String.format("Optional<%s>", returnType);
      }
      return returnType;
    }
  }

  private String getGetterMethodCall(SchemaAttribute schemaAttribute)
  {
    if (schemaAttribute.isMultiValued())
    {
      return getMultivaluedAttributeMethodName(schemaAttribute);
    }
    else
    {
      return getSimpleAttributeMethodName(schemaAttribute);
    }
  }

  private String getMultivaluedAttributeMethodName(SchemaAttribute schemaAttribute)
  {
    final String snakCaseAttributeName = schemaAttribute.getName()
                                                        .replaceAll("([A-Z])", "_$1")
                                                        .toUpperCase(Locale.ROOT);
    String methodNameSuffix = Uniqueness.NONE.equals(schemaAttribute.getUniqueness()) ? "" : "Set";
    String returnType = SharedMethods.getReturnTypeAttribute(schemaAttribute);
    if (String.class.getSimpleName().equals(returnType))
    {
      returnType = "";
    }
    else
    {
      returnType = String.format(", %s.class", returnType);
    }
    String methodCall;
    if (Type.COMPLEX.equals(schemaAttribute.getType()))
    {
      methodNameSuffix = "";
      methodCall = "getArrayAttribute";
    }
    else
    {
      methodCall = "getSimpleArrayAttribute";
    }
    return String.format("%s%s(FieldNames.%s%s);", methodCall, methodNameSuffix, snakCaseAttributeName, returnType);
  }

  private String getSimpleAttributeMethodName(SchemaAttribute schemaAttribute)
  {
    final String returnType = SharedMethods.getReturnTypeAttribute(schemaAttribute);
    final String defaultReturnValue = returnType.equals(Boolean.class.getSimpleName()) ? "false" : "null";
    final String methodEnd = schemaAttribute.isRequired() ? String.format(".orElse(%s)", defaultReturnValue) : "";
    final String methodNamePart = getSimpleAttributeMethodNameType(schemaAttribute.getType());
    final String snakCaseAttributeName = schemaAttribute.getName()
                                                        .replaceAll("([A-Z])", "_$1")
                                                        .toUpperCase(Locale.ROOT);
    final String objectAttributeType = Type.COMPLEX.equals(schemaAttribute.getType())
      ? ", " + StringUtils.capitalize(schemaAttribute.getName()) + ".class" : "";
    return String.format("get%sAttribute(FieldNames.%s%s)%s;",
                         methodNamePart,
                         snakCaseAttributeName,
                         objectAttributeType,
                         methodEnd);
  }

  private String getSimpleAttributeMethodNameType(Type type)
  {
    switch (type)
    {
      case BOOLEAN:
        return Boolean.class.getSimpleName();
      case DECIMAL:
        return Double.class.getSimpleName();
      case INTEGER:
        return Long.class.getSimpleName();
      case DATE_TIME:
        return "DateTime";
      case COMPLEX:
        return Object.class.getSimpleName();
      default:
        return String.class.getSimpleName();
    }
  }
}
