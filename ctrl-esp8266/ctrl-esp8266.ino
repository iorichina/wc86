/*
  esp8266, ESC brushed, servo
*/
#ifndef ESP8266
#define ESP8266
#endif

#include <Servo.h>
#include <ESP8266WiFi.h>
#include <WebSocketsServer.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include <Hash.h>
#include <String.h>

#include "wifi_config.h" // <<< create file and add wifi config ```const char* ssid = ; const char* password = ;```

ESP8266WebServer server(80);
WebSocketsServer webSocket = WebSocketsServer(9998);

Servo ESC;
// ESC pin D1->GPIO5 with PWM
int escPin = 5;
// ESC degree, < 90 go forward, > 90 go back
int escDegree = 90;

Servo servo;
// direction pin D5->GPIO14 with PWM
int servoPin = 14;
// direction degree, < 90 go right, > 90 go left
int servoDegree = 90;

bool escLock = false;
void escWriteTo(int target)
{
  if (escLock)
  {
    escDegree = target;
    return;
  }
  escLock = true;
  int old = escDegree;
  escDegree = target;

  // stay
  if (target == old)
  {
    ESC.write(escDegree);
    escLock = false;
    return;
  }

  // move faster or go back
  if (target > old)
  {
    Serial.print("move faster or go back from ");
    Serial.print(old);
    Serial.print(" to ");
    Serial.println(escDegree);
    for (int val = old; val <= escDegree; val += 1)
    {
      ESC.write(constrain(val, old, escDegree));
      delay(5);
    }
    escLock = false;
    return;
  }

  // slow down or go forward
  Serial.print("slow down or go forward from ");
  Serial.print(old);
  Serial.print(" to ");
  Serial.println(escDegree);
  for (int val = old; val >= escDegree; val -= 1)
  {
    ESC.write(constrain(val, escDegree, old));
    delay(5);
  }
  escLock = false;
}

bool servoLock = false;
void servoWriteTo(int target)
{
  if (servoLock)
  {
    servoDegree = target;
    return;
  }
  servoLock = true;
  int old = servoDegree;
  servoDegree = target;

  // stay
  if (target == old)
  {
    servo.write(servoDegree);
    servoLock = false;
    return;
  }

  // go left
  if (target > old)
  {
    Serial.print("go left from ");
    Serial.print(old);
    Serial.print(" to ");
    Serial.println(servoDegree);
    for (int val = old; val <= servoDegree; val += 10)
    {
      servo.write(constrain(val, old, servoDegree));
      delay(5);
    }
    servoLock = false;
    return;
  }

  // go right
  Serial.print("go right from ");
  Serial.print(old);
  Serial.print(" to ");
  Serial.println(servoDegree);
  for (int val = old; val >= servoDegree; val -= 10)
  {
    servo.write(constrain(val, servoDegree, old));
    delay(5);
  }
  servoLock = false;
}

// speed [-100, 100](back, forward) map to [60, 120], direction [-100, 100](left, right) map to [10, 170]
void ctrl(int speed, int direction)
{
  speed = constrain(speed, -100, 100);
  direction = constrain(direction, -100, 100);

  // stop
  if (speed == 0)
  {
    speed = 90;
  }
  // go back
  else if (speed < 0)
  {
    speed = map(-speed, 0, 100, 105, 108);
  }
  // go forward
  else
  {
    speed = map(-speed, -100, 0, 68, 75);
  }

  // straight
  if (direction == 0)
  {
    direction = 90;
  }
  // go left
  else if (direction < 0)
  {
    direction = map(-direction, 0, 100, 90, 170);
  }
  // go right
  else
  {
    direction = map(-direction, -100, 0, 10, 90);
  }

  Serial.print("Ctrl direction=");
  Serial.print(direction);
  Serial.print(", speed=");
  Serial.print(speed);
  Serial.println(".");

  direction = constrain(direction, 10, 170);
  servoWriteTo(direction);
  speed = constrain(speed, 68, 108);
  escWriteTo(speed);
}

void parseMotion(String str, int *speed, int *direction)
{
  size_t pos = str.indexOf(",");
  if (pos == -1)
  {
    Serial.print("error input:[");
    Serial.print(str);
    Serial.println("].");
    return;
  }

  *speed = str.substring(0, pos).toInt();
  *direction = str.substring(pos + 1).toInt();
  Serial.print("Read direction=");
  Serial.print(*direction);
  Serial.print(", speed=");
  Serial.print(*speed);
  Serial.println(".");
}

long long wsCtrTs = millis();
void webSocketEvent(uint8_t num, WStype_t type, uint8_t *payload, size_t length)
{

  switch (type)
  {
  case WStype_DISCONNECTED:
    Serial.printf("[%u] Disconnected!\n", num);
    ctrl(0, 0);
    break;
  case WStype_CONNECTED:
  {
    IPAddress ip = webSocket.remoteIP(num);
    Serial.printf("[%u] Connected from %d.%d.%d.%d url: %s\n", num, ip[0], ip[1], ip[2], ip[3], payload);

    // send message to client
    webSocket.sendTXT(num, "Connected");
  }
  break;
  case WStype_TEXT:
    Serial.printf("[%u] get Text: %s\n", num, payload);

    if (payload[0] == '#')
    {
      // we get motion data
      String str = (const char *)&payload[1];
      int speed = 0;
      int direction = 0;
      // decode motion data Speed,Direction
      parseMotion(str, &speed, &direction);

      ctrl(speed, direction);
      wsCtrTs = millis();
    }

    break;
  }
}

// the setup routine runs once when you press reset:
void setup()
{
  Serial.begin(115200);
  while (Serial.read() >= 0)
  {
  } // clear serial port's buffer

  Serial.print("Speed initial pin ");
  Serial.print(escPin);
  Serial.print("(1000, 2000)");
  Serial.print(", initial angle ");
  Serial.println(escDegree);
  ESC.attach(escPin, 1000, 2000);
  ESC.write(escDegree); // writeMicroseconds set servo to mid-point 1500

  Serial.print("Direction initial pin ");
  Serial.print(servoPin);
  Serial.print("(1000, 2000)");
  Serial.print(", initial angle ");
  Serial.println(servoDegree);
  servo.attach(servoPin, 1000, 2000);
  servo.write(servoDegree); // writeMicroseconds set servo to mid-point 1500

  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, HIGH); // turn the LED on (HIGH is the voltage level)

  // wifi
  {
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, password);
    Serial.println("Connecting to wifi");

    // Wait for connection
    while (WiFi.status() != WL_CONNECTED)
    {
      delay(500);
      Serial.print(".");
    }

    Serial.println("");
    Serial.print("Connected to ");
    Serial.println(ssid);
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
  }

  // start webSocket server
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  // handle index
  server.on("/", []()
            {
    // send index.html
    server.send(200, "text/html", "<html><head><script>var connection=new WebSocket('ws://' + location.hostname + ':9998/', ['arduino',]); connection.onopen=function (){ connection.send('Connect ' + new Date()); sendRGB();}; connection.onerror=function (error){ console.log('WebSocket Error ', error);}; connection.onmessage=function (e){ console.log('Server: ', e.data);}; function sendRGB(){ var rr=(document.getElementById('SPEED').value); var gg=(document.getElementById('DIRECTION').value); var rgb='#'+rr + ',' + gg; connection.send(rgb); console.log('send: ' + rgb);} </script></head><body>Controller: <br /><br />SPEED: <input id='SPEED' type='range' value='0' min='-100' max='100' step='5' oninput='sendRGB();' /><br />DIRECTION: <input id='DIRECTION' type='range' value='0' min='-100' max='100' step='5' oninput='sendRGB();' /><br /></body></html>"); });

  server.begin();
}

long long ts_led = millis();
// the loop routine runs over and over again forever:
void loop()
{
  webSocket.loop();
  server.handleClient();

  // put your main code here, to run repeatedly:
  if (Serial.available() > 0)
  {
    // listen the Serial port, run the code when something catched..
    delay(10);
    String str = Serial.readString(); // read out the string
    int speed = 0;
    int direction = 0;
    parseMotion(str, &speed, &direction);
    ctrl(speed, direction);
  }

  if (millis() - ts_led > 2000)
  {
    digitalWrite(LED_BUILTIN, HIGH ^ digitalRead(LED_BUILTIN));
    ts_led = millis();

    Serial.print("Servo write ");
    Serial.print(servoDegree); // show the integer number on Serial Monitor
    servoWriteTo(servoDegree); // write the integer number to Servo in unit of micro-second

    Serial.print(", ESC write ");
    Serial.println(escDegree); // show the integer number on Serial Monitor
    escWriteTo(escDegree);     // write the integer number to Servo in unit of micro-second
  }
}
