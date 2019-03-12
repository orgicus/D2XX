package com.hirschandmann.serial.d2xx;

import com.ftdichip.ftd2xx.DataBits;
import com.ftdichip.ftd2xx.Parity;
import com.ftdichip.ftd2xx.StopBits;

import processing.core.PApplet;

public class SunSpots {

	D2XX panel;

	//Sunspot panel expects packets such as =
	// {startbit,address,red,green, blue, white,flag,endBit}
	private byte[] packet = new byte[8];

	private int startBit = 0xAA;
	private int address  = 0xFF; // 0xFF for broadcast or the number of the idividual tile address
	private int red      = 40;
	private int green    = 30;
	private int blue     = 50;
	private int white    = 20;
	private int flag     = 0xFF; // 0xF0 to update indivudal LED, 0xFF to send colour to all LEDs, or LED address to send a colour to individual LED
	private int endBit   = 0xCC;
	
	private static final int STARTBIT = 0;
	private static final int ADDRESS  = 1;
	private static final int RED	  = 2;
	private static final int GREEN    = 3;
	private static final int BLUE     = 4;
	private static final int WHITE    = 5;
	private static final int FLAG     = 6;
	private static final int ENDBIT   = 7;

	public SunSpots(PApplet parent, int portIndex, int baudRate){
		
		panel = new D2XX(parent, portIndex, baudRate);
		panel.setDataCharacteristics(DataBits.DATA_BITS_8, StopBits.STOP_BITS_2, Parity.NONE);
		
		packet[STARTBIT] = (byte)startBit;
		packet[ADDRESS] = (byte)address;
		packet[RED] = (byte)red;
		packet[GREEN] = (byte)green;
		packet[BLUE] = (byte)blue;
		packet[WHITE] = (byte)white;
		packet[FLAG] = (byte)flag;
		packet[ENDBIT] = (byte)endBit;
	}
	
	/** Send a RGBW colour to every pixel in every panel
	 * 
	 * @param r - red value 0-255
	 * @param g - green value 0-255
	 * @param b - blue value 0-255
	 * @param w - white value 0-255
	 */
	public void broadcast(int r, int g, int b, int w){
		packet[ADDRESS]  = (byte)0xFF;
		packet[RED]      = (byte)r;
		packet[GREEN]	 = (byte)g;
		packet[BLUE] 	 = (byte)b;
		packet[WHITE]	 = (byte)w;
		packet[FLAG] 	 = (byte)0xFF;
		send();
	}
	
	/** Send a RGBW colour to every pixel in a particular panel
	 * 
	 * @param panel - the panel ID number 
	 * @param r - red value 0-255
	 * @param g - green value 0-255
	 * @param b - blue value 0-255
	 * @param w - white value 0-255
	 */
	public void broadcast(int panel, int r, int g, int b, int w){
		packet[ADDRESS] = (byte)panel;
		packet[RED]     = (byte)r;
		packet[GREEN]	= (byte)g;
		packet[BLUE] 	= (byte)b;
		packet[WHITE]	= (byte)w;
		packet[FLAG] 	= (byte)0xFF;
		send();
	}
	
	/** Load a RGBW colour for a particular pixel on every panel 
	 * 
	 * @param panel - the panel ID number 
	 * @param r - red value 0-255
	 * @param g - green value 0-255
	 * @param b - blue value 0-255
	 * @param w - white value 0-255
	 * @param pixel - the individual pixel ID (0 - 99)
	 */
	public void loadPixel(int r, int g, int b, int w, int pixel){
		packet[ADDRESS] = (byte)0xFF;
		packet[RED]     = (byte)r;
		packet[GREEN]   = (byte)g;
		packet[BLUE]    = (byte)b;
		packet[WHITE]	= (byte)w;
		packet[FLAG]    = (byte)pixel;
		send();
	}
	
	/** Load a RGBW colour for a particular pixel on a particular panel 
	 * 
	 * @param panel - the panel ID number 
	 * @param r - red value 0-255
	 * @param g - green value 0-255
	 * @param b - blue value 0-255
	 * @param w - white value 0-255
	 * @param pixel - the individual pixel ID (0 - 99)
	 */
	public void loadPixel(int panel, int r, int g, int b, int w, int pixel){
		packet[ADDRESS] = (byte)panel;
		packet[RED]     = (byte)r;
		packet[GREEN]   = (byte)g;
		packet[BLUE]    = (byte)b;
		packet[WHITE]	= (byte)w;
		packet[FLAG]    = (byte)pixel;
		send();
	}
	
	/** Update every panel
	 * 
	 */
	public void update(){
		packet[ADDRESS] = (byte)0xFF;
		packet[FLAG]    = (byte)0xF0;
		send();
	}
	
	/** Update a particular panel
	 * 
	 * @param panel - the panel ID number 
	 */
	public void update(int panel){
		packet[ADDRESS] = (byte)panel;
		packet[FLAG]    = (byte)0xF0;
		send();
	}
	
	private void send(){
		panel.write(packet);
	}
}