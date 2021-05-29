package com.blikoon.roosterplus;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.blikoon.roosterplus.adapters.ChatListAdapter;
import com.blikoon.roosterplus.model.Chat;
import com.blikoon.roosterplus.model.ChatModel;
import com.blikoon.roosterplus.xmpp.RoosterConnectionService;

public class ChatListActivity extends AppCompatActivity implements ChatListAdapter.OnItemClickListener, ChatListAdapter.OnItemLongClickListener {

    private static final String LOGTAG = "ChatListActivity";
    private RecyclerView chatsRecyclerView;
    private FloatingActionButton newConversationButton;
    protected static final int REQUEST_EXCEMPT_OP = 188;
    protected static final int ROOSTER_REQUEST_EXTERNAL_STORAGE = 11;
    ChatListAdapter mAdapter;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ROOSTER_REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permissions granted for Rooster+", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Without these permissions, you won't be able to send or receive files with Rooster+", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);


        boolean logged_in_state = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean("xmpp_logged_in", false);
        if (!logged_in_state) {
            Log.d(LOGTAG, "Logged in state :" + logged_in_state);
            Intent i = new Intent(ChatListActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        } else {
            if (!Utilities.isServiceRunning(RoosterConnectionService.class, getApplicationContext())) {
                Log.d(LOGTAG, "Service not running, starting it ...");
                //Start the service
                Intent i1 = new Intent(this, RoosterConnectionService.class);
                startService(i1);

            } else {
                Log.d(LOGTAG, "The service is already running.");
            }

        }


        chatsRecyclerView = (RecyclerView) findViewById(R.id.chatsRecyclerView);
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        mAdapter = new ChatListAdapter(getApplicationContext());
        mAdapter.setmOnItemClickListener(this);
        mAdapter.setOnItemLongClickListener(this);
        chatsRecyclerView.setAdapter(mAdapter);

        newConversationButton = (FloatingActionButton) findViewById(R.id.new_conversation_floating_button);
        newConversationButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        newConversationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(ChatListActivity.this, ContactListActivity.class);
                startActivity(i);

            }
        });

        askStoragePermissions();


    }

    private void askStoragePermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, ROOSTER_REQUEST_EXTERNAL_STORAGE);
            }
        }


    }

    private void requestBatteryOptimizationsFavor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
            builder.setTitle("Battery optimization request");
            builder.setMessage("Battery optimizations are needed to make the app work right");

            // Set up the buttons
            builder.setPositiveButton(R.string.ignore_bat_allow, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(LOGTAG, "User clicked on OK");

                    Intent intent = new Intent();
                    String packageName = getPackageName();

                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivityForResult(intent, REQUEST_EXCEMPT_OP);


                }
            });
            builder.setNegativeButton(R.string.add_contact_cancel_text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(LOGTAG, "User clicked on Cancel");
                    //Save the user's choice and never bother them again.
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    prefs.edit().putBoolean("denied_battery_optimization_request", true).commit();
                    dialog.cancel();
                }
            });
            builder.show();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_EXCEMPT_OP) {
                Log.d(LOGTAG, "User wants to excempt app from BATTERY OPTIMIZATIONS");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent();
                    String packageName = getPackageName();
                    PowerManager pm = (PowerManager) getSystemService(getApplicationContext().POWER_SERVICE);
                    if (pm.isIgnoringBatteryOptimizations(packageName)) {
                        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    }

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    prefs.edit().putBoolean("user_has_gone_through_battery_optimizations", true).commit();

                }
            }

        } else {
            if (requestCode == REQUEST_EXCEMPT_OP) {
                Log.d(LOGTAG, "Result code is cancel");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case Constants.BroadCastMessages.UI_NEW_CHAT_ITEM:
                        mAdapter.onChatCountChange();
                        return;
                }

            }
        };

        IntentFilter filter = new IntentFilter(Constants.BroadCastMessages.UI_NEW_CHAT_ITEM);
        registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_me_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.me) {
            Intent i = new Intent(ChatListActivity.this, MeActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(String contactJid, Chat.ContactType chatType) {

        Intent i = new Intent(ChatListActivity.this, ChatViewActivity.class);
        i.putExtra("contact_jid", contactJid);
        i.putExtra("chat_type", chatType);
        startActivity(i);
    }

    @Override
    public void onItemLongClick(final String contactJid, final int chatUniqueId, View anchor) {

        PopupMenu popup = new PopupMenu(ChatListActivity.this, anchor, Gravity.CENTER);
        //Inflating the Popup using xml file
        popup.getMenuInflater()
                .inflate(R.menu.chat_list_popup_menu, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.delete_chat:
                        if (ChatModel.get(getApplicationContext()).deleteChat(chatUniqueId)) {
                            mAdapter.onChatCountChange();
                            Toast.makeText(
                                    ChatListActivity.this,
                                    "Chat deleted successfully ",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        break;
                }
                return true;
            }
        });
        popup.show();

    }
}
