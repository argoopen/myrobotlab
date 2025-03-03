package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class BeagleBoardBlackMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(BeagleBoardBlackMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @param name
   *          n
   * 
   */
  public BeagleBoardBlackMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("service to access the beagle board black hardware");
    addCategory("microcontroller");
    setAvailable(false);

  }

}
