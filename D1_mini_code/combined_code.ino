//http://192.168.4.1/?ssid=Sameh&password=1481976Wa&email=sam@a.com&epassword=123123

#include <ESP8266WiFi.h>
#include <EEPROM.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>

/* 2. Define the API Key */
#define API_KEY "AIzaSyCyEekgfYi5o1y20-P_4Q2LVJA-QK-YCI0"

/* 3. Define the project ID */
#define FIREBASE_PROJECT_ID "alsalert"


//Define Firebase Data object
FirebaseData fbdo;

FirebaseAuth auth;
FirebaseConfig config;

String uid;

String path;

const char* ssid = "ALS Alert";
const char* password = "";

const int ssidMaxLen = 32; // Maximum length of SSID
const int passMaxLen = 64; // Maximum length of password
const int emailMaxLen = 64; // Maximum length of SSID
const int epassMaxLen = 64; // Maximum length of password
const int successMaxLen = 32; // Maximum length of success

const int ssidAddr = 0; // Address to store SSID in EEPROM
const int passAddr = ssidMaxLen; // Address to store password in EEPROM
const int emailAddr = ssidMaxLen + passMaxLen; // Address to store SSID in EEPROM
const int epassAddr = ssidMaxLen + passMaxLen + emailMaxLen; // Address to store password in EEPROM
const int successAddr = ssidMaxLen + passMaxLen + emailMaxLen + epassMaxLen; // Address to store password in EEPROM

bool buttonPressed = false;
const int buttonPin = 4;
const int holdDuration = 5000;
int buttonPressStartTime = 0;
String storedSSID;
String storedSuccess;

bool alarmTriggered = false;

WiFiServer server(80);

void setup() {
  //clearStoredCredentials();
  Serial.begin(115200);
  pinMode(buttonPin,INPUT);
  delay(100);

  // Initialize EEPROM
  EEPROM.begin(512);

  // Read stored WiFi credentials from EEPROM
  storedSSID = readEEPROM(ssidAddr, 32);
  String storedPass = readEEPROM(passAddr, 64);
  String storedEmail = readEEPROM(emailAddr, 64);
  String storedEpass = readEEPROM(epassAddr, 64);
  storedSuccess = readEEPROM(successAddr, 32);

  // If credentials are stored, connect to WiFi
  if (storedSSID.length() > 0) {

    // clearStoredCredentials();
    // ESP.restart();

    Serial.println("here");
    Serial.println(storedSSID.c_str());
    Serial.println(storedPass.c_str());
    Serial.println("Connecting to WiFi...");

    WiFi.begin(storedSSID.c_str(), storedPass.c_str());

    int wificount = 0;
    while (WiFi.status() != WL_CONNECTED) {
      delay(500);
      Serial.print(".");
      // Serial.println(storedSuccess);
      // Serial.println(storedSuccess.length());
      wificount++;
      if(wificount>30 && storedSuccess != "success"){
        clearStoredCredentials();
        ESP.restart();
      }
    }
    Serial.println("\nConnected to WiFi.");
    Serial.println("");
    Serial.println(WiFi.localIP());
   

    /* Assign the api key (required) */
    config.api_key = API_KEY;

    /* Assign the user sign in credentials */
    auth.user.email = storedEmail;
    auth.user.password = storedEpass;

    /* Assign the callback function for the long running token generation task */
    config.token_status_callback = tokenStatusCallback; //see addons/TokenHelper.h

    Firebase.begin(&config, &auth);
   
    Firebase.reconnectWiFi(true);

    //----------------------------------------------
   // Getting the user UID might take a few seconds
   //-----------------------------------------------
    Serial.println("Getting User UID");
    while ((auth.token.uid) == "") {
      Serial.print('.');
      delay(1000);
    }
      //-----------------
    // Print user UID
    //------------------
    uid = auth.token.uid.c_str();
    Serial.print("User UID: ");
    Serial.println(uid);

  //time_t now = time(nullptr);

  // Convert the current time to a Firestore Timestamp
  //Timestamp timestamp = Timestamp(now);

    //Set "connected" field to 1
    FirebaseJson content;
    content.set("fields/connected/stringValue", "1");
    content.set("fields/ipaddress/stringValue", WiFi.localIP().toString()); //This keeps track of ip address

    //I programmed Cloud Functions to catch "now" and put the current timestamp
    content.set("fields/lastConnectiontemp/stringValue", "now");
   
     
      //esp is the collection id, user uid is the document id in collection info.
    path = "ALSAlert/"+uid+"";
   
    Serial.print("Create document... ");
 
    if (Firebase.Firestore.patchDocument(&fbdo, FIREBASE_PROJECT_ID, "",path.c_str(),content.raw(),"connected,lastConnectiontemp,ipaddress")) //"connected" makes it only change that field. If ommitted, it clears all other fields
        Serial.printf("ok\n%s\n\n", fbdo.payload().c_str());

    writeEEPROM(successAddr, "success");

    // Start the server to check if d1 mini should check the database.
    server.begin();
    Serial.println("Server started");


    //Delay before restarting to test again
    //delay(10000);
    // clearStoredCredentials();
    // ESP.restart();

  } else {

    // Set up the ESP8266 as an access point
    WiFi.mode(WIFI_AP);
    WiFi.softAP(ssid, password);
    server.begin();
    Serial.println("Access Point started.");
    
  }
}

// //YOYOYO, this is test code to try out adding startCal to the Firebase

// //initialize functions

// void addFieldToFirestore(const String &collectionRef, const String &fieldName, const String &value){
//   //Collection, Document based on the user, field
// //   Firebase.Firestore.getDocument("uid").Set({{"startCal", FieldValue::String("0")}}, SetOptions::Merge());
// //     .OnCompletion([](const Future<void>& future) {
// //       if (future.error() == Error::kErrorOk) {
// //         std::cout << "DocumentSnapshot successfully written!" << std::endl;
// //       } else {
// //         std::cout << "Error writing document: " << future.error_message()
// //                   << std::endl;
// //       }
// //     });
// // }
// // Define the collection and document you want to update
//   String documentPath = "ALSAlert/uid";  // Example: users is your collection and uid is the document ID
  
//   // Prepare the data to update the document
//   FirebaseJson json;
//   json.set("startCal", "0");  // Set the field "startCal" to "0"

//   // Update the document in Firestore
//   if (Firebase.Firestore.getDocument(fbdo, documentPath, &json)) {
//     Serial.println("Document successfully updated!");
//   } else {
//     Serial.println("Error updating document: " + Firebase.error());
//   }
// }

// //end of this line of test


void loop() {
  if(storedSSID.length() <= 0){
    getWifiInfo();
  } else {

    if(alarmTriggered){
      checkTest();
      delay(2000);
    }

    // Check if a client has connected
    WiFiClient client = server.available();   // Listen for incoming clients

    if (client) {                             // If a new client connects
        Serial.println("New Client.");
        while (!client.available()) {            // Wait until the client sends some data
          // delay(1);
        }
        String request = client.readStringUntil('\r');
        Serial.println(request);
        client.flush();

      // Check if the request contains "checkNow"
      if (request.indexOf("checknow") != -1) {
        // Call your function here
        checkTest();
      }
     
      if (request.indexOf("ssid=") != -1 && request.indexOf("password=") != -1) {
        resetWifiInfo(client, request);
      }

      //Send a response to the client. Keeps it from refreshing and sending more than once.
      client.println("HTTP/1.1 200 OK");
      client.println("Content-Type: text/html");
      client.println();
      client.println("<!DOCTYPE HTML>");
      client.stop();
    }
   
 
  // //If this delay is not here, it will cost $0.16 per day per device.
  //   //With 2s delay, it will cost 0.03$ per day per device.
  // delay(1000);


  }

}

void checkTest() {
  Serial.println("Checked now!");
  //Get Document
  //--------------------
  path = "ALSAlert/" + uid ;

  //Serial.print("Get a document... ");
  if (Firebase.Firestore.getDocument(&fbdo, FIREBASE_PROJECT_ID, "", path.c_str(), "")) {
    //Serial.printf("ok\n%s\n\n", fbdo.payload().c_str());

    // Create a FirebaseJson object and set content with received payload
    FirebaseJson payload;
    payload.setJsonData(fbdo.payload().c_str());

    // Get the data from FirebaseJson object
    FirebaseJsonData jsonData;
    payload.get(jsonData, "fields/connected/stringValue", true);
    //Serial.println(jsonData.stringValue);
   
    if(jsonData.stringValue == "3"){
      Serial.println("Connected was read to be 3. Now changing to 4");

    setFirestoreValue("lastConnectiontemp","now");
   
    delay(1500);

    setFirestoreValue("connected", "4");
   
    }

    jsonData.clear();
    payload.get(jsonData, "fields/startAlarm/stringValue", true);
    if(jsonData.stringValue == "1"){
      Serial.println("Alarm Triggered! Setting to 0");

    setFirestoreValue("startAlarm","0");
    alarmTriggered = true;
   
    }

    jsonData.clear();
    payload.get(jsonData, "fields/stopAlarm/stringValue", true);
    if(jsonData.stringValue == "1"){
      Serial.println("Alarm Stopped! Setting to 0");

    setFirestoreValue("stopAlarm","0");
    alarmTriggered = false;

    }

    //Code to set the startCal to 0 to verify it reads through
    jsonData.clear();
        payload.get(jsonData, "fields/startCal/stringValue", true);
        if(jsonData.stringValue == "1"){
          Serial.println("Calibration Stopped! Setting to 0");

        setFirestoreValue("startCal","0");
        alarmTriggered = false;

    }


    jsonData.clear();
    payload.get(jsonData, "fields/resetAll/stringValue", true);
    if(jsonData.stringValue == "1"){
      Serial.println("Resetting All Settings. Bye Bye!");

    setFirestoreValue("resetAll","0");
    clearStoredCredentials();
    ESP.restart();
   
    }    

  }
}



String setFirestoreValue(String key, String value) {
  FirebaseJson content;
  content.set("fields/" + key +"/stringValue", value);

  Firebase.Firestore.patchDocument(&fbdo, FIREBASE_PROJECT_ID, "",path.c_str(),content.raw(),key);
  return fbdo.payload().c_str();
}

void getWifiInfo() {
  WiFiClient client = server.available();   // Listen for incoming clients

  if (client) {                             // If a new client connects
    Serial.println("New Client.");
    while (!client.available()) {            // Wait until the client sends some data
      delay(1);
    }

    String request = client.readStringUntil('\r');
    Serial.println(request);
    client.flush();

    // Check if request contains WiFi credentials
    if (request.indexOf("ssid=") != -1 && request.indexOf("password=") != -1) {
      // Parse SSID and password from the request
      int ssidIndex = request.indexOf("ssid=");
      int passIndex = request.indexOf("&password=");
      int emailIndex = request.indexOf("&email=");
      int epassIndex = request.indexOf("&epassword=");
      int htmlIndex = request.indexOf(" HTTP/1.1");

    if (ssidIndex != -1 && passIndex != -1) {
      // Extract SSID and password from the request string
        String ssidStr = request.substring(ssidIndex + 5, passIndex);
        ssidStr.replace("%20"," ");
        String passStr = request.substring(passIndex + 10, emailIndex);
        String emailStr = request.substring(emailIndex + 7, epassIndex);
        String epassStr = request.substring(epassIndex + 11, htmlIndex);
   
        Serial.println(ssidStr);
        Serial.println("pass:" + passStr + ":END");
        Serial.println("email:" + emailStr + ":END");
        Serial.println("epass:" + epassStr + ":END");

        // Save SSID and password to EEPROM
        writeEEPROM(ssidAddr, ssidStr);
        writeEEPROM(passAddr, passStr);
        writeEEPROM(emailAddr, emailStr);
        writeEEPROM(epassAddr, epassStr);

        // Restart ESP8266
        ESP.restart();
      }
    }

    // Send a response to the client
    client.println("HTTP/1.1 200 OK");
    client.println("Content-Type: text/html");
    client.println();
    client.println("<!DOCTYPE HTML>");
    client.println("<html>");
    client.println("<head><title>ESP8266 Setup</title></head>");
    client.println("<body>");
    client.println("<h1>ESP8266 Setup</h1>");
    client.println("<p>Send WiFi SSID and password to set up.</p>");
    client.println("</body>");
    client.println("</html>");

    delay(1);
    Serial.println("Client disconnected.");
    client.stop();
  }
}

void resetWifiInfo(WiFiClient &client, String request) {
 
  if (client) {                             // If a new client connects
    Serial.println("New Client.");
    while (!client.available()) {            // Wait until the client sends some data
      delay(1);
    }

    Serial.println(request);
    client.flush();

    // Check if request contains WiFi credentials
    if (request.indexOf("ssid=") != -1 && request.indexOf("password=") != -1) {
      // Parse SSID and password from the request
      int ssidIndex = request.indexOf("ssid=");
      int passIndex = request.indexOf("&password=");
      int emailIndex = request.indexOf("&email=");
      int epassIndex = request.indexOf("&epassword=");
      int htmlIndex = request.indexOf(" HTTP/1.1");

    if (ssidIndex != -1 && passIndex != -1) {
      // Extract SSID and password from the request string
        String ssidStr = request.substring(ssidIndex + 5, passIndex);
        ssidStr.replace("%20"," ");
        String passStr = request.substring(passIndex + 10, emailIndex);
        String emailStr = request.substring(emailIndex + 7, epassIndex);
        String epassStr = request.substring(epassIndex + 11, htmlIndex);
   
        Serial.println(ssidStr);
        Serial.println("pass:" + passStr + ":END");
        Serial.println("email:" + emailStr + ":END");
        Serial.println("epass:" + epassStr + ":END");

        // Save SSID and password to EEPROM
        writeEEPROM(ssidAddr, ssidStr);
        writeEEPROM(passAddr, passStr);
        writeEEPROM(emailAddr, emailStr);
        writeEEPROM(epassAddr, epassStr);

        // Restart ESP8266
        ESP.restart();
      }
    }

    // Send a response to the client
    client.println("HTTP/1.1 200 OK");
    client.println("Content-Type: text/html");
    client.println();
    client.println("<!DOCTYPE HTML>");
    client.println("<html>");
    client.println("<head><title>ESP8266 Setup</title></head>");
    client.println("<body>");
    client.println("<h1>ESP8266 Setup</h1>");
    client.println("<p>Send WiFi SSID and password to set up.</p>");
    client.println("</body>");
    client.println("</html>");

    delay(1);
    Serial.println("Client disconnected.");
    client.stop();
  }
}

String getValue(String data, char separator) {
  int separatorIndex = data.indexOf(separator);
  return data.substring(separatorIndex + 1);
}

void writeEEPROM(int addr, String data) {
  for (int i = 0; i < data.length(); ++i) {
    EEPROM.write(addr + i, data[i]);
  }
  EEPROM.write(addr + data.length(), '\0'); // Null terminator
  EEPROM.commit();
}

String readEEPROM(int addr, int length) {
  char data[length];
  for (int i = 0; i < length; ++i) {
    data[i] = EEPROM.read(addr + i);
    if (data[i] == '\0') {
      break; // Reached end of string
    }
  }
  return String(data);
}

void clearStoredCredentials() {
  // Clear stored SSID and password from EEPROM
  char empty[1] = {0};
  EEPROM.begin(ssidMaxLen + passMaxLen + emailMaxLen + epassMaxLen + successMaxLen);
  EEPROM.put(ssidAddr, empty);
  EEPROM.put(passAddr, empty);
  EEPROM.put(emailAddr, empty);
  EEPROM.put(epassAddr, empty);
  EEPROM.put(successAddr, empty);
  EEPROM.commit();
  EEPROM.end();
  Serial.println("Stored WiFi credentials cleared.");
}
