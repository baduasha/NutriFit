package com.example.nutrifit;

import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class FoodClassifier {
    private Interpreter interpreter;
    private List<String> labels;
    private final int imageSize = 224;
    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    public FoodClassifier(Context context) {
        loadModel(context);
        loadLabels(context);
    }

    private void loadModel(Context context) {
        try {
            // Убедитесь, что имя файла соответствует тому, что в assets
            MappedByteBuffer modelBuffer = loadModelFile(context, "food_classifier_float32.tflite");
            interpreter = new Interpreter(modelBuffer);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load model: " + e.getMessage());
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws Exception {
        FileInputStream inputStream = new FileInputStream(context.getAssets().openFd(modelPath).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd(modelPath).getStartOffset();
        long declaredLength = context.getAssets().openFd(modelPath).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadLabels(Context context) {
        labels = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("labels.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
            reader.close();
            if (labels.isEmpty()) throw new RuntimeException("labels.txt is empty");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load labels.txt: " + e.getMessage());
        }
    }

    public ClassificationResult classify(Bitmap bitmap) {
        if (labels == null || labels.isEmpty()) {
            throw new IllegalStateException("Labels not loaded");
        }
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true);
        float[][][][] inputArray = new float[1][imageSize][imageSize][3];
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                int pixel = resized.getPixel(x, y);
                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;
                inputArray[0][y][x][0] = (r - MEAN[0]) / STD[0];
                inputArray[0][y][x][1] = (g - MEAN[1]) / STD[1];
                inputArray[0][y][x][2] = (b - MEAN[2]) / STD[2];
            }
        }
        float[][] outputArray = new float[1][labels.size()];
        interpreter.run(inputArray, outputArray);
        int maxIndex = 0;
        float maxValue = outputArray[0][0];
        for (int i = 1; i < labels.size(); i++) {
            if (outputArray[0][i] > maxValue) {
                maxValue = outputArray[0][i];
                maxIndex = i;
            }
        }
        return new ClassificationResult(labels.get(maxIndex), maxValue);
    }

    public static class ClassificationResult {
        public final String label;
        public final float confidence;
        public ClassificationResult(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }
}
