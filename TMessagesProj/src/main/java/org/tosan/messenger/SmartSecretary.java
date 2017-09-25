package org.tosan.messenger;


import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.query.MessagesQuery;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class SmartSecretary implements NotificationCenter.NotificationCenterDelegate{

    public CharSequence replyMessage="";

    public SmartSecretary() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.tosanConfigsChanged);
        if(Tosan.prefs.getBoolean(Tosan.key_secretary, false))
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.didReceivedNewMessages);

        setMessage(Tosan.prefs.getString(Tosan.key_secretary_message, ""));
    }


    public void setMessage(CharSequence text){
        replyMessage = AndroidUtilities.getTrimmedString(text);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if(id == NotificationCenter.didReceivedNewMessages){
            if(ConnectionsManager.getInstance().getConnectionState() == ConnectionsManager.ConnectionStateUpdating)
                return;

            ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
            if(arr==null || arr.size()!=1)
                return;
            long did = (Long) args[0];
            TLRPC.User user=MessagesController.getInstance().getUser((int) did);
            if(user!=null && !user.bot) {
                // only work for users.
                CharSequence text = replyMessage;
                MessageObject mo = arr.get(0);
                if (!mo.isOut() && mo.messageOwner.action == null && !mo.messageOwner.silent) {
                    if (text.length() != 0) {
                        int count = (int) Math.ceil(text.length() / 4096.0f);
                        for (int a = 0; a < count; a++) {
                            CharSequence[] message = new CharSequence[]{text.subSequence(a * 4096, Math.min((a + 1) * 4096, text.length()))};
                            ArrayList<TLRPC.MessageEntity> entities = MessagesQuery.getEntities(message);
                            SendMessagesHelper.getInstance().sendMessage(message[0].toString(), did, null, null, true, entities, null, null);
                        }
                    }
                }
            }
        }else if(id == NotificationCenter.tosanConfigsChanged){
            // check secretary is enabled or not
            if(Tosan.key_secretary.equals(args[0])){
                boolean enabled = (boolean) args[1];
                if(enabled){
                    setMessage(Tosan.prefs.getString(Tosan.key_secretary_message, ""));
                    NotificationCenter.getInstance().addObserver(this, NotificationCenter.didReceivedNewMessages);
                }else{
                    NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didReceivedNewMessages);
                }
            }
        }

    }
}
