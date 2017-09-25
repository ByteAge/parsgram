package org.tosan.messenger.sql;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

import co.uk.rushorm.core.RushSearch;

public class UsersChangeUpdater {

    private static boolean isUpdating=false;

    public static void updateChanges(){
        if(isUpdating)
            return;
        isUpdating=true;
        new Thread(){
            @Override
            public void run() {
                ContactsController.getInstance().readContacts();

//                RushCore.getInstance().deleteAll(ContactModel.class);
//                RushCore.getInstance().deleteAll(ContactChangeModel.class);

//                Log.v("changes", "on read contacts");
//                Log.v("changes", "usersSectsDictsSize "+ContactsController.getInstance().usersSectionsDict.keySet().size());
                for (String str : ContactsController.getInstance().usersSectionsDict.keySet()) {
//                    Log.v("changes", "dict "+str);
                    for (Object o : ((ArrayList) ContactsController.getInstance().usersSectionsDict.get(str))) {
                        TLRPC.TL_contact tL_contact = (TLRPC.TL_contact) o;
                        TLRPC.User user = MessagesController.getInstance().getUser(tL_contact.user_id);
                        if (user == null){
//                            Log.v("changes", "user null for "+tL_contact.user_id);
                            continue;
                        }

                        ContactModel model = new RushSearch().whereEqual("uid", user.id).findSingle(ContactModel.class);
//                        Log.v("changes", "mdoel found for "+model);
                        if (model == null) {
                            model = new ContactModel();
                            model.uid = user.id;
                            model.phone = user.phone;
                            model.displayName=UserObject.getUserName(user);
                            model.username = user.username;
                            model.updatedAt= System.currentTimeMillis();
                            if (user.photo != null)
                                model.pic = user.photo.photo_id + "";
                            if (user.status != null) {
                                String st = String.valueOf(user.status);
                                model.status = st.substring(0, st.indexOf("@"));
                            }
                            model.save();
//                            Log.v("changes", "model saved");
                            continue;
                        }
                        List<ContactChangeModel> changes = model.contactChanges;
                        if (changes == null)
                            changes = new ArrayList<>();
                        model.contactChanges = changes;
                        model.updatedAt= System.currentTimeMillis();
                        if (user.username != null) {
                            if (model.username == null || !model.username.equals(user.username)) {
                                ContactChangeModel change = new ContactChangeModel();
                                changes.add(0, change);
                                change.userId = user.id;
                                change.type = ContactChangeModel.USERNAME;
                                change.username = user.username;
                                model.username = user.username;
//                                Log.v("changes", "username updated");
                            }
                        }
                        if (user.photo != null) {
                            if (model.pic == null || !model.pic.equals(user.photo.photo_id + "")) {
                                if(user.photo.photo_small!=null || user.photo.photo_big!=null){
                                    ContactChangeModel change = new ContactChangeModel();
                                    changes.add(0, change);
                                    change.userId = user.id;
                                    change.type = ContactChangeModel.PHOTO;

                                    if(user.photo.photo_small!=null)
                                        change.photoSmall=ContactChangeModel.fileLocationToString(user.photo.photo_small);
                                    if(user.photo.photo_big!=null)
                                        change.photoBig=ContactChangeModel.fileLocationToString(user.photo.photo_big);
                                    change.save();
                                    model.pic = user.photo.photo_id + "";
//                                    Log.v("changes", "photo updated");
                                }
                                model.pic = user.photo.photo_id + "";
                            }
                        }
                        if (user.phone != null) {
                            if (model.phone == null || !model.phone.equals(user.phone)) {
                                ContactChangeModel change = new ContactChangeModel();
                                changes.add(0, change);
                                change.userId = user.id;
                                change.type = ContactChangeModel.PHONE;
                                change.phone = user.phone;
                                change.save();
                                model.phone = user.phone;
//                                Log.v("changes", "phone updated");
                            }
                        }

                        if (user.status != null) {
                            String st = String.valueOf(user.status);
                            st = st.substring(0, st.indexOf("@"));
                            if (model.status == null || !model.status.equals(st)) {
//                                ContactChangeModel change = new ContactChangeModel();
//                                changes.add(change);
//                                change.userId = user.id;
//                                change.type = ContactChangeModel.STATUS;
//                                change.status = st;
//                                change.save();
                                model.status = st;
//                                Log.v("changes", "status updated. "+st);
                            }
                        }
//                        Log.v("changes", "model witch children saved. "+changes.size());
                        model.save();
                    }
                }
//                Log.v("changes", "models size "+new RushSearch().find(ContactModel.class).size());
                isUpdating=false;
            }
        }.start();
    }

    public static void processUpdate(TLRPC.Update update){
        if(update==null)
            return;
        if(UserConfig.getClientUserId()==update.user_id)
            return;
        TLRPC.User user=MessagesController.getInstance().getUser(update.user_id);
        if(user==null)
            return;
//        Log.v("updater", "processing update. uname "+update.username);
        ContactModel contact=new RushSearch().whereEqual("uid", update.user_id).findSingle(ContactModel.class);
        if(contact==null){
            contact=new ContactModel();
            contact.uid=user.id;
            contact.phone = user.phone;
            contact.displayName=UserObject.getUserName(user);
            contact.username = user.username;
            if (user.photo != null)
                contact.pic = user.photo.photo_id + "";
            if (user.status != null) {
                String st = String.valueOf(user.status);
                contact.status = st.substring(0, st.indexOf("@"));
            }
            contact.updatedAt= System.currentTimeMillis();
            contact.save();
        }else{
            List<ContactChangeModel> changes = contact.contactChanges;
            if(changes==null){
                changes=new ArrayList<>();
                contact.contactChanges=changes;
            }
            ContactChangeModel change=new ContactChangeModel();
            change.userId=user.id;
            if(update.date!=0){
                change.time = (1000*update.date)+"";
            }
            if(update instanceof TLRPC.TL_updateUserName){
                String dName=ContactsController.formatName(update.first_name, update.last_name);
                if(update.username!=null){
                    if(contact.username==null || !contact.username.equals(update.username)){
                        change.type=ContactChangeModel.USERNAME;
                        change.username=dName+" - "+update.username;
                        changes.add(0, change);
                        contact.username=update.username;
                    }else if(contact.displayName==null || !contact.displayName.equals(dName)){
                        change.type=ContactChangeModel.USERNAME;
                        change.username=dName;
                        changes.add(0, change);
                        contact.username=update.username;
                        contact.displayName=dName;
                    }
                }else{
                    if(contact.displayName==null || !contact.displayName.equals(dName)){
                        change.type=ContactChangeModel.USERNAME;
                        change.username=dName;
                        changes.add(0, change);
                        contact.username=update.username;
                        contact.displayName=dName;
                    }
                }

            }else if(update instanceof TLRPC.TL_updateUserPhone){
                if (user.phone == null || !user.phone.equals(update.phone)) {
                    change.type=ContactChangeModel.PHONE;
                    change.phone=update.phone;
                    contact.phone=update.phone;
                    changes.add(0, change);
                }
            }else if(update instanceof TLRPC.TL_updateUserPhoto){
                if(update.photo!=null){
                    if(user.photo == null || user.photo.photo_id!=update.photo.photo_id){
                        change.type=ContactChangeModel.PHOTO;
                        contact.pic=update.photo.photo_id+"";
                        changes.add(0, change);
                        if(update.photo.photo_small!=null){
                            change.photoSmall=ContactChangeModel.fileLocationToString(update.photo.photo_small);
                        }
                        if(update.photo.photo_big!=null){
                            change.photoBig=ContactChangeModel.fileLocationToString(update.photo.photo_big);
                        }
                    }
                }
            }

            if(change.type==0){
//                contact.displayName=UserObject.getUserName(user);
//                contact.save();
                return;
            }
            contact.updatedAt= System.currentTimeMillis();
            contact.displayName=UserObject.getUserName(user);
//            change.save();
            contact.save();
        }
    }


}
