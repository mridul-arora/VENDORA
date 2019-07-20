package com.example.Vendora;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.cab.R;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback ,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {


    private String requestService;
    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    private Button CustomerLogoutButton;
    private Button SettingsButton;
    GeoQuery geoQuery;
    private FirebaseAuth mAuth;
    private FirebaseUser CurrentUser;
    private Button CallVendorButton;
    private String customerid;
    private DatabaseReference CustomerDatabaseRef;
    private FusedLocationProviderClient mFusedLocationClient;


    private TextView txtName, txtPhone , txtCartNumber;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;


    private DatabaseReference VendorAvailableRef;
    private int radius =1;
    private boolean VendorFound= false, requestType= false;
    private  String VendorFoundId;

    private  DatabaseReference VendorsRef;
    private  DatabaseReference VendorLocationRef;

    Marker  VendorMarker , Pickupmarker;
    private ValueEventListener VendorLocationRefListner;
    private LatLng CustomerLocation;


    private RadioGroup mRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        CustomerLogoutButton = (Button) findViewById(R.id.customer_logout_btn);
        SettingsButton= (Button)findViewById(R.id.customer_settings_btn);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        CallVendorButton = (Button) findViewById(R.id.customer_call_vendor_btn);
        mAuth = FirebaseAuth.getInstance();
        CurrentUser = mAuth.getCurrentUser();
        customerid = FirebaseAuth.getInstance().getCurrentUser().getUid();
       CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customers Requests");
       VendorAvailableRef = FirebaseDatabase.getInstance().getReference().child("Vendors Available");
       VendorLocationRef = FirebaseDatabase.getInstance().getReference().child("Vendors Working");

        txtName = findViewById(R.id.name_vendor);
        txtPhone = findViewById(R.id.phone_vendor);
        txtCartNumber = findViewById(R.id.cart_number_vendor);
        profilePic = findViewById(R.id.profile_image_vendor);
        relativeLayout = findViewById(R.id.rell);



        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.Vegetable_Vendor)  ;

        // Obtain the SupportMapFnragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        SettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomersMapActivity.this , SettingsActivity.class);
                intent.putExtra("type","Customers");
                startActivity(intent);

            }
        });


        CustomerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
            mAuth.signOut();;
            LogoutCustomer();
            }
        });

        CallVendorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {   if (requestType)
            {
                requestType = false;
                geoQuery.removeAllListeners();


                VendorLocationRef.removeEventListener(VendorLocationRefListner);
                if (VendorFoundId != null)
                {
                    VendorsRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Vendors").child(VendorFoundId).child("CustomerItemId");

                    VendorsRef.removeValue();
                    VendorFoundId = null;

                }
                VendorFound = false;
                radius =1;
                String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                geoFire.removeLocation(customerId);

                if(Pickupmarker != null)
                {
                    Pickupmarker.remove();
                }

                if(VendorMarker != null)
                {
                    VendorMarker.remove();
                }

              CallVendorButton.setText("Call a Vendor");

                relativeLayout.setVisibility(View.GONE);
            }
            else
            {

                int  selectId = mRadioGroup.getCheckedRadioButtonId();

                final RadioButton radioButton = (RadioButton) findViewById(selectId);

                if(radioButton.getText()==null){
                    return;
                }

                requestService = radioButton.getText().toString();
                requestType = true;
                String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                geoFire.setLocation(customerId, new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));
                CustomerLocation  = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                Pickupmarker = mMap.addMarker(new MarkerOptions().position(CustomerLocation).title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user )));



                // show customers

                CallVendorButton.setText("Getting a vendor who has your goods....");
                GetClosestVendor();
            }


            }
        });
    }
// whenever the vendor is available this method is  available
    private void GetClosestVendor()
    {
      GeoFire geoFire = new GeoFire(VendorAvailableRef);
      geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerLocation.latitude,CustomerLocation.longitude),radius);
      geoQuery.removeAllListeners();
      geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
          @Override
          public void onKeyEntered(String key, GeoLocation location)
          {
            if(!VendorFound && requestType)
            {   DatabaseReference mCustomerDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Vendors").child(key);
                mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if(dataSnapshot.exists()&& dataSnapshot.getChildrenCount()>0){
                            Map<String,Object> vendorMap = (Map<String, Object>) dataSnapshot.getValue();
                            if(VendorFound){
                                return;
                            }


                            if(vendorMap.get("service").equals(requestService))
                            {
                                VendorFound= true;
                                VendorFoundId= dataSnapshot.getKey();

                                VendorsRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Vendors").child(VendorFoundId);
                                HashMap VendorMap = new HashMap();
                                VendorMap.put("CustomerItemId", customerid);
                                VendorsRef.updateChildren(VendorMap);

                                GettingVendorLocation();
                                CallVendorButton.setText("Looking for vendors location");
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });





            }
          }

          @Override
          public void onKeyExited(String key) {

          }

          @Override
          public void onKeyMoved(String key, GeoLocation location) {

          }

          @Override
          public void onGeoQueryReady()
          {
            if(!VendorFound)
            {
              radius = radius+1;
              GetClosestVendor();

              if(radius == 100)
              {
                  Toast.makeText(CustomersMapActivity.this, "No vendor is in your location Please request other time", Toast.LENGTH_SHORT).show();

              }
            }
          }

          @Override
          public void onGeoQueryError(DatabaseError error) {

          }
      });
    }

    private void GettingVendorLocation()
    {
        VendorLocationRefListner = VendorLocationRef.child(VendorFoundId).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
               if(dataSnapshot.exists() && requestType)
               {
                   List<Object>  VendorLocationMap = (List<Object>) dataSnapshot.getValue();
                   double LocationLat =0;
                   double   LocationLng =0;
                   CallVendorButton.setText("Vendor Found");

                   relativeLayout.setVisibility(View.VISIBLE);
                   getAssignedVendorInformation();

                   if(VendorLocationMap.get(0) != null) {
                       LocationLat= Double.parseDouble(VendorLocationMap.get(0).toString());
                   }

                   if(VendorLocationMap.get(1) != null) {
                       LocationLng= Double.parseDouble(VendorLocationMap.get(1).toString());
                   }
                   LatLng VendorLatLng = new LatLng(LocationLat, LocationLng);
                   if(VendorMarker != null)
                   {
                        VendorMarker.remove();
                   }
                    // getting latitute longitute of vendor
                   Location location2 = new Location("");
                   location2.setLatitude(VendorLatLng.latitude);
                   location2.setLongitude(VendorLatLng.longitude);


                   Location location1 = new Location("");
                   location1.setLatitude(CustomerLocation.latitude);
                   location1.setLongitude(CustomerLocation.longitude);

                   float Distance = location1.distanceTo(location2);
                   if (Distance < 90) {
                       CallVendorButton.setText("Vendor has reached");
                   }
                   else {
                       CallVendorButton.setText("Vendor Found"+ String.valueOf(Distance));
                   }
                   VendorMarker =  mMap.addMarker(new MarkerOptions().position(VendorLatLng).title("Your Vendor is here now ").icon(BitmapDescriptorFactory.fromResource(R.drawable.tro)));
               }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });

    }



/*
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // now let set user location enable
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }
*/


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            }else{
                checkLocationPermission();
            }
        }

        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!=null){
                    lastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                    if(!getDriversAroundStarted)
                        getDriversAround();
                }
            }
        }
    };








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
    public void onLocationChanged(Location location)
    {

        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));



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
    protected void onStop()
    {
        super.onStop();

    }

    private void LogoutCustomer()
    {
        Intent welcomeIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    private void getAssignedVendorInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Vendors").child(VendorFoundId);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    String cart = dataSnapshot.child("cart").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);
                    txtCartNumber.setText(cart);

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

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomersMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(CustomersMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1:{
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else{
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }




    boolean getDriversAroundStarted = false;
    List<Marker> markers = new ArrayList<Marker>();
    private void getDriversAround(){
        getDriversAroundStarted = true;
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(lastLocation.getLongitude(), lastLocation.getLatitude()), 999999999);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key))
                        return;
                }

                LatLng driverLocation = new LatLng(location.latitude, location.longitude);

                Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title(key).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round)));
                mDriverMarker.setTag(key);

                markers.add(mDriverMarker);


            }

            @Override
            public void onKeyExited(String key) {
                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key)){
                        markerIt.remove();
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key)){
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
}


