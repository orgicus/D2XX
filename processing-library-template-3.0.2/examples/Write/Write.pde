import com.hirschandmann.serial.d2xx.*;

D2XX d2xx;

void setup() {
  size(400,400);
  smooth();
  
  d2xx = new D2XX(this, 1, 9600);
  d2xx.openDevice(1);
}

void draw() {
  background(0);
  fill(255);
  
  byte[] packet = {a,b,c};
  d2xx.sendBytes(packet);

  delay(100);
  
}