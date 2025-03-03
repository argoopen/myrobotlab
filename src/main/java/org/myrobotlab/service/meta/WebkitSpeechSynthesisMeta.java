package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class WebkitSpeechSynthesisMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebkitSpeechSynthesisMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   */
  public WebkitSpeechSynthesisMeta(String name) {

    super(name);
    addDescription("Web speech api using Chrome or Firefox speech synthesis");
    setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    addCategory("speech", "sound");

  }

}
