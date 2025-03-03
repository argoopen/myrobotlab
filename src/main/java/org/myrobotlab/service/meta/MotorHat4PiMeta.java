package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class MotorHat4PiMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MotorHat4PiMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public MotorHat4PiMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("Motor service for the Raspi Motor HAT");
    addCategory("motor");
    addPeer("hat", "AdafruitMotorHat4Pi", "Motor HAT");
    setAvailable(true);

  }

}
