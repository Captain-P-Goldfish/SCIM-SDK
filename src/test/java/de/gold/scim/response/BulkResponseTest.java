package de.gold.scim.response;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.InvalidSchemaException;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.schemas.ResourceTypeFactoryUtil;
import de.gold.scim.schemas.Schema;
import de.gold.scim.schemas.SchemaFactory;
import de.gold.scim.schemas.SchemaValidator;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 23:34 <br>
 * <br>
 */
public class BulkResponseTest
{


  /**
   * verifies that the correct schema uri is set
   */
  @Test
  public void testSchemaUriIsSet()
  {
    BulkResponse bulkResponse = new BulkResponse();
    Assertions.assertEquals(1, bulkResponse.getSchemas().size());
    Assertions.assertEquals(SchemaUris.BULK_RESPONSE_URI, bulkResponse.getSchemas().get(0));
  }

  /**
   * verifies that the getter and setter methods are working as expected
   */
  @Test
  public void testGetAndSetValues()
  {
    final List<BulkResponseOperation> operations = Collections.singletonList(BulkResponseOperation.builder().build());

    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();

    Assertions.assertEquals(operations, bulkResponse.getBulkResponseOperations());

    Assertions.assertEquals(operations.size(), bulkResponse.get("Operations").size());
  }

  /**
   * verifies that the bulk request operation is never null if getter is called
   */
  @Test
  public void testOperationsIsNeverNull()
  {
    BulkResponse bulkResponse = new BulkResponse();
    Assertions.assertNotNull(bulkResponse.getBulkResponseOperations());
  }

  /**
   * this test will verify that the bulk request can be validated against its schema
   */
  @Test
  public void testSchemaValidation()
  {
    final String method = "POST";
    final String bulkId = UUID.randomUUID().toString();
    final String version = UUID.randomUUID().toString();
    final String location = EndpointPaths.USERS + "/" + UUID.randomUUID().toString();
    final Integer status = HttpStatus.SC_OK;
    final ErrorResponse response = new ErrorResponse(new InvalidSchemaException("invalid syntax", null,
                                                                                HttpStatus.SC_BAD_REQUEST,
                                                                                ScimType.RFC7644.INVALID_SYNTAX));

    List<BulkResponseOperation> operations = Collections.singletonList(BulkResponseOperation.builder()
                                                                                            .method(method)
                                                                                            .bulkId(bulkId)
                                                                                            .version(version)
                                                                                            .location(location)
                                                                                            .status(status)
                                                                                            .response(response)
                                                                                            .build());
    BulkResponse bulkResponse = BulkResponse.builder().bulkResponseOperation(operations).build();
    ResourceTypeFactory resourceTypeFactory = new ResourceTypeFactory();
    SchemaFactory schemaFactory = ResourceTypeFactoryUtil.getSchemaFactory(resourceTypeFactory);
    Schema bulkResponseSchema = schemaFactory.getMetaSchema(SchemaUris.BULK_RESPONSE_URI);
    Assertions.assertDoesNotThrow(() -> SchemaValidator.validateSchemaDocument(resourceTypeFactory,
                                                                               bulkResponseSchema,
                                                                               bulkResponse.getAsJsonNode()));
  }
}
