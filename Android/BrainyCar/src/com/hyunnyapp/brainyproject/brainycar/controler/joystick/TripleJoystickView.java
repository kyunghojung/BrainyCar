package com.hyunnyapp.brainyproject.brainycar.controler.joystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class TripleJoystickView extends LinearLayout 
{
	private static final String TAG = TripleJoystickView.class.getSimpleName();
	private final boolean D = false;

	private Paint dbgPaint1;

	private JoystickView stickL;
	private JoystickView stickR;
	private JoystickView stickC;

	private View padL;
	private View padR;

	public TripleJoystickView(Context context) 
	{
		super(context);
		stickL = new JoystickView(context);
		stickR = new JoystickView(context);
		stickC = new JoystickView(context);
		initTripleJoystickView();
	}

	public TripleJoystickView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		stickL = new JoystickView(context, attrs);
		stickR = new JoystickView(context, attrs);
		stickC = new JoystickView(context, attrs);
		initTripleJoystickView();
	}

	private void initTripleJoystickView() 
	{
		setOrientation(LinearLayout.HORIZONTAL);

		if(D)
		{
			dbgPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
			dbgPaint1.setColor(Color.CYAN);
			dbgPaint1.setStrokeWidth(1);
			dbgPaint1.setStyle(Paint.Style.STROKE);
		}
		
		padL = new View(getContext());
		padR = new View(getContext());
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		removeView(stickL);
		removeView(stickR);
		removeView(stickC);

		float padW = (getMeasuredWidth()-(getMeasuredHeight()*3))/2;
		int joyWidth = (int) ((getMeasuredWidth()-padW*2)/3);
		LayoutParams joyLParams = new LayoutParams(joyWidth,getMeasuredHeight());

		joyLParams.gravity = Gravity.CENTER_HORIZONTAL; 
		
		stickL.setLayoutParams(joyLParams);
		stickR.setLayoutParams(joyLParams);
		stickC.setLayoutParams(joyLParams);

		stickL.TAG = "L";
		stickR.TAG = "R";
		stickC.TAG = "C";
		
		stickL.setPointerId(JoystickView.INVALID_POINTER_ID);
		stickR.setPointerId(JoystickView.INVALID_POINTER_ID);
		stickC.setPointerId(JoystickView.INVALID_POINTER_ID);

		addView(stickL);
		
		ViewGroup.LayoutParams padLParams = new ViewGroup.LayoutParams((int) padW,getMeasuredHeight());
		removeView(padL);
		padL.setLayoutParams(padLParams);
		addView(padL);
		
		addView(stickC);

		ViewGroup.LayoutParams padRParams = new ViewGroup.LayoutParams((int) padW,getMeasuredHeight());
		removeView(padR);
		padR.setLayoutParams(padLParams);
		addView(padR);
		
		addView(stickR);

	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) 
	{
		super.onLayout(changed, l, t, r, b);
		stickL.setTouchOffset(stickL.getLeft(), stickL.getTop());
		stickR.setTouchOffset(stickR.getLeft(), stickR.getTop());
		stickC.setTouchOffset(stickC.getLeft(), stickC.getTop());

		if(D)
		{
			Log.d(TAG,"onLayout()");
			Log.d(TAG,"stickL.getLeft(): "+stickL.getLeft()+", stickL.getTop(): "+stickL.getLeft());
			Log.d(TAG,"stickR.getLeft(): "+stickR.getLeft()+", stickR.getTop(): "+stickR.getLeft());
			Log.d(TAG,"stickC.getLeft(): "+stickC.getLeft()+", stickC.getTop(): "+stickC.getLeft());
		}
	}
	
	public void setAutoReturnToCenter(boolean left, boolean right, boolean center) 
	{
		stickL.setAutoReturnToCenter(left);
		stickR.setAutoReturnToCenter(right);
		stickC.setAutoReturnToCenter(center);
	}
	
	public void setOnJostickMovedListener(JoystickMovedListener left, JoystickMovedListener right, JoystickMovedListener center) 
	{
		stickL.setOnJostickMovedListener(left);
		stickR.setOnJostickMovedListener(right);
		stickC.setOnJostickMovedListener(center);
	}
	
	public void setOnJostickClickedListener(JoystickClickedListener left, JoystickClickedListener right, JoystickClickedListener center) 
	{
		stickL.setOnJostickClickedListener(left);
		stickR.setOnJostickClickedListener(right);
		stickC.setOnJostickClickedListener(center);
	}

	public void setOnJostickDoubleClickedListener(JoystickDoubleClickedListener left, JoystickDoubleClickedListener right, JoystickDoubleClickedListener center) 
	{
		stickL.setOnJostickDoubleClickedListener(left);
		stickR.setOnJostickDoubleClickedListener(right);
		stickC.setOnJostickDoubleClickedListener(center);
	}
	
	public void setYAxisInverted(boolean leftYAxisInverted, boolean rightYAxisInverted, boolean centerYAxisInvered) 
	{
		stickL.setYAxisInverted(leftYAxisInverted);
		stickR.setYAxisInverted(rightYAxisInverted);
		stickC.setYAxisInverted(centerYAxisInvered);
	}

	public void setMovementConstraint(int movementConstraint) 
	{
		stickL.setMovementConstraint(movementConstraint);
		stickR.setMovementConstraint(movementConstraint);
		stickC.setMovementConstraint(movementConstraint);
	}

	public void setMovementRange(float movementRangeLeft, float movementRangeRight, float movementRangeCenter) 
	{
		stickL.setMovementRange(movementRangeLeft);
		stickR.setMovementRange(movementRangeRight);
		stickC.setMovementRange(movementRangeCenter);
	}

	public void setMoveResolution(float leftMoveResolution, float rightMoveResolution, float centerMoveResolution) 
	{
		stickL.setMoveResolution(leftMoveResolution);
		stickR.setMoveResolution(rightMoveResolution);
		stickC.setMoveResolution(centerMoveResolution);
	}

	public void setUserCoordinateSystem(int leftCoordinateSystem, int rightCoordinateSystem, int centerCoordinateSystem) 
	{
		stickL.setUserCoordinateSystem(leftCoordinateSystem);
		stickR.setUserCoordinateSystem(rightCoordinateSystem);
		stickC.setUserCoordinateSystem(centerCoordinateSystem);
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) 
	{
		super.dispatchDraw(canvas);
		if(D)
		{
			canvas.drawRect(1, 1, getMeasuredWidth()-1, getMeasuredHeight()-1, dbgPaint1);
		}
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) 
	{
    	boolean l = stickL.dispatchTouchEvent(ev);
    	boolean r = stickR.dispatchTouchEvent(ev);
    	boolean c = stickC.dispatchTouchEvent(ev);
    	return l || r || c;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) 
	{
    	boolean l = stickL.onTouchEvent(ev);
    	boolean r = stickR.onTouchEvent(ev);
    	boolean c = stickC.onTouchEvent(ev);
    	return l || r || c;
	}

}
