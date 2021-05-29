package com.blikoon.roosterplus.model;

import android.content.ContentValues;
import android.text.format.DateFormat;

import java.util.concurrent.TimeUnit;

/**
 * Created by gakwaya on 2017/12/31.
 */

public class ChatMessage {
    private String message;
    private long timestamp;
    private Type type;
    private String contactJid;
    private int persistID;
    private SentStatus sentStatus;
    private String attachmentPath;

    public static final String TABLE_NAME = "chatMessages";

    public static final class Cols
    {
        public static final String CHAT_MESSAGE_UNIQUE_ID = "chatMessageUniqueId";
        public static final String MESSAGE = "message";
        public static final String TIMESTAMP = "timestamp";
        public static final String MESSAGE_TYPE = "messageType";
        public static final String CONTACT_JID = "contactjid";
        public static final String SENT_STATUS = "sentStatus";
        public static final String ATTACHMENT_PATH= "attachmentPath";
    }


    public ChatMessage(String message, long timestamp, Type type , String contactJid){
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.contactJid = contactJid;
        this.sentStatus = SentStatus.NONE;
        this.attachmentPath = "NONE";

    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Type getType() {
        return type;
    }

    public String getContactJid() {
        return contactJid;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setContactJid(String contactJid) {
        this.contactJid = contactJid;
    }

    public int getPersistID() {
        return persistID;
    }

    public void setPersistID(int persistID) {
        this.persistID = persistID;
    }

    public SentStatus getSentStatus() {
        return sentStatus;
    }

    public void setSentStatus(SentStatus sentStatus) {
        this.sentStatus = sentStatus;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getTypeStringValue(Type type)
    {
        if(type==Type.SENT)
            return "SENT";
        else if( type == Type.RECEIVED)
            return "RECEIVED";
        else if( type == Type.IMAGE_SENT)
            return "IMAGE_SENT";
        else if( type == Type.IMAGE_RECEIVED)
            return "IMAGE_RECEIVED";
        else if( type == Type.AUDIO_SENT)
            return "AUDIO_SENT";
        else if( type == Type.AUDIO_RECEIVED)
            return "AUDIO_RECEIVED";
        else if( type == Type.VIDEO_SENT)
            return "VIDEO_SENT";
        else if( type == Type.VIDEO_RECEIVED)
            return "VIDEO_RECEIVED";
        else if( type == Type.PDF_SENT)
            return "PDF_SENT";
        else if( type == Type.PDF_RECEIVED)
            return "PDF_RECEIVED";
        else if( type == Type.OFFICE_SENT)
            return "OFFICE_SENT";
        else if( type == Type.OFFICE_RECEIVED)
            return "OFFICE_RECEIVED";
        else if( type == Type.OTHER_SENT)
            return "OTHER_SENT";
        else
            return "OTHER_RECEIVED";
    }

    public String getSentStatusStringValue(SentStatus sentStatus)
    {
        switch (sentStatus)
        {
            case NONE:
                return "NONE";
            case SENDING:
                return "SENDING";
            case SENT:
                return "SENT";
            case FAILED:
                return "FAILED";
            case RECEIVED:
                return "RECEIVED";
        }
        return "INDETERMINATE";
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        values.put(Cols.MESSAGE, message);
        values.put(Cols.TIMESTAMP, timestamp);
        values.put(Cols.MESSAGE_TYPE,getTypeStringValue(type));
        values.put(Cols.CONTACT_JID,contactJid);
        values.put(Cols.SENT_STATUS,getSentStatusStringValue(sentStatus));
        values.put(Cols.ATTACHMENT_PATH,attachmentPath);

        return values;
    }

    /*
    * This enum is used to keep track of the status of sent messages. When the user selects
    * a file from the device and confirms to send, the file goes in the SENDING status,when
    * we get the slot successfully and send the message to the other party the message changes
    * to the SENT status. If something has gone wrong in the process, we flag the message
    * as FAILED.NONE is the default message status that is used for simple text messages.
    * RECEIVED flags received messages.
    * */
    public enum SentStatus
    {
        NONE,SENDING,SENT,RECEIVED,FAILED
    }

    public enum  Type {
        SENT,RECEIVED,
        IMAGE_SENT,IMAGE_RECEIVED,
        AUDIO_SENT,AUDIO_RECEIVED,
        VIDEO_SENT,VIDEO_RECEIVED,
        PDF_SENT,PDF_RECEIVED,
        OFFICE_SENT,OFFICE_RECEIVED,
        OTHER_SENT,OTHER_RECEIVED
    }


}
