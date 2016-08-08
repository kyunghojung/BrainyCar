package com.hyunnyapp.brainyproject.brainycar.arduino;

public class ArduinoInfo
{
	private final String TAG = "ArduinoControler";


    // Direction
    public static final int STOP = 0;
    public static final int RUN_FORWARD = 1;
    public static final int TRUN_LEFT = 2;
    public static final int RUN_BACKWARD = 3;
    public static final int TURN_RIGHT = 4;

    public static final int SERVO_CENTER = 5;
    public static final int SERVO_UP = 6;
    public static final int SERVO_DOWN = 7;
    public static final int SERVO_LEFT = 8;
    public static final int SERVO_RIGHT= 9;

    public static final int WRIST_UP = 10;
    public static final int WRIST_DOWN = 11;
    public static final int HAND_OPEN = 12;
    public static final int HAND_CLOSE = 13;
    
	public static int get8Direction(int pan, int tilt)
	{
		// limit to {0..10}
		int radius = (byte) ( Math.min( Math.sqrt((pan*pan) + (tilt*tilt)), 10.0 ) );
		// scale to {0..35}
		int angle = (byte) ( Math.atan2(-pan, -tilt) * 18.0 / Math.PI + 36.0 + 0.5 );

		if( angle >= 36 )	
			angle = (byte)(angle-36);
		
		if((angle >= 0 && angle < 3) || (angle >= 34 && angle < 36))
			return RUN_FORWARD;
		else if(angle >= 3 && angle < 7)
			return RUN_FORWARD;
		else if(angle >= 7 && angle < 12)
			return TRUN_LEFT;
		else if(angle >= 12 && angle < 16)
			return RUN_BACKWARD;
		else if(angle >= 16 && angle < 21)
			return RUN_BACKWARD;
		else if(angle >= 21 && angle < 25)
			return RUN_BACKWARD;
		else if(angle >= 25 && angle < 30)
			return TURN_RIGHT;
		else if(angle >= 30 && angle < 34)
			return RUN_FORWARD;
		else
			return STOP;
    }

	public static int get8Direction(int angle) 
	{

		if((angle >= 0 && angle < 3) || (angle >= 34 && angle < 36))
			return RUN_FORWARD;
		else if(angle >= 3 && angle < 7)
			return RUN_FORWARD;
		else if(angle >= 7 && angle < 12)
			return TRUN_LEFT;
		else if(angle >= 12 && angle < 16)
			return RUN_BACKWARD;
		else if(angle >= 16 && angle < 21)
			return RUN_BACKWARD;
		else if(angle >= 21 && angle < 25)
			return RUN_BACKWARD;
		else if(angle >= 25 && angle < 30)
			return TURN_RIGHT;
		else if(angle >= 30 && angle < 34)
			return RUN_FORWARD;
		else
			return STOP;
    }

	public static int get4Direction(int pan, int tilt)
	{
		// limit to {0..10}
		int radius = (byte) ( Math.min( Math.sqrt((pan*pan) + (tilt*tilt)), 10.0 ) );
		// scale to {0..35}
		int angle = (byte) ( Math.atan2(-pan, -tilt) * 18.0 / Math.PI + 36.0 + 0.5 );

		if( angle >= 36 )	
			angle = (byte)(angle-36);
		
		if((angle >= 0 && angle < 5) || (angle >= 32 && angle < 36))
			return RUN_FORWARD;
		else if(angle >= 5 && angle < 14)
			return TRUN_LEFT;
		else if(angle >= 14 && angle < 23)
			return RUN_BACKWARD;
		else if(angle >= 23 && angle < 32)
			return TURN_RIGHT;
		else
			return STOP;
    }

	public static int get4Direction(int angle) 
    {
		if((angle >= 0 && angle < 5) || (angle >= 32 && angle < 36))
			return RUN_FORWARD;
		else if(angle >= 5 && angle < 14)
			return TRUN_LEFT;
		else if(angle >= 14 && angle < 23)
			return RUN_BACKWARD;
		else if(angle >= 23 && angle < 32)
			return TURN_RIGHT;
		else
			return STOP;
    }

	public static String getDirectionString(int direction) 
	{
		switch(direction)
		{
		case RUN_FORWARD:
			return "RUN_FORWARD";
		case TRUN_LEFT:
			return "TRUN_LEFT";
		case RUN_BACKWARD:
			return "RUN_BACKWARD";
		case TURN_RIGHT:
			return "TURN_RIGHT";
		case STOP:
			return "STOP";
		}
		return "MOVE_NONE";
    }

	public static int getServoDirection(int angle)
	{

		if((angle >= 0 && angle < 5) || (angle >= 32 && angle < 36))
			return SERVO_UP;
		else if(angle >= 5 && angle < 14)
			return SERVO_LEFT;
		else if(angle >= 14 && angle < 23)
			return SERVO_DOWN;
		else if(angle >= 23 && angle < 32)
			return SERVO_RIGHT;
		else
			return STOP;
	}

	public static String getServoDirectionString(int direction) 
	{
		switch(direction)
		{
		case SERVO_CENTER:
			return "SERVO_CENTER";
		case SERVO_UP:
			return "SERVO_UP";
		case SERVO_DOWN:
			return "SERVO_LEFT";
		case SERVO_LEFT:
			return "SERVO_RIGHT_45";
		case SERVO_RIGHT:
			return "SERVO_RIGHT";
		}
		return "MOVE_NONE";
    }

	public static int getHandAction(int angle)
	{

		if((angle >= 0 && angle < 5) || (angle >= 32 && angle < 36))
			return WRIST_UP;
		else if(angle >= 14 && angle < 23)
			return WRIST_DOWN;
		else if(angle >= 5 && angle < 14)
			return HAND_OPEN;
		else if(angle >= 23 && angle < 32)
			return HAND_CLOSE;
		else
			return STOP;
	}
	
	public static String getHandActionString(int direction) 
	{
		switch(direction)
		{
		case WRIST_UP:
			return "WRIST_UP";
		case WRIST_DOWN:
			return "WRIST_DOWN";
		case HAND_OPEN:
			return "HAND_OPEN";
		case HAND_CLOSE:
			return "HAND_CLOSE";
		}
		return "MOVE_NONE";
    }
}
