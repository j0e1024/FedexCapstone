#include <Arduino.h>
#include <SoftwareSerial.h>]

SoftwareSerial EEBlue(BT_RX, BT_TX); // RX | TX

// String response;
 String request;

void handle(String);

void setup()
{
  request = "";  
  Serial.begin(9600);
  EEBlue.begin(9600);  //Default Baud for comm, it may be different for your Module. 
  //pinMode(BT_STATE, INPUT);
  pinMode(BT_ENABLE, OUTPUT);
  digitalWrite(BT_ENABLE, HIGH);

  delay(2000);

  Serial.println("Connect to HC-05 from any other bluetooth device with 1234 as pairing key!.");
}
  
void loop()
{

  if (EEBlue.available()){
    char input = EEBlue.read();
    Serial.print(input);
    if(input == '\n'){
      Serial.print(input);
      Serial.println("received request: ");
      Serial.println(request);
      handle(request);
      request = "";
    } else {
      request += input;
    }
  }
}

void handle(String request) {
  if(request == "name") {
    char* message = " Bay Door 001\0";
    EEBlue.write(message);
    Serial.print(message);
  } else if(request == "uuid") {
    char* message = "00001101-0000-1000-8000-00805f9b34fb\0";
    EEBlue.write(message);
    Serial.print(message);
  } else {
    Serial.print("Unknown request");
  }
    EEBlue.write('\n');
}