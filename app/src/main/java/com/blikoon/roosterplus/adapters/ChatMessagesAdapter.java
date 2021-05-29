package com.blikoon.roosterplus.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blikoon.roosterplus.BuildConfig;
import com.blikoon.roosterplus.R;
import com.blikoon.roosterplus.Utilities;
import com.blikoon.roosterplus.model.ChatMessage;
import com.blikoon.roosterplus.model.ChatMessagesModel;
import com.blikoon.roosterplus.model.ChatModel;
import com.blikoon.roosterplus.xmpp.RoosterConnection;
import com.blikoon.roosterplus.xmpp.RoosterConnectionService;

import java.io.File;
import java.util.List;

/**
 * Created by gakwaya on 2017/12/31.
 */

public class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessageViewHolder> {

    /* Interface to let the view recycler view know that an item has been added so it
     * can scroll down. */
    public interface OnInformRecyclerViewToScrollDownListener {
        public void onInformRecyclerViewToScrollDown(int size);
    }

    public interface OnItemLongClickListener {
        public void onItemLongClick(int uniqueId, View anchor);
    }

    private static final int SENT = 1;
    private static final int RECEIVED = 2;
    private static final int IMAGE_SENT = 3;
    private static final int IMAGE_RECEIVED = 4;
    private static final int AUDIO_SENT = 5;
    private static final int AUDIO_RECEIVED = 6;
    private static final int VIDEO_SENT = 7;
    private static final int VIDEO_RECEIVED = 8;
    private static final int PDF_SENT = 9;
    private static final int PDF_RECEIVED = 10;
    private static final int OFFICE_SENT = 11;
    private static final int OFFICE_RECEIVED = 12;
    private static final int OTHER_SENT = 13;
    private static final int OTHER_RECEIVED = 14;
    private static final String LOGTAG = "ChatMessageAdapter";

    private List<ChatMessage> mChatMessageList;
    private final LayoutInflater mLayoutInflater;
    private Context mContext;
    private final String contactJid;
    private OnInformRecyclerViewToScrollDownListener mOnInformRecyclerViewToScrollDownListener;
    private OnItemLongClickListener onItemLongClickListener;
    private boolean shouldScrollRecyclerViewDown;


    public void setmOnInformRecyclerViewToScrollDownListener(OnInformRecyclerViewToScrollDownListener mOnInformRecyclerViewToScrollDownListener) {
        this.mOnInformRecyclerViewToScrollDownListener = mOnInformRecyclerViewToScrollDownListener;
    }

    public OnItemLongClickListener getOnItemLongClickListener() {
        return onItemLongClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    public boolean shouldScrollRecyclerViewDown() {
        return shouldScrollRecyclerViewDown;
    }

    public void setShouldScrollRecyclerViewDown(boolean shouldScrollRecyclerViewDown) {
        this.shouldScrollRecyclerViewDown = shouldScrollRecyclerViewDown;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public ChatMessagesAdapter(Context context, String contactJid) {
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mContext = context;
        this.contactJid = contactJid;
        this.shouldScrollRecyclerViewDown = true;

        mChatMessageList = ChatMessagesModel.get(context).getMessages(contactJid);
        Log.d(LOGTAG, "Getting messages for :" + contactJid);


    }

    public void informRecyclerViewToScrollDown() {
        mOnInformRecyclerViewToScrollDownListener.onInformRecyclerViewToScrollDown(mChatMessageList.size());
    }

    @Override
    public ChatMessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case SENT:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_sent, parent, false);
                return new ChatMessageViewHolder(itemView, this);
            //----------------------------------------------------
            case IMAGE_SENT:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_image_sent, parent, false);
                return new ChatMessageViewHolder(itemView, this);

            case IMAGE_RECEIVED:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_image_received, parent, false);
                return new ChatMessageViewHolder(itemView, this);

            case AUDIO_SENT:

            case PDF_SENT:

            case OFFICE_SENT:
            case OTHER_SENT:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_file_sent, parent, false);
                return new ChatMessageViewHolder(itemView, this);

            case AUDIO_RECEIVED:

            case PDF_RECEIVED:
            case OFFICE_RECEIVED:
            case OTHER_RECEIVED:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_file_received, parent, false);
                return new ChatMessageViewHolder(itemView, this);

            case VIDEO_SENT:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_video_sent, parent, false);
                return new ChatMessageViewHolder(itemView, this);

            case VIDEO_RECEIVED:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_video_received, parent, false);
                return new ChatMessageViewHolder(itemView, this);

            //-------------------------------------------------------
            default:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_received, parent, false);
                return new ChatMessageViewHolder(itemView, this);
        }
    }

    @Override
    public void onBindViewHolder(ChatMessageViewHolder holder, int position) {
        ChatMessage chatMessage = mChatMessageList.get(position);
        holder.bindChat(chatMessage);

    }

    @Override
    public int getItemCount() {
        return mChatMessageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage.Type messageType = mChatMessageList.get(position).getType();

        if (messageType == ChatMessage.Type.SENT) {
            return SENT;
        }
        if (messageType == ChatMessage.Type.RECEIVED) {
            return RECEIVED;
        }
        if (messageType == ChatMessage.Type.IMAGE_SENT) {
            return IMAGE_SENT;
        }
        if (messageType == ChatMessage.Type.IMAGE_RECEIVED) {
            return IMAGE_RECEIVED;
        }
        if (messageType == ChatMessage.Type.AUDIO_SENT) {
            return AUDIO_SENT;
        }
        if (messageType == ChatMessage.Type.AUDIO_RECEIVED) {
            return AUDIO_RECEIVED;
        }
        if (messageType == ChatMessage.Type.VIDEO_SENT) {
            return VIDEO_SENT;
        }
        if (messageType == ChatMessage.Type.VIDEO_RECEIVED) {
            return VIDEO_RECEIVED;
        }
        if (messageType == ChatMessage.Type.PDF_SENT) {
            return PDF_SENT;
        }
        if (messageType == ChatMessage.Type.PDF_RECEIVED) {
            return PDF_RECEIVED;
        }
        if (messageType == ChatMessage.Type.OFFICE_SENT) {
            return OFFICE_SENT;
        }
        if (messageType == ChatMessage.Type.OFFICE_RECEIVED) {
            return OFFICE_RECEIVED;
        }
        if (messageType == ChatMessage.Type.OTHER_SENT) {
            return OTHER_SENT;
        }
        if (messageType == ChatMessage.Type.OTHER_RECEIVED) {
            return OTHER_RECEIVED;
        }
        return SENT;
    }

    public void onMessageAdd() {
        mChatMessageList = ChatMessagesModel.get(mContext).getMessages(contactJid);
        notifyDataSetChanged();
        informRecyclerViewToScrollDown();

    }
}

class ChatMessageViewHolder extends RecyclerView.ViewHolder {

    private static final String LOGTAG = "ChatMessageViewHolder";
    private final TextView mMessageBody;
    private final TextView mMessageTimestamp;
    private final ImageView profileImage;
    private ChatMessage mChatMessage;
    private final ChatMessagesAdapter mAdapter;
    private final ImageView videoPreviewImageView;
    private final TextView attachmentFileName;
    private final ImageView imageViewFileIcon;

    public ChatMessageViewHolder(final View itemView, final ChatMessagesAdapter mAdapter) {
        super(itemView);
        mMessageBody = (TextView) itemView.findViewById(R.id.text_message_body);
        mMessageTimestamp = (TextView) itemView.findViewById(R.id.text_message_timestamp);
        profileImage = (ImageView) itemView.findViewById(R.id.profile);
        videoPreviewImageView = (ImageView) itemView.findViewById(R.id.video_preview_image_view);
        attachmentFileName = (TextView) itemView.findViewById(R.id.attachmentFileName);
        imageViewFileIcon = (ImageView) itemView.findViewById(R.id.imageViewFileIcon);

        this.mAdapter = mAdapter;

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOGTAG, "User clicked on item");
                //We don't want to offer to open a file that doesn't exist. Bad user experience !
                File attachmentFile = new File(mChatMessage.getAttachmentPath());
                if (attachmentFile.exists()) {

                    if (mChatMessage.getType() == ChatMessage.Type.IMAGE_SENT || mChatMessage.getType() == ChatMessage.Type.IMAGE_RECEIVED ||
                            mChatMessage.getType() == ChatMessage.Type.VIDEO_SENT || mChatMessage.getType() == ChatMessage.Type.VIDEO_RECEIVED ||
                            mChatMessage.getType() == ChatMessage.Type.AUDIO_SENT || mChatMessage.getType() == ChatMessage.Type.AUDIO_RECEIVED ||
                            mChatMessage.getType() == ChatMessage.Type.OFFICE_SENT || mChatMessage.getType() == ChatMessage.Type.OFFICE_RECEIVED ||
                            mChatMessage.getType() == ChatMessage.Type.PDF_SENT || mChatMessage.getType() == ChatMessage.Type.PDF_RECEIVED ||
                            mChatMessage.getType() == ChatMessage.Type.OTHER_SENT || mChatMessage.getType() == ChatMessage.Type.OTHER_RECEIVED) {
                        //Give the user the ability to open the file
                        File file = new File(mChatMessage.getAttachmentPath());

                        Uri uri = FileProvider.getUriForFile(mAdapter.getContext(),
                                BuildConfig.APPLICATION_ID + ".files",
                                file);

                        Intent openIntent = new Intent(Intent.ACTION_VIEW);
                        String mime = Utilities.getMimeType(file.getAbsolutePath());
                        if (mime == null) {
                            mime = "*/*";
                        }

                        openIntent.setDataAndType(uri, mime);
                        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        PackageManager manager = mAdapter.getContext().getPackageManager();
                        List<ResolveInfo> info = manager.queryIntentActivities(openIntent, 0);
                        if (info.size() == 0) {
                            openIntent.setDataAndType(uri, "*/*");
                        }

                        try {
                            mAdapter.setShouldScrollRecyclerViewDown(false);
                            mAdapter.getContext().startActivity(openIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(mAdapter.getContext(), "No Application found to open file", Toast.LENGTH_SHORT).show();
                        }

                    }

                }

            }
        });

        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ChatMessagesAdapter.OnItemLongClickListener listener = mAdapter.getOnItemLongClickListener();
                if (listener != null) {
                    listener.onItemLongClick(mChatMessage.getPersistID(), itemView);
                }
                return false;
            }
        });
    }

    private void setSentMessageProfilePicture() {
        RoosterConnection rc = RoosterConnectionService.getConnection();
        if (rc != null) {
            String selfJid = PreferenceManager.getDefaultSharedPreferences(mAdapter.getContext())
                    .getString("xmpp_jid", null);

            if (selfJid != null) {
                Log.d(LOGTAG, "God a valid self jid : " + selfJid);
                String imageAbsPath = rc.getProfileImageAbsolutePath(selfJid);
                if (imageAbsPath != null) {
                    Drawable d = Drawable.createFromPath(imageAbsPath);
                    profileImage.setImageDrawable(d);
                }
            } else {
                Log.d(LOGTAG, "Could not get a valid self jid ");
            }
        }

    }

    private void setReceivedMessageProfilePicture() {
        RoosterConnection rc = RoosterConnectionService.getConnection();
        if (rc != null) {
            String imageAbsPath = rc.getProfileImageAbsolutePath(mChatMessage.getContactJid());
            if (imageAbsPath != null) {
                Drawable d = Drawable.createFromPath(imageAbsPath);
                profileImage.setImageDrawable(d);
            }

        }
    }


    public void bindChat(ChatMessage chatMessage) {
        mChatMessage = chatMessage;
        mMessageTimestamp.setText(Utilities.getFormattedTime(chatMessage.getTimestamp()));

        ChatMessage.Type type = mChatMessage.getType();

        if (type == ChatMessage.Type.RECEIVED) {
            mMessageBody.setText(chatMessage.getMessage());
            setReceivedMessageProfilePicture();
        }

        if (type == ChatMessage.Type.SENT) {
            mMessageBody.setText(chatMessage.getMessage());
            setSentMessageProfilePicture();
        }


        if (type == ChatMessage.Type.IMAGE_RECEIVED) {
            videoPreviewImageView.setImageResource(R.drawable.ic_profile);
            setReceivedMessageProfilePicture();
        }

        if (type == ChatMessage.Type.IMAGE_SENT) {
            videoPreviewImageView.setImageResource(R.drawable.ic_profile);
            setSentMessageProfilePicture();
        }

        if (type == ChatMessage.Type.VIDEO_RECEIVED) {
            videoPreviewImageView.setImageResource(R.drawable.ic_profile);
            setReceivedMessageProfilePicture();
        }

        if (type == ChatMessage.Type.VIDEO_SENT) {
            videoPreviewImageView.setImageResource(R.drawable.ic_profile);
            setSentMessageProfilePicture();
        }

        if (type == ChatMessage.Type.AUDIO_RECEIVED) {
            imageViewFileIcon.setImageResource(R.drawable.ic_picture_as_audio_48dp);
            setReceivedMessageProfilePicture();
        }

        if (type == ChatMessage.Type.AUDIO_SENT) {
            imageViewFileIcon.setImageResource(R.drawable.ic_picture_as_audio_48dp);
            setSentMessageProfilePicture();
        }

        if (type == ChatMessage.Type.OFFICE_RECEIVED) {
            imageViewFileIcon.setImageResource(R.drawable.ic_picture_as_document_48dp);
            setReceivedMessageProfilePicture();
        }

        if (type == ChatMessage.Type.OFFICE_SENT) {
            imageViewFileIcon.setImageResource(R.drawable.ic_picture_as_document_48dp);
            setSentMessageProfilePicture();
        }

        if (type == ChatMessage.Type.PDF_RECEIVED) {
            imageViewFileIcon.setImageResource(R.drawable.ic_picture_as_pdf_black_48dp);
            setReceivedMessageProfilePicture();
        }

        if (type == ChatMessage.Type.PDF_SENT) {
            imageViewFileIcon.setImageResource(R.drawable.ic_picture_as_pdf_black_48dp);
            setSentMessageProfilePicture();
        }

        if (type == ChatMessage.Type.OTHER_RECEIVED) {
            imageViewFileIcon.setImageResource(R.drawable.ic_picture_as_attachment_48dp);
            setReceivedMessageProfilePicture();
        }

        if (type == ChatMessage.Type.OTHER_SENT) {
            imageViewFileIcon.setImageResource(R.drawable.ic_picture_as_attachment_48dp);
            setSentMessageProfilePicture();
        }

        //For Videos extract the thumbnail to be shown
        if (type == ChatMessage.Type.VIDEO_SENT || type == ChatMessage.Type.VIDEO_RECEIVED) {
            File file = new File(mChatMessage.getAttachmentPath());
            if (!file.exists()) {
                itemView.setClickable(false);
                //Should find a file removed image to show here
                Log.d(LOGTAG, "Video File does not exist");
                videoPreviewImageView.setImageResource(R.drawable.ic_picture_as_file_deleted_48dp);
            } else {
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(mChatMessage.getAttachmentPath(),
                        MediaStore.Images.Thumbnails.MINI_KIND);
                videoPreviewImageView.setImageBitmap(thumbnail);
            }

        }

        //For images just show the image preview
        if (type == ChatMessage.Type.IMAGE_SENT || type == ChatMessage.Type.IMAGE_RECEIVED) {
            File file = new File(mChatMessage.getAttachmentPath());
            if (!file.exists()) {
                itemView.setClickable(false);
                //Should find a file removed image to show here
                Log.d(LOGTAG, "Image File does not exist");
                videoPreviewImageView.setImageResource(R.drawable.ic_picture_as_file_deleted_48dp);

            } else {
                Bitmap bitmap = BitmapFactory.decodeFile(mChatMessage.getAttachmentPath());
                videoPreviewImageView.setImageBitmap(bitmap);
            }
        }

        //For other files show the filename
        if (type == ChatMessage.Type.AUDIO_SENT || type == ChatMessage.Type.AUDIO_RECEIVED ||
                type == ChatMessage.Type.OFFICE_SENT || type == ChatMessage.Type.OFFICE_RECEIVED ||
                type == ChatMessage.Type.PDF_SENT || type == ChatMessage.Type.PDF_RECEIVED ||
                type == ChatMessage.Type.OTHER_SENT || type == ChatMessage.Type.OTHER_RECEIVED) {
            File file = new File(mChatMessage.getAttachmentPath());
            if (file.exists()) {
                attachmentFileName.setText(file.getName());

            } else {
                attachmentFileName.setText("File has been deleted");

            }

        }


    }
}
