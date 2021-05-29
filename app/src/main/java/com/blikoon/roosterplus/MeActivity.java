package com.blikoon.roosterplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.blikoon.roosterplus.xmpp.RoosterConnection;
import com.blikoon.roosterplus.xmpp.RoosterConnectionService;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

public class MeActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOGTAG = "MeActivity" ;
    private BroadcastReceiver mBroadcastReceiver;
    private TextView connectionStatusTextView;
    private ImageView profileImageView;
    private static final int SELECT_PHOTO = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_me);

        String status;
        RoosterConnection connection = RoosterConnectionService.getConnection();
        connectionStatusTextView = (TextView) findViewById(R.id.connection_status);

        if(  connection != null)
        {
            status = connection.getConnectionStateString();
            connectionStatusTextView.setText(status);
        }

        profileImageView = (ImageView) findViewById(R.id.profile_image);
        profileImageView.setOnClickListener(this);

        String selfJid = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString("xmpp_jid",null);

        RoosterConnection rc = RoosterConnectionService.getConnection();

        profileImageView.setImageResource(R.drawable.ic_profile);
        if(rc != null)
        {
            String imageAbsPath = rc.getProfileImageAbsolutePath(selfJid);
            if ( imageAbsPath != null)
            {
                Drawable d = Drawable.createFromPath(imageAbsPath);
                profileImageView.setImageDrawable(d);
            }
        }
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
                    case Constants.BroadCastMessages.UI_CONNECTION_STATUS_CHANGE_FLAG:

                        String status = intent.getStringExtra(Constants.UI_CONNECTION_STATUS_CHANGE);
                        connectionStatusTextView.setText(status);
                        break;
                }



            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BroadCastMessages.UI_CONNECTION_STATUS_CHANGE_FLAG);
        this.registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onClick(View v) {

        //When user clicks on the profile image
        Log.d(LOGTAG,"Clicked on the profile picture");
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Log.d(LOGTAG,"Result is OK");
                    Uri selectedImage = data.getData();

                    Bitmap bm = null;

                    try {
                        bm = decodeUri(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    if( bm != null)
                    {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bm.compress(Bitmap.CompressFormat.PNG, 0, stream);
                        byte[] byteArray = stream.toByteArray();
                        Log.d(LOGTAG,"Bitmap not NULL, proceeding with setting image. The array size is :" +byteArray.length);
                        RoosterConnection rc = RoosterConnectionService.getConnection();
                        if ( rc != null) {
                            if (rc.setSelfAvatar(byteArray)) {
                                Log.d(LOGTAG, "Avatar set correctly");
                                //Set the avatar to be shown in the profile Image View
                                Drawable image = new BitmapDrawable(getResources(),
                                        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
                                profileImageView.setImageDrawable(image);
                            } else
                            {
                                Log.d(LOGTAG,"Could not set user avatar");
                            }

                        }

                    }
                }else
                {
                    Log.d(LOGTAG,"Canceled out the Image selection act");
                }
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 140;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }


}
