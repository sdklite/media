package com.sdklite.media.widget;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.sdklite.media.CameraDevice;

/**
 * Surface view for camera preview
 */
public class CameraView extends SurfaceView {

    private final CameraDevice mCameraDevice;
    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private final SensorEventListener mSensorListener;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    public CameraView(final Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating a view from XML.
     *
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     */
    public CameraView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * This constructor of View allows subclasses to use their own base style when they are inflating.
     *
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr
     *            An attribute in the current theme that contains a reference to a style resource that supplies default values for the view. Can be 0 to not look for defaults.
     */
    public CameraView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.mCameraDevice = CameraDevice.newInstance(context);
        this.getHolder().addCallback(this.mCameraDevice);
        this.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        this.mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.mSensorListener = new SensorEventListener() {
            float mX = 0, mY = 0, mZ = 0;

            @Override
            public void onSensorChanged(final SensorEvent event) {
                final float x = event.values[0];
                final float y = event.values[1];
                final float z = event.values[2];

                if (Math.abs(x - mX) > 0.5 || Math.abs(y - mY) > 0.5 || Math.abs(z - mZ) > 0.5) {
                    mCameraDevice.setAutoFocus();
                }

                mX = x;
                mY = y;
                mZ = z;
            }

            @Override
            public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
            }
        };
        this.mAccelerometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (MotionEvent.ACTION_DOWN == event.getActionMasked()) {
            this.mCameraDevice.setAutoFocus();
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSensorManager.registerListener(this.mSensorListener, this.mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mSensorManager.unregisterListener(this.mSensorListener, this.mAccelerometer);
        super.onDetachedFromWindow();
    }
}
