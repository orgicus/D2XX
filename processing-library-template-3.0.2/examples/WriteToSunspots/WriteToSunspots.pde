import com.hirschandmann.serial.d2xx.*;

SunSpots sunSpots;

void setup() {
  size(400,400);
  smooth();

  sunSpots = new SunSpots(this, 4, 2000000);
}

void draw() {

  int red = int(map(mouseX,0,width,0,255));
  int green = int(map(mouseY,0,height,0,255));

  background(red,green,20);
  sunSpots.broadcast(red, green, 20, 20);

  delay(100);
}
