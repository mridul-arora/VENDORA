package com.example.Vendora;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cab.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {
    private String getType;
    private CircleImageView profileImageView;
    private EditText nameEditText, phoneEditText, vendorCartNumber;
    private ImageView closeButton, saveButton;
    private TextView profileChangeBtn;
    private Uri imageUri;
    private String myUrl = "";
    private StorageTask uploadTask;
    private StorageReference storageProfilePicRef;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String checker = "";
    private String mService ;

    int i =0;
    private RadioGroup mRadioGroup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getType = getIntent().getStringExtra("type");
        Toast.makeText(this, getType, Toast.LENGTH_SHORT).show();

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);
        storageProfilePicRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");


        profileImageView = findViewById(R.id.profile_image);
        nameEditText = findViewById(R.id.name);
        phoneEditText = findViewById(R.id.phone_number);
        vendorCartNumber = findViewById(R.id.vendor_thela_number);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);


        if (getType.equals("Vendors")) {
            vendorCartNumber.setVisibility(View.VISIBLE);
        }
        saveButton = findViewById(R.id.save_button);
        closeButton = findViewById(R.id.close_button);
        profileChangeBtn = findViewById(R.id.change_picture_btn);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getType.equals("Vendors")) {
                    startActivity(new Intent(SettingsActivity.this, VendorsMapsActivity.class));
                } else {
                    startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
                }
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checker.equals("clicked")) {
                    validateControllers();
                } else {

                    validateAndSaveOnlyInformation();
                }
            }
        });
        profileChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker = "clicked";
                CropImage.activity().setAspectRatio(1, 1).start(SettingsActivity.this);

            }
        });
       System.out.println("the value of i is "+ i);

            getUserInformation();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();

            profileImageView.setImageURI(imageUri);
        } else {

            if (getType.equals("Vendors")) {
                startActivity(new Intent(SettingsActivity.this, VendorsMapsActivity.class));
            }
            else {
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
            }
            Toast.makeText(this, "Error Try Again", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateControllers() {
        if (TextUtils.isEmpty(nameEditText.getText().toString())) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(phoneEditText.getText().toString())) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
        } else if (getType.equals("Vendors") && TextUtils.isEmpty(phoneEditText.getText().toString())) {
            Toast.makeText(this, "Please provide your cart number", Toast.LENGTH_SHORT).show();
        } else if (checker.equals("clicked")) {
            uploadProfilePicture();
        }
    }

    private void uploadProfilePicture() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Settings Account Information");
        progressDialog.setMessage("Please  wait, while we are setting your account information");
        progressDialog.show();


        if (imageUri != null) {
            final StorageReference fileRef = storageProfilePicRef.child(mAuth.getCurrentUser().getUid() + ".jpg");
            uploadTask = fileRef.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();
                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", mAuth.getCurrentUser().getUid());
                        userMap.put("name", nameEditText.getText().toString());
                        userMap.put("phone", phoneEditText.getText().toString());
                        userMap.put("image", myUrl);

                        if (getType.equals("Vendors")) {
                            userMap.put("cart", vendorCartNumber.getText().toString());
                            int selectId = mRadioGroup.getCheckedRadioButtonId();

                            final RadioButton radioButton = (RadioButton) findViewById(selectId);

                            if (radioButton.getText() == null) {
                                return;
                            }
                            userMap.put("service", radioButton.getText().toString());

                        }

                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);
                        progressDialog.dismiss();

                        if (getType.equals("Vendors")) {
                            startActivity(new Intent(SettingsActivity.this, VendorsMapsActivity.class));
                        } else {
                            startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
                        }
                    }
                }
            });
        } else {
            Toast.makeText(this, "Image is not selected", Toast.LENGTH_SHORT).show();
        }


    }

    private void validateAndSaveOnlyInformation() {
        if (TextUtils.isEmpty(nameEditText.getText().toString()))
        {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(phoneEditText.getText().toString()))
        {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
        }
        else if (getType.equals("Vendors") && TextUtils.isEmpty(vendorCartNumber.getText().toString())) {
            Toast.makeText(this, "Please provide your cart number", Toast.LENGTH_SHORT).show();
        }
           else
        {




            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("uid", mAuth.getCurrentUser().getUid());
            userMap.put("name", nameEditText.getText().toString());
            userMap.put("phone", phoneEditText.getText().toString());


            if (getType.equals("Vendors")) {
                int  selectId = mRadioGroup.getCheckedRadioButtonId();

                final RadioButton radioButton = (RadioButton) findViewById(selectId);

                if(radioButton.getText()==null){
                    return;
                }

                mService = radioButton.getText().toString();

                userMap.put("cart", vendorCartNumber.getText().toString());
                userMap.put("service", mService);
            }

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);


            if (getType.equals("Vendors")) {
                startActivity(new Intent(SettingsActivity.this, VendorsMapsActivity.class));
            }
            else
            {
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
            }
        }

    }
private  void getUserInformation()
{    i= 1;
    databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                String name = dataSnapshot.child("name").getValue().toString();
                String phone = dataSnapshot.child("phone").getValue().toString();

                nameEditText.setText(name);
                phoneEditText.setText(phone);

                if (getType.equals("Vendors")) {
                    String cart = dataSnapshot.child("cart").getValue().toString();
                    vendorCartNumber.setText(cart);
                    mService = dataSnapshot.child("service").getValue().toString();

                    switch (mService)
                    {
                        case "Vegetables Vendor":
                            mRadioGroup.check(R.id.Vegetable_Vendor);
                            break;

                        case "Fruits Vendor":
                            mRadioGroup.check(R.id.Fruits_Vendor);
                            break;

                        case "Chaat Vendor":
                            mRadioGroup.check(R.id.Chaat_Vendor);
                            break;
                    }
                }
                if (dataSnapshot.hasChild("image")) {
                    String image = dataSnapshot.child("image").getValue().toString();
                    Picasso.get().load(image).into(profileImageView);
                }
            }
        }
        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    });
}

}



