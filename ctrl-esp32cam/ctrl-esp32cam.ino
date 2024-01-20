
#include <Servo.h>
#include <WebSocketsServer.h>
#include <WiFi.h>
#include "camera_wrap.h"
#include <vector>
using namespace std;

const char *apSsid = "wcctrl32cam"; // without pass
#include "wifi_config.h"// <<< create file and add wifi config ```const char* ssid = ; const char* password = ;``` 
// #define DEBUG
// #define SAVE_IMG

enum TRACK{
  TRACK_NONE = 0,
  TRACK_FW,
  TRACK_LEFT,
  TRACK_RIGHT,
  TRACK_STOP
};

//holds the current upload
int cameraInitState = -1;
uint8_t* jpgBuff = new uint8_t[68123];
size_t   jpgLength = 0;
uint8_t camNo=0;
bool clientConnected = false;

//udp?
IPAddress addrRemote;
unsigned int portRemote;
const int RECVLENGTH = 16;
byte packetBuffer[RECVLENGTH];

WebSocketsServer webSocket = WebSocketsServer(86);

Servo ESC;
// ESC pin D1->GPIO5 with PWM
int escPin = 5;?
// ESC degree, < 90 go forward, > 90 go back
int escDegree = 90;

Servo servo;
// direction pin D5->GPIO14 with PWM
int servoPin = 14;?
// direction degree, < 90 go right, > 90 go left
int servoDegree = 90;


const int LED_BUILT_IN        = 4;
const uint8_t TRACK_DUTY      = 100;
const int PIN_SERVO_PITCH     = 12;
// const int PIN_SERVO_YAW       = 2;
const int PINDC_LEFT_BACK     = 13;
const int PINDC_LEFT_FORWARD  = 15;
const int PINDC_RIGHT_BACK    = 14;
const int PINDC_RIGHT_FORWARD = 2;
const int LEFT_CHANNEL        = 2;
const int RIGHT_CHANNEL       = 3;
const int SERVO_PITCH_CHANNEL = 4;
const int SERVO_YAW_CHANNEL   = 5;
const int SERVO_RESOLUTION    = 16;
unsigned long previousMillisServo = 0;
const unsigned long intervalServo = 10;
bool servoUp = false;
bool servoDown = false;
bool servoRotateLeft = false;
bool servoRotateRight = false;
int posServo = 75;
int PWMTrackHIGH = 138;
int PWMTrackLOW = 138;

void servoWrite(uint8_t channel, uint8_t angle) {
  // regarding the datasheet of sg90 servo, pwm period is 20 ms and duty is 1->2ms
  uint32_t maxDuty = (pow(2,SERVO_RESOLUTION)-1)/10; 
  uint32_t minDuty = (pow(2,SERVO_RESOLUTION)-1)/20; 
  uint32_t duty = (maxDuty-minDuty)*angle/180 + minDuty;
  ledcWrite(channel, duty);
}

void controlServo(){
  if(servoUp){
    if(posServo>2){
      posServo -= 2;
    }
  }
  if(servoDown){
    if(posServo<180){
      posServo += 2;
    }
  }
  servoWrite(SERVO_PITCH_CHANNEL,posServo);
}

void controlDC(int left0, int left1, int right0, int right1){
  digitalWrite(PINDC_LEFT_BACK, left0);
  if(left1 == HIGH){
    ledcWrite(LEFT_CHANNEL, 255);
  }else{
    ledcWrite(LEFT_CHANNEL, 0);
  }
  digitalWrite(PINDC_RIGHT_BACK, right0);
  if(right1 == HIGH){
    ledcWrite(RIGHT_CHANNEL, 255);
  }else{
    ledcWrite(RIGHT_CHANNEL, 0);
  }
}

void controlDCTrack(int left, int right){
  digitalWrite(PINDC_LEFT_BACK, 0);
  ledcWrite(LEFT_CHANNEL, left);
  digitalWrite(PINDC_RIGHT_BACK, 0);
  ledcWrite(RIGHT_CHANNEL, right);
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {

  switch(type) {
      case WStype_DISCONNECTED:
          Serial.printf("[%u] Disconnected!\n", num);
          camNo = num;
          clientConnected = false;
          break;
      case WStype_CONNECTED:
          Serial.printf("[%u] Connected!\n", num);
          clientConnected = true;
          break;
      case WStype_TEXT:
      case WStype_BIN:
      case WStype_ERROR:
      case WStype_FRAGMENT_TEXT_START:
      case WStype_FRAGMENT_BIN_START:
      case WStype_FRAGMENT:
      case WStype_FRAGMENT_FIN:
          Serial.println(type);
          break;
  }
}

std::vector<String> splitString(String data, String delimiter){
    std::vector<String> ret;
    // initialize first part (string, delimiter)
    char* ptr = strtok((char*)data.c_str(), delimiter.c_str());

    while(ptr != NULL) {
        ret.push_back(String(ptr));
        // create next part
        ptr = strtok(NULL, delimiter.c_str());
    }
    return ret;
}

void processUDPData(){
  int cb = UDPServer.parsePacket();

  if (cb) {
      UDPServer.read(packetBuffer, RECVLENGTH);
      addrRemote = UDPServer.remoteIP();
      portRemote = UDPServer.remotePort();

      String strPackage = String((const char*)packetBuffer);
  #ifdef DEBUG
      Serial.print("receive: ");
      // for (int y = 0; y < RECVLENGTH; y++){
      //   Serial.print(packetBuffer[y]);
      //   Serial.print("\n");
      // }
      Serial.print(strPackage);
      Serial.print(" from: ");
      Serial.print(addrRemote);
      Serial.print(":");
      Serial.println(portRemote);
  #endif
      if(strPackage.equals("whoami")){
          UDPServer.beginPacket(addrRemote, portRemote-1);
          String res = "ESP32-CAM";
          UDPServer.write((const uint8_t*)res.c_str(),res.length());
          UDPServer.endPacket();
          Serial.println("response");
      }else if(strPackage.equals("forward")){
        controlDC(LOW,HIGH,LOW,HIGH);
      }else if(strPackage.equals("backward")){
        controlDC(HIGH,LOW,HIGH,LOW);
      }else if(strPackage.equals("left")){
        controlDC(LOW,LOW,LOW,HIGH);
      }else if(strPackage.equals("right")){
        controlDC(LOW,HIGH,LOW,LOW);
      }else if(strPackage.equals("stop")){
        controlDC(LOW,LOW,LOW,LOW);
      }else if(strPackage.equals("camup")){
        servoUp = true;
      }else if(strPackage.equals("camdown")){
        servoDown = true;
      }else if(strPackage.equals("camstill")){
        servoUp = false;
        servoDown = false;
      }else if(strPackage.equals("ledon")){
        digitalWrite(LED_BUILT_IN, HIGH);
      }else if(strPackage.equals("ledoff")){
        digitalWrite(LED_BUILT_IN, LOW);
      }else if(strPackage.equals("lefttrack")){
        controlDCTrack(0, PWMTrackHIGH);
      }else if(strPackage.equals("righttrack")){
        controlDCTrack(PWMTrackHIGH, 0);
      }else if(strPackage.equals("fwtrack")){
        controlDCTrack(PWMTrackLOW, PWMTrackLOW);
      }

      memset(packetBuffer, 0, RECVLENGTH);
  }

}

void initWifi() {
  
  if (String(WiFi.SSID()) == String(ssid)) {
    return;
  }
  
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  Serial.printf("Connecting to %s\n", ssid);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("Connected! IP address: ");
  String ipAddress = WiFi.localIP().toString();
  Serial.println(ipAddress);

}

void setup(void) {

  Serial.begin(115200);
  Serial.print("\n");
  #ifdef DEBUG
  Serial.setDebugOutput(true);
  #endif

  pinMode(LED_BUILT_IN, OUTPUT);
  digitalWrite(LED_BUILT_IN, LOW);

  pinMode(PINDC_LEFT_BACK, OUTPUT);
  ledcSetup(LEFT_CHANNEL, 100, 8);//channel, freq, resolution
  ledcAttachPin(PINDC_LEFT_FORWARD, LEFT_CHANNEL);
  pinMode(PINDC_RIGHT_BACK, OUTPUT);
  ledcSetup(RIGHT_CHANNEL, 100, 8);//channel, freq, resolution
  ledcAttachPin(PINDC_RIGHT_FORWARD, RIGHT_CHANNEL);

  controlDC(LOW,LOW,LOW,LOW);

  // 1. 50hz ==> period = 20ms (sg90 servo require 20ms pulse, duty cycle is 1->2ms: -90=>90degree)
  // 2. resolution = 16, maximum value is 2^16-1=65535
  // From 1 and 2 => -90=>90 degree or 0=>180degree ~ 3276=>6553
  ledcSetup(SERVO_PITCH_CHANNEL, 50, 16);//channel, freq, resolution
  ledcAttachPin(PIN_SERVO_PITCH, SERVO_PITCH_CHANNEL);// pin, channel
  servoWrite(SERVO_PITCH_CHANNEL, posServo);

  // ledcSetup(SERVO_YAW_CHANNEL, 50, 16);//channel, freq, resolution
  // ledcAttachPin(PIN_SERVO_YAW, SERVO_YAW_CHANNEL);// pin, channel
  // servoWrite(SERVO_YAW_CHANNEL, posServo);

  cameraInitState = initCamera();

  Serial.printf("camera init state %d\n", cameraInitState);

  if(cameraInitState != 0){
    return;
  }

  //WIFI INIT
  initWifi();

  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
  Serial.println("Stream webSocket listening at port: 86");

  UDPServer.begin(UDPPort); 
  Serial.print("UDP listening at port: ");
  Serial.println(UDPPort);
}

void loop(void) {
  initWifi();
  
  webSocket.loop();
  if(clientConnected == true){
    grabImage(jpgLength, jpgBuff);
    webSocket.sendBIN(camNo, jpgBuff, jpgLength);
    // Serial.print("send img: ");
    // Serial.println(jpgLength);
  }

  unsigned long currentMillis = millis();
  if (currentMillis - previousMillisServo >= intervalServo) {
    previousMillisServo = currentMillis;
    processUDPData();
    controlServo();
  }

  #ifdef DEBUG
  if (Serial.available()) {
    String data = Serial.readString();
    Serial.println(data);
    std::vector<String> vposVals = splitString(data, ",");
    if(vposVals.size() != 4){
      return;
    }
    int left0 = vposVals[0].toInt();
    int left1 = vposVals[1].toInt();
    int left2 = vposVals[2].toInt();
    int left3 = vposVals[3].toInt();
    controlDC(left0, left1, left2, left3);
  }
  #endif
}
