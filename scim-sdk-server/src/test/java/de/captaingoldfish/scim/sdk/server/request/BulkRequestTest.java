package de.captaingoldfish.scim.sdk.server.request;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactoryUtil;
import de.captaingoldfish.scim.sdk.server.schemas.SchemaFactory;
import de.captaingoldfish.scim.sdk.server.schemas.SchemaValidator;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 23:01 <br>
 * <br>
 */
public class BulkRequestTest implements FileReferences
{

  /**
   * this test will verify that the bulk request can be validated against its schema
   */
  @Test
  public void testSchemaValidation()
  {
    final Integer failOnErrors = 5;
    final HttpMethod method = HttpMethod.POST;
    final String bulkId = UUID.randomUUID().toString();
    final String path = EndpointPaths.USERS;
    final String data = readResourceFile(FileReferences.USER_RESOURCE);

    final List<BulkRequestOperation> operations = Collections.singletonList(BulkRequestOperation.builder()
                                                                                                .method(method)
                                                                                                .bulkId(bulkId)
                                                                                                .path(path)
                                                                                                .data(data)
                                                                                                .build());

    BulkRequest bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();
    ResourceTypeFactory resourceTypeFactory = new ResourceTypeFactory();
    SchemaFactory schemaFactory = ResourceTypeFactoryUtil.getSchemaFactory(resourceTypeFactory);
    Schema bulkRequestSchema = schemaFactory.getMetaSchema(SchemaUris.BULK_REQUEST_URI);
    Assertions.assertDoesNotThrow(() -> SchemaValidator.validateSchemaDocument(bulkRequestSchema, bulkRequest));
  }
}
