package com.prembros.artooproject;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.prembros.artooproject.customer.CustomerMapActivity;
import com.prembros.artooproject.databinding.ActivityLoginBinding;
import com.prembros.artooproject.delivery_agent.DeliveryAgentMapActivity;

import static com.prembros.artooproject.MainActivity.CUSTOMER;
import static com.prembros.artooproject.MainActivity.DELIVERY_AGENT;
import static com.prembros.artooproject.MainActivity.USER_TYPE;

public class LoginActivity extends AppCompatActivity {

    public static boolean EXIT_FLAG;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthStateListener;
    private ActivityLoginBinding binding;
    private int USER_CODE;

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(firebaseAuthStateListener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EXIT_FLAG = false;
        USER_CODE = getIntent().getIntExtra(USER_TYPE, CUSTOMER);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            switch (USER_CODE) {
                case CUSTOMER:
                    actionBar.setTitle("Customer Login");
                    break;
                case DELIVERY_AGENT:
                    actionBar.setTitle("Delivery Agent Login");
                    break;
                default:
                    break;
            }
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        setFirebaseAuthStateListener();
    }

    private void setFirebaseAuthStateListener() {
        firebaseAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    switch (USER_CODE) {
                        case CUSTOMER:
                            if (!EXIT_FLAG)
                                startActivity(new Intent(LoginActivity.this, CustomerMapActivity.class));
                            else finish();
                            break;
                        case DELIVERY_AGENT:
                            if (!EXIT_FLAG)
                                startActivity(new Intent(LoginActivity.this, DeliveryAgentMapActivity.class));
                            else finish();
                            break;
                        default:
                            break;
                    }
                }
            }
        };
    }

    public void onLoginButtonClick(View view) {
        firebaseAuth.signInWithEmailAndPassword(binding.email.getText().toString(), binding.password.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login failed!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void onRegisterButtonClick(View view) {
        View view1 = getLayoutInflater().inflate(R.layout.confirm_password, null);
        final TextInputEditText confirmPasswordEditText = view1.findViewById(R.id.confirm_password);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        confirmPasswordEditText.requestFocus();
        imm.showSoftInput(confirmPasswordEditText, InputMethodManager.SHOW_IMPLICIT);
        builder.setView(view1)
                .setPositiveButton("Register", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onRegisterConfirmed(confirmPasswordEditText.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    public void onRegisterConfirmed(String confirmedPassword) {
        String password = binding.password.getText().toString();
        if (!password.isEmpty() && !confirmedPassword.isEmpty() && confirmedPassword.equals(password)) {
            firebaseAuth.createUserWithEmailAndPassword(binding.email.getText().toString(), password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                String message = "";
                                try {
                                    //noinspection ThrowableResultOfMethodCallIgnored,ConstantConditions
                                    message = task.getException().getMessage();
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }
                                Toast.makeText(LoginActivity.this, "Sign up error!\n" + message, Toast.LENGTH_SHORT).show();
                            } else {
                                if (firebaseAuth.getCurrentUser() != null) {
                                    DatabaseReference currentUserDb;
                                    if (USER_CODE == CUSTOMER) {
                                        currentUserDb = FirebaseDatabase.getInstance()
                                                .getReference()
                                                .child("Users")
                                                .child("Customers")
                                                .child(firebaseAuth.getCurrentUser().getUid());
                                    } else {
                                        currentUserDb = FirebaseDatabase.getInstance()
                                                .getReference()
                                                .child("Users")
                                                .child("Delivery Agents")
                                                .child(firebaseAuth.getCurrentUser().getUid());
                                    }
                                    currentUserDb.setValue(true);
                                }
                            }
                        }
                    });
        } else
            Toast.makeText(this, R.string.password_mismatch, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(firebaseAuthStateListener);
    }
}