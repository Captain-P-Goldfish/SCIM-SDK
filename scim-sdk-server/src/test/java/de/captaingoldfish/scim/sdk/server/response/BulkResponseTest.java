package de.captaingoldfish.scim.sdk.server.response;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidSchemaException;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactoryUtil;
import de.captaingoldfish.scim.sdk.server.schemas.SchemaFactory;
import de.captaingoldfish.scim.sdk.server.schemas.SchemaValidator;


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
