package gaurav.handgesturedetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import android.provider.MediaStore.Images.Media;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.net.URI;

import gaurav.handgesturedetector.Detector.ImageClassifier;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final int CAMERA_REQUEST = 1888;
    private static final int FROM_CAMERA = 1;
    private static final int FROM_GALLERY = 2;

    ImageView imageView;
    MaterialTextView tv_resulttext;
    MaterialButton btn_capture, btn_choose;
    ImageClassifier imageClassifier;
    MaterialToolbar toolbar;

    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageClassifier = new ImageClassifier(this);

        imageView = findViewById(R.id.imageView_display);
        tv_resulttext = findViewById(R.id.tv_resulttext);
        btn_capture = findViewById(R.id.btn_capture);
        btn_choose = findViewById(R.id.btn_choose);

        toolbar = findViewById(R.id.activityToolBar);

        btn_capture.setOnClickListener(view -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            } else {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, FROM_CAMERA);
            }
        });

        btn_choose.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, FROM_GALLERY);
        });


        toolbar.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {

                case R.id.about:

                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("About");
                    alertDialog.setMessage("Thank you for using the app\n\n" +
                            "Developed by Gaurav\n" +
                            "Developer mail: gaureadhikari@gmail.com");
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Close",
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.show();

                    return true;

                case R.id.catalogue:

                    ImageView imageView = new ImageView(this);
                    imageView.setImageResource(R.drawable.signs);
                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(this).
                                    setTitle("Hand Signs to Try !!").
                                    setPositiveButton("Close", (dialogInterface, i) -> dialogInterface.dismiss()).
                                    setView(imageView);
                    builder.create().show();

                    return true;

                default:
                    return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    public MainActivity() {
        super();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult: result code " + requestCode);
        Log.d(TAG, "onActivityResult: activity result " + resultCode);


        switch (requestCode) {
            case FROM_CAMERA:
                if (resultCode == Activity.RESULT_OK) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    imageView.setImageBitmap(photo);

                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(photo, 28, 28, false);
                    String guess = imageClassifier.classify(scaledBitmap);
                    Log.d(TAG, "Found Image = " + guess);

                    tv_resulttext.setVisibility(View.VISIBLE);
                    tv_resulttext.setText(String.format("%s %s", getResources().getString(R.string.Result_txt), String.valueOf(guess)));
                }
                break;

            case FROM_GALLERY:
                if (resultCode == Activity.RESULT_OK) {

                    final Uri uri = data.getData();
                    try {
                        Bitmap photo = Media.getBitmap(this.getContentResolver(), uri);
                        imageView.setImageBitmap(photo);

                        Log.d(TAG, "onActivityResult: " + photo);

                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(photo, 28, 28, false);
                        String guess = imageClassifier.classify(scaledBitmap);

                        Log.d(TAG, "Found Image = " + guess);
                        tv_resulttext.setVisibility(View.VISIBLE);
                        tv_resulttext.setText(String.format("%s %s", getResources().getString(R.string.Result_txt), String.valueOf(guess)));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;


        }
    }
}

