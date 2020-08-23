package com.example.eyetrackerdemo;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class FaceDetect extends AppCompatActivity {

    String TAG = "ScannerActivity";

    GraphicOverlay barcodeOverlay;
    CameraSourcePreview preview;
    private CameraSource mCameraSource = null;
    List< FirebaseVisionFace > capturedFaces;
    List< FirebaseVisionPoint > leftEyeContour;
    List< FirebaseVisionPoint > rightEyeContour;

    FirebaseVisionFaceLandmark leftEye;
    FirebaseVisionFaceLandmark rightEye;

    FirebaseVisionPoint leftEyePos;
    FirebaseVisionPoint rightEyePos;



    FaceDetectionProcessor faceDetectionProcessor;
    FaceDetectionResultListener faceDetectionResultListener = null;

    Bitmap bmpCapturedImage;



    private static final int REQUEST_CAMERA_PERMISSION = 1;

    int counter = 0;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detect);



            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    Intent i = new Intent(FaceDetect.this, MainUppLeft.class);

                    startActivity(i);
                    //Intent intent = new Intent(FaceDetect.this,MainActivity.class);
                   // intent.putExtra("key", value);
                    //(intent);

                    finish();


                }
            },15000);
      
            preview = findViewById(R.id.preview);
            barcodeOverlay = findViewById(R.id.barcodeOverlay);

            if (ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // permission not granted, initiate request
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
            } else {
                createCameraSource();

            }

    }


    private void createCameraSource() {

        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                        .enableTracking()
                        .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);

        // To connect the camera resource with the detector

        mCameraSource = new CameraSource(this, barcodeOverlay);
        mCameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);


        // FaceContourDetectorProcessor faceDetectionProcessor = new FaceContourDetectorProcessor(detector);

        faceDetectionProcessor = new FaceDetectionProcessor(detector);
        faceDetectionProcessor.setFaceDetectionResultListener(getFaceDetectionListener());

        mCameraSource.setMachineLearningFrameProcessor(faceDetectionProcessor);

        startCameraSource();
    }

    private FaceDetectionResultListener getFaceDetectionListener() {
        if (faceDetectionResultListener == null)
            faceDetectionResultListener = new FaceDetectionResultListener() {
                @Override
                public void onSuccess(@Nullable Bitmap originalCameraImage, @NonNull List<FirebaseVisionFace> faces, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay) {
                    boolean isEnable;
                    isEnable = faces.size() > 0;
                    JSONObject jsonObject = new JSONObject();


                    //return jsonObject;


                    for (FirebaseVisionFace face : faces)
                    {
                        long currentTime = Calendar.getInstance().getTimeInMillis();

                        // To get the results

                        Log.d(TAG, "Face bounds : " + face.getBoundingBox());


                        // To get this, we have to set the ClassificationMode attribute as ALL_CLASSIFICATIONS
                        String leftEyeProp = String.valueOf(face.getLeftEyeOpenProbability());
                        String rightEyeProp = String.valueOf(face.getRightEyeOpenProbability());
                        Log.d(TAG, "Left eye open probability : " + face.getLeftEyeOpenProbability());
                        Log.d(TAG, "Right eye open probability : " + face.getRightEyeOpenProbability());
                        Log.d(TAG, "Smiling probability : " + face.getSmilingProbability());
                     //   appendLog("Left Eye Propability: " + currentTime);
                     //   appendLog(leftEyeProp);

                        try {
                            jsonObject.put("Face ID", face.getTrackingId());
                            jsonObject.put("Position", "Left Up");
                            jsonObject.put("Timestamp", currentTime);
                            jsonObject.put("Left Eye Propability", leftEyeProp);
                            jsonObject.put("Right Eye Propability", rightEyeProp);
                            if (face.getLeftEyeOpenProbability() <= 0.4 || face.getLeftEyeOpenProbability() <= 0.4){
                                counter++;
                                jsonObject.put("Blinking", 1);
                            }

                            else
                                jsonObject.put("Blinking", 0);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                      //  appendLog("Right Eye Propability: " + currentTime);
                       // appendLog(rightEyeProp);

                        // To get this, we have to enableTracking

                        Log.d(TAG, "Face ID : " + face.getTrackingId());


                        //Log.d(TAG, "Left eye contour: " + Arrays.toString(leftEyeContour));

                        leftEyeContour = face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
                        //appendLog(leftEyeContour.toString());
                        try {
                            for (int i=0; i<leftEyeContour.size(); i++){
                                Log.d(TAG, "Left eye contour: " + i + ' ' + leftEyeContour.get(i).getX().toString());
                                Log.d(TAG, "Left eye contour: " + i + ' ' + leftEyeContour.get(i).getY().toString());


                                jsonObject.put("Left Eye contour x " + i, leftEyeContour.get(i).getX().toString());
                                jsonObject.put("Left Eye contour y " + i, leftEyeContour.get(i).getY().toString());
                            }

                            //appendLog(Arrays.toString(rightEyeContour.get(i).getX().toString()));
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }

                        rightEyeContour = face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints();
                        //appendLog(leftEyeContour.toString());
                        try {
                            for (int i=0; i<rightEyeContour.size(); i++){
                                Log.d(TAG,"Point" + i);


                                jsonObject.put("Right Eye contour x " + i, rightEyeContour.get(i).getX().toString());
                                jsonObject.put("Right Eye contour y " + i, rightEyeContour.get(i).getY().toString());



                            }
                            //appendLog(Arrays.toString(rightEyeContour.get(i).getX().toString()));
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //appendLog("Right eye contour " + currentTime);
                        //appendLog(Arrays.toString(new List[]{contour.getPoints()}));



                        // If contour detection was enabled:
                        List<FirebaseVisionPoint> leftEyeCon =
                                face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
                       // appendLog("Left Eye Contour " + currentTime + leftEyeCon.toString());

                        leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
                        if (leftEye != null) {
                            leftEyePos = leftEye.getPosition();
                            Log.d(TAG, "Left eye position: " + leftEyePos);
                        //    appendLog("Left Eye Position " + currentTime);

                          ///  appendLog(leftEyePos.toString());
                            try {
                                jsonObject.put("Left Eye position x", leftEyePos.getX());
                                jsonObject.put("Left Eye position y", leftEyePos.getY());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);
                        if (rightEye != null) {
                            rightEyePos = rightEye.getPosition();
                            Log.d(TAG, "Right eye position: " + rightEyePos);
                           // appendLog("Right Eye Position " + currentTime);
                           // appendLog(rightEyePos.toString());
                            try {
                                jsonObject.put("Right Eye position x", rightEyePos.getX());
                                jsonObject.put("Right Eye position y", rightEyePos.getY());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    try {
                        jsonObject.put("Total Blinks", counter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String userString = jsonObject.toString();
                    // Define the File Path and its Name
                    appendLog(userString);
// Define the File Path and its Name
                   // File file = new File("sdcard/gliko.file");
                  //  FileWriter fileWriter = null;
                   // try {
                    //    fileWriter = new FileWriter(file);
                    //    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    //    bufferedWriter.write(userString);
                    //    bufferedWriter.close();
                    //} catch (IOException e) {
                    //    e.printStackTrace();
                   // }


                    runOnUiThread(() -> {
                        Log.d(TAG, "button enable true ");
                        bmpCapturedImage = originalCameraImage;
                        capturedFaces = faces;

                    });
                }


                @Override
                public void onFailure(@NonNull Exception e) {

                }
            };

        return faceDetectionResultListener;
    }




    public FirebaseVisionPoint getLeftEyePosition(FirebaseVisionFace face){
        FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);

        if(leftEye != null){
            FirebaseVisionPoint leftEyePosition = leftEye.getPosition();
            return leftEyePosition;
        }

        return null;
    }

    public FirebaseVisionPoint getRightEyePosition(FirebaseVisionFace face){
        FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);

        if(rightEye != null){
            FirebaseVisionPoint rightEyePosition = rightEye.getPosition();
            return rightEyePosition;
        }

        return null;
    }

    List<FirebaseVisionPoint> getLeftEyeContour(FirebaseVisionFace face){
        FirebaseVisionPoint leftEyePosition = getLeftEyePosition(face);

        if(leftEyePosition != null){
            List<FirebaseVisionPoint> leftEyeContour = face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
            return leftEyeContour;
        }

        return null;
    }

    List<FirebaseVisionPoint> getRightEyeContour(FirebaseVisionFace face){
        FirebaseVisionPoint rightEyePosition = getRightEyePosition(face);

        if(rightEyePosition != null){
            List<FirebaseVisionPoint> rightEyeContour = face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints();
            return rightEyeContour;
        }

        return null;
    }
    public void appendLog(String text)
    {
        File logFile = new File("sdcard/gliko.json");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));

            buf.append(text);
            buf.newLine();
            buf.close();


        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }


    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());

        Log.d(TAG, "startCameraSource: " + code);

        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, REQUEST_CAMERA_PERMISSION);
            dlg.show();
        }

        if (mCameraSource != null && preview != null && barcodeOverlay != null) {
            try {
                Log.d(TAG, "startCameraSource: ");
                preview.start(mCameraSource, barcodeOverlay);
            } catch (IOException e) {
                Log.d(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        } else
            Log.d(TAG, "startCameraSource: not started");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult: " + requestCode);
        preview.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (preview != null)
            preview.stop();
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


}
