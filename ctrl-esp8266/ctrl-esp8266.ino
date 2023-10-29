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

#ifndef LED_BUILTIN
#define LED_BUILTIN 2
#endif

const char *apSsid = "wcctrl8266"; // without pass
#include "wifi_config.h"           // <<< create file and add wifi config ```const char* ssid = "";const char* password = "";```

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
  if (true)
  {
    escDegree = target;
    ESC.write(escDegree);
    return;
  }

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
    for (int val = old; val <= escDegree; val += 5)
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
  for (int val = old; val >= escDegree; val -= 5)
  {
    ESC.write(constrain(val, escDegree, old));
    delay(5);
  }
  escLock = false;
}

bool servoLock = false;
void servoWriteTo(int target)
{
  if (true)
  {
    servoDegree = target;
    servo.write(servoDegree);
    return;
  }

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

void writeTo(int speed, int direction)
{
  // do it for safty
  speed = constrain(speed, 10, 170);
  escWriteTo(speed);
  // do it for safty
  direction = constrain(direction, 10, 170);
  servoWriteTo(direction);
}

// speed [-100, 100](back, forward) map to [10, 170], direction [-100, 100](left, right) map to [10, 170] at client
long long ctrlTs = millis();
void ctrl(int speed, int direction)
{
  ctrlTs = millis();
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
    // do it at client
    //  speed = map(-speed, 0, 100, 105, 108);
    speed = map(-speed, 0, 100, 90, 180);
  }
  // go forward
  else
  {
    // do it at client
    //  speed = map(-speed, -100, 0, 68, 75);
    speed = map(-speed, -100, 0, 0, 90);
  }

  // straight
  if (direction == 0)
  {
    direction = 90;
  }
  // go left
  else if (direction < 0)
  {
    direction = map(-direction, 0, 100, 90, 180);
  }
  // go right
  else
  {
    direction = map(-direction, -100, 0, 0, 90);
  }

  Serial.print("Ctrl direction=");
  Serial.print(direction);
  Serial.print(", speed=");
  Serial.print(speed);
  Serial.println(".");
  writeTo(speed, direction);
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
    webSocket.sendTXT(num, "joystickClient");
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

      wsCtrTs = millis();
      ctrl(speed, direction);
    }

    break;
  }
}

bool resetWifi(char *staSsid, char *staPassword, int wait)
{
  WiFi.disconnect();
  WiFi.mode(WIFI_AP_STA);

  WiFi.softAP(apSsid);              // 设置AP网络参数
  IPAddress myIP = WiFi.softAPIP(); // 192.168.4.1
  Serial.print("AP: ");
  Serial.print(apSsid);
  Serial.print(" IP address: ");
  Serial.println(myIP);

  WiFi.begin(staSsid, staPassword);
  Serial.print("Connecting to wifi ");
  Serial.println(staSsid);

  // Wait for connection
  for (size_t i = 0; i < wait; i++)
  {
    if (WiFi.status() == WL_CONNECTED)
    {
      break;
    }
    delay(1000);
    Serial.print(".");
  }
  if (WiFi.status() != WL_CONNECTED)
  {
    Serial.println("Connect fail after " + String(wait) + " seconds.");
    return false;
  }

  ssid = staSsid;
  password = staPassword;

  Serial.println();
  Serial.println("Connected");
  Serial.print("STA IP address: ");
  Serial.println(WiFi.localIP());
  return true;
}

void _handleWifiSet()
{
  String newssid = server.arg("ssid");
  String newpass = server.arg("pass");
  bool w = resetWifi((char *)newssid.c_str(), (char *)newpass.c_str(), 30);
  if (w)
  {
    server.send(200, "text/plain", "ok, current ip " + WiFi.localIP().toString());
  }
  else
  {
    resetWifi((char *)ssid, (char *)password, 30);
    server.send(200, "text/plain", "fail, current ip " + WiFi.localIP().toString());
  }
}
void _handleWifiGet()
{
  server.send(200, "application/json", "{\"ip\":\"" + WiFi.localIP().toString() + "\",\"ssid\":\"" + ssid + "\",\"pass\":\"" + password + "\"}");
}
void initServerHandler()
{
  // handle index
  server.on("/", []()
            {
    // send index.html
    server.send(200, "text/html", "<html><head><script>var cs=console; var conn=new WebSocket('ws://' + location.hostname + ':9998/', ['arduino', ]); conn.onopen=function (){ conn.send('Connect ' + new Date()); ctr();}; conn.onerror=function (error){ cs.log('WebSocket Error ', error);}; conn.onmessage=function (e){ cs.log('Server: ', e.data);}; function ctr(){ var s=(document.getElementById('SPEED').value); var d=(document.getElementById('DIREC').value); var v='#' + s + ',' + d; conn.send(v); cs.log('send: ' + v);} </script></head><body>SPEED: <input id='SPEED' type='range' style='transform: rotate(-90deg);height: 129px;' value='0' min='-100' max='100' step='5' oninput='ctr();' /><br />DIREC: <input id='DIREC' type='range' value='0' min='-100' max='100' step='5' oninput='ctr();' /><br /></body></html>"); });
  // handle post wifi config
  server.on("/wifi/set", _handleWifiSet);
  server.on("/wifi/get", _handleWifiGet);
}

// the setup routine runs once when you press reset:
void setup()
{
  Serial.begin(115200);
  delay(1500);
  while (Serial.read() >= 0)
  {
  } // clear serial port's buffer

  Serial.println();
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
  Serial.print("LED_BUILTIN ");
  Serial.println(LED_BUILTIN);

  // wifi
  resetWifi((char *)ssid, (char *)password, 60);

  // start webSocket server
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  initServerHandler();
  server.begin();
}

long long ledTs = millis();
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

  if (millis() - ledTs > 2000)
  {
    ledTs = millis();
    digitalWrite(LED_BUILTIN, HIGH ^ digitalRead(LED_BUILTIN));
  }

  if (millis() - ctrlTs > 2000)
  {
    ctrlTs = millis();
    writeTo(90, 90);
  }
}
