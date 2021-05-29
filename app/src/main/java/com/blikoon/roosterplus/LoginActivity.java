package com.blikoon.roosterplus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.blikoon.roosterplus.xmpp.RoosterConnectionService;

import java.util.ArrayList;
import java.util.List;



/**
 * A login screen that offers login via Jid/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    private static final String LOGTAG = "RoosterPlus";

    // UI references.
    private AutoCompleteTextView mJidView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private BroadcastReceiver mBroadcastReceiver;//Allows us to know when to show the ChatListActivity


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mJidView = (AutoCompleteTextView) findViewById(R.id.jid);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mJidSignInButton = (Button) findViewById(R.id.jid_sign_in_button);
        mJidSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action)
                {
                    case Constants.BroadCastMessages.UI_AUTHENTICATED:
                        Log.d(LOGTAG,"Got a broadcast to show the main app window");
                        //Show the main app window
                        showProgress(false);
                        Intent i = new Intent(getApplicationContext(),ChatListActivity.class);
                        startActivity(i);
                        finish();
                        break;

                    case Constants.BroadCastMessages.UI_CONNECTION_ERROR:
                        Log.d(LOGTAG,"Got Connection Error in login activity");
                        showProgress(false);
                        mJidView.setError("Something went wrong while connecting. Make sure the credentials are valid and try again. ");
                        break;
                }

            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BroadCastMessages.UI_AUTHENTICATED);
        filter.addAction(Constants.BroadCastMessages.UI_CONNECTION_ERROR);

        this.registerReceiver(mBroadcastReceiver, filter);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid Jid, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mJidView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String jid = mJidView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid Jid address.
        if (TextUtils.isEmpty(jid)) {
            mJidView.setError(getString(R.string.error_field_required));
            focusView = mJidView;
            cancel = true;
        } else if (!isJidValid(jid)) {
            mJidView.setError(getString(R.string.error_invalid_jid));
            focusView = mJidView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            saveCredentialsAndLogin();

        }
    }

    private boolean isJidValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    private void saveCredentialsAndLogin()
    {
        Log.d(LOGTAG,"saveCredentialsAndLogin() called.");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putString("xmpp_jid", mJidView.getText().toString())
                .putString("xmpp_password", mPasswordView.getText().toString())
                .commit();

        //Start the service
        Intent i1 = new Intent(this, RoosterConnectionService.class);
        startService(i1);

    }



}





