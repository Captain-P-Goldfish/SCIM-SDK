package de.gold.scim.server.response;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.common.constants.EndpointPaths;
import de.gold.scim.common.constants.HttpStatus;
import de.gold.scim.common.constants.SchemaUris;
import de.gold.scim.common.constants.ScimType;
import de.gold.scim.common.constants.enums.HttpMethod;
import de.gold.scim.common.exceptions.InvalidSchemaException;
import de.gold.scim.common.response.BulkResponse;
import de.gold.scim.common.response.BulkResponseOperation;
import de.gold.scim.common.response.ErrorResponse;
import de.gold.scim.common.schemas.Schema;
import de.gold.scim.server.schemas.ResourceTypeFactory;
import de.gold.scim.server.schemas.ResourceTypeFactoryUtil;
import de.gold.scim.server.schemas.SchemaFactory;
import de.gold.scim.server.schemas.SchemaValidator;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 23:34 <br>
 * <br>
 */
public class BulkResponseTest
{

  /**
   * this test will verify that the bulk request can be validated against its schema
   */
  @Test
  public void testSchemaValidation()
  {
    final HttpMethod method = HttpMethod.POST;
    final String bulkId = UUID.randomUUID().toString();
    final String version = UUID.randomUUID().toString();
    final String location = EndpointPaths.USERS + "/" + UUID.randomUUID().toString();
    final Integer status = HttpStatus.OK;
    final ErrorResponse response = new ErrorResponse(new InvalidSchemaException("invalid syntax", null,
                                                                                HttpStatus.BAD_REQUEST,
                                                                                ScimType.RFC7644.INVALID_SYNTAX));
    List<BulkResponseOperation> operations = Collections.singletonList(BulkResponseOperation.builder()
                                                                                            .method(method)
                                                                                            .bulkId(bulkId)
                                                                                            .version(version)
                                                                                            .location(location)
                                                                                            .status(status)
                                                                                            .response(response)
                                                                                            .build());
    BulkResponse bulkResponse = BulkResponse.builder()
                                            .httpStatus(HttpStatus.OK)
                                            .bulkResponseOperation(operations)
                                            .build();
    Assertions.assertEquals(HttpStatus.OK, bulkResponse.getHttpStatus());
    ResourceTypeFactory resourceTypeFactory = new ResourceTypeFactory();
    SchemaFactory schemaFactory = ResourceTypeFactoryUtil.getSchemaFactory(resourceTypeFactory);
    Schema bulkResponseSchema = schemaFactory.getMetaSchema(SchemaUris.BULK_RESPONSE_URI);
    Assertions.assertDoesNotThrow(() -> SchemaValidator.validateSchemaDocument(resourceTypeFactory,
                                                                               bulkResponseSchema,
                                                                               bulkResponse));
  }
}
