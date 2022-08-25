package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import com.fasterxml.jackson.databind.node.ArrayNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 25.08.2022 - 18:43 <br>
 * <br>
 */
public class BulkIdReferenceArrayWrapper implements BulkIdReferenceWrapper
{

  private final ArrayNode parentNode;

  private final int index;

  @Getter
  private final String bulkId;

  public BulkIdReferenceArrayWrapper(ArrayNode parentNode, int index)
  {
    this.parentNode = parentNode;
    this.index = index;
    this.bulkId = parentNode.get(index).textValue().replace(String.format("%s:", AttributeNames.RFC7643.BULK_ID), "");
  }

  @Override
  public void replaceValueNode(String newValue)
  {
    parentNode.remove(index);
    parentNode.insert(index, newValue);
  }
}
