package com.example.assamese_character_recognition;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.assamese_character_recognition.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.gpu.CompatibilityList;


import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

public class MainActivity extends AppCompatActivity {
    Button detectBtn;
    ImageButton takePhotoBtn, choosePhotoBtn,clearCanvasBtn;
    ImageView imageView;
    // Define the pic id
    private static final int pic_id = 123;
    private static final int PICK_IMAGE = 100;
    Uri imageUri;

    SeekBar strokeBar;

    private float floatStartX=-1,floatStartY=-1,floatEndX=-1,floatEndY=-1;

    Interpreter tflite;

    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint=new Paint();
    private int width=2;

    private int imageSize=28;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            tflite = new Interpreter(loadModelFile());
        }catch (Exception ex){
            ex.printStackTrace();
        }


        imageView = findViewById(R.id.imageView);
        takePhotoBtn =findViewById(R.id.take_photo_btn);
        choosePhotoBtn=findViewById(R.id.choose_photo_btn);
        clearCanvasBtn=findViewById(R.id.clear_canvas_btn);
        detectBtn=findViewById(R.id.detect_button);
        strokeBar=findViewById(R.id.stroke_bar);
        strokeBar.setMax(100);
        strokeBar.setMin(2);

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bitmap!=null){
                    bitmap=Bitmap.createScaledBitmap(bitmap,imageSize,imageSize,false);

                    Bitmap bmpGrayscale = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmpGrayscale);
                    Paint paint = new Paint();
                    ColorMatrix cm = new ColorMatrix();
                    cm.setSaturation(0);
                    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
                    paint.setColorFilter(f);
                    c.drawBitmap(bitmap, 0, 0, paint);

                    try {
                        classifyImage(bmpGrayscale);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,"Failed",Toast.LENGTH_SHORT).show();
                        Log.i("Error 1", String.valueOf(e));
                    }
                }
            }
        });

        clearCanvasBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bitmap=null;
                imageView.setImageDrawable(null);
            }
        });

        strokeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                width=i;
                Toast.makeText(MainActivity.this,"Brush Size "+i,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        takePhotoBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent camera_intent
                        = new Intent(

                                MediaStore
                        .ACTION_IMAGE_CAPTURE);





                // Start the activity with camera_intent,
                // and request pic id
                startActivityForResult(camera_intent, pic_id);
            }

        });
        choosePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(gallery, PICK_IMAGE);
            }
        });






    }

    private void processImage(){
        if(bitmap==null){


        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor=this.getAssets().openFd("model.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset=fileDescriptor.getStartOffset();
        long declareLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declareLength);
    }


    
    private void classifyImage(Bitmap image) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 1);
        int[] intValues = new int[imageSize * imageSize];
        image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        int pixel = 0;
        Log.i("Confidence", "3");
        //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
        try {
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; // RGB
                    // byteBuffer.putFloat(((val >> 16) & 0xFF) /255.f);
                    // byteBuffer.putFloat(((val >> 8) & 0xFF) /255.f);
                    byteBuffer.putFloat((val & 0xFF) / 255.f);
                }
                Log.i("Confidence 1", String.valueOf(pixel));
            }

            try {
                result = tflite.run(byteBuffer);
            } catch (Exception e) {
                Log.i("Confidence Failed", String.valueOf(e));
            }

        }
    }





       /* try {
            Model model = Model.newInstance(MainActivity.this);

            //Creates Inputs for referce
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 28, 28,1}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3 );
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize ];
            image.getPixels(intValues,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());

            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; //RGB

                    try{
                        byteBuffer.putFloat(((val >> 16) & 0xFF)/255.f);
                        byteBuffer.putFloat(((val >> 8) & 0xFF)/ 255.f);
                        byteBuffer.putFloat((val & 0xFF)/255.f);
                        Log.i("Pixel", String.valueOf(val));
                    }catch(Exception e){
                        Log.i("Pixel 1", String.valueOf(e));
                    }
                }


            }

            Log.i("Confidence","1");
            inputFeature0.loadBuffer(byteBuffer);
            Log.i("Confidence","2");
            try{
                Model.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                Log.i("Confidence","3");

                Log.i("Confidence","4");
                float[] confidence = outputFeature0.getFloatArray();
                Log.i("Confidence",String.valueOf(confidence.length));
                int maxPos = 0;
                float maxConfidence = 0;

                for (int i = 0; i < confidence.length; i++) {
                    if (confidence[i] > maxConfidence) {
                        maxConfidence = confidence[i];
                        maxPos = i;
                    }
                }
                Log.i("Confidence",String.valueOf(maxPos));
                Toast.makeText(MainActivity.this, String.valueOf(maxPos), Toast.LENGTH_SHORT).show();
                model.close();
            }catch(Exception e){
                Log.i("Confidence 1",String.valueOf(e));
            }



        }catch (Exception e){
            Log.i("Error 2",String.valueOf(e));
        }

        try {
            Model model = Model.newInstance(getApplicationContext());
            Log.i("Confidence","1");
            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 28, 28, 1}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 1);
            byteBuffer.order(ByteOrder.nativeOrder());
            Log.i("Confidence","2");
            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            Log.i("Confidence","3");
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            try{
                for(int i = 0; i < imageSize; i ++){
                    for(int j = 0; j < imageSize; j++){
                        int val = intValues[pixel++]; // RGB
                       // byteBuffer.putFloat(((val >> 16) & 0xFF) /255.f);
                       // byteBuffer.putFloat(((val >> 8) & 0xFF) /255.f);
                        byteBuffer.putFloat((val & 0xFF)/255.f);
                    }
                    Log.i("Confidence 1", String.valueOf(pixel));
                }
            }catch(Exception e){
                Log.i("Confidence 1", String.valueOf(e));
            }

            Log.i("Confidence","4");
            inputFeature0.loadBuffer(byteBuffer);
            Log.i("Confidence 5",String.valueOf(inputFeature0.getShape())); */
            // Runs model inference and gets result.

            // Creates inputs for reference.
       /*     TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 28, 28}, DataType.FLOAT32);
            Log.i("Confidence","2");

            TensorImage tensorImage=new TensorImage(DataType.FLOAT32);
            Log.i("Confidence","3");
            tensorImage.load(image);
            Log.i("Confidence","4");
            ByteBuffer byteBuffer=tensorImage.getBuffer();
            Log.i("Confidence","5");
            inputFeature0.loadBuffer(byteBuffer);
            Log.i("Confidence","60");
            Model.Outputs outputs = model.process(inputFeature0);
            Log.i("Confidence","61");
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            Log.i("Confidence","6");
            float[] confidences = outputFeature0.getFloatArray();
            Log.i("Confidence","7");
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            Log.i("Confidence","8");
            Log.i("Confidence",String.valueOf(confidences[28]));
            Toast.makeText(MainActivity.this, String.valueOf(maxPos), Toast.LENGTH_SHORT).show();


            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.i("Confidence",String.valueOf(e));

        }*/




   /* private Bitmap convertToBinary(Bitmap image){
        Bitmap binaryImage=Bitmap.createBitmap(imageSize,imageSize, Bitmap.Config.ARGB_8888);
        int A,R,G,B;
        int colorPixel;
        for (int i=0;i<imageSize;i++){
            for (int j=0;j<imageSize;j++){
                colorPixel=image.getPixel(i,j);
                A=Color.alpha(colorPixel);
                R=Color.alpha(colorPixel);
                B=Color.alpha(colorPixel);
                A=Color.alpha(colorPixel);

            }
        }
    }*/


    private void DrawPaintSketch(){
        if(bitmap==null ){
            bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            paint.setColor(Color.BLACK);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(width);
        }
        canvas.drawLine(floatStartX-50,floatStartY-400,floatEndX-50,floatEndY-400,paint);
        imageView.setImageBitmap(bitmap);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(event.getAction()==MotionEvent.ACTION_DOWN){
            floatStartX=event.getX();
            floatStartY=event.getY();
        }
        if(event.getAction()==MotionEvent.ACTION_MOVE){
            floatEndX= event.getX();
            floatEndY=event.getY();
            DrawPaintSketch();
            floatStartX=event.getX();
            floatStartY=event.getY();

        }
        if(event.getAction()==MotionEvent.ACTION_UP){
            floatEndX= event.getX();
            floatEndY=event.getY();
            DrawPaintSketch();
        }


        return super.onTouchEvent(event);




    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data){

        // Match the request 'pic id with requestCode

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == pic_id) {

            // BitMap is data structure of image file
            // which stor the image in memory
            bitmap= (Bitmap) data.getExtras()
                    .get("data");

            bitmap=Bitmap.createScaledBitmap(bitmap,imageSize,imageSize,false);
            // Set the image in imageview for display
            imageView.setImageBitmap(bitmap);
        }
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            imageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                bitmap=Bitmap.createScaledBitmap(bitmap,imageSize,imageSize,false);

            } catch (IOException e) {
                e.printStackTrace();
            }
            imageView.setImageBitmap(bitmap);
        }
    }

}