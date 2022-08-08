package de.captaingoldfish.scim.sdk.translator.shell.utils;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 08.08.2022 - 00:19 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UtilityMethods
{

  public static String toClasspath(File file)
  {
    final String absolutePath = file.getAbsolutePath().replaceAll("\\\\", "/");
    if (absolutePath.matches(".*?src/.*?/resources.*"))
    {
      return absolutePath.replaceFirst(".*?src/.*?/resources(.*)", "$1");
    }
    else
    {
      return file.getAbsolutePath();
    }
  }

  public static String getResourcesPackage(String packageName, boolean filePath)
  {
    String packagePath = String.format("%s.resources", packageName);
    return filePath ? toFilePath(packagePath) : packagePath;
  }

  public static String getEndpointsPackage(String packageName, boolean filePath)
  {
    String packagePath = String.format("%s.endpoints", packageName);
    return filePath ? toFilePath(packagePath) : packagePath;
  }

  public static String getResourceHandlerPackage(String packageName, boolean filePath)
  {
    String packagePath = String.format("%s.resourcehandler", packageName);
    return filePath ? toFilePath(packagePath) : packagePath;
  }

  public static String getScimConfigPackage(String packageName, boolean filePath)
  {
    String packagePath = String.format("%s.projectconfig", packageName);
    return filePath ? toFilePath(packagePath) : packagePath;
  }

  private static String toFilePath(String packagePath)
  {
    return packagePath.replaceAll("\\.", "/");
  }

  public static String getResourceName(String resourceJsonName)
  {
    return StringUtils.capitalize(resourceJsonName).replaceAll("\\s", "");
  }

  public static String getEndpointDefinitionName(String resourceTypeName)
  {
    return String.format("%sEndpointDefinition", getResourceName(resourceTypeName));
  }

  public static String getResourceHandlerName(String resourceJsonName)
  {
    return String.format("%sResourceHandler", getResourceName(resourceJsonName));
  }
}
