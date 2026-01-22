//millis() will overflow after 50 days. Make sure to reset it after each charge.

#include <SPI.h>
#include <SD.h>
#include <arduinoFFT.h>
#include "ESP8266TimerInterrupt.h"       //https://github.com/khoih-prog/ESP8266TimerInterrupt
#include "ESP8266_ISR_Timer.hpp"         //https://github.com/khoih-prog/ESP8266TimerInterrupt
#include <ESP8266WiFi.h>
#include <EEPROM.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>


#define SAMPLES 128            // Number of samples
#define TIMER_INTERVAL_MS 20   // Frequency = 1/(TIMER_INTERVAL_MS/1000) = 50 Hz

// Fourier Transform Variables
double vReal[SAMPLES];
double vImag[SAMPLES];
double fftMagnitude[SAMPLES];
int valArr[SAMPLES];
int maxIndex;
uint8_t fftCount = 0;
ArduinoFFT<double> FFT = ArduinoFFT<double>(vReal, vImag, SAMPLES, 50);


// Variables for reset
const int ssidMaxLen = 32; // Maximum length of SSID
const int passMaxLen = 64; // Maximum length of password
const int emailMaxLen = 64; // Maximum length of SSID
const int epassMaxLen = 64; // Maximum length of password

const int ssidAddr = 0; // Address to store SSID in EEPROM
const int passAddr = 32; // Address to store password in EEPROM
const int emailAddr = 64; // Address to store SSID in EEPROM
const int epassAddr = 96; // Address to store password in EEPROM

int val;
int t = 0;
int throwOutTime = 400; // Window of time before a single look is discarded (ms) e.g. holding left
int maxTime = 15; // Window of time before a move is discarded (s)
int arrSize = 10; // How many moves are needed
long arr[10]; // UPDATE THIS BASED ON ARRSIZE
int arrLen = 0;
bool moved = false;

ESP8266Timer ITimer; // Init ESP8266 only and only Timer 1
volatile uint32_t timer = 0; // Tracks the time of the code without millis()
volatile uint16_t buttonCount = 0; // Tracks if the button has been held
File myFile; // SD file

// Struct to store the users data
struct UserData {
  int max;
  int min;
};
UserData User;

bool threshTrig = false;
bool fftTrig = false;
bool trigger = false;

// Interrupt Handler
void IRAM_ATTR Interrupt() {

  update(); // Updates the thresholding and fourier variables
  checkTrigger(); // Checks if the trigger requirements are met
  // Serial.println(String(User.min) + " " + String(User.max) + " " + String(maxIndex) + ", 0, 1024, " + String(val) + " " + String(arrLen) + " " + String(fftTrig) + " " + String(trigger));
  // SDSave(); // Save data to the SD card
  //Serial.println(String(timer) + ", " + String(val));
  Serial.println(String(val));
}

void setup() {
  // Initialize the serial communication:
  Serial.begin(115200);

  // if (!SD.begin(10)) {
  //   Serial.println("Card failed, or not present");
  //   // don't do anything more:
  // }
  Serial.println("Starting");
  // myFile = SD.open("data3.txt", FILE_WRITE);
  
  pinMode(D1, OUTPUT); // Button
  pinMode(D2, OUTPUT); // Speaker

  // Initialize variables for the user
  User.min = 200; // Default values
  User.max = 800;
  User = Initialize(); 
  
  if(ITimer.attachInterruptInterval(TIMER_INTERVAL_MS * 1000, Interrupt)) 
  {
  }
}

void loop(){
}

void update() {
  
  val = analogRead(A0); // Get input
  timer += TIMER_INTERVAL_MS; // Update timer based on our interrupt time

  ////////////////////
  // USE FFT METHOD //
  ////////////////////

  maxIndex = CalcFFT();

  // Update the counter if they are moving their eyes at a consistent rate 
  if(maxIndex >= 2 && maxIndex <= 8 && fftCount < 200) {
    fftCount++;
  } else if(fftCount > 0 && maxIndex < 2) {
    fftCount--;
  }
  
  /////////////////////////////
  // USE THRESHOLDING METHOD //
  /////////////////////////////

  removeArray(); // Removes any movements that occured too long ago

  // If they look right, store the time and confirm the movement
  if(val>=User.max && !moved){
    moved = true;
    t = timer;
  }

  // If they look back left quickly enough after moving right, add to the array
  if (val<=User.min && moved) {
    moved = false;
    if(arrLen<arrSize) {
      // Add the time to the array, and increase arrLen for trigger checking
      arr[arrLen]=timer;
      arrLen++;
    } else {
      // If the array is full, shift the last one off
      for(int j = 0; j<arrLen; j++){
        arr[j] = arr[j+1];
      }
      arr[arrLen] = timer;
    }
  }

  // Throw out the move if the user never crossed the threshold again
  if(moved && t+throwOutTime < timer) {
    moved = false;
  }

}


void removeArray() {
  if(timer>(arr[0]+(15*1000)) && arrLen > 0){
    for(int j = 0; j<arrLen; j++){
      arr[j] = arr[j+1];
    }
    arrLen-=1;
  }
}

void checkTrigger() {

  /////////////////
  // FFT TRIGGER //
  /////////////////

  if(fftCount >= 150) {
    fftTrig = true;
  } else {
    fftTrig = false;
  }

  //////////////////////////
  // THRESHOLDING TRIGGER //
  //////////////////////////

  if(arrLen >= arrSize){
    threshTrig = true;
  } else {
    threshTrig = false;
  }

  if(threshTrig && fftTrig){
    trigger = true;
  } else {
    trigger = false;
  }

  ///////////////////////////////////////////////
  ///// READ BUTTON INPUT, TURN TRIGGER OFF /////
  ///// AND RESET VARIABLES FOR TRIGGERING //////
  ///////////////////////////////////////////////

  // If the trigger variable is true, sound the alarm
  if(trigger) {
    digitalWrite(D2, HIGH);
  }

  // If the button is pressed turn off the alarm and reset the variables
  if(digitalRead(D1) == 1) {
    digitalWrite(D2, LOW);
    trigger = false;
    arrLen = 0;
    for(int j = 0; j<arrSize; j++){
      arr[j] = 0;
    }
    fftCount = 0;
    buttonCount += TIMER_INTERVAL_MS;
    if(buttonCount >= 2500) {
      ESP.restart();
    }
  } else {
    buttonCount = 0;
  }

  

}

int CalcFFT() {

  // Populate the real part of the input data
  for (int i = SAMPLES-2; i >= 0; i--) {
    valArr[i+1] = valArr[i];
    vReal[i+1] = valArr[i+1];
    vImag[i+1] = 0;
  }

  valArr[0] = val;
  vReal[0] = val;
  vImag[0] = 0;

  FFT.dcRemoval();
  FFT.windowing(vReal, SAMPLES, FFT_WIN_TYP_HAMMING, FFT_FORWARD);
  FFT.compute(vReal, vImag, SAMPLES, FFT_FORWARD);
  FFT.complexToMagnitude(vReal, vImag, SAMPLES);
  double maxMagnitude = 0;
  int maxIndex = 0;

  for (int i = 0; i < SAMPLES / 2; i++) { // Only consider first half (Nyquist limit)
    if (vReal[i] > maxMagnitude) {
      maxMagnitude = vReal[i];
      maxIndex = i;
    }
  }

  return maxIndex;
}

UserData Initialize() {
  int initTime = 15 * 1000;
  int locMax = 0;
  int locMin = 1500;
  int maxIdx = 0;
  int minIdx = 0;
  volatile int tempArr[3];
  volatile int tempTime[3];
  int minVs[50];
  int maxVs[50];
  memset(minVs,0,sizeof(minVs));
  memset(maxVs,0,sizeof(maxVs));
  bool movedI = false;

  while(millis() < initTime) {
    // Read the voltage
    val = analogRead(A0);
    Serial.println(val);

    // Update the arrays with values
    tempArr[0] = tempArr[1];
    tempArr[1] = tempArr[2];
    tempArr[2] = val;
    tempTime[0] = tempTime[1];
    tempTime[1] = tempTime[2];
    tempTime[2] = millis();

    // Check if there is a peak above the threshold
    if(tempArr[1] > tempArr[0] && tempArr[1] > tempArr[2] && tempArr[1] > 600){
      if(movedI){
        movedI = false;
        minVs[minIdx] = locMin;
        minIdx++;
        locMin = 1500;
      }

      if(tempArr[1] > locMax) {
        locMax = tempArr[1];
      }
    }

    // Check if there is a valley below the threshold
    if(tempArr[1] < tempArr[0] && tempArr[1] < tempArr[2] && tempArr[1] < 400){
      if(!movedI) {
        movedI = true;
        maxVs[maxIdx] = locMax;
        maxIdx++;
        locMax = 0;
      }
      
      if(tempArr[1] < locMin) {
        locMin = tempArr[1];
      }
    }

  }

  // Print the data and calculate the users average min and max
  uint16_t userMin = 200;
  uint16_t userMax = 800;
  float percentOffset = 0.3;

  if (minIdx > 2) {
    for(int i = 1; i < minIdx; i++){
      userMin += minVs[i];
    }
    userMin /= (minIdx-2);
    userMin += (512-userMin)*percentOffset;
  } else {
    userMin = 200;
  }

  if (maxIdx > 2) {
    int maxCount = 0;
    for(int i = 1; i < maxIdx; i++){
      userMax += maxVs[i];
      maxCount++;
    }
    userMax /= maxCount;
    userMax -= (userMax-512)*percentOffset;
  } else {
    userMax = 800;
  }

  // Store the data in a structure to return
  UserData thisUser;
  thisUser.max = userMax;
  thisUser.min = userMin;
  return thisUser;
}

void SDSave() {  
  // Serial.println(myFile);
  if (myFile) {
    myFile.print(timer);
    myFile.print(",");
    myFile.println(analogRead(A0));
    // close the file:
  } else {
    // if the file didn't open, print an error:
    myFile.close();
  }
}
