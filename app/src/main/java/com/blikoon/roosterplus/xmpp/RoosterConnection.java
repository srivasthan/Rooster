package com.blikoon.roosterplus.xmpp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blikoon.roosterplus.Constants;
import com.blikoon.roosterplus.MimeUtils;
import com.blikoon.roosterplus.Utilities;
import com.blikoon.roosterplus.model.ChatMessage;
import com.blikoon.roosterplus.model.ChatMessagesModel;
import com.blikoon.roosterplus.model.ChatModel;
import com.blikoon.roosterplus.model.Contact;
import com.blikoon.roosterplus.model.ContactModel;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.SubscribeListener;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jivesoftware.smackx.httpfileupload.UploadProgressListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.android.ServerPingWithAlarmManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

import static com.blikoon.roosterplus.model.Chat.ContactType.STRANGER;

/**
 * Created by gakwaya on 2018/1/11.
 */

public class RoosterConnection implements ConnectionListener,
        SubscribeListener, RosterListener {


    private class UploadFileTask extends AsyncTask<String, Long, URL> {

        String fileFullPath = null;
        String counterpartJid = null;

        @Override
        protected URL doInBackground(String... arguments) {
            fileFullPath = arguments[0];
            counterpartJid = arguments[1];

            try {
                return httpFileUploadManager.uploadFile(new File(fileFullPath), new UploadProgressListener() {
                    @Override
                    public void onUploadProgress(long uploadedBytes, long totalBytes) {
                        publishProgress((uploadedBytes / totalBytes) * 100);

                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
                return null;
            } catch (SmackException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }


        }

        @Override
        protected void onPostExecute(URL mUrl) {

            if (mUrl != null) {
                Log.d("UploadTask", "File upload done ,File Get Url is : " + mUrl.toString() +
                        ", File full path is :" + fileFullPath +
                        ", Counterpart Jid is :" + counterpartJid);
                
                ChatMessage message = new ChatMessage(mUrl.toString(), System.currentTimeMillis(), Utilities.getMessageTypefromFileFullPath(fileFullPath, true), counterpartJid);
                message.setAttachmentPath(fileFullPath);
                message.setSentStatus(ChatMessage.SentStatus.SENT);

                if (sendMessage(message)) {
                    Log.d("UploadTask ", "File message : " + message.getAttachmentPath() +
                            " successfully sent to " + message.getContactJid());
                } else {
                    Log.d("UploadTask ", "Something went wrong while sending message : " + message.getAttachmentPath() +
                            " to " + message.getContactJid());
                }
            } else {
                Log.d("UploadTask", "File upload Failed.");
            }

            super.onPostExecute(mUrl);
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            Log.d("UploadTask", "Progress :" + Long.toString(values[0]));
            super.onProgressUpdate(values);
        }
    }

    //Params : Url,filename,rootPathString,contactJid

    private class DownloadFileTask extends AsyncTask<String, Integer, String> {

        String inputUrl;
        String inputFileName;
        String inputRootPathString;
        String inputContactJid;

        @Override
        protected String doInBackground(String... strings) {
            inputUrl = strings[0];
            inputFileName = strings[1];
            inputRootPathString = strings[2];
            inputContactJid = strings[3];
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    String error = "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                    Log.d("DownloadFileTask", error);
                    return null;
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                Log.d("DownloadFileTask", " Inside doInbackground ,File will be saved to : " + inputRootPathString + "/" + inputFileName);
                output = new FileOutputStream(inputRootPathString + "/" + inputFileName);


                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return null;
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return inputRootPathString + "/" + inputFileName;

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d("DownloadFileTask", " Progress :" + Integer.toString(values[0]));
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.d("DownloadFileTask", " File download was successful, file saved to :" + inputRootPathString + "/" + inputFileName);

                File file = new File(inputRootPathString + "/" + inputFileName);

                if (file.exists()) {
                    Log.d("DownloadFileTask", " File exists :" + inputRootPathString + "/" + inputFileName);
                    ChatMessage chatMessage = new ChatMessage(inputUrl, System.currentTimeMillis(), Utilities.getMessageTypefromFileFullPath(inputRootPathString + "/" + inputFileName, false), inputContactJid);
                    chatMessage.setSentStatus(ChatMessage.SentStatus.RECEIVED);
                    chatMessage.setAttachmentPath(inputRootPathString + "/" + inputFileName);
                    if (ChatMessagesModel.get(mApplicationContext).addMessage(chatMessage)) {
                        Log.d("DownloadFileTask", " File message successfully saved to model");
                        informChatViewRecyclerViewOfNewMessage();

                        //Cause a scan for the image to show up in gallery,only videos and images
                        if (chatMessage.getType() == ChatMessage.Type.VIDEO_RECEIVED ||
                                chatMessage.getType() == ChatMessage.Type.IMAGE_RECEIVED) {
                            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            File f = new File(inputRootPathString + "/" + inputFileName);
                            Uri contentUri = Uri.fromFile(f);
                            mediaScanIntent.setData(contentUri);
                            mApplicationContext.sendBroadcast(mediaScanIntent);
                        }


                    } else {
                        Log.d("DownloadFileTask", " File message could not be added to model");

                    }
                } else {
                    Log.d("DownloadFileTask", "File does not exist :" + inputRootPathString + "/" + inputFileName);
                    return;
                }


            } else {
                Log.d("DownloadFileTask", "Something went wrong while downloading file :" + inputUrl);

            }
            //super.onPostExecute(result);
        }
    }


    private static final String LOGTAG = "RoosterConnection";

    private final Context mApplicationContext;
    private String mUsername;
    private String mPassword;
    private String mServiceName;
    private XMPPTCPConnection mConnection;
    private ConnectionState mConnectionState;
    private PingManager pingManager;
    private ChatManager chatManager;
    private Roster mRoster;
    private VCardManager vCardManager;
    private HttpFileUploadManager httpFileUploadManager;


    public static enum ConnectionState {
        OFFLINE, CONNECTING, ONLINE
    }

    public ConnectionState getmConnectionState() {
        return mConnectionState;
    }

    public void setmConnectionState(ConnectionState mConnectionState) {
        this.mConnectionState = mConnectionState;
    }

    public String getConnectionStateString() {
        switch (mConnectionState) {
            case OFFLINE:
                return "Offline";

            case CONNECTING:
                return "Connecting...";

            case ONLINE:
                return "Online";

            default:
                return "Offline";
        }

    }

    private void updateActivitiesOfConnectionStateChange(ConnectionState mConnectionState) {
        ConnectionState connectionState = mConnectionState;
        String status;
        switch (mConnectionState) {
            case OFFLINE:
                status = "Offline";
                break;
            case CONNECTING:
                status = "Connecting...";
                break;
            case ONLINE:
                status = "Online";
                break;
            default:
                status = "Offline";
                break;
        }
        Intent i = new Intent(Constants.BroadCastMessages.UI_CONNECTION_STATUS_CHANGE_FLAG);
        i.putExtra(Constants.UI_CONNECTION_STATUS_CHANGE, status);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
    }

    public RoosterConnection(Context mApplicationContext) {

        Log.d(LOGTAG, "RoosterConnection Constructor called.");
        this.mApplicationContext = mApplicationContext;
    }

    public void connect() throws IOException, XMPPException, SmackException {

        mConnectionState = ConnectionState.CONNECTING;
        updateActivitiesOfConnectionStateChange(ConnectionState.CONNECTING);
        gatherCredentials();

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        MemorizingTrustManager mtm = new MemorizingTrustManager(mApplicationContext);
        try {
            if (sc != null) {
                sc.init(null, new X509TrustManager[]{mtm}, new SecureRandom());
            }
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .setCustomSSLContext(sc)
                .setHostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()))
                .setXmppDomain(mServiceName)
                .setResource("Rooster+")
                .setDebuggerEnabled(true)
                .build();

        Log.i(LOGTAG, "Username : " + mUsername);
        Log.i(LOGTAG, "Password : " + mPassword);
        Log.i(LOGTAG, "Server : " + mServiceName);

        SmackConfiguration.DEBUG = true;
        XMPPTCPConnection.setUseStreamManagementDefault(true);


        mConnection = new XMPPTCPConnection(conf);
        mConnection.setPacketReplyTimeout(30000);
        mConnection.setUseStreamManagement(true);
        mConnection.setUseStreamManagementResumption(true);
        mConnection.setPreferredResumptionTime(5);
        mConnection.addConnectionListener(this);

        mRoster = Roster.getInstanceFor(mConnection);
        mRoster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        mRoster.addSubscribeListener(this);
        mRoster.addRosterListener(this);

        vCardManager = VCardManager.getInstanceFor(mConnection);
        httpFileUploadManager = HttpFileUploadManager.getInstanceFor(mConnection);


        chatManager = ChatManager.getInstanceFor(mConnection);
        chatManager.addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
                Log.d(LOGTAG, "message.getBody() :" + message.getBody());
                Log.d(LOGTAG, "message.getFrom() :" + message.getFrom());

                String messageSource = message.getFrom().toString();

                String contactJid = "";
                if (messageSource.contains("/")) {
                    contactJid = messageSource.split("/")[0];
                    Log.d(LOGTAG, "The real jid is :" + contactJid);
                    Log.d(LOGTAG, "The message is from :" + from);
                } else {
                    contactJid = messageSource;
                }


                //Test if the received file is a Url
                if (Utilities.isStringFileUrl(message.getBody())) {
                    //Initiate the process to download the file
                    downloadFileFromServer(message.getBody(), contactJid);
                } else {
                    //Add message to the model
                    ChatMessagesModel.get(mApplicationContext).addMessage(new ChatMessage(message.getBody(), System.currentTimeMillis(), ChatMessage.Type.RECEIVED, contactJid));


                    //Add Chat for Stranger if not already available
                    if (ContactModel.get(mApplicationContext).isContactStranger(contactJid)) {
                        List<com.blikoon.roosterplus.model.Chat> chats = ChatModel.get(mApplicationContext).getChatsByJid(contactJid);
                        if (chats.size() == 0) {
                            Log.d(LOGTAG, contactJid + " is a new chat, adding them. With timestamp :" + Utilities.getFormattedTime(System.currentTimeMillis()));

                            com.blikoon.roosterplus.model.Chat chatRooster = new com.blikoon.roosterplus.model.Chat(contactJid, message.getBody(), com.blikoon.roosterplus.model.Chat.ContactType.ONE_ON_ONE, System.currentTimeMillis(), 0);
                            ChatModel.get(mApplicationContext).addChat(chatRooster);

                            //Notify interested activities
                            Intent intent = new Intent(Constants.BroadCastMessages.UI_NEW_CHAT_ITEM);
                            intent.setPackage(mApplicationContext.getPackageName());
                            mApplicationContext.sendBroadcast(intent);
                        }
                    }
                    informChatViewRecyclerViewOfNewMessage();

                }


            }
        });


        ServerPingWithAlarmManager.getInstanceFor(mConnection).setEnabled(true);
        pingManager = PingManager.getInstanceFor(mConnection);
        pingManager.setPingInterval(30);

        try {
            Log.d(LOGTAG, "Calling connect() ");
            mConnection.connect();
            mConnection.login(mUsername, mPassword);
            Log.d(LOGTAG, " login() Called ");
            syncContactListWithRemoteRoster();
            //Cache the avatars for fast local use
            saveUserAvatarsLocaly();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void downloadFileFromServer(String fileUrl, String contactJid) {

        //Get the filename and the extension
        URL url;

        try {
            url = new URL(fileUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        String extension = MimeUtils.extractRelevantExtension(url);
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

        if (extension != null) {
            Log.d(LOGTAG, "Received Extension is not null, filename is : " + fileName);

            //Create the file path on the device

            File rootPath;

            if (MimeUtils.isFileVideo(extension)) {
                rootPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "RoosterPlus");

            } else if (MimeUtils.isFileAudio(extension)) {
                rootPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "RoosterPlus");

            } else if (MimeUtils.isFileImage(extension)) {
                rootPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "RoosterPlus");
            } else if (MimeUtils.isFileDocument(extension)) {
                rootPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "RoosterPlus");
            } else {
                rootPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RoosterPlus");

            }
            //Create the root Dir if it is not there
            if (!rootPath.exists()) {
                if (rootPath.mkdirs()) {
                    Log.d(LOGTAG, " Files directory created successfully :" + rootPath.getAbsolutePath());
                } else {
                    Log.d(LOGTAG, " Could not create Files directory :" + rootPath.getAbsolutePath());
                }
            }

            // Pass over to the AsyncTask to do the download. Give it the url, the filename and the rootpath
            //Params : Url,filename,rootPathString,contactJid
            DownloadFileTask downloadFileTask = new DownloadFileTask();
            downloadFileTask.execute(fileUrl, fileName, rootPath.getAbsolutePath(), contactJid);

        }


    }

    public void sendFile(String fileFullPath, String counterpartJid) {
        UploadFileTask uploadFileTask = new UploadFileTask();
        uploadFileTask.execute(fileFullPath, counterpartJid);
    }


    private void informChatViewRecyclerViewOfNewMessage() {
        //If the view (ChatViewActivity) is visible, inform it so it can do necessary adjustments
        Intent intent = new Intent(Constants.BroadCastMessages.UI_NEW_MESSAGE_FLAG);
        intent.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(intent);
    }

    /**
     * Sets the avatar for the currently connected user
     */
    public boolean setSelfAvatar(byte[] image) {
        //Get the avatar for display
        VCard vCard = new VCard();
        vCard.setAvatar(image);

        if (vCardManager != null) {
            try {
                vCardManager.saveVCard(vCard);
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
                return false;
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
                return false;
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                return false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Fetches the avatars from the server and saves them in a local directory on external storage for
     * use in App without needing to user network all the time.
     */
    public void saveUserAvatarsLocaly() {
        Log.d(LOGTAG, "SAVING THE USER AVATARS TO DISK...");
        File rootPath = new File(mApplicationContext.getExternalFilesDir("DIRECTORY_PICTURES"), "profile_pictures");

        //Create the root Dir if it is not there
        if (!rootPath.exists()) {
            if (rootPath.mkdirs()) {
                Log.d(LOGTAG, "profile_pictures directory created successfully :" + rootPath.getAbsolutePath());
            } else {
                Log.d(LOGTAG, "Could not create profile_pictures directory :" + rootPath.getAbsolutePath());
            }
        }

        //Save self profile image to disk if available
        String selfJid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid", null);
        if (selfJid != null) {
            Log.d(LOGTAG, "Got a valid self Jid :" + selfJid);
            VCard vCard = null;
            try {
                vCard = vCardManager.loadVCard();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (vCard != null) {
                saveAvatarToDisk(vCard, rootPath, selfJid);
            }
        } else {
            Log.d(LOGTAG, "Self jid is NULL");
        }


        //Save other contacts profile images to disk
        List<String> contacts = ContactModel.get(mApplicationContext).getContactsJidStrings();
        for (String contact : contacts) {

            VCard vCard = getUserVCard(contact);
            if (vCard != null) {
                saveAvatarToDisk(vCard, rootPath, contact);
            }
        }
    }

    private void saveAvatarToDisk(VCard vCard, File rootPath, String contact) {

        String imageMimeType = null;
        String imageExtension = null;
        Bitmap.CompressFormat format = null;
        if (vCard != null) {
            byte[] image_data = vCard.getAvatar();
            imageMimeType = vCard.getAvatarMimeType();
            if (image_data != null) {
                Log.d(LOGTAG, "Found an avatar for user : " + contact);

                if (imageMimeType.equals("image/jpeg")) {
                    Log.d(LOGTAG, "The image mime type is JPEG");
                    imageExtension = "jpeg";
                    format = Bitmap.CompressFormat.JPEG;
                } else if (imageMimeType.equals("image/jpg")) {
                    Log.d(LOGTAG, "The image mime type is JPG");
                    imageExtension = "jpg";
                    format = Bitmap.CompressFormat.JPEG;
                } else if (imageMimeType.equals("image/png")) {
                    Log.d(LOGTAG, "The image mime type is PNG");
                    imageExtension = "png";
                    format = Bitmap.CompressFormat.PNG;
                }

                Bitmap bitmap = BitmapFactory.decodeByteArray(image_data, 0, image_data.length);
                Bitmap bpResized = bitmap.createScaledBitmap(bitmap, 70, 70, false);

                File file = new File(rootPath, contact + "." + imageExtension);
                if (file.exists())
                    file.delete();
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    bpResized.compress(format, 90, out);
                    out.flush();
                    out.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(LOGTAG, "Image write operation successful.File :" + file.getAbsolutePath());

            } else {
                Log.d(LOGTAG, "Could not get avatar for user : " + contact);
            }
        }

    }


    public VCard getUserVCard(String user) {
        EntityBareJid jid = null;
        try {
            jid = JidCreate.entityBareFrom(user);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        VCard vCard = null;

        if (vCardManager != null) {
            try {
                vCard = vCardManager.loadVCard(jid);
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        if (vCard != null) {
            return vCard;
        }
        return null;
    }

    /**
     * This function assumes that the avatars have been previously saved by a call to saveUserAvatarsLocaly ().
     * You should make sure it has been called at least once before you call this method.
     */
    public String getProfileImageAbsolutePath(String jid) {
        File rootPath = new File(mApplicationContext.getExternalFilesDir("DIRECTORY_PICTURES"), "profile_pictures");

        //Create the root Dir if it is not there
        if (!rootPath.exists()) {
            if (rootPath.mkdirs()) {
                Log.d(LOGTAG, "profile_pictures directory created successfully :" + rootPath.getAbsolutePath());
            } else {
                Log.d(LOGTAG, "Could not create profile_pictures directory :" + rootPath.getAbsolutePath());
            }
        }

        /**TO DO  : FIND A BETTER CONSTRUCT TO USE HERE AND GET RID OF THESE NESTED IFs **/

        File file = new File(rootPath, jid + ".jpeg");
        if (!file.exists()) {
            file = new File(rootPath, jid + ".jpg");
            if (!file.exists()) {
                file = new File(rootPath, jid + ".png");
                if (!file.exists()) {
                    return null;
                } else {
                    return file.getAbsolutePath();
                }

            } else {
                return file.getAbsolutePath();
            }
        } else {
            return file.getAbsolutePath();
        }
    }


    /**
     * Retrieves roster contacts from the server and syncs with the contact list saved in the db
     */
    public void syncContactListWithRemoteRoster() {
        Log.d(LOGTAG, "Roster SYNCING...");
        //Get roster form server
        Collection<RosterEntry> entries = getRosterEntries();

        Log.d(LOGTAG, "Retrieving roster entries from server. " + entries.size() + " contacts in his roster");

        for (RosterEntry entry : entries) {
            RosterPacket.ItemType itemType = entry.getType();

            Log.d(LOGTAG, "Entry " + entry.getJid() + " has subscription " + entry.getType());
//            String stringItemType = getRosterItemTypeString(itemType);

            //Update data in the db
            //Get all the contacts
            List<String> contacts = ContactModel.get(mApplicationContext).getContactsJidStrings();

            //Add new roster entries
            if ((!contacts.contains(entry.getJid().toString()))
                    && (itemType != RosterPacket.ItemType.none)) {
                /* We only add contacts that we don't have already and that don't have a subscription type of none.
                 * none subscriptions add no needed information to our local contact list */
                //Add it to the db
                if (ContactModel.get(mApplicationContext).addContact(new Contact(entry.getJid().toString(),
                        rosterItemTypeToContactSubscriptionType(itemType)))) {
                    Log.d(LOGTAG, "New Contact " + entry.getJid().toString() + "Added successfully");
                    //mAdapter.notifyForUiUpdate();
                } else {
                    Log.d(LOGTAG, "Could not add Contact " + entry.getJid().toString());
                }
            }

            //Update already existing entries if necessary
            if ((contacts.contains(entry.getJid().toString()))) {

                Contact.SubscriptionType subscriptionType = rosterItemTypeToContactSubscriptionType(itemType);
                boolean isSubscriptionPending = entry.isSubscriptionPending();
                Contact mContact = ContactModel.get(mApplicationContext)
                        .getContactByJidString(entry.getJid().toString());
                mContact.setPendingTo(isSubscriptionPending);
                mContact.setSubscriptionType(subscriptionType);
                ContactModel.get(mApplicationContext).updateContactSubscription(mContact);
            }

        }
    }


    public Collection<RosterEntry> getRosterEntries() {
        Collection<RosterEntry> entries = mRoster.getEntries();
        Log.d(LOGTAG, "The current user has " + entries.size() + " contacts in his roster");
        return entries;
    }

    public void disconnect() {
        Log.d(LOGTAG, "Disconnecting from server " + mServiceName);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        prefs.edit().putBoolean("xmpp_logged_in", false).commit();

        if (mConnection != null) {
            mConnection.disconnect();
        }
    }

    public void sendMessage(String body, String toJid) {
        Log.d(LOGTAG, "Sending message to :" + toJid);

        EntityBareJid jid = null;

        try {
            jid = JidCreate.entityBareFrom(toJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Chat chat = chatManager.chatWith(jid);
        try {
            Message message = new Message(jid, Message.Type.chat);
            message.setBody(body);
            chat.send(message);
            //Add the message to the model
            ChatMessagesModel.get(mApplicationContext).addMessage(new ChatMessage(body, System.currentTimeMillis(), ChatMessage.Type.SENT, toJid));

        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public boolean sendMessage(ChatMessage chatMessage) {
        Log.d(LOGTAG, "Sending message to :" + chatMessage.getContactJid());

        EntityBareJid jid = null;

        try {
            jid = JidCreate.entityBareFrom(chatMessage.getContactJid());
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return false;
        }

        Chat chat = chatManager.chatWith(jid);
        try {
            Message message = new Message(jid, Message.Type.chat);
            message.setBody(chatMessage.getMessage());
            chat.send(message);
            //Add the message to the model
            if (ChatMessagesModel.get(mApplicationContext).addMessage(chatMessage)) {
                informChatViewRecyclerViewOfNewMessage();
                return true;
            } else {
                return false;
            }
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    //Adds contact to the remote roster. We maintain our own local contact list[Roster]
    public boolean addContactToRoster(String contactJid) {
        Jid jid;
        try {
            jid = JidCreate.from(contactJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return false;
        }

        try {
            mRoster.createEntry(jid.asBareJid(), "", null);
        } catch (SmackException.NotLoggedInException e) {
            e.printStackTrace();
            return false;
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
            return false;
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            return false;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Contact.SubscriptionType rosterItemTypeToContactSubscriptionType(RosterPacket.ItemType itemType) {
        if (itemType == RosterPacket.ItemType.none) {
            return Contact.SubscriptionType.NONE;
        } else if (itemType == RosterPacket.ItemType.from) {
            return Contact.SubscriptionType.FROM;
        } else if (itemType == RosterPacket.ItemType.to) {
            return Contact.SubscriptionType.TO;
        } else if (itemType == RosterPacket.ItemType.both) {
            return Contact.SubscriptionType.BOTH;
        } else
            return Contact.SubscriptionType.NONE;

    }

    public boolean subscribe(String contact) {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from(contact);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return false;
        }
        Presence subscribe = new Presence(jidTo, Presence.Type.subscribe);
        if (sendPresense(subscribe)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean unsubscribe(String contact) {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from(contact);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return false;
        }
        Presence unsubscribe = new Presence(jidTo, Presence.Type.unsubscribe);
        if (sendPresense(unsubscribe)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean unsubscribed(String contact) {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from(contact);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return false;
        }
        Presence unsubscribed = new Presence(jidTo, Presence.Type.unsubscribed);
        if (sendPresense(unsubscribed)) {
            return true;
        } else {
            return false;
        }

    }

    public boolean subscribed(String contact) {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from(contact);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return false;
        }
        Presence subscribe = new Presence(jidTo, Presence.Type.subscribed);
        sendPresense(subscribe);

        return true;
    }

    public boolean removeRosterEntry(String contactJid) {
        Jid jid;
        try {
            jid = JidCreate.from(contactJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return false;
        }

        RosterEntry entry = mRoster.getEntry(jid.asBareJid());
        try {
            mRoster.removeEntry(entry);
        } catch (SmackException.NotLoggedInException e) {
            e.printStackTrace();
            return false;
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
            return false;
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            return false;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public boolean sendPresense(Presence presence) {
        if (mConnection != null) {
            try {
                mConnection.sendStanza(presence);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                return false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }


    private void gatherCredentials() {
        String jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid", null);
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_password", null);


        if (jid != null) {
            mUsername = jid.split("@")[0];
            mServiceName = jid.split("@")[1];
        } else {
            mUsername = "";
            mServiceName = "";
        }
    }

    private void notifyUiForConnectionError() {
        Intent i = new Intent(Constants.BroadCastMessages.UI_CONNECTION_ERROR);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(LOGTAG, "Sent the broadcast for connection Error");
    }

    @Override
    public void connected(XMPPConnection connection) {

        Log.d(LOGTAG, "Connected");
        mConnectionState = ConnectionState.CONNECTING;
        updateActivitiesOfConnectionStateChange(ConnectionState.CONNECTING);

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        mConnectionState = ConnectionState.ONLINE;
        updateActivitiesOfConnectionStateChange(ConnectionState.ONLINE);


        Log.d(LOGTAG, "Authenticated");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        prefs.edit()
                .putBoolean("xmpp_logged_in", true)
                .commit();

        Intent i = new Intent(Constants.BroadCastMessages.UI_AUTHENTICATED);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(LOGTAG, "Sent the broadcast that we are authenticated");
    }

    @Override
    public void connectionClosed() {
        Log.d(LOGTAG, "connectionClosed");
        notifyUiForConnectionError();
        mConnectionState = ConnectionState.OFFLINE;
        updateActivitiesOfConnectionStateChange(ConnectionState.OFFLINE);

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Log.d(LOGTAG, "connectionClosedOnError");
        notifyUiForConnectionError();
        mConnectionState = ConnectionState.OFFLINE;
        updateActivitiesOfConnectionStateChange(ConnectionState.OFFLINE);


    }

    @Override
    public void reconnectionSuccessful() {
        Log.d(LOGTAG, "reconnectionSuccessful");
        mConnectionState = ConnectionState.ONLINE;
        updateActivitiesOfConnectionStateChange(ConnectionState.ONLINE);


    }

    @Override
    public void reconnectingIn(int seconds) {
        Log.d(LOGTAG, "Reconnecting in " + seconds + "seconds");
        mConnectionState = ConnectionState.CONNECTING;
        updateActivitiesOfConnectionStateChange(ConnectionState.CONNECTING);


    }

    @Override
    public void reconnectionFailed(Exception e) {
        Log.d(LOGTAG, "reconnectionFailed");
        mConnectionState = ConnectionState.OFFLINE;
        updateActivitiesOfConnectionStateChange(ConnectionState.OFFLINE);


    }

    /**
     * SubscribeListener Overrides
     */
    @Override
    public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
        Log.d(LOGTAG, "--------------------processSubscribe Called---------------------.");
        Log.d(LOGTAG, "JID is :" + from.toString());
        Log.d(LOGTAG, "Presence type :" + subscribeRequest.getType().toString());

        /*If somebody is not in our contact list, we should not process their subscription requests
         * We should however process their messages. After whe have exchanged a few messages, can we
         * then subscribe to each other's presence.*/

        if (!ContactModel.get(mApplicationContext).isContactStranger(from.toString())) {
            Log.d(LOGTAG, "Contact NOT a stranger");
            Contact mContact = ContactModel.get(mApplicationContext).getContactByJidString(from.toString());
            mContact.setPendingFrom(true);
            ContactModel.get(mApplicationContext).updateContactSubscription(mContact);
        } else {
            //Create a Chat with type STRANGER
            List<com.blikoon.roosterplus.model.Chat> chats = ChatModel.get(mApplicationContext).getChatsByJid(from.toString());
            if (chats.size() == 0) {
                //Only add the chat when it is not already available
                if (ChatModel.get(mApplicationContext).addChat(new com.blikoon.roosterplus.model.Chat(from.toString(), "Subscription Request", STRANGER,
                        System.currentTimeMillis(), 1))) {
                    Log.d(LOGTAG, "Chat item for stranger " + from.toString() + " successfully added to chat model");
                }
            }
        }
        //We do not provide an answer right away, we let the user actively accept or deny this subscription.
        return null;
    }

    /**
     * RosterListener Overrides
     */
    @Override
    public void entriesAdded(Collection<Jid> addresses) {

        for (Jid jid : addresses) {
            RosterEntry entry = mRoster.getEntry(jid.asBareJid());
            RosterPacket.ItemType itemType = entry.getType();
            boolean isSubscriptionPending = entry.isSubscriptionPending();

            //Get all the contacts
            List<String> contacts = ContactModel.get(mApplicationContext).getContactsJidStrings();

            //Add new roster entries
            if ((!contacts.contains(entry.getJid().toString()))
                    && (itemType != RosterPacket.ItemType.none)) {
                /* We only add contacts that we don't have already and that don't have a subscription type of none.
                 * none subscriptions add no needed information to our local contact list */
                //Add it to the db

                Contact mContact = new Contact(entry.getJid().toString(),
                        rosterItemTypeToContactSubscriptionType(itemType));
                mContact.setPendingTo(isSubscriptionPending);
                if (ContactModel.get(mApplicationContext).addContact(mContact)) {
                    Log.d(LOGTAG, "New Contact " + entry.getJid().toString() + "Added successfully");
                    //mAdapter.notifyForUiUpdate();
                } else {
                    Log.d(LOGTAG, "Could not add Contact " + entry.getJid().toString());
                }
            }

            //Update already existing entries if necessary
            if ((contacts.contains(entry.getJid().toString()))) {

                Contact.SubscriptionType subscriptionType = rosterItemTypeToContactSubscriptionType(itemType);
                Contact mContact = ContactModel.get(mApplicationContext)
                        .getContactByJidString(entry.getJid().toString());
                mContact.setPendingTo(isSubscriptionPending);
                mContact.setSubscriptionType(subscriptionType);
                ContactModel.get(mApplicationContext).updateContactSubscription(mContact);
            }
        }

    }

    @Override
    public void entriesUpdated(Collection<Jid> addresses) {

        for (Jid jid : addresses) {
            RosterEntry entry = mRoster.getEntry(jid.asBareJid());
            RosterPacket.ItemType itemType = entry.getType();
            boolean isSubscriptionPending = entry.isSubscriptionPending();

            List<String> contacts = ContactModel.get(mApplicationContext).getContactsJidStrings();

            //Update already existing entries if necessary
            if ((contacts.contains(entry.getJid().toString()))) {

                Contact.SubscriptionType subscriptionType = rosterItemTypeToContactSubscriptionType(itemType);
                Contact mContact = ContactModel.get(mApplicationContext)
                        .getContactByJidString(entry.getJid().toString());
                mContact.setPendingTo(isSubscriptionPending);
                mContact.setSubscriptionType(subscriptionType);
                ContactModel.get(mApplicationContext).updateContactSubscription(mContact);
            }
        }

    }

    @Override
    public void entriesDeleted(Collection<Jid> addresses) {

        for (Jid jid : addresses) {
            if (!ContactModel.get(mApplicationContext).isContactStranger(jid.toString())) {
                Contact mContact = ContactModel.get(mApplicationContext).getContactByJidString(jid.toString());
                if (ContactModel.get(mApplicationContext).deleteContact(mContact)) {
                    Log.d(LOGTAG, "Contact " + jid.toString() + " successfully deleted from the database");
                }
            }
        }


    }

    @Override
    public void presenceChanged(Presence presence) {

        Log.d(LOGTAG, "PresenceChanged Called .Presence is :" + presence.toString());

        Presence mPresence = mRoster.getPresence(presence.getFrom().asBareJid());
        Log.d(LOGTAG, "Best Presence is :" + mPresence.toString());
        Log.d(LOGTAG, "Type is  :" + mPresence.getType());
        Contact mContact = ContactModel.get(mApplicationContext).getContactByJidString(presence.getFrom().asBareJid().toString());

        if (mPresence.isAvailable() && (!mPresence.isAway())) {
            mContact.setOnlineStatus(true);
        } else {
            mContact.setOnlineStatus(false);
        }

        ContactModel.get(mApplicationContext).updateContactSubscription(mContact);

        Intent intent = new Intent(Constants.BroadCastMessages.UI_ONLINE_STATUS_CHANGE);
        intent.putExtra(Constants.ONLINE_STATUS_CHANGE_CONTACT, presence.getFrom().asBareJid().toString());
        intent.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(intent);

    }
}
