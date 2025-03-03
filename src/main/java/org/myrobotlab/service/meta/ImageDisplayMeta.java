package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ImageDisplayMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ImageDisplayMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public ImageDisplayMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("IBus serial protocol");
    setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary

    // TEMPORARY CORE DEPENDENCIES !!! (for uber-jar)
    // addDependency("orgId", "artifactId", "2.4.0");

    setAvailable(false);
    addCategory("general");

  }

}
