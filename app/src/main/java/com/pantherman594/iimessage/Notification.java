package com.pantherman594.iimessage;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by david on 4/17.
 *
 * @author david
 */
class Notification {
    private static iiMessageApplication pma = iiMessageApplication.getInstance();
    private static ArrayList<String> notifIds = new ArrayList<>();

    @TargetApi(Build.VERSION_CODES.M)
    static synchronized void updateNotification(String chatId) {
        Chat chat = iiMessageApplication.getInstance().getChats().get(chatId);
        if (chat.getNumUnread() == 0) return;

        if (!notifIds.contains(chatId)) {
            notifIds.add(chatId);
        }
        int notifId = notifIds.indexOf(chatId);

        Intent resultIntent = new Intent(pma, MessageActivity.class);
        resultIntent.putExtra(Constants.Col.CHAT_ID, chatId);
        resultIntent.putExtra(Constants.Col.CHAT_NAME, chat.getName());
        resultIntent.putExtra(Constants.Col.ID, notifId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(pma);

        stackBuilder.addParentStack(MessageActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        String replyLabel = pma.getResources().getString(R.string.reply_label);
        Log.i("Notification", replyLabel);
        RemoteInput remoteInput = new RemoteInput.Builder(Constants.KEY_TEXT_REPLY)
                .setLabel(replyLabel)
                .build();

        android.app.Notification.Action action = new android.app.Notification.Action.Builder(Icon.createWithResource("", R.mipmap.notif_icon), replyLabel, resultPendingIntent)
                .addRemoteInput(remoteInput)
                .build();

        android.app.Notification.InboxStyle inboxStyle = new android.app.Notification.InboxStyle();

        for (Message message : chat.getUnreadMessages()) {
            if (message.getSender().equals(chat.getName())) inboxStyle.addLine(message.getMsg());
            else inboxStyle.addLine(message.getSender() + ": " + message.getMsg());
        }

        android.app.Notification.Builder mBuilder = new android.app.Notification.Builder(pma)
                .setSmallIcon(R.mipmap.notif_icon)
                .setContentTitle(chat.getName())
                .setContentText(chat.getNumUnread() + " messages")
                .setContentIntent(resultPendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .addAction(action)
                .setStyle(inboxStyle);

        NotificationManager mNotificationManager = (NotificationManager) pma.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(notifId, mBuilder.build());
    }
}
