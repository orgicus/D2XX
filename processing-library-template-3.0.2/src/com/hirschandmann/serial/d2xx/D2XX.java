package com.hirschandmann.serial.d2xx;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;

import javax.print.DocFlavor.BYTE_ARRAY;

import com.ftdichip.ftd2xx.*;
import com.ftdichip.ftd2xx.Device;

import processing.core.*;

/**
 * Reads and writes serial data using the D2XX high speed serial communication library 
 *
 * @example Write
 * @example ListDevices
 * @example WriteToSunspots 
 */

public class D2XX implements Runnable{
	
	// reference to the parent sketch
	PApplet parent;
	
	// Native platform initialising variables
	private boolean nativeLoaded;
	private static boolean isArm = false;
	private static String PATH_SEPARATOR = System.getProperty("Path.separator");
	private static String FILE_SEPARATOR = File.separator;

	// Device variables
	private static Device[] devices;
	private Device dev;
	private Port port;
	private int portIndex;
	private int baudRate;
	private boolean isOpen;
	private DataBits dataBits = DataBits.DATA_BITS_8;
	private StopBits stopBits = StopBits.STOP_BITS_1;
	private Parity parity 	  = Parity.NONE;
	
	// Packet variables
	private int byteToWrite = -1;
	private byte[] bytesToWrite;
	private int writeOffset = -1;
	private int writeLength = -1;

	private final int NO_DATA = 0;
	private final int BYTE = 1;
	private final int BYTE_ARRAY = 2;
	private final int BYTES_OFFSET = 3;
	// Toggle to trigger writing a particular type of data packet
	private int hasNewData = NO_DATA;

	// Running logic variables
	private boolean threadActive = true;
	private static final int SLEEP_TIME = 3;
	private static boolean returnCachedDeviceList = false;
	
	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the Library.
	 * 
	 * @example Write
	 * @example ListDevices
	 * @example WriteToSunspots
	 * 
	 * @param parent 	- the parent sketch
	 * @param portIndex	- the port index to open (depends on how many are available)
	 * @param baudRate	- how many bauds per second should it use for communication
	 */
	public D2XX(PApplet parent,int portIndex,int baudRate) {
		if (parent != null && portIndex >= 0 && baudRate != 0){
			this.parent = parent;
			this.portIndex = portIndex;
			this.baudRate = baudRate;
			initNative();
			
		    new Thread(this).start();
		    
			this.devices = listDevices();
			if(devices.length > 0 && portIndex < devices.length){
				if (openDevice()){
					isOpen = true;
					System.out.println("Device successfully openend");
				}
				try {
					this.parent.registerMethod("dispose", this);
				} catch (Exception e){
					e.printStackTrace();
				}
			} else {
				System.err.println("Trying to initialise with a portIndex larger than available ports!");
			}
		} else {
			System.err.println("Trying to initialise with null args!");
		}
	}

	
	/**
	 * This method scans and returns a list of connected serial 
	 * devices of type - "unknown" 
	 * 
	 * @return Devices[] - the list of devices discovered
	 */
	private static Device[] listDevices(){
		Device[] deviceList = null;
		if (!returnCachedDeviceList){			
			try {
				deviceList = Service.listDevicesByType(DeviceType.FT_DEVICE_UNKNOWN);
				returnCachedDeviceList = true;
				System.out.println("Number of devices: " + deviceList.length);
				for (int x = 0; x < deviceList.length; x++){
					System.out.println(deviceList[x]);
				}
			} catch (Exception e){
				e.printStackTrace();
			}
		} else {
			deviceList = devices;
		}
		return deviceList;
	}

	/** Finds the requested device in the device list and attempts
	 *  to open a connection. 
	 * 
	 * @return boolean - returns whether or not the suggested device 
	 * 					 was successfully opened 
	 */
	public boolean openDevice(){
		boolean openingSuccess = false;
		int numDevices = devices.length;
		if (nativeLoaded){
			if(numDevices > 0 && portIndex < numDevices){
				dev = devices[portIndex];
				try{
					dev.open();
					port = dev.getPort();
					port.setBaudRate(baudRate);
					
//					port.setDataCharacteristics(dataBits, stopBits, parity);
					port.setDataCharacteristics(DataBits.DATA_BITS_8, StopBits.STOP_BITS_2, Parity.NONE);

					
					openingSuccess = true;
				}catch(Exception e){
					System.out.println("caught:");
					e.printStackTrace();
					openingSuccess = false;
				}
			} else {
				System.err.println("Trying to initialise with a portIndex larger than available ports!");
				openingSuccess = false;
			}
		}
		return openingSuccess;
	}

	/**Method to change the DataBits, StopBits and parity of the connection. 
	 * 
	 * @param newDataBits 
	 * @param newStopBits
	 * @param newParity
	 */
	public void setDataCharacteristics(DataBits newDataBits, StopBits newStopBits, Parity newParity){
		if (newDataBits != null && newStopBits != null && newParity != null){
			dataBits = newDataBits;
			stopBits = newStopBits;
			parity = newParity;
			try {
				port.setDataCharacteristics(dataBits, stopBits, parity);			
			} catch(FTD2xxException e){
				e.printStackTrace();
			}
		}
	}
	
	/**Sending an integer to the connected device
	 * 
	 * @param bytes
	 */
	public void write(int bytes){
		if (bytes > 0){
			byteToWrite = constrain(bytes, 0, 255);
			hasNewData = BYTE;
		} else {
			System.err.println("Attempting to write null bytes!");
		}
	}
	
	/** Sending an array of bytes to the connected device
	 * 
	 * @param bytes
	 */
	public void write(byte[] bytes){
		if (bytes != null){
			bytesToWrite = bytes;
			hasNewData = BYTE_ARRAY;			
		} else {
			System.err.println("Attempting to write null bytes!");
		}
	}
	
	/**Sending a particular selection of an array of bytes to the
	 * connected device.
	 * 
	 * @param buffer
	 * @param offset
	 * @param length
	 */
	public void write(byte[] buffer, int offset, int length){
		if (buffer != null && offset > 0 && length > 0){
			bytesToWrite = buffer;
			writeOffset = offset;
			writeLength = length;
			hasNewData = BYTES_OFFSET;
		}else{
			System.err.println("Attempting to write null information!");
		}
	}
	
	/** Reading available data from the connected device
	 * 
	 * @return int - the read data
	 */
	public int read(){
		int readData = 0;
		try {
			readData = dev.read();
		} catch(FTD2xxException e){
			e.printStackTrace();
		}
		return readData;
	}
	
	/** Reading a target amount of the available data from the connected device
	 * 
	 * @param target
	 * @return int - the read data
	 */
	public int read(byte[] target){
		int readData = 0;
		try {
			readData = dev.read(target);
		} catch(FTD2xxException e){
			e.printStackTrace();
		}
		return readData;
	}
	
	/** Reading a specific amount of the targeted data from the connected device
	 * 
	 * 
	 * @param buffer
	 * @param offset
	 * @param length
	 * @return int - the read data
	 */
	public int read(byte[] buffer, int offset, int length){
		int readData = 0;
		try {
			readData = dev.read(buffer, offset, length);
		}catch(FTD2xxException e){
			e.printStackTrace();
		}
		return readData;
	}
	
	/** Returns the connection status of the device
	 * 
	 * @return boolean - is the device connection open
	 */
	public boolean isOpen(){
		return isOpen;
	}
	
	/** Main method that runs continuously in it's own thread. 
	 *  If there is data ready to send it checks the type of data packet
	 *  and sends it to the connected device
	 */
	public void run(){
		while (threadActive){
			if (hasNewData != NO_DATA){
				if (dev != null){
					if (hasNewData == BYTE){
						try {
							dev.write(byteToWrite);
							byteToWrite = -1;
						} catch (FTD2xxException e){
							e.printStackTrace();
						}
					} else if (hasNewData == BYTE_ARRAY){
						try {
							dev.write(bytesToWrite);
							bytesToWrite = null;
						} catch (FTD2xxException e){
							e.printStackTrace();
						}
					} else if (hasNewData == BYTES_OFFSET){
						try {
							dev.write(bytesToWrite, writeOffset, writeLength);
							bytesToWrite = null;
							writeOffset = -1;
							writeLength = -1;
						} catch (FTD2xxException e){
							e.printStackTrace();
						}
					}
				} else {
					System.err.println("Trying to send data to null device!");
				}
				hasNewData = NO_DATA;
			}
			try{			
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e){
				throw new IllegalStateException(e);
			}
		}
	}
	
	/**
	 *  Closes connection to the device on shutdown of the program
	 */
	public void dispose(){
		try {			
			dev.close();
			isOpen = false;
			System.out.println("Connection closed!");
		} catch (FTD2xxException e){
			e.printStackTrace();
		}
	}
	
	/** A handy function to constrain a value's number between a max and min
	 * 
	 * @param val
	 * @param min
	 * @param max
	 * @return
	 */
	private int constrain(int val, int min, int max){
		return Math.min(Math.max(val, min), max);
	}
	
	/** A method to remove any conflicting native USB serial drivers
	 * 
	 */
	private void removeDrivers(){
		if (this.parent.platform == PConstants.LINUX){
			this.parent.exec("sudo", "rmmod", "ftdi_sio");
			this.parent.exec("sudo", "rmmod", "usbserial");
		} else if (this.parent.platform == PConstants.MACOSX){
			this.parent.exec("sudo", "kextunload", "-b","com.apple.AppleFTDISerial");
		}
	}
	
	/** A function to load the appropriate ftd2xx library based on the
	 * platform's operating system. Currently set up for macosx64, windows32, windows64, arm6 and arm7
	 * 
	 * initNative, getLibPath and addLibraryPath functions heavily 'inspired by' OpenCV's platform
	 * specific library loading:
	 * https://github.com/atduskgreg/opencv-processing/blob/master/src/gab/opencv/OpenCV.java#L395
	 */
	private void initNative(){
		if (!nativeLoaded){
			int bitsJVM = this.parent.parseInt(System.getProperty("sun.arch.data.model"));
			String osArch = System.getProperty("os.arch");
			String nativeLibPath = getLibPath();
			String path = null;
			String fileName = null;
			
			if (this.parent.platform == PConstants.WINDOWS){ // If running on a Windows platform
				path = nativeLibPath + "windows" + bitsJVM;
				fileName = "ftd2xx";
				path = path.replaceAll("//", FILE_SEPARATOR);
			}
			if (this.parent.platform == PConstants.MACOSX){ // if running on Mac platform
				removeDrivers();
				fileName = "ftd2xxj";
				path = nativeLibPath + "macosx" + bitsJVM;
			}
			if (this.parent.platform == PConstants.LINUX){ // if running on Linux platform
				isArm = osArch.contains("arm");
				fileName = "ftd2xxj";
				PATH_SEPARATOR = ":";

				if (isArm){
					// removing native usb serial drivers
					removeDrivers();
					// RPi solution to not have a dependacy on libraries in /usr/local/lib/
					System.load(nativeLibPath + "arm7/libftd2xx.so");
				}
				
				path = isArm ? nativeLibPath + "arm7" : nativeLibPath + "linux" + bitsJVM;
			}
			// make sure the determined path exists
			try {
				File libDir = new File(path);
				if (libDir.exists()) {
					nativeLibPath = path;
				}
			} catch (NullPointerException e){
				System.err.println("Cannot load local version of D2XX!");
				e.printStackTrace();
				nativeLoaded = false;
			}
			// add library path to java.library.path
			try {
				addLibraryPath(nativeLibPath);
			} catch (Exception e){
				e.printStackTrace();
				nativeLoaded = false;
			}
			// load the desired library
			try {
				System.loadLibrary(fileName);
				nativeLoaded = true;
			} catch (UnsatisfiedLinkError e) {
				e.printStackTrace();
				nativeLoaded = false;
			}
		}
	}
	
	/** Returns the path to the current operating directory of this library
	 * 
	 * @return String - path to the current operating directory of this library
	 */
	private String getLibPath() {
		URL url = this.getClass().getResource("D2XX.class");
		if (url!= null){
			String path = url.toString().replaceAll("%20", " ");
			int n0 = path.indexOf('/');
			int n1 = -1;
			n1 = path.indexOf("D2XX.jar");
			if (this.parent.platform == PConstants.WINDOWS){
				// In Windows, path string starts with "jar file/C:/.."
				// so the substring up to  the first / is removed
				n0++;
			}
			if ((-1 < n0) && (-1 < n1)){
				return path.substring(n0, n1);
			} else {
				return "";
			}
		}
		return "";
	}
	
	/** Adds the path determined by initNative() to the java.library.path
	 * 
	 * @param path - the path to be added to java.library.path
	 * @throws Exception 
	 */
	private static void addLibraryPath(String path) throws Exception {
		String originalPath = System.getProperty("java.library.path");
		
		if (isArm) {
			if (originalPath.indexOf("linux32") != -1) {
				originalPath = originalPath.replaceAll(":[^:]*?linux32", "");
			}
		}
		try {
			System.setProperty("java.library.path", originalPath + PATH_SEPARATOR + path);
		} catch (Exception e){
			e.printStackTrace();
		}
		//set sys_paths to null
		final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
		sysPathsField.setAccessible(true);
		sysPathsField.set(null, null);
	}	
}
