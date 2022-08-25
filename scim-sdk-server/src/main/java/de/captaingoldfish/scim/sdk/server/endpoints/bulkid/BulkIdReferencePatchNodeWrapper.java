package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 22.08.2022
 */
public class BulkIdReferencePatchNodeWrapper implements BulkIdReferenceWrapper
{

  private static final Pattern GET_BULK_ID_PATTERN = Pattern.compile("^bulkId:(.*)|\"bulkId:(.*?)\"");

  private final PatchRequestOperation patchRequestOperation;

  private final String operationValue;

  private final int valueIndex;

  @Getter
  private final String bulkId;

  public BulkIdReferencePatchNodeWrapper(PatchRequestOperation patchRequestOperation,
                                         String operationValue,
                                         int valueIndex)
  {
    this.patchRequestOperation = patchRequestOperation;
    this.operationValue = operationValue;
    this.valueIndex = valueIndex;

    Matcher matcher = GET_BULK_ID_PATTERN.matcher(operationValue);
    matcher.find();
    this.bulkId = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
  }

  @Override
  public void replaceValueNode(String newValue)
  {
    List<String> operations = patchRequestOperation.getValues();
    operations.remove(valueIndex);
    String newOperationValue = operationValue.replaceAll(String.format("%s:%s", AttributeNames.RFC7643.BULK_ID, bulkId),
                                                         newValue);
    operations.add(valueIndex, newOperationValue);
    patchRequestOperation.setValues(operations);
  }
}
