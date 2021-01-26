package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import lombok.AllArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 20.10.2019 - 12:16 <br>
 * <br>
 */
@AllArgsConstructor
public class SchemaHandler extends ResourceHandler<Schema>
{

  /**
   * creates the error message for the not supported operations
   */
  private static final Function<String, String> ERROR_MESSAGE_SUPPLIER = operation -> {
    return "the '" + operation + "'-operation is not supported for Schemas";
  };

  /**
   * needed for unit tests to prevent application context pollution
   */
  private final ResourceTypeFactory resourceTypeFactory;

  /**
   * creating of schemas not supported
   */
  @Override
  public Schema createResource(Schema resource, Authorization authorization)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("create"));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Schema getResource(String id,
                            Authorization authorization,
                            List<SchemaAttribute> attributes,
                            List<SchemaAttribute> excludedAttributes)
  {
    Schema schema = resourceTypeFactory.getAllResourceTypes()
                                       .stream()
                                       .map(ResourceType::getAllSchemas)
                                       .flatMap(Collection::stream)
                                       .distinct()
                                       .filter(s -> id.equals(s.getId().orElse(null)))
                                       .findAny()
                                       .orElse(null);
    if (schema == null)
    {
      throw new ResourceNotFoundException("a schema with the uri identifier '" + id + "' does not exist", null, null);
    }
    return schema;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PartialListResponse<Schema> listResources(long startIndex,
                                                   int count,
                                                   FilterNode filter,
                                                   SchemaAttribute sortBy,
                                                   SortOrder sortOrder,
                                                   List<SchemaAttribute> attributes,
                                                   List<SchemaAttribute> excludedAttributes,
                                                   Authorization authorization)
  {
    List<Schema> allSchemas = resourceTypeFactory.getAllResourceTypes()
                                                 .stream()
                                                 .map(ResourceType::getAllSchemas)
                                                 .flatMap(Collection::stream)
                                                 .distinct()
                                                 .collect(Collectors.toList());
    return PartialListResponse.<Schema> builder().resources(allSchemas).totalResults(allSchemas.size()).build();
  }

  /**
   * updating of schemas not supported
   */
  @Override
  public Schema updateResource(Schema schema, Authorization authorization)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("update"));
  }

  /**
   * deleting of schemas not supported
   */
  @Override
  public void deleteResource(String id, Authorization authorization)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("delete"));
  }
}
