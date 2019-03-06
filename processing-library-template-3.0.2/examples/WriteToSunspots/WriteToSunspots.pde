import com.hirschandmann.serial.d2xx.*;

D2XX d2xx;

/*
  Example of using the D2XX library to send LED data to a sunspot panel.
*/

// Sunspot panel expects packets such as = {startbit,address,red,green, blue, white,flag,endBit}
byte[] packet = new byte[8];

int startBit = 0xAA;
int address  = 0xFF; // 0xFF for broadcast or the number of the idividual tile address
int red      = 40;
int green    = 30;
int blue     = 50;
int white    = 20;
int flag     = 0xFF; // 0xF0 to update indivudal LED, 0xFF to send colour to all LEDs, or LED address to send a colour to individual LED
int endBit   = 0xCC;

void setup() {
  size(400,400);
  smooth();

  d2xx = new D2XX(this, 0, 9600);
}

void draw() {
  background(0);

  red = int(map(mouseX,0,width,0,255));
  green = int(map(mouseY,0,height,0,255));
  send();

  fill(red,green,0);

  delay(100);
}

void send(){
  packet[0] = (byte)startBit;
  packet[1] = (byte)address;
  packet[2] = (byte)red;
  packet[3] = (byte)green;
  packet[4] = (byte)blue;
  packet[5] = (byte)white;
  packet[6] = (byte)flag;
  packet[7] = (byte)endBit;
  d2xx.write(packet);
}
