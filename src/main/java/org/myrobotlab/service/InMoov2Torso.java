package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * InMoovTorso - The inmoov torso. This will allow control of the topStom,
 * midStom, and lowStom servos.
 *
 */
public class InMoov2Torso extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoov2Torso.class);

  transient public ServoControl topStom;
  transient public ServoControl midStom;
  transient public ServoControl lowStom;

  public InMoov2Torso(String n, String id) {
    super(n, id);
    // TODO: just call startPeers here.
    // // createReserves(n); // Ok this might work but IT CANNOT BE IN SERVICE
    // // FRAMEWORK !!!!!
    // topStom = (ServoControl) createPeer("topStom");
    // midStom = (ServoControl) createPeer("midStom");
    // lowStom = (ServoControl) createPeer("lowStom");
    // // controller = (ServoController) createPeer("arduino");

    // FIXME - createPeers ?
  }

  public void startService() {
    super.startService();
    startPeers();
    topStom.setPin(27);
    midStom.setPin(28);
    lowStom.setPin(29);

    topStom.map(60.0, 120.0, 60.0, 120.0);
    midStom.map(0.0, 180.0, 0.0, 180.0);
    lowStom.map(0.0, 180.0, 0.0, 180.0);
    topStom.setRest(90.0);
    topStom.setPosition(90.0);
    midStom.setRest(90.0);
    midStom.setPosition(90.0);
    lowStom.setRest(90.0);
    lowStom.setPosition(90.0);

    setVelocity(5.0, 5.0, 5.0);

  }

  public void releaseService() {
    try {
      disable();
      releasePeers();
      super.releaseService();
    } catch (Exception e) {
      error(e);
    }
  }

  public void enable() {
    topStom.enable();
    midStom.enable();
    lowStom.enable();
  }

  public void setAutoDisable(Boolean param) {
    topStom.setAutoDisable(param);
    midStom.setAutoDisable(param);
    lowStom.setAutoDisable(param);
  }

  @Override
  public void broadcastState() {
    if (topStom != null)
      topStom.broadcastState();
    if (midStom != null)
      midStom.broadcastState();
    if (lowStom != null)
      lowStom.broadcastState();
  }

  public void disable() {
    topStom.disable();
    midStom.disable();
    lowStom.disable();
  }

  public long getLastActivityTime() {
    long minLastActivity = Math.max(topStom.getLastActivityTime(), midStom.getLastActivityTime());
    minLastActivity = Math.max(minLastActivity, lowStom.getLastActivityTime());
    return minLastActivity;
  }

  @Deprecated /* use LangUtils */
  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH, "%s.moveTorso(%.2f,%.2f,%.2f)\n", inMoovServiceName, topStom.getCurrentInputPos(), midStom.getCurrentInputPos(),
        lowStom.getCurrentInputPos());
  }

  public void moveTo(Double topStomPos, Double midStomPos, Double lowStomPos) {
    if (log.isDebugEnabled()) {
      log.debug("{} moveTo {} {} {}", getName(), topStomPos, midStomPos, lowStomPos);
    }
    if (topStom != null && topStomPos != null) { this.topStom.moveTo(topStomPos); }
    if (midStom != null && midStomPos != null) { this.midStom.moveTo(midStomPos); }
    if (lowStom != null && lowStomPos != null) { this.lowStom.moveTo(lowStomPos); }
  }

  public void moveToBlocking(Double topStomPos, Double midStomPos, Double lowStomPos) {
    log.info("init {} moveToBlocking ", getName());
    moveTo(topStomPos, midStomPos, lowStomPos);
    waitTargetPos();
    log.info("end {} moveToBlocking", getName());
  }

  public void waitTargetPos() {
    if (topStom != null) { topStom.waitTargetPos(); }
    if (midStom != null) { midStom.waitTargetPos(); }
    if (lowStom != null) { lowStom.waitTargetPos(); }
  }

  public void rest() {
    if (topStom != null) { topStom.rest(); }
    if (midStom != null) { midStom.rest(); }
    if (lowStom != null) { lowStom.rest(); }
  }

  @Override
  public boolean save() {
    super.save();
    topStom.save();
    midStom.save();
    lowStom.save();
    return true;
  }

  @Deprecated
  public boolean loadFile(String file) {
    File f = new File(file);
    Python p = (Python) Runtime.getService("python");
    log.info("Loading  Python file {}", f.getAbsolutePath());
    if (p == null) {
      log.error("Python instance not found");
      return false;
    }
    String script = null;
    try {
      script = FileIO.toString(f.getAbsolutePath());
    } catch (IOException e) {
      log.error("IO Error loading file : ", e);
      return false;
    }
    // evaluate the scripts in a blocking way.
    boolean result = p.exec(script, true);
    if (!result) {
      log.error("Error while loading file {}", f.getAbsolutePath());
      return false;
    } else {
      log.debug("Successfully loaded {}", f.getAbsolutePath());
    }
    return true;
  }

  /**
   * Sets the output min/max values for all servos in the torso. input limits on
   * servos are not modified in this method.
   * 
   * @param topStomMin
   *          a
   * @param topStomMax
   *          a
   * @param midStomMin
   *          a
   * @param midStomMax
   *          a
   * @param lowStomMin
   *          a
   * @param lowStomMax
   *          a
   * 
   */
  public void setLimits(double topStomMin, double topStomMax, double midStomMin, double midStomMax, double lowStomMin, double lowStomMax) {
    topStom.setMinMaxOutput(topStomMin, topStomMax);
    midStom.setMinMaxOutput(midStomMin, midStomMax);
    lowStom.setMinMaxOutput(lowStomMin, lowStomMax);
  }

  // ------------- added set pins
  public void setpins(Integer topStomPin, Integer midStomPin, Integer lowStomPin) {
    // createPeers();
    /*
     * this.topStom.setPin(topStom); this.midStom.setPin(midStom);
     * this.lowStom.setPin(lowStom);
     */

    /**
     * FIXME - has to be done outside of
     * 
     * arduino.servoAttachPin(topStom, topStomPin);
     * arduino.servoAttachPin(topStom, midStomPin);
     * arduino.servoAttachPin(topStom, lowStomPin);
     */
  }

  public void setSpeed(Double topStom, Double midStom, Double lowStom) {
    log.warn("setspeed deprecated please use setvelocity");
    this.topStom.setSpeed(topStom);
    this.midStom.setSpeed(midStom);
    this.lowStom.setSpeed(lowStom);
  }

  public void test() {

    topStom.moveTo(topStom.getCurrentInputPos() + 2);
    midStom.moveTo(midStom.getCurrentInputPos() + 2);
    lowStom.moveTo(lowStom.getCurrentInputPos() + 2);

    moveTo(35.0, 45.0, 55.0);
  }

  @Deprecated /* use setSpeed */
  public void setVelocity(Double topStom, Double midStom, Double lowStom) {
    this.topStom.setSpeed(topStom);
    this.midStom.setSpeed(midStom);
    this.lowStom.setSpeed(lowStom);
  }

  static public void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      VirtualArduino v = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");

      v.connect("COM4");
      InMoov2Torso torso = (InMoov2Torso) Runtime.start("i01.torso", "InMoovTorso");
      Runtime.start("webgui", "WebGui");
      torso.test();
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void fullSpeed() {
    topStom.fullSpeed();
    midStom.fullSpeed();
    lowStom.fullSpeed();
  }

  public void stop() {
    topStom.stop();
    midStom.stop();
    lowStom.stop();
  }

}
