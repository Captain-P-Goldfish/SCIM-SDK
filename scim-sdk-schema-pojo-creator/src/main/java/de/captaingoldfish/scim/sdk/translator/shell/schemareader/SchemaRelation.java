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

  private final JsonNode resourceType;

  private final Schema resourceSchema;

  private final List<Schema> extensions;
}
