package de.captaingoldfish.scim.sdk.translator.classbuilder.constructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.translator.classbuilder.setter.SetterMethod;
import de.captaingoldfish.scim.sdk.translator.classbuilder.setter.SetterMethodBuilder;
import de.captaingoldfish.scim.sdk.translator.utils.SharedMethods;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 29.01.2022
 */
@RequiredArgsConstructor
public class ConstructorBuilder
{

  private final Schema resourceSchema;

  private final List<Schema> schemaExtensions;

  @Override
  public String toString()
  {
    String constructorName = StringUtils.capitalize(resourceSchema.getNonNullId());
    String constructorSetterCalls = getConstructorParametersString();
    String constructorParameterString = getConstructorParamsString();
    return String.format("public %s (%s)\n{\n%s\n}\n",
                         constructorName,
                         constructorParameterString,
                         constructorSetterCalls);
  }

  private String getConstructorParamsString()
  {
    List<ConstructorParameter> constructorParameters = ConstructorParameterBuilder.getConstructorParams(resourceSchema,
                                                                                                        schemaExtensions);
    Optional.ofNullable(schemaExtensions).ifPresent(extensions -> {
      extensions.forEach(extension -> {
        final String extensionName = SharedMethods.getSchemaClassname(extension);
        final String extensionParamName = extension.getNonNullId();
        constructorParameters.add(new ConstructorParameter(extensionName, extensionParamName));
      });
    });
    String constructorParameterString = constructorParameters.stream()
                                                             .map(ConstructorParameter::toString)
                                                             .collect(Collectors.joining(",\n"));
    return constructorParameterString;
  }

  private String getConstructorParametersString()
  {
    SetterMethodBuilder setterMethodBuilder = new SetterMethodBuilder();
    List<SetterMethod> setterMethodList = resourceSchema.getAttributes()
                                                        .stream()
                                                        .map(setterMethodBuilder::generateSimpleSetterMethod)
                                                        .collect(Collectors.toList());
    String constructorSetterCalls = setterMethodList.stream()
                                                    .map(SetterMethod::getCallSetter)
                                                    .collect(Collectors.joining(","));
    return constructorSetterCalls;
  }
}
