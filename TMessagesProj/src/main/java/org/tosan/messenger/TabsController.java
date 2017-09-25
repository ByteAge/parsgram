package org.tosan.messenger;

import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;
import org.tosan.messenger.sql.CatModel;
import org.tosan.messenger.sql.DialogModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import co.uk.rushorm.core.RushCore;
import co.uk.rushorm.core.RushSearch;

/**
 * Created by Ramin Boodaghi on 8/21/2017.
 */

public class TabsController {

    public static final int BOTS=5,
            GROUPS=2, SUPERGROUPS=3, FAVORITES=6, CHANNELS=4, USERS=1, ALL=0, UNREADS=7;
    public static final int FAVOR=1, HIDDEN=2, LOCKED=3;
    public ArrayList<TLRPC.TL_dialog>
            bots,
            uncategorizedDialogs,
            usersAll, usersEncrypted, users,
            favorites, hiddens,
            groups, megaGroups, channels, allGroups,
            all, unreads;//, ownGroups, ownMegaGroups, ownChannels;
    SparseArray<ArrayList<TLRPC.TL_dialog>> dialogKeys;
    public int totalUnreads=0;
    private HashMap<Long, String> dialogsPasscode;


    public ArrayList<Cat> cats=new ArrayList<>();

    private final Object unreadLock=new Object();

    public SparseBooleanArray favoriteDialogs, hiddenDialogs;

    public TabsController() {

        uncategorizedDialogs=new ArrayList<>();

        bots=new ArrayList<>();

        usersAll=new ArrayList<>();
        usersEncrypted=new ArrayList<>();
        users=new ArrayList<>();

        favorites=new ArrayList<>();
        hiddens=new ArrayList<>();

        groups=new ArrayList<>();
        allGroups=new ArrayList<>();
        megaGroups=new ArrayList<>();
        channels=new ArrayList<>();

        dialogKeys=new SparseArray<>();
        favoriteDialogs=new SparseBooleanArray();
        hiddenDialogs=new SparseBooleanArray();
        unreads=new ArrayList<>();
        all=new ArrayList<>();
        //lockedDialogs=new ArrayList<>();
        dialogsPasscode=new HashMap<>();

        dialogKeys.put(BOTS, bots);
        dialogKeys.put(GROUPS, groups);
        dialogKeys.put(SUPERGROUPS, megaGroups);
        dialogKeys.put(FAVORITES, favorites);
        dialogKeys.put(CHANNELS, channels);
        dialogKeys.put(USERS, usersAll);
        dialogKeys.put(ALL, all);
        dialogKeys.put(UNREADS, unreads);

        initModels(FAVOR);
        initModels(HIDDEN);
        initModels(LOCKED);

        List<CatModel> cats=new RushSearch().find(CatModel.class);
        if(cats==null || cats.isEmpty())
            return;
        for(CatModel cat: cats){
            Cat catt=new Cat();
            catt.name=cat.name;

            String ds=cat.dialogs;
            if(TextUtils.isEmpty(ds))
                continue;
            String[] sDids=ds.split(",");
            for (String sDid: sDids){
                try {
                    int did=Integer.valueOf(sDid);
                    catt.sqlDialogs.put(did, true);
                }catch (Throwable ignored){}
            }

            this.cats.add(catt);

        }
    }

    public void setTabAsRead(int tabId){
        MessagesController mc=MessagesController.getInstance();
        for (TLRPC.TL_dialog dialog:dialogKeys.get(tabId)){
            mc.markDialogAsRead(dialog.id, dialog.top_message,dialog.top_message, 0,false,false);
        }
    }
    public int getTabDialogsCount(int tabId){
        return dialogKeys.get(tabId).size();
    }

    public void cleanUp(){
        uncategorizedDialogs.clear();
        for(Cat cat:cats){
            cat.dialogs.clear();
        }

        //NotificationCenter.getInstance().removeObserver(unreadsResolver, NotificationCenter.dialogsNeedReload);
        bots.clear();

        usersAll.clear();
        usersEncrypted.clear();
        users.clear();

        favorites.clear();
        hiddens.clear();

        allGroups.clear();
        groups.clear();
        megaGroups.clear();
        channels.clear();

        unreads.clear();
        all.clear();
        totalUnreads=0;
    }
    public void endSort(){
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
    }
    public void refreshUnreads(){
        ArrayList<TLRPC.TL_dialog> removes=new ArrayList<>();
        synchronized (unreadLock){
            for (TLRPC.TL_dialog d:
                    unreads) {
                if(d.unread_count<1)
                    removes.add(d);
            }
            //Log.v("ramin", "current size "+unreads.size()+", removing "+removes.size());
            unreads.removeAll(removes);
            //Log.v("ramin", "now size "+unreads.size());
        }
    }

    /*private NotificationCenter.NotificationCenterDelegate unreadsResolver=new NotificationCenter.NotificationCenterDelegate() {
        @Override
        public void didReceivedNotification(int id, Object... args) {
            if(id==NotificationCenter.dialogsNeedReload){

            }
        }
    };*/

    public void deleteCategory(Cat catt){
        CatModel cat=new RushSearch().whereEqual("name", catt.name).findSingle(CatModel.class);
        if(cat!=null)
            cat.delete();
        cats.remove(catt);
    }
    public CatModel creatCat(String name){
        CatModel catModel=new CatModel();
        catModel.name=name;
        catModel.save();
//        catDialogsDict.put(name, new ArrayList<TLRPC.TL_dialog>());
//        categories.add(name);
        Cat cat=new Cat();
        cat.name=name;
        cats.add(cat);
        return catModel;
    }
    public boolean addToCategory(ArrayList<TLRPC.TL_dialog> dialogs, Cat cat){
        CatModel catModel=new RushSearch().whereEqual("name", cat.name).findSingle(CatModel.class);
        if(catModel==null)
            return false;
        // persist sql
        String sDs=catModel.dialogs;
        if(TextUtils.isEmpty(sDs)){
            sDs="";
        }
        for (TLRPC.TL_dialog d:dialogs){
            sDs+=d.id+",";
            uncategorizedDialogs.remove(d);
            cat.dialogs.add(d);
        }
        catModel.dialogs=sDs;
        catModel.save();
        Log.v("ramin", "generated dialogs "+sDs);
        return true;
    }
    public boolean removeFromCategory(List<TLRPC.TL_dialog> dialogs, Cat cat){
        CatModel catModel=new RushSearch().whereEqual("name", cat.name).findSingle(CatModel.class);
        if(catModel==null)
            return false;
        String ds=catModel.dialogs;
        if(TextUtils.isEmpty(ds))
            return false;
        for (TLRPC.TL_dialog d:dialogs){
            uncategorizedDialogs.add(d);
            cat.dialogs.remove(d);
        }
        String[] sDids=ds.split(",");
        String newDs="";
        for (String sDid: sDids){
            try {
                int did=Integer.valueOf(sDid);
                boolean isToRemove=false;
                shit: for (TLRPC.TL_dialog dialog: dialogs){
                    isToRemove=did==dialog.id;
                    if(isToRemove)
                        break shit;
                }
                if(!isToRemove)
                    newDs+=sDid+",";
            }catch (Throwable ignored){}
        }
        catModel.dialogs=newDs;
        catModel.save();
        return false;
    }

    public int getUnreadForTab(int tabId){
        int unreads=0;
        for (TLRPC.TL_dialog d :
                dialogKeys.get(tabId)) {
            unreads += d.unread_count;
        }
        return unreads;
    }
    ArrayList<TLRPC.TL_dialog> getDialogs(int whichTab){
        ArrayList<TLRPC.TL_dialog> ds= dialogKeys.get(whichTab);
        if(ds==null)
            ds= new ArrayList<>();
        return ds;
    }

    public boolean isHidden(long did){
        for (TLRPC.TL_dialog dialog :
                hiddens) {
            if (dialog.id == did)
                return true;
        }
        return false;
    }

    public void process(TLRPC.TL_dialog dialog){

        totalUnreads+=dialog.unread_count;

        boolean hidden=hiddenDialogs.get((int) dialog.id);
        if(hidden){
            //Log.v("ramin", "processing hidden "+dialog.id);
            hiddens.add(dialog);
        }
        else if(favoriteDialogs.get((int) dialog.id)){
            favorites.add(dialog);
        }
        if(!hidden)
            all.add(dialog);
        if(!hidden && dialog.unread_count>0){
            synchronized (unreadLock){
                unreads.add(dialog);
            }
        }

        if(!hidden){
            String catName=null;
            for(Cat cat:cats){
                if(cat.sqlDialogs.get((int) dialog.id)){
                    catName=cat.name;
                    cat.dialogs.add(dialog);
                    break;
                }
            }
            if(catName==null){
                uncategorizedDialogs.add(dialog);
            }
        }
        //if(isLocked(dialog))
        //    lockedDialogs.add(dialog);
    }
    public void addMegaGroup(TLRPC.TL_dialog dialog, boolean owner){
        if(hiddenDialogs.get((int) dialog.id))
            return;
        megaGroups.add(dialog);
        allGroups.add(dialog);
    }
    public void addChannel(TLRPC.TL_dialog dialog, boolean owner){
        if(hiddenDialogs.get((int) dialog.id))
            return;
        channels.add(dialog);
    }
    public void addGroup(TLRPC.TL_dialog  dialog, boolean owner){
        if(hiddenDialogs.get((int) dialog.id))
            return;
        groups.add(dialog);
        allGroups.add(dialog);
    }
    public void addBot(TLRPC.TL_dialog dialog){
        if(hiddenDialogs.get((int) dialog.id))
            return;
        bots.add(dialog);
    }
    public void addUser(TLRPC.TL_dialog dialog, boolean encrypted){
        if(hiddenDialogs.get((int) dialog.id))
            return;
        usersAll.add(dialog);
        if(encrypted)
            usersEncrypted.add(dialog);
        else users.add(dialog);
    }

    public void delete(TLRPC.TL_dialog dialog1){
        bots.remove(dialog1);

        usersAll.remove(dialog1);
        usersEncrypted.remove(dialog1);
        users.remove(dialog1);

        favorites.remove(dialog1);
        hiddens.remove(dialog1);

        allGroups.remove(dialog1);
        groups.remove(dialog1);
        megaGroups.remove(dialog1);
        channels.remove(dialog1);
        all.remove(dialog1);
        unreads.remove(dialog1);
        //lockedDialogs.remove(dialog1);

        totalUnreads-=dialog1.unread_count;
    }


    public void addToFavorites(TLRPC.TL_dialog d){
        addToModels(d, FAVOR);
    }
    public void removeFromFavorites(TLRPC.TL_dialog d){
        removeFromModel(d, FAVOR);
    }
    public void addToHiddens(TLRPC.TL_dialog d){

        addToModels(d, HIDDEN);
    }
    public void removeFromHiddens(TLRPC.TL_dialog d){
        removeFromModel(d, HIDDEN);
    }

    public DialogModel getLockedModel(long dialogId){
        if(!isLocked(dialogId))
            return null;
        try {
            List<DialogModel> found=new RushSearch()
                    .whereEqual("did", dialogId).and().whereEqual("type", LOCKED).limit(1).find(DialogModel.class);
            if(found==null||found.isEmpty())
                return null;
            return found.get(0);
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        }
        return null;
    }
    public boolean isLocked(long did){
        String passcode=dialogsPasscode.get(did);
        return passcode!=null;
    }
    public boolean isLocked(TLRPC.TL_dialog dialog){
        return isLocked(dialog.id);
    }
    public void lockDialog(TLRPC.TL_dialog d, boolean lock, String password, int passwordType){
        //Log.v("ramin", "locking? "+lock+", did: "+d.id);
        if(lock){
            if(isLocked(d))
                return;
            DialogModel dm=new DialogModel(d.id, LOCKED);
            dm.password= password;
            dm.passwordType=passwordType;
            dm.save();
            dialogsPasscode.put(d.id, password);
        }else {
            // unlock
            if(!isLocked(d))
                return;
            dialogsPasscode.remove(d.id);
            delete(new RushSearch()
                    .whereEqual("did", d.id).and().whereEqual("type", LOCKED));
        }
    }


    private void addToModels(TLRPC.TL_dialog d, int type){
        SparseBooleanArray target=type==FAVOR?favoriteDialogs:hiddenDialogs;
        //Log.v("ramin", "adding to list "+d.id);
        if(!target.get((int) d.id)){
            new DialogModel(d.id, type).save();
            target.put((int) d.id, true);
            //Log.v("ramin", "added to list "+type);
            if(type==HIDDEN){
                delete(d);
                hiddens.add(d);
            }else {
                if(isHidden(d.id))
                    return;
                favorites.remove(d);
                favorites.add(d);
            }
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
        }//else Log.v("ramin", "already in list "+type);
    }
    private void removeFromModel(TLRPC.TL_dialog d, int type){
        SparseBooleanArray target=type==FAVOR?favoriteDialogs:hiddenDialogs;
        if(target.get((int) d.id)){
            target.delete((int) d.id);
            delete(new RushSearch()
                    .whereEqual("did", d.id).and().whereEqual("type", type));
            if(target==hiddenDialogs){
                MessagesController mc=MessagesController.getInstance();
                mc.sortDialogs(null);
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            }else {
                favorites.remove(d);
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            }
        }
    }
    private void sortUnhiddens(){
        all.clear();
        for (TLRPC.TL_dialog d:
                MessagesController.getInstance().dialogs_dict.values()) {
            if(!isHidden(d.id)){
                all.add(d);
                process(d);
            }
        }
        Collections.sort(all, MessagesController.getInstance().dialogComparator);
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);

    }
    private void delete(RushSearch q){
        List<DialogModel> models=q.find(DialogModel.class);
        if(models==null||models.isEmpty())
            return;
        RushCore.getInstance().delete(models);
        /*for (DialogModel model :
                models) {
            model.delete();
        }*/
    }
    private void initModels(int type){

        RushSearch q=new RushSearch().whereEqual("type", type);
        if(type==LOCKED){
            dialogsPasscode.clear();
            for (DialogModel model :
                    q.find(DialogModel.class)) {
                dialogsPasscode.put(model.did, model.password);
            }
            return;
        }
        SparseBooleanArray target=type==FAVOR?favoriteDialogs:hiddenDialogs;
        for (DialogModel favor:
                q.find(DialogModel.class)) {
            target.put((int) favor.did, true);
        }
    }


    public class Cat {

        public String name;
        public ArrayList<TLRPC.TL_dialog> dialogs=new ArrayList<>();
        public SparseBooleanArray sqlDialogs=new SparseBooleanArray();

        public int getUnreadCount(){
            int count=0;
            for (TLRPC.TL_dialog d:dialogs){
                count+=d.unread_count;
            }
            return count;
        }

    }
}
