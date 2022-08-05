package de.captaingoldfish.scim.sdk.translator.shell.schemareader;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 28.01.2022
 */
@Getter
@RequiredArgsConstructor
public class FileInfoWrapper
{

  private final File resourceFile;

  private final JsonNode jsonNode;
}
