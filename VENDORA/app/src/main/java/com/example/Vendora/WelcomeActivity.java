package com.example.Vendora;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cab.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

public class
WelcomeActivity extends AppCompatActivity {

    private Button  WelcomeVendorButton;
    private Button  WelcomeCustomerButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        WelcomeCustomerButton = (Button) findViewById(R.id.welcome_customer_btn);
        WelcomeVendorButton = (Button) findViewById(R.id.welcome_vendor_btn);

        Dexter.withActivity(WelcomeActivity.this).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if(report.areAllPermissionsGranted()) {
                    Toast.makeText(WelcomeActivity.this, "All permissions granted", Toast.LENGTH_SHORT).show();
                }
                if (!report.areAllPermissionsGranted()) {
                    Toast.makeText(WelcomeActivity.this, "Permissions compulsory", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();

            }
        }).check();

        WelcomeCustomerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent LoginRegisterCustomerIntent = new Intent(WelcomeActivity.this , CustomerLoginRegisterActivity.class);
                startActivity(LoginRegisterCustomerIntent);
            }
        });


        WelcomeVendorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent LoginRegisterVendorIntent = new Intent(WelcomeActivity.this , VendorLoginRegisterActivity.class);
                startActivity(LoginRegisterVendorIntent);
            }
        });




    }

}
