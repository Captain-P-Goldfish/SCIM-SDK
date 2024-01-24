package de.captaingoldfish.scim.sdk.server.patch.operations;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.utils.ScimAttributeHelper;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 12.01.2024
 */
@Getter
public abstract class PatchOperation<T extends JsonNode> implements ScimAttributeHelper
{

  /**
   * the schema that is representational for the patch-operation
   */
  protected final Schema schema;

  /**
   * the representational attribute that is being patched
   */
  protected final SchemaAttribute schemaAttribute;

  /**
   * the operation that should be applied on the given attribute
   */
  protected final PatchOp patchOp;

  protected final T valuesNode;

  protected final List<String> valueStringList;

  public PatchOperation(SchemaAttribute schemaAttribute, PatchOp patchOp, JsonNode valuesNode)
  {
    this.schema = schemaAttribute.getSchema();
    this.schemaAttribute = schemaAttribute;
    this.patchOp = patchOp;
    this.valuesNode = parseJsonNode(valuesNode);
    this.valueStringList = new ArrayList<>();
  }

  public PatchOperation(Schema schema, SchemaAttribute schemaAttribute, PatchOp patchOp, JsonNode valuesNode)
  {
    this.schema = schema;
    this.schemaAttribute = schemaAttribute;
    this.patchOp = patchOp;
    this.valuesNode = parseJsonNode(valuesNode);
    this.valueStringList = new ArrayList<>();
  }

  /**
   * will parse the given jsonNode into its appropriate representation
   */
  public abstract T parseJsonNode(JsonNode jsonNode);

  /**
   * @return the representational path-expression on the attribute
   */
  public abstract AttributePathRoot getAttributePath();

  public boolean isWithFilter()
  {
    return getAttributePath().getChild() != null;
  }

}
