package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class PingdarMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(PingdarMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public PingdarMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("used as a ultra sonic radar");
    addCategory("sensors", "display");
    // put peer definitions in
    addPeer("controller", "Arduino", "controller for servo and sensor");
    addPeer("sensors", "UltrasonicSensor", "sensors");
    addPeer("servo", "Servo", "servo");

    // theoretically - Servo should follow the same share config
    // sharePeer("servo.controller", "controller", "Arduino", "shared
    // arduino");

  }

}
