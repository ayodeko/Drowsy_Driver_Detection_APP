package com.example.android.gplt;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.widget.Button;
import android.widget.TextView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.example.android.gplt.camera.CameraSourcePreview;
import com.example.android.gplt.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class MainActivity extends AppCompatActivity {
    private static final String TAG = "FaceTracker";
    TextView textView;
    int fps = 0;
    int rate = 0;
    float blinkTime = 0;
    int fpsHolder = 0;
    ArrayList<Integer> fpsArray = new ArrayList<>();
    TextView textView2;
    TextView detectionTextView;
    int drowsinessFlag = 0;
    int cameraFlag;
    MediaPlayer mediaPlayer;
    AlertDialog.Builder builder;


    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private final float THRESHOLD = 0.35f;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;


    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        //Get Camera Flag from Launch Activity
        Intent intent = getIntent();
        cameraFlag = intent.getIntExtra(LaunchActivity.intentKey, 1);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);
        detectionTextView = findViewById(R.id.detectionTextView);

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mediaPlayer.pause();
                finish();
            }
        };
        builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Drowsiness Detected")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener);

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        //Select which camera to use

        if (cameraFlag == 1) {
            mCameraSource = new CameraSource.Builder(context, detector)
                    .setRequestedPreviewSize(640, 480)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedFps(30.0f)
                    .build();
        }
        if (cameraFlag == 0) {
            mCameraSource = new CameraSource.Builder(context, detector)
                    .setRequestedPreviewSize(640, 480)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(30.0f)
                    .build();
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);

            final float eulerAngleY = face.getEulerY();

            final float headHeight = face.getHeight();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView2.setText("Face Height is "+ Float.toString(headHeight));
                }
            });


            final String rightEye = mFaceGraphic.getRightEyeProbability();

            if ((face.getIsLeftEyeOpenProbability() < THRESHOLD) && (face.getIsRightEyeOpenProbability() < THRESHOLD)){
                fps++;

                if (fps == 1){
                    fpsArray.add(fpsHolder);
                    blinkTime = (fpsHolder)/30;
                    if (fpsArray.size() > 9){
                        fpsArray.clear();
                    }
                }
                else if (fps>10){
                    fpsHolder = fps;
                }


                if (fps == 1) {
                    rate = rate + 1;

                }
                String s = "";
                for (int i = 0; i < fpsArray.size(); i++) {
                    s += fpsArray.get(i) + "*";
                }

                final String finalS = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        textView.setText("Eyes Closed, Blinks is " + Integer.toString(fps) + "Blink Rate " + Integer.toString(rate) + " EulerY is " + eulerAngleY + finalS + fpsHolder);
                    }
                });

                if ((fps > 34 && fps < 40) || (eulerAngleY > 15 && eulerAngleY < 20)){

                    detectDrowsiness(1);


                }
                else if ((fps > 40) || (eulerAngleY > 20) || (eulerAngleY < -20)){
                    detectDrowsiness(2);
                }

            }

            //if ((face.getIsLeftEyeOpenProbability() > THRESHOLD) && (face.getIsRightEyeOpenProbability() > THRESHOLD)) {
            else{
                Log.i(TAG, "onUpdate: Eyes Detected");
                fps = 0;
                String s = "";
                for (int i = 0; i < fpsArray.size(); i++) {
                    s += fpsArray.get(i) + "*";
                }

                final String finalS = s;
                displayOnUI(textView, " Eyes Opened, Blinks is "+ Integer.toString(fps) + "Blink Rate " + Integer.toString(rate) + " EulerY is " + eulerAngleY + finalS + fpsHolder);


            }

        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }

    public void detectDrowsiness(int flag){
        if (flag == 1){
            displayOnUI(detectionTextView, "Drowsiness Suspected");
        }
        else if (flag == 2){
            displayOnUI(detectionTextView, "Drowsiness Detected");
            drowsinessAlert();
            mCameraSource.stop();
        }
    }
    public void displayOnUI(final TextView textView, final String string){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                textView.setText(string);
            }
        });
    }

    public void drowsinessAlert(){
        // Create the Alarm Sound


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                builder.show();
                mediaPlayer.start();
            }
        });
    }
}