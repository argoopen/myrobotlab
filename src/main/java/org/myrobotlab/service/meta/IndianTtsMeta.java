package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class IndianTtsMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(IndianTtsMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public IndianTtsMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("Hindi text to speech support - requires keys");
    setCloudService(true);
    addCategory("speech", "cloud");
    setSponsor("moz4r");
    addCategory("speech", "sound");
    addPeer("httpClient", "HttpClient", "httpClient");

    setRequiresKeys(true);

  }

}
