package de.captaingoldfish.scim.sdk.server.etag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.NotModifiedException;
import de.captaingoldfish.scim.sdk.common.exceptions.PreconditionFailedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 21.11.2019 - 08:28 <br>
 * <br>
 * this class is used for handling entity tags
 */
@Slf4j
public class ETagHandler
{

  /**
   * if the service provider has its support vor eTag set to true this method will return either the already set
   * version from the meta-attribute of the resource node or will generate a version value by generating a
   * base64 encoded SHA-1 hash of the resource
   *
   * @param serviceProvider the service provider configuration
   * @param resourceNode the current resource node
   * @return the version set by the developer or a base64 encoded SHA-1 hash. An empty if etag is not supported
   */
  public static Optional<ETag> getResourceVersion(ServiceProvider serviceProvider,
                                                  ResourceType resourceType,
                                                  ResourceNode resourceNode)
  {
    if (!serviceProvider.getETagConfig().isSupported())
    {
      log.trace("Not handling eTags for service provider support for eTags is set to false");
      return Optional.empty();
    }
    else if (!resourceType.getFeatures().getETagFeature().isEnabled())
    {
      log.trace("Not handling eTags for support on resource type {} is disabled", resourceType.getName());
      return Optional.empty();
    }
    Optional<ETag> version = resourceNode.getMeta().flatMap(Meta::getVersion);
    if (version.isPresent())
    {
      log.trace("Version already set to: {}", version.get().getEntityTag());
      return version;
    }
    return Optional.of(generateVersionOfResource(resourceNode));
  }

  /**
   * generates a base64 encoded SHA-1 hash of the given resource node
   *
   * @param resourceNode the resource node for which we want to get a version value
   * @return the etag of the given resource node
   */
  private static ETag generateVersionOfResource(ResourceNode resourceNode)
  {
    try
    {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
      return ETag.builder()
                 .weak(true)
                 .tag(Base64.getEncoder()
                            .encodeToString(messageDigest.digest(resourceNode.toString()
                                                                             .getBytes(StandardCharsets.UTF_8))))
                 .build();
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new InternalServerException(e.getMessage(), e, null);
    }
  }

  /**
   * will validate if the given httpHeaders do contain an entity tag within the request and if this entity tag
   * matches the state of the current resource
   *
   * @param serviceProvider the service provider configuration
   * @param currentState supplier that gives access to the resource that should be checked
   * @param httpHeaders the http headers that might contain the corresponding http request headers
   */
  public static void validateVersion(ServiceProvider serviceProvider,
                                     ResourceType resourceType,
                                     Supplier<ResourceNode> currentState,
                                     Map<String, String> httpHeaders)
  {
    if (!serviceProvider.getETagConfig().isSupported())
    {
      log.trace("Not handling eTags for service provider support for eTags is set to false");
      return;
    }
    else if (!resourceType.getFeatures().getETagFeature().isEnabled())
    {
      log.trace("Not handling eTags for for support on resource type {} is disabled", resourceType.getName());
      return;
    }
    Optional<ETag> ifNoneMatchEtag = getETagFromHeader(httpHeaders, true);
    Optional<ETag> ifMatchEtag = getETagFromHeader(httpHeaders, false);
    if (!ifNoneMatchEtag.isPresent() && !ifMatchEtag.isPresent())
    {
      return;
    }
    ResourceNode resourceNode = currentState.get();
    if (resourceNode == null)
    {
      throw new ResourceNotFoundException(null, null, null);
    }
    Optional<ETag> version = resourceNode.getMeta().flatMap(Meta::getVersion);
    ETag currentVersion = version.orElseGet(() -> generateVersionOfResource(resourceNode));
    if (ifNoneMatchEtag.isPresent())
    {
      // the client wants a response under the condition that the ETag versions do not match so in case they do
      // match we should return a http-response code of 304 (not modified)
      if (compareEtags(currentVersion, ifNoneMatchEtag.get()))
      {
        throw new NotModifiedException();
      }
    }
    else
    {
      // the client wants a response under the condition that the ETag versions do match and the comparison fails a
      // precondition failed exception should be thrown
      if (!compareEtags(currentVersion, ifMatchEtag.get()))
      {
        throw new PreconditionFailedException("eTag status of resource has changed. Current value is: "
                                              + currentVersion.getEntityTag());
      }
    }
  }

  /**
   * checks if the entity from the request matches the entity tag of the current resource version
   *
   * @param currentVersion the current resource version that is being accessed
   * @param requestVersion the entity tag extracted from the client request
   * @return true if the entity tags do match, false else
   */
  private static boolean compareEtags(ETag currentVersion, ETag requestVersion)
  {
    return requestVersion.equals(currentVersion);
  }

  /**
   * will extract either the If-Match header from the request or the If-None-Match header if one of these do
   * exist. If both headers exist an exception is thrown since these headers are mutually exclusive
   *
   * @param httpHeaders the http headers from the request that optionally have one of the entity tag headers
   * @param ifNot if set to true the If-None-Match header is extracted, the If-Match header if set to false
   * @return an empty of the specified entity tag
   */
  protected static Optional<ETag> getETagFromHeader(Map<String, String> httpHeaders, boolean ifNot)
  {
    String ifMatchValue = httpHeaders.keySet()
                                     .stream()
                                     .filter(header -> StringUtils.equalsIgnoreCase(header, HttpHeader.IF_MATCH_HEADER))
                                     .findAny()
                                     .orElse(null);
    String ifNoneMatchValue = httpHeaders.keySet()
                                         .stream()
                                         .filter(header -> StringUtils.equalsIgnoreCase(header,
                                                                                        HttpHeader.IF_NONE_MATCH_HEADER))
                                         .findAny()
                                         .orElse(null);
    if (ifMatchValue != null && ifNoneMatchValue != null)
    {
      throw new BadRequestException("the http header '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                                    + HttpHeader.IF_NONE_MATCH_HEADER + "' are mutually"
                                    + " exclusive you should only send one of these headers per request", null,
                                    ScimType.Custom.INVALID_PARAMETERS);
    }

    Function<Optional<ETag>, Optional<ETag>> getValue = value -> {
      if (value.isPresent())
      {
        if (StringUtils.isBlank(value.get().getTag()))
        {
          return Optional.empty();
        }
        else
        {
          return value;
        }
      }
      else
      {
        return Optional.empty();
      }
    };
    if (ifNot && ifNoneMatchValue != null)
    {
      return getValue.apply(Optional.ofNullable(httpHeaders.get(ifNoneMatchValue)).map(ETag::parseETag));
    }
    else if (!ifNot && ifMatchValue != null)
    {
      return getValue.apply(Optional.ofNullable(httpHeaders.get(ifMatchValue)).map(ETag::parseETag));
    }
    return Optional.empty();
  }

}
