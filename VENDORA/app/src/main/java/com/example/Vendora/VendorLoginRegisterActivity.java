package com.example.Vendora;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cab.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class VendorLoginRegisterActivity extends AppCompatActivity {
    private Button VendorLoginButton;
    private Button VendorRegisterButton;
    private TextView VendorRegisterLink;
    private TextView VendorStatus;
    private EditText EmailVendor;
    private EditText PasswordVendor;
    private ProgressDialog loadingBar;

    private FirebaseAuth mAuth;
    private DatabaseReference VendorDatabaseRef;
    private String OnlineVendorId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_login_register);
        VendorLoginButton = (Button) findViewById(R.id.vendor_login_btn);
        VendorRegisterButton = (Button) findViewById(R.id.vendor_register_btn);
        VendorRegisterLink = (TextView) findViewById(R.id.vendor_register_link);
        VendorStatus = (TextView) findViewById(R.id.vendor_status);
        EmailVendor = (EditText) findViewById(R.id.email_vendor);
        PasswordVendor = (EditText) findViewById(R.id.password_vendor);
        loadingBar= new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();




        VendorRegisterButton.setVisibility(View.INVISIBLE);
        VendorRegisterButton.setEnabled(false);

        VendorRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VendorLoginButton.setVisibility(View.INVISIBLE);
                VendorRegisterLink.setVisibility(View.INVISIBLE);
                VendorStatus.setText("Register Vendor");
                VendorRegisterButton.setVisibility(View.VISIBLE);
                VendorRegisterButton.setEnabled(true);
            }
        });

        VendorRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = EmailVendor.getText().toString();
                String password = PasswordVendor.getText().toString();

                RegisterVendor(email, password);
            }
        });

        VendorLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = EmailVendor.getText().toString();
                String password = PasswordVendor.getText().toString();

                SignInVendor(email, password);
            }
        });
    }

    private void SignInVendor(String email, String password) {
        if (TextUtils.isEmpty(email))
        {
            Toast.makeText(VendorLoginRegisterActivity.this, "Please write Email ", Toast.LENGTH_SHORT).show();
        }

        if (TextUtils.isEmpty(password))
        {
            Toast.makeText(VendorLoginRegisterActivity.this, "Please enter Password ", Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Vendor Login");
            loadingBar.setMessage("Please wait, while we are checking your credentials");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task)
                {
                    if(task.isSuccessful())
                    {

                        Intent VendorIntent = new Intent(VendorLoginRegisterActivity.this, VendorsMapsActivity.class);
                        startActivity(  VendorIntent);

                        Toast.makeText(VendorLoginRegisterActivity.this, "Vendor Logged in Successfully", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();

                    }

                    else
                    {
                        Toast.makeText(VendorLoginRegisterActivity.this, "Login Unsuccessfull, Please try again", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }

    }


    private void RegisterVendor(String email, String password)
    {
        if (TextUtils.isEmpty(email))
        {
            Toast.makeText(VendorLoginRegisterActivity.this, "Please write Email ", Toast.LENGTH_SHORT).show();
        }

        if (TextUtils.isEmpty(password))
        {
            Toast.makeText(VendorLoginRegisterActivity.this, "Please enter Password ", Toast.LENGTH_SHORT).show();
        }
        else
            { loadingBar.setTitle("Vendor Registration");
              loadingBar.setMessage("Please wait, while we register your data");
              loadingBar.show();

              mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                      if(task.isSuccessful())
                      {
                          OnlineVendorId= mAuth.getCurrentUser().getUid();
                          VendorDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Vendors").child(OnlineVendorId);

                          VendorDatabaseRef.setValue("true");
                          Intent vendorIntent = new Intent(VendorLoginRegisterActivity.this,VendorsMapsActivity.class);
                          startActivity(vendorIntent);

                          Toast.makeText(VendorLoginRegisterActivity.this, "Vendor Register Successfully", Toast.LENGTH_SHORT).show();
                          loadingBar.dismiss();

                          Intent VendorIntent = new Intent(VendorLoginRegisterActivity.this, VendorsMapsActivity.class);
                          startActivity(  VendorIntent);
                      }

                      else
                          {
                              Toast.makeText(VendorLoginRegisterActivity.this, "Vendor Register Unsuccessfully, Please try again", Toast.LENGTH_SHORT).show();
                              loadingBar.dismiss();
                          }
                       }
                });
            }
    }
}