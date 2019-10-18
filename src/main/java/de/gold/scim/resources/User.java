package de.gold.scim.resources;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.resources.complex.Address;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.resources.complex.Name;
import de.gold.scim.resources.multicomplex.Email;
import de.gold.scim.resources.multicomplex.Entitlement;
import de.gold.scim.resources.multicomplex.GroupNode;
import de.gold.scim.resources.multicomplex.Ims;
import de.gold.scim.resources.multicomplex.PersonRole;
import de.gold.scim.resources.multicomplex.PhoneNumber;
import de.gold.scim.resources.multicomplex.Photo;
import de.gold.scim.resources.multicomplex.ScimX509Certificate;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 23:22 <br>
 * <br>
 * SCIM provides a resource type for "User" resources. The core schema for "User" is identified using the
 * following schema URI: "urn:ietf:params:scim:schemas:core:2.0:User".
 */
public class User extends ResourceNode
{

  public User()
  {
    setSchemas(Arrays.asList(SchemaUris.USER_URI));
  }

  @Builder
  private User(String id,
               String externalId,
               Meta meta,
               String userName,
               Name name,
               String displayName,
               String nickName,
               String profileUrl,
               String title,
               String userType,
               String preferredLanguage,
               String locale,
               String timeZone,
               Boolean active,
               String password,
               List<Email> emails,
               List<PhoneNumber> phoneNumbers,
               List<Ims> ims,
               List<Photo> photos,
               List<Address> addresses,
               List<GroupNode> groups,
               List<Entitlement> entitlements,
               List<PersonRole> roles,
               List<ScimX509Certificate> x509Certificates,
               EnterpriseUser enterpriseUser)
  {
    this();
    setId(id);
    setExternalId(externalId);
    setMeta(meta);
    setUserName(userName);
    setNameNode(name);
    setDisplayName(displayName);
    setNickName(nickName);
    setProfileUrl(profileUrl);
    setTitle(title);
    setUserType(userType);
    setPreferredLanguage(preferredLanguage);
    setLocale(locale);
    setTimezone(timeZone);
    setActive(active);
    setPassword(password);
    setEmails(emails);
    setPhoneNumbers(phoneNumbers);
    setIms(ims);
    setPhotos(photos);
    setAddresses(addresses);
    setGroups(groups);
    setEntitlements(entitlements);
    setRoles(roles);
    setX509Certificates(x509Certificates);
    setEnterpriseUser(enterpriseUser);
  }

  /**
   * A service provider's unique identifier for the user, typically used by the user to directly authenticate to
   * the service provider. Often displayed to the user as their unique identifier within the system (as opposed
   * to "id" or "externalId", which are generally opaque and not user-friendly identifiers). Each User MUST
   * include a non-empty userName value. This identifier MUST be unique across the service provider's entire set
   * of Users. This attribute is REQUIRED and is case insensitive.
   */
  public Optional<String> getUserName()
  {
    return getStringAttribute(AttributeNames.RFC7643.USER_NAME);
  }

  /**
   * A service provider's unique identifier for the user, typically used by the user to directly authenticate to
   * the service provider. Often displayed to the user as their unique identifier within the system (as opposed
   * to "id" or "externalId", which are generally opaque and not user-friendly identifiers). Each User MUST
   * include a non-empty userName value. This identifier MUST be unique across the service provider's entire set
   * of Users. This attribute is REQUIRED and is case insensitive.
   */
  public void setUserName(String userName)
  {
    setAttribute(AttributeNames.RFC7643.USER_NAME, userName);
  }

  /**
   * The components of the user's name. Service providers MAY return just the full name as a single string in
   * the formatted sub-attribute, or they MAY return just the individual component attributes using the other
   * sub-attributes, or they MAY return both. If both variants are returned, they SHOULD be describing the same
   * name, with the formatted name indicating how the component attributes should be combined.
   */
  public Optional<Name> getNameNode()
  {
    return getObjectAttribute(AttributeNames.RFC7643.NAME, Name.class);
  }

  /**
   * The components of the user's name. Service providers MAY return just the full name as a single string in
   * the formatted sub-attribute, or they MAY return just the individual component attributes using the other
   * sub-attributes, or they MAY return both. If both variants are returned, they SHOULD be describing the same
   * name, with the formatted name indicating how the component attributes should be combined.
   */
  public void setNameNode(Name name)
  {
    setAttribute(AttributeNames.RFC7643.NAME, name);
  }

  /**
   * The name of the user, suitable for display to end-users. Each user returned MAY include a non-empty
   * displayName value. The name SHOULD be the full name of the User being described, if known (e.g., "Babs
   * Jensen" or "Ms. Barbara J Jensen, III") but MAY be a username or handle, if that is all that is available
   * (e.g., "bjensen"). The value provided SHOULD be the primary textual label by which this User is normally
   * displayed by the service provider when presenting it to end-users.
   */
  public Optional<String> getDisplayName()
  {
    return getStringAttribute(AttributeNames.RFC7643.DISPLAY_NAME);
  }

  /**
   * The name of the user, suitable for display to end-users. Each user returned MAY include a non-empty
   * displayName value. The name SHOULD be the full name of the User being described, if known (e.g., "Babs
   * Jensen" or "Ms. Barbara J Jensen, III") but MAY be a username or handle, if that is all that is available
   * (e.g., "bjensen"). The value provided SHOULD be the primary textual label by which this User is normally
   * displayed by the service provider when presenting it to end-users.
   */
  public void setDisplayName(String displayName)
  {
    setAttribute(AttributeNames.RFC7643.DISPLAY_NAME, displayName);
  }

  /**
   * The casual way to address the user in real life, e.g., "Bob" or "Bobby" instead of "Robert". This attribute
   * SHOULD NOT be used to represent a User's username (e.g., bjensen or mpepperidge).
   */
  public Optional<String> getNickName()
  {
    return getStringAttribute(AttributeNames.RFC7643.NICK_NAME);
  }

  /**
   * The casual way to address the user in real life, e.g., "Bob" or "Bobby" instead of "Robert". This attribute
   * SHOULD NOT be used to represent a User's username (e.g., bjensen or mpepperidge).
   */
  public void setNickName(String nickName)
  {
    setAttribute(AttributeNames.RFC7643.NICK_NAME, nickName);
  }

  /**
   * A URI that is a uniform resource locator (as defined in Section 1.1.3 of [RFC3986]) and that points to a
   * location representing the user's online profile (e.g., a web page). URIs are canonicalized per Section 6.2
   * of [RFC3986].
   */
  public Optional<String> getProfileUrl()
  {
    return getStringAttribute(AttributeNames.RFC7643.PROFILE_URL);
  }

  /**
   * A URI that is a uniform resource locator (as defined in Section 1.1.3 of [RFC3986]) and that points to a
   * location representing the user's online profile (e.g., a web page). URIs are canonicalized per Section 6.2
   * of [RFC3986].
   */
  public void setProfileUrl(String profileUrl)
  {
    setAttribute(AttributeNames.RFC7643.PROFILE_URL, profileUrl);
  }

  /**
   * The user's title, such as "Vice President".
   */
  public Optional<String> getTitle()
  {
    return getStringAttribute(AttributeNames.RFC7643.TITLE);
  }

  /**
   * The user's title, such as "Vice President".
   */
  public void setTitle(String title)
  {
    setAttribute(AttributeNames.RFC7643.TITLE, title);
  }

  /**
   * Used to identify the relationship between the organization and the user. Typical values used might be
   * "Contractor", "Employee", "Intern", "Temp", "External", and "Unknown", but any value may be used.
   */
  public Optional<String> getUserType()
  {
    return getStringAttribute(AttributeNames.RFC7643.USER_TYPE);
  }

  /**
   * Used to identify the relationship between the organization and the user. Typical values used might be
   * "Contractor", "Employee", "Intern", "Temp", "External", and "Unknown", but any value may be used.
   */
  public void setUserType(String userType)
  {
    setAttribute(AttributeNames.RFC7643.USER_TYPE, userType);
  }

  /**
   * Indicates the user's preferred written or spoken languages and is generally used for selecting a localized
   * user interface. The value indicates the set of natural languages that are preferred. The format of the
   * value is the same as the HTTP Accept-Language header field (not including "Accept-Language:") and is
   * specified in Section 5.3.5 of [RFC7231]. The intent of this value is to enable cloud applications to
   * perform matching of language tags [RFC4647] to the user's language preferences, regardless of what may be
   * indicated by a user agent (which might be shared), or in an interaction that does not involve a user (such
   * as in a delegated OAuth 2.0 [RFC6749] style interaction) where normal HTTP Accept-Language header
   * negotiation cannot take place.
   */
  public Optional<String> getPreferredLanguage()
  {
    return getStringAttribute(AttributeNames.RFC7643.PREFERRED_LANGUAGE);
  }

  /**
   * Indicates the user's preferred written or spoken languages and is generally used for selecting a localized
   * user interface. The value indicates the set of natural languages that are preferred. The format of the
   * value is the same as the HTTP Accept-Language header field (not including "Accept-Language:") and is
   * specified in Section 5.3.5 of [RFC7231]. The intent of this value is to enable cloud applications to
   * perform matching of language tags [RFC4647] to the user's language preferences, regardless of what may be
   * indicated by a user agent (which might be shared), or in an interaction that does not involve a user (such
   * as in a delegated OAuth 2.0 [RFC6749] style interaction) where normal HTTP Accept-Language header
   * negotiation cannot take place.
   */
  public void setPreferredLanguage(String preferredLanguage)
  {
    setAttribute(AttributeNames.RFC7643.PREFERRED_LANGUAGE, preferredLanguage);
  }

  // @formatter:off
  /**
   * Used to indicate the User's default location for purposes of
   * localizing such items as currency, date time format, or numerical
   * representations.  A valid value is a language tag as defined in
   * [RFC5646].  Computer languages are explicitly excluded.
   *
   * A language tag is a sequence of one or more case-insensitive
   * sub-tags, each separated by a hyphen character ("-", %x2D).  For
   * backward compatibility, servers MAY accept tags separated by an
   * underscore character ("_", %x5F).  In most cases, a language tag
   * consists of a primary language sub-tag that identifies a broad
   * family of related languages (e.g., "en" = English) and that is
   * optionally followed by a series of sub-tags that refine or narrow
   * that language's range (e.g., "en-CA" = the variety of English as
   * communicated in Canada).  Whitespace is not allowed within a
   * language tag.  Example tags include:
   *
   *      fr, en-US, es-419, az-Arab, x-pig-latin, man-Nkoo-GN
   *
   * See [RFC5646] for further information.
   */
  // @formatter:on
  public Optional<String> getLocale()
  {
    return getStringAttribute(AttributeNames.RFC7643.LOCALE);
  }

  // @formatter:off
  /**
   * Used to indicate the User's default location for purposes of
   * localizing such items as currency, date time format, or numerical
   * representations.  A valid value is a language tag as defined in
   * [RFC5646].  Computer languages are explicitly excluded.
   *
   * A language tag is a sequence of one or more case-insensitive
   * sub-tags, each separated by a hyphen character ("-", %x2D).  For
   * backward compatibility, servers MAY accept tags separated by an
   * underscore character ("_", %x5F).  In most cases, a language tag
   * consists of a primary language sub-tag that identifies a broad
   * family of related languages (e.g., "en" = English) and that is
   * optionally followed by a series of sub-tags that refine or narrow
   * that language's range (e.g., "en-CA" = the variety of English as
   * communicated in Canada).  Whitespace is not allowed within a
   * language tag.  Example tags include:
   *
   *      fr, en-US, es-419, az-Arab, x-pig-latin, man-Nkoo-GN
   *
   * See [RFC5646] for further information.
   */
  // @formatter:on
  public void setLocale(String locale)
  {
    setAttribute(AttributeNames.RFC7643.LOCALE, locale);
  }

  /**
   * The User's time zone, in IANA Time Zone database format [RFC6557], also known as the "Olson" time zone
   * database format [Olson-TZ] (e.g., "America/Los_Angeles").
   */
  public Optional<String> getTimezone()
  {
    return getStringAttribute(AttributeNames.RFC7643.TIMEZONE);
  }

  /**
   * The User's time zone, in IANA Time Zone database format [RFC6557], also known as the "Olson" time zone
   * database format [Olson-TZ] (e.g., "America/Los_Angeles").
   */
  public void setTimezone(String timezone)
  {
    setAttribute(AttributeNames.RFC7643.TIMEZONE, timezone);
  }

  /**
   * A Boolean value indicating the user's administrative status. The definitive meaning of this attribute is
   * determined by the service provider. As a typical example, a value of true implies that the user is able to
   * log in, while a value of false implies that the user's account has been suspended.
   */
  public Optional<Boolean> isActive()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.ACTIVE);
  }

  /**
   * A Boolean value indicating the user's administrative status. The definitive meaning of this attribute is
   * determined by the service provider. As a typical example, a value of true implies that the user is able to
   * log in, while a value of false implies that the user's account has been suspended.
   */
  public void setActive(Boolean active)
  {
    setAttribute(AttributeNames.RFC7643.ACTIVE, active);
  }

  // @formatter:off
  /**
   * This attribute is intended to be used as a means to set, replace,
   * or compare (i.e., filter for equality) a password.  The cleartext
   * value or the hashed value of a password SHALL NOT be returnable by
   * a service provider.  If a service provider holds the value
   * locally, the value SHOULD be hashed.  When a password is set or
   * changed by the client, the cleartext password SHOULD be processed
   * by the service provider as follows:
   *
   *   *  Prepare the cleartext value for international language
   *      comparison.  See Section 7.8 of [RFC7644].
   *
   *   *  Validate the value against server password policy.  Note: The
   *      definition and enforcement of password policy are beyond the
   *      scope of this document.
   *
   *   *  Ensure that the value is encrypted (e.g., hashed).  See
   *      Section 9.2 for acceptable hashing and encryption handling when
   *      storing or persisting for provisioning workflow reasons.
   *
   *   A service provider that immediately passes the cleartext value on
   *   to another system or programming interface MUST pass the value
   *   directly over a secured connection (e.g., Transport Layer Security
   *   (TLS)).  If the value needs to be temporarily persisted for a
   *   period of time (e.g., because of a workflow) before provisioning,
   *   then the value MUST be protected by some method, such as
   *   encryption.
   *
   *   Testing for an equality match MAY be supported if there is an
   *   existing stored hashed value.  When testing for equality, the
   *   service provider:
   *
   *   *  Prepares the filter value for international language
   *      comparison.  See Section 7.8 of [RFC7644].
   *
   *   *  Generates the salted hash of the filter value and tests for a
   *      match with the locally held value.
   *
   *   The mutability of the password attribute is "writeOnly",
   *   indicating that the value MUST NOT be returned by a service
   *   provider in any form (the attribute characteristic "returned" is
   *   "never").
   */
  // @formatter:on
  public Optional<String> getPassword()
  {
    return getStringAttribute(AttributeNames.RFC7643.PASSWORD);
  }

  // @formatter:off
  /**
   * This attribute is intended to be used as a means to set, replace,
   * or compare (i.e., filter for equality) a password.  The cleartext
   * value or the hashed value of a password SHALL NOT be returnable by
   * a service provider.  If a service provider holds the value
   * locally, the value SHOULD be hashed.  When a password is set or
   * changed by the client, the cleartext password SHOULD be processed
   * by the service provider as follows:
   *
   *   *  Prepare the cleartext value for international language
   *      comparison.  See Section 7.8 of [RFC7644].
   *
   *   *  Validate the value against server password policy.  Note: The
   *      definition and enforcement of password policy are beyond the
   *      scope of this document.
   *
   *   *  Ensure that the value is encrypted (e.g., hashed).  See
   *      Section 9.2 for acceptable hashing and encryption handling when
   *      storing or persisting for provisioning workflow reasons.
   *
   *   A service provider that immediately passes the cleartext value on
   *   to another system or programming interface MUST pass the value
   *   directly over a secured connection (e.g., Transport Layer Security
   *   (TLS)).  If the value needs to be temporarily persisted for a
   *   period of time (e.g., because of a workflow) before provisioning,
   *   then the value MUST be protected by some method, such as
   *   encryption.
   *
   *   Testing for an equality match MAY be supported if there is an
   *   existing stored hashed value.  When testing for equality, the
   *   service provider:
   *
   *   *  Prepares the filter value for international language
   *      comparison.  See Section 7.8 of [RFC7644].
   *
   *   *  Generates the salted hash of the filter value and tests for a
   *      match with the locally held value.
   *
   *   The mutability of the password attribute is "writeOnly",
   *   indicating that the value MUST NOT be returned by a service
   *   provider in any form (the attribute characteristic "returned" is
   *   "never").
   */
  // @formatter:on
  public void setPassword(String password)
  {
    setAttribute(AttributeNames.RFC7643.PASSWORD, password);
  }

  /**
   * Email addresses for the User. The value SHOULD be specified according to [RFC5321]. Service providers
   * SHOULD canonicalize the value according to [RFC5321], e.g., "bjensen@example.com" instead of
   * "bjensen@EXAMPLE.COM". The "display" sub-attribute MAY be used to return the canonicalized representation
   * of the email value. The "type" sub-attribute is used to provide a classification meaningful to the (human)
   * user. The user interface should encourage the use of basic values of "work", "home", and "other" and MAY
   * allow additional type values to be used at the discretion of SCIM clients.
   */
  public List<Email> getEmails()
  {
    return getArrayAttribute(AttributeNames.RFC7643.EMAILS, Email.class);
  }

  /**
   * Email addresses for the User. The value SHOULD be specified according to [RFC5321]. Service providers
   * SHOULD canonicalize the value according to [RFC5321], e.g., "bjensen@example.com" instead of
   * "bjensen@EXAMPLE.COM". The "display" sub-attribute MAY be used to return the canonicalized representation
   * of the email value. The "type" sub-attribute is used to provide a classification meaningful to the (human)
   * user. The user interface should encourage the use of basic values of "work", "home", and "other" and MAY
   * allow additional type values to be used at the discretion of SCIM clients.
   */
  public void setEmails(List<Email> emails)
  {
    setAttribute(AttributeNames.RFC7643.EMAILS, emails);
  }

  /**
   * Email addresses for the User. The value SHOULD be specified according to [RFC5321]. Service providers
   * SHOULD canonicalize the value according to [RFC5321], e.g., "bjensen@example.com" instead of
   * "bjensen@EXAMPLE.COM". The "display" sub-attribute MAY be used to return the canonicalized representation
   * of the email value. The "type" sub-attribute is used to provide a classification meaningful to the (human)
   * user. The user interface should encourage the use of basic values of "work", "home", and "other" and MAY
   * allow additional type values to be used at the discretion of SCIM clients.
   */
  public void addEmail(Email email)
  {
    addAttribute(AttributeNames.RFC7643.EMAILS, email);
  }

  /**
   * Phone numbers for the user. The value SHOULD be specified according to the format defined in [RFC3966],
   * e.g., 'tel:+1-201-555-0123'. Service providers SHOULD canonicalize the value according to [RFC3966] format,
   * when appropriate. The "display" sub-attribute MAY be used to return the canonicalized representation of the
   * phone number value. The sub-attribute "type" often has typical values of "work", "home", "mobile", "fax",
   * "pager", and "other" and MAY allow more types to be defined by the SCIM clients.
   */
  public List<PhoneNumber> getPhoneNumbers()
  {
    return getArrayAttribute(AttributeNames.RFC7643.PHONE_NUMBERS, PhoneNumber.class);
  }

  /**
   * Phone numbers for the user. The value SHOULD be specified according to the format defined in [RFC3966],
   * e.g., 'tel:+1-201-555-0123'. Service providers SHOULD canonicalize the value according to [RFC3966] format,
   * when appropriate. The "display" sub-attribute MAY be used to return the canonicalized representation of the
   * phone number value. The sub-attribute "type" often has typical values of "work", "home", "mobile", "fax",
   * "pager", and "other" and MAY allow more types to be defined by the SCIM clients.
   */
  public void setPhoneNumbers(List<PhoneNumber> phoneNumbers)
  {
    setAttribute(AttributeNames.RFC7643.PHONE_NUMBERS, phoneNumbers);
  }

  /**
   * Phone numbers for the user. The value SHOULD be specified according to the format defined in [RFC3966],
   * e.g., 'tel:+1-201-555-0123'. Service providers SHOULD canonicalize the value according to [RFC3966] format,
   * when appropriate. The "display" sub-attribute MAY be used to return the canonicalized representation of the
   * phone number value. The sub-attribute "type" often has typical values of "work", "home", "mobile", "fax",
   * "pager", and "other" and MAY allow more types to be defined by the SCIM clients.
   */
  public void addPhoneNumber(PhoneNumber phoneNumber)
  {
    addAttribute(AttributeNames.RFC7643.PHONE_NUMBERS, phoneNumber);
  }

  /**
   * Instant messaging address for the user. No official canonicalization rules exist for all instant messaging
   * addresses, but service providers SHOULD, when appropriate, remove all whitespace and convert the address to
   * lowercase. The "type" sub-attribute SHOULD take one of the following values: "aim", "gtalk", "icq", "xmpp",
   * "msn", "skype", "qq", "yahoo", or "other" (representing currently popular IM services at the time of this
   * writing). Service providers MAY add further values if new IM services are introduced and MAY specify more
   * detailed canonicalization rules for each possible value.
   */
  public List<Ims> getIms()
  {
    return getArrayAttribute(AttributeNames.RFC7643.IMS, Ims.class);
  }

  /**
   * Instant messaging address for the user. No official canonicalization rules exist for all instant messaging
   * addresses, but service providers SHOULD, when appropriate, remove all whitespace and convert the address to
   * lowercase. The "type" sub-attribute SHOULD take one of the following values: "aim", "gtalk", "icq", "xmpp",
   * "msn", "skype", "qq", "yahoo", or "other" (representing currently popular IM services at the time of this
   * writing). Service providers MAY add further values if new IM services are introduced and MAY specify more
   * detailed canonicalization rules for each possible value.
   */
  public void setIms(List<Ims> ims)
  {
    setAttribute(AttributeNames.RFC7643.IMS, ims);
  }

  /**
   * Instant messaging address for the user. No official canonicalization rules exist for all instant messaging
   * addresses, but service providers SHOULD, when appropriate, remove all whitespace and convert the address to
   * lowercase. The "type" sub-attribute SHOULD take one of the following values: "aim", "gtalk", "icq", "xmpp",
   * "msn", "skype", "qq", "yahoo", or "other" (representing currently popular IM services at the time of this
   * writing). Service providers MAY add further values if new IM services are introduced and MAY specify more
   * detailed canonicalization rules for each possible value.
   */
  public void addIms(Ims ims)
  {
    addAttribute(AttributeNames.RFC7643.IMS, ims);
  }

  /**
   * A URI that is a uniform resource locator (as defined in Section 1.1.3 of [RFC3986]) that points to a
   * resource location representing the user's image. The resource MUST be a file (e.g., a GIF, JPEG, or PNG
   * image file) rather than a web page containing an image. Service providers MAY return the same image in
   * different sizes, although it is recognized that no standard for describing images of various sizes
   * currently exists. Note that this attribute SHOULD NOT be used to send down arbitrary photos taken by this
   * user; instead, profile photos of the user that are suitable for display when describing the user should be
   * sent. Instead of the standard canonical values for type, this attribute defines the following canonical
   * values to represent popular photo sizes: "photo" and "thumbnail".
   */
  public List<Photo> getPhotos()
  {
    return getArrayAttribute(AttributeNames.RFC7643.PHOTOS, Photo.class);
  }

  /**
   * A URI that is a uniform resource locator (as defined in Section 1.1.3 of [RFC3986]) that points to a
   * resource location representing the user's image. The resource MUST be a file (e.g., a GIF, JPEG, or PNG
   * image file) rather than a web page containing an image. Service providers MAY return the same image in
   * different sizes, although it is recognized that no standard for describing images of various sizes
   * currently exists. Note that this attribute SHOULD NOT be used to send down arbitrary photos taken by this
   * user; instead, profile photos of the user that are suitable for display when describing the user should be
   * sent. Instead of the standard canonical values for type, this attribute defines the following canonical
   * values to represent popular photo sizes: "photo" and "thumbnail".
   */
  public void setPhotos(List<Photo> photos)
  {
    setAttribute(AttributeNames.RFC7643.PHOTOS, photos);
  }

  /**
   * A URI that is a uniform resource locator (as defined in Section 1.1.3 of [RFC3986]) that points to a
   * resource location representing the user's image. The resource MUST be a file (e.g., a GIF, JPEG, or PNG
   * image file) rather than a web page containing an image. Service providers MAY return the same image in
   * different sizes, although it is recognized that no standard for describing images of various sizes
   * currently exists. Note that this attribute SHOULD NOT be used to send down arbitrary photos taken by this
   * user; instead, profile photos of the user that are suitable for display when describing the user should be
   * sent. Instead of the standard canonical values for type, this attribute defines the following canonical
   * values to represent popular photo sizes: "photo" and "thumbnail".
   */
  public void addPhoto(Photo photo)
  {
    addAttribute(AttributeNames.RFC7643.PHOTOS, photo);
  }

  /**
   * A physical mailing address for this user. Canonical type values of "work", "home", and "other". This
   * attribute is a complex type with the following sub-attributes. All sub-attributes are OPTIONAL.
   */
  public List<Address> getAddresses()
  {
    return getArrayAttribute(AttributeNames.RFC7643.ADDRESSES, Address.class);
  }

  /**
   * A physical mailing address for this user. Canonical type values of "work", "home", and "other". This
   * attribute is a complex type with the following sub-attributes. All sub-attributes are OPTIONAL.
   */
  public void setAddresses(List<Address> addresses)
  {
    setAttribute(AttributeNames.RFC7643.ADDRESSES, addresses);
  }

  /**
   * A physical mailing address for this user. Canonical type values of "work", "home", and "other". This
   * attribute is a complex type with the following sub-attributes. All sub-attributes are OPTIONAL.
   */
  public void addAddress(Address address)
  {
    addAttribute(AttributeNames.RFC7643.ADDRESSES, address);
  }

  /**
   * A list of groups to which the user belongs, either through direct membership, through nested groups, or
   * dynamically calculated. The values are meant to enable expression of common group-based or role-based
   * access control models, although no explicit authorization model is defined. It is intended that the
   * semantics of group membership and any behavior or authorization granted as a result of membership are
   * defined by the service provider. The canonical types "direct" and "indirect" are defined to describe how
   * the group membership was derived. Direct group membership indicates that the user is directly associated
   * with the group and SHOULD indicate that clients may modify membership through the "Group" resource.
   * Indirect membership indicates that user membership is transitive or dynamic and implies that clients cannot
   * modify indirect group membership through the "Group" resource but MAY modify direct group membership
   * through the "Group" resource, which may influence indirect memberships. If the SCIM service provider
   * exposes a "Group" resource, the "value" sub-attribute MUST be the "id", and the "$ref" sub-attribute must
   * be the URI of the corresponding "Group" resources to which the user belongs. Since this attribute has a
   * mutability of "readOnly", group membership changes MUST be applied via the "Group" Resource (Section 4.2).
   * This attribute has a mutability of "readOnly".
   */
  public List<GroupNode> getGroups()
  {
    return getArrayAttribute(AttributeNames.RFC7643.GROUPS, GroupNode.class);
  }

  /**
   * A list of groups to which the user belongs, either through direct membership, through nested groups, or
   * dynamically calculated. The values are meant to enable expression of common group-based or role-based
   * access control models, although no explicit authorization model is defined. It is intended that the
   * semantics of group membership and any behavior or authorization granted as a result of membership are
   * defined by the service provider. The canonical types "direct" and "indirect" are defined to describe how
   * the group membership was derived. Direct group membership indicates that the user is directly associated
   * with the group and SHOULD indicate that clients may modify membership through the "Group" resource.
   * Indirect membership indicates that user membership is transitive or dynamic and implies that clients cannot
   * modify indirect group membership through the "Group" resource but MAY modify direct group membership
   * through the "Group" resource, which may influence indirect memberships. If the SCIM service provider
   * exposes a "Group" resource, the "value" sub-attribute MUST be the "id", and the "$ref" sub-attribute must
   * be the URI of the corresponding "Group" resources to which the user belongs. Since this attribute has a
   * mutability of "readOnly", group membership changes MUST be applied via the "Group" Resource (Section 4.2).
   * This attribute has a mutability of "readOnly".
   */
  public void setGroups(List<GroupNode> groups)
  {
    setAttribute(AttributeNames.RFC7643.GROUPS, groups);
  }

  /**
   * A list of groups to which the user belongs, either through direct membership, through nested groups, or
   * dynamically calculated. The values are meant to enable expression of common group-based or role-based
   * access control models, although no explicit authorization model is defined. It is intended that the
   * semantics of group membership and any behavior or authorization granted as a result of membership are
   * defined by the service provider. The canonical types "direct" and "indirect" are defined to describe how
   * the group membership was derived. Direct group membership indicates that the user is directly associated
   * with the group and SHOULD indicate that clients may modify membership through the "Group" resource.
   * Indirect membership indicates that user membership is transitive or dynamic and implies that clients cannot
   * modify indirect group membership through the "Group" resource but MAY modify direct group membership
   * through the "Group" resource, which may influence indirect memberships. If the SCIM service provider
   * exposes a "Group" resource, the "value" sub-attribute MUST be the "id", and the "$ref" sub-attribute must
   * be the URI of the corresponding "Group" resources to which the user belongs. Since this attribute has a
   * mutability of "readOnly", group membership changes MUST be applied via the "Group" Resource (Section 4.2).
   * This attribute has a mutability of "readOnly".
   */
  public void addGroup(GroupNode group)
  {
    addAttribute(AttributeNames.RFC7643.GROUPS, group);
  }

  /**
   * A list of entitlements for the user that represent a thing the user has. An entitlement may be an
   * additional right to a thing, object, or service. No vocabulary or syntax is specified; service providers
   * and clients are expected to encode sufficient information in the value so as to accurately and without
   * ambiguity determine what the user has access to. This value has no canonical types, although a type may be
   * useful as a means to scope entitlements.
   */
  public List<Entitlement> getEntitlements()
  {
    return getArrayAttribute(AttributeNames.RFC7643.ENTITLEMENTS, Entitlement.class);
  }

  /**
   * A list of entitlements for the user that represent a thing the user has. An entitlement may be an
   * additional right to a thing, object, or service. No vocabulary or syntax is specified; service providers
   * and clients are expected to encode sufficient information in the value so as to accurately and without
   * ambiguity determine what the user has access to. This value has no canonical types, although a type may be
   * useful as a means to scope entitlements.
   */
  public void setEntitlements(List<Entitlement> entitlements)
  {
    setAttribute(AttributeNames.RFC7643.ENTITLEMENTS, entitlements);
  }

  /**
   * A list of entitlements for the user that represent a thing the user has. An entitlement may be an
   * additional right to a thing, object, or service. No vocabulary or syntax is specified; service providers
   * and clients are expected to encode sufficient information in the value so as to accurately and without
   * ambiguity determine what the user has access to. This value has no canonical types, although a type may be
   * useful as a means to scope entitlements.
   */
  public void addEntitlement(Entitlement entitlement)
  {
    addAttribute(AttributeNames.RFC7643.ENTITLEMENTS, entitlement);
  }

  /**
   * A list of roles for the user that collectively represent who the user is, e.g., "Student", "Faculty". No
   * vocabulary or syntax is specified, although it is expected that a role value is a String or label
   * representing a collection of entitlements. This value has no canonical types.
   */
  public List<PersonRole> getRoles()
  {
    return getArrayAttribute(AttributeNames.RFC7643.ROLES, PersonRole.class);
  }

  /**
   * A list of roles for the user that collectively represent who the user is, e.g., "Student", "Faculty". No
   * vocabulary or syntax is specified, although it is expected that a role value is a String or label
   * representing a collection of entitlements. This value has no canonical types.
   */
  public void setRoles(List<PersonRole> personRoles)
  {
    setAttribute(AttributeNames.RFC7643.ROLES, personRoles);
  }

  /**
   * A list of roles for the user that collectively represent who the user is, e.g., "Student", "Faculty". No
   * vocabulary or syntax is specified, although it is expected that a role value is a String or label
   * representing a collection of entitlements. This value has no canonical types.
   */
  public void addRole(PersonRole personRole)
  {
    addAttribute(AttributeNames.RFC7643.ROLES, personRole);
  }

  /**
   * A list of certificates associated with the resource (e.g., a User). Each value contains exactly one
   * DER-encoded X.509 certificate (see Section 4 of [RFC5280]), which MUST be base64 encoded per Section 4 of
   * [RFC4648]. A single value MUST NOT contain multiple certificates and so does not contain the encoding
   * "SEQUENCE OF Certificate" in any guise.
   */
  public List<ScimX509Certificate> getX509Certificates()
  {
    return getArrayAttribute(AttributeNames.RFC7643.X509_CERTIFICATES, ScimX509Certificate.class);
  }

  /**
   * A list of certificates associated with the resource (e.g., a User). Each value contains exactly one
   * DER-encoded X.509 certificate (see Section 4 of [RFC5280]), which MUST be base64 encoded per Section 4 of
   * [RFC4648]. A single value MUST NOT contain multiple certificates and so does not contain the encoding
   * "SEQUENCE OF Certificate" in any guise.
   */
  public void setX509Certificates(List<ScimX509Certificate> x509Certificates)
  {
    setAttribute(AttributeNames.RFC7643.X509_CERTIFICATES, x509Certificates);
  }

  /**
   * A list of certificates associated with the resource (e.g., a User). Each value contains exactly one
   * DER-encoded X.509 certificate (see Section 4 of [RFC5280]), which MUST be base64 encoded per Section 4 of
   * [RFC4648]. A single value MUST NOT contain multiple certificates and so does not contain the encoding
   * "SEQUENCE OF Certificate" in any guise.
   */
  public void addX509Certificate(ScimX509Certificate x509Certificate)
  {
    addAttribute(AttributeNames.RFC7643.X509_CERTIFICATES, x509Certificate);
  }

  /**
   * The following SCIM extension defines attributes commonly used in representing users that belong to, or act
   * on behalf of, a business or enterprise. The enterprise User extension is identified using the following
   * schema URI: "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User".
   */
  public Optional<EnterpriseUser> getEnterpriseUser()
  {
    return getObjectAttribute(SchemaUris.ENTERPRISE_USER_URI, EnterpriseUser.class);
  }

  /**
   * The following SCIM extension defines attributes commonly used in representing users that belong to, or act
   * on behalf of, a business or enterprise. The enterprise User extension is identified using the following
   * schema URI: "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User".
   */
  public void setEnterpriseUser(EnterpriseUser enterpriseUser)
  {
    setAttribute(SchemaUris.ENTERPRISE_USER_URI, enterpriseUser);
  }

}
