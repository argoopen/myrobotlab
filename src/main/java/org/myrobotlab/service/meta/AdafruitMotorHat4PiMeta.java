package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class AdafruitMotorHat4PiMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AdafruitMotorHat4PiMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @param name
   *          n
   */
  public AdafruitMotorHat4PiMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("description of service");
    addCategory("general", "unknown");
  }

}
