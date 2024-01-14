package de.captaingoldfish.scim.sdk.server.patch;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames.RFC7643;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidFilterException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpointBridge;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.AllTypesHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.GroupHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.msazure.MsAzurePatchComplexValueRebuilder;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 30.10.2019 - 09:27 <br>
 * <br>
 */
@Slf4j
public class PatchTargetHandlerTest implements FileReferences
{

  /**
   * contains the current patch configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * needed to extract the {@link ResourceType}s which are necessary to check if the given attribute-names are
   * valid or not
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource type for all types definition. Contains data types of any possible scim representation
   */
  private ResourceType allTypesResourceType;

  /**
   * the resource-handler that can handle the requests for {@link AllTypes}
   */
  private AllTypesHandlerImpl allTypesResourceHandler;

  /**
   * used to access the default
   * {@link de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround}-handlers
   */
  private ResourceEndpoint resourceEndpoint;

  /**
   * initializes a new {@link ResourceTypeFactory} for the following tests
   */
  @BeforeEach
  public void initialize()
  {
    this.serviceProvider = ServiceProvider.builder().patchConfig(PatchConfig.builder().supported(true).build()).build();
    JsonNode allTypesResourceType = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    this.resourceTypeFactory = ResourceEndpointBridge.getResourceTypeFactory(resourceEndpoint);

    this.allTypesResourceHandler = new AllTypesHandlerImpl();
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceType,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         this.allTypesResourceHandler));
  }

  /**
   * adds an allTypes object to the {@link #allTypesHandler}
   */
  private void addAllTypesToProvider(AllTypes allTypes)
  {
    if (!allTypes.getId().isPresent())
    {
      allTypes.setId(UUID.randomUUID().toString());
    }
    allTypesResourceHandler.getInMemoryMap().put(allTypes.getId().get(), allTypes);
  }

  /**
   * adds an user to the given userhandler
   */
  private void addUserToProvider(UserHandlerImpl userHandler, User user)
  {
    if (!user.getId().isPresent())
    {
      user.setId(UUID.randomUUID().toString());
    }
    userHandler.getInMemoryMap().put(user.getId().get(), user);
  }

  /**
   * adds a group to the given grouphandler
   */
  private void addGroupToProvider(GroupHandlerImpl groupHandler, Group group)
  {
    if (!group.getId().isPresent())
    {
      group.setId(UUID.randomUUID().toString());
    }
    groupHandler.getInMemoryMap().put(group.getId().get(), group);
  }

  private AllTypes patchAllTypes(AllTypes allTypes, PatchOpRequest patchOpRequest, boolean expectChanged)
  {
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    try
    {
      AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.assertEquals(expectChanged, patchRequestHandler.isResourceChanged(), patchedAllTypes.toPrettyString());
      return patchedAllTypes;
    }
    catch (Exception ex)
    {
      log.debug(ex.getMessage(), ex);
      Assertions.assertEquals(expectChanged, patchRequestHandler.isResourceChanged());
      throw ex;
    }
  }

  /**
   * this test will verify that the patch operation is able to add values that are not yet present within the
   * resource
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "decimal,5.8", "bool,true", "bool,false", "date,1996-03-10T00:00:00Z"})
  public void testAddSimpleAttribute(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes(true);

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertNotNull(patchedAllTypes.get(attributeName));
    Assertions.assertEquals(value, patchedAllTypes.get(attributeName).asText());
    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has(attributeName));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * this test will verify that the patch operation is able to add values that are not yet present within the
   * resource
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "decimal,5.8", "bool,true", "bool,false", "date,1996-03-10T00:00:00Z",
              "binary,aGVsbG8gZ29sZGZpc2g="})
  public void testReplaceSimpleAttribute(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    AllTypes allTypes = new AllTypes(true);

    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertNotNull(patchedAllTypes.get(attributeName));
    Assertions.assertEquals(value, patchedAllTypes.get(attributeName).asText());
    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has(attributeName));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * will verify that already existing attributes are getting replaced by the add operation
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "decimal,5.8", "bool,true", "date,1996-03-10T00:00:00Z",
              "binary,aGVsbG8gZ29sZGZpc2g="})
  public void testAddSimpleAttributeWithExistingValues(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class);
    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);
    Assertions.assertNotNull(patchedAllTypes.get(attributeName));
    Assertions.assertEquals(value, patchedAllTypes.get(attributeName).asText());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * will verify that already existing attributes are getting replaced by the replace operation
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "decimal,5.8", "bool,true", "date,1996-03-10T00:00:00Z",
              "binary,aGVsbG8gZ29sZGZpc2g="})
  public void testReplaceSimpleAttributeWithExistingValues(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class);

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertNotNull(patchedAllTypes.get(attributeName));
    Assertions.assertEquals(value, patchedAllTypes.get(attributeName).asText());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that several values can be added to simple string array types
   */
  @Test
  public void testAddSimpleStringArrayAttribute()
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("stringArray"));
    Assertions.assertEquals(values.size(), patchedAllTypes.getStringArray().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a string array can be added with the replace operation
   */
  @Test
  public void testAddSimpleStringArrayAttributeWithReplaceOperation()
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("stringArray"));
    Assertions.assertEquals(values.size(), patchedAllTypes.getStringArray().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that the modification of an attribute in a complex attribute is marked as unchanged if the new
   * value matches the old value
   */
  @Test
  public void testAddComplexStringAttributeWithReplaceOperationWithoutChange()
  {
    String attributeName = "complex.string";
    String value = "hello world";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .value(value)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString(value);
    allTypes.setComplex(complex);

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertFalse(patchRequestHandler.isResourceChanged());

    Assertions.assertNotNull(patchedAllTypes.getComplex().orElse(null));
    Assertions.assertEquals(value, patchedAllTypes.getComplex().get().getString().get());
  }

  /**
   * verifies that a string array can be replaced with the replace operation
   */
  @Test
  public void testReplaceSimpleStringArrayAttribute()
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setStringArray(Arrays.asList("1", "2", "3", "4", "humpty dumpty"));

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("stringArray"));
    Assertions.assertEquals(values.size(), patchedAllTypes.getStringArray().size());
    MatcherAssert.assertThat(patchedAllTypes.getStringArray(), Matchers.hasItems(values.toArray(new String[0])));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a binary array can be replaced with the replace operation
   */
  @Test
  public void testReplaceSimpleBinaryArrayAttribute()
  {
    List<String> values = Arrays.asList(Base64.getEncoder().encodeToString("hello world".getBytes()),
                                        Base64.getEncoder().encodeToString("goodbye world".getBytes()));
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("binaryarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setBinaryArray(Arrays.asList("1".getBytes(),
                                          "2".getBytes(),
                                          "3".getBytes(),
                                          "4".getBytes(),
                                          "humpty dumpty".getBytes()));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("binaryArray"));
    Assertions.assertEquals(values.size(), patchedAllTypes.getBinaryArray().size());
    MatcherAssert.assertThat(patchedAllTypes.getBinaryArray()
                                            .stream()
                                            .map(Base64.getEncoder()::encodeToString)
                                            .collect(Collectors.toList()),
                             Matchers.hasItems(values.toArray(new String[0])));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a binary array can be replaced with the replace operation
   */
  @Test
  public void testReplaceComplexBinaryArrayAttribute()
  {
    List<String> values = Arrays.asList(Base64.getEncoder().encodeToString("hello world".getBytes()),
                                        Base64.getEncoder().encodeToString("goodbye world".getBytes()));
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("complex.binaryarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(true);
    complex.setBinaryArray(Arrays.asList("1".getBytes(),
                                         "2".getBytes(),
                                         "3".getBytes(),
                                         "4".getBytes(),
                                         "humpty dumpty".getBytes()));
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(2, complex.size(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals(values.size(), patchedAllTypes.getComplex().get().getBinaryArray().size());
    MatcherAssert.assertThat(patchedAllTypes.getComplex()
                                            .get()
                                            .getBinaryArray()
                                            .stream()
                                            .map(Base64.getEncoder()::encodeToString)
                                            .collect(Collectors.toList()),
                             Matchers.hasItems(values.toArray(new String[0])));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that several values can be added to simple number array types
   */
  @ParameterizedTest
  @ValueSource(strings = {"numberarray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberarray"})
  public void testAddSimpleNumberArrayAttribute(String attributeName)
  {
    List<String> values = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 0).map(String::valueOf).collect(Collectors.toList());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("numberArray"));
    Assertions.assertEquals(values.size(), patchedAllTypes.getNumberArray().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that several values can be added to simple number array types with the replace operation
   */
  @ParameterizedTest
  @ValueSource(strings = {"numberarray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberarray"})
  public void testReplaceSimpleNumberArrayAttribute(String attributeName)
  {
    List<String> values = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 0).map(String::valueOf).collect(Collectors.toList());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("numberArray"));
    Assertions.assertEquals(values.size(), patchedAllTypes.getNumberArray().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that several values can be added to simple number array types
   */
  @ParameterizedTest
  @ValueSource(strings = {"numberarray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberarray"})
  public void testAddSimpleNumberArrayAttributeAndPreserveOldValues(String attributeName)
  {
    List<String> values = Stream.of(9, 0).map(String::valueOf).collect(Collectors.toList());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    List<Long> numberList = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    allTypes.setNumberArray(numberList);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("numberArray"));
    Assertions.assertEquals(10, patchedAllTypes.getNumberArray().size());
    MatcherAssert.assertThat(patchedAllTypes.getNumberArray(), Matchers.hasItems(numberList.toArray(new Long[0])));
    MatcherAssert.assertThat(patchedAllTypes.getNumberArray(), Matchers.hasItems(9L, 0L));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that an array attribute can be replaced
   */
  @ParameterizedTest
  @ValueSource(strings = {"numberarray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberarray"})
  public void testReplaceSimpleNumberArrayAttributeAndPreserveOldValues(String attributeName)
  {
    List<String> values = Stream.of(9, 0).map(String::valueOf).collect(Collectors.toList());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    List<Long> numberList = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    allTypes.setNumberArray(numberList);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("numberArray"));
    Assertions.assertEquals(2, patchedAllTypes.getNumberArray().size());
    MatcherAssert.assertThat(patchedAllTypes.getNumberArray(), Matchers.hasItems(9L, 0L));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that simple attribute can successfully be added to complex types
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "number," + Long.MAX_VALUE, "decimal,5.8", "bool,true", "bool,false",
              "date,1996-03-10T00:00:00Z", "binary,aGVsbG8gZ29sZGZpc2g="})
  public void testAddSimpleAttributeToComplexType(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("complex." + attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertEquals(value, patchedAllTypes.getComplex().get().get(attributeName).asText());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that simple attribute can successfully be added to complex types with the replace operation
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "number," + Long.MAX_VALUE, "decimal,5.8", "bool,true", "bool,false",
              "date,1996-03-10T00:00:00Z", "binary,aGVsbG8gZ29sZGZpc2g="})
  public void testAddSimpleAttributeToComplexTypeWithReplaceOperation(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("complex." + attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals(value, patchedAllTypes.getComplex().get().get(attributeName).asText());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that simple attribute can successfully be added to complex types while preserving already existing
   * values
   */
  @ParameterizedTest
  @ValueSource(strings = {"complex.stringarray",
                          "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.stringarray"})
  public void testAddSimpleAttributeToComplexTypeAndPreserveOldValues(String attributeName)
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("salty");
    AllTypes complex = new AllTypes(false);
    complex.setString("sweet");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(5, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.has("string"));
    Assertions.assertTrue(patchedAllTypes.getString().isPresent());
    Assertions.assertEquals("salty", patchedAllTypes.getString().get());
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getString().isPresent());
    Assertions.assertEquals("sweet", patchedAllTypes.getComplex().get().getString().get());
    Assertions.assertEquals("happy day", patchedAllTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("hello world", patchedAllTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertEquals("goodbye world", patchedAllTypes.getComplex().get().getStringArray().get(2));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that arrays on complex types can be replaced
   */
  @ParameterizedTest
  @ValueSource(strings = {"complex.stringarray",
                          "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.stringarray"})
  public void testReplaceArrayOnComplexType(String attributeName)
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("salty");
    AllTypes complex = new AllTypes(false);
    complex.setString("sweet");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(5, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.has("string"));
    Assertions.assertTrue(patchedAllTypes.getString().isPresent());
    Assertions.assertEquals("salty", patchedAllTypes.getString().get());
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getString().isPresent());
    Assertions.assertEquals("sweet", patchedAllTypes.getComplex().get().getString().get());
    Assertions.assertEquals(2, patchedAllTypes.getComplex().get().getStringArray().size());
    Assertions.assertEquals("hello world", patchedAllTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("goodbye world", patchedAllTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a complex type can be set as whole object
   */
  @ParameterizedTest
  @ValueSource(strings = {"complex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex"})
  public void testAddSimpleAttributeToComplexType(String attributeName)
  {

    // @formatter:off
    List<String> values = Collections.singletonList(  "{"
                                                    + "   \"number\": " + Long.MAX_VALUE + ","
                                                    + "   \"stringArray\":[\"hello world\", \"goodbye world\"]"
                                                    + "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertEquals(2,
                            patchedAllTypes.getComplex().get().size(),
                            patchedAllTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(2, patchedAllTypes.getComplex().get().getStringArray().size());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(Long.MAX_VALUE, patchedAllTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals("hello world", patchedAllTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("goodbye world", patchedAllTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a complex type can be set as whole object with replace operation
   */
  @ParameterizedTest
  @ValueSource(strings = {"complex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex"})
  public void testAddSimpleAttributeToComplexTypeWithReplace(String attributeName)
  {

    // @formatter:off
    List<String> values = Collections.singletonList(  "{"
                                                    + "   \"number\": " + Long.MAX_VALUE + ","
                                                    + "   \"stringArray\":[\"hello world\", \"goodbye world\"]"
                                                    + "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertEquals(2,
                            patchedAllTypes.getComplex().get().size(),
                            patchedAllTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(2, patchedAllTypes.getComplex().get().getStringArray().size());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(Long.MAX_VALUE, patchedAllTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals("hello world", patchedAllTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("goodbye world", patchedAllTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a complex type can be removed from the document
   */
  @ParameterizedTest
  @ValueSource(strings = {"complex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex"})
  public void testAddRemoveComplexType(String attributeName)
  {
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(attributeName)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertFalse(patchedAllTypes.getComplex().isPresent(), patchedAllTypes.toPrettyString());
    MatcherAssert.assertThat(patchedAllTypes.toPrettyString(),
                             patchedAllTypes.getSchemas().size(),
                             Matchers.greaterThan(0));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent(),
                          patchedAllTypes.toPrettyString());
  }

  /**
   * verifies that a complex type can be set as whole object and that existing values are kept
   */
  @Test
  public void testAddSimpleAttributeToComplexTypeWithAlreadyExistingValues()
  {

    // @formatter:off
    List<String> values = Collections.singletonList(  "{"
                                                    + "   \"number\": " + Long.MAX_VALUE + ","
                                                    + "   \"stringArray\":[\"hello world\", \"goodbye world\"]"
                                                    + "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("complex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hf");

    AllTypes complex = new AllTypes(false);
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(5, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.has("string"));
    Assertions.assertTrue(patchedAllTypes.getString().isPresent());
    Assertions.assertEquals("hf", patchedAllTypes.getString().get());
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertEquals(3,
                            patchedAllTypes.getComplex().get().size(),
                            patchedAllTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(3, patchedAllTypes.getComplex().get().getStringArray().size());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertTrue(patchedAllTypes.getString().isPresent());
    Assertions.assertEquals("hf", patchedAllTypes.getString().get());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getString().isPresent());
    Assertions.assertEquals("goldfish", patchedAllTypes.getComplex().get().getString().get());
    Assertions.assertEquals(Long.MAX_VALUE, patchedAllTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals("happy day", patchedAllTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("hello world", patchedAllTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertEquals("goodbye world", patchedAllTypes.getComplex().get().getStringArray().get(2));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a complex type can be completely replaced
   */
  @Test
  public void testReplaceComplexAttribute()
  {

    // @formatter:off
    List<String> values = Collections.singletonList(  "{"
                                                    + "   \"number\": " + Long.MAX_VALUE + ","
                                                    + "   \"stringArray\":[\"hello world\", \"goodbye world\"]"
                                                    + "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("complex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hf");

    AllTypes complex = new AllTypes(false);
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(5, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("string"));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.getString().isPresent());
    Assertions.assertEquals("hf", patchedAllTypes.getString().get());
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertEquals(2,
                            patchedAllTypes.getComplex().get().size(),
                            patchedAllTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(2, patchedAllTypes.getComplex().get().getStringArray().size());
    Assertions.assertFalse(patchedAllTypes.getComplex().get().getString().isPresent());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(Long.MAX_VALUE, patchedAllTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals(2, patchedAllTypes.getComplex().get().getStringArray().size());
    Assertions.assertEquals("hello world", patchedAllTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("goodbye world", patchedAllTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that complex types can be added to a multivalued complex type
   */
  @Test
  public void testAddNewAttributeToMultiValuedComplexNode()
  {
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                        "\"string\": \"hello world\"," +
                                        "\"number\": 5," +
                                        "\"boolArray\": [true, false, true]," +
                                        "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}",

                                        "{" +
                                        "\"string\": \"happy day\"," +
                                        "\"number\": 45678987646454," +
                                        "\"stringArray\": [\"true\", \"wtf\", \"hello world\"]," +
                                        "\"dateArray\": [ \"1976-03-10T00:00:00Z\", \"1986-03-10T00:00:00Z\" ]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("multicomplex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(0)), patchedAllTypes.getMultiComplex().get(0));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(1)), patchedAllTypes.getMultiComplex().get(1));
  }

  /**
   * verifies that complex types can be added to a multivalued complex type with the replace operation
   */
  @Test
  public void testAddNewAttributeToMultiValuedComplexNodeWithReplaceOperation()
  {
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                        "\"string\": \"hello world\"," +
                                        "\"number\": 5," +
                                        "\"boolArray\": [true, false, true]," +
                                        "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}",

                                        "{" +
                                        "\"string\": \"happy day\"," +
                                        "\"number\": 45678987646454," +
                                        "\"stringArray\": [\"true\", \"wtf\", \"hello world\"]," +
                                        "\"dateArray\": [ \"1976-03-10T00:00:00Z\", \"1986-03-10T00:00:00Z\" ]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("multicomplex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(0)), patchedAllTypes.getMultiComplex().get(0));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(1)), patchedAllTypes.getMultiComplex().get(1));
  }

  /**
   * verifies that complex types can be added to a multivalued complex type while preserving old values
   */
  @Test
  public void testAddNewAttributeToMultiValuedComplexNodeAndPreserveOldEntries()
  {
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                        "\"string\": \"hello world\"," +
                                        "\"number\": 5," +
                                        "\"boolArray\": [true, false, true]," +
                                        "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}",

                                        "{" +
                                        "\"string\": \"happy day\"," +
                                        "\"number\": 45678987646454," +
                                        "\"stringArray\": [\"true\", \"wtf\", \"hello world\"]," +
                                        "\"dateArray\": [ \"1976-03-10T00:00:00Z\", \"1986-03-10T00:00:00Z\" ]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("multicomplex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Collections.singletonList(complex));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(complex, patchedAllTypes.getMultiComplex().get(0));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(0)), patchedAllTypes.getMultiComplex().get(1));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(1)), patchedAllTypes.getMultiComplex().get(2));
  }

  /**
   * verifies that a multivalued complex type can be replaced
   */
  @Test
  public void testReplaceMultiValuedComplexType()
  {
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                        "\"string\": \"hello world\"," +
                                        "\"number\": 5," +
                                        "\"boolArray\": [true, false, true]," +
                                        "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}",

                                        "{" +
                                        "\"string\": \"happy day\"," +
                                        "\"number\": 45678987646454," +
                                        "\"stringArray\": [\"true\", \"wtf\", \"hello world\"]," +
                                        "\"dateArray\": [ \"1976-03-10T00:00:00Z\", \"1986-03-10T00:00:00Z\" ]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("multicomplex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Collections.singletonList(complex));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    MatcherAssert.assertThat(patchedAllTypes.getMultiComplex(), Matchers.not(Matchers.hasItem(complex)));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(0)), patchedAllTypes.getMultiComplex().get(0));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(1)), patchedAllTypes.getMultiComplex().get(1));
  }

  /**
   * verifies nothing happens to the resource if the expression did not match<br>
   * the expression would add the stringarray attribute to all multicomplex nodes but since there is no node the
   * stringarray attribute can neither be added nor replaced
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddArrayAttributeToMultiComplexTypeWithMissingTarget(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("hello world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path("multicomplex.stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, false);

    Assertions.assertFalse(patchedAllTypes.getMeta().isPresent());
  }

  /**
   * this test will show that values can be added to arrays within multi complex attributes. If no filter is
   * given then the new value will be added to all complex representations within the multivalued complex array
   */
  @Test
  public void testAddArrayAttributeToMultiComplexType()
  {
    List<String> values = Collections.singletonList("hello world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("multicomplex.stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    AllTypes complex2 = new AllTypes(false);
    complex.setString("goldfish");
    complex2.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * this test will show that arrays within multi valued complex types can be replaced.
   */
  @Test
  public void testReplaceArrayAttributeInMultiComplexType()
  {
    List<String> values = Collections.singletonList("hello world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("multicomplex.stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    AllTypes complex2 = new AllTypes(false);
    complex.setString("goldfish");
    complex2.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("hello world", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(0));
    Assertions.assertEquals("hello world", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(0));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * this test will show that a simple attribute within multivalued complex types can be replaced if already
   * existent
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToMultiComplexType(PatchOp patchOp)
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path("multicomplex.string")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes complex = new AllTypes(false);
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));

    AllTypes complex2 = new AllTypes(false);
    complex2.setString("goldfish");
    complex2.setStringArray(Collections.singletonList("happy day"));

    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4,
                            patchedAllTypes.size(),
                            "multiComplex and meta must be present\n" + patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(value, patchedAllTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(value, patchedAllTypes.getMultiComplex().get(1).getString().get());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that the meta attribute is not touched if no effective change was made
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToMultiComplexTypeWithoutEffectiveChange(PatchOp patchOp)
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path("multicomplex.string")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    AllTypes complex2 = new AllTypes(false);
    complex.setString(value);
    complex2.setString(value);
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, false);

    Assertions.assertEquals(3,
                            patchedAllTypes.size(),
                            "multiComplex and meta must be present\n" + patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(value, patchedAllTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(value, patchedAllTypes.getMultiComplex().get(1).getString().get());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertFalse(patchedAllTypes.getMeta().isPresent());
  }

  /**
   * this test will show that a sanitized exception is thrown if the client gave an illegal value for a complex
   * type injection
   */
  @Test
  public void testAddComplexIntoMultiValuedWithIllegalValue()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals("the value parameters must be valid json representations but was 'goldfish'",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * this test will show that a sanitized exception is thrown if the client gave an illegal value for a complex
   * type replacement
   */
  @Test
  public void testReplaceComplexIntoMultiValuedWithIllegalValue()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"]";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals("the values are expected to be valid json representations for an expression as "
                              + "'multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"]' "
                              + "but was: goldfish",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_PATH, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * this test will show that a sanitized exception is thrown if the target has a none matching filter
   */
  @Test
  public void testReplaceComplexIntoMultiValuedWithNoneMatchingFilter()
  {
    AllTypes multiComplex = new AllTypes(false);
    multiComplex.setString("goldfish");
    final String path = "multiComplex[stringarray eq \"none matching value\"]";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .valueNode(multiComplex)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals("Cannot 'REPLACE' value on path 'multiComplex[stringarray eq "
                              + "\"none matching value\"]' for no matching object was found",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * verifies that an exception is thrown if the client tries to add a non json value into a complex type
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddComplexIntoMultiValuedWithSimpleValue(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals("the value parameters must be valid json representations but was 'goldfish'",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * verifies that an exception is thrown if the client tries to add several values to a simple type
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSeveralValuesToSimpleType(PatchOp patchOp)
  {
    List<String> values = Arrays.asList("value1", "value2");
    final String path = "string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals("Too many values found for 'STRING'-type attribute 'string': [value1, value2]",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * verifies that an exception is thrown if the subAttribute of a expression filter is unknown
   */
  @Test
  public void testAddSimpleAttributeToComplexTypesWithUnknownSubAttribute()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].unknown";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("This point must not be reached");
    }
    catch (ScimException ex)
    {
      ex.printStackTrace();
      Assertions.assertEquals("The attribute with the name 'multiComplex.unknown' is unknown to "
                              + "resource type 'AllTypes'",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_PATH, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * verifies that a simple attribute can be added to multi complex type that will match the given filter
   * expression
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToComplexTypesMatchingFilter(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);
    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(2,
                            patchedAllTypes.getMultiComplex().get(0).size(),
                            patchedAllTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(2,
                            patchedAllTypes.getMultiComplex().get(1).size(),
                            patchedAllTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(2).size(),
                            patchedAllTypes.getMultiComplex().get(2).toPrettyString());
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(1).getString().get());
    Assertions.assertFalse(patchedAllTypes.getMultiComplex().get(2).getString().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that the last modified value is not set if no effective change was made
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToComplexTypesWithoutEffectiveChange(PatchOp patchOp)
  {
    final String value = "happy day";
    List<String> values = Collections.singletonList(value);
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setString(value);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setString(value);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, false);
    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(2,
                            patchedAllTypes.getMultiComplex().get(0).size(),
                            patchedAllTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(2,
                            patchedAllTypes.getMultiComplex().get(1).size(),
                            patchedAllTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(2).size(),
                            patchedAllTypes.getMultiComplex().get(2).toPrettyString());
    Assertions.assertEquals(value, patchedAllTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(value, patchedAllTypes.getMultiComplex().get(1).getString().get());
    Assertions.assertFalse(patchedAllTypes.getMultiComplex().get(2).getString().isPresent());
    Assertions.assertFalse(patchedAllTypes.getMeta().isPresent(),
                           "no effective change has been made so meta must be empty");
  }

  /**
   * verifies that a simple attribute can be added to multi complex types that will match the given filter
   * expression
   */
  @Test
  public void testAddArrayAttributeToComplexTypesMatchingFilter()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(0).size(),
                            patchedAllTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(1).size(),
                            patchedAllTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(2).size(),
                            patchedAllTypes.getMultiComplex().get(2).toPrettyString());
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("hello world", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(0));
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(1));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goodbye world", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(0));
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(1));
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("empty world", patchedAllTypes.getMultiComplex().get(2).getStringArray().get(0));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that multiple values can be added to the stringarray attribute if the filter does match
   */
  @Test
  public void testAddMultipleValuesToComplexTypesMatchingFilterAndJsonNode()
  {
    ArrayNode values = new ArrayNode(JsonNodeFactory.instance);
    values.add("goldfish");
    values.add("pool");
    final String path = "multicomplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .valueNode(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(0).size(),
                            patchedAllTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(1).size(),
                            patchedAllTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(2).size(),
                            patchedAllTypes.getMultiComplex().get(2).toPrettyString());

    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("hello world", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(0));
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(1));
    Assertions.assertEquals("pool", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(2));

    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goodbye world", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(0));
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(1));
    Assertions.assertEquals("pool", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(2));

    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("empty world", patchedAllTypes.getMultiComplex().get(2).getStringArray().get(0));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that multiple values can be added to the stringarray attribute if the filter does match and a java
   * list is used as parameter
   */
  @Test
  public void testAddMultipleValuesToComplexTypesMatchingFilterAndJavaList()
  {
    List<String> values = Arrays.asList("goldfish", "pool");
    final String path = "multicomplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(0).size(),
                            patchedAllTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(1).size(),
                            patchedAllTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(2).size(),
                            patchedAllTypes.getMultiComplex().get(2).toPrettyString());

    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("hello world", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(0));
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(1));
    Assertions.assertEquals("pool", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(2));

    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goodbye world", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(0));
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(1));
    Assertions.assertEquals("pool", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(2));

    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("empty world", patchedAllTypes.getMultiComplex().get(2).getStringArray().get(0));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that an array within a multivalued complex type can be replaced
   */
  @Test
  public void testReplaceArrayAttributeInComplexTypesMatchingFilter()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(0).size(),
                            patchedAllTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(1).size(),
                            patchedAllTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            patchedAllTypes.getMultiComplex().get(2).size(),
                            patchedAllTypes.getMultiComplex().get(2).toPrettyString());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(0));
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(0));
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("empty world", patchedAllTypes.getMultiComplex().get(2).getStringArray().get(0));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that an exception is thrown if the filter does not return any results
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testReplaceWithNotMatchingFilter(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[stringarray eq \"goldfish\" or stringarray eq \"blubb\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(BadRequestException.class, ex.getClass());
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
      Assertions.assertEquals("No target found for path-filter 'multicomplex[stringarray eq \"goldfish\" or "
                              + "stringarray eq \"blubb\"].stringarray'",
                              ex.getMessage());
    }
  }

  /**
   * verifies that an exception is thrown if the filter does not return any results
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testReplaceWithFilterOnNormalComplexSubAttribute(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "complex[string eq \"hello\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setId(UUID.randomUUID().toString());
    AllTypes complex = new AllTypes(false);
    complex.setString("hello");
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);
    Assertions.assertEquals("hello", patchedAllTypes.getComplex().get().getString().get());
    Assertions.assertEquals(1, patchedAllTypes.getComplex().get().getStringArray().size());
    Assertions.assertEquals("goldfish", patchedAllTypes.getComplex().get().getStringArray().get(0));
  }

  /**
   * verifies that the value is only added on values on which the and-filter-expression does match
   */
  @Test
  public void testAndExpressionFilter()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[stringarray eq \"hello world\" and string eq \"blubb\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setString("blubb");
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());

    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().get(0).size());
    Assertions.assertEquals("blubb", patchedAllTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(0));

    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(1).size());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goodbye world", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(0));

    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(2).size());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("empty world", patchedAllTypes.getMultiComplex().get(2).getStringArray().get(0));

    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a not expression filter is correctly evaluated and the values are correctly replaced
   */
  @Test
  public void testNotExpressionFilter()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[not (stringarray eq \"hello world\")].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setString("blubb");
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());

    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().get(0).size());
    Assertions.assertEquals("blubb", patchedAllTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("hello world", patchedAllTypes.getMultiComplex().get(0).getStringArray().get(0));

    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(1).size());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(1).getStringArray().get(0));

    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(2).size());
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("goldfish", patchedAllTypes.getMultiComplex().get(2).getStringArray().get(0));

    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies an add operation will also be handled on an extension
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToExtension(PatchOp patchOp)
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    final String path = "costCenter";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("humpty dumpty").build());

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has(SchemaUris.ENTERPRISE_USER_URI));
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().get().getCostCenter().isPresent());
    Assertions.assertEquals(value, patchedAllTypes.getEnterpriseUser().get().getCostCenter().get());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
    Assertions.assertEquals(2, patchedAllTypes.getSchemas().size(), patchedAllTypes.toPrettyString());
    MatcherAssert.assertThat(patchedAllTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * verifies a remove operation will successfully remove an attribute from an extension
   */
  @Test
  public void testRemoveSimpleAttributeFromExtension()
  {
    final String path = "costCenter";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("humpty dumpty").department("department").build());

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has(SchemaUris.ENTERPRISE_USER_URI));
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().get().getDepartment().isPresent());
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().get().getCostCenter().isPresent());
    Assertions.assertEquals(2, patchedAllTypes.getSchemas().size(), patchedAllTypes.toPrettyString());
    MatcherAssert.assertThat(patchedAllTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * verifies a remove operation will successfully remove a multi valued complex type attribute from an
   * extension
   */
  @Test
  public void testRemoveMultiValuedComplexAttributeFromExtension()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema enterpriseUserSchema = resourceTypeFactory.getSchemaFactory()
                                                     .getResourceSchema(SchemaUris.ENTERPRISE_USER_URI);
    enterpriseUserSchema.addAttribute(emailsDef);


    final String path = RFC7643.EMAILS;
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
    ScimArrayNode emailsArrayNode = new ScimArrayNode(null);
    Email email = Email.builder().value(UUID.randomUUID().toString()).build();
    emailsArrayNode.add(email);
    enterpriseUser.set(RFC7643.EMAILS, emailsArrayNode);
    allTypes.setEnterpriseUser(enterpriseUser);

    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("string"));
    Assertions.assertTrue(patchedAllTypes.getString().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
    Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent());
    MatcherAssert.assertThat(patchedAllTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes"));
  }

  /**
   * verifies adding or replacing a multivalued complex type within an extension does work as specified
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddMultiValuedComplexAttributeToExtension(PatchOp patchOp)
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema enterpriseUserSchema = resourceTypeFactory.getSchemaFactory()
                                                     .getResourceSchema(SchemaUris.ENTERPRISE_USER_URI);
    enterpriseUserSchema.addAttribute(emailsDef);


    // @formatter:off
    final List<String> values = Arrays.asList("{"
                                                  + "\"value\": \"chuck@norris.com\","
                                                  + "\"type\": \"work\","
                                                  + "\"primary\": true"
                                             + "}");
    // @formatter:on
    final String path = RFC7643.EMAILS;
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes"));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has(SchemaUris.ENTERPRISE_USER_URI));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent(),
                          patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent(), patchedAllTypes.toPrettyString());
    ArrayNode emails = (ArrayNode)patchedAllTypes.getEnterpriseUser().get().get(RFC7643.EMAILS);
    Assertions.assertNotNull(emails, patchedAllTypes.toPrettyString());
    Assertions.assertEquals(1, emails.size(), patchedAllTypes.toPrettyString());
    MatcherAssert.assertThat(patchedAllTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * verifies a remove operation will successfully remove also the extension itself if the extension is empty
   * after the removal of the attribute
   */
  @Test
  public void testRemoveSimpleAttributeFromExtensionAndExtensionItself()
  {
    final String path = "costCenter";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("humpty dumpty").build());

    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size(), patchedAllTypes.toPrettyString());
    MatcherAssert.assertThat(patchedAllTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes"));
  }

  /**
   * verifies a remove operation will successfully remove also the extension itself if the extension is empty
   * after the removal of the attribute but in this case we will at first make the manager attribute empty
   */
  @Test
  public void testRemoveSimpleAttributeFromExtensionComplexAndExtensionItself()
  {
    final String path = "manager.value";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().manager(Manager.builder().value("123456").build()).build());

    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size(), patchedAllTypes.toPrettyString());
    MatcherAssert.assertThat(patchedAllTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes"));
  }

  /**
   * verifies a remove operation will successfully remove a field from an extension if the fully qualified path
   * is used
   */
  @Test
  public void testRemoveSimpleAttributeFromExtensionComplexAndExtensionItself2()
  {
    final String path = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().manager(Manager.builder().value("123456").build()).build());

    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size(), patchedAllTypes.toPrettyString());
    MatcherAssert.assertThat(patchedAllTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes"));
  }

  /**
   * verifies an add operation will also be handled on an extension that has not been set yet
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToExtensionIfExtensionNotPresent(PatchOp patchOp)
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    final String path = "costCenter";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has(SchemaUris.ENTERPRISE_USER_URI));
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().get().getCostCenter().isPresent());
    Assertions.assertEquals(value, patchedAllTypes.getEnterpriseUser().get().getCostCenter().get());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that an exception is thrown if the target is missing on a remove operation
   */
  @Test
  public void testMissingTargetForRemove()
  {
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder().op(PatchOp.REMOVE).build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals("Missing target for remove operation", ex.getDetail());
    }
  }

  /**
   * verifies a remove operation will fail if values are added into the request
   */
  @Test
  public void testRemoveWithValuesSet()
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    final String path = "string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals("Values must not be set for remove operation but was: hello world", ex.getDetail());
    }
  }

  /**
   * verifies a remove operation will correctly remove a field if the full qualified name is used
   */
  @Test
  public void testRemoveWithFullyQualifiedPath()
  {
    final String path = "urn:gold:params:scim:schemas:custom:2.0:AllTypes:string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertFalse(patchedAllTypes.getString().isPresent(), patchedAllTypes.toPrettyString());
  }

  /**
   * verifies that an exception is thrown if the target does not exist
   */
  @ParameterizedTest
  @ValueSource(strings = {"string", "stringArray", "complex", "complex.string", "complex.stringarray", "multicomplex"})
  public void testRemoveNotExistingTarget(String path)
  {
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);

    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(BadRequestException.class, ex.getClass());
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
      Assertions.assertEquals(String.format("No target found for path-filter '%s'", path), ex.getMessage());
    }
  }

  /**
   * verifies that a remove operation without filter on a multivalued complex attribute does not cause an error
   */
  @ParameterizedTest
  @ValueSource(strings = {"multicomplex.string", "multicomplex.stringarray"})
  public void testRemoveNotExistingTargetOnMultivaluedComplex(String path)
  {
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(new AllTypes(true), patchOpRequest, false);

    Assertions.assertEquals(2, patchedAllTypes.size());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
  }

  /**
   * verifies removing a simple attribute works as expected
   */
  @Test
  public void testRemoveSimpleAttribute()
  {
    final String path = "string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a simple array attribute works as expected
   */
  @Test
  public void testRemoveSimpleArrayAttribute()
  {
    final String path = "stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    allTypes.setStringArray(Collections.singletonList("hello world"));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that removing an attribute from a complex attribute and the complex attribute is empty afterwards,
   * that the whole complex attribute is removed
   */
  @Test
  public void testRemoveSimpleAttributeWithinComplex()
  {
    final String path = "complex.string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertFalse(patchedAllTypes.getComplex().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a simple attribute from a complex type works as expected
   */
  @Test
  public void testRemoveSimpleAttributeWithinComplexWithSeveralValues()
  {
    final String path = "complex.string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    complex.setNumber(5L);
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertEquals(1, patchedAllTypes.getComplex().get().size());
    Assertions.assertFalse(patchedAllTypes.getComplex().get().getString().isPresent());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(5L, patchedAllTypes.getComplex().get().getNumber().get());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a simple array attribute works as expected
   */
  @Test
  public void testRemoveArrayAttributeFromComplex()
  {
    final String path = "complex.stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setStringArray(Collections.singletonList("hello world"));
    complex.setNumber(5L);
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertEquals(1,
                            patchedAllTypes.getComplex().get().size(),
                            patchedAllTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(0, patchedAllTypes.getComplex().get().getStringArray().size());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(5L, patchedAllTypes.getComplex().get().getNumber().get());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a multi complex type works as expected
   */
  @Test
  public void testRemoveMulticomplex()
  {
    final String path = "multicomplex";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setStringArray(Collections.singletonList("hello world"));
    complex.setNumber(5L);
    AllTypes complex2 = new AllTypes(false);
    complex2.setNumber(3L);
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertEquals(0, patchedAllTypes.getMultiComplex().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing the only value from a multi complex type removes the whole type
   */
  @Test
  public void testRemoveTheOnlyAttributeFromMulticomplex()
  {
    final String path = "multicomplex.number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setNumber(5L);
    allTypes.setMultiComplex(Collections.singletonList(complex));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertEquals(0, patchedAllTypes.getMultiComplex().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a value from a multi complex type works as expected
   */
  @Test
  public void testRemoveMulticomplexSimpleAttribute()
  {
    final String path = "multicomplex.number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setNumber(5L);
    complex.setBool(true);
    allTypes.setMultiComplex(Collections.singletonList(complex));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().size());
    Assertions.assertTrue(patchedAllTypes.getMultiComplex().get(0).getBool().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMultiComplex().get(0).getBool().get());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a value from a multivalued complex type with filter removes the whole complex type if it
   * was the only attribute
   */
  @Test
  public void testRemoveMulticomplexSimpleAttributeByFilter()
  {
    final String path = "multicomplex[number eq 5].number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setNumber(5L);
    AllTypes complex2 = new AllTypes(false);
    complex2.setNumber(10L);
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.getMultiComplex().get(0).getNumber().isPresent());
    Assertions.assertEquals(10L, patchedAllTypes.getMultiComplex().get(0).getNumber().get());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a value from a multivalued complex type with filter works as expected
   */
  @Test
  public void testRemoveMulticomplexSimpleAttributeByFilter2()
  {
    final String path = "multicomplex[string eq \"hello world\"].number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    complex.setNumber(5L);
    complex.setBool(true);
    AllTypes complex2 = new AllTypes(false);
    complex2.setNumber(10L);
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    Assertions.assertFalse(patchedAllTypes.getMultiComplex().get(0).getNumber().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMultiComplex().get(0).getString().isPresent());
    Assertions.assertEquals("hello world", patchedAllTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertTrue(patchedAllTypes.getMultiComplex().get(1).getNumber().isPresent());
    Assertions.assertEquals(10L, patchedAllTypes.getMultiComplex().get(1).getNumber().get());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a whole complex type from an array with a filter works as expected
   */
  @Test
  public void testRemoveMulticomplexByFilter()
  {
    final String path = "multicomplex[string eq \"hello world\"]";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    complex.setNumber(5L);
    complex.setBool(true);
    AllTypes complex2 = new AllTypes(false);
    complex2.setNumber(10L);
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.getMultiComplex().get(0).getNumber().isPresent());
    Assertions.assertEquals(10L, patchedAllTypes.getMultiComplex().get(0).getNumber().get());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies adding and replacing a subattribute to a complex type with a matching filter expression works
   * correctly
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddAttributeWithFilterExpression(PatchOp patchOp)
  {
    final String path = "complex[string eq \"hello world\"].number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(Collections.singletonList("5"))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals(2, patchedAllTypes.getComplex().get().size(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals("hello world",
                            patchedAllTypes.getComplex().get().getString().get(),
                            patchedAllTypes.toPrettyString());
    Assertions.assertEquals(5L, patchedAllTypes.getComplex().get().getNumber().get(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size());
  }

  /**
   * verifies adding and replacing a subattribute to a complex type with a none matching filter will result in
   * an exception
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSubAttributeWithFilterExpressionThatDoesNotMatch(PatchOp patchOp)
  {
    final String path = "complex[string eq \"hello world\"].number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(Collections.singletonList("5"))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("jippie ay yay");
    allTypes.setComplex(complex);

    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(BadRequestException.class, ex.getClass());
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
      Assertions.assertEquals("No target found for path-filter 'complex[string eq \"hello world\"].number'",
                              ex.getMessage());
    }
  }

  /**
   * verifies that an exception is thrown if the filter expression does not match for a complex type
   * attribute<br>
   * the filter here would normally add the given values or replace them if the filter matches but since the
   * filter will not match, an error must occur
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testReplaceComplexTypeValuesWithNoneMatchingFilterPath(PatchOp patchOp)
  {
    final String path = "complex[string eq \"hello world\"]";
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                          "\"string\": \"hello world\"," +
                                          "\"number\": 5," +
                                          "\"boolArray\": [true, false, true]," +
                                          "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("jippie ay yay");
    allTypes.setComplex(complex);

    try
    {
      patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(BadRequestException.class, ex.getClass());
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
      Assertions.assertEquals("No target found for path-filter 'complex[string eq \"hello world\"]'", ex.getMessage());
    }
  }

  /**
   * verifies that the values within a complex type are added/replaced if the filter expression does match
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testReplaceComplexTypeValuesWithFilterPath(PatchOp patchOp)
  {
    final String path = "complex[string eq \"hello world\"]";
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                          "\"string\": \"dooms day\"," +
                                          "\"number\": 5," +
                                          "\"boolArray\": [true, false, true]," +
                                          "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    allTypes.setComplex(complex);

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("complex"));
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getString().isPresent());
    Assertions.assertEquals("dooms day", patchedAllTypes.getComplex().get().getString().get());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(5L, patchedAllTypes.getComplex().get().getNumber().get());
    MatcherAssert.assertThat(patchedAllTypes.getComplex().get().getBoolArray(), Matchers.contains(true, false, true));
    MatcherAssert.assertThat(patchedAllTypes.getComplex().get().getDecimalArray(), Matchers.contains(1.1, 2.2, 3.3));
  }

  /**
   * this test will verify that the implementation follows the defined behaviour of RFC7644 for primary types:
   * <br>
   *
   * <pre>
   *   For multi-valued attributes, a PATCH operation that sets a value's
   *   "primary" sub-attribute to "true" SHALL cause the server to
   *   automatically set "primary" to "false" for any other values in the
   *   array.
   * </pre>
   */
  @Test
  public void testSetNewPrimaryValueWithAdd()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);
    List<Email> emails = Arrays.asList(Email.builder().value("1@1.de").primary(true).build(),
                                       Email.builder().value("2@2.de").build(),
                                       Email.builder().value("3@3.de").build());
    AllTypes allTypes = new AllTypes(true);
    ArrayNode emailArray = new ScimArrayNode(null);
    emails.forEach(emailArray::add);
    allTypes.set(RFC7643.EMAILS, emailArray);

    final String path = "emails";
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                          "\"value\": \"4@4.de\"," +
                                          "\"primary\": true" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    log.debug(patchedAllTypes.toPrettyString());
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size());
    emailArray = (ArrayNode)patchedAllTypes.get(RFC7643.EMAILS);
    Assertions.assertNotNull(emailArray);
    Assertions.assertEquals(4, emailArray.size());
    for ( JsonNode email : emailArray )
    {
      String emailText = email.get(RFC7643.VALUE).textValue();
      if (emailText.equals("4@4.de"))
      {
        Assertions.assertTrue(email.get(RFC7643.PRIMARY).booleanValue(), patchedAllTypes.toPrettyString());
      }
      else
      {
        Assertions.assertNull(email.get(RFC7643.PRIMARY), patchedAllTypes.toPrettyString());
      }
    }
  }

  /**
   * this test will verify that the implementation follows the defined behaviour of RFC7644 for primary types:
   * <br>
   *
   * <pre>
   *   For multi-valued attributes, a PATCH operation that sets a value's
   *   "primary" sub-attribute to "true" SHALL cause the server to
   *   automatically set "primary" to "false" for any other values in the
   *   array.
   * </pre>
   */
  @Test
  public void testSetNewPrimaryValueWithReplaceAndFilter()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);
    List<Email> emails = Arrays.asList(Email.builder().value("1@1.de").primary(true).build(),
                                       Email.builder().value("2@2.de").build(),
                                       Email.builder().value("3@3.de").build());
    AllTypes allTypes = new AllTypes(true);
    ArrayNode emailArray = new ScimArrayNode(null);
    emails.forEach(emailArray::add);
    allTypes.set(RFC7643.EMAILS, emailArray);

    final String path = "emails[value sw \"2\"].primary";
    List<String> values = Arrays.asList("true");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size(), patchedAllTypes.toPrettyString());
    emailArray = (ArrayNode)patchedAllTypes.get(RFC7643.EMAILS);
    Assertions.assertNotNull(emailArray);
    Assertions.assertEquals(3, emailArray.size());
    for ( JsonNode email : emailArray )
    {
      String emailText = email.get(RFC7643.VALUE).textValue();
      if (emailText.equals("2@2.de"))
      {
        Assertions.assertTrue(email.get(RFC7643.PRIMARY).booleanValue(), patchedAllTypes.toPrettyString());
      }
      else
      {
        Assertions.assertNull(email.get(RFC7643.PRIMARY), patchedAllTypes.toPrettyString());
      }
    }
  }

  /**
   * this test will verify that the implementation follows the defined behaviour of RFC7644 for primary types:
   * <br>
   *
   * <pre>
   *   For multi-valued attributes, a PATCH operation that sets a value's
   *   "primary" sub-attribute to "true" SHALL cause the server to
   *   automatically set "primary" to "false" for any other values in the
   *   array.
   * </pre>
   */
  @Test
  public void testSetNewPrimaryValueWithAddAndResourceValue()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);
    List<Email> emails = Arrays.asList(Email.builder().value("1@1.de").primary(true).build(),
                                       Email.builder().value("2@2.de").build(),
                                       Email.builder().value("3@3.de").build());
    AllTypes allTypes = new AllTypes(true);
    ArrayNode emailArray = new ScimArrayNode(null);
    emails.forEach(emailArray::add);
    allTypes.set(RFC7643.EMAILS, emailArray);

    AllTypes patchResource = new AllTypes(true);
    ArrayNode patchEmailArray = new ScimArrayNode(null);
    patchEmailArray.add(Email.builder().value("4@4.de").primary(true).build());
    patchResource.set(RFC7643.EMAILS, patchEmailArray);

    List<String> values = Collections.singletonList(patchResource.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .values(values)
                                                                                .build());


    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size(), patchedAllTypes.toPrettyString());
    emailArray = (ArrayNode)patchedAllTypes.get(RFC7643.EMAILS);
    Assertions.assertNotNull(emailArray, patchedAllTypes.toPrettyString());
    Assertions.assertEquals(4, emailArray.size(), patchedAllTypes.toPrettyString());
    for ( JsonNode email : emailArray )
    {
      String emailText = email.get(RFC7643.VALUE).textValue();
      if (emailText.equals("4@4.de"))
      {
        Assertions.assertTrue(email.get(RFC7643.PRIMARY).booleanValue(), patchedAllTypes.toPrettyString());
      }
      else
      {
        Assertions.assertNull(email.get(RFC7643.PRIMARY), patchedAllTypes.toPrettyString());
      }
    }
  }

  /**
   * this test will verify that the implementation follows the defined behaviour of RFC7644 for primary types:
   * <br>
   *
   * <pre>
   *   For multi-valued attributes, a PATCH operation that sets a value's
   *   "primary" sub-attribute to "true" SHALL cause the server to
   *   automatically set "primary" to "false" for any other values in the
   *   array.
   * </pre>
   */
  @Test
  public void testSetNewPrimaryValueWithReplaceAndResourceValue()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);
    List<Email> emails = Arrays.asList(Email.builder().value("1@1.de").primary(true).build(),
                                       Email.builder().value("2@2.de").build(),
                                       Email.builder().value("3@3.de").build());
    AllTypes originalAllTypes = new AllTypes(true);
    ArrayNode emailArray = new ScimArrayNode(null);
    emails.forEach(emailArray::add);
    originalAllTypes.set(RFC7643.EMAILS, emailArray);

    AllTypes patchResource = new AllTypes(true);
    ArrayNode patchEmailArray = new ScimArrayNode(null);
    patchEmailArray.add(Email.builder().value("4@4.de").primary(true).build());
    patchResource.set(RFC7643.EMAILS, patchEmailArray);

    List<String> values = Collections.singletonList(patchResource.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .values(values)
                                                                                .build());


    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(originalAllTypes, patchOpRequest, true);

    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size(), originalAllTypes.toPrettyString());
    emailArray = (ArrayNode)patchedAllTypes.get(RFC7643.EMAILS);
    Assertions.assertNotNull(emailArray, patchedAllTypes.toPrettyString());
    Assertions.assertEquals(1, emailArray.size(), patchedAllTypes.toPrettyString());
    JsonNode email = emailArray.get(0);
    String emailText = email.get(RFC7643.VALUE).textValue();
    Assertions.assertEquals("4@4.de", emailText);
    Assertions.assertTrue(email.get(RFC7643.PRIMARY).booleanValue(), patchedAllTypes.toPrettyString());
  }

  /**
   * verifies that a simple readOnly attribute cannot be patched.
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE", "REMOVE"})
  public void testPatchReadOnlyAttribute(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    final String path = "id";
    final String newId = "1";
    List<String> values = PatchOp.REMOVE.equals(patchOp) ? null : Collections.singletonList(newId);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    final String resourceId = UUID.randomUUID().toString();
    User user = User.builder().id(resourceId).userName("goldfish").build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(resourceId, patchedUser.getId().get());
  }

  /**
   * will verify that an immutable attribute as the username of a resource cannot be patched if already assigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testPatchAssignedImmutableAttribute(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    TestHelper.modifyAttributeMetaData(userResourceType.getMainSchema(),
                                       "userName",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);

    final String path = "username";
    final String newUsername = "goldfish";
    List<String> values = PatchOp.REMOVE.equals(patchOp) ? null : Collections.singletonList(newUsername);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    final String username = UUID.randomUUID().toString();
    User user = User.builder().id(UUID.randomUUID().toString()).userName(username).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    try
    {
      addUserToProvider(userHandler, user);
      PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
      patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.fail("this point must not be reached\n" + user.toPrettyString());
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      Assertions.assertEquals("The attribute 'userName' is 'IMMUTABLE' and is not unassigned. Current value is: "
                              + username,
                              ex.getDetail());
    }
  }

  /**
   * will verify that an immutable attribute as the username of a resource can be patched if unassigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testPatchUnassignedImmutableAttribute(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));
    JsonNode usernameDef = JsonHelper.readJsonDocument(TestHelper.getAttributeString("userName",
                                                                                     Type.STRING,
                                                                                     false,
                                                                                     true,
                                                                                     true,
                                                                                     Mutability.IMMUTABLE,
                                                                                     Returned.DEFAULT,
                                                                                     Uniqueness.SERVER));
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(usernameDef);

    final String path = "username";
    final String newUsername = "goldfish";
    List<String> values = PatchOp.REMOVE.equals(patchOp) ? null : Collections.singletonList(newUsername);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder().id(UUID.randomUUID().toString()).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(newUsername, patchedUser.getUserName().get());
  }

  /**
   * will verify that an immutable attribute can be unassigned with the remove operation
   */
  @Test
  public void testPatchUnassignedImmutableAttribute()
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    JsonNode usernameDef = JsonHelper.readJsonDocument(TestHelper.getAttributeString("userName",
                                                                                     Type.STRING,
                                                                                     false,
                                                                                     true,
                                                                                     true,
                                                                                     Mutability.IMMUTABLE,
                                                                                     Returned.DEFAULT,
                                                                                     Uniqueness.SERVER));
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(usernameDef);

    final String path = "username";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(PatchOp.REMOVE)
                                                                                .build());

    final String username = "goldfish";
    User user = User.builder().id(UUID.randomUUID().toString()).userName(username).build();
    addUserToProvider(userHandler, user);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchedUser.getUserName().isPresent());
  }

  /**
   * verifies that a readOnly complex type cannot be patched
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE", "REMOVE"})
  public void testChangeReadOnlyOnComplexType(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    SchemaAttribute nameAttribute = userResourceType.getSchemaAttribute("name").get();
    nameAttribute.setMutability(Mutability.READ_ONLY);

    final String path = "name";
    Name name = Name.builder().givenName("chuck").build();
    List<String> values = patchOp.equals(PatchOp.REMOVE) ? null : Arrays.asList(name.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder().name(Name.builder().givenName("Bruce").familyName("Lee").build()).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals("Bruce", patchedUser.getName().flatMap(Name::getGivenName).get());
    Assertions.assertEquals("Lee", patchedUser.getName().flatMap(Name::getFamilyName).get());
  }

  /**
   * verifies that a sub-attribute of a readOnly-complex-attribute cannot be patched
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE", "REMOVE"})
  public void testChangeReadOnlyOnComplexSubType(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    SchemaAttribute nameAttribute = userResourceType.getSchemaAttribute("name").get();
    nameAttribute.setMutability(Mutability.READ_ONLY);

    final String path = "name.givenName";
    List<String> values = patchOp.equals(PatchOp.REMOVE) ? null : Arrays.asList("Happy");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder().name(Name.builder().givenName("Gilmore").build()).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals("Gilmore", patchedUser.getName().flatMap(Name::getGivenName).orElse(null));
  }

  /**
   * verifies that a complex subtype cannot be patched if the subtype itself is readOnly
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE", "REMOVE"})
  public void testChangeReadOnlySubTypeOnComplexType(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    TestHelper.modifyAttributeMetaData(userResourceType.getMainSchema(),
                                       "name.givenName",
                                       null,
                                       Mutability.READ_ONLY,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);

    final String path = "name.givenName";
    List<String> values = patchOp.equals(PatchOp.REMOVE) ? null : Arrays.asList("Happy");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder().name(Name.builder().givenName("Gilmore").build()).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals("Gilmore", patchedUser.getName().flatMap(Name::getGivenName).orElse(null));
  }

  /**
   * verifies that an immutable complex type cannot be patched if already assigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testChangeImmutableAssignedOnComplexType(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    TestHelper.modifyAttributeMetaData(userResourceType.getMainSchema(),
                                       "name",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);

    final String path = "name";
    Name name = Name.builder().givenName("chuck").build();
    List<String> values = patchOp.equals(PatchOp.REMOVE) ? null : Arrays.asList(name.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder().name(Name.builder().familyName("norris").build()).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    try
    {
      addUserToProvider(userHandler, user);
      PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
      patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.fail("this point must not be reached\n" + user.toPrettyString());
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      MatcherAssert.assertThat(ex.getDetail(),
                               Matchers.startsWith("The attribute 'name' is 'IMMUTABLE' and is not unassigned. "
                                                   + "Current value is: "));
    }
  }

  /**
   * verifies that a subtype of an immutable complex type cannot be patched if the complex type does already
   * exist
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testChangeAssignedSubTypeOnImmutableComplexType(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    TestHelper.modifyAttributeMetaData(userResourceType.getMainSchema(),
                                       "name",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);


    final String path = "name.givenName";
    List<String> values = Arrays.asList("chuck");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder().name(Name.builder().givenName("norris").build()).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    try
    {
      addUserToProvider(userHandler, user);
      PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
      patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.fail("this point must not be reached\n" + user.toPrettyString());
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      MatcherAssert.assertThat(ex.getDetail(),
                               Matchers.startsWith("The attribute 'name' is 'IMMUTABLE' and is not unassigned. "
                                                   + "Current value is: "));
    }
  }

  /**
   * verifies that an immutable subtype of a readWrite complex type cannot be patched if already assigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testChangeImmutableAssignedSubTypeOnComplexType(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    TestHelper.modifyAttributeMetaData(userResourceType.getMainSchema(),
                                       "name.givenName",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);


    final String path = "name.givenName";
    List<String> values = Arrays.asList("chuck");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder().name(Name.builder().givenName("norris").build()).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    try
    {
      addUserToProvider(userHandler, user);
      PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
      patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.fail("this point must not be reached\n" + user.toPrettyString());
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      MatcherAssert.assertThat(ex.getDetail(),
                               Matchers.startsWith("The attribute 'name.givenName' is 'IMMUTABLE' and is not "
                                                   + "unassigned. Current value is: norris"));
    }
  }

  /**
   * verifies that an immutable subtype of a readWrite complex type can be patched if not assigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testChangeImmutableUnassignedSubTypeOnComplexType(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    TestHelper.modifyAttributeMetaData(userResourceType.getMainSchema(),
                                       "name",
                                       null,
                                       Mutability.READ_WRITE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);

    final String path = "name.givenName";
    List<String> values = Arrays.asList("chuck");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder().name(Name.builder().familyName("norris").build()).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertTrue(patchedUser.getName().isPresent());
    Assertions.assertTrue(patchedUser.getName().get().getGivenName().isPresent());
    Assertions.assertEquals("chuck", patchedUser.getName().get().getGivenName().get());
  }

  /**
   * verifies that an immutable complex type can be patched if unassigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testChangeImmutableUnassignedOnComplexType(PatchOp patchOp)
  {
    JsonNode readOnlyNameDef = JsonHelper.loadJsonDocument(IMMUTABLE_NAME_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(readOnlyNameDef);


    final String path = "name";
    Name name = Name.builder().givenName("chuck").build();
    List<String> values = patchOp.equals(PatchOp.REMOVE) ? null : Arrays.asList(name.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    AllTypes allTypes = new AllTypes(true);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertNotNull(patchedAllTypes.get(RFC7643.NAME));
    Assertions.assertNotNull(patchedAllTypes.get(RFC7643.NAME).get(RFC7643.GIVEN_NAME));
  }

  /**
   * verifies that an immutable complex type can be set to unassigned with a remove operation if set
   */
  @Test
  public void testChangeImmutableUnassignedOnComplexType()
  {
    JsonNode readOnlyNameDef = JsonHelper.loadJsonDocument(IMMUTABLE_NAME_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(readOnlyNameDef);

    final String path = "name";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(PatchOp.REMOVE)
                                                                                .build());

    Name name = Name.builder().givenName("chuck").build();
    User user = User.builder().id(UUID.randomUUID().toString()).name(name).build();
    AllTypes allTypes = JsonHelper.copyResourceToObject(user, AllTypes.class);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertNull(patchedAllTypes.get(RFC7643.NAME));
  }

  /**
   * verifies that a readOnly multivalued complex type cannot be patched
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE", "REMOVE"})
  public void testChangeReadOnlyOnMultiValuedComplexType(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    SchemaAttribute emailsAttribute = userResourceType.getSchemaAttribute("emails").get();
    emailsAttribute.setMutability(Mutability.READ_ONLY);

    final String path = "emails";
    final String emailsValue = "max@mustermann.de";
    final String emailsType = "home";
    Email email = Email.builder().value(emailsValue).type(emailsType).build();
    List<String> values = patchOp.equals(PatchOp.REMOVE) ? null : Arrays.asList(email.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    final String originalEmailsValue = "erika@mustermann.de";
    final String originalEmailsType = "work";
    User user = User.builder()
                    .id(UUID.randomUUID().toString())
                    .emails(Arrays.asList(Email.builder().value(originalEmailsValue).type(originalEmailsType).build()))
                    .build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(1, patchedUser.getEmails().size());
    Assertions.assertEquals(originalEmailsValue, patchedUser.getEmails().get(0).getValue().get());
    Assertions.assertEquals(originalEmailsType, patchedUser.getEmails().get(0).getType().get());
  }

  /**
   * verifies that an immutable multivalued complex type cannot be patched if already assigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testChangeAssignedImmutableOnMultiValuedComplexType(PatchOp patchOp)
  {
    JsonNode immutableEmailsDef = JsonHelper.loadJsonDocument(IMMUTABLE_EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(immutableEmailsDef);


    final String path = "emails";
    Email email = Email.builder().value(UUID.randomUUID().toString()).type("home").build();
    List<String> values = patchOp.equals(PatchOp.REMOVE) ? null : Arrays.asList(email.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder()
                    .emails(Arrays.asList(Email.builder().value(UUID.randomUUID().toString()).build()))
                    .build();
    AllTypes allTypes = JsonHelper.copyResourceToObject(user, AllTypes.class);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    try
    {
      addAllTypesToProvider(allTypes);
      PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                allTypesResourceHandler,
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
      patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.fail("this point must not be reached\n" + user.toPrettyString());
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      MatcherAssert.assertThat(ex.getDetail(),
                               Matchers.startsWith("The attribute 'emails' is 'IMMUTABLE' and is not unassigned. "
                                                   + "Current value is: "));
    }
  }

  /**
   * verifies that an immutable multivalued complex type can be patched if unassigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testChangeUnassignedImmutableOnMultiValuedComplexType(PatchOp patchOp)
  {
    JsonNode immutableEmailsDef = JsonHelper.loadJsonDocument(IMMUTABLE_EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(immutableEmailsDef);


    final String path = "emails";
    Email email = Email.builder().value(UUID.randomUUID().toString()).type("home").build();
    List<String> values = patchOp.equals(PatchOp.REMOVE) ? null : Arrays.asList(email.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    AllTypes allTypes = new AllTypes(true);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    ArrayNode emails = (ArrayNode)patchedAllTypes.get(RFC7643.EMAILS);
    Assertions.assertNotNull(emails);
    Assertions.assertEquals(1, emails.size());
    Assertions.assertEquals(email, emails.get(0));
  }

  /**
   * verifies that an immutable multivalued complex type can be removed if assigned
   */
  @Test
  public void testRemoveAssignedImmutableOnMultiValuedComplexType()
  {
    JsonNode immutableEmailsDef = JsonHelper.loadJsonDocument(IMMUTABLE_EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(immutableEmailsDef);

    final String path = "emails";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(PatchOp.REMOVE)
                                                                                .build());

    Email email = Email.builder().value(UUID.randomUUID().toString()).type("home").build();
    User user = User.builder().emails(Collections.singletonList(email)).build();
    AllTypes allTypes = JsonHelper.copyResourceToObject(user, AllTypes.class);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    User patchedUser = JsonHelper.copyResourceToObject(patchedAllTypes, User.class);

    Assertions.assertEquals(0, patchedUser.getEmails().size());
  }

  /**
   * verifies that an immutable multivalued complex subtype can be removed if assigned
   */
  @Test
  public void testRemoveAssignedImmutableOnMultiValuedComplexSubType()
  {
    JsonNode immutableEmailsDef = JsonHelper.loadJsonDocument(IMMUTABLE_EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(immutableEmailsDef);

    final String path = "emails.type";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(PatchOp.REMOVE)
                                                                                .build());

    Email email = Email.builder().value(UUID.randomUUID().toString()).type("home").build();
    User user = User.builder().emails(Collections.singletonList(email)).build();
    AllTypes allTypes = JsonHelper.copyResourceToObject(user, AllTypes.class);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    ArrayNode emails = (ArrayNode)patchedAllTypes.get(RFC7643.EMAILS);
    Assertions.assertNotNull(emails);
    Assertions.assertEquals(1, emails.size());
    Assertions.assertNull(emails.get(0).get(RFC7643.TYPE));
    Assertions.assertNotNull(emails.get(0).get(RFC7643.VALUE));
  }

  /**
   * verifies that an immutable multivalued complex subtype can be patched if unassigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testChangeUnassignedImmutableOnMultiValuedComplexSubType(PatchOp patchOp)
  {
    JsonNode immutableEmailsDef = JsonHelper.loadJsonDocument(IMMUTABLE_EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(immutableEmailsDef);


    final String path = "emails.type";
    List<String> values = Arrays.asList("home");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder()
                    .emails(Collections.singletonList(Email.builder().value(UUID.randomUUID().toString()).build()))
                    .build();
    AllTypes allTypes = JsonHelper.copyResourceToObject(user, AllTypes.class);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    User patchedUser = JsonHelper.copyResourceToObject(patchedAllTypes, User.class);

    Assertions.assertEquals(1, patchedUser.getEmails().size());
    Assertions.assertTrue(patchedUser.getEmails().get(0).getValue().isPresent());
    Assertions.assertTrue(patchedUser.getEmails().get(0).getType().isPresent());
    Assertions.assertEquals("home", patchedUser.getEmails().get(0).getType().get());
  }

  /**
   * verifies that an immutable multivalued complex subtype cannot be patched if already assigned
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testChangeAssignedImmutableOnMultiValuedComplexSubType(PatchOp patchOp)
  {
    JsonNode immutableEmailsDef = JsonHelper.loadJsonDocument(IMMUTABLE_EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(immutableEmailsDef);


    final String path = "emails.type";
    List<String> values = Arrays.asList("home");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    User user = User.builder()
                    .emails(Collections.singletonList(Email.builder()
                                                           .value(UUID.randomUUID().toString())
                                                           .type("work")
                                                           .build()))
                    .build();
    AllTypes allTypes = JsonHelper.copyResourceToObject(user, AllTypes.class);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    try
    {
      addAllTypesToProvider(allTypes);
      PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                allTypesResourceHandler,
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
      patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      MatcherAssert.assertThat(ex.getDetail(),
                               Matchers.startsWith("The attribute 'emails.type' is 'IMMUTABLE' and is not unassigned. "
                                                   + "Current value is: "));
    }
  }

  /**
   * verifies that a readOnly-sub-attribute on a readWrite-multivalued-complex-attribute cannot be patched.
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE", "REMOVE"})
  public void testChangeReadOnlyOnMultiValuedComplexSubType(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    SchemaAttribute emailsType = userResourceType.getSchemaAttribute("emails.type").get();
    emailsType.setMutability(Mutability.READ_ONLY);

    final String path = "emails.type";
    List<String> values = PatchOp.REMOVE.equals(patchOp) ? null : Arrays.asList("home");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(path)
                                                                                .op(patchOp)
                                                                                .values(values)
                                                                                .build());

    final String emailValue = "max@mustermann.de";
    final String emailType = "work";
    User user = User.builder()
                    .emails(Collections.singletonList(Email.builder().value(emailValue).type(emailType).build()))
                    .build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(user.getId().get(), userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(1, patchedUser.getEmails().size());
    Assertions.assertEquals(emailValue, patchedUser.getEmails().get(0).getValue().get());
    Assertions.assertEquals(emailType, patchedUser.getEmails().get(0).getType().get());
  }

  /**
   * Verifies that the broken patch-remove requests from Azure are accepted. Such a request does look like this:
   *
   * <pre>
   * PATCH /scim/Groups/2752513
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "op": "Remove",
   *             "path": "members",
   *             "value": [
   *                 {
   *                     "value": "2392066"
   *                 }
   *             ]
   *         }
   *     ]
   * }
   * </pre>
   * <p>
   * the value in the request must not be present. Instead, the request should look like this:
   *
   * <pre>
   * PATCH /scim/Groups/2752513
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "op": "Remove",
   *             "path": "members[value eq \"2392066\"]"
   *         }
   *     ]
   * }
   * </pre>
   */
  @Test
  public void testMsAzureWorkaround()
  {
    GroupHandlerImpl groupHandler = new GroupHandlerImpl();
    ResourceType groupResourceType = resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(groupHandler));

    final String path = RFC7643.MEMBERS;
    final String value = "123456";
    final ObjectNode valueNode = new ObjectNode(JsonNodeFactory.instance);
    valueNode.set("value", new TextNode(value));
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .valueNode(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    Group group = Group.builder()
                       .displayName("admin")
                       .members(Arrays.asList(Member.builder().value(UUID.randomUUID().toString()).build(),
                                              Member.builder().value(value).build(),
                                              Member.builder().value(UUID.randomUUID().toString()).build()))
                       .build();

    addGroupToProvider(groupHandler, group);
    PatchRequestHandler<Group> patchRequestHandler = new PatchRequestHandler(group.getId().get(),
                                                                             groupResourceType.getResourceHandlerImpl(),
                                                                             resourceEndpoint.getPatchWorkarounds(),
                                                                             new Context(null));
    Group patchedResource = patchRequestHandler.handlePatchRequest(patchOpRequest);


    Assertions.assertEquals(5, patchedResource.size(), group.toPrettyString());
    Assertions.assertTrue(patchedResource.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedResource.has(RFC7643.ID));
    Assertions.assertTrue(patchedResource.has(RFC7643.META));
    Assertions.assertTrue(patchedResource.has(RFC7643.MEMBERS));
    Assertions.assertEquals(2, patchedResource.getMembers().size(), group.toPrettyString());
    Assertions.assertFalse(patchedResource.getMembers()
                                          .stream()
                                          .anyMatch(member -> member.getValue().get().equals(value)));
  }

  /**
   * @see MsAzurePatchComplexValueRebuilder for more details
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/541
   */
  @DisplayName("MsAzure workaround is executed for patch with path expressions")
  @ParameterizedTest
  @CsvSource({"manager,ADD", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager,ADD",
              "manager,REPLACE", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager,REPLACE"})
  public void testMsAzureComplexPatchPathValueWorkaround(String attributePath, PatchOp patchOp)
  {
    serviceProvider.getPatchConfig().setMsAzureComplexSimpleValueWorkaroundActive(true);
    SchemaAttribute manager = allTypesResourceType.getAllSchemaExtensions().get(0).getSchemaAttribute(RFC7643.MANAGER);
    Assertions.assertNotNull(manager);
    Assertions.assertEquals(Type.COMPLEX, manager.getType());

    final String value = "271";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(attributePath)
                                                                                .value(value)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes(true);
    AllTypes patchedResource = patchAllTypes(allTypes, patchOpRequest, true);

    EnterpriseUser enterpriseUser = patchedResource.getEnterpriseUser().get();
    Assertions.assertEquals(value, enterpriseUser.getManager().flatMap(Manager::getValue).orElse(null));
  }

  /**
   * verifies that the {@link PatchConfig#isIgnoreUnknownAttribute()} configuration is correctly handled on
   * patch with path expressions
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/539
   */
  @DisplayName("Activate ignoreUnknownAttribute config with patch-path and verify it works")
  @Test
  public void testNoExceptionOnInvalidAttributePatchPath()
  {
    serviceProvider.getPatchConfig().setIgnoreUnknownAttribute(true);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("unknown")
                                                                                .value("my-value")
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes(true);
    AllTypes patchedResource = patchAllTypes(allTypes, patchOpRequest, false);
    Assertions.assertEquals(allTypes, patchedResource);
  }

  /**
   * verifies that the {@link PatchConfig#isIgnoreUnknownAttribute()} configuration is correctly handled on
   * patch without path expressions
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/539
   */
  @DisplayName("Activate ignoreUnknownAttribute config with patch-resource and verify it works")
  @Test
  public void testNoExceptionOnInvalidAttributeOnPatchResource()
  {
    serviceProvider.getPatchConfig().setIgnoreUnknownAttribute(true);
    AllTypes patchRequestObject = new AllTypes(true);
    patchRequestObject.set("unknownAttribute", new TextNode("my-value"));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(patchRequestObject)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes(true);
    AllTypes patchedResource = patchAllTypes(allTypes, patchOpRequest, false);
    Assertions.assertEquals(allTypes, patchedResource);
  }

  /**
   * Makes sure that the workaround is not executed if not explicitly activated in the {@link PatchConfig}
   *
   * @see MsAzurePatchComplexValueRebuilder for more details
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/541
   */
  @DisplayName("MsAzure workaround is not executed for patch with path expressions if deactivated")
  @ParameterizedTest
  @CsvSource({"manager,ADD", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager,ADD",
              "manager,REPLACE", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager,REPLACE"})
  public void testMsAzureComplexPatchPathSimpleValueWorkaroundIsIgnoredIfDeactivated(String attributePath,
                                                                                     PatchOp patchOp)
  {
    serviceProvider.getPatchConfig().setMsAzureComplexSimpleValueWorkaroundActive(false);
    SchemaAttribute manager = allTypesResourceType.getAllSchemaExtensions().get(0).getSchemaAttribute(RFC7643.MANAGER);
    Assertions.assertNotNull(manager);
    Assertions.assertEquals(Type.COMPLEX, manager.getType());

    final String value = "271";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(attributePath)
                                                                                .value(value)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes();
    BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                     () -> patchAllTypes(allTypes, patchOpRequest, false));
    String expectedErrorMessage = "Value for path 'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager' "
                                  + "must be a complex-node representation but was: 271";
    Assertions.assertEquals(expectedErrorMessage, ex.getMessage());
  }

  /**
   * verifies that several attributes are correctly removed if attributes are removed in the order they are
   * present in the json structure. This test shall reproduce the following issue:
   * https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/111
   */
  @Test
  public void testRemoveSeveralAttributesInOrder()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setDecimal(1.1);
    multicomplex.setNumber(5L);
    AllTypes multicomplex2 = new AllTypes(false);
    multicomplex2.setDecimal(10.2);
    multicomplex2.setNumber(6L);
    AllTypes multicomplex3 = new AllTypes(false);
    multicomplex3.setDecimal(5.5);
    multicomplex3.setNumber(0L);
    allTypes.setMultiComplex(Arrays.asList(multicomplex, multicomplex2, multicomplex3));

    final String path = "multicomplex[decimal eq 10.2 or number eq 5]";
    PatchRequestOperation patchRequestOperation = PatchRequestOperation.builder().op(PatchOp.REMOVE).path(path).build();
    List<PatchRequestOperation> operations = Arrays.asList(patchRequestOperation);


    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(multicomplex3,
                            patchedAllTypes.getMultiComplex().get(0),
                            "multicomplex and multicomplex2 should have been removed");
  }

  /**
   * verifies that several attributes are correctly removed if attributes are removed in the order they are
   * present in the json structure. This test shall reproduce the following issue:
   * https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/111
   */
  @Test
  public void testRemoveSeveralAttributesWithStepOverOrder()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setDecimal(1.1);
    multicomplex.setNumber(5L);
    AllTypes multicomplex2 = new AllTypes(false);
    multicomplex2.setDecimal(10.2);
    multicomplex2.setNumber(6L);
    AllTypes multicomplex3 = new AllTypes(false);
    multicomplex3.setDecimal(5.5);
    multicomplex3.setNumber(0L);
    allTypes.setMultiComplex(Arrays.asList(multicomplex, multicomplex2, multicomplex3));

    final String path = "multicomplex[decimal eq 5.5 or number eq 5]";
    PatchRequestOperation patchRequestOperation = PatchRequestOperation.builder().op(PatchOp.REMOVE).path(path).build();
    List<PatchRequestOperation> operations = Arrays.asList(patchRequestOperation);

    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(multicomplex2,
                            patchedAllTypes.getMultiComplex().get(0),
                            "multicomplex and multicomplex 3 should have been removed");
  }

  /**
   * this test will verify that two add operations will be applied to the resource if the filter expression do
   * match in both cases
   */
  @Test
  public void testTwoAddOperationsWithMatchingFilter()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes firstChangeMulticomplex = new AllTypes(false);
    firstChangeMulticomplex.setString("hello goldfish");
    AllTypes secondChangeMulticomplex = new AllTypes(false);
    secondChangeMulticomplex.setString("hello chuck");
    final String path = "multicomplex[string eq \"hello world\"]";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder()
                                                                .op(PatchOp.ADD)
                                                                .path(path)
                                                                .valueNode(firstChangeMulticomplex)
                                                                .build();
    PatchRequestOperation secondOperation = PatchRequestOperation.builder()
                                                                 .op(PatchOp.ADD)
                                                                 .path(path)
                                                                 .valueNode(secondChangeMulticomplex)
                                                                 .build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation, secondOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(multicomplex, patchedAllTypes.getMultiComplex().get(0));
    Assertions.assertEquals(firstChangeMulticomplex, patchedAllTypes.getMultiComplex().get(1));
    Assertions.assertEquals(secondChangeMulticomplex, patchedAllTypes.getMultiComplex().get(2));
  }

  /**
   * this test will verify that two replace operations will be applied correctly to the resource if the filter
   * expression do match in both cases
   */
  @Test
  public void testTwoReplaceOperationsWithMatchingFilter()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes firstChangeMulticomplex = new AllTypes(false);
    firstChangeMulticomplex.setString("hello goldfish");
    AllTypes secondChangeMulticomplex = new AllTypes(false);
    secondChangeMulticomplex.setString("hello chuck");
    final String path1 = "multicomplex[string eq \"hello world\"]";
    final String path2 = "multicomplex[string eq \"hello goldfish\"]";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder()
                                                                .op(PatchOp.REPLACE)
                                                                .path(path1)
                                                                .valueNode(firstChangeMulticomplex)
                                                                .build();
    PatchRequestOperation secondOperation = PatchRequestOperation.builder()
                                                                 .op(PatchOp.REPLACE)
                                                                 .path(path2)
                                                                 .valueNode(secondChangeMulticomplex)
                                                                 .build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation, secondOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(secondChangeMulticomplex, patchedAllTypes.getMultiComplex().get(0));
  }

  /**
   * this test will verify that an add and a replace operation do work after another if the filter expression do
   * match in both cases
   */
  @Test
  public void testFirstAddThenReplace()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes firstChangeMulticomplex = new AllTypes(false);
    firstChangeMulticomplex.setString("hello goldfish");
    AllTypes secondChangeMulticomplex = new AllTypes(false);
    secondChangeMulticomplex.setString("hello chuck");
    final String path = "multicomplex[string eq \"hello world\"]";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder()
                                                                .op(PatchOp.ADD)
                                                                .path(path)
                                                                .valueNode(firstChangeMulticomplex)
                                                                .build();
    PatchRequestOperation secondOperation = PatchRequestOperation.builder()
                                                                 .op(PatchOp.REPLACE)
                                                                 .path(path)
                                                                 .valueNode(secondChangeMulticomplex)
                                                                 .build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation, secondOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(firstChangeMulticomplex, patchedAllTypes.getMultiComplex().get(0));
    Assertions.assertEquals(secondChangeMulticomplex, patchedAllTypes.getMultiComplex().get(1));
  }

  /**
   * this test will verify that a request with two operations will fail if the second operation fails.<br>
   * this test reproduces the github issue: https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/201
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testSecondOperationHasPathMismatch(PatchOp patchOp)
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes firstChangeMulticomplex = new AllTypes(false);
    firstChangeMulticomplex.setString("hello goldfish");
    AllTypes secondChangeMulticomplex = new AllTypes(false);
    secondChangeMulticomplex.setString("hello chuck");
    final String path = "multicomplex[string eq \"hello world\"]";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder()
                                                                .op(PatchOp.REPLACE)
                                                                .path(path)
                                                                .valueNode(firstChangeMulticomplex)
                                                                .build();
    PatchRequestOperation secondOperation = PatchRequestOperation.builder()
                                                                 .op(patchOp)
                                                                 .path(path)
                                                                 .valueNode(secondChangeMulticomplex)
                                                                 .build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation, secondOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    try
    {
      AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);
      Assertions.fail(String.format("this point must not be reached: %s", patchedAllTypes.toPrettyString()));
    }
    catch (BadRequestException ex)
    {
      final String expectedMessage = String.format("Cannot '%s' value on path '%s' for no matching object was found",
                                                   patchOp,
                                                   path);
      Assertions.assertEquals(expectedMessage, ex.getMessage());
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
    }
  }

  /**
   * verify that replace on multivalued complex types with a path-filter works as expected<br>
   * this test reproduces https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/201
   */
  @Test
  public void testReplaceOnMultivaluedComplexTypesWithFiterOnPath()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex1 = new AllTypes(false);
    multicomplex1.setString("hello world");
    AllTypes multicomplex2 = new AllTypes(false);
    multicomplex2.setString("hello goldfish");
    AllTypes multicomplex3 = new AllTypes(false);
    multicomplex3.setString("hello pool");
    allTypes.setMultiComplex(Arrays.asList(multicomplex1, multicomplex2, multicomplex3));

    AllTypes firstChangeMulticomplex = new AllTypes(false);
    firstChangeMulticomplex.setString("replace it");
    final String path = "multicomplex[string eq \"hello world\"]";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder()
                                                                .op(PatchOp.REPLACE)
                                                                .path(path)
                                                                .valueNode(firstChangeMulticomplex)
                                                                .build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());

    List<AllTypes> multiComplexNodes = patchedAllTypes.getMultiComplex();
    Assertions.assertEquals(3, multiComplexNodes.size());
    Assertions.assertEquals("hello goldfish", multiComplexNodes.get(0).getString().get());
    Assertions.assertEquals("hello pool", multiComplexNodes.get(1).getString().get());
    Assertions.assertEquals("replace it", multiComplexNodes.get(2).getString().get());
  }

  /**
   * verifies that all attributes are removed from the multivalued complex attributes if the path is set and the
   * attribute is present on each node
   */
  @Test
  public void testRemoveAllAttributesOnMultiComplex()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex1 = new AllTypes(false);
    multicomplex1.setString("hello world");
    multicomplex1.setNumber(1L);
    AllTypes multicomplex2 = new AllTypes(false);
    multicomplex2.setString("hello goldfish");
    multicomplex2.setNumber(2L);
    AllTypes multicomplex3 = new AllTypes(false);
    multicomplex3.setString("hello pool");
    multicomplex3.setNumber(3L);
    allTypes.setMultiComplex(Arrays.asList(multicomplex1, multicomplex2, multicomplex3));

    final String path = "multicomplex.number";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder().op(PatchOp.REMOVE).path(path).build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());

    List<AllTypes> multiComplexNodes = patchedAllTypes.getMultiComplex();
    Assertions.assertEquals(3, multiComplexNodes.size());
    Assertions.assertEquals("hello world", multiComplexNodes.get(0).getString().get());
    Assertions.assertEquals("hello goldfish", multiComplexNodes.get(1).getString().get());
    Assertions.assertEquals("hello pool", multiComplexNodes.get(2).getString().get());
  }

  /**
   * verifies that all attributes are removed from the multivalued complex attributes if the path is set and the
   * attribute is only present on two from three of the multivalued nodes
   */
  @Test
  public void testRemoveAllAttributesOnMultiComplexWithoutHavingAttributeOnAllNodes()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex1 = new AllTypes(false);
    multicomplex1.setString("hello world");
    multicomplex1.setNumber(1L);
    AllTypes multicomplex2 = new AllTypes(false);
    multicomplex2.setString("hello goldfish");
    multicomplex2.setNumber(2L);
    AllTypes multicomplex3 = new AllTypes(false);
    multicomplex3.setString("hello pool");
    allTypes.setMultiComplex(Arrays.asList(multicomplex1, multicomplex2, multicomplex3));

    final String path = "multicomplex.number";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder().op(PatchOp.REMOVE).path(path).build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());

    List<AllTypes> multiComplexNodes = patchedAllTypes.getMultiComplex();
    Assertions.assertEquals(3, multiComplexNodes.size());
    Assertions.assertEquals("hello world", multiComplexNodes.get(0).getString().get());
    Assertions.assertEquals("hello goldfish", multiComplexNodes.get(1).getString().get());
    Assertions.assertEquals("hello pool", multiComplexNodes.get(2).getString().get());
  }

  /**
   * verifies that all array attributes are removed from the multivalued complex attributes if the path is set
   * and the attribute is only present on two from three of the multivalued nodes
   */
  @Test
  public void testRemoveAllAttributesOnMultiComplexWithoutHavingArrayAttributeOnAllNodes()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex1 = new AllTypes(false);
    multicomplex1.setString("hello world");
    multicomplex1.setNumberArray(Arrays.asList(1L, 2L));
    AllTypes multicomplex2 = new AllTypes(false);
    multicomplex2.setString("hello goldfish");
    AllTypes multicomplex3 = new AllTypes(false);
    multicomplex3.setString("hello pool");
    multicomplex3.setNumberArray(Arrays.asList(3L, 4L));
    allTypes.setMultiComplex(Arrays.asList(multicomplex1, multicomplex2, multicomplex3));

    final String path = "multicomplex.numberArray";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder().op(PatchOp.REMOVE).path(path).build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());

    List<AllTypes> multiComplexNodes = patchedAllTypes.getMultiComplex();
    Assertions.assertEquals(3, multiComplexNodes.size());
    Assertions.assertEquals("hello world", multiComplexNodes.get(0).getString().get());
    Assertions.assertEquals("hello goldfish", multiComplexNodes.get(1).getString().get());
    Assertions.assertEquals("hello pool", multiComplexNodes.get(2).getString().get());
  }

  /**
   * verifies that a multivalued subattribute can be removed from a multivalued complex attribute if the path
   * has a filter
   */
  @Test
  public void testRemoveMultivaluedSubAttributeWithFilterFromMultivaluedComplex()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex1 = new AllTypes(false);
    multicomplex1.setString("hello world");
    multicomplex1.setNumberArray(Arrays.asList(1L, 2L));
    AllTypes multicomplex2 = new AllTypes(false);
    multicomplex2.setString("hello goldfish");
    AllTypes multicomplex3 = new AllTypes(false);
    multicomplex3.setString("hello pool");
    multicomplex3.setNumberArray(Arrays.asList(1L, 2L));
    allTypes.setMultiComplex(Arrays.asList(multicomplex1, multicomplex2, multicomplex3));

    final String path = "multicomplex[numberArray co 1].numberArray";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder().op(PatchOp.REMOVE).path(path).build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);

    Assertions.assertEquals(4, patchedAllTypes.size());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());

    List<AllTypes> multiComplexNodes = patchedAllTypes.getMultiComplex();
    Assertions.assertEquals(3, multiComplexNodes.size());
    Assertions.assertEquals("hello world", multiComplexNodes.get(0).getString().get());
    Assertions.assertEquals("hello goldfish", multiComplexNodes.get(1).getString().get());
    Assertions.assertEquals("hello pool", multiComplexNodes.get(2).getString().get());
  }

  /**
   * verifies that a remove operation on a multivalued subattribute of a multivalued complex attribute causes a
   * bad request exception if the given filter expression does not match any targets
   */
  @Test
  public void testRemoveMultivaluedSubAttributeWithFilterFromMultivaluedComplex2()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex1 = new AllTypes(false);
    multicomplex1.setString("hello world");
    multicomplex1.setNumberArray(Arrays.asList(1L, 2L));
    AllTypes multicomplex2 = new AllTypes(false);
    multicomplex2.setString("hello goldfish");
    AllTypes multicomplex3 = new AllTypes(false);
    multicomplex3.setString("hello pool");
    multicomplex3.setNumberArray(Arrays.asList(1L, 2L));
    allTypes.setMultiComplex(Arrays.asList(multicomplex1, multicomplex2, multicomplex3));

    final String path = "multicomplex[numberArray co 3].numberArray";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder().op(PatchOp.REMOVE).path(path).build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    try
    {
      AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, false);
      Assertions.fail(String.format("this point must not be reached: %s", patchedAllTypes.toPrettyString()));
    }
    catch (BadRequestException ex)
    {
      Assertions.assertEquals(BadRequestException.class, ex.getClass());
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
      Assertions.assertEquals("No target found for path-filter 'multicomplex[numberArray co 3].numberArray'",
                              ex.getMessage());
    }
  }

  /**
   * verifies that on a replace operation with filter all matching multivalued nodes are removed and exchanged
   * for the given nodes
   */
  @Test
  public void testReplaceMultivaluedComplexWithFilterAndSeveralMatches()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes multicomplex1 = new AllTypes(false);
    multicomplex1.setString("hello world");
    multicomplex1.setNumberArray(Arrays.asList(1L, 2L));
    AllTypes multicomplex2 = new AllTypes(false);
    multicomplex2.setString("hello goldfish");
    AllTypes multicomplex3 = new AllTypes(false);
    multicomplex3.setString("hello pool");
    multicomplex3.setNumberArray(Arrays.asList(1L, 2L));
    allTypes.setMultiComplex(Arrays.asList(multicomplex1, multicomplex2, multicomplex3));

    AllTypes replacement = new AllTypes(false);
    replacement.setNumber(999L);
    replacement.setDate(Instant.now());
    final String path = "multicomplex[numberArray co 1 or numberArray co 2]";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder()
                                                                .op(PatchOp.REPLACE)
                                                                .path(path)
                                                                .valueNode(replacement)
                                                                .build();
    // the same operation twice in a row should fail because the value should be changed after the first execution
    // so the second must fail
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);
    Assertions.assertEquals(4, patchedAllTypes.size());
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getSchemas().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());

    List<AllTypes> multiComplexNodes = patchedAllTypes.getMultiComplex();
    Assertions.assertEquals(2, multiComplexNodes.size());
    Assertions.assertEquals("hello goldfish", multiComplexNodes.get(0).getString().get());
    Assertions.assertFalse(multiComplexNodes.get(1).getString().isPresent());
    Assertions.assertTrue(multiComplexNodes.get(1).getNumberArray().isEmpty());
    Assertions.assertTrue(multiComplexNodes.get(1).getDate().isPresent());
    Assertions.assertTrue(multiComplexNodes.get(1).getNumber().isPresent());
    Assertions.assertEquals(999L, multiComplexNodes.get(1).getNumber().get());
  }

  /**
   * verifies that an extension can be removed if the extension id is directly referenced within the path
   * attribute
   *
   * <pre>
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "path": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
   *             "op": "remove"
   *         }
   *     ]
   * }
   * </pre>
   */
  @Test
  public void testRemoveExtension()
  {
    String employeeNumber = "1111";
    String costCenter = "2222";

    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().employeeNumber(employeeNumber).costCenter(costCenter).build());

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(SchemaUris.ENTERPRISE_USER_URI)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent());
  }

  /**
   * verifies that an extension can be removed if a replace operation with an empty extension object is used
   *
   * <pre>
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "path": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
   *             "op": "replace",
   *             "value": ["{\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\": {}}"]
   *         }
   *     ]
   * }
   * </pre>
   */
  @Test
  public void testReplaceExtensionWithEmptyObject()
  {
    String employeeNumber = "1111";
    String costCenter = "2222";

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setEnterpriseUser(EnterpriseUser.builder()
                                                   .employeeNumber(employeeNumber)
                                                   .costCenter(costCenter)
                                                   .build());

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(SchemaUris.ENTERPRISE_USER_URI)
                                                                                .valueNode(EnterpriseUser.builder()
                                                                                                         .build())
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes patchedAllTypes = patchAllTypes(allTypeChanges, patchOpRequest, true);
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent());
  }

  /**
   * verifies that an extension can be completely replaced if set within the patch values when directly accessed
   *
   * <pre>
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "path": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
   *             "op": "replace",
   *             "value": ["{\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\": {
   *                 \"costCenter\": \"2222\",
   *                 \"employeeNumber\": \"1111\" }}"]
   *         }
   *     ]
   * }
   * </pre>
   */
  @Test
  public void testReplaceExtensionWithNonEmptyObject()
  {
    String employeeNumber = "1111";
    String costCenter = "2222";

    AllTypes allTypeChanges = new AllTypes(true);

    EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                  .employeeNumber(employeeNumber)
                                                  .costCenter(costCenter)
                                                  .build();

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(SchemaUris.ENTERPRISE_USER_URI)
                                                                                .valueNode(enterpriseUser)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes patchedAllTypes = patchAllTypes(allTypeChanges, patchOpRequest, true);
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals(enterpriseUser, patchedAllTypes.getEnterpriseUser().get());
  }

  /**
   * verifies that an exception is thrown if too many values are set
   *
   * <pre>
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "path": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
   *             "op": "replace",
   *             "value": ["{
   *                          \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\":
   *                          {
   *                            \"costCenter\": \"2222\",
   *                            \"employeeNumber\": \"1111\"
   *                          }
   *                        }",
   *                        {
   *                          \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\":
   *                          {
   *                            \"costCenter\": \"3333\",
   *                            \"employeeNumber\": \"4444\"
   *                          }
   *                        }"]
   *         }
   *     ]
   * }
   * </pre>
   */
  @Test
  public void testReplaceExtensionWithTooManyValues()
  {
    EnterpriseUser enterpriseUser1 = EnterpriseUser.builder().employeeNumber("1111").costCenter("2222").build();
    EnterpriseUser enterpriseUser2 = EnterpriseUser.builder().employeeNumber("3333").costCenter("4444").build();
    List<String> values = Arrays.asList(enterpriseUser1.toString(), enterpriseUser2.toString());

    AllTypes allTypeChanges = new AllTypes(true);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(SchemaUris.ENTERPRISE_USER_URI)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                     () -> patchAllTypes(allTypeChanges, patchOpRequest, false));
    String errorMessage = String.format("Patch request contains too many values. Expected a single value "
                                        + "representing an extension but got several. '%s'",
                                        values);
    Assertions.assertEquals(errorMessage, ex.getMessage());
  }

  /**
   * verifies that an exception is thrown if the value contains an empty string
   *
   * <pre>
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "path": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
   *             "op": "replace",
   *             "value": [""]
   *         }
   *     ]
   * }
   * </pre>
   */
  @Test
  public void testReplaceExtensionWithEmptyValue()
  {

    AllTypes allTypeChanges = new AllTypes(true);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(SchemaUris.ENTERPRISE_USER_URI)
                                                                                .values(Collections.singletonList(""))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                     () -> patchAllTypes(allTypeChanges, patchOpRequest, false));
    String errorMessage = "Received invalid data on patch values. Expected an extension resource but got: ''";
    Assertions.assertEquals(errorMessage, ex.getMessage());
  }

  /**
   * this test will verify that filter-expressions on binary types in patch-path-representations will be
   * rejected
   */
  @Test
  public void testRejectFilterExpressionOnBinaryTypeInPatchPath()
  {
    AllTypes allTypes = new AllTypes(true);
    allTypes.setBinary("hello goldfish".getBytes());

    AllTypes firstChangeMulticomplex = new AllTypes(false);
    firstChangeMulticomplex.setBinary("replace it".getBytes());
    final String path = "multicomplex[binary eq \"aGVsbG8gZ29sZGZpc2g=\"]";
    PatchRequestOperation firstOperation = PatchRequestOperation.builder()
                                                                .op(PatchOp.REPLACE)
                                                                .path(path)
                                                                .valueNode(firstChangeMulticomplex)
                                                                .build();
    List<PatchRequestOperation> operations = Arrays.asList(firstOperation);
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    InvalidFilterException ex = Assertions.assertThrows(InvalidFilterException.class,
                                                        () -> patchAllTypes(allTypes, patchOpRequest, false));
    Assertions.assertEquals("binary types like 'urn:gold:params:scim:schemas:custom:2.0:AllTypes:"
                            + "multiComplex.binary' are not suitable for filter expressions",
                            ex.getMessage());
  }

  @DisplayName("Do not fail if patch-operations have no valid target")
  @Nested
  public class DoNotFailOnNoTargetTests
  {

    @BeforeEach
    public void init()
    {
      serviceProvider.getPatchConfig().setDoNotFailOnNoTarget(true);
    }

    @DisplayName("Do not fail on no target for when remove simple attribute")
    @Test
    public void testIgnoreNoTargetOnRemoveSimpleAttribute()
    {
      final String attributeName = "string";
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REMOVE)
                                                                                  .path(attributeName)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      AllTypes allTypes = new AllTypes(true);
      AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchAllTypes(allTypes, patchOpRequest, false));
      Assertions.assertEquals(2, patchedResource.size());
      Assertions.assertTrue(patchedResource.has(RFC7643.SCHEMAS));
      Assertions.assertTrue(patchedResource.has(RFC7643.ID));
    }

    @DisplayName("Do not fail on no target when remove simple-array attribute")
    @Test
    public void testIgnoreNoTargetOnRemoveSimpleArray()
    {
      final String attributeName = "stringArray";
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REMOVE)
                                                                                  .path(attributeName)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      AllTypes allTypes = new AllTypes(true);
      AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchAllTypes(allTypes, patchOpRequest, false));
      Assertions.assertEquals(2, patchedResource.size());
      Assertions.assertTrue(patchedResource.has(RFC7643.SCHEMAS));
      Assertions.assertTrue(patchedResource.has(RFC7643.ID));
    }

    @DisplayName("Do not fail on no target when remove simple-array-child value")
    @Test
    public void testIgnoreNoTargetOnRemoveSimpleArrayChild()
    {
      final String attributeName = "stringArray";
      final String valuePath = attributeName + "[value eq \"no-match\"]";
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REMOVE)
                                                                                  .path(valuePath)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      AllTypes allTypes = new AllTypes(true);
      allTypes.setStringArray(Arrays.asList("hello", "world"));
      AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchAllTypes(allTypes, patchOpRequest, false));
      Assertions.assertEquals(3, patchedResource.size());
      Assertions.assertTrue(patchedResource.has(RFC7643.SCHEMAS));
      Assertions.assertTrue(patchedResource.has(RFC7643.ID));
      Assertions.assertTrue(patchedResource.has(attributeName));
      Assertions.assertEquals(2, patchedResource.getStringArray().size());
      Assertions.assertTrue(patchedResource.getStringArray().containsAll(Arrays.asList("hello", "world")));
    }

    @DisplayName("Do not fail on no target when remove complex-child attribute")
    @Test
    public void testIgnoreNoTargetOnRemoveComplexChild()
    {
      final String attributeName = "complex.stringArray";
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REMOVE)
                                                                                  .path(attributeName)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      AllTypes allTypes = new AllTypes(true);
      AllTypes complex = new AllTypes(false);
      allTypes.setComplex(complex);
      complex.setNumber(4L);
      AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchAllTypes(allTypes, patchOpRequest, false));
      Assertions.assertEquals(3, patchedResource.size());
      Assertions.assertTrue(patchedResource.has(RFC7643.SCHEMAS));
      Assertions.assertTrue(patchedResource.has(RFC7643.ID));
      Assertions.assertTrue(patchedResource.has("complex"));
      Assertions.assertEquals(1, patchedResource.getComplex().get().size());
      Assertions.assertTrue(patchedResource.getComplex().get().has("number"));
    }

    @DisplayName("Do not fail on no target when remove complex attribute")
    @Test
    public void testIgnoreNoTargetOnRemoveComplex()
    {
      final String attributeName = "complex";
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REMOVE)
                                                                                  .path(attributeName)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      AllTypes allTypes = new AllTypes(true);
      AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchAllTypes(allTypes, patchOpRequest, false));
      Assertions.assertEquals(2, patchedResource.size());
      Assertions.assertTrue(patchedResource.has(RFC7643.SCHEMAS));
      Assertions.assertTrue(patchedResource.has(RFC7643.ID));
    }

    @DisplayName("Do not fail on no target when remove multivalued-complex attribute")
    @Test
    public void testIgnoreNoTargetOnRemoveMultivaluedComplex()
    {
      final String attributeName = "multiComplex";
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REMOVE)
                                                                                  .path(attributeName)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      AllTypes allTypes = new AllTypes(true);
      AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchAllTypes(allTypes, patchOpRequest, false));
      Assertions.assertEquals(2, patchedResource.size());
      Assertions.assertTrue(patchedResource.has(RFC7643.SCHEMAS));
      Assertions.assertTrue(patchedResource.has(RFC7643.ID));
    }

    @DisplayName("Do not fail on no target when remove multivalued-complex-children attributes")
    @Test
    public void testIgnoreNoTargetOnRemoveMultivaluedComplexChildren()
    {
      final String attributeName = "multiComplex.string";
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REMOVE)
                                                                                  .path(attributeName)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      AllTypes allTypes = new AllTypes(true);
      AllTypes complex = new AllTypes(false);
      complex.setNumber(4L);
      allTypes.setMultiComplex(Arrays.asList(complex));
      AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchAllTypes(allTypes, patchOpRequest, false));
      Assertions.assertEquals(3, patchedResource.size());
      Assertions.assertTrue(patchedResource.has(RFC7643.SCHEMAS));
      Assertions.assertTrue(patchedResource.has(RFC7643.ID));
      Assertions.assertTrue(patchedResource.has("multiComplex"));
      Assertions.assertEquals(1, patchedResource.getMultiComplex().size());
      Assertions.assertEquals(1, patchedResource.getMultiComplex().get(0).size());
      Assertions.assertTrue(patchedResource.getMultiComplex().get(0).has("number"));
    }

    @DisplayName("Do not fail on no target when remove multivalued-complex-child attribute with filter")
    @Test
    public void testIgnoreNoTargetOnRemoveMultivaluedComplexChildWithFilter()
    {
      final String attributeName = "multiComplex";
      final String valuePath = attributeName + "[string eq \"no-match\"]";
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REMOVE)
                                                                                  .path(valuePath)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      AllTypes allTypes = new AllTypes(true);
      AllTypes complex = new AllTypes(false);
      complex.setNumber(4L);
      allTypes.setMultiComplex(Arrays.asList(complex));
      AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchAllTypes(allTypes, patchOpRequest, false));
      Assertions.assertEquals(3, patchedResource.size());
      Assertions.assertTrue(patchedResource.has(RFC7643.SCHEMAS));
      Assertions.assertTrue(patchedResource.has(RFC7643.ID));
      Assertions.assertTrue(patchedResource.has("multiComplex"));
      Assertions.assertEquals(1, patchedResource.getMultiComplex().size());
      Assertions.assertEquals(1, patchedResource.getMultiComplex().get(0).size());
      Assertions.assertTrue(patchedResource.getMultiComplex().get(0).has("number"));
    }
  }

  @DisplayName("Filter simple values from array with patch-remove")
  @Nested
  public class SimpleArrayFilterRemoveTests
  {

    /**
     * remove specific value from string-type attribute with custom-filter-expression
     */
    @DisplayName("Remove value from simple string-array with: stringArray[value eq \"world\"]")
    @Test
    public void testRemoveValueFromSimpleStringArrayWithFilterExpression()
    {
      AllTypes allTypes = new AllTypes(true);
      allTypes.setStringArray(Arrays.asList("hello", "world", "world", "next-day"));

      final String path = "stringArray[value eq \"world\"]";
      PatchRequestOperation patchRequestOperation = PatchRequestOperation.builder()
                                                                         .op(PatchOp.REMOVE)
                                                                         .path(path)
                                                                         .build();
      List<PatchRequestOperation> operations = Arrays.asList(patchRequestOperation);

      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

      AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);
      Assertions.assertEquals(2, patchedAllTypes.getStringArray().size());
      Assertions.assertEquals("hello", patchedAllTypes.getStringArray().get(0));
      Assertions.assertEquals("next-day", patchedAllTypes.getStringArray().get(1));
    }

    /**
     * will throw a no-target error because the filter does not match any values
     */
    @DisplayName("Remove value from simple string-array with: stringArray[value eq \"world\"]")
    @Test
    public void testRemoveValueFromSimpleStringArrayWithFilterExpressionButNoMatch()
    {
      AllTypes allTypes = new AllTypes(true);
      allTypes.setStringArray(Arrays.asList("hello", "next-day"));

      final String path = "stringArray[value eq \"world\"]";
      PatchRequestOperation patchRequestOperation = PatchRequestOperation.builder()
                                                                         .op(PatchOp.REMOVE)
                                                                         .path(path)
                                                                         .build();
      List<PatchRequestOperation> operations = Arrays.asList(patchRequestOperation);

      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

      BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                       () -> patchAllTypes(allTypes, patchOpRequest, false));
      Assertions.assertEquals(String.format("No target found for path-filter '%s'", path), ex.getMessage());
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
    }

    /**
     * remove specific value from string-type attribute with custom-filter-expression
     */
    @DisplayName("Remove value from simple integer-array with: numberArray[value eq 3]")
    @Test
    public void testRemoveValueFromSimpleIntArrayWithFilterExpression()
    {
      AllTypes allTypes = new AllTypes(true);
      allTypes.setNumberArray(Arrays.asList(1L, 1L, 2L, 2L, 3L, 3L, 4L, 4L, 5L, 5L));

      final String path = "numberArray[value eq 3]";
      PatchRequestOperation patchRequestOperation = PatchRequestOperation.builder()
                                                                         .op(PatchOp.REMOVE)
                                                                         .path(path)
                                                                         .build();
      List<PatchRequestOperation> operations = Arrays.asList(patchRequestOperation);

      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

      AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);
      Assertions.assertEquals(8, patchedAllTypes.getNumberArray().size());
      Assertions.assertEquals(1L, patchedAllTypes.getNumberArray().get(0));
      Assertions.assertEquals(1L, patchedAllTypes.getNumberArray().get(1));
      Assertions.assertEquals(2L, patchedAllTypes.getNumberArray().get(2));
      Assertions.assertEquals(2L, patchedAllTypes.getNumberArray().get(3));
      Assertions.assertEquals(4L, patchedAllTypes.getNumberArray().get(4));
      Assertions.assertEquals(4L, patchedAllTypes.getNumberArray().get(5));
      Assertions.assertEquals(5L, patchedAllTypes.getNumberArray().get(6));
      Assertions.assertEquals(5L, patchedAllTypes.getNumberArray().get(7));
    }

    /**
     * remove specific value from decimal-type attribute with custom-filter-expression
     */
    @DisplayName("Remove value from simple decimal-array with: decimalArray[value gt 2.8]")
    @Test
    public void testRemoveValueFromDecimalStringArrayWithFilterExpression()
    {
      AllTypes allTypes = new AllTypes(true);
      allTypes.setDecimalArray(Arrays.asList(1.1, 2.9, 3.0));

      final String path = "decimalArray[value gt 2.8]";
      PatchRequestOperation patchRequestOperation = PatchRequestOperation.builder()
                                                                         .op(PatchOp.REMOVE)
                                                                         .path(path)
                                                                         .build();
      List<PatchRequestOperation> operations = Arrays.asList(patchRequestOperation);

      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

      AllTypes patchedAllTypes = patchAllTypes(allTypes, patchOpRequest, true);
      Assertions.assertEquals(1, patchedAllTypes.getDecimalArray().size());
      Assertions.assertEquals(1.1, patchedAllTypes.getDecimalArray().get(0));
    }
  }
}
