package org.tosan.messenger;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import com.onesignal.OneSignal;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.tosan.messenger.sql.CatModel;
import org.tosan.messenger.sql.ContactChangeModel;
import org.tosan.messenger.sql.ContactModel;
import org.tosan.messenger.sql.DialogModel;

import java.util.ArrayList;
import java.util.Arrays;

import co.uk.rushorm.android.AndroidInitializeConfig;
import co.uk.rushorm.android.AndroidRushConfig;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushConfig;
import co.uk.rushorm.core.RushCore;


public class Tosan {

    public static final
            String key_enable_tabs="e_tabs",
            key_show_share_tabs="e_share_tabs",
            key_hidden_passtype="h_passtype",
            key_hidden_enabled="h_enabled",
            key_hidden_passcode="h_passcode",
            key_disable_innervp="disable_dp",
            key_ghost_enabled="enable_ghost",
            key_hide_typing="hide_typing",
            key_last_tab_index="hide_typing",
            key_avoid_send_read ="send_read",
            key_use_persian_calendar ="persian_calendar",
            key_download_path="dl_path",
            key_hide_phone="hide_phone",
            keY_real_members_count="real_mem_count",
            key_secretary="smart_secretary",
            key_secretary_message="smart_secretary_message",
            key_document_confirm_send="gs_confirm_send";

    public static SharedPreferences prefs;
//    public static boolean ghostModeEnabled;

    public static void init(final Context context){
        prefs=context.getSharedPreferences("parsgram-prefs", Context.MODE_PRIVATE);
//        ghostModeEnabled=prefs.getBoolean("ghost-mode", false);
        OneSignal.startInit(context)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
        AndroidInitializeConfig config=new AndroidInitializeConfig(context){
            RushConfig config;
            @Override
            public RushConfig getRushConfig() {
                if(config==null){
                    ArrayList<String> packRoots=new ArrayList<>();
                    packRoots.add("co.uk.rushorm");
                    packRoots.add("com.tosan.sql");
                    config=new AndroidRushConfig(context, packRoots){
                        @Override
                        public String dbName() {
                            return "chitchat.db";
                        }

                        @Override
                        public int dbVersion() {
                            return 7;
                        }

                        @Override
                        public boolean log() {
                            return false;
                        }

                        @Override
                        public boolean inDebug() {
                            return false;
                        }
                    };
                }
                return config;
            }
        };
        //AndroidInitializeConfig config=new AndroidInitializeConfig(context);
        //config.addPackage("com.towsan.applock.db");
        config.setClasses(Arrays.<Class<? extends Rush>>asList(DialogModel.class, CatModel.class, ContactChangeModel.class, ContactModel.class));
        RushCore.initialize(config);
    }

    public static boolean isOnline(TLRPC.User user){
        int currentTime = ConnectionsManager.getInstance().getCurrentTime();
        return user != null && user.status != null && (user.status.expires > currentTime ||
                user.id == UserConfig.getClientUserId()) && user.status.expires > 10000;
        //String status= LocaleController.formatUserStatus(user);
        //return status.equals(LocaleController.getString("Online", R.string.Online));
    }
   /* public static boolean isLongTimeAgo(TLRPC.User user){
        String status= LocaleController.formatUserStatus(user);
        return status.equals(LocaleController.getString("ALongTimeAgo", R.string.ALongTimeAgo));
    }*/


   public static void showMediaSendConfirmation(Context context, final Runnable onConfirm){
       if(!prefs.getBoolean(key_document_confirm_send, false)){
           onConfirm.run();
       }else{
           new AlertDialog.Builder(context)
                   .setTitle("پارس گرام")
                   .setMessage("آیا از ارسال این رسانه اطمینان دارید؟")
                   .setNegativeButton("خیر", null)
                   .setPositiveButton("بلی", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                           onConfirm.run();
                       }
                   })
                   .show();
       }
   }

    public static boolean isChannel(TLRPC.Chat chat){
        return chat.id < 0 || ChatObject.isChannel(chat) && !chat.megagroup;
    }
    public static boolean isGroup(TLRPC.Chat chat){
        return !isChannel(chat);
    }

    public static ArrayList<TLRPC.TL_dialog> getDialogsArray(int dialogsType, boolean hiddenDialogs) {
        if(hiddenDialogs)
            return MessagesController.getInstance().tabsController.hiddens;
        if (dialogsType == 0) {
            return MessagesController.getInstance().tabsController.all;
        } else if (dialogsType == 1) {
            return MessagesController.getInstance().dialogsServerOnly;
        } else if (dialogsType == 2) {
            return MessagesController.getInstance().dialogsGroupsOnly;
        }
        return MessagesController.getInstance().tabsController.getDialogs(dialogsType-3);
    }


    public static boolean isHiddenDialogsPasswordSet(){
        return prefs.getString(key_hidden_passcode, null)!=null;
    }


}
