package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RoboClawMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(RoboClawMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public RoboClawMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("interface for the powerful RoboClaw motor controller");
    addCategory("motor", "control");
    addPeer("serial", "Serial", "Serial Port");

  }

}
