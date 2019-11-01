package de.gold.scim.server.resources.multicomplex;

import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:14 <br>
 * <br>
 * A URI that is a uniform resource locator (as defined in Section 1.1.3 of [RFC3986]) that points to a
 * resource location representing the user's image. The resource MUST be a file (e.g., a GIF, JPEG, or PNG
 * image file) rather than a web page containing an image. Service providers MAY return the same image in
 * different sizes, although it is recognized that no standard for describing images of various sizes
 * currently exists. Note that this attribute SHOULD NOT be used to send down arbitrary photos taken by this
 * user; instead, profile photos of the user that are suitable for display when describing the user should be
 * sent. Instead of the standard canonical values for type, this attribute defines the following canonical
 * values to represent popular photo sizes: "photo" and "thumbnail".
 */
@NoArgsConstructor
public class Photo extends MultiComplexNode
{

  @Builder
  public Photo(String type, Boolean primary, String display, String value, String ref)
  {
    super(type, primary, display, value, ref);
  }
}
