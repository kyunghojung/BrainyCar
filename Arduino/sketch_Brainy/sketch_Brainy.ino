//Brainy Project
#include <Wire.h>
#include <Adafruit_MotorShield.h>
#include "utility/Adafruit_PWMServoDriver.h"
#include <Servo.h> 

Adafruit_MotorShield AFMS = Adafruit_MotorShield(); 

Adafruit_DCMotor *leftWheelMotor = AFMS.getMotor(3);   //Left
Adafruit_DCMotor *rightWheelMotor = AFMS.getMotor(4);   //Right
Adafruit_DCMotor *handMotor = AFMS.getMotor(2);
Adafruit_DCMotor *wristMotor = AFMS.getMotor(1);

enum CarDirection
{
  STOP,            //0
  RUN_FORWARD,     //1
  TURN_LEFT,       //2
  RUN_BACKWARD,    //3
  TURN_RIGHT       //4
};

enum ViewDirection
{
    SERVO_UP,
    SERVO_DOWN,
    SERVO_LEFT,
    SERVO_RIGHT
};

enum HandAction
{
    WRIST_UP,
    WRIST_DOWN,
    HAND_OPEN,
    HAND_CLOSE
};

int rangerOutput = 3; // Ranger sensor

int LED = 13;
int SERVO_LEFTRIGHT = 9;
int SERVO_UPDOWN = 10;

char currentDirection = STOP;
char currentSpeed = 0;

unsigned long moveTime = 0;
unsigned long rangerTime = 0;

Servo cameraViewLeftRight;    // Move camera Left <-> Right 0 ~ 180(center 90)
Servo cameraViewUpDown;       // Move camera Up <-> Down 80-180(center 120)
int currentViewUpDownAngle = -1;
int currentViewLeftRightAngle = -1;

int stopTimer;

String inData;

void Stop()
{
    currentDirection = STOP;

    leftWheelMotor->run(RELEASE);
    rightWheelMotor->run(RELEASE);
}

void Forward()
{
    currentDirection = RUN_FORWARD;
    //SetSpeed(255);
    leftWheelMotor->run(FORWARD);
    rightWheelMotor->run(FORWARD);
}
void Backward()
{
    currentDirection = RUN_BACKWARD;
    //SetSpeed(100);
    leftWheelMotor->run(BACKWARD);
    rightWheelMotor->run(BACKWARD);
}

void TurnLeft()
{
    currentDirection = TURN_LEFT;
    //SetSpeed(100);
    leftWheelMotor->run(FORWARD);
    rightWheelMotor->run(BACKWARD);
}

void TurnRight()
{
    currentDirection = TURN_RIGHT;
    //SetSpeed(100);
    leftWheelMotor->run(BACKWARD);
    rightWheelMotor->run(FORWARD);
}

void SetSpeed(int val)
{
    if(currentSpeed == val)
    {
        return;
    }
    
    leftWheelMotor->setSpeed(val);
    rightWheelMotor->setSpeed(val);
}

void WristAction(int val)
{
    if(val == WRIST_UP)
    {
        wristMotor->run(BACKWARD);
    }
    else
    {
        wristMotor->run(FORWARD);
    }
}

void HandAction(int val)
{
    if(val == HAND_OPEN)
    {
        handMotor->run(BACKWARD);
    }
    else
    {
        handMotor->run(FORWARD);
    }
}

void HandStop()
{
    handMotor->run(RELEASE);
    wristMotor->run(RELEASE);
}

void SetHandSpeed()
{
    handMotor->setSpeed(50);
    wristMotor->setSpeed(100);
}

void ServoCenter()
{
    currentViewLeftRightAngle = 90;
    cameraViewLeftRight.write(currentViewLeftRightAngle);
    currentViewUpDownAngle = 165;
    cameraViewUpDown.write(currentViewUpDownAngle);
}

void ServoUpDown(int UpDown)
{
    if(UpDown == SERVO_UP)
    {
        currentViewUpDownAngle = currentViewUpDownAngle - 5;

        if(currentViewUpDownAngle < 0)
        {
            currentViewUpDownAngle = 0;
        }

    }
    else if(UpDown == SERVO_DOWN)
    {
        currentViewUpDownAngle = currentViewUpDownAngle + 5;

        if(currentViewUpDownAngle > 180)
        {
            currentViewUpDownAngle = 180;
        }

    }
    else
    {
        return;
    }

    cameraViewUpDown.write(currentViewUpDownAngle);
}

void ServoLeftRight(int LeftRight)
{
    if(LeftRight == SERVO_LEFT)
    {
        currentViewLeftRightAngle = currentViewLeftRightAngle - 2;

        if(currentViewLeftRightAngle > 180)
        {
            currentViewLeftRightAngle = 180;
        }

    }
    else if(LeftRight == SERVO_RIGHT)
    {
        currentViewLeftRightAngle = currentViewLeftRightAngle + 2;

        if(currentViewLeftRightAngle < 0)
        {
            currentViewLeftRightAngle = 0;
        }

    }
    else
    {
        return;
    }
    
    cameraViewLeftRight.write(currentViewLeftRightAngle);
}

int measureDistance()
{
    long distance, val;
  
    pinMode(rangerOutput, OUTPUT);
    digitalWrite(rangerOutput, LOW);
    delayMicroseconds(2);
    digitalWrite(rangerOutput, HIGH);
    delayMicroseconds(5);
    digitalWrite(rangerOutput,LOW);
    pinMode(rangerOutput,INPUT);
  
    val = pulseIn(rangerOutput,HIGH);
    distance = val/29/2;

    return distance;
}

String readSerial() 
{
    inData = "";
    if (Serial.available() > 0) 
    {
        int h = Serial.available();
        for (int i = 0; i < h; i++) 
        {
            inData += (char)Serial.read();
        }
        return inData;
    }
    return "";
}

void setup() {
    Serial.begin(115200);
    
    AFMS.begin();
    
    SetSpeed(0);
    SetHandSpeed();
    
    Stop();
    
    rangerTime = moveTime = millis();
    
    cameraViewLeftRight.attach(SERVO_LEFTRIGHT);
    cameraViewUpDown.attach(SERVO_UPDOWN);
    ServoCenter();
}

void loop() 
{
    String distance;
    String writeStr;
    String readStr;
    int value;
    int DIndex, SIndex, CIndex, HIndex;

/*
    if(GetETime(rangerTime) > 50)
    {
        rangerTime = millis();
        
        distance = String(measureDistance(), DEC);
        
        writeStr = "D:"+distance;
        
        //Serial.print(writeStr);
    }
*/
    /*
    // 정지 후 500ms 동안은 움직이지 않는다.
    if(stopTimer != 0 && GetETime(stopTimer) < 500)
    {
        return;
    }
    else if(stopTimer != 0 && GetETime(stopTimer) >= 500)
    {
        stopTimer = 0;
    }
    */
    
    if(GetETime(moveTime) > 50)
    {
        moveTime = millis();
        
        readStr = readSerial();
        
        if(readStr.length() == 0)
        {
            return;
        }
        
        Serial.print("\nR: ");
        Serial.println(readStr);
        
        DIndex = readStr.lastIndexOf("D:");
        
        SIndex = readStr.lastIndexOf("S:");
        
        CIndex = readStr.lastIndexOf("C:");
        
        HIndex = readStr.lastIndexOf("H:");
        
        if(SIndex != -1)
        {
            Serial.println(readStr.substring(SIndex+2, SIndex+5));
            value = readStr.substring(SIndex+2, SIndex+5).toInt();

            SetSpeed(value);
        }
        
        if(DIndex != -1)
        {
            Serial.println(readStr.substring(DIndex+2, DIndex+3));
            value = readStr.substring(DIndex+2, DIndex+3).toInt();
            
            CarMove(value);
        }
        
        if(CIndex != -1)
        {
            char val;
            Serial.println(readStr.substring(CIndex+2, CIndex+3));
            value = readStr.substring(CIndex+2, CIndex+3).toInt();

            CameraMove(value);

        }
        
        if(HIndex != -1)
        {
            char val;
            Serial.println(readStr.substring(HIndex+2, HIndex+3));
            val = readStr.substring(HIndex+2, HIndex+3).charAt(0);

            HandMove(val);
        }
    }
}

void CarMove(int val)
{
    if(currentDirection == val)
    {
        return;
    }
    
    switch (val)
    {
        case 0://STOP
            stopTimer = millis();
            Serial.println("STOP");
            Stop();
            break;
        case 1://RUN_FORWARD
            Serial.println("R_F");
            Forward();
            break;
        case 2://TRUN_LEFT 
            Serial.println("T_L");
            TurnLeft();
            break;
        case 3://RUN_BACKWARD
            Serial.println("R_B");
            Backward();
            break;
        case 4://TURN_RIGHT
            Serial.println("T_R");
            TurnRight();
            break;
        default:
            //Stop();
            break;
    }
}

void CameraMove(int val)
{
    switch (val)
    {
        case 5://SERVO_CENTER
            Serial.println("S_C");
            ServoCenter();
            break;
        case 6://SERVO_UP
            Serial.println("S_U");
            ServoUpDown(SERVO_UP);
            break;
        case 7://SERVO_DOWN
            Serial.println("S_D");
            ServoUpDown(SERVO_DOWN);
            break;
        case 8://SERVO_LEFT
            Serial.println("S_L");
            ServoLeftRight(SERVO_LEFT);
            break;
        case 9://SERVO_RIGHT
            Serial.println("S_R");
            ServoLeftRight(SERVO_RIGHT);
            break;
        default:
            //Stop();
            break;
    }
}

void HandMove(char val)
{
    switch(val)
    {
        case 'a'://WRIST_UP
            Serial.println("W_U");
            WristAction(WRIST_UP);
            break;
        case 'b'://WRIST_DOWN
            Serial.println("W_D");
            WristAction(WRIST_DOWN);
            break;
        case 'c'://HAND_OPEN
            Serial.println("H_O");
            HandAction(HAND_OPEN);
            break;
        case 'd'://HAND_CLOSE
            Serial.println("H_C");
            HandAction(HAND_CLOSE);
            break;
        case '0':
            Serial.println("H_S");
            HandStop();
            break;
        default:
            break;
    }
}
unsigned long GetETime(unsigned long referenceTime)
{
  unsigned long returnValue;
  unsigned long currentMillis = millis();
  if (referenceTime > currentMillis)
  {
    returnValue = 4294967295 + (currentMillis - referenceTime);
    returnValue++;
  }
  else
  {
    returnValue = currentMillis - referenceTime;
  }
  return returnValue;
}

unsigned long GetEmicroTime(unsigned long referenceTime)
{
  unsigned long returnValue;
  unsigned long currentMicros = micros();
  if (referenceTime > currentMicros)
  {
    returnValue = 4294967295 + (currentMicros - referenceTime);
    returnValue++;
  }
  else
  {
    returnValue = currentMicros - referenceTime;
  }
  return returnValue;
}

