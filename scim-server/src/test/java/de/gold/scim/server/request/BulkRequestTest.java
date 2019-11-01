package de.gold.scim.server.request;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.common.constants.EndpointPaths;
import de.gold.scim.common.constants.SchemaUris;
import de.gold.scim.common.constants.enums.HttpMethod;
import de.gold.scim.common.request.BulkRequest;
import de.gold.scim.common.request.BulkRequestOperation;
import de.gold.scim.common.schemas.Schema;
import de.gold.scim.server.schemas.ResourceTypeFactory;
import de.gold.scim.server.schemas.ResourceTypeFactoryUtil;
import de.gold.scim.server.schemas.SchemaFactory;
import de.gold.scim.server.schemas.SchemaValidator;
import de.gold.scim.server.utils.FileReferences;


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
    Assertions.assertDoesNotThrow(() -> SchemaValidator.validateSchemaDocument(resourceTypeFactory,
                                                                               bulkRequestSchema,
                                                                               bulkRequest));
  }
}
