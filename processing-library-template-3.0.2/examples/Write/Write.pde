import com.hirschandmann.serial.d2xx.*;

D2XX d2xx;

void setup() {
  size(400,400);
  smooth();
  
  d2xx = new D2XX(this, 1, 9600);
}

void draw() {
  background(0);
  fill(255);
}