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
	private boolean isArm = false;
	private Device[] devices;
	private Device dev;
	
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
		initNative();
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
			// 64 or 32
			int bitsJVM = this.parent.parseInt(System.getProperty("sun.arch.data.model"));
			// returns 'x86_64' on Adam's mac
			String osArch = System.getProperty("os.arch");
			String nativeLibPath = getLibPath();
			System.out.println("original nativeLibPath: " + nativeLibPath);
			String path = null;
			
			if (this.parent.platform == PConstants.WINDOWS){ // If running on a Windows platform
				switch(bitsJVM) {
				case 32:
					path = nativeLibPath + "windows" + bitsJVM + File.separator + "ftd2xxj.dll";
					System.out.println("platform: windows path: " + path);
					break;
				case 64:
					path = nativeLibPath + "windows" + bitsJVM + File.separator + "ftd2xxj.dll";
					System.out.println("platform: windows path: " + path);
					break;
				}
				path = path.replaceAll("/", File.separator);
			}
			if (this.parent.platform == PConstants.MACOSX){ // if running on Mac platform
				path = nativeLibPath + "macosx" + bitsJVM + File.separator + "libftdxx.1.2.2.dylib";
				System.out.println("platform: mac path: " + path);
			}
			if (this.parent.platform == PConstants.LINUX){ // if running on Linux platform
				isArm = osArch.contains("arm");
				path = isArm ? nativeLibPath + "linux-armv6hf" : nativeLibPath + "linux" + bitsJVM;
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
			
			try {
				addLibraryPath(nativeLibPath);
			} catch (Exception e){
				e.printStackTrace();
			}
			
			System.out.println(System.getProperty("java.library.path"));
			try {
				System.loadLibrary("ftdxx.1.2.2");
			} catch (UnsatisfiedLinkError e) {
				e.printStackTrace();
			}
			
			nativeLoaded = true;
		}
	}
	
	private void addLibraryPath(String path) throws Exception {
		String originalPath = System.getProperty("java.library.path");
		
		if (isArm) {
			if (originalPath.indexOf("linux32") != -1) {
				originalPath = originalPath.replaceAll(":[^:]*?linux32", "");
			}
		}
		
		try {
			System.setProperty("java.library.path", originalPath +System.getProperty("path.separator")+ path);
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
	
	public void openDevice(int portIndex){
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

