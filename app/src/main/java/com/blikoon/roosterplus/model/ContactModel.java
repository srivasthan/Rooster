package com.blikoon.roosterplus.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.blikoon.roosterplus.persistence.ContactCursorWrapper;
import com.blikoon.roosterplus.persistence.DatabaseBackend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gakwaya on 2018/1/2.
 */

public class ContactModel {

    private static final String LOGTAG = "ContactModel";
    private static ContactModel sContactMoel;
    private Context mContext;
    private SQLiteDatabase mDatabase;

    public static ContactModel get(Context context)
    {
        if(sContactMoel == null)
        {
            sContactMoel = new ContactModel(context);
        }
        return sContactMoel;
    }

    private ContactModel(Context context)
    {
        mContext = context;
        mDatabase = DatabaseBackend.getInstance(mContext).getWritableDatabase();
    }

    public List<Contact> getContacts()
    {
        List<Contact> contacts = new ArrayList<>();

        ContactCursorWrapper cursor = queryContacts(null,null);

        try
        {
            cursor.moveToFirst();
            while( !cursor.isAfterLast())
            {
                contacts.add(cursor.getContact());
                cursor.moveToNext();
            }

        }finally {
            cursor.close();
        }
        return contacts;
    }

    public Contact getContactByJidString(String jidString)
    {
        List<Contact> contacts = getContacts();
        List<String> stringJids = new ArrayList<>();

        Contact mContact = null;

        Log.d(LOGTAG,"Looping around contacts============");

        for(Contact contact :contacts)
        {
            Log.d(LOGTAG,"Contact Jid :"+contact.getJid());
            Log.d(LOGTAG,"Subscription type :"+ contact.getTypeStringValue(contact.getSubscriptionType()));
            if(contact.getJid().equals(jidString))
            {
                mContact = contact;
            }
        }
        return mContact;
    }

    public List<String> getContactsJidStrings()
    {
        List<Contact> contacts = getContacts();
        List<String> stringJids = new ArrayList<>();
        for(Contact contact :contacts)
        {
            Log.d(LOGTAG,"Contact Jid :"+contact.getJid());
            stringJids.add(contact.getJid());
        }
        return stringJids;
    }

    public boolean isContactStranger(String contactJid)
    {
        List<String> contacts = getContactsJidStrings();
        return !contacts.contains(contactJid);
    }

    private ContactCursorWrapper queryContacts(String whereClause , String [] whereArgs)
    {
        Cursor cursor = mDatabase.query(
                Contact.TABLE_NAME,
                null ,//Columns - null selects all columns
                whereClause,
                whereArgs,
                null ,//groupBy
                null, //having
                null//orderBy
        );
        return new ContactCursorWrapper(cursor);
    }


    public boolean addContact(Contact c)
    {
        //TODO: Check if contact already in db before adding.
        ContentValues values = c.getContentValues();
        if ((mDatabase.insert(Contact.TABLE_NAME, null, values)== -1))
        {
            return false;
        }else
        {
            return true;
        }
    }

    public boolean updateContactSubscription(Contact contact)
    {
        Contact mContact = contact;
        String jidString = contact.getJid();

        //Get new contentvalues to add to db
        ContentValues values = contact.getContentValues();
        //db.update returns the number of affected rows in the db, if this return value is not zero, we succeeded

        int rows = mDatabase.update(Contact.TABLE_NAME, values, "jid = ? ", new String[] { jidString } );
        Log.d(LOGTAG,rows + " rows affected in db");

        if( rows != 0)
        {
            Log.d(LOGTAG,"DB record update successful ");
            return true;
        }
        return false;
    }
    /** Subscription changes to FROM in the FROM direction */
    public void updateContactSubscriptionOnSendSubscribed(String contact)
    {
        //When we send a subscribed, the pending_from changes to from
        Contact mContact = getContactByJidString(contact);
        mContact.setPendingFrom(false);
        updateContactSubscription(mContact);
    }


    public boolean deleteContact(Contact c)
    {
        int uniqueId = c.getPersistID();
        return deleteContact(uniqueId);

    }

    public boolean deleteContact ( int uniqueId)
    {
        int value =mDatabase.delete(Contact.TABLE_NAME,Contact.Cols.CONTACT_UNIQUE_ID+"=?",new String[] {String.valueOf(uniqueId)});

        if(value == 1)
        {
            Log.d(LOGTAG, "Successfully deleted a record");
            return true;
        }else
        {
            Log.d(LOGTAG, "Could not delete record");
            return false;
        }

    }

}
