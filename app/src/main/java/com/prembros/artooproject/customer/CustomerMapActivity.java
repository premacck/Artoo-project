package com.prembros.artooproject.customer;

import android.Manifest;
import android.animation.Animator;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prembros.artooproject.Directions;
import com.prembros.artooproject.R;

import java.util.HashMap;
import java.util.List;

import static android.view.ViewAnimationUtils.createCircularReveal;
import static com.prembros.artooproject.LoginActivity.EXIT_FLAG;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private LatLng deliveryLocation;
    private GeoQuery geoQuery;
    private int radius = 1;
    private boolean isAgentFound = false;
    private String foundAgentId;
    private Marker currentLocationMarker;
    private Marker agentMarker;
    private DatabaseReference agentLocationReference;
    private ValueEventListener agentLocationReferenceListener;
    private Button bottomButton;
    private Button cancelButton;
    private boolean locationUpdatedFlag;
    private Directions directions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        locationUpdatedFlag = true;

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map2);
        mapFragment.getMapAsync(this);

        cancelButton = (Button) this.findViewById(R.id.cancel_request_btn);
        bottomButton = (Button) this.findViewById(R.id.request_delivery_btn);

//        binding = DataBindingUtil.setContentView(this, R.layout.activity_customer_map);
    }

    private void rippleAnimation(final View mRevealView){
        mRevealView.setBackgroundResource(R.color.colorPrimaryComplementaryCyan);
        int centerX = mRevealView.getWidth() / 2;
        int centerY = mRevealView.getHeight() / 2;
        int startRadius = 0;
        int endRadius = (int) (Math.hypot(mRevealView.getWidth() * 2, mRevealView.getHeight() * 2));
        Animator animator = createCircularReveal(mRevealView, centerX, centerY, startRadius, endRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(1500);
        animator.start();
        mRevealView.setVisibility(View.VISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mRevealView.startAnimation(AnimationUtils.loadAnimation(CustomerMapActivity.this, android.R.anim.fade_out));
                mRevealView.setVisibility(View.INVISIBLE);
            }
        }, 600);
    }

    public void onRequestDone(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.cancelling_request)
                .setMessage(R.string.cancel_request_confirmation)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        rippleAnimation(CustomerMapActivity.this.findViewById(R.id.reveal_frame_layout));

                        if (directions != null)
                            directions.removeNavigation();
                        directions = null;
                        if (geoQuery != null)
                            geoQuery.removeAllListeners();
                        if (agentLocationReference != null)
                            agentLocationReference.removeEventListener(agentLocationReferenceListener);
                        cancelButton.setText(R.string.request_cancelled);
                        bottomButton.setText(R.string.request_delivery);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                cancelButton.setText(R.string.cancel_request);
                                cancelButton.setVisibility(View.INVISIBLE);
                            }
                        }, 1000);

                        /*removing data from database*/
                        if (foundAgentId != null) {
                            DatabaseReference agentReference = FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("Users")
                                    .child("Delivery Agents")
                                    .child(foundAgentId);
                            agentReference.setValue(true);
                            foundAgentId = null;
                        }
                        isAgentFound = false;
                        radius = 1;
                        if (agentMarker != null)
                            agentMarker.remove();
                        if (currentLocationMarker != null)
                            currentLocationMarker.remove();

                        FirebaseUser customer = FirebaseAuth.getInstance().getCurrentUser();
                        if (customer != null) {
                            String customerUid = customer.getUid();
                            GeoFire geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference().child("customerRequest"));
                            geoFire.removeLocation(customerUid);
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    public void onFoodRequested(View view) {
        cancelButton.setVisibility(View.VISIBLE);
        if (bottomButton.getText().toString().equals(String.valueOf(R.string.your_food_is_arriving))) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.confirm_delivery)
                    .setMessage("Click CONFIRM to confirm the delivery.")
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            onRequestDone(null);
                            Toast.makeText(CustomerMapActivity.this, "Enjoy your food!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
        } else {
            FirebaseUser customer = FirebaseAuth.getInstance().getCurrentUser();
            if (customer != null) {
                String customerUid = customer.getUid();
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("customerRequest");

                GeoFire geoFire = new GeoFire(reference);
                geoFire.setLocation(customerUid, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                deliveryLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                currentLocationMarker = mMap.addMarker(new MarkerOptions().position(deliveryLocation)
                        .title("You are here")
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pin_customer)));

                bottomButton.setText(R.string.requesting);
                getNearestAgent();
            }
        }
    }

    private void getNearestAgent() {
        DatabaseReference agentLocation = FirebaseDatabase.getInstance().getReference().child("agentsAvailable");
        GeoFire geoFire = new GeoFire(agentLocation);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(deliveryLocation.latitude, deliveryLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            /**called anytime a delivery agent is found within the specified radius*/
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!isAgentFound) {
                    isAgentFound = true;
                    foundAgentId = key;

                    DatabaseReference agentReference = FirebaseDatabase.getInstance()
                            .getReference()
                            .child("Users")
                            .child("Delivery Agents")
                            .child(foundAgentId);

                    /*Telling the delivery agent about the requesting customer*/
                    //noinspection ConstantConditions
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("requestingCustomerId", customerId);
                    agentReference.updateChildren(hashMap);

                    getAgentLocation();
                }
            }

            /**called when every agent within the specified radius is found*/
            @Override
            public void onGeoQueryReady() {
                if (!isAgentFound) {
                    radius++;
                    getNearestAgent();
                }
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("GeoQuery ERROR: ", error.getMessage());
                bottomButton.setText(R.string.request_delivery);
            }
        });
    }

    private void getAgentLocation() {
        agentLocationReference = FirebaseDatabase.getInstance()
                .getReference()
                .child("agentsWorking")
                .child(foundAgentId)
                .child("l");

        agentLocationReferenceListener = agentLocationReference.addValueEventListener(new ValueEventListener() {

            /**called whenever the location of the agent changes*/
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //noinspection unchecked
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLatitude = 0;
                    double locationLongitude = 0;

                    if (map != null) {
                        if (map.get(0) != null)
                            locationLatitude = Double.parseDouble(map.get(0).toString());
                        if (map.get(1) != null)
                            locationLongitude = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng agentLatLng = new LatLng(locationLatitude, locationLongitude);
                    if (agentMarker != null)
                        agentMarker.remove();

                    /*tell the customer where the requested delivery agent is*/
                    agentMarker = mMap.addMarker(new MarkerOptions().position(agentLatLng)
                            .title("Your delivery agent is here")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pin)));
//                    agentMarker.showInfoWindow();

                    Location location1 = new Location("");
                    location1.setLatitude(deliveryLocation.latitude);
                    location1.setLongitude(deliveryLocation.longitude);

                    Location location2 = new Location("");
                    location2.setLatitude(agentLatLng.latitude);
                    location2.setLongitude(agentLatLng.longitude);

                    float distance = location1.distanceTo(location2);

                    if (distance < 100) {
                        bottomButton.setText(R.string.your_food_is_arriving);
                    } else
                        bottomButton.setText("Agent is within " + distance + " meters");

                    adjustCamera(currentLocationMarker, agentMarker);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void adjustCamera(Marker currentMarker, Marker agentsMarker) {
        if (currentMarker != null && agentsMarker != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(currentMarker.getPosition());
            builder.include(agentsMarker.getPosition());
            LatLngBounds latLngBounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 400));
        } else if (currentMarker != null && locationUpdatedFlag) {
            locationUpdatedFlag = false;
            LatLng latLng = new LatLng(currentMarker.getPosition().latitude, currentMarker.getPosition().longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private void plotNavigation(LatLng origin, LatLng destination) {
        // Getting URL to the Google Directions API
        directions = new Directions(mMap);
        String url = directions.getDirectionsUrl(origin, destination);

        // Start downloading json data from Google Directions API
        directions.executeDownloadTask(url);
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        adjustCamera(currentLocationMarker, agentMarker);
        if (currentLocationMarker != null && agentMarker != null)
            plotNavigation(currentLocationMarker.getPosition(), agentMarker.getPosition());
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);                                      //1000 MILLISECONDS
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map_customer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                showLogoutDialog();
                return true;
            default:
                return false;
        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logging out")
                .setMessage("Are you sure?")
                .setPositiveButton("Log Out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FirebaseAuth.getInstance().signOut();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setNeutralButton("Just Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EXIT_FLAG = true;
                        finish();
                    }
                })
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        showLogoutDialog();
    }
}