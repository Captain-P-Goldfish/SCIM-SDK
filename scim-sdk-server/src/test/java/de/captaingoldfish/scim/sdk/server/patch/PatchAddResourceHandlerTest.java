package de.captaingoldfish.scim.sdk.server.patch;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PersonRole;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpointBridge;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.AllTypesHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.RequestContextException;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 30.10.2019 - 09:02 <br>
 * <br>
 */
@Slf4j
public class PatchAddResourceHandlerTest implements FileReferences
{

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
   * the service provider with the current endpoints configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * used to access the default
   * {@link de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround}-handlers
   */
  private ResourceEndpoint resourceEndpoint;

  /**
   * the resource-handler used for the patch-tests
   */
  private AllTypesHandlerImpl allTypesHandler;

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
    this.allTypesHandler = new AllTypesHandlerImpl();
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceType,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));
    this.resourceTypeFactory = ResourceEndpointBridge.getResourceTypeFactory(resourceEndpoint);
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
    allTypesHandler.getInMemoryMap().put(allTypes.getId().get(), allTypes);
  }

  /**
   * adds an allTypes object to the {@link #allTypesHandler}
   */
  private void addUserToProvider(UserHandlerImpl userHandler, User user)
  {
    if (!user.getId().isPresent())
    {
      user.setId(UUID.randomUUID().toString());
    }
    userHandler.getInMemoryMap().put(user.getId().get(), user);
  }

  @Test
  public void testFieldsAreSet()
  {
    final String notNeededId = UUID.randomUUID().toString();
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(notNeededId,
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    Assertions.assertNotNull(patchRequestHandler.getPatchConfig());
    Assertions.assertNotNull(patchRequestHandler.getPatchWorkarounds());
    Assertions.assertFalse(patchRequestHandler.getPatchWorkarounds().isEmpty());
    Assertions.assertNotNull(patchRequestHandler.getMainSchema());
    Assertions.assertNotNull(patchRequestHandler.getExtensionSchemas());
    Assertions.assertFalse(patchRequestHandler.getExtensionSchemas().isEmpty());
    Assertions.assertNotNull(patchRequestHandler.getResourceType());
    Assertions.assertNotNull(patchRequestHandler.getPatchOperationHandler());
  }

  /**
   * verifies that simple attributes can successfully be added by a patch operation
   */
  @TestFactory
  public List<DynamicTest> testAddSimpleAttribute()
  {
    List<DynamicTest> dynamicTestList = new ArrayList<>();
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("string", new TextNode("hello world"))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("date", new TextNode("1940-03-10T00:00:00Z"))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("date",
                                                                    new TextNode("1940-03-10T00:00:00+02:00"))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("number", new IntNode(5))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("number", new LongNode(Long.MAX_VALUE))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("decimal", new DoubleNode(5.4))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("decimal", new DoubleNode(Double.MAX_VALUE))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("bool", BooleanNode.getTrue())));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("bool", BooleanNode.getFalse())));

    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("string", new TextNode("hello world")),
                                                  new NameValuePair("number", new IntNode(5)),
                                                  new NameValuePair("bool", BooleanNode.getTrue()),
                                                  new NameValuePair("decimal", new DoubleNode(5.6))));

    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    complex.setNumber(Long.MAX_VALUE);
    complex.setDecimal(5.8);
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("complex", complex)));
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.add(complex);
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("multiComplex", arrayNode)));

    return dynamicTestList;
  }

  /**
   * this test will verify that multivalued complex types will be added to already existing attributes
   */
  @Test
  public void testAddMultiValuedComplexType()
  {
    AllTypes allTypes = new AllTypes(true);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes multiComplex = new AllTypes(false);
    multiComplex.setString("hello world");
    multiComplex.setNumber(Long.MAX_VALUE);
    multiComplex.setDecimal(5.8);
    allTypes.setMultiComplex(Collections.singletonList(multiComplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multiComplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(multiComplex, patchedAllTypes.getMultiComplex().get(0));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * this test will verify that the meta-attribute will be ignored in a patch-resource request
   */
  @DisplayName("Ignore meta-attribute in patch-resource-request")
  @Test
  public void testMetaAttributeDoesNotCauseProblems()
  {
    AllTypes allTypes = new AllTypes(true);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes patchResource = new AllTypes(true);
    patchResource.setString("hello world");
    patchResource.setMeta(Meta.builder().created(Instant.now()).version("1").build());

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(patchResource)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has("string"));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.META));
  }

  /**
   * this test will verify that no effective change is made on a multicomplex type if the replace value is
   * identical to the previous array
   */
  @Test
  public void testReplaceMultiValuedComplexTypeWithNoChange()
  {
    AllTypes allTypes = new AllTypes(true);
    LocalDateTime created = LocalDateTime.now();
    Meta meta = Meta.builder().created(created).lastModified(created).build();
    allTypes.setMeta(meta);

    AllTypes multiComplex = new AllTypes(false);
    multiComplex.setString("hello world");
    multiComplex.setNumber(Long.MAX_VALUE);
    multiComplex.setDecimal(5.8);
    allTypes.setMultiComplex(Collections.singletonList(multiComplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multiComplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(multiComplex, patchedAllTypes.getMultiComplex().get(0));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertEquals(patchedAllTypes.getMeta().get().getCreated().get(),
                            patchedAllTypes.getMeta().get().getLastModified().get());
  }

  /**
   * this test will verify that a replace operation of a multicomplex node is done successfully
   */
  @Test
  public void testReplaceMultiValuedComplexTypeWithChange()
  {
    AllTypes allTypes = new AllTypes(true);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes multiComplex = new AllTypes(false);
    multiComplex.setString("hello world");
    multiComplex.setNumber(Long.MAX_VALUE);
    multiComplex.setDecimal(5.8);
    allTypes.setMultiComplex(Collections.singletonList(multiComplex));

    AllTypes allTypeChanges = new AllTypes(true);
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setString("happy day");
    multiComplex2.setNumber(Long.MIN_VALUE);
    multiComplex2.setDecimal(88454.8);
    allTypeChanges.setMultiComplex(Collections.singletonList(multiComplex2));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(4, patchedAllTypes.size());
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.META));
    Assertions.assertTrue(patchedAllTypes.has("multiComplex"));
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().size());
    Assertions.assertEquals(multiComplex2, patchedAllTypes.getMultiComplex().get(0));
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that an exception with an appropriate error message is thrown if the value path is unknown
   */
  @Test
  public void testAttributeDoesNotExist()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.set("unknown", new TextNode("unknown"));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    try
    {
      log.warn(patchRequestHandler.handlePatchRequest(patchOpRequest).toPrettyString());
      Assertions.fail();
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(ScimType.RFC7644.INVALID_PATH, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * verifies that no exception is thrown if the addressed attribute does not exist and the configuration
   * property {@link PatchConfig#isIgnoreUnknownAttribute()} is set to true
   */
  @Test
  public void testAttributeDoesNotExistAndDoNotFailOnNoTarget()
  {
    serviceProvider.getPatchConfig().setIgnoreUnknownAttribute(true);
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.set("emails", new TextNode("unknown"));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(2, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.ID));
  }

  /**
   * this test will verify that a simple attribute reference that is unknown will not cause an error if the
   * patchconfig has the field {@link PatchConfig#isIgnoreUnknownAttribute()} set to true
   */
  @Test
  public void testAddWithMsAzureStyleSimpleAttributeReferenceWithUnknownAttributeButNotFailing()
  {
    serviceProvider.getPatchConfig().setIgnoreUnknownAttribute(true);

    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    ArrayNode attributes = (ArrayNode)enterpriseUserSchema.get(AttributeNames.RFC7643.ATTRIBUTES);
    attributes.add(JsonHelper.readJsonDocument(getComplexNodeDefinitionForTest()));
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceTypeNode,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));


    Schema enterpriseSchema = resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes(true);
    AllTypes allTypeChanges = new AllTypes(true);

    allTypeChanges.set(enterpriseSchema.getNonNullId() + ":number", new IntNode(4));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals(2, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.ID));
  }

  /**
   * this test will verify that no changes are made to the resource if the given complex type is identical to
   * the existing one
   */
  @Test
  public void testComplexTypeAlreadyExists()
  {
    AllTypes allTypes = new AllTypes(true);
    LocalDateTime created = LocalDateTime.now();
    Meta meta = Meta.builder().created(created).lastModified(created).build();
    allTypes.setMeta(meta);

    AllTypes complex = new AllTypes(false);
    complex.setNumber(Long.MAX_VALUE);
    complex.setDecimal(5.8);
    allTypes.setComplex(complex);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setComplex(complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals(allTypes.getId().get(), patchedAllTypes.getId().get());
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertEquals(complex, patchedAllTypes.getComplex().get());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    // this is important. The change must not have been made so the lastModified value must be identical with the
    // created value
    Assertions.assertEquals(patchedAllTypes.getMeta().get().getCreated().get(),
                            patchedAllTypes.getMeta().get().getLastModified().get());
  }

  /**
   * this test will verify that existing attributes from complex nodes are not removed if new attributes are
   * added
   */
  @SneakyThrows
  @Test
  public void testAddSimpleValuesToComplexAttribute()
  {
    AllTypes allTypes = new AllTypes(true);
    LocalDateTime created = LocalDateTime.now();
    allTypes.setMeta(Meta.builder().created(created).lastModified(created).build());
    AllTypes innerComplex = new AllTypes(false);
    innerComplex.setString("hello world");
    allTypes.setComplex(innerComplex);

    AllTypes complex = new AllTypes(false);
    complex.setNumber(Long.MAX_VALUE);
    complex.setDecimal(5.8);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setComplex(complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    Thread.sleep(1); // to make sure that the lastModified value will differ
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertTrue(patchedAllTypes.getComplex().isPresent());
    Assertions.assertTrue(patchedAllTypes.getComplex().get().getString().isPresent(),
                          patchedAllTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals("hello world", patchedAllTypes.getComplex().get().getString().get());
    Assertions.assertEquals(4, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertEquals(allTypes.getId().get(), patchedAllTypes.getId().get());
    Assertions.assertNotEquals(complex, patchedAllTypes.getComplex().get());
    Assertions.assertEquals(3, patchedAllTypes.getComplex().get().size());
    Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
    // this is important. The change must not have been made so the lastModified value must not be present!
    Assertions.assertNotEquals(patchedAllTypes.getMeta().get().getCreated().get(),
                               patchedAllTypes.getMeta().get().getLastModified().get());
  }

  /**
   * verifies that no change was made if a simple attribute was added that has the same value as the existing
   * one
   */
  @Test
  public void testAddIdenticalSimpleValue()
  {
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(3, patchedAllTypes.size(), patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.SCHEMAS));
    Assertions.assertTrue(patchedAllTypes.has(AttributeNames.RFC7643.ID));
    Assertions.assertTrue(patchedAllTypes.has("string"));
    Assertions.assertTrue(patchedAllTypes.getString().isPresent());
    Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
    // no change must have been made so the last-modified value must be identical to before
    Assertions.assertFalse(patchedAllTypes.getMeta().isPresent());
  }

  /**
   * this test will verify that attributes can be added to simple arrays
   */
  @Test
  public void testAddMultiValuedArrayOnExistingArray()
  {
    AllTypes allTypes = new AllTypes(true);
    allTypes.setNumberArray(Arrays.asList(1L, 2L));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setNumberArray(Arrays.asList(3L, 4L));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(4, patchedAllTypes.getNumberArray().size());
    MatcherAssert.assertThat(patchedAllTypes.getNumberArray(), Matchers.hasItems(1L, 2L, 3L, 4L));
  }

  /**
   * this test will verify that attributes can be added to simple arrays
   */
  @Test
  public void testAddMultiValuedArrayOnNotExistingArray()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setNumberArray(Arrays.asList(3L, 4L));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(2, patchedAllTypes.getNumberArray().size());
    MatcherAssert.assertThat(patchedAllTypes.getNumberArray(), Matchers.hasItems(3L, 4L));
  }

  /**
   * this test will verify that an appropriate error is returned if the array contains illegal values
   */
  @Test
  public void testAddIllegalValueOnMultivaluedArray()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    ArrayNode illegalNumberArray = new ArrayNode(JsonNodeFactory.instance);
    illegalNumberArray.add("hello");
    illegalNumberArray.add("world");
    allTypeChanges.set("numberArray", illegalNumberArray);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                         () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    SchemaAttribute schemaAttribute = allTypesResourceType.getMainSchema().getSchemaAttribute("numberArray");
    ErrorResponse errorResponse = new ErrorResponse(ex);
    ex.getValidationContext().writeToErrorResponse(errorResponse);

    Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\",\"world\"]'",
                            errorResponse.getDetail().get());

    List<String> fieldErrors = errorResponse.getFieldErrors().get(schemaAttribute.getScimNodeName());
    Assertions.assertEquals(2, fieldErrors.size());
    Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\",\"world\"]'",
                            fieldErrors.get(0));
    Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                          + "with value '\"hello\"'",
                                          schemaAttribute.getFullResourceName()),
                            fieldErrors.get(1));
  }

  /**
   * makes sure that a simple value is accepted for an array value. If the brackets in the json are missing we
   * will accept it as single element array instead of throwing an error
   */
  @Test
  public void testAddToArrayWithSimpleAttribute()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.set("numberArray", new IntNode(5));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(1, patchedAllTypes.getNumberArray().size());
    Assertions.assertEquals(5, patchedAllTypes.getNumberArray().get(0));
  }

  /**
   * makes sure that a simple value is accepted for an array value. If the brackets in the json are missing we
   * will accept it as single element array instead of throwing an error
   */
  @Test
  public void testAddIllegalValueToArrayWithSimpleAttribute()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.set("numberArray", new TextNode("illegal-value"));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                         () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    SchemaAttribute schemaAttribute = allTypesResourceType.getMainSchema().getSchemaAttribute("numberArray");
    ErrorResponse errorResponse = new ErrorResponse(ex);
    ex.getValidationContext().writeToErrorResponse(errorResponse);

    log.warn(errorResponse.toPrettyString());
    Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"illegal-value\"]'",
                            errorResponse.getDetail().get());
    List<String> fieldErrors = errorResponse.getFieldErrors().get(schemaAttribute.getScimNodeName());
    Assertions.assertEquals(2, fieldErrors.size());
    Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"illegal-value\"]'",
                            fieldErrors.get(0));
    Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                          + "with value '\"illegal-value\"'",
                                          schemaAttribute.getFullResourceName()),
                            fieldErrors.get(1));
  }

  /**
   * This test will make sure that unknown attributes in multivalued-complex direct references are not added to
   * the resource
   */
  @Test
  public void testAddMultiValuedArrayWithUnknownAttributeWithPatchAdd()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    multicomplex.set("unknown", new TextNode("unknown"));
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    {
      // the first attribute is unchanged
      Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().get(0).size());
      Assertions.assertEquals(multicomplex.getString().get(),
                              patchedAllTypes.getMultiComplex().get(0).getString().get());
      Assertions.assertEquals(multicomplex.get("unknown"), patchedAllTypes.getMultiComplex().get(0).get("unknown"));
      Assertions.assertEquals(multicomplex.get("unknown"), patchedAllTypes.getMultiComplex().get(0).get("unknown"));
    }
    {
      // the added attribute does not have the unknown-attribute
      Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(1).size());
      Assertions.assertEquals(multicomplex.getString().get(),
                              patchedAllTypes.getMultiComplex().get(1).getString().get());
      Assertions.assertNull(patchedAllTypes.getMultiComplex().get(1).get("unknown"));
    }
  }

  /**
   * This test will make sure that unknown attributes in multivalued-complex direct references are not added to
   * the resource
   */
  @Test
  public void testAddMultiValuedArrayWithUnknownAttributeWithPatchReplace()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    multicomplex.set("unknown", new TextNode("unknown"));
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().size());
    {
      // the added attribute does not have the unknown-attribute
      Assertions.assertEquals(1, patchedAllTypes.getMultiComplex().get(0).size());
      Assertions.assertEquals(multicomplex.getString().get(),
                              patchedAllTypes.getMultiComplex().get(0).getString().get());
      Assertions.assertNull(patchedAllTypes.getMultiComplex().get(0).get("unknown"));
    }
  }

  /**
   * This test will make sure that unknown attributes in complex direct references are not added to the resource
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddComplexWithUnknownAttribute(PatchOp patchOp)
  {
    serviceProvider.getPatchConfig().setIgnoreUnknownAttribute(true);

    AllTypes patchResource = new AllTypes(true);
    AllTypes patchComplex = new AllTypes(false);
    patchComplex.setString("hello world");
    patchComplex.set("unknown", new TextNode("unknown"));
    patchResource.setComplex(patchComplex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .valueNode(patchResource)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    allTypes.setComplex(complex);
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertEquals(1, patchedAllTypes.getComplex().get().size());
    {
      // the added attribute does not have the unknown-attribute
      Assertions.assertEquals(complex.getString().get(), patchedAllTypes.getComplex().get().getString().get());
      Assertions.assertNull(patchedAllTypes.getComplex().get().get("unknown"));
    }
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
  }

  /**
   * builds a patch operation test in which the given values are added to the resource by the patch operation
   *
   * @param nameValuePair a mandatory name value pair that must be added
   * @param nameValuePairs additional name value pairs to check if several attributes are added
   */
  private DynamicTest getAddSimpleAttributeTest(NameValuePair nameValuePair, NameValuePair... nameValuePairs)
  {
    String testName = "\"" + nameValuePair.getAttributeName() + "\": \"" + nameValuePair.getValue() + "\"";
    if (nameValuePairs != null)
    {
      for ( NameValuePair valuePair : nameValuePairs )
      {
        testName += " ; " + "\"" + valuePair.getAttributeName() + "\": \"" + valuePair.getValue() + "\"";
      }
    }
    return DynamicTest.dynamicTest(testName, () -> {
      AllTypes allTypes = new AllTypes(true);
      AllTypes allTypeChanges = new AllTypes(true);
      allTypeChanges.set(nameValuePair.getAttributeName(), nameValuePair.getValue());
      if (nameValuePairs != null)
      {
        for ( NameValuePair valuePair : nameValuePairs )
        {
          allTypeChanges.set(valuePair.getAttributeName(), valuePair.getValue());
        }
      }
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.ADD)
                                                                                  .valueNode(allTypeChanges)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      addAllTypesToProvider(allTypes);
      PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                  allTypesResourceType.getResourceHandlerImpl(),
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
      Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
      Assertions.assertTrue(allTypes.has(AttributeNames.RFC7643.ID));
      AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

      Assertions.assertTrue(patchRequestHandler.isResourceChanged());
      Assertions.assertTrue(patchedAllTypes.size() > 1, patchedAllTypes.toPrettyString());
      // the added attribute and lastModifed have been added by patch
      Assertions.assertEquals(1/* id */ + 3 + (nameValuePairs == null ? 0 : nameValuePairs.length),
                              patchedAllTypes.size());
      Assertions.assertNotNull(patchedAllTypes.get(nameValuePair.getAttributeName()));
      Assertions.assertEquals(nameValuePair.getValue(), patchedAllTypes.get(nameValuePair.getAttributeName()));
      Assertions.assertTrue(patchedAllTypes.getMeta().isPresent());
      Assertions.assertTrue(patchedAllTypes.getMeta().get().getLastModified().isPresent());
      if (nameValuePairs != null)
      {
        for ( NameValuePair valuePair : nameValuePairs )
        {
          Assertions.assertNotNull(patchedAllTypes.get(valuePair.getAttributeName()));
          Assertions.assertEquals(valuePair.getValue(), patchedAllTypes.get(valuePair.getAttributeName()));
        }
      }
    });
  }

  /**
   * verifies that an empty value list is not tolerated
   */
  @Test
  public void testNoValuesPresent()
  {
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder().op(PatchOp.ADD).build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    try
    {
      log.warn(patchRequestHandler.handlePatchRequest(patchOpRequest).toPrettyString());
      Assertions.fail();
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    }
  }

  /**
   * verifies that several values are not tolerated if no target is specified
   */
  @Test
  public void testNoTargetSpecifiedAndTwoResourcesAreAdded()
  {
    List<String> values = Arrays.asList(new AllTypes(true).toString(), new AllTypes(true).toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    try
    {
      log.warn(patchRequestHandler.handlePatchRequest(patchOpRequest).toPrettyString());
      Assertions.fail();
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    }
  }

  /**
   * this test must verify that an attribute may be set if the immutable attribute has not been set yet
   */
  @Test
  public void testAddImmutableAttributeIfNotSet()
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "string",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertTrue(patchedAllTypes.getString().isPresent());
    Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly does not lead to a
   * {@link BadRequestException} if tried to set to the same value.
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testAddImmutableAndReadOnlyAttributeWithNoChange(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta, "string", null, mutability, null, null, null, null, null, null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    try
    {
      AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.assertFalse(patchRequestHandler.isResourceChanged());
      Assertions.assertTrue(patchedAllTypes.getString().isPresent());
      Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
    }
    catch (ScimException ex)
    {
      Assertions.fail("this point must not be reached", ex);
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly does not lead to a
   * {@link BadRequestException} if tried to set to the same value.
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testReplaceImmutableAndReadOnlyAttributeWithNoChange(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta, "string", null, mutability, null, null, null, null, null, null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    try
    {
      AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.assertFalse(patchRequestHandler.isResourceChanged());
      Assertions.assertTrue(patchedAllTypes.getString().isPresent());
      Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail());
      Assertions.fail("this point must not be reached");
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable leads to a
   * {@link BadRequestException} if tried to set. The immutable object should fail because it is already set
   * within this test
   */
  @Test
  public void testAddImmutableAttributeWithChange()
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "string",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("new hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    try
    {
      log.warn(patchRequestHandler.handlePatchRequest(patchOpRequest).toPrettyString());
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable leads to a
   * {@link BadRequestException} if tried to set. The immutable object should fail because it is already set
   * within this test
   */
  @Test
  public void testReplaceImmutableAttributeWithChange()
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "string",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("new hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    try
    {
      log.warn(patchRequestHandler.handlePatchRequest(patchOpRequest).toPrettyString());
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of readOnly will not cause an error but will
   * be ignored
   */
  @Test
  public void testReplaceReadOnlyAttributeWithChange()
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "string",
                                       null,
                                       Mutability.READ_ONLY,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("new hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable leads to a
   * {@link BadRequestException} if tried to set. The immutable object should fail because it is already set
   * within this test
   */
  @Test
  public void testAddImmutableAndReadOnlyAttributeForComplex()
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "complex.string",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    AllTypes innerComplex = new AllTypes(false);
    innerComplex.setString("hello world");
    allTypes.setComplex(innerComplex);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes complex = new AllTypes(false);
    complex.setString("new hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setComplex(complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    try
    {
      log.warn(patchRequestHandler.handlePatchRequest(patchOpRequest).toPrettyString());
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    }
  }

  /**
   * this test will verify that immutable subAttributes are ignored in case that a new multiComplex attribute is
   * added without a filter
   */
  @Test
  public void testAddImmutableAndReadOnlyAttributeForMultiComplexSubAttribute()
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "multiComplex.string",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    for ( AllTypes patchedMultiComplex : patchedAllTypes.getMultiComplex() )
    {
      Assertions.assertEquals(1, patchedMultiComplex.size());
      Assertions.assertEquals(multicomplex.getString().get(), patchedMultiComplex.getString().get());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable leads to a
   * {@link BadRequestException} if tried to set. The immutable object should fail because it is already set
   * within this test
   */
  @Test
  public void testAddImmutableAndReadOnlyAttributeForMultiComplex()
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "multiComplex",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    try
    {
      log.warn(patchRequestHandler.handlePatchRequest(patchOpRequest).toPrettyString());
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
      Assertions.assertFalse(patchRequestHandler.isResourceChanged());
    }
  }

  /**
   * this method will verify that a second added primary value will replace the previous set primary value
   */
  @Test
  public void testSetPrimary()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.set(AttributeNames.RFC7643.PRIMARY, BooleanNode.getTrue());
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    AtomicBoolean primaryFound = new AtomicBoolean(false);
    for ( JsonNode complex : patchedAllTypes.getMultiComplex() )
    {
      JsonNode prime = complex.get(AttributeNames.RFC7643.PRIMARY);
      if (primaryFound.get())
      {
        Assertions.fail("two primary values in the response");
      }
      primaryFound.weakCompareAndSet(false, prime != null && prime.booleanValue());
    }
    Assertions.assertTrue(primaryFound.get());
  }

  /**
   * verifies that a patch request containing two primary elements is rejected
   */
  @Test
  public void testSetTwoPrimaryValues()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.set(AttributeNames.RFC7643.PRIMARY, BooleanNode.getTrue());
    multicomplex.setNumber(4L);
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Arrays.asList(multicomplex, multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                         () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    SchemaAttribute schemaAttribute = allTypesResourceType.getSchemaAttribute("multiComplex").get();
    ErrorResponse errorResponse = new ErrorResponse(ex);
    ex.getValidationContext().writeToErrorResponse(errorResponse);
    Assertions.assertEquals(String.format("Attribute '%s' has at least two primary values but only one primary is "
                                          + "allowed '[{\"primary\":true,\"number\":4},{\"primary\":true,\"number\":4}]'",
                                          schemaAttribute.getFullResourceName()),
                            errorResponse.getDetail().get());
  }

  /**
   * verifies that it is illegal to add 2 primary values into a multivalued complex type
   */
  @Test
  public void testAddMultipleComplexValues()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.set(AttributeNames.RFC7643.PRIMARY, BooleanNode.getTrue());
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    AllTypes newMultivalued = new AllTypes(false);
    newMultivalued.setString("hello world");
    newMultivalued.setNumber(5L);
    allTypeChanges.setMultiComplex(Arrays.asList(newMultivalued, newMultivalued));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(3, patchedAllTypes.getMultiComplex().size());
    {
      Assertions.assertTrue(patchedAllTypes.getMultiComplex()
                                           .get(0)
                                           .get(AttributeNames.RFC7643.PRIMARY)
                                           .booleanValue());
    }
    {
      Assertions.assertEquals(newMultivalued.getString().get(),
                              patchedAllTypes.getMultiComplex().get(1).getString().get());
      Assertions.assertEquals(newMultivalued.getNumber().get(),
                              patchedAllTypes.getMultiComplex().get(1).getNumber().get());
    }
    {
      Assertions.assertEquals(newMultivalued.getString().get(),
                              patchedAllTypes.getMultiComplex().get(2).getString().get());
      Assertions.assertEquals(newMultivalued.getNumber().get(),
                              patchedAllTypes.getMultiComplex().get(2).getNumber().get());
    }
  }

  /**
   * verifies that it is illegal to add 2 primary values into a multivalued complex type
   */
  @Test
  public void testReplaceMultipleComplexValues()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.set(AttributeNames.RFC7643.PRIMARY, BooleanNode.getTrue());
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    AllTypes newMultivalued = new AllTypes(false);
    newMultivalued.setString("hello world");
    newMultivalued.setNumber(5L);
    allTypeChanges.setMultiComplex(Arrays.asList(newMultivalued, newMultivalued));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    Assertions.assertEquals(2, patchedAllTypes.getMultiComplex().size());
    {
      Assertions.assertEquals(newMultivalued.getString().get(),
                              patchedAllTypes.getMultiComplex().get(0).getString().get());
      Assertions.assertEquals(newMultivalued.getNumber().get(),
                              patchedAllTypes.getMultiComplex().get(0).getNumber().get());
    }
    {
      Assertions.assertEquals(newMultivalued.getString().get(),
                              patchedAllTypes.getMultiComplex().get(1).getString().get());
      Assertions.assertEquals(newMultivalued.getNumber().get(),
                              patchedAllTypes.getMultiComplex().get(1).getNumber().get());
    }
  }

  /**
   * verifies that the emails array can be replaced if a primary is set once
   */
  @Test
  public void testReplaceEmailsWithPrimary()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);

    List<Email> emails = Arrays.asList(Email.builder().value("1@1.de").primary(true).build(),
                                       Email.builder().value("2@2.de").build(),
                                       Email.builder().value("3@3.de").build());
    ScimArrayNode emailArray = new ScimArrayNode(null);
    emails.forEach(emailArray::add);
    allTypeChanges.set(AttributeNames.RFC7643.EMAILS, emailArray);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    log.debug(patchOpRequest.toPrettyString());
    addAllTypesToProvider(allTypes);
    log.info(allTypes.toPrettyString());
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    log.warn(patchedAllTypes.toPrettyString());
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(emailArray, patchedAllTypes.get(AttributeNames.RFC7643.EMAILS));
  }

  /**
   * verifies that the email is accepted in the resource even if it was not set as arrayNode
   */
  @Test
  public void testPatchEmailsWithWithNonArrayComplexNode()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);

    Email email = Email.builder().value("1@1.de").primary(true).build();
    allTypeChanges.set(AttributeNames.RFC7643.EMAILS, email);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    ArrayNode emailsNode = (ArrayNode)patchedAllTypes.get(AttributeNames.RFC7643.EMAILS);
    Assertions.assertNotNull(emailsNode);
    Assertions.assertEquals(1, emailsNode.size());
    Assertions.assertEquals(email, emailsNode.get(0));
  }

  /**
   * this test will verify that attribute adding works also for extensions
   */
  @Test
  public void testAddValuesWithinExtension()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setEnterpriseUser(EnterpriseUser.builder().costCenter("something").build());

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
  }

  /**
   * this test will verify that attribute adding works also for extensions even if there are naming conflicts
   * with the main schema
   */
  @Test
  public void testAddValuesWithinExtensionWithAttributeNameConflict()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    final String ambiguousAttributeName = "number";
    this.allTypesResourceType = TestHelper.addAttributeToSchema(resourceEndpoint,
                                                                allTypesHandler,
                                                                ambiguousAttributeName,
                                                                Type.STRING,
                                                                allTypesResourceTypeNode,
                                                                allTypesSchema,
                                                                enterpriseUserSchema);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setNumber(50L);

    AllTypes allTypeChanges = new AllTypes(true);
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
    enterpriseUser.set(ambiguousAttributeName, new TextNode("hello world"));
    allTypeChanges.setEnterpriseUser(enterpriseUser);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals("hello world",
                            patchedAllTypes.getEnterpriseUser().get().get(ambiguousAttributeName).textValue());
  }

  /**
   * this test will verify that attribute adding works also for extensions even if there are naming conflicts
   * with the main schema
   */
  @Test
  public void testAddValuesWithinExtensionWithAttributeNameConflictOnComplex()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    ArrayNode attributes = (ArrayNode)enterpriseUserSchema.get(AttributeNames.RFC7643.ATTRIBUTES);
    attributes.add(JsonHelper.readJsonDocument(getComplexNodeDefinitionForTest()));
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceTypeNode,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));


    resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    AllTypes allTypeChanges = new AllTypes(true);
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
    ScimObjectNode complexEnterprise = new ScimObjectNode();
    complexEnterprise.set("number", new TextNode("hello world"));
    enterpriseUser.set("complex", complexEnterprise);
    allTypeChanges.setEnterpriseUser(enterpriseUser);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals("hello world",
                            patchedAllTypes.getEnterpriseUser().get().get("complex").get("number").textValue());
  }

  /**
   * this test will verify that attribute adding works also for extensions even if there are naming conflicts
   * with the main schema with msAzure style sub-attribute reference on extension
   */
  @Test
  public void testAddWithMsAzureStyleSubAttributeReference()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    ArrayNode attributes = (ArrayNode)enterpriseUserSchema.get(AttributeNames.RFC7643.ATTRIBUTES);
    attributes.add(JsonHelper.readJsonDocument(getComplexNodeDefinitionForTest()));
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceTypeNode,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));


    Schema enterpriseSchema = resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.set(enterpriseSchema.getNonNullId() + ":complex.number", new TextNode("hello world"));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals("hello world",
                            patchedAllTypes.getEnterpriseUser().get().get("complex").get("number").textValue());
  }

  /**
   * this test will verify that complex attributes can be added with the msAzure style notation
   */
  @Test
  public void testAddWithMsAzureStyleDirectComplexAttributeReference()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    ArrayNode attributes = (ArrayNode)enterpriseUserSchema.get(AttributeNames.RFC7643.ATTRIBUTES);
    attributes.add(JsonHelper.readJsonDocument(getComplexNodeDefinitionForTest()));
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceTypeNode,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));


    Schema enterpriseSchema = resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    // the generated extra-attribute has the field number defined as string
    complexAllTypes.set("number", new TextNode("hello world"));
    allTypes.setComplex(complexAllTypes);

    AllTypes allTypeChanges = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.set("number", new TextNode("goodbye world"));
    allTypeChanges.set(enterpriseSchema.getNonNullId() + ":complex", complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals("goodbye world",
                            patchedAllTypes.getEnterpriseUser().get().get("complex").get("number").textValue());
  }

  /**
   * this test will verify that complex sub-attributes can be added with the msAzure style notation
   */
  @Test
  public void testAddWithMsAzureStyleComplexSubattributeReference()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    ArrayNode attributes = (ArrayNode)enterpriseUserSchema.get(AttributeNames.RFC7643.ATTRIBUTES);
    attributes.add(JsonHelper.readJsonDocument(getComplexNodeDefinitionForTest()));
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceTypeNode,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));


    Schema enterpriseSchema = resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    // the generated extra-attribute has the field number defined as string
    complexAllTypes.set("number", new TextNode("hello world"));
    allTypes.setComplex(complexAllTypes);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.set(enterpriseSchema.getNonNullId() + ":complex.number", new TextNode("goodbye world"));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals("goodbye world",
                            patchedAllTypes.getEnterpriseUser().get().get("complex").get("number").textValue());
  }

  /**
   * this test will verify that a simple attribute can be added with the msAzure style notation
   */
  @Test
  public void testAddWithMsAzureStyleSimpleAttributeReference()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    ArrayNode attributes = (ArrayNode)enterpriseUserSchema.get(AttributeNames.RFC7643.ATTRIBUTES);
    attributes.add(JsonHelper.readJsonDocument(getComplexNodeDefinitionForTest()));
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceTypeNode,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));


    Schema enterpriseSchema = resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setNumber(1L);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.set(enterpriseSchema.getNonNullId() + ":" + AttributeNames.RFC7643.COST_CENTER,
                       new TextNode("costCenterValue"));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertTrue(patchedAllTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals("costCenterValue",
                            patchedAllTypes.getEnterpriseUser()
                                           .get()
                                           .get(AttributeNames.RFC7643.COST_CENTER)
                                           .textValue());
  }

  /**
   * this test will verify that a simple attribute reference that is unknown will cause an error
   */
  @Test
  public void testAddWithMsAzureStyleSimpleAttributeReferenceWithUnknownAttribute()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    ArrayNode attributes = (ArrayNode)enterpriseUserSchema.get(AttributeNames.RFC7643.ATTRIBUTES);
    attributes.add(JsonHelper.readJsonDocument(getComplexNodeDefinitionForTest()));
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceTypeNode,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));


    Schema enterpriseSchema = resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setNumber(1L);

    AllTypes allTypeChanges = new AllTypes(true);
    final String attributeName = enterpriseSchema.getNonNullId() + ":number";
    allTypeChanges.set(attributeName, new IntNode(4));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                     () -> patchRequestHandler.handlePatchRequest(patchOpRequest));

    Assertions.assertEquals(String.format("Attribute '%s' is unknown to resource type 'AllTypes'", attributeName),
                            ex.getMessage());
    Assertions.assertEquals(ScimType.RFC7644.INVALID_PATH, ex.getScimType());
  }

  /**
   * this test will verify that attribute adding works also for extensions even if there are naming conflicts
   * with the main schema with msAzure style attribute reference on extension
   */
  @Test
  public void testAddAttributeWithIllegalType()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    ArrayNode attributes = (ArrayNode)enterpriseUserSchema.get(AttributeNames.RFC7643.ATTRIBUTES);
    attributes.add(JsonHelper.readJsonDocument(getComplexNodeDefinitionForTest()));
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceTypeNode,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));


    Schema enterpriseSchema = resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    // the generated extra-attribute has the field number defined as string
    complexAllTypes.set("number", new TextNode("hello world"));
    allTypes.setComplex(complexAllTypes);

    AllTypes allTypeChanges = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setNumber(10L);
    allTypeChanges.set(enterpriseSchema.getNonNullId() + ":complex", complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));
    RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                         () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    SchemaAttribute schemaAttribute = enterpriseSchema.getSchemaAttribute("complex.number");
    ErrorResponse errorResponse = new ErrorResponse(ex);
    ex.getValidationContext().writeToErrorResponse(errorResponse);

    Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'string' but of type 'number' with value '10'",
                                          schemaAttribute.getFullResourceName()),
                            errorResponse.getDetail().get());

    List<String> fieldErrors = errorResponse.getFieldErrors().get(schemaAttribute.getScimNodeName());
    Assertions.assertEquals(1, fieldErrors.size());
    Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'string' but of type 'number' with value '10'",
                                          schemaAttribute.getFullResourceName()),
                            fieldErrors.get(0));
  }

  /**
   * this test will verify that the MsAzure Workaround is executed for add and replace operations.
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testMsAzureWorkaroundIsExecuted(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    User user = new User();

    // @formatter:off
    final String patchValue = "{" +
                                "  \"active\": true," +
                                "  \"name.givenName\": \"Terrence\"," +
                                "  \"name.familyName\": \"William\"" +
                                "}";
    // @formatter:on

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .value(patchValue)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));

    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    Assertions.assertTrue(patchedUser.isActive().orElse(false));
    Assertions.assertEquals("Terrence", patchedUser.getName().flatMap(Name::getGivenName).orElse(null));
    Assertions.assertEquals("William", patchedUser.getName().flatMap(Name::getFamilyName).orElse(null));
  }

  /**
   * verifies that the problematic ms-azure notation will be supported for multivalued-complex types.
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testMsAzureNotationForMultivaluedComplexTypes(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);
    User user = new User();

    final String patchValue = "{\"emails.value\": \"max@mustermann.de\"}";

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .value(patchValue)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));

    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    log.warn(patchedUser.toPrettyString());

    Assertions.assertEquals(1, patchedUser.getEmails().size(), patchedUser.toPrettyString());
    Assertions.assertEquals("max@mustermann.de", patchedUser.getEmails().get(0).getValue().get());
  }

  /**
   * verifies that if the ms-azure patch filter workaround is active that the values of the filter are added if
   * the filter does not match an existing entry.<br>
   * <br>
   * Example:<br>
   * the request
   *
   * <pre>
   * {
   *   "schemas": [
   *     "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *   ],
   *   "Operations": [
   *     {
   *       "op": "add",
   *       "path": "emails[type eq \"work\"].value",
   *       "value": "max@mustermann.de"
   *     }
   *   ]
   * }
   * </pre>
   *
   * must result in
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *   "emails" : [ {
   *     "type" : "work",
   *     "value" : "max@mustermann.de"
   *   } ],
   *   "meta" : {
   *     "lastModified" : "2023-02-05T11:56:25.8049737+01:00"
   *   }
   * }
   * </pre>
   *
   * if the user did not have any emails before
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testMsAzureBehaviourForMultivaluedComplexTypesWithFilterInPathExpression(PatchOp patchOp)
  {
    serviceProvider.getPatchConfig().setMsAzureFilterWorkaroundActive(true);

    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);
    User user = new User();

    final String path = "emails[type eq \"work\"].value";
    final String patchValue = "max@mustermann.de";

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .value(patchValue)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));

    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    Assertions.assertEquals(1, patchedUser.getEmails().size(), patchedUser.toPrettyString());
    Assertions.assertEquals("max@mustermann.de", patchedUser.getEmails().get(0).getValue().get());
    Assertions.assertEquals("work", patchedUser.getEmails().get(0).getType().get());
  }

  /**
   * the same as {@link #testMsAzureBehaviourForMultivaluedComplexTypesWithFilterInPathExpression(PatchOp)}
   * except that the ms-azure workaround is deactivated
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testMsAzureBehaviourForMultivaluedComplexTypesWithFilterInPathExpressionFail(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);
    User user = new User();

    final String path = "emails[type eq \"work\"].value";
    final String patchValue = "max@mustermann.de";

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .value(patchValue)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));

    BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                     () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    Assertions.assertEquals("No target found for path-filter 'emails[type eq \"work\"].value'", ex.getMessage());
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
  }

  /**
   * verifies that if the ms-azure patch filter workaround is active that the values of the filter are added if
   * the filter does not match an existing entry.<br>
   * <br>
   * Example:<br>
   * the request
   *
   * <pre>
   * {
   *   "schemas": [
   *     "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *   ],
   *   "Operations": [
   *     {
   *       "op": "add",
   *       "path": "emails[type eq \"work\"].value",
   *       "value": "max@mustermann.de"
   *     }
   *   ]
   * }
   * </pre>
   *
   * must result in
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *   "emails" : [
   *     {
   *       "value" : "erika@mustermann.de"
   *     },
   *     {
   *       "type" : "work",
   *       "value" : "max@mustermann.de"
   *     }
   *   ],
   *   "meta" : {
   *     "lastModified" : "2023-02-05T11:56:25.8049737+01:00"
   *   }
   * }
   * </pre>
   *
   * if the user did have the email "erika@mustermann.de" before
   */
  @Test
  public void testMsAzureBehaviourForMultivaluedComplexTypesWithFilterInPathExpression2Add()
  {
    PatchOp patchOp = PatchOp.ADD;
    serviceProvider.getPatchConfig().setMsAzureFilterWorkaroundActive(true);

    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    User user = User.builder().emails(Arrays.asList(Email.builder().value("erika@mustermann.de").build())).build();

    final String path = "emails[type eq \"work\"].value";
    final String patchValue = "max@mustermann.de";

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .value(patchValue)
                                                                                .build());

    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));

    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    Assertions.assertEquals(2, patchedUser.getEmails().size(), patchedUser.toPrettyString());
    Assertions.assertEquals("max@mustermann.de", patchedUser.getEmails().get(1).getValue().get());
    Assertions.assertEquals("work", patchedUser.getEmails().get(1).getType().get());
  }

  /**
   * the same as {@link #testMsAzureBehaviourForMultivaluedComplexTypesWithFilterInPathExpression2(PatchOp)}
   * except that the ms-azure workaround is deactivated
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testMsAzureBehaviourForMultivaluedComplexTypesWithFilterInPathExpression2Fail(PatchOp patchOp)
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    User user = User.builder().emails(Arrays.asList(Email.builder().value("erika@mustermann.de").build())).build();

    final String path = "emails[type eq \"work\"].value";
    final String patchValue = "max@mustermann.de";

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .value(patchValue)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));

    BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                     () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    Assertions.assertEquals("No target found for path-filter 'emails[type eq \"work\"].value'", ex.getMessage());
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
  }

  /**
   * verifies that if the ms-azure patch filter workaround is active that the values of the filter are added if
   * the filter does not match an existing entry.<br>
   * <br>
   * Example:<br>
   * the request
   *
   * <pre>
   * {
   *   "schemas": [
   *     "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *   ],
   *   "Operations": [
   *     {
   *       "op": "add",
   *       "path": "emails[primary eq true].value",
   *       "value": "max@mustermann.de"
   *     }
   *   ]
   * }
   * </pre>
   *
   * must result in
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *   "emails" : [
   *     {
   *       "value" : "erika@mustermann.de"
   *     },
   *     {
   *       "primary" : true,
   *       "value" : "max@mustermann.de"
   *     }
   *   ],
   *   "meta" : {
   *     "lastModified" : "2023-02-05T11:56:25.8049737+01:00"
   *   }
   * }
   * </pre>
   *
   * if the user did have the email "erika@mustermann.de" before
   */
  @Test
  public void testMsAzureBehaviourForMultivaluedComplexTypesWithFilterInPathExpression3Add()
  {
    PatchOp patchOp = PatchOp.ADD;
    serviceProvider.getPatchConfig().setMsAzureFilterWorkaroundActive(true);

    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    User user = User.builder().emails(Arrays.asList(Email.builder().value("erika@mustermann.de").build())).build();

    final String path = "emails[primary eq true].value";
    final String patchValue = "max@mustermann.de";

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .value(patchValue)
                                                                                .build());

    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    Assertions.assertEquals(2, patchedUser.getEmails().size(), patchedUser.toPrettyString());
    Assertions.assertEquals("max@mustermann.de", patchedUser.getEmails().get(1).getValue().get());
    Assertions.assertTrue(patchedUser.getEmails().get(1).isPrimary());
  }

  /**
   * this test simply shows that the default behaviour is maintained if a more complex filter expression is used
   * since we will support a simplex filter-expressions only for the ms-azure workaround
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testMsAzureBehaviourForMultivaluedComplexTypesWithFilterInPathExpression4Fail(PatchOp patchOp)
  {
    serviceProvider.getPatchConfig().setMsAzureFilterWorkaroundActive(true);

    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    User user = User.builder().emails(Arrays.asList(Email.builder().value("erika@mustermann.de").build())).build();

    final String path = "emails[type eq \"work\" and primary eq true].value";
    final String patchValue = "max@mustermann.de";

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .value(patchValue)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));

    BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                     () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    Assertions.assertEquals("No target found for path-filter 'emails[type eq \"work\" and primary eq true].value'",
                            ex.getMessage());
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
  }

  /**
   * this will test that PatchHandler.isChangedResource is true if any attribute is changed, and not just if the
   * last attributes is changed.
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testIsChangedResourceForExtensionValueInMsAzureAdStyle(PatchOp patchOp)
  {
    String employeeNumber = "1111";
    String employeeNumberChanged = "2222";
    String costCenter = "2222";

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setEnterpriseUser(EnterpriseUser.builder()
                                                   .employeeNumber(employeeNumber)
                                                   .costCenter(costCenter)
                                                   .build());

    // @formatter:off
    String valueNode = "{" +
      "  \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber\": \"" +
      employeeNumberChanged+ "\"," +
      "  \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter\": \"" +
      costCenter+ "\"" +
      "}";
    // @formatter:on

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    AllTypes allTypes = new AllTypes(true);
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));

    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    Assertions.assertEquals(employeeNumberChanged, patchedAllTypes.getEnterpriseUser().get().getEmployeeNumber().get());
    Assertions.assertEquals(costCenter, patchedAllTypes.getEnterpriseUser().get().getCostCenter().get());
  }

  /**
   * this test will make sure, that the extension is removed if a patch operation with an empty extension is
   * added as operation:
   *
   * <pre>
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "op": "replace",
   *             "value": ["{\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\": {}}"]
   *         }
   *     ]
   * }
   * </pre>
   */
  @Test
  public void testExtensionIsRemovedOnEmptyReplace()
  {
    String employeeNumber = "1111";
    String costCenter = "2222";

    AllTypes allTypesWithEnterpriseUser = new AllTypes(true);
    allTypesWithEnterpriseUser.setEnterpriseUser(EnterpriseUser.builder()
                                                               .employeeNumber(employeeNumber)
                                                               .costCenter(costCenter)
                                                               .build());

    AllTypes allTypesWithEmptyEnterpriseUser = new AllTypes(true);
    allTypesWithEmptyEnterpriseUser.setEnterpriseUser(EnterpriseUser.builder().build());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(allTypesWithEmptyEnterpriseUser.toString())
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypesWithEnterpriseUser);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypesWithEnterpriseUser.getId()
                                                                                                          .get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));

    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent());
  }

  @Test
  public void testPatchOfNonexistentUser()
  {
    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("active")
                                                                                .value("false")
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler("nonexistent-user-id",
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));

    ResourceNotFoundException ex = Assertions.assertThrows(ResourceNotFoundException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    Assertions.assertEquals("the 'User' resource with id 'nonexistent-user-id' does not exist", ex.getMessage());
    Assertions.assertFalse(patchRequestHandler.isResourceChanged());
  }

  /**
   * this test will make sure that the sailspoint workaround for complex types does correctly work.
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/327
   */
  @Test
  public void testHandleReplaceOnComplexTypesAsAdd()
  {
    serviceProvider.getPatchConfig().setSailsPointWorkaroundActive(true);

    AllTypes originalResource = new AllTypes(true);
    {
      originalResource.setString("hello world");
      originalResource.setNumber(2L);
      AllTypes complex = new AllTypes(false);
      complex.setString("test");
      complex.setNumber(3L);
      complex.setStringArray(Arrays.asList("test1", "test2"));
      originalResource.setComplex(complex);
    }

    AllTypes patchValue1 = new AllTypes(true);
    {
      AllTypes complex = new AllTypes(false);
      complex.setString("new value");
      patchValue1.setComplex(complex);
    }

    AllTypes patchValue2 = new AllTypes(true);
    {
      AllTypes complex = new AllTypes(false);
      complex.setNumber(999L);
      patchValue2.setComplex(complex);
    }

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(patchValue1.toString())
                                                                                .build(),
                                                           PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(patchValue2.toString())
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(originalResource);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(originalResource.getId().get(),
                                                                                allTypesResourceType.getResourceHandlerImpl(),
                                                                                resourceEndpoint.getPatchWorkarounds(),
                                                                                new Context(null));

    AllTypes patchedAllTypes = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
    Assertions.assertEquals(2L, patchedAllTypes.getNumber().get());
    Assertions.assertEquals(999L, patchedAllTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals("new value", patchedAllTypes.getComplex().get().getString().get());
    MatcherAssert.assertThat(patchedAllTypes.getComplex().get().getStringArray(),
                             Matchers.containsInAnyOrder("test1", "test2"));
  }

  /**
   * this patch request is based on the github-issue: https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/516.
   * MsAzure is building illegal patch-requests that prevents a correct resolving of the patch request.
   */
  private String getMsAzureSubValueAttributeTestString()
  {
    return " { \"schemas\" : [ \"urn:ietf:params:scim:api:messages:2.0:PatchOp\" ], "//
           + "\"Operations\" : [ "//
           + "   { \"path\" : \"roles\", "//
           + "       \"op\" : \"add\", "//
           + "    \"value\" : [ " //
           + "                  {" //
           + "                    \"value\": \"{\\\"$ref\\\":\\\"827f0d2e-be15-4d8f-a8e3-f7697239c112\\\","
           + "                                  \\\"value\\\":\\\"DocumentMgmt-BuyerAdmin\\\","
           + "                                  \\\"display\\\":\\\"DocumentMgmt BuyerAdmin\\\""
           + "                                 }\"" //
           + "                  },"//
           + "                  {" //
           + "                    \"value\": \"{\\\"$ref\\\":\\\"8ae06bd4-35bb-4fcd-977e-12e074ad1192\\\","
           + "                                  \\\"value\\\":\\\"Buyer-Admin\\\","
           + "                                  \\\"display\\\":\\\"Buyer Admin\\\""
           + "                                  }\"" //
           + "                  }"//
           + "                ]"//
           + "  } "//
           + "]}";
  }

  /**
   * This test makes sure that the illegal MsAzure Patch-Requests with the value sub-attribute object structure
   * is resolved correctly if the feature is activated
   *
   * @see #getMsAzureSubValueAttributeTestString()
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/516
   */
  @DisplayName("MsAzure value-subAttribute workaround is active and resolves correctly")
  @Test
  public void testMsAzureSubValueAttributeResolvingWithWorkaroundActive()
  {
    serviceProvider.getPatchConfig().setMsAzureValueSubAttributeWorkaroundActive(true);

    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    final String resourceId = UUID.randomUUID().toString();
    final String patchRequestString = getMsAzureSubValueAttributeTestString();
    PatchOpRequest patchOpRequest = JsonHelper.readJsonDocument(patchRequestString, PatchOpRequest.class);
    User user = User.builder().id(resourceId).userName("goldfish").build();

    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));
    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    List<PersonRole> personRoles = patchedUser.getRoles();
    Assertions.assertEquals(2, personRoles.size());

    {
      PersonRole role1 = personRoles.get(0);
      Assertions.assertFalse(role1.getRef().isPresent(), "this attribute is not defined in the schema");
      Assertions.assertEquals("DocumentMgmt-BuyerAdmin", role1.getValue().get());
      Assertions.assertEquals("DocumentMgmt BuyerAdmin", role1.getDisplay().get());
    }

    {
      PersonRole role2 = personRoles.get(1);
      Assertions.assertFalse(role2.getRef().isPresent(), "this attribute is not defined in the schema");
      Assertions.assertEquals("Buyer-Admin", role2.getValue().get());
      Assertions.assertEquals("Buyer Admin", role2.getDisplay().get());
    }
  }

  /**
   * This test makes sure that the illegal MsAzure Patch-Requests with the value sub-attribute object structure
   * is not resolved anymore if the feature is deactivated
   *
   * @see #getMsAzureSubValueAttributeTestString()
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/516
   */
  @DisplayName("MsAzure value-subAttribute workaround is inactive and resolves correctly")
  @Test
  public void testMsAzureSubValueAttributeResolvingWithWorkaroundInActive()
  {
    serviceProvider.getPatchConfig().setMsAzureValueSubAttributeWorkaroundActive(false);

    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    UserEndpointDefinition endpointDefinition = new UserEndpointDefinition(userHandler);
    ResourceType userResourceType = resourceEndpoint.registerEndpoint(endpointDefinition);

    final String resourceId = UUID.randomUUID().toString();
    final String patchRequestString = getMsAzureSubValueAttributeTestString();
    PatchOpRequest patchOpRequest = JsonHelper.readJsonDocument(patchRequestString, PatchOpRequest.class);
    User user = User.builder().id(resourceId).userName("goldfish").build();

    addUserToProvider(userHandler, user);
    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler(user.getId().get(),
                                                                            userResourceType.getResourceHandlerImpl(),
                                                                            resourceEndpoint.getPatchWorkarounds(),
                                                                            new Context(null));

    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());

    List<PersonRole> personRoles = patchedUser.getRoles();
    Assertions.assertEquals(2, personRoles.size());

    List<String> values = patchOpRequest.getOperations().get(0).getValues();

    {
      PersonRole role1 = personRoles.get(0);
      String expectedContent = JsonHelper.readJsonDocument(values.get(0), ObjectNode.class)
                                         .get(AttributeNames.RFC7643.VALUE)
                                         .textValue();
      Assertions.assertEquals(expectedContent, role1.getValue().get());
      Assertions.assertFalse(role1.getRef().isPresent());
      Assertions.assertFalse(role1.getDisplay().isPresent());
    }

    {
      PersonRole role2 = personRoles.get(1);
      String expectedContent = JsonHelper.readJsonDocument(values.get(1), ObjectNode.class)
                                         .get(AttributeNames.RFC7643.VALUE)
                                         .textValue();
      Assertions.assertEquals(expectedContent, role2.getValue().get());
      Assertions.assertFalse(role2.getRef().isPresent());
      Assertions.assertFalse(role2.getDisplay().isPresent());
    }
  }

  /**
   * this method returns a specific attribute definition that will be added to the enterprise user that is used
   * as extension for the alltypes schema. this shall provoke a naming conflict with a complex type in the
   * extension
   */
  private String getComplexNodeDefinitionForTest()
  {
    // @formatter:off
    return "{\n" +
             "  \"name\": \"complex\",\n" +
             "  \"type\": \"complex\",\n" +
             "  \"mutability\": \"readWrite\",\n" +
             "  \"returned\": \"default\",\n" +
             "  \"uniqueness\": \"none\",\n" +
             "  \"description\": \"test\",\n" +
             "  \"multiValued\": false,\n" +
             "  \"required\": false,\n" +
             "  \"caseExact\": false,\n" +
             "  \"subAttributes\": [\n" +
             "    {\n" +
             "      \"name\": \"number\",\n" +
             "      \"type\": \"string\",\n" +
             "      \"mutability\": \"readWrite\",\n" +
             "      \"returned\": \"default\",\n" +
             "      \"uniqueness\": \"none\",\n" +
             "      \"description\": \"test\",\n" +
             "      \"multiValued\": false,\n" +
             "      \"required\": false,\n" +
             "      \"caseExact\": false\n" +
             "    }\n" +
             "  ]\n" +
             "}";
    // @formatter:on
  }

  /**
   * used for easier creation of dynamic tests
   */
  @AllArgsConstructor
  @Getter
  public static class NameValuePair
  {

    private String attributeName;

    private JsonNode value;
  }
}
