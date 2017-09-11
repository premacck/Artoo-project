package com.prembros.artooproject.delivery_agent;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
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
import com.prembros.artooproject.Directions;
import com.prembros.artooproject.R;

import java.util.List;

import static com.prembros.artooproject.LoginActivity.EXIT_FLAG;

public class DeliveryAgentMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        ConnectionCallbacks, OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private String customerId = "";
    private DatabaseReference customerDeliveryLocationReference;
    private ValueEventListener valueEventListener;
    private Marker customerLocationMarker;
    private boolean isLoggingOut = false;
    private Location lastLocation;
    private Directions directions;
    private boolean locationUpdatedFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationUpdatedFlag = true;
        setContentView(R.layout.activity_delivery_agent_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getAssignedCustomer();
    }

    private void getAssignedCustomer() {
        //noinspection ConstantConditions
        String agentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerReference = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Delivery Agents")
                .child(agentId)
                .child("requestingCustomerId");
        assignedCustomerReference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerDeliveryLocation();
                }

                /*
                * called every time a child is removed from "requestingCustomerId" parent
                * trigger that tells us that customer request has been cancelled
                */
                else {
                    endTracking();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void endTracking() {
        customerId = "";
        if (customerLocationMarker != null)
            customerLocationMarker.remove();
        if (customerDeliveryLocationReference != null)
            customerDeliveryLocationReference.removeEventListener(valueEventListener);
        if (directions != null)
            directions.removeNavigation();
    }

    public void confirmDelivery(View view) {
        if (customerLocationMarker != null && lastLocation != null) {
            Location deliveryLocation = new Location("");
            deliveryLocation.setLatitude(customerLocationMarker.getPosition().latitude);
            deliveryLocation.setLongitude(customerLocationMarker.getPosition().longitude);

            if (lastLocation.distanceTo(deliveryLocation) <= 100) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.confirm_delivery)
                        .setMessage("Click CONFIRM to confirm the delivery.")
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                endTracking();
                                Toast.makeText(DeliveryAgentMapActivity.this, "Food Delivered!\nRequest the customer to confirm delivery too.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            } else
                Toast.makeText(this, "You are still too far away from your destination.\nWhy so lazy?", Toast.LENGTH_LONG).show();
        }
    }

    private void getAssignedCustomerDeliveryLocation() {
        customerDeliveryLocationReference = FirebaseDatabase.getInstance()
                .getReference()
                .child("customerRequest")
                .child(customerId)
                .child("l");
        valueEventListener = customerDeliveryLocationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")) {
                    //noinspection unchecked
                    List<Object> list = (List<Object>) dataSnapshot.getValue();
                    double locationLatitude = 0;
                    double locationLongitude = 0;
//                    button.setText("Agent found within " + radius + " Km");
//                    button.setClickable(false);

                    if (list != null) {
                        if (list.get(0) != null)
                            locationLatitude = Double.parseDouble(list.get(0).toString());
                        if (list.get(1) != null)
                            locationLongitude = Double.parseDouble(list.get(1).toString());
                    }
                    LatLng agentLatLng = new LatLng(locationLatitude, locationLongitude);

                    /*tell the delivery agent where the assigned customer is*/
                    customerLocationMarker = mMap.addMarker(new MarkerOptions().position(agentLatLng)
                            .title("Your customer is here")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pin_customer)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
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

        directions.removeNavigation();

        // Start downloading json data from Google Directions API
        directions.executeDownloadTask(url);
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (locationUpdatedFlag) {
            locationUpdatedFlag = false;
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
        }

        /*Updating database through GeoFire*/
        FirebaseUser deliveryAgent = FirebaseAuth.getInstance().getCurrentUser();
        if (deliveryAgent != null) {
            String agentId = deliveryAgent.getUid();

            DatabaseReference availableAgentReference = FirebaseDatabase.getInstance().getReference("agentsAvailable");
            GeoFire availableAgentGeoFire = new GeoFire(availableAgentReference);

            DatabaseReference workingAgentReference = FirebaseDatabase.getInstance().getReference("agentsWorking");
            GeoFire workingAgentGeoFire = new GeoFire(workingAgentReference);

            switch (customerId) {
                /*when agent is available*/
                case "":
                    workingAgentGeoFire.removeLocation(agentId);
                    availableAgentGeoFire.setLocation(agentId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                /*when agent is working*/
                default:
                    availableAgentGeoFire.removeLocation(agentId);
                    workingAgentGeoFire.setLocation(agentId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }

            if (customerLocationMarker != null)
                plotNavigation(latLng, customerLocationMarker.getPosition());
        }
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
        getMenuInflater().inflate(R.menu.menu_map_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                showLogoutDialog();
                return true;
            case R.id.action_logout:
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
                        isLoggingOut = true;
                        disconnectAgent();
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

    private void disconnectAgent() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        /*removing agentId from firebase console*/
        FirebaseUser deliveryAgent = FirebaseAuth.getInstance().getCurrentUser();
        if (deliveryAgent != null) {
            String agentId = deliveryAgent.getUid();
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("agentsAvailable");

            GeoFire geoFire = new GeoFire(reference);
            geoFire.removeLocation(agentId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLoggingOut) {
            disconnectAgent();
        }
    }

    @Override
    public void onBackPressed() {
        showLogoutDialog();
    }
}