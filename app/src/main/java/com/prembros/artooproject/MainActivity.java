package com.prembros.artooproject;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.prembros.artooproject.databinding.ActivityMainBinding;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.INTERNET;

public class MainActivity extends AppCompatActivity {

    public static final String USER_TYPE = "userType";
    public static final int CUSTOMER = 1;
    public static final int DELIVERY_AGENT = 2;
    private static final int REQUEST_CODE = 100;
    private ActivityMainBinding binding;

    @Override
    protected void onResume() {
        super.onResume();
        checkCurrentLoggedInUser();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!checkPermission()) {
                    requestPermission();
                }
            }
        }, 600);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        checkCurrentLoggedInUser();
    }

    @SuppressWarnings("ConstantConditions")
    private void checkCurrentLoggedInUser() {
        try {
            if (FirebaseAuth.getInstance().getCurrentUser().getEmail().contains("cust")) {
                binding.deliveryAgentBtn.setVisibility(View.GONE);
                binding.customerBtn.setVisibility(View.VISIBLE);
            } else if (FirebaseAuth.getInstance().getCurrentUser().getEmail().contains("agent")) {
                binding.deliveryAgentBtn.setVisibility(View.VISIBLE);
                binding.customerBtn.setVisibility(View.GONE);
            } else {
                binding.deliveryAgentBtn.setVisibility(View.VISIBLE);
                binding.customerBtn.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
//            e.printStackTrace();
            binding.deliveryAgentBtn.setVisibility(View.VISIBLE);
            binding.customerBtn.setVisibility(View.VISIBLE);
        }
    }

    public void onDeliveryAgentButtonClick(View view) {
        startActivity(DELIVERY_AGENT);
    }
    public void onCustomerButtonClick(View view) {
        startActivity(CUSTOMER);
    }

    private void startActivity(int userCode) {
        Intent intent = new Intent(this, LoginActivity.class);
        switch (userCode) {
            case CUSTOMER:
                intent.putExtra(USER_TYPE, CUSTOMER);
                break;
            case DELIVERY_AGENT:
                intent.putExtra(USER_TYPE, DELIVERY_AGENT);
                break;
            default:
                break;
        }
        startActivity(intent);
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkPermission() {
        int fineLocationResult = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int coarseLocationResult = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_COARSE_LOCATION);
        int internetResult = ContextCompat.checkSelfPermission(getApplicationContext(), INTERNET);

        return fineLocationResult == PackageManager.PERMISSION_GRANTED &&
                coarseLocationResult == PackageManager.PERMISSION_GRANTED &&
                internetResult == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, INTERNET}, REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean fineLocationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean coarseLocationAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean internetAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    if (fineLocationAccepted && coarseLocationAccepted && internetAccepted)
                        Snackbar.make(getWindow().getDecorView(),
                                R.string.permission_granted,
                                Snackbar.LENGTH_LONG).show();
                    else {
                        Snackbar.make(getWindow().getDecorView(),
                                R.string.permission_denied,
                                Snackbar.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                                showMessageOKCancel(
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(
                                                            new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, INTERNET},
                                                            REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }

                    }
                }


                break;
        }
    }


    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage("please allow the permissions to enable us to know your location.")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}
