package gaurav.handgesturedetector.Detector;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class ImageClassifier {

    private final String TAG = this.getClass().getSimpleName();
    private Interpreter tflite;
    private List<String> labelList;
    private ByteBuffer inputBuffer = null;
    private float[][] mnistOutput = null;
    private static final String MODEL_PATH = "frozentensorflowModel.tflite";
    private static final String LABEL_PATH = "labels.txt";
    private static final int RESULTS_TO_SHOW = 1;
    private static final int NUMBER_LENGTH = 24;
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_IMG_SIZE_X = 28;
    private static final int DIM_IMG_SIZE_Y = 28;
    private static final int DIM_PIXEL_SIZE = 1;
    private static final int BYTE_SIZE_OF_FLOAT = 4;

    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });


    public ImageClassifier(Activity activity) {
        try {
            tflite = new Interpreter(loadModelFile(activity));
            labelList = loadLabelList(activity);
            inputBuffer =
                    ByteBuffer.allocateDirect(
                            BYTE_SIZE_OF_FLOAT * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
            inputBuffer.order(ByteOrder.nativeOrder());
            mnistOutput = new float[DIM_BATCH_SIZE][NUMBER_LENGTH];
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    protected void runInference() {
        tflite.run(inputBuffer, mnistOutput);
    }


    public String classify(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "Image classifier not initialized");
        }
        preprocess(bitmap);
        runInference();
        return printTopKLabels();
    }

    private String printTopKLabels() {
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), mnistOutput[0][i]));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }
        String textToShow = "";
        final int size = sortedLabels.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            textToShow = label.getKey();
        }
        return textToShow;
    }

    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void preprocess(Bitmap bitmap) {
        if (bitmap == null || inputBuffer == null) {
            return;
        }
        // Reset the image data
        inputBuffer.rewind();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        long startTime = SystemClock.uptimeMillis();

        // The bitmap shape should be 28 x 28
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; ++i) {
            // Set 0 for white and 255 for black pixels
            int pixel = pixels[i];
            // The color of the input is black so the blue channel will be 0xFF.
            int channel = pixel & 0xff;
            inputBuffer.putFloat(0xff - channel);
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Time cost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }

}
