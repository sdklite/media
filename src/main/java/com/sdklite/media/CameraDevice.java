package com.sdklite.media;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.util.Date;
import java.util.List;

/**
 * The abstraction of camera device
 */
public abstract class CameraDevice implements SurfaceHolder.Callback {

    private static final String TAG = "CameraDevice";

    /**
     * Callback for taking picture
     */
    public interface OnPictureTakenCallback {
        /**
         * Called when picture has been taken
         *
         * @param camera
         *            The camera device
         * @param uri
         *            The taken picture uri
         */
        void onPictureTaken(final CameraDevice camera, final Uri uri);
    }

    /**
     * Callback for auto focus
     */
    public interface OnAutoFocusCallback {
        /**
         * Called when auto focus performed
         *
         * @param camera
         *            The camera device
         * @param success
         *            A boolean indicates whether auto focus success or failed
         */
        void onAutoFocus(final CameraDevice camera, final boolean success);
    }

    private static final CameraManager MANAGER = new CameraManager();

    /**
     * Create an instance of {@link CameraDevice}
     *
     * @param context
     *            A context
     * @return an instance of {@link CameraDevice}
     */
    public static CameraDevice newInstance(final Context context) {
        return MANAGER.new CameraDeviceProxy(context);
    }

    /**
     * Starts preview with the specified surface holder
     *
     * @param holder
     *            The surface holder to preview
     */
    public final boolean startPreview(final SurfaceHolder holder) {
        return this.startPreview(holder, Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    /**
     * Starts preview with the specified surface holder
     *
     * @param holder
     *            The surface holder to preview
     * @param cameraId
     *            The camera id to use
     * @return true if the message of start view enqueued
     */
    public abstract boolean startPreview(final SurfaceHolder holder, final int cameraId);

    /**
     * Sets the camera device auto focus
     *
     * @return true if the message of auto focus enqueued
     */
    public final boolean setAutoFocus() {
        return this.setAutoFocus(MANAGER.mOnAutoFocusCallback);
    }

    /**
     * Sets the camera device auto focus with the specified callback
     *
     * @param callback
     *            The auto focus callback
     * @return true if the message of auto focus enqueued
     */
    public abstract boolean setAutoFocus(final OnAutoFocusCallback callback);

    /**
     * Stops preview
     *
     * @return true if the message of auto focus enqueued
     */
    public abstract boolean stopPreview();

    /**
     * Takes picture with the specified callback
     *
     * @param callback
     *            The take picture callback
     * @return true if the message of take picture enqueued
     */
    public abstract boolean takePicture(final OnPictureTakenCallback callback);

    private static final class CameraManager implements Handler.Callback {

        private static final int MSG_START_PREVIEW = 1;
        private static final int MSG_STOP_PREVIEW = 2;
        private static final int MSG_CONFIG_SURFACE = 3;
        private static final int MSG_AUTO_FOCUS = 4;
        private static final int MSG_TAKE_PICTURE = 5;
        private static final int MSG_ADJUST_ROTATION = 6;

        private final class CameraDeviceProxy extends CameraDevice {

            final Context mContext;

            public CameraDeviceProxy(final Context context) {
                this.mContext = context;
            }

            @Override
            public boolean startPreview(final SurfaceHolder holder, final int cameraId) {
                sendMessage(MSG_START_PREVIEW, new PreviewArguments(this, this.mContext, holder, cameraId));
                return true;
            }

            @Override
            public boolean setAutoFocus(final OnAutoFocusCallback callback) {
                if (null == mCamera) {
                    return false;
                }

                sendMessage(MSG_AUTO_FOCUS, new AutoFocusArguments(this, this.mContext, callback));
                return true;
            }

            @Override
            public boolean stopPreview() {
                if (null == mCamera) {
                    return false;
                }

                sendMessage(MSG_STOP_PREVIEW, null);
                return true;
            }

            @Override
            public boolean takePicture(final OnPictureTakenCallback callback) {
                if (null == mCamera || mCapturing || hasMessages(MSG_TAKE_PICTURE)) {
                    return false;
                }

                mCapturing = true;
                sendMessage(MSG_TAKE_PICTURE, new TakePictureArguments(this, this.mContext, mCameraId, callback));
                return true;
            }

            @Override
            public void surfaceCreated(final SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(final SurfaceHolder holder, int format, int width, int height) {
                sendMessage(MSG_CONFIG_SURFACE, new ConfigArguments(this, this.mContext, holder, format, width, height));
            }

            @Override
            public void surfaceDestroyed(final SurfaceHolder holder) {
                sendMessage(MSG_STOP_PREVIEW, null);
            }
        }

        private final HandlerThread mThread;
        private final Handler mHandler;

        private final OnAutoFocusCallback mOnAutoFocusCallback = new OnAutoFocusCallback() {
            @Override
            public void onAutoFocus(final CameraDevice camera, final boolean success) {
                Log.v(TAG, "Camera auto focus " + success);
            }
        };

        private volatile Camera mCamera;
        private volatile boolean mCapturing;
        private volatile int mCameraId;

        public CameraManager() {
            this.mThread = new HandlerThread("CameraManager");
            this.mThread.start();
            this.mHandler = new Handler(this.mThread.getLooper(), this);
        }

        @Override
        public boolean handleMessage(final Message message) {
            switch (message.what) {
                case MSG_START_PREVIEW: {
                    startPreview((PreviewArguments) message.obj);
                    break;
                }
                case MSG_STOP_PREVIEW: {
                    stopPreview();
                    break;
                }
                case MSG_CONFIG_SURFACE: {
                    configSurface((ConfigArguments) message.obj);
                    break;
                }
                case MSG_AUTO_FOCUS: {
                    setAutoFocus((AutoFocusArguments) message.obj);
                    break;
                }
                case MSG_TAKE_PICTURE: {
                    takePicture((TakePictureArguments) message.obj);
                    break;
                }
            }
            return true;
        }

        private void sendMessage(final int what) {
            this.mHandler.sendEmptyMessage(what);
        }

        private void sendMessage(final int what, final Object arg) {
            this.mHandler.obtainMessage(what, arg).sendToTarget();
        }

        private boolean hasMessages(final int what) {
            return this.mHandler.hasMessages(what);
        }

        private void startPreview(final PreviewArguments args) {
            final Configuration cfg = args.context.getResources().getConfiguration();
            Log.v(TAG, "Display orientation " + cfg.orientation);

            final int cameraId = args.cameraId;
            if (this.mCameraId == cameraId && null != this.mCamera) {
                return;
            }

            // Check device policy
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                if (args.context instanceof Activity) {
                    final DevicePolicyManager dpm = (DevicePolicyManager) args.context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                    if (dpm.getCameraDisabled(((Activity) args.context).getComponentName())) {
                        Log.e(TAG, "Camera is disabled");
                    }
                }
            }

            // Stop preview if camera is opened
            stopPreview();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                if (Camera.getNumberOfCameras() > cameraId) {
                    this.mCameraId = cameraId;
                } else {
                    this.mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
            } else {
                this.mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            }

            try {
                this.mCamera = Camera.open(this.mCameraId);
                if (null == this.mCamera) {
                    return;
                }

                final Camera.Parameters parameters = this.mCamera.getParameters();
                final List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
                final List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();

                for (final Camera.Size size : previewSizes) {
                    Log.v(TAG, "Supported preview size: " + size.width + "x" + size.height);
                }

                for (final Camera.Size size : pictureSizes) {
                    Log.v(TAG, "Supported picture size: " + size.width + "x" + size.height);
                }

                final int orientation = getCameraDisplayOrientation(args.context, this.mCameraId);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
                    parameters.setRotation(orientation);
                } else {
                    this.mCamera.setDisplayOrientation(orientation);
                }

                parameters.setPictureFormat(ImageFormat.JPEG);
                parameters.setJpegQuality(100);

                if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }

                this.mCamera.setParameters(parameters);
                this.mCamera.setPreviewDisplay(args.holder);
                this.mCamera.startPreview();
                this.mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(final boolean success, final Camera camera) {
                        Log.e(TAG, "Camera auto focus " + success);
                    }
                });
            } catch (final Exception e) {
                Log.e(TAG, "Failed to start preview", e);
                stopPreview();
            }
        }

        private void stopPreview() {
            if (null != this.mCamera) {
                this.mCamera.release();
                this.mCamera = null;
                this.mCameraId = -1;
                this.mCapturing = false;
            }
        }

        private void configSurface(final ConfigArguments args) {
            if (null == this.mCamera) {
                return;
            }

            final int width = args.width;
            final int height = args.height;
            final Camera.Parameters parameters = this.mCamera.getParameters();
            final Camera.Size previewSize = determinePreviewSize(parameters, args.isPortrait(), width, height);
            final Camera.Size pictureSize = determinePictureSize(parameters, previewSize);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            Log.v(TAG, "Set preview size " + previewSize.width + "x" + previewSize.height);
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            Log.v(TAG, "Set picture size " + pictureSize.width + "x" + pictureSize.height);
            this.mCamera.setParameters(parameters);
        }

        private void setAutoFocus(final AutoFocusArguments args) {
            if (null != this.mCamera) {
                this.mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(final boolean success, final Camera camera) {
                        args.callback.onAutoFocus(args.device, success);
                    }
                });
            }
        }

        private void takePicture(final TakePictureArguments args) {
            if (null == this.mCamera) {
                return;
            }

            if (null == args.callback) {
                return;
            }

            if (null == this.mCamera) {
                Log.e(TAG, "Preview not started");
                return;
            }

            final int cameraId = args.cameraId;
            this.mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] data, final Camera camera) {
                    try {
                        final Camera.Parameters parameters = camera.getParameters();
                        final Camera.Size size = parameters.getPictureSize();
                        final int orientation = getCameraOrientation(cameraId);
                        final String title = MediaStorage.generateFilename();
                        final String path = MediaStorage.generatePicturePath(title + ".jpeg");
                        MediaStorage.writeFile(path, data);
                        final Uri uri = MediaStorage.addImage(args.context.getContentResolver(), title, new Date(args.timestamp), null, orientation, data.length, path, size.width, size.height);
                        args.callback.onPictureTaken(args.device, uri);
                    } finally {
                        mCapturing = false;
                    }
                }
            });
        }

        private static int getCameraOrientation(final int cameraId) {
            final Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            return info.orientation;
        }

        private static int getCameraDisplayOrientation(final Context context, final int cameraId) {
            final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            final Display display = wm.getDefaultDisplay();
            final int degrees = display.getRotation() * 90;
            final Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return (360 - ((info.orientation + degrees) % 360)) % 360;
            } else {
                return (info.orientation - degrees + 360) % 360;
            }
        }

        private Camera.Size determinePreviewSize(final Camera.Parameters parameters, final boolean portrait, final int reqWidth, final int reqHeight) {
            // Meaning of width and height is switched for preview when portrait,
            // while it is the same as user's view for surface and metrics.
            // That is, width must always be larger than height for setPreviewSize.
            int reqPreviewWidth; // requested width in terms of camera hardware
            int reqPreviewHeight; // requested height in terms of camera hardware
            if (portrait) {
                reqPreviewWidth = reqHeight;
                reqPreviewHeight = reqWidth;
            } else {
                reqPreviewWidth = reqWidth;
                reqPreviewHeight = reqHeight;
            }

            // Adjust surface size with the closest aspect-ratio
            final float reqRatio = ((float) reqPreviewWidth) / reqPreviewHeight;
            float curRatio, deltaRatio;
            float deltaRatioMin = Float.MAX_VALUE;
            Camera.Size retSize = null;
            for (final Camera.Size size : parameters.getSupportedPreviewSizes()) {
                curRatio = ((float) size.width) / size.height;
                deltaRatio = Math.abs(reqRatio - curRatio);
                if (deltaRatio < deltaRatioMin) {
                    deltaRatioMin = deltaRatio;
                    retSize = size;
                }
            }

            return retSize;
        }

        private Camera.Size determinePictureSize(final Camera.Parameters parameters, final Camera.Size previewSize) {
            final List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            for (final Camera.Size size : sizes) {
                if (size.equals(previewSize)) {
                    return size;
                }
            }

            Camera.Size retSize = null;
            // if the preview size is not supported as a picture size
            final float reqRatio = ((float) previewSize.width) / previewSize.height;
            float curRatio, deltaRatio;
            float deltaRatioMin = Float.MAX_VALUE;
            for (final Camera.Size size : sizes) {
                curRatio = ((float) size.width) / size.height;
                deltaRatio = Math.abs(reqRatio - curRatio);
                if (deltaRatio < deltaRatioMin) {
                    deltaRatioMin = deltaRatio;
                    retSize = size;
                }
            }

            return retSize;
        }
    }

    private static abstract class BasicArguments {

        final CameraDevice device;
        final Context context;
        final long timestamp;

        private BasicArguments(final CameraDevice device, final Context context) {
            this.device = device;
            this.context = context;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isPortrait() {
            return this.context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        }
    }

    private static abstract class SurfaceArguments extends BasicArguments {

        final SurfaceHolder holder;

        protected SurfaceArguments(final CameraDevice device, final Context context, final SurfaceHolder holder) {
            super(device, context);
            this.holder = holder;
        }
    }

    private static final class PreviewArguments extends SurfaceArguments {

        final int cameraId;

        private PreviewArguments(final CameraDevice device, final Context context, final SurfaceHolder holder, final int cameraId) {
            super(device, context, holder);
            this.cameraId = cameraId;
        }
    }

    private static class ConfigArguments extends SurfaceArguments {

        final int width;
        final int height;
        final int format;

        private ConfigArguments(final CameraDevice device, final Context context, final SurfaceHolder holder, int format, int width, int height) {
            super(device, context, holder);
            this.width = width;
            this.height = height;
            this.format = format;
        }
    }

    private static final class AutoFocusArguments extends BasicArguments {

        final OnAutoFocusCallback callback;

        private AutoFocusArguments(final CameraDevice device, final Context context, final OnAutoFocusCallback callback) {
            super(device, context);
            this.callback = callback;
        }
    }

    private static final class TakePictureArguments extends BasicArguments {

        final int cameraId;
        final OnPictureTakenCallback callback;

        public TakePictureArguments(final CameraDevice device, final Context context, final int cameraId, final OnPictureTakenCallback callback) {
            super(device, context);
            this.cameraId = cameraId;
            this.callback = callback;
        }
    }

}
