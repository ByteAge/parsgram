package org.tosan.messenger.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.LetterSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Ramin Boodaghi on 9/8/2017.
 */

public class OnlineContactsAdapter extends RecyclerListView.SectionsAdapter {

    private Context mContext;
    private int onlyUsers;
    private boolean needPhonebook;
    private HashMap<Integer, TLRPC.User> ignoreUsers;
    private HashMap<Integer, ?> checkedMap;
    private boolean scrolling;
    private boolean isAdmin;
    private ArrayList<TLRPC.TL_contact> onlineContacts=new ArrayList<>();

    public OnlineContactsAdapter(Context context, int onlyUsersType, boolean arg2, HashMap<Integer, TLRPC.User> arg3, boolean arg4) {
        mContext = context;
        onlyUsers = 1;
        needPhonebook = false;
        ignoreUsers = arg3;
        isAdmin = false;
        reloadContacts();
    }
    public void reloadContacts(){
        onlineContacts.clear();
        MessagesController mc=MessagesController.getInstance();
        ConnectionsManager cm=ConnectionsManager.getInstance();
        for (TLRPC.TL_contact person :
                ContactsController.getInstance().contacts) {

            TLRPC.User user = mc.getUser(person.user_id);
            int currentTime = cm.getCurrentTime();
            boolean online= user != null && user.status != null && (user.status.expires > currentTime ||
                    user.id == UserConfig.getClientUserId()) && user.status.expires > 10000;
            if(online){
                onlineContacts.add(person);
            }
        }
        notifyDataSetChanged();
    }
    public void setCheckedMap(HashMap<Integer, ?> map) {
        checkedMap = map;
    }

    public void setIsScrolling(boolean value) {
        scrolling = value;
    }

    public Object getItem(int section, int position) {
        return MessagesController.getInstance().getUser(onlineContacts.get(position).user_id);
    }

    @Override
    public boolean isEnabled(int section, int row) {
        return true;
    }

    @Override
    public int getSectionCount() {
        return 1;
    }

    @Override
    public int getCountForSection(int section) {
        return onlineContacts.size();
    }

    @Override
    public View getSectionHeaderView(int section, View view) {
        if (view == null) {
            view = new LetterSectionCell(mContext);
        }
        LetterSectionCell cell = (LetterSectionCell) view;
        cell.setLetter("");
        return view;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = new UserCell(mContext, 8, 1, false);
                break;
            case 1:
                view = new TextCell(mContext);
                break;
            case 2:
                view = new GraySectionCell(mContext);
                ((GraySectionCell) view).setText(LocaleController.getString("OnlineContacts", R.string.OnlineContacts).toUpperCase());
                break;
            case 3:
            default:
                view = new DividerCell(mContext);
                view.setPadding(AndroidUtilities.dp(LocaleController.isRTL ? 28 : 72), 0, AndroidUtilities.dp(LocaleController.isRTL ? 72 : 28), 0);
                break;
        }
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(int section, int position, RecyclerView.ViewHolder holder) {
        switch (holder.getItemViewType()) {
            case 0:
                UserCell userCell = (UserCell) holder.itemView;
//                HashMap<String, ArrayList<TLRPC.TL_contact>> usersSectionsDict = onlyUsers == 2 ? ContactsController.getInstance().usersMutualSectionsDict : ContactsController.getInstance().usersSectionsDict;
//                ArrayList<String> sortedUsersSectionsArray = onlyUsers == 2 ? ContactsController.getInstance().sortedUsersMutualSectionsArray : ContactsController.getInstance().sortedUsersSectionsArray;

                ArrayList<TLRPC.TL_contact> arr = onlineContacts;
                TLRPC.User user = MessagesController.getInstance().getUser(arr.get(position).user_id);
                userCell.setData(user, null, null, 0);
                if (checkedMap != null) {
                    userCell.setChecked(checkedMap.containsKey(user.id), !scrolling);
                }
                if (ignoreUsers != null) {
                    if (ignoreUsers.containsKey(user.id)) {
                        userCell.setAlpha(0.5f);
                    } else {
                        userCell.setAlpha(1.0f);
                    }
                }
                break;
        }
    }

    @Override
    public int getItemViewType(int section, int position) {
        return 0;
    }

    @Override
    public String getLetter(int position) {
        return null;
    }

    @Override
    public int getPositionForScrollProgress(float progress) {
        return (int) (getItemCount() * progress);
    }
}
