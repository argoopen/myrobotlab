package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class AudioFileMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AudioFileMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @param name
   *          n
   */
  public AudioFileMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("can play audio files on multiple tracks");
    addCategory("sound", "music");

    addDependency("javazoom", "jlayer", "1.0.1");
    addDependency("com.googlecode.soundlibs", "mp3spi", "1.9.5.4");
    addDependency("com.googlecode.soundlibs", "vorbisspi", "1.0.3.3"); // is
    // this
    // being
    // used
    // ?

    /*
     * addDependency("javazoom.spi", "1.9.5");
     * addDependency("javazoom.jl.player", "1.0.1");
     * addDependency("org.tritonus.share.sampled.floatsamplebuffer", "0.3.6");
     */

  }

}
