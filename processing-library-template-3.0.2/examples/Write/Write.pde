import com.hirschandmann.serial.d2xx.*;

D2XX d2xx;

byte[] ByteArrayPacket	= {1,2,3,4,5,6};
int packetOffset	= 2;
int packetLength	= 3;
int intPacket	= 1;

void setup() {
  size(400,400);
  smooth();
  
  d2xx = new D2XX(this, 1, 9600);
}

void draw() {
  background(0);
  fill(255);
  
  d2xx.write(intPacket);
  
  d2xx.write(ByteArrayPacket);
  
  d2xx.write(ByteArrayPacket, packetOffset, packetLength);

  delay(100);
  
}	
