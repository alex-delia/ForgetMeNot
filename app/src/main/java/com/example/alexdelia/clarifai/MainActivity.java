package com.example.alexdelia.clarifai;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.Model;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;


public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCamera = (Button)findViewById(R.id.btnCamera);
        imageView = findViewById(R.id.imageView);
        output = findViewById((R.id.Output));

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final ClarifaiClient client = new ClarifaiBuilder("965a7dc6b690415aad6aec7df983bbaf").buildSync();

        Model<Concept> generalModel = client.getDefaultModels().generalModel();

        final File image = new File(mCurrentPhotoPath);
        final Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());



        new Thread(new Runnable() {
            public void run() {
                ExifInterface ei = null;
                try {
                    ei = new ExifInterface(mCurrentPhotoPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);

                final Bitmap rotatedBitmap;
                switch(orientation) {

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotatedBitmap = rotateImage(bitmap, 90);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotatedBitmap = rotateImage(bitmap, 180);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotatedBitmap = rotateImage(bitmap, 270);
                        break;

                    case ExifInterface.ORIENTATION_NORMAL:
                    default:
                        rotatedBitmap = bitmap;
                }
                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(rotatedBitmap);
                    }
                });

                try {
                    FileOutputStream fos = new FileOutputStream(image);
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                final ClarifaiResponse<List<ClarifaiOutput<Concept>>> responseList =
                        client.getDefaultModels().generalModel().predict()
                                .withInputs(ClarifaiInput.forImage(image))
                                .executeSync();
                output.post(new Runnable() {
                    @Override
                    public void run() {
                       output.setText(responseList.get().get(0).data().get(1).name());
                       if(responseList.get().get(0).data().get(1).name().equals("toothbrush")){
                           Uri uri = Uri.parse("https://www.youtube.com/watch?v=JLmenBLKTb0"); // missing 'http://' will cause crashed
                           Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                           startActivity(intent);
                       } else if (responseList.get().get(0).data().get(1).name().equals("medicine")){
                           Uri uri = Uri.parse("http://www.ismrd.org/news_and_events/?a=13503"); // missing 'http://' will cause crashed
                           Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                           startActivity(intent);
                       } else if (responseList.get().get(0).data().get(1).name().equals("")){

                       }else {

                       }
                    }
                });

            }
        }).start();
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.alexdelia.clarifai.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
}

