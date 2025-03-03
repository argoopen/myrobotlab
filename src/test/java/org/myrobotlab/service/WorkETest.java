package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class WorkETest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(WorkETest.class);

  final static byte[] M1_FORWARD_POWER_LEVEL_12 = new byte[] { 6, -128, 0, 12 };

  final static byte[] M1_FORWARD_POWER_LEVEL_20 = new byte[] { 12, -128, 0, 20 };

  final static byte[] M1_FORWARD_POWER_LEVEL_3 = new byte[] { -86, -128, 0, 3 };

  final static byte[] M1_FORWARD_POWER_LEVEL_6 = new byte[] { 3, -128, 0, 6 };

  static Serial uart = null;

  static WorkE worke = null;

  public static void main(String[] args) {
    try {
      // LoggingFactory.init("WARN");

      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(WorkETest.class);
      log.info("Result failures: {}", result.getFailureCount());
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    // Runtime.shutdown(); - this shuts down junit which will be considered a
    // "failure"
  }

  @Test
  public final void integrationTest() throws Exception {

    worke = (WorkE) Runtime.start("worke", "WorkE");
    worke.startPeer("joystick");

    // Joystick joystick = (Joystick)Runtime.start("worke", "Joystick");

    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();

    // opportunity to do substitutions - with create
    // e.g. - worke.setAxisLeft("zz");

    // virtualize for unit testing
    // uart = worke.virtualize(); FIXME

    // start the services

    // attach with existing configuration
    // can be skipped if user wants to do all the attaching manually
    // worke.attach();
    // get left axis of joystick
    String lefAxis = worke.getAxisLeft();

    byte[] sabertoothMsg = new byte[4];

    // get virtual joystick
    // and move work-e around

    /**
     * <pre>
     *
     * Joystick joystick = worke.getJoystick();
     * 
     * // joystick and validating appropriate power level
     * joystick.moveTo(lefAxis, 0.16); uart.read(sabertoothMsg);
     * assertTrue(Arrays.equals(M1_FORWARD_POWER_LEVEL_3, sabertoothMsg));
     * 
     * joystick.moveTo(lefAxis, 0.32); uart.read(sabertoothMsg);
     * assertTrue(Arrays.equals(M1_FORWARD_POWER_LEVEL_6, sabertoothMsg));
     * 
     * joystick.moveTo(lefAxis, 0.64); uart.read(sabertoothMsg);
     * assertTrue(Arrays.equals(M1_FORWARD_POWER_LEVEL_12, sabertoothMsg));
     * 
     * joystick.moveTo(lefAxis, 1.0); uart.read(sabertoothMsg);
     * assertTrue(Arrays.equals(M1_FORWARD_POWER_LEVEL_20, sabertoothMsg));
     * 
     * joystick.pressButton("a");
     * 
     * Runtime.releaseAll();
     */
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }
}
