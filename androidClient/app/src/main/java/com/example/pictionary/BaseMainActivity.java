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

/**
 * This abstract class represents the base main activity of the application.
 */
public abstract class BaseMainActivity extends AppCompatActivity {
    /** error messages */
    public static final String HAVE_TO_CONNECT_ALERT = "You have to connect as a user first";
    public static final String EMPTY_ID_INPUT = "Id field is empty";
    public static final String ID_DOES_NOT_EXIST = "Nonexistent id";
    public static final String ALREADY_IN_GAME = "Game is ongoing, wait for everyone to finish the game";

    /** textViews */
    // logged in as
    private TextView loggedAs;

    /** buttons */
    // log out button
    private ImageView logOutBtn;
    // log in button
    private ImageView logInBtn;
    // exit current screen button
    private ImageView exitCurrentScreen;
    // go to statistics page button
    private ImageView goToStatisticsPage;
    // go to settings page button
    private ImageView goToSettingsPage;

    /** controllers */
    // database
    protected static DatabaseController databaseController;
    // client controller
    protected static ClientController clientController;

    /** other */
    // flag to check if onStart is called already
    private boolean isOnStartCalled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseController = DatabaseController.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // if onStart is called already, return
        if (isOnStartCalled) {
            return;
        }
        // setup button and sound effects
        setButtonListeners();
        SoundEffects.init(this);

        // log in with cached user
        databaseController.logInWithCachedUser(new DatabaseController.LoginCallback() {
            @Override
            public void onLoginSuccess(DatabaseUser user) {
                onUserLoggedIn(user);
            }

            @Override
            public void onLoginFailure(String errorMessage) {
            }
        });

        isOnStartCalled = true;
    }

    /**
     * Inflates the login dialog.
     * @param activity The activity where the dialog is to be inflated.
     */
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

    /**
     * Handles the event when a user logs in.
     * @param databaseUser The user who logged in.
     */
    public void onUserLoggedIn(DatabaseUser databaseUser) {
        if (clientController != null) {
            clientController.updateUserLoggedOut();
        }
        clientController = ClientController.getInstance();
        clientController.updateUserLoggedIn(databaseUser.getUsername());
        updateToLoginInterface(databaseUser.getUsername());
    }

    /**
     * Inflates the register dialog.
     * @param activity The activity where the dialog is to be inflated.
     */
    public void inflateRegisterDialog(Activity activity) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.sign_up, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(true);

        EditText editTextUsername = dialogView.findViewById(R.id.username_register);
        EditText editTextPassword = dialogView.findViewById(R.id.password_register);
        EditText editTextEmail = dialogView.findViewById(R.id.email_register);
        EditText editTextConfirmPassword = dialogView.findViewById(R.id.confirm_password_register);
        Button buttonRegister = dialogView.findViewById(R.id.register_button);

        Dialog dialog = dialogBuilder.create();
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        dialog.show();

        buttonRegister.setOnClickListener(v -> onRegisterButtonClick(editTextEmail, editTextPassword, editTextUsername, editTextConfirmPassword, dialog));
    }

    /**
     * Updates the interface to the logout interface.
     */
    public void updateToLogoutInterface() {
        loggedAs.setText("Logged in as\nguest");
        logOutBtn.setVisibility(View.GONE);
    }

    /**
     * Updates the interface to the login interface.
     * @param username The username of the logged in user.
     */
    @SuppressLint("SetTextI18n")
    public void updateToLoginInterface(String username) {
        loggedAs.setText("Logged in as:\n" + username);
        logOutBtn.setVisibility(View.VISIBLE);
    }

    /**
     * Handles the event when a user logs out.
     */
    public void onUserLogout() {
        clientController.updateUserLoggedOut();
        clientController = null;
        updateToLogoutInterface();
    }

    /**
     * Navigates to the statistics activity.
     * @param activity The activity from where the navigation is to be done.
     */
    public void goToStatisticsActivity(Activity activity) {
        Intent intent = new Intent(activity, Statistics.class);
        activity.startActivity(intent);
    }

    /**
     * Navigates to the settings activity.
     * @param activity The activity from where the navigation is to be done.
     */
    public void goToSettingsActivity(Activity activity) {
        Intent intent = new Intent(activity, Settings.class);
        activity.startActivity(intent);
    }

    /**
     * Creates an alert with the given message.
     * @param message The message of the alert.
     * @return The alert builder.
     */
    public AlertDialog.Builder alert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", null);
        return builder;
    }

    /**
     * Sets the button listeners.
     */
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

        // if not main menu, set exit button back to the main menu
        if (!(this instanceof MainMenu)) {
            exitCurrentScreen = findViewById(R.id.exit);
            exitCurrentScreen.setOnClickListener(v -> exitToMainMenu());
        }

    }

    /**
     * Handles the event when the register link is clicked.
     * @param dialog The dialog where the register link is clicked.
     * @param activity The activity where the dialog is.
     */
    private void onRegisterLinkClick(Dialog dialog, Activity activity) {
        dialog.dismiss();
        inflateRegisterDialog(activity);
    }

    /**
     * Handles the event when the login button is clicked.
     * @param editTextEmail The EditText for the email.
     * @param editTextPassword The EditText for the password.
     * @param dialog The dialog where the login button is clicked.
     */
    private void onLoginButtonClick(EditText editTextEmail, EditText editTextPassword, Dialog dialog) {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            alert("All fields must be filled").show();
            return;
        }

        // log in
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

    /**
     * Handles the event when the register button is clicked.
     * @param editTextEmail The EditText for the email.
     * @param editTextPassword The EditText for the password.
     * @param editTextUsername The EditText for the username.
     * @param editTextConfirmPassword The EditText for the confirm password.
     * @param dialog The dialog where the register button is clicked.
     */
    private void onRegisterButtonClick(EditText editTextEmail, EditText editTextPassword, EditText editTextUsername, EditText editTextConfirmPassword, Dialog dialog) {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();
        String username = editTextUsername.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            alert("All fields must be filled").show();
            return;
        }

        // register
        databaseController.register(email, password, confirmPassword, username, this, new DatabaseController.RegisterCallback() {
            @Override
            public void onRegisterSuccess(String email, String password) {
                // log in
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

    /**
     * Exits to the main menu.
     */
    public void exitToMainMenu() {
        finish();
    }
}
