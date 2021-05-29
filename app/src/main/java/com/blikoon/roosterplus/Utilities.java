package com.blikoon.roosterplus;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateFormat;

import com.blikoon.roosterplus.model.ChatMessage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by gakwaya on 2018/1/12.
 */

public class Utilities {

    //Check if service is running.
    public static boolean isServiceRunning(Class<?> serviceClass ,Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static String getFormattedTime(long timestamp){

        long oneDayInMillis = TimeUnit.DAYS.toMillis(1); // 24 * 60 * 60 * 1000;

        long timeDifference = System.currentTimeMillis() - timestamp;

        return timeDifference < oneDayInMillis
                ? DateFormat.format("hh:mm a", timestamp).toString()
                : DateFormat.format("dd MMM - hh:mm a", timestamp).toString();
    }

    /**
     * @param fileFullPath : Full path for the file you are checking for
     * @param send : true if you are testing for sent files and false if  you are checking for received files.
     * @return ChatMessage type based on the file extension of the file passed in.
     */
    public static ChatMessage.Type getMessageTypefromFileFullPath(String fileFullPath , boolean send)
    {
        int index = fileFullPath.lastIndexOf(".");

        if(index == -1)
        {
            if( send)
                return ChatMessage.Type.OTHER_SENT;
            else
                return ChatMessage.Type.OTHER_RECEIVED;
        }

        String extension =  fileFullPath.substring(index);

        if( extension.equals(".jpg")  || extension.equals(".png") || extension.equals(".jpeg"))
        {
            if(send)
                return ChatMessage.Type.IMAGE_SENT;
            else
                return ChatMessage.Type.IMAGE_RECEIVED;
        }else if( extension.equals(".mp4") || extension.equals(".mpg") ||
                extension == ".mov" || extension == ".avi")
        {
            if(send)
                return  ChatMessage.Type.VIDEO_SENT;
            else
                return ChatMessage.Type.VIDEO_RECEIVED;
        }else if(extension.equals(".doc") || extension.equals(".docx") || extension.equals(".ppt") || extension.equals(".pptx") ||
                extension.equals(".xls") || extension.equals(".xlsx"))
        {
            if(send)
                return  ChatMessage.Type.OFFICE_SENT;
            else
                return ChatMessage.Type.OFFICE_RECEIVED;
        }else if(extension.equals(".mp3") || extension.equals(".wav"))
        {
            if(send)
                return  ChatMessage.Type.AUDIO_SENT;
            else
                return ChatMessage.Type.AUDIO_RECEIVED;
        }else if(extension.equals(".pdf")) {
            if(send)
                return  ChatMessage.Type.PDF_SENT;
            else
                return ChatMessage.Type.PDF_RECEIVED;
        }else
        {
            if(send)
                return  ChatMessage.Type.OTHER_SENT;
            else
                return ChatMessage.Type.OTHER_RECEIVED;
        }
    }

    /**
     *
     * @param messageBody  The input to the method to be checked to see if it is a link to a file sent by a contact.
     *                     we first check to see if it is a valid URL and then eliminate the cases where the url points to
     *                     a .html,.php,.jsp page [other web page technologies may be added later]. The user doesn't want
     *                     to download these, we instead offer them in the UI to be clicked and opened in the browser.
     * @return  true if the link points to a file that the the user can download, false otherwise.
     */

    public static boolean isStringFileUrl(String  messageBody)
    {
        URL url;
        try {
            url = new URL(messageBody);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }

        String extension = MimeUtils.extractRelevantExtension(url);

        if(extension!=null)
        {
            if(extension.equals("html") || extension.equals("php") || extension.equals("jsp")
                    || extension.equals("htm"))
            {
                return false;
            }
            return true;
        }else
        {
            return false;
        }

    }

    public static String getMimeType (String path) {

        int start = path.lastIndexOf('.') + 1;
        if (start < path.length()) {
            String mime = MimeUtils.guessMimeTypeFromExtension(path.substring(start));
            return mime == null ? null : mime;
        } else {
            return null;
        }
    }
}
