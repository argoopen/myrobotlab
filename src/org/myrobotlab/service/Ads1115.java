package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.Ads1115Control;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.VoltageSensorControl;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * AdaFruit Ina219 Shield Controller Service
 * 
 * @author Mats
 * 
 *         References :
 *         https://learn.adafruit.com/adafruit-4-channel-adc-breakouts
 *         /programming The code here is to a large extent based on the Adafruit
 *         C++ libraries here: https://github.com/adafruit/Adafruit_ADS1X15
 * 
 *         Next follows the license agreement from Adafruit that this service is
 *         based on. It has been converted fron C++ to Java.
 * 
 *         Software License Agreement (BSD License)
 * 
 *         Copyright (c) 2012, Adafruit Industries All rights reserved.
 * 
 *         Redistribution and use in source and binary forms, with or without
 *         modification, are permitted provided that the following conditions
 *         are met: 1. Redistributions of source code must retain the above
 *         copyright notice, this list of conditions and the following
 *         disclaimer. 2. Redistributions in binary form must reproduce the
 *         above copyright notice, this list of conditions and the following
 *         disclaimer in the documentation and/or other materials provided with
 *         the distribution. 3. Neither the name of the copyright holders nor
 *         the names of its contributors may be used to endorse or promote
 *         products derived from this software without specific prior written
 *         permission.
 * 
 *         THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ''AS IS'' AND ANY
 *         EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *         IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *         PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
 *         LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *         CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *         SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *         BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *         WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *         OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *         EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class Ads1115 extends Service implements I2CControl, Ads1115Control {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Ads1115.class);
	transient I2CController controller;

	/*
	 * public static final byte INA219_SHUNTVOLTAGE = 0x01; public static final
	 * byte INA219_BUSVOLTAGE = 0x02;
	 */
	public List<String> deviceAddressList = Arrays.asList("0x48", "0x48",
			"0x4A", "0x4B");

	public String deviceAddress = "0x48";

	public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4",
			"5", "6", "7", "8");
	public String deviceBus = "1";

	public int adc0 = 0;
	public int adc1 = 0;
	public int adc2 = 0;
	public int adc3 = 0;

	public double voltage0 = 0;
	public double voltage1 = 0;
	public double voltage2 = 0;
	public double voltage3 = 0;

	private int m_conversionDelay;
	private int m_bitShift;
	private int m_gain = 0;
	private double m_fs = 0;

	public List<String> controllers;
	public String controllerName;
	private boolean isAttached = false;

	/*
	 * =========================================================================
	 * CONVERSION DELAY (in mS)
	 * -----------------------------------------------------------------------
	 */
	static byte ADS1015_CONVERSIONDELAY = 1;
	static byte ADS1115_CONVERSIONDELAY = 8;
	/* ========================================================================= */

	/*
	 * =========================================================================
	 * POINTER REGISTER
	 * -----------------------------------------------------------------------
	 */
	static byte ADS1015_REG_POINTER_MASK = 0x03;
	static byte ADS1015_REG_POINTER_CONVERT = 0x00;
	static byte ADS1015_REG_POINTER_CONFIG = 0x01;
	static byte ADS1015_REG_POINTER_LOWTHRESH = 0x02;
	static byte ADS1015_REG_POINTER_HITHRESH = 0x03;
	/* ========================================================================= */

	/*
	 * =========================================================================
	 * CONFIG REGISTER
	 * -----------------------------------------------------------------------
	 */
	static int ADS1015_REG_CONFIG_OS_MASK = 0x8000;
	static int ADS1015_REG_CONFIG_OS_SINGLE = 0x8000; // Write: Set to start a
														// single-conversion
	static int ADS1015_REG_CONFIG_OS_BUSY = 0x0000; // Read: Bit = 0 when
													// conversion is in progress
	static int ADS1015_REG_CONFIG_OS_NOTBUSY = 0x8000; // Read: Bit = 1 when
														// device is not
														// performing a
														// conversion

	static int ADS1015_REG_CONFIG_MUX_MASK = 0x7000;
	static int ADS1015_REG_CONFIG_MUX_DIFF_0_1 = 0x0000; // Differential P =
															// AIN0,
															// N = AIN1
															// (default;
	static int ADS1015_REG_CONFIG_MUX_DIFF_0_3 = 0x1000; // Differential P =
															// AIN0,
															// N = AIN3
	static int ADS1015_REG_CONFIG_MUX_DIFF_1_3 = 0x2000; // Differential P =
															// AIN1,
															// N = AIN3
	static int ADS1015_REG_CONFIG_MUX_DIFF_2_3 = 0x3000; // Differential P =
															// AIN2,
															// N = AIN3
	static int ADS1015_REG_CONFIG_MUX_SINGLE_0 = 0x4000; // Single-ended AIN0
	static int ADS1015_REG_CONFIG_MUX_SINGLE_1 = 0x5000; // Single-ended AIN1
	static int ADS1015_REG_CONFIG_MUX_SINGLE_2 = 0x6000; // Single-ended AIN2
	static int ADS1015_REG_CONFIG_MUX_SINGLE_3 = 0x7000; // Single-ended AIN3

	static int ADS1015_REG_CONFIG_PGA_MASK = 0x0E00;
	static final int ADS1015_REG_CONFIG_PGA_6_144V = 0x0000; // +/-6.144V range
																// = Gain
																// 2/3
	static final int ADS1015_REG_CONFIG_PGA_4_096V = 0x0200; // +/-4.096V range
																// = Gain 1
	static final int ADS1015_REG_CONFIG_PGA_2_048V = 0x0400; // +/-2.048V range
																// = Gain 2
																// (default)
	static final int ADS1015_REG_CONFIG_PGA_1_024V = 0x0600; // +/-1.024V range
																// = Gain 4
	static final int ADS1015_REG_CONFIG_PGA_0_512V = 0x0800; // +/-0.512V range
																// = Gain 8
	static final int ADS1015_REG_CONFIG_PGA_0_256V = 0x0A00; // +/-0.256V range
																// = Gain
																// 16

	static int ADS1015_REG_CONFIG_MODE_MASK = 0x0100;
	static int ADS1015_REG_CONFIG_MODE_CONTIN = 0x0000; // Continuous conversion
														// mode
	static int ADS1015_REG_CONFIG_MODE_SINGLE = 0x0100; // Power-down
														// single-shot
														// mode (default)

	static int ADS1015_REG_CONFIG_DR_MASK = 0x00E0;
	static int ADS1015_REG_CONFIG_DR_128SPS = 0x0000; // 128 samples per second
	static int ADS1015_REG_CONFIG_DR_250SPS = 0x0020; // 250 samples per second
	static int ADS1015_REG_CONFIG_DR_490SPS = 0x0040; // 490 samples per second
	static int ADS1015_REG_CONFIG_DR_920SPS = 0x0060; // 920 samples per second
	static int ADS1015_REG_CONFIG_DR_1600SPS = 0x0080; // 1600 samples per
														// second
														// (default)
	static int ADS1015_REG_CONFIG_DR_2400SPS = 0x00A0; // 2400 samples per
														// second
	static int ADS1015_REG_CONFIG_DR_3300SPS = 0x00C0; // 3300 samples per
														// second

	static int ADS1015_REG_CONFIG_CMODE_MASK = 0x0010;
	static int ADS1015_REG_CONFIG_CMODE_TRAD = 0x0000; // Traditional comparator
														// with hysteresis
														// (default)
	static int ADS1015_REG_CONFIG_CMODE_WINDOW = 0x0010; // Window comparator

	static int ADS1015_REG_CONFIG_CPOL_MASK = 0x0008;
	static int ADS1015_REG_CONFIG_CPOL_ACTVLOW = 0x0000; // ALERT/RDY pin is low
															// when active
															// (default)
	static int ADS1015_REG_CONFIG_CPOL_ACTVHI = 0x0008; // ALERT/RDY pin is high
														// when active

	static int ADS1015_REG_CONFIG_CLAT_MASK = 0x0004; // Determines if ALERT/RDY
														// pin latches once
														// asserted
	static int ADS1015_REG_CONFIG_CLAT_NONLAT = 0x0000; // Non-latching
														// comparator
														// (default)
	static int ADS1015_REG_CONFIG_CLAT_LATCH = 0x0004; // Latching comparator

	static int ADS1015_REG_CONFIG_CQUE_MASK = 0x0003;
	static int ADS1015_REG_CONFIG_CQUE_1CONV = 0x0000; // Assert ALERT/RDY after
														// one conversions
	static int ADS1015_REG_CONFIG_CQUE_2CONV = 0x0001; // Assert ALERT/RDY after
														// two conversions
	static int ADS1015_REG_CONFIG_CQUE_4CONV = 0x0002; // Assert ALERT/RDY after
														// four conversions
	static int ADS1015_REG_CONFIG_CQUE_NONE = 0x0003; // Disable the comparator
														// and put ALERT/RDY in
														// high
														// state (default)

	static int GAIN_TWOTHIRDS = ADS1015_REG_CONFIG_PGA_6_144V;
	static int GAIN_ONE = ADS1015_REG_CONFIG_PGA_4_096V;
	static int GAIN_TWO = ADS1015_REG_CONFIG_PGA_2_048V;
	static int GAIN_FOUR = ADS1015_REG_CONFIG_PGA_1_024V;
	static int GAIN_EIGHT = ADS1015_REG_CONFIG_PGA_0_512V;
	static int GAIN_SIXTEEN = ADS1015_REG_CONFIG_PGA_0_256V;

	/* ========================================================================= */
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			Ads1115 ads1115 = (Ads1115) Runtime.start("Ads1115", "Ads1115");
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public Ads1115(String n) {
		super(n);
		refreshControllers();
		subscribe(Runtime.getInstance().getName(), "registered",
				this.getName(), "onRegistered");
		init_ADS1115();
	}

	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		broadcastState();

	}

	public List<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(I2CController.class);
		return controllers;
	}

	/**
	 * This methods sets the i2c Controller that will be used to communicate
	 * with the i2c device
	 */
	// @Override
	public boolean setController(String controllerName, String deviceBus,
			String deviceAddress) {
		return setController(
				(I2CController) Runtime.getService(controllerName), deviceBus,
				deviceAddress);
	}

	public boolean setController(String controllerName) {
		return setController(
				(I2CController) Runtime.getService(controllerName),
				this.deviceBus, this.deviceAddress);
	}

	public boolean setController(I2CController controller) {
		return setController(controller, this.deviceBus, this.deviceAddress);
	}

	@Override
	public void setController(DeviceController controller) {
		setController(controller);
	}

	/**
	 * This methods sets the i2c Controller that will be used to communicate
	 * with the i2c device
	 */
	public boolean setController(I2CController controller, String deviceBus,
			String deviceAddress) {
		if (controller == null) {
			error("setting null as controller");
			return false;
		}
		controllerName = controller.getName();
		this.controller = controller;
		this.deviceBus = deviceBus;
		this.deviceAddress = deviceAddress;
		createDevice();
		isAttached = true;

		log.info(String
				.format("%s setController %s", getName(), controllerName));

		broadcastState();
		return true;
	}

	public void unsetController() {
		controller = null;
		controllerName = null;
		this.deviceBus = null;
		this.deviceAddress = null;
		isAttached = false;
		broadcastState();
	}

	public I2CController getController() {
		return controller;
	}

	/**
	 * This method creates the i2c device
	 */
	boolean createDevice() {
		if (controller != null) {
			// controller.releaseI2cDevice(this, Integer.parseInt(deviceBus),
			// Integer.decode(deviceAddress));
			controller.createI2cDevice(this, Integer.parseInt(deviceBus),
					Integer.decode(deviceAddress));
		}

		log.info(String.format("Creating device on bus: %s address %s",
				deviceBus, deviceAddress));
		return true;
	}

	public String getControllerName() {

		String controlerName = null;

		if (controller != null) {
			controlerName = controller.getName();
		}

		return controlerName;
	}

	public void setDeviceBus(String deviceBus) {
		this.deviceBus = deviceBus;
		broadcastState();
	}

	public boolean SetDeviceAddress(String deviceAddress) {
		this.deviceAddress = deviceAddress;
		broadcastState();
		return true;
	}

	/**
	 * This method creates the i2c device
	 */
	public void setDeviceAddress(String DeviceAddress) {
		if (controller != null) {
			if (deviceAddress != DeviceAddress) {
				controller.releaseI2cDevice(this, Integer.parseInt(deviceBus),
						Integer.decode(deviceAddress));
				controller.createI2cDevice(this, Integer.parseInt(deviceBus),
						Integer.decode(deviceAddress));
			}
		}

		log.info(String.format("Setting device address to %s", deviceAddress));
		this.deviceAddress = DeviceAddress;
	}

	/**
	 * This method reads all four A/D pins
	 */
	public void refresh() {

		adc0 = readADC_SingleEnded(0);
		adc1 = readADC_SingleEnded(1);
		adc2 = readADC_SingleEnded(2);
		adc3 = readADC_SingleEnded(3);
		convertToVoltage();
		broadcastState();
	}

	void convertToVoltage() {
		int max = 256 * 128;
		voltage0 = adc0 * m_fs / max;
		voltage1 = adc1 * m_fs / max;
		voltage2 = adc2 * m_fs / max;
		voltage3 = adc3 * m_fs / max;

	}

	/**
	 * This method initiates the ADS1015
	 */
	public void init_ADS1015() {
		m_conversionDelay = ADS1015_CONVERSIONDELAY;
		m_bitShift = 4;
		m_gain = GAIN_TWOTHIRDS; /* +/- 6.144V range (limited to VDD +0.3V max!) */
		this.m_fs = 6.144;
	}

	/**
	 * This method initiates the ADS1115
	 */
	public void init_ADS1115() {
		m_conversionDelay = ADS1115_CONVERSIONDELAY;
		m_bitShift = 0;
		m_gain = GAIN_TWOTHIRDS; /* +/- 6.144V range (limited to VDD +0.3V max!) */
		m_fs = 6.144;
	}

	/**
	 * ************************************************************************
	 *
	 * Sets the gain and input voltage range
	 *
	 * /
	 **************************************************************************/
	public void setGain(int gain) {
		m_gain = gain;
		switch (gain) {
		case ADS1015_REG_CONFIG_PGA_6_144V:
			m_fs = 6.144;
			break;
		case ADS1015_REG_CONFIG_PGA_4_096V:
			m_fs = 4.096;
			break;
		case ADS1015_REG_CONFIG_PGA_2_048V:
			m_fs = 2.096;
			break;
		case ADS1015_REG_CONFIG_PGA_1_024V:
			m_fs = 1.024;
			break;
		case ADS1015_REG_CONFIG_PGA_0_512V:
			m_fs = 0.512;
			break;
		case ADS1015_REG_CONFIG_PGA_0_256V:
			m_fs = 0.256;
			break;
		}
	}

	/**
	 * ************************************************************************
	 * 
	 * Gets a gain and input voltage range
	 * 
	 * /
	 **************************************************************************/
	public int getGain() {
		return m_gain;
	}

	/**
	 * This method reads and returns the Voltage in milliVolts
	 */
	public int readADC_SingleEnded(int channel) {
		if (channel > 3) {
			return 0;
		}
		// Start with default values
		int config = ADS1015_REG_CONFIG_CQUE_NONE | // Disable the comparator
													// (default val)
				ADS1015_REG_CONFIG_CLAT_NONLAT | // Non-latching (default val)
				ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low
													// (default val)
				ADS1015_REG_CONFIG_CMODE_TRAD | // Traditional comparator
												// (default val)
				ADS1015_REG_CONFIG_DR_1600SPS | // 1600 samples per second
												// (default)
				ADS1015_REG_CONFIG_MODE_SINGLE; // Single-shot mode (default)

		// Set PGA/voltage range
		config |= m_gain;

		switch (channel) {
		case 0:
			config |= ADS1015_REG_CONFIG_MUX_SINGLE_0;
			break;
		case 1:
			config |= ADS1015_REG_CONFIG_MUX_SINGLE_1;
			break;
		case 2:
			config |= ADS1015_REG_CONFIG_MUX_SINGLE_2;
			break;
		case 3:
			config |= ADS1015_REG_CONFIG_MUX_SINGLE_3;
			break;
		}
		// Set 'start single-conversion' bit
		config |= ADS1015_REG_CONFIG_OS_SINGLE;

		// Write config register to the ADC
		writeRegister(ADS1015_REG_POINTER_CONFIG, config);

		// Wait for the conversion to complete
		sleep(m_conversionDelay);

		// Read the conversion results
		// Shift 12-bit results right 4 bits for the ADS1015
		return readRegister(ADS1015_REG_POINTER_CONVERT) >> m_bitShift;
	}

	/**************************************************************************/
	/*
	 * !
	 * 
	 * @brief Reads the conversion results, measuring the voltage difference
	 * between the P (AIN0) and N (AIN1) input. Generates a signed value since
	 * the difference can be either positive or negative.
	 */
	/**************************************************************************/
	public int readADC_Differential_0_1() {
		// Start with default values
		int config = ADS1015_REG_CONFIG_CQUE_NONE | // Disable the comparator
													// (default val)
				ADS1015_REG_CONFIG_CLAT_NONLAT | // Non-latching (default val)
				ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low
													// (default val)
				ADS1015_REG_CONFIG_CMODE_TRAD | // Traditional comparator
												// (default val)
				ADS1015_REG_CONFIG_DR_1600SPS | // 1600 samples per second
												// (default)
				ADS1015_REG_CONFIG_MODE_SINGLE; // Single-shot mode (default)

		// Set PGA/voltage range
		config |= m_gain;

		// Set channels
		config |= ADS1015_REG_CONFIG_MUX_DIFF_0_1; // AIN0 = P, AIN1 = N

		// Set 'start single-conversion' bit
		config |= ADS1015_REG_CONFIG_OS_SINGLE;

		// Write config register to the ADC
		writeRegister(ADS1015_REG_POINTER_CONFIG, config);

		// Wait for the conversion to complete
		sleep(m_conversionDelay);

		// Read the conversion results
		int res = readRegister(ADS1015_REG_POINTER_CONVERT);
		if (m_bitShift == 0) {
			return res;
		} else {
			// Shift 12-bit results right 4 bits for the ADS1015,
			// making sure we keep the sign bit intact
			return res / 16;
		}
	}

	/**************************************************************************/
	/*
	 * !
	 * 
	 * @brief Reads the conversion results, measuring the voltage difference
	 * between the P (AIN2) and N (AIN3) input. Generates a signed value since
	 * the difference can be either positive or negative.
	 */
	/**************************************************************************/
	public int readADC_Differential_2_3() {
		// Start with default values
		int config = ADS1015_REG_CONFIG_CQUE_NONE | // Disable the comparator
													// (default val)
				ADS1015_REG_CONFIG_CLAT_NONLAT | // Non-latching (default val)
				ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low
													// (default val)
				ADS1015_REG_CONFIG_CMODE_TRAD | // Traditional comparator
												// (default val)
				ADS1015_REG_CONFIG_DR_1600SPS | // 1600 samples per second
												// (default)
				ADS1015_REG_CONFIG_MODE_SINGLE; // Single-shot mode (default)

		// Set PGA/voltage range
		config |= m_gain;

		// Set channels
		config |= ADS1015_REG_CONFIG_MUX_DIFF_2_3; // AIN2 = P, AIN3 = N

		// Set 'start single-conversion' bit
		config |= ADS1015_REG_CONFIG_OS_SINGLE;

		// Write config register to the ADC
		writeRegister(ADS1015_REG_POINTER_CONFIG, config);

		// Wait for the conversion to complete
		sleep(m_conversionDelay);

		// Read the conversion results
		int res = readRegister(ADS1015_REG_POINTER_CONVERT) >> m_bitShift;
		if (m_bitShift == 0) {
			return res;
		} else {
			// Shift 12-bit results right 4 bits for the ADS1015,
			// making sure we keep the sign bit intact
			return res / 16;
		}
	}

	/**************************************************************************/
	/*
	 * !
	 * 
	 * @brief Sets up the comparator to operate in basic mode, causing the
	 * ALERT/RDY pin to assert (go from high to low) when the ADC value exceeds
	 * the specified threshold. This will also set the ADC in continuous
	 * conversion mode.
	 */
	/**************************************************************************/
	public void startComparator_SingleEnded(int channel, int threshold) {
		// Start with default values
		int config = ADS1015_REG_CONFIG_CQUE_1CONV | // Comparator enabled and
														// asserts on 1 match
				ADS1015_REG_CONFIG_CLAT_LATCH | // Latching mode
				ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low
													// (default val)
				ADS1015_REG_CONFIG_CMODE_TRAD | // Traditional comparator
												// (default val)
				ADS1015_REG_CONFIG_DR_1600SPS | // 1600 samples per second
												// (default)
				ADS1015_REG_CONFIG_MODE_CONTIN | // Continuous conversion mode
				ADS1015_REG_CONFIG_MODE_CONTIN; // Continuous conversion mode

		// Set PGA/voltage range
		config |= m_gain;

		// Set single-ended input channel
		switch (channel) {
		case (0):
			config |= ADS1015_REG_CONFIG_MUX_SINGLE_0;
			break;
		case (1):
			config |= ADS1015_REG_CONFIG_MUX_SINGLE_1;
			break;
		case (2):
			config |= ADS1015_REG_CONFIG_MUX_SINGLE_2;
			break;
		case (3):
			config |= ADS1015_REG_CONFIG_MUX_SINGLE_3;
			break;
		}

		// Set the high threshold register
		// Shift 12-bit results left 4 bits for the ADS1015
		writeRegister(ADS1015_REG_POINTER_HITHRESH, threshold << m_bitShift);

		// Write config register to the ADC
		writeRegister(ADS1015_REG_POINTER_CONFIG, config);
	}

	/**************************************************************************/
	/*
	 * !
	 * 
	 * @brief In order to clear the comparator, we need to read the conversion
	 * results. This function reads the last conversion results without changing
	 * the config value.
	 */
	/**************************************************************************/
	public int getLastConversionResults() {
		// Wait for the conversion to complete
		sleep(ADS1115_CONVERSIONDELAY);

		// Read the conversion results
		int res = readRegister(ADS1015_REG_POINTER_CONVERT) >> m_bitShift;
		if (m_bitShift == 0) {
			return res;
		} else {
			// Shift 12-bit results right 4 bits for the ADS1015,
			// making sure we keep the sign bit intact
			return res / 16;
		}
	}

	void i2cWrite(int reg) {
		byte[] writebuffer = { (byte) reg };
		controller.i2cWrite(this, Integer.parseInt(deviceBus),
				Integer.decode(deviceAddress), writebuffer, writebuffer.length);
	}

	void writeRegister(int reg, int value) {
		byte[] writebuffer = { (byte) reg, (byte) (value >> 8),
				(byte) (value & 0xff) };
		controller.i2cWrite(this, Integer.parseInt(deviceBus),
				Integer.decode(deviceAddress), writebuffer, writebuffer.length);
	}

	int readRegister(int reg) {
		i2cWrite(ADS1015_REG_POINTER_CONVERT);
		byte[] readbuffer = new byte[2];
		controller.i2cRead(this, Integer.parseInt(deviceBus),
				Integer.decode(deviceAddress), readbuffer, readbuffer.length);
		return ((int) readbuffer[0]) << 8 | (int) (readbuffer[1] & 0xff);
	}

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(Ads1115.class.getCanonicalName());
		meta.addDescription("Adafruit ADS1115 AD Converter");
		meta.addCategory("shield", "sensor");
		meta.setSponsor("Mats");
		return meta;
	}

	@Override
	public boolean isAttached() {
		// TODO Auto-generated method stub
		return isAttached;
	}

}
