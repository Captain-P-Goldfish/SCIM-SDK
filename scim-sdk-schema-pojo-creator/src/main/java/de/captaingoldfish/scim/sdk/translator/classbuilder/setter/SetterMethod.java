package de.captaingoldfish.scim.sdk.translator.classbuilder.setter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 29.01.2022
 */
@Getter
@RequiredArgsConstructor
public class SetterMethod
{

  private final String javadoc;

  private final String methodName;

  private final String parameterType;

  private final String parameterName;

  @Getter
  private final String methodBody;

  public String getMethodParameter()
  {
    return String.format("%s %s", parameterType, parameterName);
  }

  public String getCallSetter()
  {
    return String.format("%s(%s);", methodName, parameterName);
  }

  @Override
  public String toString()
  {
    // @formatter:off
    return String.format("%s  public void %s(%s %s)\n" +
                         "  {\n " +
                         "    %s\n" +
                         "  }\n",
                         javadoc,
                         methodName,
                         parameterType,
                         parameterName,
                         methodBody);
    // @formatter:on
  }
}
