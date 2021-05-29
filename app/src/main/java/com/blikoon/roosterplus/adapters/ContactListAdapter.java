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
import com.blikoon.roosterplus.model.Chat;
import com.blikoon.roosterplus.model.Contact;
import com.blikoon.roosterplus.model.ContactModel;
import com.blikoon.roosterplus.xmpp.RoosterConnection;
import com.blikoon.roosterplus.xmpp.RoosterConnectionService;

import java.util.List;

/**
 * Created by gakwaya on 2018/1/2.
 */

public class ContactListAdapter extends RecyclerView.Adapter<ContactHolder> {

    public interface OnItemClickListener {
        public void onItemClick(String contactJid );
    }

    public interface OnItemLongClickListener{
        public void onItemLongClick(int uniqueId, String contactJid ,View anchor);
    }

    private List<Contact> mContacts;
    private Context mContext;
    private static final String LOGTAG = "ContactListAdapter";
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    public ContactListAdapter(Context context)
    {
        mContacts = ContactModel.get(context).getContacts();
        mContext = context;
        Log.d(LOGTAG,"Constructor of ChatListAdapter , the size of the backing list is :" +mContacts.size());
    }

    public OnItemClickListener getmOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setmOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public OnItemLongClickListener getmOnItemLongClickListener() {
        return mOnItemLongClickListener;
    }

    public void setmOnItemLongClickListener(OnItemLongClickListener mOnItemLongClickListener) {
        this.mOnItemLongClickListener = mOnItemLongClickListener;
    }

    @Override
    public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater
                .inflate(R.layout.contact_list_item, parent,
                        false);
        return new ContactHolder(view,this);
    }

    @Override
    public void onBindViewHolder(ContactHolder holder, int position) {
        Contact contact = mContacts.get(position);
        holder.bindContact(contact);

    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    public void onContactCountChange() {
        mContacts = ContactModel.get(mContext).getContacts();
        notifyDataSetChanged();
        Log.d(LOGTAG, "ContactListAdapter knows of the change in messages");
    }
}

class ContactHolder extends RecyclerView.ViewHolder {

    private TextView jidTexView;
    private TextView subscriptionTypeTextView;
    private Contact mContact;
    private ImageView profile_image;
    private ContactListAdapter mAdapter;
    private static final String LOGTAG = "ContactHolder";

    public ContactHolder(final  View itemView , ContactListAdapter adapter) {
        super(itemView);
        mAdapter = adapter;
        jidTexView = (TextView) itemView.findViewById(R.id.contact_jid_string);
        subscriptionTypeTextView = (TextView) itemView.findViewById(R.id.suscription_type);
        profile_image = (ImageView) itemView.findViewById(R.id.profile_contact);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOGTAG,"User clicked on Contact Item");
                ContactListAdapter.OnItemClickListener listener = mAdapter.getmOnItemClickListener();
                if ( listener != null)
                {
                    Log.d(LOGTAG,"Calling the listener method");

                    listener.onItemClick(jidTexView.getText().toString());
                }
            }
        });


        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ContactListAdapter.OnItemLongClickListener listener = mAdapter.getmOnItemLongClickListener();
                if ( listener != null)
                {
                    mAdapter.getmOnItemLongClickListener().onItemLongClick(mContact.getPersistID(),mContact.getJid(),itemView);
                    return true;
                }
                return false;
            }
        });
    }


    void bindContact(Contact c)
    {
        mContact = c;
        if ( mContact == null)
        {
            return;
        }
        jidTexView.setText(mContact.getJid());
        subscriptionTypeTextView.setText(mContact.getTypeStringValue(mContact.getSubscriptionType()));
        profile_image.setImageResource(R.drawable.ic_profile);

        RoosterConnection rc = RoosterConnectionService.getConnection();
        if(rc != null)
        {
            String imageAbsPath = rc.getProfileImageAbsolutePath(mContact.getJid());
            if ( imageAbsPath != null)
            {
                Drawable d = Drawable.createFromPath(imageAbsPath);
                profile_image.setImageDrawable(d);
            }
        }

    }
}
