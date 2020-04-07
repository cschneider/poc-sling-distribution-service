package org.apache.sling.distribution.service.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;

/**
 * Experiment to export webjars (especially swagger ui.
 * See also bnd.bnd where the webjar is added to the Bundle-Classpath
 */
@Component(service = Webjars.class)
@HttpWhiteboardResource(pattern = "/webjars", prefix = "META-INF/resources/webjars")
public class Webjars {

}
