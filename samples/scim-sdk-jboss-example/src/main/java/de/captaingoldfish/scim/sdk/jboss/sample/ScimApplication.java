package de.captaingoldfish.scim.sdk.jboss.sample;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


/**
 * @author Pascal Knueppel
 * @since 22.01.2021
 */
@ApplicationScoped
@ApplicationPath("/jaxrs")
public class ScimApplication extends Application
{

}
