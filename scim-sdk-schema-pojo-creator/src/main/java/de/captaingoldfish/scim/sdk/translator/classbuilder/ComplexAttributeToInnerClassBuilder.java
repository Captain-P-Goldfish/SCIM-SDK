package de.captaingoldfish.scim.sdk.translator.classbuilder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 02.05.2021
 */
@Slf4j
public class ComplexAttributeToInnerClassBuilder
{

  protected String generateComplexAttributeClass(SchemaAttribute schemaAttribute)
  {
    final String getterMethod = new GetterMethodBuilder().generateSimpleGetterMethod(schemaAttribute);
    final SetterMethodBuilder setterMethodBuilder = new SetterMethodBuilder();
    final String setterMethod = setterMethodBuilder.generateSimpleSetterMethod(schemaAttribute);

    final String javadoc = String.format("/** %s */", schemaAttribute.getDescription());
    final String className = StringUtils.capitalize(schemaAttribute.getName());
    final String innerClassStructure = buildInnerClassStructure(schemaAttribute);

    return String.format("  %s\n  %s\n  %s\n  public static class %s\n  {\n    %s\n  }",
                         getterMethod,
                         setterMethod,
                         javadoc,
                         className,
                         innerClassStructure);
  }

  private String buildInnerClassStructure(SchemaAttribute schemaAttribute)
  {

    List<String> constructorAttributes = new ArrayList<>();
    List<String> setterMethodCalls = new ArrayList<>();
    List<String> getterAndSetterMethodDefinitions = new ArrayList<>();

    for ( SchemaAttribute subAttribute : schemaAttribute.getSubAttributes() )
    {
      final GetterMethodBuilder getterMethodBuilder = new GetterMethodBuilder();
      final SetterMethodBuilder setterMethodBuilder = new SetterMethodBuilder();
      String getterMethod = getterMethodBuilder.generateSimpleGetterMethod(subAttribute);
      String setterMethod = setterMethodBuilder.generateSimpleSetterMethod(subAttribute);

      getterAndSetterMethodDefinitions.add(String.format("%s\n%s", getterMethod, setterMethod));
      setterMethodCalls.add(setterMethodBuilder.getSetterCall());
      constructorAttributes.add(setterMethodBuilder.getSetterParameter());
    }

    String constructor = getConstructor(schemaAttribute, constructorAttributes, setterMethodCalls);
    StringBuilder getterAndSetterMethodCalls = new StringBuilder();
    for ( int i = 0 ; i < getterAndSetterMethodDefinitions.size() ; i++ )
    {
      String getterAndSetterDefinition = getterAndSetterMethodDefinitions.get(i);
      getterAndSetterMethodCalls.append(getterAndSetterDefinition).append('\n');
    }

    return String.format("%s%s", constructor, getterAndSetterMethodCalls);
  }

  private String getConstructor(SchemaAttribute schemaAttribute,
                                List<String> constructorAttributes,
                                List<String> setterMethodCalls)
  {
    String constructorParams = String.join(", ", constructorAttributes);
    String setterCalls = String.join("\n    ", setterMethodCalls);
    String constructorName = StringUtils.capitalize(schemaAttribute.getName());
    return String.format("public %s(%s) \n    {\n    %s\n    }\n", constructorName, constructorParams, setterCalls);
  }
}
