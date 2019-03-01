package template.library;


import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;

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

	public final static String VERSION = "##library.prettyVersion##";
	
	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the Library.
	 * 
	 * @example Write
	 * 
	 * @param parent 	- the parent sketch
	 * @param portIndex	- the port index to open (depends on how many are available)
	 * @param baudRate	- how many bauds per second should it use for communication
	 */
	public D2XX(PApplet parent,int portIndex,int baudRate) {
		this.parent = parent;
		welcome();
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
	
	private void initNative(){
		if (!nativeLoaded){
			int bitsJVM = this.parent.parseInt(System.getProperty("sun.arch.data.model"));
			String osArch = System.getProperty("os.arch");
			String nativeLibPath = getLibPath();
			String path = null;
			
			if (this.parent.platform == PConstants.WINDOWS){ // If running on a Windows platform
				path = nativeLibPath + "windows" + bitsJVM;
				System.out.println("platform: windows path: " + path);
			}
			if (this.parent.platform == PConstants.MACOSX){ // if running on Mac platform
				path = nativeLibPath + "macosx" + bitsJVM;
				System.out.println("platform: mac path: " + path);
			}
			if (PApplet.platform == PConstants.LINUX){ // if running on Linux platform
				isArm = osArch.contains("arm");
				path = isArm ? nativeLibPath + "linux-armv6hf" : nativeLibPath + "linux" + bitsJVM;
			}
			
			try {
				File libDir = new File(path);
				if (libDir.exists()) {
					nativeLibPath = path;
					System.out.println("it worked!");
					System.out.println(path);
				}
			} catch (NullPointerException e){
				System.err.println("Cannot load local version of D2XX!");
				e.printStackTrace();
			}
			
			if ((this.parent.platform == PConstants.MACOSX && bitsJVM == 64) || 
								(this.parent.platform == PConstants.WINDOWS) || 
								(this.parent.platform == PConstants.LINUX)){
				try {
					addLibraryPath(nativeLibPath);
				} catch (Exception e){
					e.printStackTrace();
				}
				System.loadLibrary("D2XX");
			} else {
				System.err.println("cannot load local version of D2XX");
			}
			
			nativeLoaded = true;
		}
	}
	
	private void addLibraryPath(String path) throws Exception {
		String originalPath = System.getProperty("java.library.path");
		
        // If this is an arm device running linux, Processing seems to include the linux32 dirs in the path,
        // which conflict with the arm-specific libs. To fix this, we remove the linux32 segments from the path.
        //
        // Alternatively, we could do one of the following:
        // 		A) prepend to the path instead of append, forcing our libs to be used
        // 		B) rename the libopencv_java245 in the arm7 dir and add logic to load it instead above in System.loadLibrary(...)
		
		System.out.println("original path: " + originalPath);
		
//		if (isArm) {
//			if (originalPath.indexOf("linux32") != -1){
//				originalPath = originalPath.replaceAll(":[^:]*?linux32", "");
//			}
//		}
//		
//		System.setProperty("java.library.path", originalPath + System.getProperty("path.seperator") + path);
//		
//		final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
//		sysPathsField.setAccessible(true);
//		sysPathsField.set(null, null);
	}
	
	
	private void welcome() {
		System.out.println("##library.name## ##library.prettyVersion## by ##author##");
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

