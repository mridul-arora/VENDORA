package com.example.Vendora;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.RoutingListener;
import com.example.cab.R;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class VendorsMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        RoutingListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private  String userID;

    private Button LogoutVendorButton;
    private Button SettingsVendorButton;
    private FirebaseAuth mAuth;
    private FirebaseUser CurrentUser;
    private Boolean currentLogOutVendorStatus = false;
    Marker PickUpMarker;

    private DatabaseReference AssignCustomerRef , AssignedCustomerPickupRef;
    private String VendorId, CustomerId="";
     private ValueEventListener AssignedCustomerPickupRefListner;

    private TextView txtName, txtPhone , txtCartNumber;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendors_maps);

        mAuth = FirebaseAuth.getInstance();
        CurrentUser = mAuth.getCurrentUser();
        VendorId = mAuth.getCurrentUser().getUid();
        LogoutVendorButton = (Button) findViewById(R.id.vendor_logout_btn);
        SettingsVendorButton = (Button) findViewById(R.id.vendor_settings_btn);

        txtName = findViewById(R.id.name_customer);
        txtPhone = findViewById(R.id.phone_customer);

        profilePic = findViewById(R.id.profile_image_customer);
        relativeLayout = findViewById(R.id.rel2);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        SettingsVendorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(VendorsMapsActivity.this , SettingsActivity.class);
                intent.putExtra("type","Vendors");
                startActivity(intent);

            }
        });

        LogoutVendorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLogOutVendorStatus = true;
                DisconnectTheVendor();
                mAuth.getInstance().signOut();
                LogoutVendor();
            }
        });
        GetAssignedCustomerRequest();
    }

    private void GetAssignedCustomerRequest()
    {
      AssignCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Vendors").child(VendorId).child("CustomerItemId");
     AssignCustomerRef.addValueEventListener(new ValueEventListener() {
         @Override
         public void onDataChange(@NonNull DataSnapshot dataSnapshot)
         {
             if(dataSnapshot.exists())
             {
                 CustomerId = dataSnapshot.getValue().toString();

                 GetAssignedCustomerLocation();

                  relativeLayout.setVisibility(View.VISIBLE);
                 getAssignedCustomerInformation();
             }

             else
             {
                 CustomerId = "";
                 if (PickUpMarker != null)
                 {
                     PickUpMarker.remove();
                 }

                 if(AssignedCustomerPickupRefListner != null)
                 {
                     AssignedCustomerPickupRef.removeEventListener(AssignedCustomerPickupRefListner);
                 }
                 relativeLayout.setVisibility(View.GONE);
             }
         }

         @Override
         public void onCancelled(@NonNull DatabaseError databaseError)
         {

         }
     }) ;


    }

    private void GetAssignedCustomerLocation()
    {
     AssignedCustomerPickupRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests").child(CustomerId).child("l");

        AssignedCustomerPickupRefListner = AssignedCustomerPickupRef.addValueEventListener(new ValueEventListener() {
         @Override
         public void onDataChange(@NonNull DataSnapshot dataSnapshot)
         {
           if(dataSnapshot.exists())
           {
               List<Object> CustomerLocationMap = (List<Object>) dataSnapshot.getValue();
               double LocationLat =0;
               double   LocationLng =0;


               if(CustomerLocationMap.get(0) != null)
               {
                   LocationLat= Double.parseDouble(CustomerLocationMap.get(0).toString());


               }

               if(CustomerLocationMap.get(1) != null)
               {
                   LocationLng= Double.parseDouble(CustomerLocationMap.get(1).toString());


               }
               LatLng VendorLatLng = new LatLng(LocationLat, LocationLng);
               PickUpMarker= mMap.addMarker(new MarkerOptions().position(VendorLatLng).title("Your Customer is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

           }
         }

         @Override
         public void onCancelled(@NonNull DatabaseError databaseError) {

         }
     });
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState)
    {
        super.onCreate(savedInstanceState, persistentState);


    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            //

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext() != null)
        {
            lastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));


            String userID = mAuth.getCurrentUser().getUid();
            Log.i("ok",userID);
            DatabaseReference VendorAvailibilityRef = FirebaseDatabase.getInstance().getReference().child("Vendors Available");
            GeoFire geoFireAvalibility = new GeoFire(VendorAvailibilityRef);
          DatabaseReference VendorWorkingRef = FirebaseDatabase.getInstance().getReference().child("Vendors Working");
            GeoFire geoFireWorking = new GeoFire(VendorWorkingRef);
           switch (CustomerId)
           {
               case "":{
                       geoFireWorking.removeLocation(userID);
                       geoFireAvalibility.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                       break;}
                       default: {
                           geoFireAvalibility.removeLocation(userID);
                           geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                          break;}
           }

        }


    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!currentLogOutVendorStatus) {
            DisconnectTheVendor();
        }


    }

    private void DisconnectTheVendor() {
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference VendorAvailibilityRef = FirebaseDatabase.getInstance().getReference().child("Vendors Available");

        GeoFire geoFire = new GeoFire(VendorAvailibilityRef);
        geoFire.removeLocation(userID);
    }


    private void LogoutVendor() {
        Intent welcomeIntent = new Intent(getApplicationContext(), WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();


    }


    private void getAssignedCustomerInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Customers").child(CustomerId);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();


                    txtName.setText(name);
                    txtPhone.setText(phone);


                    if (dataSnapshot.hasChild("image"))
                    {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profilePic);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRoutingFailure(RouteException e) {

    }

    @Override
    public void onRoutingStart()
    {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> arrayList, int i)
    {

    }

    @Override
    public void onRoutingCancelled()
    {

    }
}