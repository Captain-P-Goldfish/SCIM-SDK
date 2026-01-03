package de.captaingoldfish.scim.sdk.client.builder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.tree.GenericTree;
import de.captaingoldfish.scim.sdk.common.tree.TreeNode;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 08.03.2020 <br>
 * <br>
 */
@Slf4j
public class BulkBuilder extends RequestBuilder<BulkResponse>
{

  /**
   * the builder object to build the bulk request
   */
  private final BulkRequest.BulkRequestBuilder builder;

  /**
   * the bulk request operations that should be executed
   */
  @Getter
  private final List<BulkRequestOperation> bulkRequestOperationList;

  /**
   * a thread safe-map that holds the request operations references. This can be used by client implementations
   * to compare the request with the returned response operations. This might be useful to write log-messages
   * based on the requests content.
   */
  private final Map<String, BulkRequestOperation> bulkRequestOperationMap;

  /**
   * the fully qualified url to the required resource
   */
  private final String fullUrl;

  /**
   * contains the configuration of the service provider that is used to determine the max-operations of a bulk
   * request and to help to split the operations into several requests if necessary. <br>
   * This object might be null
   */
  @Setter
  private Supplier<ServiceProvider> serviceProviderSupplier;

  /**
   * if the resource should be retrieved by using the fully qualified url
   *
   * @param baseUrl the fully qualified url to the required resource
   * @param scimHttpClient the http client instance
   * @param isFullUrl if the given base url is the fully qualified url or not
   * @param serviceProviderSupplier contains the configuration of the service provider that is used to determine
   *          the max-operations of a bulk request and to help to split the operations into several requests if
   *          necessary. <br>
   *          This object might be null
   */
  public BulkBuilder(String baseUrl,
                     ScimHttpClient scimHttpClient,
                     boolean isFullUrl,
                     Supplier<ServiceProvider> serviceProviderSupplier)
  {
    super(isFullUrl ? null : baseUrl, EndpointPaths.BULK, BulkResponse.class, scimHttpClient);

    builder = BulkRequest.builder();
    bulkRequestOperationList = Collections.synchronizedList(new ArrayList<>());
    bulkRequestOperationMap = new ConcurrentHashMap<>();
    builder.bulkRequestOperation(bulkRequestOperationList);
    this.fullUrl = isFullUrl ? baseUrl : null;
    this.serviceProviderSupplier = Optional.ofNullable(serviceProviderSupplier).orElse(() -> null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BulkBuilder setExpectedResponseHeaders(Map<String, String> requiredResponseHeaders)
  {
    return (BulkBuilder)super.setExpectedResponseHeaders(requiredResponseHeaders);
  }

  /**
   * retrieves a request operation from the builder by its bulkId. Modifying the returned operation will also
   * modify the request
   *
   * @param bulkId the bulkId of the operation that should be returned
   * @return the request operation with the matching bulkId
   */
  public BulkRequestOperation getOperationByBulkId(String bulkId)
  {
    return bulkRequestOperationMap.get(bulkId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isExpectedResponseCode(int httpStatus)
  {
    return HttpStatus.OK == httpStatus;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected HttpUriRequest getHttpUriRequest()
  {
    HttpPost httpPost;
    if (StringUtils.isBlank(fullUrl))
    {
      httpPost = new HttpPost(getBaseUrl() + getEndpoint());
    }
    else
    {
      httpPost = new HttpPost(fullUrl);
    }
    StringEntity stringEntity = new StringEntity(getResource(), StandardCharsets.UTF_8);
    httpPost.setEntity(stringEntity);
    return httpPost;
  }

  /**
   * overrides the default method from the superclass to have easier control of the resource that will be put
   * into the request body
   */
  @Override
  public String getResource()
  {
    return builder.build().toString();
  }

  /**
   * checks if the response contains a schema-uri that matches the value of
   * {@link de.captaingoldfish.scim.sdk.common.constants.SchemaUris#BULK_RESPONSE_URI}
   */
  @Override
  protected Function<HttpResponse, Boolean> isResponseParseable()
  {
    return httpResponse -> {
      String responseBody = httpResponse.getResponseBody();
      if (StringUtils.isNotBlank(responseBody) && responseBody.contains(SchemaUris.BULK_RESPONSE_URI))
      {
        return true;
      }
      return false;
    };
  }

  /**
   * sets how many errors are allowed on the server side before the request should be rolled back
   *
   * @param failOnErrors the number of errors that are accepted on the server side
   */
  public BulkBuilder failOnErrors(Integer failOnErrors)
  {
    builder.failOnErrors(failOnErrors);
    return this;
  }

  /**
   * sets the path to the resource endpoint e.g. "/Users" or "/Groups"
   */
  public BulkRequestOperationCreator bulkRequestOperation(String path)
  {
    return bulkRequestOperation(path, null);
  }

  /**
   * sets the path to the resource endpoint e.g. "/Users" or "/Groups"
   *
   * @param path "/Users", "/Groups" or any other registered resource path
   * @param id the id of an existing resource in case of patch, update or delete
   */
  public BulkRequestOperationCreator bulkRequestOperation(String path, String id)
  {
    String idPath = StringUtils.isBlank(id) ? "" : "/" + id;
    return new BulkRequestOperationCreator(this, path + idPath);
  }

  /**
   * adds the given list of operations
   */
  public BulkBuilder addOperations(List<BulkRequestOperation> requestOperations)
  {
    for ( BulkRequestOperation requestOperation : requestOperations )
    {
      if (!requestOperation.getBulkId().isPresent())
      {
        requestOperation.setBulkId(UUID.randomUUID().toString());
      }
      bulkRequestOperationMap.put(requestOperation.getBulkId().get(), requestOperation);
    }
    bulkRequestOperationList.addAll(requestOperations);
    return this;
  }

  /**
   * send the request to the server
   *
   * @param runSplittedRequestsParallel if the requests should be run parallel. This is only recommended if no
   *          relations between the different bulk-request-operations are set. So if no bulkId-references are
   *          set. Otherwise, the relation between these requests might break
   * @return the response from the server
   */
  public ServerResponse<BulkResponse> sendRequest(boolean runSplittedRequestsParallel)
  {
    return sendRequestWithMultiHeaders(Collections.emptyMap(), null, runSplittedRequestsParallel);
  }

  /**
   * send the request to the server
   *
   * @param responseHandler a helper method to will allow the client to react to each individual response. This
   *          makes only sense if the feature {@link ScimClientConfig#isEnableAutomaticBulkRequestSplitting()}
   *          is enabled
   * @return the response from the server
   */
  public ServerResponse<BulkResponse> sendRequest(Consumer<ServerResponse<BulkResponse>> responseHandler)
  {
    return sendRequestWithMultiHeaders(Collections.emptyMap(), responseHandler, false);
  }

  /**
   * send the request to the server
   *
   * @param responseHandler a helper method to will allow the client to react to each individual response. This
   *          makes only sense if the feature {@link ScimClientConfig#isEnableAutomaticBulkRequestSplitting()}
   *          is enabled
   * @param runSplittedRequestsParallel if the requests should be run parallel. This is only recommended if no
   *          relations between the different bulk-request-operations are set. So if no bulkId-references are
   *          set. Otherwise, the relation between these requests might break
   * @return the response from the server
   */
  public ServerResponse<BulkResponse> sendRequest(Consumer<ServerResponse<BulkResponse>> responseHandler,
                                                  boolean runSplittedRequestsParallel)
  {
    return sendRequestWithMultiHeaders(Collections.emptyMap(), responseHandler, runSplittedRequestsParallel);
  }

  /**
   * send the request to the server
   *
   * @param headers the http headers to send additionally to the default headery within the request
   * @param responseHandler a helper method to will allow the client to react to each individual response. This
   *          makes only sense if the feature {@link ScimClientConfig#isEnableAutomaticBulkRequestSplitting()}
   *          is enabled
   * @return the response from the server
   */
  public ServerResponse<BulkResponse> sendRequest(Map<String, String> headers,
                                                  Consumer<ServerResponse<BulkResponse>> responseHandler)
  {
    return sendRequest(headers, responseHandler, false);
  }

  /**
   * send the request to the server
   *
   * @param headers the http headers to send additionally to the default headery within the request
   * @param responseHandler a helper method to will allow the client to react to each individual response. This
   *          makes only sense if the feature {@link ScimClientConfig#isEnableAutomaticBulkRequestSplitting()}
   *          is enabled
   * @param runSplittedRequestsParallel if the requests should be run parallel. This is only recommended if no
   *          relations between the different bulk-request-operations are set. So if no bulkId-references are
   *          set. Otherwise, the relation between these requests might break
   * @return the response from the server
   */
  public ServerResponse<BulkResponse> sendRequest(Map<String, String> headers,
                                                  Consumer<ServerResponse<BulkResponse>> responseHandler,
                                                  boolean runSplittedRequestsParallel)
  {
    Map<String, String[]> multiHeader = new HashMap<>();
    headers.forEach((key, value) -> multiHeader.put(key, new String[]{value}));
    return sendRequestWithMultiHeaders(multiHeader, responseHandler, runSplittedRequestsParallel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <R extends ServerResponse<BulkResponse>> R sendRequestWithMultiHeaders(Map<String, String[]> httpHeaders)
  {
    return (R)sendRequestWithMultiHeaders(httpHeaders, null, false);
  }

  /**
   * {@inheritDoc}
   *
   * @param runSplittedRequestsParallel if the requests should be run parallel. This is only recommended if no
   *          relations between the different bulk-request-operations are set. So if no bulkId-references are
   *          set. Otherwise, the relation between these requests might break
   */
  public ServerResponse<BulkResponse> sendRequestWithMultiHeaders(Map<String, String[]> httpHeaders,
                                                                  boolean runSplittedRequestsParallel)
  {
    return sendRequestWithMultiHeaders(httpHeaders, null, runSplittedRequestsParallel);
  }

  /**
   * {@inheritDoc}
   *
   * @param responseHandler a helper method to will allow the client to react to each individual response. This
   *          makes only sense if the feature {@link ScimClientConfig#isEnableAutomaticBulkRequestSplitting()}
   *          is enabled
   */
  public ServerResponse<BulkResponse> sendRequestWithMultiHeaders(Map<String, String[]> httpHeaders,
                                                                  Consumer<ServerResponse<BulkResponse>> responseHandler)
  {
    return sendRequestWithMultiHeaders(httpHeaders, responseHandler, false);
  }

  /**
   * {@inheritDoc}
   *
   * @param responseHandler a helper method to will allow the client to react to each individual response. This
   *          makes only sense if the feature {@link ScimClientConfig#isEnableAutomaticBulkRequestSplitting()}
   *          is enabled
   * @param runSplittedRequestsParallel if the requests should be run parallel. This is only recommended if no
   *          relations between the different bulk-request-operations are set. So if no bulkId-references are
   *          set. Otherwise, the relation between these requests might break
   */
  public ServerResponse<BulkResponse> sendRequestWithMultiHeaders(Map<String, String[]> httpHeaders,
                                                                  Consumer<ServerResponse<BulkResponse>> responseHandler,
                                                                  boolean runSplittedRequestsParallel)
  {
    if (bulkRequestOperationList.isEmpty())
    {
      throw new IllegalStateException("Cannot send bulk-request without any operations");
    }
    final int maxNumberOfOperationns = getMaxNumberOfOperations();
    final boolean isSplittingFeatureDisabled = !getScimHttpClient().getScimClientConfig()
                                                                   .isEnableAutomaticBulkRequestSplitting();
    final boolean fitsIntoASingleRequest = bulkRequestOperationList.size() <= maxNumberOfOperationns;
    if (isSplittingFeatureDisabled || fitsIntoASingleRequest)
    {
      return super.sendRequestWithMultiHeaders(httpHeaders);
    }
    return sendMultipleBulkRequests(httpHeaders, responseHandler, runSplittedRequestsParallel);
  }

  /**
   * retrieves the current maximum number of operations allowed at the bulk endpoint
   */
  private int getMaxNumberOfOperations()
  {
    int maxNumberOfOperations = Optional.ofNullable(serviceProviderSupplier.get())
                                        .map(ServiceProvider::getBulkConfig)
                                        .map(BulkConfig::getMaxOperations)
                                        .orElse(Integer.MAX_VALUE);
    if (maxNumberOfOperations <= 0)
    {
      throw new IllegalStateException("Maximum number of operations must not be 0 or smaller.");
    }
    return maxNumberOfOperations;
  }

  /**
   * splits the currently created bulk-request into several requests and tries to sort them based on their
   * dependencies of other resources. Afterwards all responses will be put together into a single response
   * object.
   *
   * @param httpHeaders allows the user to add additional http headers to the request
   * @param responseHandler a helper method to will allow the client to react to each individual response
   * @param runSplittedRequestsParallel if the requests should be run parallel. This is only recommended if no
   *          relations between the different bulk-request-operations are set. So if no bulkId-references are
   *          set. Otherwise, the relation between these requests might break
   * @return a composed response object that results from several responses of the different requests
   */
  @SneakyThrows
  private ServerResponse<BulkResponse> sendMultipleBulkRequests(Map<String, String[]> httpHeaders,
                                                                Consumer<ServerResponse<BulkResponse>> responseHandler,
                                                                boolean runSplittedRequestsParallel)
  {
    boolean containsBulkIdReferences = getResource().contains(String.format("\"%s:", AttributeNames.RFC7643.BULK_ID));

    BulkRequestIdResolverWrapper bulkRequestIdResolverWrapper;
    if (containsBulkIdReferences)
    {
      // sort operations and split with relations preserved
      bulkRequestIdResolverWrapper = splitRequestsWithRelationOrderPreserved();
    }
    else
    {
      // simply split the requests
      List<List<BulkRequestOperation>> bulkRequestOperationRequestList = splitRequestsSimple(bulkRequestOperationList);
      bulkRequestIdResolverWrapper = new BulkRequestIdResolverWrapper(bulkRequestOperationRequestList, new HashMap<>());
    }


    List<BulkResponseOperation> synchronizedResponseOperations = Collections.synchronizedList(new ArrayList<>());

    List<List<BulkRequestOperation>> bulkRequestOperationsList = bulkRequestIdResolverWrapper.getRequestsList();
    // @formatter:off
    ArrayBlockingQueue<ServerResponse<BulkResponse>> serverResponseList =
                                                          new ArrayBlockingQueue<>(bulkRequestOperationsList.size());
    // @formatter:on

    ServiceProvider serviceProvider = serviceProviderSupplier.get();
    ScimClientConfig scimClientConfig = getScimHttpClient().getScimClientConfig();

    IntStream bulkOperationIndexStream = IntStream.range(0, bulkRequestOperationsList.size());

    Function<Runnable, ForkJoinTask> runInPool = runnable -> {
      serviceProvider.getThreadPool().awaitQuiescence(scimClientConfig.getSocketTimeout(), TimeUnit.SECONDS);
      return serviceProvider.getThreadPool().submit(runnable);
    };

    Runnable runnable = () -> {

      IntStream indexStream = runSplittedRequestsParallel ? bulkOperationIndexStream.parallel()
        : bulkOperationIndexStream;

      indexStream.forEach(index -> {
        List<BulkRequestOperation> bulkRequestOperations = bulkRequestOperationsList.get(index);

        if (log.isDebugEnabled())
        {
          log.debug("Handling bulk request '{}' of '{}' with '{}' operations.",
                    index + 1,
                    bulkRequestIdResolverWrapper.getRequestsList().size(),
                    bulkRequestOperations.size());
        }
        boolean isFullUrl = getBaseUrl() == null;
        replaceBulkRequestOperations(bulkRequestOperations, bulkRequestIdResolverWrapper);
        BulkBuilder splitBulkBuilder = new BulkBuilder(getBaseUrl(), getScimHttpClient(), isFullUrl,
                                                       serviceProviderSupplier);
        Integer failOnErrors = builder.getFailOnErrors();
        splitBulkBuilder.failOnErrors(failOnErrors).addOperations(bulkRequestOperations);
        // the request in the super-class is created from the builder, so we need to replace the original here. and
        // afterwards we are changing it back to restore the original state
        ServerResponse<BulkResponse> response = splitBulkBuilder.sendRequestWithMultiHeaders(httpHeaders);

        if (log.isDebugEnabled())
        {
          log.debug("Received response for bulk request '{}' of '{}'.",
                    index + 1,
                    bulkRequestIdResolverWrapper.getRequestsList().size());
        }

        validateResponseAndResolveResults(bulkRequestOperations,
                                          bulkRequestIdResolverWrapper,
                                          response,
                                          synchronizedResponseOperations);

        Optional.ofNullable(responseHandler).ifPresent(handler -> handler.accept(response));

        serverResponseList.add(response);
      });
    };

    runInPool.apply(runnable).get();

    log.debug("Finished handling all bulk requests. The requests will be merged and returned in a single "
              + "response-object");

    BulkResponse compositeBulkResponse = new BulkResponse();
    compositeBulkResponse.setBulkResponseOperations(synchronizedResponseOperations);

    // validate responses and also content of responses
    // if no error occurred until now everything is fine and all operations completed successfully
    HttpResponse httpResponse = HttpResponse.builder()
                                            .httpStatusCode(HttpStatus.OK)
                                            .responseBody(compositeBulkResponse.toString())
                                            // take the response headers from any request they should be the same
                                            .responseHeaders(Optional.ofNullable(serverResponseList.peek())
                                                                     .map(ServerResponse::getHttpHeaders)
                                                                     .orElse(Collections.emptyMap()))
                                            .build();
    return new ServerResponse<>(httpResponse, true, BulkResponse.class, isResponseParseable(),
                                getRequiredResponseHeaders());
  }

  /**
   * replaces the bulkId references with the children ids from previous requests
   *
   * @param bulkRequestOperations the current operations that should be executed next
   * @param bulkRequestIdResolverWrapper this object contains the results from previous requests that need to be
   *          added into the new structure
   */
  private void replaceBulkRequestOperations(List<BulkRequestOperation> bulkRequestOperations,
                                            BulkRequestIdResolverWrapper bulkRequestIdResolverWrapper)
  {
    for ( int i = 0 ; i < bulkRequestOperations.size() ; i++ )
    {
      BulkRequestOperation bulkRequestOperation = bulkRequestOperations.get(i);
      String operationString = bulkRequestOperation.toString();
      final String bulkId = bulkRequestOperation.getBulkId().orElse(null);
      if (bulkId == null)
      {
        throw new IllegalStateException("Cannot use auto-splitting feature for bulk requests if the bulkId "
                                        + "elements are missing. Please assign a bulkId to each single operation!");
      }
      List<BulkRequestOperation> childOperationList = bulkRequestIdResolverWrapper.getParentChildRelationMap()
                                                                                  .get(bulkId);
      if (childOperationList == null)
      {
        break;
      }
      for ( BulkRequestOperation childOperation : childOperationList )
      {
        final String childBulkId = childOperation.getBulkId().get();
        final String childResourceId = bulkRequestIdResolverWrapper.getResolvedBulkIds().get(childBulkId);

        final String oldReference = String.format("\"%s:%s\"", AttributeNames.RFC7643.BULK_ID, childBulkId);
        final String newReference = String.format("\"%s\"", childResourceId);
        operationString = operationString.replaceAll(oldReference, newReference);
      }

      bulkRequestOperations.remove(i);
      BulkRequestOperation newOperation = JsonHelper.readJsonDocument(operationString, BulkRequestOperation.class);
      bulkRequestOperations.add(i, newOperation);
    }
  }

  /**
   * validates the response from the server and resolves the necessary for upcoming secondary requests
   *
   * @param bulkRequestOperations the list of operations that were just executed
   * @param bulkRequestIdResolverWrapper the wrapper object that shall be extended by the created ids of
   *          previous requests
   * @param response the response from the server
   * @param responseOperations a composition of response-operations. Since we are sending several requests we
   *          will gather all response-operations in a single BulkResponse that will be returned
   */
  private void validateResponseAndResolveResults(List<BulkRequestOperation> bulkRequestOperations,
                                                 BulkRequestIdResolverWrapper bulkRequestIdResolverWrapper,
                                                 ServerResponse<BulkResponse> response,
                                                 List<BulkResponseOperation> responseOperations)
  {
    if (!response.isSuccess())
    {
      log.error("Bulk error on automatically splitted requests. Please note that this might cause unwanted results "
                + "on the server that need to be fixed manually. The following log messages shall help identifying "
                + "the problem:");
      log.error("The following request operations were not successful: \n{}",
                bulkRequestOperations.stream().map(ScimObjectNode::toPrettyString).collect(Collectors.joining("\n")));
      log.error("Response from the server: {}",
                Optional.ofNullable(response.getErrorResponse())
                        .map(ErrorResponse::toPrettyString)
                        .orElseGet(response::getResponseBody));
      final int indexOfFailedRequest = bulkRequestIdResolverWrapper.getRequestsList().indexOf(bulkRequestOperations);
      if (indexOfFailedRequest > 0)
      {
        String successOperations = bulkRequestIdResolverWrapper.getRequestsList()
                                                               .subList(0, indexOfFailedRequest)
                                                               .stream()
                                                               .flatMap(Collection::stream)
                                                               .map(ScimObjectNode::toPrettyString)
                                                               .collect(Collectors.joining("\n"));
        log.error("The following operations were executed successfully on the server and were persisted: \n{}",
                  successOperations);
      }

      throw new IllegalStateException(String.format("The bulk request failed with status: %s and message: %s",
                                                    response.getHttpStatus(),
                                                    response.getResponseBody()));
    }

    BulkResponse bulkResponse = response.getResource();
    for ( BulkResponseOperation bulkResponseOperation : bulkResponse.getBulkResponseOperations() )
    {
      final String bulkId = bulkResponseOperation.getBulkId().orElseThrow(() -> {
        return new IllegalStateException("Missing bulkId in response cannot resolve relations of split operations.");
      });
      final String resourceId = bulkResponseOperation.getResourceId().orElseGet(() -> {
        return getIdFromLocationAttribute(bulkResponseOperation);
      });
      // if the resourceId is null the operation did fail
      if (resourceId != null)
      {
        bulkRequestIdResolverWrapper.getResolvedBulkIds().put(bulkId, resourceId);
      }
      responseOperations.add(bulkResponseOperation);
    }
  }

  /**
   * extracted into its own method for unit tests.
   */
  protected String getIdFromLocationAttribute(BulkResponseOperation bulkResponseOperation)
  {
    String[] locationParts = bulkResponseOperation.getLocation().map(s -> s.split("/")).orElse(null);
    if (locationParts == null)
    {
      return null;
    }
    return locationParts[locationParts.length - 1];
  }

  /**
   * splits the list of operations into several lists with the relation order intact. This means that requests
   * containing bulkId relations will be put together. If this is not possible because the resulting list is
   * still too large we will try to send a first request in order to resolve the parent-child relationship step
   * by step.<br>
   * <br>
   * the method code is build with the following tree in mind and the service provider having a maxOperations
   * value. The relationships are based on the bulkId references meaning that node 488 has a value-field with a
   * string representation of "bulkId:111" and another one with "bulkId:523"
   *
   * <pre>
   *     ServiceProviderConfig.BulkConfig.maxOperations = 5
   *
   *     (936)              (619)              (219)              (333)              (987)              (222)
   *       |                                     |                  |
   *       |                                     |                  |
   *     (123)                                  / \                 |
   *       |  \                                /   \               /|
   *       |   \                              /     \             / |
   *       |    ---------- (443)--------------       - (488) -----  |
   *       |                                             |          |
   *       |                                             |          |
   *       |                                            / \         |
   *     (582)                                         /   \        |
   *                                                  /     \       |
   *                                                 /       \      |
   *                                               (523)      |     |
   *                                                 |        |     |
   *                                                  \       |     |
   *                                                   \      |     |
   *                                                     -- (111) --
   * </pre>
   *
   * unparented leaf-nodes are (619), (987) and (222). These nodes can be isolated and do not need any specific
   * attention, so we store the operations in a separate list and remove them from the tree. <br>
   * Then we will isolate the leaf-nodes which are (582), (443) and (111). We will create a {@link Map} element
   * that uses the bulkIds of the roots as keys and stores their children under the key. Afterwards the leafs
   * will be deleted from the tree and so parent-child relationship will no longer be represented by this tree
   * but by the created {@link Map} <br>
   * Now we are doing this again until the number of maximum operations is reached. We will create a first list
   * of operations. In the end we will return a wrapper object with two objects. First a two-dimensional list
   * that contains the operations in the order they should be executed and the number of lists represents the
   * number of requests that will be sent to the server. The second object is the {@link Map} that represents
   * the parent-child relations, so that we can replace bulkId-references after each request within the map.
   */
  private BulkRequestIdResolverWrapper splitRequestsWithRelationOrderPreserved()
  {
    final int maxNumberOfOperationns = getMaxNumberOfOperations();

    // determine parent-child relations. We need this to build an operation-relation-chain
    GenericTree<BulkRequestOperation> childParentRelationsTree = getParentChildRelationsOfRequest();


    List<BulkRequestOperation> unparentedOperations = extractUnparentedOperationsFromTree(childParentRelationsTree);
    // now we removed the unparented operations. In the tree-example above this would be (619), (987) and (222)

    // this nested list will represent the requests that will eventually be sent to the server. Each list
    // represents a single request
    List<List<BulkRequestOperation>> requestLists = new ArrayList<>();

    // the children of any node will be put with its bulkId into this map
    Map<String, List<BulkRequestOperation>> parentChildRelationMap = new HashMap<>();
    for ( TreeNode<BulkRequestOperation> treeNode : childParentRelationsTree.getAllNodes() )
    {
      if (treeNode.isLeaf())
      {
        continue;
      }
      parentChildRelationMap.put(treeNode.getValue().getBulkId().get(),
                                 treeNode.getChildren().stream().map(TreeNode::getValue).collect(Collectors.toList()));
    }

    // now iterate for as long as the tree has still nodes left
    while (childParentRelationsTree.hasNodes())
    {
      List<BulkRequestOperation> operationList = new ArrayList<>();

      // iterate for as long as the tree has nodes left and the operation-list is not full
      while (operationList.size() != maxNumberOfOperationns && childParentRelationsTree.hasNodes())
      {
        // iterate over the tree-leafs and enter them into the operations list until the list matches its maximum
        // number of operations
        for ( TreeNode<BulkRequestOperation> leaf : childParentRelationsTree.getLeafs() )
        {
          childParentRelationsTree.removeNodeFromTree(leaf);
          operationList.add(leaf.getValue());
          if (operationList.size() == maxNumberOfOperationns)
          {
            break;
          }
        }
        // if no nodes are left anymore we are finished. This will end both loops inner and outer
        if (!childParentRelationsTree.hasNodes())
        {
          requestLists.add(operationList);
          break;
        }
        // if the operations-list is not full yet, we go into the next iteration and add more operations to the list
        boolean areMoreOperationsAvailable = operationList.size() < maxNumberOfOperationns;
        if (areMoreOperationsAvailable)
        {
          continue;
        }
        // this case represents the case that the operation list is full so we will go into the next iteration phase
        // of the outer-loop, so add the operations-list to the requests-list.
        requestLists.add(operationList);
      }
    }

    /*
     * taking the example tree above when iterating from left to right we will get the following result:
     *
     * // @formatter:off
     * unparentedOperations:
     *   [(619), (987), (222)]
     *
     * requestLists:
     *   [0] (582), (443), (111), (123), (523)
     *   [1] (936), (488), (219), (333)
     * // @formatter:on
     *
     * as last operation we need to merge the unparented operations at the end of the list to get the following result:
     *
     * // @formatter:off
     * requestLists:
     *   [0] (582), (443), (111), (123), (523)
     *   [1] (936), (488), (219), (333), (619)
     *   [2] (987), (222)
     * // @formatter:on
     */

    List<BulkRequestOperation> lastParentedList = requestLists.get(requestLists.size() - 1);
    // fill the last list with the unparented operations list
    if (lastParentedList.size() < maxNumberOfOperationns)
    {
      while (!unparentedOperations.isEmpty() && lastParentedList.size() < maxNumberOfOperationns)
      {
        BulkRequestOperation bulkRequestOperation = unparentedOperations.get(0);
        unparentedOperations.remove(0);
        lastParentedList.add(bulkRequestOperation);
      }
    }
    // if the unparented operationslist is still not empty we need to add additional lists to the requests-list
    if (!unparentedOperations.isEmpty())
    {
      List<List<BulkRequestOperation>> splittedLists = splitRequestsSimple(unparentedOperations);
      requestLists.addAll(splittedLists);
    }

    return new BulkRequestIdResolverWrapper(requestLists, parentChildRelationMap);
  }

  /**
   * gets all nodes from the tree that are leafs and roots and the same time and removes them from the tree
   */
  private List<BulkRequestOperation> extractUnparentedOperationsFromTree(GenericTree<BulkRequestOperation> childParentRelationsTree)
  {
    List<BulkRequestOperation> unparentedOperations = new ArrayList<>();
    // first isolate all operations that do not have parents and thus do not have other operations referenced
    for ( TreeNode<BulkRequestOperation> leaf : childParentRelationsTree.getLeafs() )
    {
      boolean isNodeWithoutRelationsships = leaf.isLeaf() && leaf.isRoot();
      if (isNodeWithoutRelationsships)
      {
        unparentedOperations.add(leaf.getValue());
        childParentRelationsTree.removeNodeFromTree(leaf);
      }
    }
    return unparentedOperations;
  }

  /**
   * this method tries its best to identify parent-child relations in the request and will place them in a map
   * based on the child-entry so that we can identify the parents of the operation. For example if one operation
   * has several bulkId references set within the code each reference is expected to be a parent of the current
   * operation because this operation relies on the existence of the other resources.
   *
   * @return a multi-parent-tree representation of the child-parent-relationships within the request
   */
  private GenericTree<BulkRequestOperation> getParentChildRelationsOfRequest()
  {
    final String regex = String.format("\"%s:(.*?)\"", AttributeNames.RFC7643.BULK_ID);
    Pattern bulkIdPattern = Pattern.compile(regex);

    GenericTree<BulkRequestOperation> childParentRelations = new GenericTree<>();

    for ( BulkRequestOperation bulkRequestOperation : getBulkRequestOperationList() )
    {
      final String currentResource = bulkRequestOperation.toString();
      Matcher bulkIdMatcher = bulkIdPattern.matcher(currentResource);

      TreeNode<BulkRequestOperation> parentNode = childParentRelations.addDistinctNode(bulkRequestOperation);
      while (bulkIdMatcher.find())
      {
        final String bulkId = bulkIdMatcher.group(1);
        BulkRequestOperation operation = bulkRequestOperationList.stream()
                                                                 .filter(op -> bulkId.equals(op.getBulkId()
                                                                                               .orElse(null)))
                                                                 .findAny()
                                                                 .orElseThrow(() -> {
                                                                   String error = "found illegal bulkId in request '"
                                                                                  + bulkId + "':  has no parent.";
                                                                   return new IllegalStateException(error);
                                                                 });

        TreeNode<BulkRequestOperation> childNode = childParentRelations.addDistinctNode(operation);
        parentNode.addChild(childNode);
      }
    }
    return childParentRelations;
  }

  /**
   * splits the list of operations into several lists so that all lists contain equal or fewer operations than
   * the maximum number of allowed requests at the service provider
   */
  private List<List<BulkRequestOperation>> splitRequestsSimple(List<BulkRequestOperation> operationsToSplit)
  {
    final int maxNumberOfOperationns = Math.max(1, getMaxNumberOfOperations());

    List<List<BulkRequestOperation>> splittedListParts = new ArrayList<>();
    if (operationsToSplit.size() <= maxNumberOfOperationns)
    {
      splittedListParts.add(operationsToSplit);
      return splittedListParts;
    }

    int currentIndex = 0;
    while (currentIndex < operationsToSplit.size())
    {
      final int nextIndex = currentIndex + maxNumberOfOperationns;
      final int effectiveListIndex = Math.min(nextIndex, operationsToSplit.size());
      List<BulkRequestOperation> subList = operationsToSplit.subList(currentIndex, effectiveListIndex);
      splittedListParts.add(new ArrayList<>(subList));
      currentIndex += subList.size();
    }

    log.debug("Splitted bulk operations into '{}' individual bulk-requests", splittedListParts.size());
    return splittedListParts;
  }

  /**
   * an additional build step class that allows to set the values of a bulk operation
   */
  public static class BulkRequestOperationCreator
  {

    /**
     * the owning top level class reference
     */
    private final BulkBuilder bulkBuilder;

    /**
     * the builder object that is used to build the operation
     */
    private final BulkRequestOperation.BulkRequestOperationBuilder builder = BulkRequestOperation.builder();

    public BulkRequestOperationCreator(BulkBuilder bulkBuilder, String path)
    {
      this.bulkBuilder = bulkBuilder;
      builder.path(path);
    }

    /**
     * sets the http method for this bulk operation
     */
    public BulkRequestOperationCreator method(HttpMethod method)
    {
      builder.method(method);
      return this;
    }

    /**
     * sets the bulkId for this operation. Required if http method is post and optional in any other cases
     */
    public BulkRequestOperationCreator bulkId(String bulkId)
    {
      builder.bulkId(bulkId);
      return this;
    }

    /**
     * sets the request body for this operation if any is required
     */
    public BulkRequestOperationCreator data(String data)
    {
      builder.data(data);
      return this;
    }

    /**
     * sets the request body for this operation if any is required
     */
    public BulkRequestOperationCreator data(JsonNode data)
    {
      builder.data(data.toString());
      return this;
    }

    /**
     * sets the etag version for this operation which may be used on update, path and delete requests
     */
    public BulkRequestOperationCreator version(ETag version)
    {
      builder.version(version);
      return this;
    }

    /**
     * asks the server to return the resource within the bulk response. This feature is supported only by the
     * SCIM-SDK implementation.
     *
     * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Return-resources-on-Bulk-Responses
     */
    public BulkRequestOperationCreator returnResource(boolean returnResource)
    {
      builder.returnResource(returnResource);
      return this;
    }

    /**
     * only usable for the SCIM-SDKs Bulk-Get custom feature. It limits the amount of resources to be returned
     * from the server if the bulk-get feature is utilized
     */
    public BulkRequestOperationCreator maxResourceLevel(int maxResourceLevel)
    {
      builder.maxResourceLevel(maxResourceLevel);
      return this;
    }

    /**
     * @return builds the operation object and returns to the owning top level instance
     */
    public BulkBuilder next()
    {
      BulkRequestOperation operation = builder.build();
      bulkBuilder.getBulkRequestOperationList().add(operation);
      if (!operation.getBulkId().isPresent())
      {
        operation.setBulkId(UUID.randomUUID().toString());
      }
      bulkBuilder.bulkRequestOperationMap.put(operation.getBulkId().get(), operation);
      return bulkBuilder;
    }

    /**
     * builds the operation and directly sends the request to the server
     */
    public ServerResponse<BulkResponse> sendRequest()
    {
      return sendRequest(Collections.emptyMap());
    }

    /**
     * builds the operation and directly sends the request to the server
     *
     * @param responseHandler a helper method to will allow the client to react to each individual response. This
     *          makes only sense if the feature {@link ScimClientConfig#isEnableAutomaticBulkRequestSplitting()}
     *          is enabled
     */
    public ServerResponse<BulkResponse> sendRequest(Consumer<ServerResponse<BulkResponse>> responseHandler)
    {
      return sendRequest(Collections.emptyMap(), responseHandler);
    }

    /**
     * builds the operation and directly sends the request to the server
     */
    public ServerResponse<BulkResponse> sendRequest(Map<String, String> httpHeaders)
    {
      return next().sendRequest(httpHeaders);
    }

    /**
     * builds the operation and directly sends the request to the server
     *
     * @param responseHandler a helper method to will allow the client to react to each individual response. This
     *          makes only sense if the feature {@link ScimClientConfig#isEnableAutomaticBulkRequestSplitting()}
     *          is enabled
     */
    public ServerResponse<BulkResponse> sendRequest(Map<String, String> httpHeaders,
                                                    Consumer<ServerResponse<BulkResponse>> responseHandler)
    {
      return next().sendRequest(httpHeaders, responseHandler);
    }

    /**
     * builds the operation and directly sends the request to the server
     */
    public ServerResponse<BulkResponse> sendRequestWithMultiHeaders(Map<String, String[]> httpHeaders)
    {
      return next().sendRequestWithMultiHeaders(httpHeaders);
    }

    /**
     * builds the operation and directly sends the request to the server
     *
     * @param responseHandler a helper method to will allow the client to react to each individual response. This
     *          makes only sense if the feature {@link ScimClientConfig#isEnableAutomaticBulkRequestSplitting()}
     *          is enabled
     */
    public ServerResponse<BulkResponse> sendRequestWithMultiHeaders(Map<String, String[]> httpHeaders,
                                                                    Consumer<ServerResponse<BulkResponse>> responseHandler)
    {
      return next().sendRequestWithMultiHeaders(httpHeaders, responseHandler);
    }
  }

  /**
   * this wrapper object is used when resolving bulkId-requests into several requests
   */
  @Getter
  private static class BulkRequestIdResolverWrapper
  {

    /**
     * an ordered list that must be executed in the order presented
     */
    private final List<List<BulkRequestOperation>> requestsList;

    /**
     * a parent child relationship map that uses the bulkId of the parents as keys and has its children as values
     */
    private final Map<String, List<BulkRequestOperation>> parentChildRelationMap;

    /**
     * after each request the results will be stored within this map in order to resolve the references of the
     * next request
     */
    private final Map<String, String> resolvedBulkIds;

    public BulkRequestIdResolverWrapper(List<List<BulkRequestOperation>> requestsList,
                                        Map<String, List<BulkRequestOperation>> parentChildRelationMap)
    {
      this.requestsList = Objects.requireNonNull(requestsList);
      this.parentChildRelationMap = Objects.requireNonNull(parentChildRelationMap);
      this.resolvedBulkIds = new HashMap<>();
    }
  }
}
