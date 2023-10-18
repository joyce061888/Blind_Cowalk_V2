package com.example.blind_cowalk_v2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

//import com.example.blind_cowalk_v2.ml.Model;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.camera.core.ExperimentalGetImage;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private ImageAnalysis imageAnalysis;

    int imageWidth;
    int imageHeight;

    @ExperimentalGetImage
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.cameraPreview);

        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initializeCamera();
        } else {
            // Permission is not granted; request it.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

//        LoadTorchModule("pytorch.ptl");
    }

    @ExperimentalGetImage
    private void initializeCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // Handle the error
                Log.e("CameraX", "Error initializing camera", e); // Add this line to log the error
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @ExperimentalGetImage
    private void startCamera(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        setupImageAnalysis();

        cameraProvider.unbindAll(); // Unbind any existing use cases.
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);
    }

    private void segmentImage(Bitmap image) {
//        Log.d("DebugTag", "segmentImage method");
//        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(image,
//                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
//        final float[] inputs = inputTensor.getDataAsFloatArray();

        // Debug print: Print the dimensions of the 'inputs' array
//        Log.d("DebugTag", "Input Array Dimensions: " + inputs.length);

        // Convert the Bitmap to a Torch tensor
//        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(image,
//                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        // Print the input tensor values
//        float[] inputArray = inputTensor.getDataAsFloatArray();
//        for (float value : inputArray) {
//            Log.d("InputTensor", String.valueOf(value));
//        }

        // Run model inference
//        IValue inputs = IValue.from(inputTensor);
//        Tensor outputTensor = module.forward(inputs).toTensor();

        // Process the output tensor as needed
        // ...
    }

//    Module module;
//    void LoadTorchModule(String fileName) {
//        File modelFile = new File(this.getFilesDir(), fileName);
//        try {
//            if (!modelFile.exists()) {
//                InputStream inputStream = getAssets().open(fileName);
//                FileOutputStream outputStream = new FileOutputStream(modelFile);
//                byte[] buffer = new byte[2048];
//                int bytesRead = -1;
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    outputStream.write(buffer, 0, bytesRead);
//                }
//                inputStream.close();
//                outputStream.close();
//            }
//            module = LiteModuleLoader.load(modelFile.getAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @ExperimentalGetImage
    private Bitmap getBitmapFromImageProxy(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            return null;
        }
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    @ExperimentalGetImage
    private void processFrame(ImageProxy imageProxy) {
        // Process the frame here
        Bitmap frame = getBitmapFromImageProxy(imageProxy);
        if (frame != null) {
            // Resize the frame
            frame = Bitmap.createScaledBitmap(frame, 2048, 1024, false);
        }

        // Process the frame (e.g., apply segmentation)
        segmentImage(frame);

        // Close the ImageProxy
        imageProxy.close();
    }

    @ExperimentalGetImage
    private void setupImageAnalysis() {
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(512, 512))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new ImageAnalysis.Analyzer() {
            @Override
            // calls this in every frame
            public void analyze(@NonNull ImageProxy imageProxy) {
                processFrame(imageProxy);
            }
        });
    }


    @ExperimentalGetImage
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                // Handle the case where the user denied camera permission (e.g., show a message).
            }
        }
    }
}
