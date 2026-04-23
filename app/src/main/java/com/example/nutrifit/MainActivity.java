package com.example.nutrifit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FoodClassifier classifier;
    private Uri selectedImageUri;
    private Bitmap currentBitmap = null;
    private String currentLabel = "";
    private TextView tvResult, tvCalories, tvWeightValue;
    private ImageView ivPreview;
    private SeekBar seekBarWeight;
    private int currentWeight = 100;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        classifier = new FoodClassifier(this);

        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnGallery = findViewById(R.id.btnGallery);
        Button btnCalc = findViewById(R.id.btnCalc);
        tvResult = findViewById(R.id.tvResult);
        tvCalories = findViewById(R.id.tvCalories);
        tvWeightValue = findViewById(R.id.tvWeightValue);
        ivPreview = findViewById(R.id.ivPreview);
        seekBarWeight = findViewById(R.id.seekBarWeight);

        btnCamera.setOnClickListener(v -> checkCameraPermission());
        btnGallery.setOnClickListener(v -> checkStoragePermission());
        btnCalc.setOnClickListener(v -> calculateCalories());

        seekBarWeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentWeight = progress;
                tvWeightValue.setText(currentWeight + " г");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        seekBarWeight.setProgress(100);

        Glide.with(this).load(android.R.drawable.ic_menu_camera).into(ivPreview);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            takePhoto();
        }
    }

    private void checkStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_STORAGE_PERMISSION);
        } else {
            openGallery();
        }
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Камера не найдена", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) takePhoto();
            else Toast.makeText(this, "Нет разрешения на камеру", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) openGallery();
            else Toast.makeText(this, "Нет разрешения на чтение галереи", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Операция отменена", Toast.LENGTH_SHORT).show();
            return;
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
            Bundle extras = data.getExtras();
            currentBitmap = (Bitmap) extras.get("data");
            if (currentBitmap == null) {
                Toast.makeText(this, "Не удалось получить фото", Toast.LENGTH_SHORT).show();
                return;
            }
            ivPreview.setImageBitmap(currentBitmap);
            performClassification(currentBitmap);
        } else if (requestCode == REQUEST_GALLERY && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri == null) {
                Toast.makeText(this, "Не удалось получить URI фото", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                if (currentBitmap == null) {
                    Toast.makeText(this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show();
                    return;
                }
                Glide.with(this).load(selectedImageUri).into(ivPreview);
                performClassification(currentBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка чтения изображения: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void performClassification(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            Toast.makeText(this, "Изображение пустое", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            FoodClassifier.ClassificationResult result = classifier.classify(bitmap);
            currentLabel = result.label;
            int percent = (int) (result.confidence * 100);
            if (percent > 100) percent = 100;
            tvResult.setText(currentLabel + " (" + percent + "%)");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка классификации: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void calculateCalories() {
        if (currentLabel.isEmpty()) {
            Toast.makeText(this, "Сначала сделайте фото или выберите из галереи", Toast.LENGTH_SHORT).show();
            return;
        }
        String keyForMap = currentLabel.toLowerCase().replace(' ', '_');
        Map<String, Integer> caloriesMap = new HashMap<>();
        caloriesMap.put("apple_pie", 265);
        caloriesMap.put("baby_back_ribs", 320);
        caloriesMap.put("baklava", 480);
        caloriesMap.put("beef_carpaccio", 170);
        caloriesMap.put("beef_tartare", 200);
        caloriesMap.put("beet_salad", 65);
        caloriesMap.put("beignets", 380);
        caloriesMap.put("bibimbap", 180);
        caloriesMap.put("bread_pudding", 250);
        caloriesMap.put("breakfast_burrito", 300);
        caloriesMap.put("bruschetta", 120);
        caloriesMap.put("caesar_salad", 180);
        caloriesMap.put("cannoli", 350);
        caloriesMap.put("caprese_salad", 150);
        caloriesMap.put("carrot_cake", 380);
        caloriesMap.put("ceviche", 100);
        caloriesMap.put("cheese_plate", 400);
        caloriesMap.put("cheesecake", 320);
        caloriesMap.put("chicken_curry", 210);
        caloriesMap.put("chicken_wings", 260);
        caloriesMap.put("chocolate_cake", 350);
        caloriesMap.put("chocolate_mousse", 300);
        caloriesMap.put("churros", 400);
        caloriesMap.put("clam_chowder", 110);
        caloriesMap.put("club_sandwich", 320);
        caloriesMap.put("crab_cakes", 220);
        caloriesMap.put("creme_brulee", 330);
        caloriesMap.put("croque_madame", 380);
        caloriesMap.put("cup_cakes", 350);
        caloriesMap.put("deviled_eggs", 150);
        caloriesMap.put("donuts", 350);
        caloriesMap.put("dumplings", 200);
        caloriesMap.put("edamame", 120);
        caloriesMap.put("eggs_benedict", 290);
        caloriesMap.put("escargots", 160);
        caloriesMap.put("falafel", 330);
        caloriesMap.put("filet_mignon", 270);
        caloriesMap.put("fish_and_chips", 300);
        caloriesMap.put("foie_gras", 460);
        caloriesMap.put("french_fries", 310);
        caloriesMap.put("french_onion_soup", 90);
        caloriesMap.put("french_toast", 250);
        caloriesMap.put("fried_calamari", 190);
        caloriesMap.put("fried_rice", 150);
        caloriesMap.put("frozen_yogurt", 160);
        caloriesMap.put("garlic_bread", 150);
        caloriesMap.put("gnocchi", 180);
        caloriesMap.put("greek_salad", 100);
        caloriesMap.put("grilled_cheese_sandwich", 380);
        caloriesMap.put("grilled_salmon", 230);
        caloriesMap.put("guacamole", 160);
        caloriesMap.put("gyoza", 200);
        caloriesMap.put("hamburger", 290);
        caloriesMap.put("hot_and_sour_soup", 80);
        caloriesMap.put("hot_dog", 290);
        caloriesMap.put("huevos_rancheros", 260);
        caloriesMap.put("hummus", 170);
        caloriesMap.put("ice_cream", 210);
        caloriesMap.put("lasagna", 280);
        caloriesMap.put("lobster_bisque", 120);
        caloriesMap.put("lobster_roll", 260);
        caloriesMap.put("macaroni_and_cheese", 280);
        caloriesMap.put("macarons", 390);
        caloriesMap.put("miso_soup", 40);
        caloriesMap.put("mussels", 140);
        caloriesMap.put("nachos", 300);
        caloriesMap.put("omelette", 150);
        caloriesMap.put("onion_rings", 360);
        caloriesMap.put("oysters", 80);
        caloriesMap.put("pad_thai", 220);
        caloriesMap.put("paella", 180);
        caloriesMap.put("pancakes", 230);
        caloriesMap.put("panna_cotta", 280);
        caloriesMap.put("peking_duck", 330);
        caloriesMap.put("pho", 180);
        caloriesMap.put("pizza", 270);
        caloriesMap.put("pork_chop", 240);
        caloriesMap.put("poutine", 290);
        caloriesMap.put("prime_rib", 280);
        caloriesMap.put("pulled_pork_sandwich", 330);
        caloriesMap.put("ramen", 98);
        caloriesMap.put("ravioli", 180);
        caloriesMap.put("red_velvet_cake", 370);
        caloriesMap.put("risotto", 160);
        caloriesMap.put("samosa", 260);
        caloriesMap.put("sashimi", 120);
        caloriesMap.put("scallops", 150);
        caloriesMap.put("seaweed_salad", 50);
        caloriesMap.put("shrimp_and_grits", 230);
        caloriesMap.put("spaghetti_bolognese", 190);
        caloriesMap.put("spaghetti_carbonara", 220);
        caloriesMap.put("spring_rolls", 180);
        caloriesMap.put("steak", 270);
        caloriesMap.put("strawberry_shortcake", 290);
        caloriesMap.put("sushi", 150);
        caloriesMap.put("tacos", 210);
        caloriesMap.put("takoyaki", 180);
        caloriesMap.put("tiramisu", 320);
        caloriesMap.put("tuna_tartare", 170);
        caloriesMap.put("waffles", 280);

        Integer caloriesPer100g = caloriesMap.get(keyForMap);
        if (caloriesPer100g != null) {
            int total = (int) (caloriesPer100g * currentWeight / 100.0);
            tvCalories.setText(total + " ккал (вес " + currentWeight + " г)");
        } else {
            tvCalories.setText("Нет данных о калориях для \"" + currentLabel + "\"");
        }
    }
}
