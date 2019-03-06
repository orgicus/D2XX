import com.hirschandmann.serial.d2xx.*;

D2XX d2xx;

int packetStart;
int packetEnd;
int startBit = 0xAA;
int address  = 0xFF; // 0xFF for broadcast or the idividual tile address
int green    = 0xAA;
int red      = 0x12;
int blue     = 0x32;
int white    = 0x43;
int flag     = 0xFF; // 0xF0 to update indivudal LED, 0xFF to send colour to all LEDs, or LED address to send a colour to individual LED
int endBit   = 0xCC;

int[] buffer = {startBit, address, green, red, blue, white, flag, endBit};

// packet = address,green,red, blue, white,flag,  end
//            48    40    32    24    16    8
//            FF    AA    12    32    43    FF    CC
//          0xFFAA12 3243FFCC
//          AAFFAA12 3243FFCC

void setup() {
  size(400,400);
  smooth();

  d2xx = new D2XX(this, 0, 9600);

  // startBit = startBit << 24;
  // address  = address  << 16;
  // green    = green    << 8;
  //
  // blue     = blue     << 24;
  // white    = white    << 16;
  // flag     = flag     << 8;
  //
  // packetStart = startBit | address | green | red;
  // packetEnd =  blue | white | flag | endBit;

  // println(packetStart + " " + packetEnd);
  // println(hex(packetStart) + hex(packetEnd));
}

void draw() {
  background(0);
  fill(255);

  for (int x = 0; x < buffer.length; x++){
    d2xx.write(buffer[x]);
  }


  // d2xx.write(packetStart);
  // d2xx.write(packetEnd);

  delay(100);
}
