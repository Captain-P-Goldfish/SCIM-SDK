package de.gold.scim.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.schemas.ResourceTypeFactoryUtil;
import de.gold.scim.schemas.Schema;
import de.gold.scim.schemas.SchemaFactory;
import de.gold.scim.schemas.SchemaValidator;
import de.gold.scim.utils.FileReferences;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 23:01 <br>
 * <br>
 */
public class BulkRequestTest implements FileReferences
{

  /**
   * verifies that the correct schema uri is set
   */
  @Test
  public void testSchemaUriIsSet()
  {
    BulkRequest bulkRequest = new BulkRequest();
    Assertions.assertEquals(1, bulkRequest.getSchemas().size());
    Assertions.assertEquals(SchemaUris.BULK_REQUEST_URI, bulkRequest.getSchemas().get(0));
  }

  /**
   * verifies that the getter and setter methods are working as expected
   */
  @Test
  public void testGetAndSetValues()
  {
    final Integer failOnErrors = 5;
    final List<BulkRequestOperation> operations = Arrays.asList(BulkRequestOperation.builder().build(),
                                                                BulkRequestOperation.builder().build());

    BulkRequest bulkRequest = BulkRequest.builder().failOnErrors(failOnErrors).bulkRequestOperation(operations).build();

    Assertions.assertEquals(failOnErrors, bulkRequest.getFailOnErrors().get());
    Assertions.assertEquals(operations, bulkRequest.getBulkRequestOperations());

    Assertions.assertEquals(failOnErrors, bulkRequest.get("failOnErrors").intValue());
    Assertions.assertEquals(operations.size(), bulkRequest.get("Operations").size());
  }

  /**
   * verifies that the bulk request operation is never null if getter is called
   */
  @Test
  public void testOperationsIsNeverNull()
  {
    BulkRequest bulkRequest = new BulkRequest();
    Assertions.assertNotNull(bulkRequest.getBulkRequestOperations());
  }

  /**
   * this test will verify that the bulk request can be validated against its schema
   */
  @Test
  public void testSchemaValidation()
  {
    final Integer failOnErrors = 5;
    final String method = "POST";
    final String bulkId = UUID.randomUUID().toString();
    final String path = EndpointPaths.USERS;
    final String data = readResourceFile(USER_RESOURCE);

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
