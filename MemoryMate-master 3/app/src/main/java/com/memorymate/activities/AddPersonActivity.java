package com.memorymate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.memorymate.R;
import com.memorymate.models.Person;
import com.memorymate.utils.CloudSyncManager;
import com.memorymate.utils.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddPersonActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private CircleImageView ivPhoto;
    private EditText etName, etRelationship, etPhone;
    private Button btnSave, btnCancel;
    private DatabaseHelper databaseHelper;
    private Uri selectedImageUri;
    private String savedImagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_person);

        ivPhoto = findViewById(R.id.iv_photo);
        etName = findViewById(R.id.et_name);
        etRelationship = findViewById(R.id.et_relationship);
        etPhone = findViewById(R.id.et_phone);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        databaseHelper = new DatabaseHelper(this);

        ivPhoto.setOnClickListener(v -> openImagePicker());
        btnSave.setOnClickListener(v -> savePerson());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            ivPhoto.setImageURI(selectedImageUri);
            // Copy image to app's private storage to make it permanent
            savedImagePath = saveImageToInternalStorage(selectedImageUri);
        }
    }

    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            // Create a unique file name
            String fileName = "person_" + System.currentTimeMillis() + ".jpg";
            File destinationFile = new File(getFilesDir(), fileName);

            // Copy the image from content URI to our file
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            OutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            return destinationFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    private void savePerson() {
        String name = etName.getText().toString().trim();
        String relationship = etRelationship.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }
        if (relationship.isEmpty()) {
            etRelationship.setError("Relationship is required");
            return;
        }
        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            return;
        }

        Person person = new Person();
        person.setName(name);
        person.setRelationship(relationship);
        person.setPhoneNumber(phone);
        person.setPhotoPath(savedImagePath);   // store the permanent file path
        person.setVoiceNotePath("");

        long id = databaseHelper.addPerson(person);
        if (id != -1) {
            person.setId((int) id);
            CloudSyncManager.getInstance(this).syncPeopleToCloud();
            Toast.makeText(this, "Person added successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to add person", Toast.LENGTH_SHORT).show();
        }
    }
}