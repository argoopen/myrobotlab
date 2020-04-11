package org.myrobotlab.arduino;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.myrobotlab.logging.Level;
import org.myrobotlab.arduino.virtual.MrlComm;
import org.myrobotlab.string.StringUtil;

/**
 * <pre>
 * 
 Welcome to Msg.java
 Its created by running ArduinoMsgGenerator
 which combines the MrlComm message schema (src/resource/Arduino/arduinoMsg.schema)
 with the cpp template (src/resource/Arduino/generate/Msg.java.template)

   Schema Type Conversions

  Schema      ARDUINO          Java              Range
  none    byte/unsigned char    int (cuz Java byte bites)    1 byte - 0 to 255
  boolean    boolean          boolean              0 1
    b16      int            int (short)            2 bytes  -32,768 to 32,767
    b32      long          int                4 bytes -2,147,483,648 to 2,147,483, 647
    bu32    unsigned long      long              0 to 4,294,967,295
    str      char*, size        String              variable length
    []      byte[], size      int[]              variable length

 All message editing should be done in the arduinoMsg.schema

 The binary wire format of an MrlComm is:

 MAGIC_NUMBER|MSG_SIZE|METHOD_NUMBER|PARAM0|PARAM1 ...
 
 </pre>

 */

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * MrlComm byte stream parser
 *
 * @author GroG
 * @author kwatters
 *
 */

public class VirtualMsg {

  public static final int MAX_MSG_SIZE = 64;
  public static final int MAGIC_NUMBER = 170; // 10101010
  public static final int MRLCOMM_VERSION = 64;
  
  int ackMaxWaitMs = 1000;
  
    boolean waiting = false;
  
  
  // send buffer
  int sendBufferSize = 0;
  int sendBuffer[] = new int[MAX_MSG_SIZE];
  
  // recv buffer
  int ioCmd[] = new int[MAX_MSG_SIZE];
  
  AtomicInteger byteCount = new AtomicInteger(0);
  int msgSize = 0;

  // ------ device type mapping constants
  int method = -1;
  public boolean debug = false;
  boolean invoke = true;
  
  private int errorServiceToHardwareRxCnt = 0;
  private int errorHardwareToServiceRxCnt = 0;
  
  boolean ackEnabled = false;
  private ByteArrayOutputStream baos = null;
  public volatile boolean pendingMessage = false;
  public volatile boolean clearToSend = false;
    
  public static class AckLock {
    // first is always true - since there
    // is no msg to be acknowledged...
    volatile boolean acknowledged = true;
  }
   
  transient AckLock ackRecievedLock = new AckLock();
  
  // recording related
  transient FileOutputStream record = null;
  transient StringBuilder rxBuffer = new StringBuilder();
  transient StringBuilder txBuffer = new StringBuilder();  

  public static final int DEVICE_TYPE_UNKNOWN   =     0;
  public static final int DEVICE_TYPE_ARDUINO   =     1;
  public static final int DEVICE_TYPE_ULTRASONICSENSOR   =     2;
  public static final int DEVICE_TYPE_STEPPER   =     3;
  public static final int DEVICE_TYPE_MOTOR   =     4;
  public static final int DEVICE_TYPE_SERVO   =     5;
  public static final int DEVICE_TYPE_SERIAL   =     6;
  public static final int DEVICE_TYPE_I2C   =     7;
  public static final int DEVICE_TYPE_NEOPIXEL   =     8;
  public static final int DEVICE_TYPE_ENCODER   =     9;
    
  // < publishMRLCommError/str errorMsg
  public final static int PUBLISH_MRLCOMM_ERROR = 1;
  // > getBoardInfo
  public final static int GET_BOARD_INFO = 2;
  // < publishBoardInfo/version/boardType/b16 microsPerLoop/b16 sram/activePins/[] deviceSummary
  public final static int PUBLISH_BOARD_INFO = 3;
  // > enablePin/address/type/b16 rate
  public final static int ENABLE_PIN = 4;
  // > setDebug/bool enabled
  public final static int SET_DEBUG = 5;
  // > setSerialRate/b32 rate
  public final static int SET_SERIAL_RATE = 6;
  // > softReset
  public final static int SOFT_RESET = 7;
  // > enableAck/bool enabled
  public final static int ENABLE_ACK = 8;
  // < publishAck/function
  public final static int PUBLISH_ACK = 9;
  // > echo/f32 myFloat/myByte/f32 secondFloat
  public final static int ECHO = 10;
  // < publishEcho/f32 myFloat/myByte/f32 secondFloat
  public final static int PUBLISH_ECHO = 11;
  // > customMsg/[] msg
  public final static int CUSTOM_MSG = 12;
  // < publishCustomMsg/[] msg
  public final static int PUBLISH_CUSTOM_MSG = 13;
  // > deviceDetach/deviceId
  public final static int DEVICE_DETACH = 14;
  // > i2cBusAttach/deviceId/i2cBus
  public final static int I2C_BUS_ATTACH = 15;
  // > i2cRead/deviceId/deviceAddress/size
  public final static int I2C_READ = 16;
  // > i2cWrite/deviceId/deviceAddress/[] data
  public final static int I2C_WRITE = 17;
  // > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
  public final static int I2C_WRITE_READ = 18;
  // < publishI2cData/deviceId/[] data
  public final static int PUBLISH_I2C_DATA = 19;
  // > neoPixelAttach/deviceId/pin/b32 numPixels
  public final static int NEO_PIXEL_ATTACH = 20;
  // > neoPixelSetAnimation/deviceId/animation/red/green/blue/b16 speed
  public final static int NEO_PIXEL_SET_ANIMATION = 21;
  // > neoPixelWriteMatrix/deviceId/[] buffer
  public final static int NEO_PIXEL_WRITE_MATRIX = 22;
  // > analogWrite/pin/value
  public final static int ANALOG_WRITE = 23;
  // > digitalWrite/pin/value
  public final static int DIGITAL_WRITE = 24;
  // > disablePin/pin
  public final static int DISABLE_PIN = 25;
  // > disablePins
  public final static int DISABLE_PINS = 26;
  // > pinMode/pin/mode
  public final static int PIN_MODE = 27;
  // < publishDebug/str debugMsg
  public final static int PUBLISH_DEBUG = 28;
  // < publishPinArray/[] data
  public final static int PUBLISH_PIN_ARRAY = 29;
  // > setTrigger/pin/triggerValue
  public final static int SET_TRIGGER = 30;
  // > setDebounce/pin/delay
  public final static int SET_DEBOUNCE = 31;
  // > servoAttach/deviceId/pin/b16 initPos/b16 initVelocity/str name
  public final static int SERVO_ATTACH = 32;
  // > servoAttachPin/deviceId/pin
  public final static int SERVO_ATTACH_PIN = 33;
  // > servoDetachPin/deviceId
  public final static int SERVO_DETACH_PIN = 34;
  // > servoSetVelocity/deviceId/b16 velocity
  public final static int SERVO_SET_VELOCITY = 35;
  // > servoSweepStart/deviceId/min/max/step
  public final static int SERVO_SWEEP_START = 36;
  // > servoSweepStop/deviceId
  public final static int SERVO_SWEEP_STOP = 37;
  // > servoMoveToMicroseconds/deviceId/b16 target
  public final static int SERVO_MOVE_TO_MICROSECONDS = 38;
  // > servoSetAcceleration/deviceId/b16 acceleration
  public final static int SERVO_SET_ACCELERATION = 39;
  // < publishServoEvent/deviceId/eventType/b16 currentPos/b16 targetPos
  public final static int PUBLISH_SERVO_EVENT = 40;
  // > serialAttach/deviceId/relayPin
  public final static int SERIAL_ATTACH = 41;
  // > serialRelay/deviceId/[] data
  public final static int SERIAL_RELAY = 42;
  // < publishSerialData/deviceId/[] data
  public final static int PUBLISH_SERIAL_DATA = 43;
  // > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
  public final static int ULTRASONIC_SENSOR_ATTACH = 44;
  // > ultrasonicSensorStartRanging/deviceId
  public final static int ULTRASONIC_SENSOR_START_RANGING = 45;
  // > ultrasonicSensorStopRanging/deviceId
  public final static int ULTRASONIC_SENSOR_STOP_RANGING = 46;
  // < publishUltrasonicSensorData/deviceId/b16 echoTime
  public final static int PUBLISH_ULTRASONIC_SENSOR_DATA = 47;
  // > setAref/b16 type
  public final static int SET_AREF = 48;
  // > motorAttach/deviceId/type/[] pins
  public final static int MOTOR_ATTACH = 49;
  // > motorMove/deviceId/pwr
  public final static int MOTOR_MOVE = 50;
  // > motorMoveTo/deviceId/pos
  public final static int MOTOR_MOVE_TO = 51;
  // > encoderAttach/deviceId/type/pin
  public final static int ENCODER_ATTACH = 52;
  // > setZeroPoint/deviceId
  public final static int SET_ZERO_POINT = 53;
  // < publishEncoderData/deviceId/b16 position
  public final static int PUBLISH_ENCODER_DATA = 54;
  // < publishMrlCommBegin/version
  public final static int PUBLISH_MRL_COMM_BEGIN = 55;
  // > servoStop/deviceId
  public final static int SERVO_STOP = 56;


/**
 * These methods will be invoked from the Msg class as callbacks from MrlComm.
 */
  
  // public void getBoardInfo(){}
  // public void enablePin(Integer address/*byte*/, Integer type/*byte*/, Integer rate/*b16*/){}
  // public void setDebug(Boolean enabled/*bool*/){}
  // public void setSerialRate(Integer rate/*b32*/){}
  // public void softReset(){}
  // public void enableAck(Boolean enabled/*bool*/){}
  // public void echo(Float myFloat/*f32*/, Integer myByte/*byte*/, Float secondFloat/*f32*/){}
  // public void customMsg(int[] msg/*[]*/){}
  // public void deviceDetach(Integer deviceId/*byte*/){}
  // public void i2cBusAttach(Integer deviceId/*byte*/, Integer i2cBus/*byte*/){}
  // public void i2cRead(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, Integer size/*byte*/){}
  // public void i2cWrite(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, int[] data/*[]*/){}
  // public void i2cWriteRead(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, Integer readSize/*byte*/, Integer writeValue/*byte*/){}
  // public void neoPixelAttach(Integer deviceId/*byte*/, Integer pin/*byte*/, Integer numPixels/*b32*/){}
  // public void neoPixelSetAnimation(Integer deviceId/*byte*/, Integer animation/*byte*/, Integer red/*byte*/, Integer green/*byte*/, Integer blue/*byte*/, Integer speed/*b16*/){}
  // public void neoPixelWriteMatrix(Integer deviceId/*byte*/, int[] buffer/*[]*/){}
  // public void analogWrite(Integer pin/*byte*/, Integer value/*byte*/){}
  // public void digitalWrite(Integer pin/*byte*/, Integer value/*byte*/){}
  // public void disablePin(Integer pin/*byte*/){}
  // public void disablePins(){}
  // public void pinMode(Integer pin/*byte*/, Integer mode/*byte*/){}
  // public void setTrigger(Integer pin/*byte*/, Integer triggerValue/*byte*/){}
  // public void setDebounce(Integer pin/*byte*/, Integer delay/*byte*/){}
  // public void servoAttach(Integer deviceId/*byte*/, Integer pin/*byte*/, Integer initPos/*b16*/, Integer initVelocity/*b16*/, String name/*str*/){}
  // public void servoAttachPin(Integer deviceId/*byte*/, Integer pin/*byte*/){}
  // public void servoDetachPin(Integer deviceId/*byte*/){}
  // public void servoSetVelocity(Integer deviceId/*byte*/, Integer velocity/*b16*/){}
  // public void servoSweepStart(Integer deviceId/*byte*/, Integer min/*byte*/, Integer max/*byte*/, Integer step/*byte*/){}
  // public void servoSweepStop(Integer deviceId/*byte*/){}
  // public void servoMoveToMicroseconds(Integer deviceId/*byte*/, Integer target/*b16*/){}
  // public void servoSetAcceleration(Integer deviceId/*byte*/, Integer acceleration/*b16*/){}
  // public void serialAttach(Integer deviceId/*byte*/, Integer relayPin/*byte*/){}
  // public void serialRelay(Integer deviceId/*byte*/, int[] data/*[]*/){}
  // public void ultrasonicSensorAttach(Integer deviceId/*byte*/, Integer triggerPin/*byte*/, Integer echoPin/*byte*/){}
  // public void ultrasonicSensorStartRanging(Integer deviceId/*byte*/){}
  // public void ultrasonicSensorStopRanging(Integer deviceId/*byte*/){}
  // public void setAref(Integer type/*b16*/){}
  // public void motorAttach(Integer deviceId/*byte*/, Integer type/*byte*/, int[] pins/*[]*/){}
  // public void motorMove(Integer deviceId/*byte*/, Integer pwr/*byte*/){}
  // public void motorMoveTo(Integer deviceId/*byte*/, Integer pos/*byte*/){}
  // public void encoderAttach(Integer deviceId/*byte*/, Integer type/*byte*/, Integer pin/*byte*/){}
  // public void setZeroPoint(Integer deviceId/*byte*/){}
  // public void servoStop(Integer deviceId/*byte*/){}
  

  
  public transient final static Logger log = LoggerFactory.getLogger(Msg.class);

  public VirtualMsg(MrlComm arduino, SerialDevice serial) {
    this.arduino = arduino;
    this.serial = serial;
  }
  
  public void begin(SerialDevice serial){
    this.serial = serial;
  }

  // transient private Msg instance;

  // ArduinoSerialCallBacks - TODO - extract interface
  transient private MrlComm arduino;
  
  transient private SerialDevice serial;
  
  public void setInvoke(boolean b){
    invoke = b;
  }
  
  public void processCommand(){
    processCommand(ioCmd);
  }
  
  public void processCommand(int[] ioCmd) {
    int startPos = 0;
    method = ioCmd[startPos];
    // always process mrlbegin.. 
    log.info("Process Command: {} Method: {}", Msg.methodToString(method), ioCmd);
    if (method != PUBLISH_MRL_COMM_BEGIN) {
      if (!clearToSend) {
        log.warn("Not Clear to send yet.  Dumping command {}", ioCmd);
        System.err.println("\nDumping command not clear to send.\n");
        return;
      }
    } else {
      // Process!
      log.info("Clear to process!!!!!!!!!!!!!!!!!!");
      this.clearToSend = true;
    }
    switch (method) {
    case GET_BOARD_INFO: {
      if(invoke){
        arduino.invoke("getBoardInfo");
      } else { 
         arduino.getBoardInfo();
      }
      break;
    }
    case ENABLE_PIN: {
      Integer address = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer type = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer rate = b16(ioCmd, startPos+1);
      startPos += 2; //b16
      if(invoke){
        arduino.invoke("enablePin",  address,  type,  rate);
      } else { 
         arduino.enablePin( address,  type,  rate);
      }
      break;
    }
    case SET_DEBUG: {
      Boolean enabled = (ioCmd[startPos+1] == 0)?false:true;
      startPos += 1;
      if(invoke){
        arduino.invoke("setDebug",  enabled);
      } else { 
         arduino.setDebug( enabled);
      }
      break;
    }
    case SET_SERIAL_RATE: {
      Integer rate = b32(ioCmd, startPos+1);
      startPos += 4; //b32
      if(invoke){
        arduino.invoke("setSerialRate",  rate);
      } else { 
         arduino.setSerialRate( rate);
      }
      break;
    }
    case SOFT_RESET: {
      if(invoke){
        arduino.invoke("softReset");
      } else { 
         arduino.softReset();
      }
      break;
    }
    case ENABLE_ACK: {
      Boolean enabled = (ioCmd[startPos+1] == 0)?false:true;
      startPos += 1;
      if(invoke){
        arduino.invoke("enableAck",  enabled);
      } else { 
         arduino.enableAck( enabled);
      }
      break;
    }
    case ECHO: {
      Float myFloat = f32(ioCmd, startPos+1);
      startPos += 4; //f32
      Integer myByte = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Float secondFloat = f32(ioCmd, startPos+1);
      startPos += 4; //f32
      if(invoke){
        arduino.invoke("echo",  myFloat,  myByte,  secondFloat);
      } else { 
         arduino.echo( myFloat,  myByte,  secondFloat);
      }
      break;
    }
    case CUSTOM_MSG: {
      int[] msg = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
      startPos += 1 + ioCmd[startPos+1];
      if(invoke){
        arduino.invoke("customMsg",  msg);
      } else { 
         arduino.customMsg( msg);
      }
      break;
    }
    case DEVICE_DETACH: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("deviceDetach",  deviceId);
      } else { 
         arduino.deviceDetach( deviceId);
      }
      break;
    }
    case I2C_BUS_ATTACH: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer i2cBus = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("i2cBusAttach",  deviceId,  i2cBus);
      } else { 
         arduino.i2cBusAttach( deviceId,  i2cBus);
      }
      break;
    }
    case I2C_READ: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer deviceAddress = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer size = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("i2cRead",  deviceId,  deviceAddress,  size);
      } else { 
         arduino.i2cRead( deviceId,  deviceAddress,  size);
      }
      break;
    }
    case I2C_WRITE: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer deviceAddress = ioCmd[startPos+1]; // bu8
      startPos += 1;
      int[] data = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
      startPos += 1 + ioCmd[startPos+1];
      if(invoke){
        arduino.invoke("i2cWrite",  deviceId,  deviceAddress,  data);
      } else { 
         arduino.i2cWrite( deviceId,  deviceAddress,  data);
      }
      break;
    }
    case I2C_WRITE_READ: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer deviceAddress = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer readSize = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer writeValue = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("i2cWriteRead",  deviceId,  deviceAddress,  readSize,  writeValue);
      } else { 
         arduino.i2cWriteRead( deviceId,  deviceAddress,  readSize,  writeValue);
      }
      break;
    }
    case NEO_PIXEL_ATTACH: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer numPixels = b32(ioCmd, startPos+1);
      startPos += 4; //b32
      if(invoke){
        arduino.invoke("neoPixelAttach",  deviceId,  pin,  numPixels);
      } else { 
         arduino.neoPixelAttach( deviceId,  pin,  numPixels);
      }
      break;
    }
    case NEO_PIXEL_SET_ANIMATION: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer animation = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer red = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer green = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer blue = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer speed = b16(ioCmd, startPos+1);
      startPos += 2; //b16
      if(invoke){
        arduino.invoke("neoPixelSetAnimation",  deviceId,  animation,  red,  green,  blue,  speed);
      } else { 
         arduino.neoPixelSetAnimation( deviceId,  animation,  red,  green,  blue,  speed);
      }
      break;
    }
    case NEO_PIXEL_WRITE_MATRIX: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      int[] buffer = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
      startPos += 1 + ioCmd[startPos+1];
      if(invoke){
        arduino.invoke("neoPixelWriteMatrix",  deviceId,  buffer);
      } else { 
         arduino.neoPixelWriteMatrix( deviceId,  buffer);
      }
      break;
    }
    case ANALOG_WRITE: {
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer value = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("analogWrite",  pin,  value);
      } else { 
         arduino.analogWrite( pin,  value);
      }
      break;
    }
    case DIGITAL_WRITE: {
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer value = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("digitalWrite",  pin,  value);
      } else { 
         arduino.digitalWrite( pin,  value);
      }
      break;
    }
    case DISABLE_PIN: {
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("disablePin",  pin);
      } else { 
         arduino.disablePin( pin);
      }
      break;
    }
    case DISABLE_PINS: {
      if(invoke){
        arduino.invoke("disablePins");
      } else { 
         arduino.disablePins();
      }
      break;
    }
    case PIN_MODE: {
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer mode = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("pinMode",  pin,  mode);
      } else { 
         arduino.pinMode( pin,  mode);
      }
      break;
    }
    case SET_TRIGGER: {
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer triggerValue = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("setTrigger",  pin,  triggerValue);
      } else { 
         arduino.setTrigger( pin,  triggerValue);
      }
      break;
    }
    case SET_DEBOUNCE: {
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer delay = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("setDebounce",  pin,  delay);
      } else { 
         arduino.setDebounce( pin,  delay);
      }
      break;
    }
    case SERVO_ATTACH: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer initPos = b16(ioCmd, startPos+1);
      startPos += 2; //b16
      Integer initVelocity = b16(ioCmd, startPos+1);
      startPos += 2; //b16
      String name = str(ioCmd, startPos+2, ioCmd[startPos+1]);
      startPos += 1 + ioCmd[startPos+1];
      if(invoke){
        arduino.invoke("servoAttach",  deviceId,  pin,  initPos,  initVelocity,  name);
      } else { 
         arduino.servoAttach( deviceId,  pin,  initPos,  initVelocity,  name);
      }
      break;
    }
    case SERVO_ATTACH_PIN: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("servoAttachPin",  deviceId,  pin);
      } else { 
         arduino.servoAttachPin( deviceId,  pin);
      }
      break;
    }
    case SERVO_DETACH_PIN: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("servoDetachPin",  deviceId);
      } else { 
         arduino.servoDetachPin( deviceId);
      }
      break;
    }
    case SERVO_SET_VELOCITY: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer velocity = b16(ioCmd, startPos+1);
      startPos += 2; //b16
      if(invoke){
        arduino.invoke("servoSetVelocity",  deviceId,  velocity);
      } else { 
         arduino.servoSetVelocity( deviceId,  velocity);
      }
      break;
    }
    case SERVO_SWEEP_START: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer min = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer max = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer step = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("servoSweepStart",  deviceId,  min,  max,  step);
      } else { 
         arduino.servoSweepStart( deviceId,  min,  max,  step);
      }
      break;
    }
    case SERVO_SWEEP_STOP: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("servoSweepStop",  deviceId);
      } else { 
         arduino.servoSweepStop( deviceId);
      }
      break;
    }
    case SERVO_MOVE_TO_MICROSECONDS: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer target = b16(ioCmd, startPos+1);
      startPos += 2; //b16
      if(invoke){
        arduino.invoke("servoMoveToMicroseconds",  deviceId,  target);
      } else { 
         arduino.servoMoveToMicroseconds( deviceId,  target);
      }
      break;
    }
    case SERVO_SET_ACCELERATION: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer acceleration = b16(ioCmd, startPos+1);
      startPos += 2; //b16
      if(invoke){
        arduino.invoke("servoSetAcceleration",  deviceId,  acceleration);
      } else { 
         arduino.servoSetAcceleration( deviceId,  acceleration);
      }
      break;
    }
    case SERIAL_ATTACH: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer relayPin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("serialAttach",  deviceId,  relayPin);
      } else { 
         arduino.serialAttach( deviceId,  relayPin);
      }
      break;
    }
    case SERIAL_RELAY: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      int[] data = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
      startPos += 1 + ioCmd[startPos+1];
      if(invoke){
        arduino.invoke("serialRelay",  deviceId,  data);
      } else { 
         arduino.serialRelay( deviceId,  data);
      }
      break;
    }
    case ULTRASONIC_SENSOR_ATTACH: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer triggerPin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer echoPin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("ultrasonicSensorAttach",  deviceId,  triggerPin,  echoPin);
      } else { 
         arduino.ultrasonicSensorAttach( deviceId,  triggerPin,  echoPin);
      }
      break;
    }
    case ULTRASONIC_SENSOR_START_RANGING: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("ultrasonicSensorStartRanging",  deviceId);
      } else { 
         arduino.ultrasonicSensorStartRanging( deviceId);
      }
      break;
    }
    case ULTRASONIC_SENSOR_STOP_RANGING: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("ultrasonicSensorStopRanging",  deviceId);
      } else { 
         arduino.ultrasonicSensorStopRanging( deviceId);
      }
      break;
    }
    case SET_AREF: {
      Integer type = b16(ioCmd, startPos+1);
      startPos += 2; //b16
      if(invoke){
        arduino.invoke("setAref",  type);
      } else { 
         arduino.setAref( type);
      }
      break;
    }
    case MOTOR_ATTACH: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer type = ioCmd[startPos+1]; // bu8
      startPos += 1;
      int[] pins = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
      startPos += 1 + ioCmd[startPos+1];
      if(invoke){
        arduino.invoke("motorAttach",  deviceId,  type,  pins);
      } else { 
         arduino.motorAttach( deviceId,  type,  pins);
      }
      break;
    }
    case MOTOR_MOVE: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer pwr = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("motorMove",  deviceId,  pwr);
      } else { 
         arduino.motorMove( deviceId,  pwr);
      }
      break;
    }
    case MOTOR_MOVE_TO: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer pos = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("motorMoveTo",  deviceId,  pos);
      } else { 
         arduino.motorMoveTo( deviceId,  pos);
      }
      break;
    }
    case ENCODER_ATTACH: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer type = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer pin = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("encoderAttach",  deviceId,  type,  pin);
      } else { 
         arduino.encoderAttach( deviceId,  type,  pin);
      }
      break;
    }
    case SET_ZERO_POINT: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("setZeroPoint",  deviceId);
      } else { 
         arduino.setZeroPoint( deviceId);
      }
      break;
    }
    case SERVO_STOP: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("servoStop",  deviceId);
      } else { 
         arduino.servoStop( deviceId);
      }
      break;
    }
    
    }
  }
  

  // Java-land --to--> MrlComm

  public synchronized byte[] publishMRLCommError(String errorMsg/*str*/) {
    log.info("Sending Messge: publishMRLCommError");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + (1 + errorMsg.length())); // size
      appendMessage(PUBLISH_MRLCOMM_ERROR); // msgType = 1
      appendMessage(errorMsg);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishMRLCommError");
        txBuffer.append("/");
        txBuffer.append(errorMsg);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishMRLCommError threw",e);
      return null;
    }
  }

  public synchronized byte[] publishBoardInfo(Integer version/*byte*/, Integer boardType/*byte*/, Integer microsPerLoop/*b16*/, Integer sram/*b16*/, Integer activePins/*byte*/, int[] deviceSummary/*[]*/) {
    log.info("Sending Messge: publishBoardInfo");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 2 + 2 + 1 + (1 + deviceSummary.length)); // size
      appendMessage(PUBLISH_BOARD_INFO); // msgType = 3
      appendMessage(version);
      appendMessage(boardType);
      appendMessageb16(microsPerLoop);
      appendMessageb16(sram);
      appendMessage(activePins);
      appendMessage(deviceSummary);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishBoardInfo");
        txBuffer.append("/");
        txBuffer.append(version);
        txBuffer.append("/");
        txBuffer.append(boardType);
        txBuffer.append("/");
        txBuffer.append(microsPerLoop);
        txBuffer.append("/");
        txBuffer.append(sram);
        txBuffer.append("/");
        txBuffer.append(activePins);
        txBuffer.append("/");
        txBuffer.append(Arrays.toString(deviceSummary));
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishBoardInfo threw",e);
      return null;
    }
  }

  public synchronized byte[] publishAck(Integer function/*byte*/) {
    log.info("Sending Messge: publishAck");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(PUBLISH_ACK); // msgType = 9
      appendMessage(function);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishAck");
        txBuffer.append("/");
        txBuffer.append(function);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishAck threw",e);
      return null;
    }
  }

  public synchronized byte[] publishEcho(Float myFloat/*f32*/, Integer myByte/*byte*/, Float secondFloat/*f32*/) {
    log.info("Sending Messge: publishEcho");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 4 + 1 + 4); // size
      appendMessage(PUBLISH_ECHO); // msgType = 11
      appendMessagef32(myFloat);
      appendMessage(myByte);
      appendMessagef32(secondFloat);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishEcho");
        txBuffer.append("/");
        txBuffer.append(myFloat);
        txBuffer.append("/");
        txBuffer.append(myByte);
        txBuffer.append("/");
        txBuffer.append(secondFloat);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishEcho threw",e);
      return null;
    }
  }

  public synchronized byte[] publishCustomMsg(int[] msg/*[]*/) {
    log.info("Sending Messge: publishCustomMsg");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + (1 + msg.length)); // size
      appendMessage(PUBLISH_CUSTOM_MSG); // msgType = 13
      appendMessage(msg);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishCustomMsg");
        txBuffer.append("/");
        txBuffer.append(Arrays.toString(msg));
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishCustomMsg threw",e);
      return null;
    }
  }

  public synchronized byte[] publishI2cData(Integer deviceId/*byte*/, int[] data/*[]*/) {
    log.info("Sending Messge: publishI2cData");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + (1 + data.length)); // size
      appendMessage(PUBLISH_I2C_DATA); // msgType = 19
      appendMessage(deviceId);
      appendMessage(data);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishI2cData");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("/");
        txBuffer.append(Arrays.toString(data));
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishI2cData threw",e);
      return null;
    }
  }

  public synchronized byte[] publishDebug(String debugMsg/*str*/) {
    log.info("Sending Messge: publishDebug");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + (1 + debugMsg.length())); // size
      appendMessage(PUBLISH_DEBUG); // msgType = 28
      appendMessage(debugMsg);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishDebug");
        txBuffer.append("/");
        txBuffer.append(debugMsg);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishDebug threw",e);
      return null;
    }
  }

  public synchronized byte[] publishPinArray(int[] data/*[]*/) {
    log.info("Sending Messge: publishPinArray");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + (1 + data.length)); // size
      appendMessage(PUBLISH_PIN_ARRAY); // msgType = 29
      appendMessage(data);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishPinArray");
        txBuffer.append("/");
        txBuffer.append(Arrays.toString(data));
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishPinArray threw",e);
      return null;
    }
  }

  public synchronized byte[] publishServoEvent(Integer deviceId/*byte*/, Integer eventType/*byte*/, Integer currentPos/*b16*/, Integer targetPos/*b16*/) {
    log.info("Sending Messge: publishServoEvent");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 2 + 2); // size
      appendMessage(PUBLISH_SERVO_EVENT); // msgType = 40
      appendMessage(deviceId);
      appendMessage(eventType);
      appendMessageb16(currentPos);
      appendMessageb16(targetPos);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishServoEvent");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("/");
        txBuffer.append(eventType);
        txBuffer.append("/");
        txBuffer.append(currentPos);
        txBuffer.append("/");
        txBuffer.append(targetPos);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishServoEvent threw",e);
      return null;
    }
  }

  public synchronized byte[] publishSerialData(Integer deviceId/*byte*/, int[] data/*[]*/) {
    log.info("Sending Messge: publishSerialData");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + (1 + data.length)); // size
      appendMessage(PUBLISH_SERIAL_DATA); // msgType = 43
      appendMessage(deviceId);
      appendMessage(data);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishSerialData");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("/");
        txBuffer.append(Arrays.toString(data));
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishSerialData threw",e);
      return null;
    }
  }

  public synchronized byte[] publishUltrasonicSensorData(Integer deviceId/*byte*/, Integer echoTime/*b16*/) {
    log.info("Sending Messge: publishUltrasonicSensorData");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 2); // size
      appendMessage(PUBLISH_ULTRASONIC_SENSOR_DATA); // msgType = 47
      appendMessage(deviceId);
      appendMessageb16(echoTime);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishUltrasonicSensorData");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("/");
        txBuffer.append(echoTime);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishUltrasonicSensorData threw",e);
      return null;
    }
  }

  public synchronized byte[] publishEncoderData(Integer deviceId/*byte*/, Integer position/*b16*/) {
    log.info("Sending Messge: publishEncoderData");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 2); // size
      appendMessage(PUBLISH_ENCODER_DATA); // msgType = 54
      appendMessage(deviceId);
      appendMessageb16(position);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishEncoderData");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("/");
        txBuffer.append(position);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishEncoderData threw",e);
      return null;
    }
  }

  public synchronized byte[] publishMrlCommBegin(Integer version/*byte*/) {
    log.info("Sending Messge: publishMrlCommBegin");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(PUBLISH_MRL_COMM_BEGIN); // msgType = 55
      appendMessage(version);
 
      byte[] message = sendMessage();
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> publishMrlCommBegin");
        txBuffer.append("/");
        txBuffer.append(version);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("publishMrlCommBegin threw",e);
      return null;
    }
  }


  public static String methodToString(int method) {
    switch (method) {
    case PUBLISH_MRLCOMM_ERROR:{
      return "publishMRLCommError";
    }
    case GET_BOARD_INFO:{
      return "getBoardInfo";
    }
    case PUBLISH_BOARD_INFO:{
      return "publishBoardInfo";
    }
    case ENABLE_PIN:{
      return "enablePin";
    }
    case SET_DEBUG:{
      return "setDebug";
    }
    case SET_SERIAL_RATE:{
      return "setSerialRate";
    }
    case SOFT_RESET:{
      return "softReset";
    }
    case ENABLE_ACK:{
      return "enableAck";
    }
    case PUBLISH_ACK:{
      return "publishAck";
    }
    case ECHO:{
      return "echo";
    }
    case PUBLISH_ECHO:{
      return "publishEcho";
    }
    case CUSTOM_MSG:{
      return "customMsg";
    }
    case PUBLISH_CUSTOM_MSG:{
      return "publishCustomMsg";
    }
    case DEVICE_DETACH:{
      return "deviceDetach";
    }
    case I2C_BUS_ATTACH:{
      return "i2cBusAttach";
    }
    case I2C_READ:{
      return "i2cRead";
    }
    case I2C_WRITE:{
      return "i2cWrite";
    }
    case I2C_WRITE_READ:{
      return "i2cWriteRead";
    }
    case PUBLISH_I2C_DATA:{
      return "publishI2cData";
    }
    case NEO_PIXEL_ATTACH:{
      return "neoPixelAttach";
    }
    case NEO_PIXEL_SET_ANIMATION:{
      return "neoPixelSetAnimation";
    }
    case NEO_PIXEL_WRITE_MATRIX:{
      return "neoPixelWriteMatrix";
    }
    case ANALOG_WRITE:{
      return "analogWrite";
    }
    case DIGITAL_WRITE:{
      return "digitalWrite";
    }
    case DISABLE_PIN:{
      return "disablePin";
    }
    case DISABLE_PINS:{
      return "disablePins";
    }
    case PIN_MODE:{
      return "pinMode";
    }
    case PUBLISH_DEBUG:{
      return "publishDebug";
    }
    case PUBLISH_PIN_ARRAY:{
      return "publishPinArray";
    }
    case SET_TRIGGER:{
      return "setTrigger";
    }
    case SET_DEBOUNCE:{
      return "setDebounce";
    }
    case SERVO_ATTACH:{
      return "servoAttach";
    }
    case SERVO_ATTACH_PIN:{
      return "servoAttachPin";
    }
    case SERVO_DETACH_PIN:{
      return "servoDetachPin";
    }
    case SERVO_SET_VELOCITY:{
      return "servoSetVelocity";
    }
    case SERVO_SWEEP_START:{
      return "servoSweepStart";
    }
    case SERVO_SWEEP_STOP:{
      return "servoSweepStop";
    }
    case SERVO_MOVE_TO_MICROSECONDS:{
      return "servoMoveToMicroseconds";
    }
    case SERVO_SET_ACCELERATION:{
      return "servoSetAcceleration";
    }
    case PUBLISH_SERVO_EVENT:{
      return "publishServoEvent";
    }
    case SERIAL_ATTACH:{
      return "serialAttach";
    }
    case SERIAL_RELAY:{
      return "serialRelay";
    }
    case PUBLISH_SERIAL_DATA:{
      return "publishSerialData";
    }
    case ULTRASONIC_SENSOR_ATTACH:{
      return "ultrasonicSensorAttach";
    }
    case ULTRASONIC_SENSOR_START_RANGING:{
      return "ultrasonicSensorStartRanging";
    }
    case ULTRASONIC_SENSOR_STOP_RANGING:{
      return "ultrasonicSensorStopRanging";
    }
    case PUBLISH_ULTRASONIC_SENSOR_DATA:{
      return "publishUltrasonicSensorData";
    }
    case SET_AREF:{
      return "setAref";
    }
    case MOTOR_ATTACH:{
      return "motorAttach";
    }
    case MOTOR_MOVE:{
      return "motorMove";
    }
    case MOTOR_MOVE_TO:{
      return "motorMoveTo";
    }
    case ENCODER_ATTACH:{
      return "encoderAttach";
    }
    case SET_ZERO_POINT:{
      return "setZeroPoint";
    }
    case PUBLISH_ENCODER_DATA:{
      return "publishEncoderData";
    }
    case PUBLISH_MRL_COMM_BEGIN:{
      return "publishMrlCommBegin";
    }
    case SERVO_STOP:{
      return "servoStop";
    }

    default: {
      return "ERROR UNKNOWN METHOD (" + Integer.toString(method) + ")";

    } // default
    }
  }

  public String str(int[] buffer, int start, int size) {
    byte[] b = new byte[size];
    for (int i = start; i < start + size; ++i){
      b[i - start] = (byte)(buffer[i] & 0xFF);
    }
    return new String(b);
  }

  public int[] subArray(int[] buffer, int start, int size) {    
    return Arrays.copyOfRange(buffer, start, start + size);
  }

  // signed 16 bit bucket
  public int b16(int[] buffer, int start/*=0*/) {
    return  (short)(buffer[start] << 8) + buffer[start + 1];
  }
  
  // signed 32 bit bucket
  public int b32(int[] buffer, int start/*=0*/) {
    return ((buffer[start + 0] << 24) + (buffer[start + 1] << 16)
        + (buffer[start + 2] << 8) + buffer[start + 3]);
  }
  
  // unsigned 32 bit bucket
  public long bu32(int[] buffer, int start/*=0*/) {
    long ret = ((buffer[start + 0] << 24)
        + (buffer[start + 1] << 16)
        + (buffer[start + 2] << 8) + buffer[start + 3]);
    if (ret < 0){
      return 4294967296L + ret;
    }
    
    return ret;
  }

  // float 32 bit bucket
  public float f32(int[] buffer, int start/*=0*/) {
    byte[] b = new byte[4];
    for (int i = 0; i < 4; ++i){
      b[i] = (byte)buffer[start + i];
    }
    float f = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getFloat();
    return f;
  }
  
  public void onBytes(byte[] bytes) {
    // TODO: This is a debug message only...
    String byteString = StringUtil.byteArrayToIntString(bytes);
    log.info("onBytes called byteCount: {} data: >{}<", byteCount, byteString);
    // this gives us the current full buffer that was read from the seral
    for (int i = 0 ; i < bytes.length; i++) {
      // For now, let's just call onByte for each byte upcasted as an int.
      Integer newByte = bytes[i] & 0xFF;
      try {
        /**
         * Archtype InputStream read - rxtxLib does not have this straightforward
         * design, but the details of how it behaves is is handled in the Serial
         * service and we are given a unified interface
         *
         * The "read()" is data taken from a blocking queue in the Serial service.
         * If we want to support blocking functions in Arduino then we'll
         * "publish" to our local queues
         */
        // TODO: consider reading more than 1 byte at a time ,and make this
        // callback onBytes or something like that.
        byteCount.incrementAndGet();
        
        // log.info("{} Byte Count {} MsgSize: {} On Byte: {}", i, byteCount, msgSize, newByte);
        // ++byteCount;
        if (log.isDebugEnabled()) {
          log.info("onByte {} \tbyteCount \t{}", newByte, byteCount);
        }
        if (byteCount.get() == 1) {
          if (newByte != MAGIC_NUMBER) {
            byteCount = new AtomicInteger(0);
            msgSize = 0;
            Arrays.fill(ioCmd, 0); // FIXME - optimize - remove
            // warn(String.format("Arduino->MRL error - bad magic number %d - %d rx errors", newByte, ++errorServiceToHardwareRxCnt));
            log.warn("Arduino->MRL error - bad magic number {} - {} rx errors", newByte, ++errorServiceToHardwareRxCnt);
            // dump.setLength(0);
          }
          continue;
        } else if (byteCount.get() == 2) {
          // get the size of message
          if (newByte > 64) {
            byteCount = new AtomicInteger(0);
            msgSize = 0;
            // This is an error scenario.. we should reset our byte count also.
            // error(String.format("Arduino->MRL error %d rx sz errors", ++errorServiceToHardwareRxCnt ));
            log.error("Arduino->MRL error {} rx sz errors", ++errorServiceToHardwareRxCnt);
            continue;
          }
          msgSize = newByte.intValue();
          // dump.append(String.format("MSG|SZ %d", msgSize));
        } else if (byteCount.get() == 3) {
          // This is the method..
          int method = newByte.intValue();
          // TODO: lookup the method in the label.. 
          if (!clearToSend) {
            // The only method we care about is begin!!!
            if (method != Msg.PUBLISH_MRL_COMM_BEGIN) {
              // This is a reset sort of scenario!  we should be killing our parser state
              // we are only looking for a begin message now!!
              byteCount = new AtomicInteger(0);
              msgSize = 0;
              continue;
            } else {
              // we're good to go.. maybe even clear to send at this point?
            }
          }
          if (methodToString(method).startsWith("ERROR")) {
            // we've got an error scenario here.. reset the parser and try again!
            log.error("Arduino->MRL error unknown method error. resetting parser.");
            byteCount = new AtomicInteger(0);
            msgSize = 0;
            if (isFullMessage(bytes)) {
              // TODO: This could be an infinite loop 
              // try to reprocess this byte array, maybe the parser got out of sync
              onBytes(bytes);
              return;
            }
            
          } else {
            ioCmd[byteCount.get() - 3] = method;
          }
        } else if (byteCount.get() > 3) {
          // remove header - fill msg data - (2) headbytes -1
          // (offset)
          // dump.append(String.format("|P%d %d", byteCount,
          // newByte));
          ioCmd[byteCount.get() - 3] = newByte.intValue();
        } else {
          // the case where byteCount is negative?! not got.
          log.warn("MRL error rx zero/negative size error: {} {}", byteCount, Arrays.copyOf(ioCmd, byteCount.get()));
          //error(String.format("Arduino->MRL error %d rx negsz errors", ++errorServiceToHardwareRxCnt));
          continue;
        }
        if (byteCount.get() == 2 + msgSize) {
          // we've received a full message
          int[] actualCommand = Arrays.copyOf(ioCmd, byteCount.get()-2);
          log.info("Full message received: {} Data:{}", VirtualMsg.methodToString(ioCmd[0]), actualCommand);
          // TODO: should we truncate our ioCmd that we send here?  the ioCmd array is larger than the message in almost all cases.
          // re-init the parser
          // TODO: this feels very thread un-safe!
          Arrays.fill(ioCmd, 0); // optimize remove
          // process the command.
          
          // This full command that we received. 
          
          processCommand(actualCommand);
          // we should only process this command if we are clear to sync.. 
          // if this is a begin command..  
//          if (!clearToSend) {
//            // if we're not clear to send.. we need to process this command
//            // only if it's a begin command.
//            if (isMrlCommBegin(actualCommand)) {
//              processCommand(actualCommand);
//            } else {
//              // reset the parser and attempt from the next byte
//              // TODO: check that it's not bytes.length-1
//              byte[] shiftedBytes = Arrays.copyOfRange(bytes, 1, bytes.length);
//              byteCount = new AtomicInteger(0);
//              onBytes(shiftedBytes);
//              return;
//            }
//            
//          } else {
//            processCommand(actualCommand);
//          }
          msgSize = 0;
          byteCount = new AtomicInteger(0);
          // Our 'first' getBoardInfo may not receive a acknowledgement
          // so this should be disabled until boadInfo is valid
          // clean up memory/buffers
          
        }
      } catch (Exception e) {
        ++errorHardwareToServiceRxCnt ;
        // error("msg structure violation %d", errorHardwareToServiceRxCnt);
        log.warn("msg_structure violation byteCount {} buffer {}", byteCount, Arrays.copyOf(ioCmd, byteCount.get()), e);
        // try again (clean up memory buffer)
        // Logging.logError(e);
        
        // perhpas we could find the first occurance of 170.. and then attempt to re-parse at that point.
        // find the first occurance of 170 in the bytes
        // subbytes
        // Maybe we can just walk the iterater back to the beginning based on the byte count .. and advance it by 1.. and continue.
        i = i - byteCount.get()+1;
        log.error("Trying to resume parsing the byte stream at position {} bytecount: {}", i, byteCount);
        log.error("Original Byte Array: {}", StringUtil.byteArrayToIntString(bytes));
        System.err.println("Try to consume more messages!");
        msgSize = 0;
        byteCount = new AtomicInteger(0);
        // attempt to reprocess from the beginngin
        // TODO: what about infinite loops!!!
        i = 0;
        return;
        
        
      }
    }
    // log.info("Done with onBytes method.");
    return;
  }

  String F(String msg) {
    return msg;
  }
  
  public void publishError(String error) {
    log.error(error);
  }
  
  void appendMessage(int b8) throws Exception {

    if ((b8 < 0) || (b8 > 255)) {
      log.error("writeByte overrun - should be  0 <= value <= 255 - value = {}", b8);
    }

        baos.write(b8 & 0xFF);
//    serial.write(b8 & 0xFF);
  }
  
  void startMessage() {
    baos = new ByteArrayOutputStream();
  }

  void appendMessagebool(boolean b1) throws Exception {
    if (b1) {
      appendMessage(1);
    } else {
      appendMessage(0);
    }
  }

  void appendMessageb16(int b16) throws Exception {
    if ((b16 < -32768) || (b16 > 32767)) {
      log.error("writeByte overrun - should be  -32,768 <= value <= 32,767 - value = {}", b16);
    }

    appendMessage(b16 >> 8 & 0xFF);
    appendMessage(b16 & 0xFF);
  }

  void appendMessageb32(int b32) throws Exception {
    appendMessage(b32 >> 24 & 0xFF);
    appendMessage(b32 >> 16 & 0xFF);
    appendMessage(b32 >> 8 & 0xFF);
    appendMessage(b32 & 0xFF);
  }
  
  void appendMessagef32(float f32) throws Exception {
    //  int x = Float.floatToIntBits(f32);
    byte[] f = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(f32).array();
    appendMessage(f[3] & 0xFF);
    appendMessage(f[2] & 0xFF);
    appendMessage(f[1] & 0xFF);
    appendMessage(f[0] & 0xFF);
  }
  
  void appendMessagebu32(long b32) throws Exception {
    appendMessage((int)(b32 >> 24 & 0xFF));
    appendMessage((int)(b32 >> 16 & 0xFF));
    appendMessage((int)(b32 >> 8 & 0xFF));
    appendMessage((int)(b32 & 0xFF));
  }

  void appendMessage(String str) throws Exception {
    appendMessage(str.getBytes());
  }

  void appendMessage(int[] array) throws Exception {
    // write size
    appendMessage(array.length & 0xFF);

    // write data
    for (int i = 0; i < array.length; ++i) {
      appendMessage(array[i] & 0xFF);
    }
  }

  void appendMessage(byte[] array) throws Exception {
    // write size
    appendMessage(array.length);

    // write data
    for (int i = 0; i < array.length; ++i) {
      appendMessage(array[i]);
    }
  }
  
  synchronized byte[] sendMessage() throws Exception {
    byte[] message = baos.toByteArray();
    if (ackEnabled) {
      // wait for any outstanding pending messages.
      while (pendingMessage) {
        Thread.sleep(1);
        log.info("Pending message");
      }
      // set a new pending flag.
      pendingMessage=true;
    }
    // write data if serial not null.
    if (serial != null) {
      serial.write(message);
    }
    return message;
  }
  
  public boolean isRecording() {
    return record != null;
  }
  

  public void record() throws Exception {
    
    if (record == null) {
      record = new FileOutputStream(String.format("%s.ard", arduino.getName()));
    }
  }

  public void stopRecording() {
    if (record != null) {
      try {
        record.close();
      } catch (Exception e) {
      }
      record = null;
    }
  }
  
  public static String deviceTypeToString(int typeId) {
    switch(typeId){
    case 0 :  {
      return "unknown";

    }
    case 1 :  {
      return "Arduino";

    }
    case 2 :  {
      return "UltrasonicSensor";

    }
    case 3 :  {
      return "Stepper";

    }
    case 4 :  {
      return "Motor";

    }
    case 5 :  {
      return "Servo";

    }
    case 6 :  {
      return "Serial";

    }
    case 7 :  {
      return "I2c";

    }
    case 8 :  {
      return "NeoPixel";

    }
    case 9 :  {
      return "Encoder";

    }
    
    default: {
      return "unknown";
    }
    }
  }
  
  public void enableAcks(boolean b){
    // disable local blocking
    ackEnabled = b;
    // if (!localOnly){
    // shutdown MrlComm from sending acks
    // below is a method only in Msg.java not in VirtualMsg.java
    // it depends on the definition of enableAck in arduinoMsg.schema  
    // enableAck(b);
    // }
  }
  
  public void waitForAck(){
    if (!ackEnabled || ackRecievedLock.acknowledged){
      return;
    }
    synchronized (ackRecievedLock) {
      try {
        long ts = System.currentTimeMillis();
        // log.info("***** starting wait *****");
        ackRecievedLock.wait(2000);
        // log.info("*****  waited {} ms *****", (System.currentTimeMillis() - ts));
      } catch (InterruptedException e) {// don't care}
      }

      if (!ackRecievedLock.acknowledged) {
        //log.error("Ack not received : {} {}", Msg.methodToString(ioCmd[0]), numAck);
        log.error("Ack not received");
        // part of resetting ?
        // ackRecievedLock.acknowledged = true;
        arduino.invoke("noAck");
      }
    }
  }
  
  public void ackReceived(int function){
     synchronized (ackRecievedLock) {
        ackRecievedLock.acknowledged = true;
        ackRecievedLock.notifyAll();
      }
  }
  
  public int getMethod(){
    return method;
  }
  

  public void add(int value) {
    sendBuffer[sendBufferSize] = (value & 0xFF);
    sendBufferSize += 1;
  }
  
  public int[] getBuffer() {    
    return sendBuffer;
  }
  
  public static void main(String[] args) {
    try {

      // FIXME - Test service started or reference retrieved
      // FIXME - subscribe to publishError
      // FIXME - check for any error
      // FIXME - basic design - expected state is connected and ready -
      // between classes it
      // should connect - also dumping serial comm at different levels so
      // virtual arduino in
      // Python can model "real" serial comm
      String port = "COM10";

      LoggingFactory.init(Level.INFO);
      
      /*
      Runtime.start("gui","SwingGui");
      VirtualArduino virtual = (VirtualArduino)Runtime.start("varduino","VirtualArduino");
      virtual.connectVirtualUart(port, port + "UART");
      */
      
      MrlComm arduino = (MrlComm)Runtime.start("arduino","MrlComm");
      Servo servo01 = (Servo)Runtime.start("servo01","Servo");
      
      /*
      arduino.connect(port);
      
      // test pins
      arduino.enablePin(5);
      
      arduino.disablePin(5);
      
      // test status list enabled
      arduino.enableBoardStatus(true);
      
      servo01.attach(arduino, 8);
      
      servo01.moveTo(30);
      servo01.moveTo(130);
      
      arduino.enableBoardStatus(false);
      */
      // test ack
      
      // test heartbeat
      
      

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  public void waitForBegin() {
    // poll until a begin MrlComm Message has been seen.
    log.info("Wait for Begin called in Msg.");
    while (!clearToSend ) {
      // TODO: don't sleep. rather notify
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        log.info("Wait for MrlCommBegin interrupted.", e);
      }
    }
  }

  public synchronized void onConnect(String portName) {
    // reset the parser...
    log.info("On Connect Called in VIRTUAL Msg.!!!!!!!!!!!!!!!!!!!!");
    this.byteCount = new AtomicInteger(0);
    this.msgSize = 0;
    // we're not clear to send.
    this.clearToSend = false;
    // watch for the first MrlCommBegin message;
    // TODO: we should have some sort of timeout / error handling here.
    // this.waitForBegin();
    
  }


  private boolean isMrlCommBegin(int[] actualCommand) {
    // TODO Auto-generated method stub
    int method = actualCommand[0];
    if (Msg.PUBLISH_MRL_COMM_BEGIN == method) {
      return true;
    }
    return false;
  }

  public static boolean isFullMessage(byte[] bytes) {
    // Criteria that a sequence of bytes could be parsed as a complete message.
    // can't be null
    if (bytes == null) 
      return false;
    // it's got to be at least 3 bytes long.  magic + method + size
    if (bytes.length <= 2) 
      return false;
    // first byte has to be magic
    if ((bytes[0] & 0xFF) != Msg.MAGIC_NUMBER) 
      return false;
    
    int method = bytes[1] & 0xFF;
    String strMethod = Msg.methodToString(method); 
    // only known methods. 
    // TODO: make the methodToString return null for an unknown lookup.
    if (strMethod.startsWith("ERROR")) 
      return false;
    
    // now it's got to be the proper length
    int length = bytes[1] & 0xFF;
    // max message size is 64 bytes
    if (length > 64)
      return false;

    // it's a exactly a full message or a message and more.
    if (bytes.length >= length+2)
      return true;

    
    return false;
  }

}
