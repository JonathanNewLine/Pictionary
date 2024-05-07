package com.example.pictionary;

import static androidx.constraintlayout.widget.ConstraintLayoutStates.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseMainActivity extends AppCompatActivity {
    protected FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore database = FirebaseFirestore.getInstance();
    private final CollectionReference usersTable = database.collection("users");
    protected static DatabaseUser cachedUser = null;
    protected Client client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        SoundEffects.init(this);

        findViewById(R.id.logout).setOnClickListener(v -> logoutUser());
        findViewById(R.id.user).setOnClickListener(v -> inflateLoginDialog(this));

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        usersTable.document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            DatabaseUser databaseUser = documentSnapshot.toObject(DatabaseUser.class);
            assert databaseUser != null;
            onUserLoggedIn(databaseUser);
        });
    }

    public void inflateLoginDialog(Activity activity) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.login, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(true);

        EditText editTextEmail = dialogView.findViewById(R.id.email_login);
        EditText editTextPassword = dialogView.findViewById(R.id.password_login);
        TextView registerLink = dialogView.findViewById(R.id.register_link);
        Button buttonLogin = dialogView.findViewById(R.id.login_button);

        Dialog dialog = dialogBuilder.create();
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        dialog.show();

        registerLink.setOnClickListener(v -> {
            dialog.dismiss();
            inflateRegisterDialog(activity);
        });
        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString();
            String password = editTextPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                alert("All fields must be filled").show();
                return;
            }

            login(email, password, new LoginCallback() {
                @Override
                public void onLoginSuccess(DatabaseUser user) {
                    dialog.dismiss();
                }

                @Override
                public void onLoginFailure(String errorMessage) {
                    alert(errorMessage).show();
                }
            });
        });
    }

    public void login(String email, String password, LoginCallback callBack) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener((Activity) this, (OnSuccessListener<? super AuthResult>) task -> {
                Log.d(TAG, "signInWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();

                assert user != null;
                usersTable.document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                    DatabaseUser databaseUser = documentSnapshot.toObject(DatabaseUser.class);
                    assert databaseUser != null;
                    onUserLoggedIn(databaseUser);
                    callBack.onLoginSuccess(databaseUser);
                });
            }).addOnFailureListener(e ->
                        callBack.onLoginFailure("Login up failed: " + e.getMessage()));
    }

    @SuppressLint("SetTextI18n")
    public void onUserLoggedIn(DatabaseUser databaseUser) {
        client = Client.getInstance();
        cachedUser = databaseUser;
        findViewById(R.id.logout).setVisibility(View.VISIBLE);
        TextView loggedAs = findViewById(R.id.logged_as_main);
        loggedAs.setText("Logged in as:\n" + databaseUser.getUsername());
        Client.getInstance().connectSocket(databaseUser.getUsername());
    }

    public void inflateRegisterDialog(Activity activity) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.sign_up, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(true);

        EditText editTextUsername = dialogView.findViewById(R.id.username_register);
        EditText editTextPassword = dialogView.findViewById(R.id.password_register);
        EditText editTextEmail = dialogView.findViewById(R.id.email_register);
        Button buttonRegister = dialogView.findViewById(R.id.register_button);

        Dialog dialog = dialogBuilder.create();
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        dialog.show();

        buttonRegister.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString();
            String password = editTextPassword.getText().toString();
            String username = editTextUsername.getText().toString();

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                alert("All fields must be filled").show();
                return;
            }

            register(email, password, username, new RegisterCallback() {
                @Override
                public void onRegisterSuccess(String email, String password) {
                    login(email, password, new LoginCallback() {
                        @Override
                        public void onLoginSuccess(DatabaseUser user) {

                        }

                        @Override
                        public void onLoginFailure(String errorMessage) {

                        }
                    });
                    dialog.dismiss();
                }

                @Override
                public void onRegisterFailure(String errorMessage) {
                    alert(errorMessage).show();
                }
            });

        });
    }

    public void register(String email, String password, String username, RegisterCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener((Activity) this, (OnSuccessListener<? super AuthResult>) task -> {
                Log.d(TAG, "createUserWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();

                assert user != null;
                usersTable.document(user.getUid()).set(new DatabaseUser(email, username));
                callback.onRegisterSuccess(email, password);
            }).addOnFailureListener(this, e ->
                        callback.onRegisterFailure("Sign up failed: " + e.getMessage()));
    }

    public void goToSettingsActivity(Activity activity) {
        Intent intent = new Intent(activity, Settings.class);
        activity.startActivity(intent);
    }

    public void logoutUser() {
        Client.getInstance().closeSocket();
        client = null;
        cachedUser = null;
        mAuth.signOut();
        updateToLogoutInterface();

        onUserLogout();
    }

    public void updateToLogoutInterface() {
        TextView loggedAs = findViewById(R.id.logged_as_main);
        loggedAs.setText("Logged in as\nguest");
        findViewById(R.id.logout).setVisibility(View.GONE);
    }

    public void onUserLogout() {

    }

    public void goToStatisticsActivity(Activity activity) {
        Intent intent = new Intent(activity, Statistics.class);
        activity.startActivity(intent);
    }

    public AlertDialog.Builder alert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", null);
        return builder;
    }

    interface LoginCallback {
        void onLoginSuccess(DatabaseUser user);
        void onLoginFailure(String errorMessage);
    }

    interface RegisterCallback {
        void onRegisterSuccess(String email, String password);
        void onRegisterFailure(String errorMessage);
    }
}
