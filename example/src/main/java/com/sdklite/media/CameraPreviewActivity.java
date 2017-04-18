package com.sdklite.media;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.sdklite.media.example.R;

public class CameraPreviewActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = "CameraPreviewActivity";

    private SurfaceView mSurfaceView;

    private CameraDevice mDevice;

    private OrientationEventListener mOrientationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_camera_preview);

        this.mSurfaceView = (SurfaceView) findViewById(R.id.activity_camera_preview_surface);
        this.mSurfaceView.getHolder().addCallback(this);
        this.mDevice = CameraDevice.newInstance(this);
        this.mOrientationListener = new OrientationEventListener(this) {
            Orientation mOrientation = Orientation.UNDEFINED;

            @Override
            public void onOrientationChanged(final int orientation) {
                final Orientation current = Orientation.valueOf(orientation);
                Log.v(TAG, "Orientation " + current + " " + (current == this.mOrientation ? "" : "changed"));
                this.mOrientation = current;
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mOrientationListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mOrientationListener.disable();
    }

    @Override
    public void surfaceCreated(final SurfaceHolder surfaceHolder) {
        this.mDevice.startPreview(surfaceHolder);
    }

    @Override
    public void surfaceChanged(final SurfaceHolder surfaceHolder, final int format, final int width, final int height) {
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder surfaceHolder) {
        this.mDevice.stopPreview();
    }
}
