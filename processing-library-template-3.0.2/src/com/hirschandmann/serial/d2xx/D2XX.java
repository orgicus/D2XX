package com.hirschandmann.serial.d2xx;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import com.ftdichip.ftd2xx.*;
import com.ftdichip.ftd2xx.Device;

import processing.core.*;

/**
 * Reads and writes serial data using the D2XX high speed serial communication library 
 *
 * @example Write 
 */

public class D2XX {
	
	// reference to the parent sketch
	PApplet parent;
	private boolean nativeLoaded;
	private static boolean isArm = false;
	private Device[] devices;
	private Device dev;
	private int portIndex;
	private static String PATH_SEPARATOR = System.getProperty("Path.separator");
	private static String FILE_SEPARATOR = File.separator;
	
	public final static String VERSION = "##library.prettyVersion##";
	
	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the Library.
	 * 
	 * @example Write
	 * @example ListDevices
	 * 
	 * @param parent 	- the parent sketch
	 * @param portIndex	- the port index to open (depends on how many are available)
	 * @param baudRate	- how many bauds per second should it use for communication
	 */
	public D2XX(PApplet parent,int portIndex,int baudRate) {
		this.parent = parent;
		this.portIndex = portIndex;
		initNative();
		openDevice();
	}
	
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
	
	// Code heavily 'inspired by' OpenCV's platform specific library initialising: 
	// https://github.com/atduskgreg/opencv-processing/blob/master/src/gab/opencv/OpenCV.java#L395
	private void initNative(){
		if (!nativeLoaded){
			int bitsJVM = this.parent.parseInt(System.getProperty("sun.arch.data.model"));
			String osArch = System.getProperty("os.arch");
			String nativeLibPath = getLibPath();
			String path = null;
			String fileName = null;
			
			if (this.parent.platform == PConstants.WINDOWS){ // If running on a Windows platform
				switch(bitsJVM) {
				case 32:
					fileName = "ftd2xx";
					path = nativeLibPath + "windows" + bitsJVM;
					break;
				case 64:
					fileName = "ftd2xx64";
					path = nativeLibPath + "windows" + bitsJVM;
					break;
				}
				path = path.replaceAll("//", FILE_SEPARATOR);
			}
			if (this.parent.platform == PConstants.MACOSX){ // if running on Mac platform
				fileName = "ftdxx.1.2.2";
				path = nativeLibPath + "macosx" + bitsJVM;
			}
			if (this.parent.platform == PConstants.LINUX){ // if running on Linux platform
				isArm = osArch.contains("arm");
				fileName = "ftd2xxj";
				PATH_SEPARATOR = ":";
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
			}
			
			// add library path to java.library.path
			try {
				addLibraryPath(nativeLibPath);
			} catch (Exception e){
				e.printStackTrace();
			}
			
			// load the desired library
			try {
				System.loadLibrary(fileName);
			} catch (UnsatisfiedLinkError e) {
				e.printStackTrace();
			}
			nativeLoaded = true;
		}
	}
	
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

	public void listDevices(){
		try {
			devices = Service.listDevicesByType(DeviceType.FT_DEVICE_UNKNOWN);
			System.out.println("Number of devices: " + devices.length);
			for (int x = 0; x < devices.length; x++){
				System.out.println(devices[x]);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void openDevice(){
		listDevices();
		int numDevices = devices.length;
        if(numDevices > 0 && portIndex < numDevices){
            dev = devices[portIndex];
            try{
                dev.open();
                Port port = dev.getPort();
                port.setBaudRate(4000000);
                port.setDataCharacteristics(DataBits.DATA_BITS_8, StopBits.STOP_BITS_2, Parity.NONE);
            }catch(Exception e){
                System.out.println("caught:");
                e.printStackTrace();
            }
        }
	}
	
	public void sendBytes(byte[] packet) {
        if(dev != null) {
            try {
                dev.write(packet);
            } catch (FTD2xxException e) {
                e.printStackTrace();
            }
        }
    }
	
	/**
	 * return the version of the Library.
	 * 
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}

}

