package com.blikoon.roosterplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.blikoon.roosterplus.adapters.ChatMessagesAdapter;
import com.blikoon.roosterplus.model.Chat;
import com.blikoon.roosterplus.model.ChatMessage;
import com.blikoon.roosterplus.model.ChatMessagesModel;
import com.blikoon.roosterplus.model.Contact;
import com.blikoon.roosterplus.model.ContactModel;
import com.blikoon.roosterplus.ui.KeyboardUtil;
import com.blikoon.roosterplus.xmpp.RoosterConnectionService;

public class ChatViewActivity extends AppCompatActivity implements ChatMessagesAdapter.OnInformRecyclerViewToScrollDownListener
        , KeyboardUtil.KeyboardVisibilityListener, ChatMessagesAdapter.OnItemLongClickListener {

    private static final String LOGTAG = "ChatViewActivity";
    public static final int FILE_SELECT_ACTIVITY_REQUEST_CODE = 123;
    RecyclerView chatMessagesRecyclerView;
    private EditText textSendEditText;
    private ImageButton sendMessageButton;
    ChatMessagesAdapter adapter;
    private String counterpartJid;
    private BroadcastReceiver mReceiveMessageBroadcastReceiver;
    private View snackbar;
    private View snackbarStranger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_view);

        //Get the counterpart Jid
        Intent intent = getIntent();
        counterpartJid = intent.getStringExtra("contact_jid");
        Chat.ContactType chatType = (Chat.ContactType) intent.getSerializableExtra("chat_type");
        setTitle(counterpartJid);

        chatMessagesRecyclerView = (RecyclerView) findViewById(R.id.chatMessagesRecyclerView);
        chatMessagesRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        adapter = new ChatMessagesAdapter(getApplicationContext(), counterpartJid);
        adapter.setmOnInformRecyclerViewToScrollDownListener(this);
        adapter.setOnItemLongClickListener(this);
        chatMessagesRecyclerView.setAdapter(adapter);

        textSendEditText = (EditText) findViewById(R.id.textinput);
        sendMessageButton = (ImageButton) findViewById(R.id.textSendButton);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                RoosterConnectionService.getConnection().sendMessage(textSendEditText.getText().toString(), counterpartJid);
                adapter.onMessageAdd();
                textSendEditText.getText().clear();

            }
        });

        Contact contactCheck = ContactModel.get(getApplicationContext()).getContactByJidString(counterpartJid);

        if (!ContactModel.get(getApplicationContext()).isContactStranger(counterpartJid)) {
            if (contactCheck.isOnlineStatus()) {
                Log.d(LOGTAG, counterpartJid + "is ONLINE");
                sendMessageButton.setImageDrawable(ContextCompat.getDrawable(ChatViewActivity.this, R.drawable.ic_send_text_online));
            } else {
                sendMessageButton.setImageDrawable(ContextCompat.getDrawable(ChatViewActivity.this, R.drawable.ic_send_text_offline));

                Log.d(LOGTAG, counterpartJid + "is OFFLINE");
            }

        }


        snackbar = findViewById(R.id.snackbar);
        snackbarStranger = findViewById(R.id.snackbar_stranger);

        if (!ContactModel.get(getApplicationContext()).isContactStranger(counterpartJid)) {
            snackbarStranger.setVisibility(View.GONE);
            Log.d(LOGTAG, counterpartJid + " is not a stranger");
            Contact contact = ContactModel.get(this).getContactByJidString(counterpartJid);
            Log.d(LOGTAG, "We got a contact with JID :" + contact.getJid());

            if (contact.isPendingFrom()) {
                Log.d(LOGTAG, " Your subscription to " + contact.getJid() + " is in the FROM direction is in pending state. Should show the snackbar");
                int paddingBottom = getResources().getDimensionPixelOffset(R.dimen.chatview_recycler_view_padding_huge);
                chatMessagesRecyclerView.setPadding(0, 0, 0, paddingBottom);
                snackbar.setVisibility(View.VISIBLE);
            } else {
                int paddingBottom = getResources().getDimensionPixelOffset(R.dimen.chatview_recycler_view_padding_normal);
                chatMessagesRecyclerView.setPadding(0, 0, 0, paddingBottom);
                snackbar.setVisibility(View.GONE);
            }

        } else {
            if (chatType == Chat.ContactType.STRANGER) {
                Log.d(LOGTAG, "Chat type is STRANGER");
                //We fall here if this was a subscription request from a stranger
                int paddingBottom = getResources().getDimensionPixelOffset(R.dimen.chatview_recycler_view_padding_huge);
                chatMessagesRecyclerView.setPadding(0, 0, 0, paddingBottom);
                snackbar.setVisibility(View.VISIBLE);
                snackbarStranger.setVisibility(View.GONE);

            } else {
                Log.d(LOGTAG, counterpartJid + " is a stranger. Hiding snackbar");
                int paddingBottom = getResources().getDimensionPixelOffset(R.dimen.chatview_recycler_view_padding_huge);
                chatMessagesRecyclerView.setPadding(0, 0, 0, paddingBottom);
                snackbarStranger.setVisibility(View.VISIBLE);
                snackbar.setVisibility(View.GONE);

            }


        }
        TextView snackBarActionAccept = (TextView) findViewById(R.id.snackbar_action_accept);
        snackBarActionAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //User accepts presence subscription

                //Add Them to your roster if they are strangers
                if (ContactModel.get(getApplicationContext()).isContactStranger(counterpartJid)) {
                    if (ContactModel.get(getApplicationContext()).addContact(new Contact(counterpartJid, Contact.SubscriptionType.NONE))) {
                        Log.d(LOGTAG, "Previously stranger contact " + counterpartJid + "now successfully added to local Roster");
                    }
                }
                Log.d(LOGTAG, " Accept presence subscription from :" + counterpartJid);
                if (RoosterConnectionService.getConnection().subscribed(counterpartJid)) {
                    ContactModel.get(getApplicationContext()).updateContactSubscriptionOnSendSubscribed(counterpartJid);
                    Toast.makeText(ChatViewActivity.this, "Subscription from " + counterpartJid + "accepted",
                            Toast.LENGTH_LONG).show();
                }
                snackbar.setVisibility(View.GONE);

            }
        });

        TextView snackBarActionDeny = (TextView) findViewById(R.id.snackbar_action_deny);
        snackBarActionDeny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //User denies presence subscription
                Log.d(LOGTAG, " Deny presence subscription from :" + counterpartJid);
                if (RoosterConnectionService.getConnection().unsubscribed(counterpartJid)) {
                    ContactModel.get(getApplicationContext()).updateContactSubscriptionOnSendSubscribed(counterpartJid);

                    //No action required in the Contact Model regarding subscriptions
                    Toast.makeText(getApplicationContext(), "Subscription Rejected", Toast.LENGTH_LONG).show();
                }
                snackbar.setVisibility(View.GONE);

            }
        });

        TextView snackBarStrangerAddContact = findViewById(R.id.snackbar_action_accept_stranger);
        snackBarStrangerAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContactModel.get(getApplicationContext()).addContact(new Contact(counterpartJid, Contact.SubscriptionType.NONE))) {
                    if (RoosterConnectionService.getConnection().addContactToRoster(counterpartJid)) {
                        Log.d(LOGTAG, counterpartJid + " successfully added to remote roster");
                        snackbarStranger.setVisibility(View.GONE);
                    }
                }

            }
        });

        TextView snackBarStrangerBlock = findViewById(R.id.snackbar_action_deny_stranger);
        snackBarStrangerBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChatViewActivity.this, "Feature not implemented yet", Toast.LENGTH_SHORT).show();


            }
        });

        KeyboardUtil.setKeyboardVisibilityListener(this, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.contact_details_chat_view) {
            Intent i = new Intent(ChatViewActivity.this, ContactDetailsActivity.class);
            i.putExtra("contact_jid", counterpartJid);
            startActivity(i);
        }

        if (item.getItemId() == R.id.send_file) {
            Log.d(LOGTAG, " User wants to select a file");
            sendFile();

        }

        if (item.getItemId() == R.id.send_image) {
            ChatMessage chatMessage = new ChatMessage("sent image", System.currentTimeMillis(), ChatMessage.Type.IMAGE_SENT, counterpartJid);
            chatMessage.setSentStatus(ChatMessage.SentStatus.SENT);
            chatMessage.setAttachmentPath("image path");
            if (ChatMessagesModel.get(getApplicationContext()).addMessage(chatMessage)) {
                adapter.onMessageAdd();

            } else {
                Log.d(LOGTAG, " File message could not be added to model");

            }

        }
        if (item.getItemId() == R.id.receive_video) {
            ChatMessage chatMessage = new ChatMessage("sent image", System.currentTimeMillis(), ChatMessage.Type.VIDEO_RECEIVED, counterpartJid);
            chatMessage.setSentStatus(ChatMessage.SentStatus.SENT);
            chatMessage.setAttachmentPath("image path");
            if (ChatMessagesModel.get(getApplicationContext()).addMessage(chatMessage)) {
                adapter.onMessageAdd();

            } else {
                Log.d(LOGTAG, " File message could not be added to model");

            }

        }
        if (item.getItemId() == R.id.send_office) {
            ChatMessage chatMessage = new ChatMessage("sent image", System.currentTimeMillis(), ChatMessage.Type.OFFICE_SENT, counterpartJid);
            chatMessage.setSentStatus(ChatMessage.SentStatus.SENT);
            chatMessage.setAttachmentPath("image path");
            if (ChatMessagesModel.get(getApplicationContext()).addMessage(chatMessage)) {
                adapter.onMessageAdd();

            } else {
                Log.d(LOGTAG, " File message could not be added to model");

            }

        }

        return super.onOptionsItemSelected(item);
    }

    private void sendFile() {
        Intent intent = (new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));
        startActivityForResult(intent, FILE_SELECT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String filePath;

            if (requestCode == FILE_SELECT_ACTIVITY_REQUEST_CODE) {
                Uri selectedImage = data.getData();

                //Save the file in a directory on the device and then send it.
                filePath = RealPathUtil.getRealPath(this, selectedImage);
                Log.d(LOGTAG, "File uri is : " + selectedImage.toString());
                Log.d(LOGTAG, "File path is : " + filePath);
                if (filePath == null) {
                    Log.d(LOGTAG, "File uri is null");
                    return;
                }

                RoosterConnectionService.getConnection().sendFile(filePath, counterpartJid);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiveMessageBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adapter.shouldScrollRecyclerViewDown()) {
            adapter.informRecyclerViewToScrollDown();
        } else {
            adapter.setShouldScrollRecyclerViewDown(true);
        }


        mReceiveMessageBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case Constants.BroadCastMessages.UI_NEW_MESSAGE_FLAG:
                        adapter.onMessageAdd();
                        return;
                    case Constants.BroadCastMessages.UI_ONLINE_STATUS_CHANGE:
                        String contactJid = intent.getStringExtra(Constants.ONLINE_STATUS_CHANGE_CONTACT);
                        Log.d(LOGTAG, " Online status change for " + contactJid + " received in ChatViewActivity");

                        if (!ContactModel.get(getApplicationContext()).isContactStranger(counterpartJid)) {
                            if (counterpartJid.equals(contactJid)) {
                                Contact mContact = ContactModel.get(getApplicationContext()).getContactByJidString(contactJid);
                                if (mContact.isOnlineStatus()) {
                                    Log.d(LOGTAG, "From Chat View, user " + contactJid + " has come ONLINE");
                                    sendMessageButton.setImageDrawable(ContextCompat.getDrawable(ChatViewActivity.this, R.drawable.ic_send_text_online));

                                } else {
                                    Log.d(LOGTAG, "From Chat View, user " + contactJid + " has gone OFFLINE");
                                    sendMessageButton.setImageDrawable(ContextCompat.getDrawable(ChatViewActivity.this, R.drawable.ic_send_text_offline));

                                }
                            }

                        }
                }

            }
        };

        IntentFilter filter = new IntentFilter(Constants.BroadCastMessages.UI_NEW_MESSAGE_FLAG);
        filter.addAction(Constants.BroadCastMessages.UI_ONLINE_STATUS_CHANGE);
        registerReceiver(mReceiveMessageBroadcastReceiver, filter);
    }

    @Override
    public void onInformRecyclerViewToScrollDown(int size) {
        chatMessagesRecyclerView.scrollToPosition(size - 1);

    }

    @Override
    public void onKeyboardVisibilityChanged(boolean keyboardVisible) {
        adapter.informRecyclerViewToScrollDown();
    }

    @Override
    public void onItemLongClick(final int uniqueId, View anchor) {

        PopupMenu popup = new PopupMenu(ChatViewActivity.this, anchor, Gravity.CENTER);
        //Inflating the Popup using xml file
        popup.getMenuInflater()
                .inflate(R.menu.chat_view_popup_menu, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == R.id.delete_message) {
                    if (ChatMessagesModel.get(getApplicationContext()).deleteMessage(uniqueId)) {
                        adapter.onMessageAdd();
                        Toast.makeText(
                                ChatViewActivity.this,
                                "Message deleted successfully ",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
                return true;
            }
        });
        popup.show();

    }
}
