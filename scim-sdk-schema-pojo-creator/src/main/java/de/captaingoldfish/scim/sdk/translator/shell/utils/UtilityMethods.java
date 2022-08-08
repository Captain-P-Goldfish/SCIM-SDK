package de.captaingoldfish.scim.sdk.translator.shell.utils;

import java.io.File;

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

}
