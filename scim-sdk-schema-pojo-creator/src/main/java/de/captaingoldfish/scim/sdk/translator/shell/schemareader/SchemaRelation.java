package de.captaingoldfish.scim.sdk.translator.shell.schemareader;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


/**
 * @author Pascal Knueppel
 * @since 28.01.2022
 */
@Getter
@Setter
@RequiredArgsConstructor
public class SchemaRelation
{

  /**
   * the json representation of the resource type. Might be null
   */
  private final FileInfoWrapper resourceType;

  /**
   * the resource schema that is referenced within the {@link #resourceType}.
   */
  private final FileInfoWrapper resourceSchema;

  /**
   * the extensions of the {@link #resourceType}. If the {@link #resourceType} is null this collection will be
   * empty
   */
  private final List<FileInfoWrapper> extensions;
}
