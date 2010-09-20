package se.flightplanner;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.opengl.GLSurfaceView;
import android.view.View;

public class MovingMap3D extends GLSurfaceView {
	public MovingMap3D(Context context)
	{
		super(context);
		
		
		setKeepScreenOn(true);
	}

}
