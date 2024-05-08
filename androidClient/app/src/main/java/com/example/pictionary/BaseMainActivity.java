package com.example.pictionary;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseMainActivity extends AppCompatActivity {
    // alert constants
    public static final String HAVE_TO_CONNECT_ALERT = "You have to connect as a user first";
    public static final String EMPTY_ID_INPUT = "Id field is empty";
    public static final String ID_DOES_NOT_EXIST = "Damn bro, real good id, shame it doesn't fucking exist";

    // textViews
    private TextView loggedAs;

    // buttons
    private ImageView logOutBtn;
    private ImageView logInBtn;
    private ImageView exitCurrentScreen;
    private ImageView goToStatisticsPage;
    private ImageView goToSettingsPage;

    // controllers
    protected DatabaseController databaseController;
    protected ClientController clientController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseController = new DatabaseController();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setButtonListeners();
        SoundEffects.init(this);

        databaseController.logInWithCachedUser(new DatabaseController.LoginCallback() {
            @Override
            public void onLoginSuccess(DatabaseUser user) {
                onUserLoggedIn(user);
            }

            @Override
            public void onLoginFailure(String errorMessage) {
            }
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

        registerLink.setOnClickListener(v -> onRegisterLinkClick(dialog, activity));
        buttonLogin.setOnClickListener(v -> onLoginButtonClick(editTextEmail, editTextPassword, dialog));
    }

    public void onUserLoggedIn(DatabaseUser databaseUser) {
        clientController = ClientController.getInstance();
        clientController.updateUserLoggedIn(databaseUser.getUsername());
        updateToLoginInterface(databaseUser.getUsername());
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

        buttonRegister.setOnClickListener(v -> onRegisterButtonClick(editTextEmail, editTextPassword, editTextUsername, dialog));
    }

    public void updateToLogoutInterface() {
        loggedAs.setText("Logged in as\nguest");
        logOutBtn.setVisibility(View.GONE);
    }

    @SuppressLint("SetTextI18n")
    public void updateToLoginInterface(String username) {
        loggedAs.setText("Logged in as:\n" + username);
        logOutBtn.setVisibility(View.VISIBLE);
    }

    public void onUserLogout() {
        clientController.updateUserLoggedOut();
        clientController = null;
        updateToLogoutInterface();
    }

    public void goToStatisticsActivity(Activity activity) {
        Intent intent = new Intent(activity, Statistics.class);
        activity.startActivity(intent);
    }

    public void goToSettingsActivity(Activity activity) {
        Intent intent = new Intent(activity, Settings.class);
        activity.startActivity(intent);
    }

    public AlertDialog.Builder alert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", null);
        return builder;
    }

    private void setButtonListeners() {
        logOutBtn = findViewById(R.id.logout);
        logInBtn = findViewById(R.id.user);
        loggedAs = findViewById(R.id.logged_as_main);
        goToStatisticsPage = findViewById(R.id.statistics);
        goToSettingsPage = findViewById(R.id.settings);

        logOutBtn.setOnClickListener(v -> databaseController.logoutUser(this::onUserLogout));
        logInBtn.setOnClickListener(v -> inflateLoginDialog(this));
        goToStatisticsPage.setOnClickListener(v -> goToStatisticsActivity(this));
        goToSettingsPage.setOnClickListener(v -> goToSettingsActivity(this));

        if (!(this instanceof MainMenu)) {
            exitCurrentScreen = findViewById(R.id.exit);
            exitCurrentScreen.setOnClickListener(v -> exitToMainMenu());
        }

    }

    private void onRegisterLinkClick(Dialog dialog, Activity activity) {
        dialog.dismiss();
        inflateRegisterDialog(activity);
    }

    private void onLoginButtonClick(EditText editTextEmail, EditText editTextPassword, Dialog dialog) {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            alert("All fields must be filled").show();
            return;
        }

        databaseController.logIn(email, password, this, new DatabaseController.LoginCallback() {
            @Override
            public void onLoginSuccess(DatabaseUser user) {
                dialog.dismiss();
                onUserLoggedIn(user);
            }

            @Override
            public void onLoginFailure(String errorMessage) {
                alert(errorMessage).show();
            }
        });
    }

    private void onRegisterButtonClick(EditText editTextEmail, EditText editTextPassword, EditText editTextUsername, Dialog dialog) {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();
        String username = editTextUsername.getText().toString();

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            alert("All fields must be filled").show();
            return;
        }
        databaseController.register(email, password, username, this, new DatabaseController.RegisterCallback() {
            @Override
            public void onRegisterSuccess(String email, String password) {
                databaseController.logIn(email, password, BaseMainActivity.this, new DatabaseController.LoginCallback() {
                    @Override
                    public void onLoginSuccess(DatabaseUser user) {
                        onUserLoggedIn(user);
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
    }

    public void exitToMainMenu() {
        finish();
    }
}
