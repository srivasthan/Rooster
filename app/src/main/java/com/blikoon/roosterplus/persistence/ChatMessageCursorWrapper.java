package com.blikoon.roosterplus.persistence;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.blikoon.roosterplus.model.ChatMessage;

/**
 * Created by gakwaya on 2018/1/20.
 */

public class ChatMessageCursorWrapper extends CursorWrapper {
    /**
     * Creates a cursor wrapper.
     *
     * @param cursor The underlying cursor to wrap.
     */
    public ChatMessageCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public ChatMessage getChatMessage()
    {
        String message = getString(getColumnIndex(ChatMessage.Cols.MESSAGE));
        long timestamp = getLong(getColumnIndex(ChatMessage.Cols.TIMESTAMP));
        String messageType = getString(getColumnIndex(ChatMessage.Cols.MESSAGE_TYPE));
        String counterpartJid = getString(getColumnIndex(ChatMessage.Cols.CONTACT_JID));
        int uniqueId = getInt(getColumnIndex(ChatMessage.Cols.CHAT_MESSAGE_UNIQUE_ID));

        String sentStatusString = getString(getColumnIndex(ChatMessage.Cols.SENT_STATUS));
        String attachmentPath = getString(getColumnIndex(ChatMessage.Cols.ATTACHMENT_PATH));

        ChatMessage.Type chatMessageType = null;
        ChatMessage.SentStatus sentStatus = null;

        //Retrieve the message sent status

        if( sentStatusString.equals("NONE"))
        {
            sentStatus = ChatMessage.SentStatus.NONE;
        }else if( sentStatusString.equals("SENDING"))
        {
            sentStatus = ChatMessage.SentStatus.SENDING;
        }else if ( sentStatusString.equals("SENT"))
        {
            sentStatus = ChatMessage.SentStatus.SENT;
        }else if ( sentStatusString.equals("FAILED"))
        {
            sentStatus = ChatMessage.SentStatus.FAILED;
        }else if( sentStatusString.equals("RECEIVED"))
        {
            sentStatus = ChatMessage.SentStatus.RECEIVED;
        }



        //Retrieve the message type
        if( messageType.equals("SENT"))
        {
            chatMessageType = ChatMessage.Type.SENT;
        }else if(messageType.equals("RECEIVED"))
        {
            chatMessageType = ChatMessage.Type.RECEIVED;
        }
        else if(messageType.equals("IMAGE_SENT"))
        {
            chatMessageType = ChatMessage.Type.IMAGE_SENT;
        }
        else if(messageType.equals("IMAGE_RECEIVED"))
        {
            chatMessageType = ChatMessage.Type.IMAGE_RECEIVED;
        }
        else if(messageType.equals("AUDIO_SENT"))
        {
            chatMessageType = ChatMessage.Type.AUDIO_SENT;
        }
        else if(messageType.equals("AUDIO_RECEIVED"))
        {
            chatMessageType = ChatMessage.Type.AUDIO_RECEIVED;
        }
        else if(messageType.equals("VIDEO_SENT"))
        {
            chatMessageType = ChatMessage.Type.VIDEO_SENT;
        }
        else if(messageType.equals("VIDEO_RECEIVED"))
        {
            chatMessageType = ChatMessage.Type.VIDEO_RECEIVED;
        }
        else if(messageType.equals("PDF_SENT"))
        {
            chatMessageType = ChatMessage.Type.PDF_SENT;
        }
        else if(messageType.equals("PDF_RECEIVED"))
        {
            chatMessageType = ChatMessage.Type.PDF_RECEIVED;
        }
        else if(messageType.equals("OFFICE_SENT"))
        {
            chatMessageType = ChatMessage.Type.OFFICE_SENT;
        }
        else if(messageType.equals("OFFICE_RECEIVED"))
        {
            chatMessageType = ChatMessage.Type.OFFICE_RECEIVED;
        }
        else if(messageType.equals("OTHER_SENT"))
        {
            chatMessageType = ChatMessage.Type.OTHER_SENT;
        }
        else
        {
            chatMessageType = ChatMessage.Type.OTHER_RECEIVED;
        }


        ChatMessage chatMessage = new ChatMessage(message,timestamp,chatMessageType,counterpartJid);
        chatMessage.setPersistID(uniqueId);
        chatMessage.setAttachmentPath(attachmentPath);
        chatMessage.setSentStatus(sentStatus);

        return  chatMessage;
    }
}
