package com.blikoon.roosterplus.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blikoon.roosterplus.R;
import com.blikoon.roosterplus.Utilities;
import com.blikoon.roosterplus.model.Chat;
import com.blikoon.roosterplus.model.ChatModel;
import com.blikoon.roosterplus.xmpp.RoosterConnection;
import com.blikoon.roosterplus.xmpp.RoosterConnectionService;

import java.util.List;

/**
 * Created by gakwaya on 2017/12/29.
 */

public class ChatListAdapter extends RecyclerView.Adapter<ChatHolder> {

    private static final String LOGTAG ="ChatListAdapter";

    public interface OnItemClickListener {
        public void onItemClick(String contactJid,Chat.ContactType chatType);
    }

    public interface OnItemLongClickListener{
        public void onItemLongClick(String contactJid,int chatUniqueId ,View anchor);
    }



    List<Chat> chatList;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private Context mContext;

    public ChatListAdapter(Context context) {
        this.chatList = ChatModel.get(context).getChats();
        this.mContext = context;
    }

    public OnItemClickListener getmOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setmOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public OnItemLongClickListener getOnItemLongClickListener() {
        return onItemLongClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @Override
    public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater
                .inflate(R.layout.chat_list_item, parent,
                        false);
        return new ChatHolder(view,this);

    }

    @Override
    public void onBindViewHolder(ChatHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.bindChat(chat);

    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public void onChatCountChange() {
        chatList = ChatModel.get(mContext).getChats();
        notifyDataSetChanged();
        Log.d(LOGTAG, "ChatListAdapter knows of the change in messages");
    }
}


class ChatHolder extends RecyclerView.ViewHolder{

    private static final String LOGTAG = "ChatHolder";
    private TextView contactTextView;
    private TextView messageAbstractTextView;
    private TextView timestampTextView;
    private ImageView profileImage;
    private Chat mChat;
    private ChatListAdapter mChatListAdapter;


    public ChatHolder(final  View itemView ,ChatListAdapter adapter) {
        super(itemView);

        contactTextView = (TextView) itemView.findViewById(R.id.contact_jid);
        messageAbstractTextView = (TextView) itemView.findViewById(R.id.message_abstract);
        timestampTextView = (TextView) itemView.findViewById(R.id.text_message_timestamp);
        profileImage = (ImageView) itemView.findViewById(R.id.profile);
        mChatListAdapter = adapter;

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ChatListAdapter.OnItemClickListener listener = mChatListAdapter.getmOnItemClickListener();

                if ( listener!= null)
                {
                    listener.onItemClick(contactTextView.getText().toString(),mChat.getContactType());

                }

                Log.d(LOGTAG,"Clicked on the item in the recyclerView");

            }
        });

        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ChatListAdapter.OnItemLongClickListener listener = mChatListAdapter.getOnItemLongClickListener();
                if(listener != null)
                {
                    listener.onItemLongClick(mChat.getJid(),mChat.getPersistID(),itemView);
                    return true;
                }
                return false;
            }
        });
    }

    public void bindChat(Chat chat)
    {
        mChat = chat;
        contactTextView.setText(chat.getJid());
        timestampTextView.setText(Utilities.getFormattedTime(mChat.getLastMessageTimeStamp()));


        String lastMessage = mChat.getLastMessage();
        if( Utilities.isStringFileUrl(lastMessage))
        {
            messageAbstractTextView.setText("[File]");
        }else
        {
            messageAbstractTextView.setText(mChat.getLastMessage());

        }

        profileImage.setImageResource(R.drawable.ic_profile);

        RoosterConnection rc = RoosterConnectionService.getConnection();
        if(rc != null)
        {
            String imageAbsPath = rc.getProfileImageAbsolutePath(mChat.getJid());
            if ( imageAbsPath != null)
            {
                Drawable d = Drawable.createFromPath(imageAbsPath);
                profileImage.setImageDrawable(d);
            }
        }
    }


}
