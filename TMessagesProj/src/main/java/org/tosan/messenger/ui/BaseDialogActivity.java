package org.tosan.messenger.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Adapters.DialogsAdapter;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;
import org.tosan.messenger.TabsController;
import org.tosan.messenger.Tosan;
import org.tosan.messenger.ui.fragments.PasscodeSelectorActivity;
import org.tosan.messenger.ui.fragments.PasscodeVerifyFragment;


public class BaseDialogActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public TabsWidget tabsWidget;
    protected boolean showTabs=true;
    public boolean isHiddenDialogs;
    private int currentType;
    protected boolean searching;

    public int tabsHeight= AndroidUtilities.dp(40);
    protected DialogsActivity.DialogsActivityDelegate delegate;

    public BaseDialogActivity(Bundle args) {
        super(args);
        isHiddenDialogs=args!=null && args.getBoolean("hidden_dialogs", false);
        showTabs=!isHiddenDialogs;
        currentType= !showTabs ? 3 : Tosan.prefs.getInt(Tosan.key_last_tab_index, 0)+3;
    }

    public void createFloatingButton(FrameLayout parent, boolean onlySelect){
        Context context=parent.getContext();
//        showTabs=showTabs&&!onlySelect;
        currentType= !showTabs ? 3 : Tosan.prefs.getInt(Tosan.key_last_tab_index, 0)+3;
        tabsWidget=new TabsWidget(context){
            @Override
            public void selectTab(int index) {
                super.selectTab(index);
                currentType=index+3;
                updateAdapter(currentType);
            }
        };
        parent.addView(tabsWidget, LayoutHelper.createFrame(-1,40, Gravity.TOP,0,0,0,0));
        actionBar.setCastShadows(false);
        tabsWidget.setVisibility(!showTabs ? View.GONE : View.VISIBLE);

        if(isHiddenDialogs){
            ActionBarMenu menu=actionBar.createMenu();
            ActionBarMenuItem item=menu.addItem(10, R.drawable.ic_ab_other);
            item.addSubItem(45, LocaleController.getString("ChangePasscode",R.string.ChangePasscode));
        }

        if(!isHiddenDialogs){
            actionBar.createMenu().addItem(2, R.drawable.pg_ab_cloud).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    int uid= UserConfig.getClientUserId();
                    Bundle d=new Bundle();
                    d.putInt("user_id", uid);
                    presentFragment(new ChatActivity(d));
                }
            });
        }
    }

    public void showProtectedChats(){
        if(Tosan.isHiddenDialogsPasswordSet()){
            PasscodeVerifyFragment fragment=new PasscodeVerifyFragment(Tosan.prefs.getInt(Tosan.key_hidden_passtype, 0),
                    Tosan.prefs.getString(Tosan.key_hidden_passcode, "1324"),
                    new PasscodeVerifyFragment.VerifyDelegate() {
                        @Override
                        public void onVerify() {
                            presendHiddenDialogs(true);
                        }
                    });
            presentFragment(fragment);
        }else{
            presendHiddenDialogs(false);
        }
    }

    private void presendHiddenDialogs(boolean passcodeShown){
        Bundle args=getArguments();
        if(args==null)
            args=new Bundle();
        args.putBoolean("hidden_dialogs", true);
        DialogsActivity da=new DialogsActivity(args);
        da.setDelegate(delegate);
        presentFragment(da, getArguments()!=null || passcodeShown);

        if(passcodeShown && getArguments()!=null)
            removeSelfFromStack();
    }


    protected void onMenuClick(int id){
        if(id==45){
            PasscodeSelectorActivity activity=new PasscodeSelectorActivity(1, new PasscodeSelectorActivity.FreshPasscodeDelegate() {
                @Override
                public void onNewPassword(String password, int passcodeType) {
                    Log.v("ramin", "passcode set "+password+", typ e "+passcodeType);
                    Tosan.prefs.edit().putString(Tosan.key_hidden_passcode, password).putInt(Tosan.key_hidden_passtype, passcodeType).apply();
                }
            });
            presentFragment(activity);
//            if(Tosan.isHiddenDialogsPasswordSet()){
//
//            }else{
//
//            }
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if(id==NotificationCenter.tosanConfigsChanged){
            if(Tosan.key_enable_tabs.equals(args[0])){
                showTabs=!isHiddenDialogs && (boolean)args[1];
                currentType= !showTabs ? 3 : Tosan.prefs.getInt(Tosan.key_last_tab_index, 0)+3;
                updatePasscodeButton();
                updateAdapter(currentType);
            }
        }
    }

    protected void updatePasscodeButton() {
        if(tabsWidget==null)
            return;
        if(!searching && showTabs){
            tabsWidget.setVisibility(View.VISIBLE);
        }else{
            tabsWidget.setVisibility(View.GONE);
        }
    }

    public void updateAdapter(int type){}
    protected ThemeDescription.ThemeDescriptionDelegate tabsDelegate=new ThemeDescription.ThemeDescriptionDelegate() {
        @Override
        public void didSetColor(int color) {
            tabsWidget.colorsUpdated();
        }
    };


    public int getCurrentType(int type){
        if(showTabs && tabsWidget.getVisibility()== View.VISIBLE)
            return currentType;
        return type;
    }


    /**
     * Lighten or darken a color
     *
     * @param color
     *     color value
     * @param percent
     *     -1.0 to 1.0
     * @return new shaded color
     * @see #shadeColor(String, double)
     */
    public static int shadeColor(int color, double percent) {
        return shadeColor(String.format("#%06X", (0xFFFFFF & color)), percent); // ignores alpha channel
    }

    /**
     * Lighten or darken a color
     *
     * @param color
     *     7 character string representing the color.
     * @param percent
     *     -1.0 to 1.0
     * @return new shaded color
     * @see #shadeColor(int, double)
     */
    public static int shadeColor(String color, double percent) {
        // based off http://stackoverflow.com/a/13542669/1048340
        long f = Long.parseLong(color.substring(1), 16);
        double t = percent < 0 ? 0 : 255;
        double p = percent < 0 ? percent * -1 : percent;
        long R = f >> 16;
        long G = f >> 8 & 0x00FF;
        long B = f & 0x0000FF;
        int red = (int) (Math.round((t - R) * p) + R);
        int green = (int) (Math.round((t - G) * p) + G);
        int blue = (int) (Math.round((t - B) * p) + B);
        return Color.rgb(red, green, blue);
    }







    public boolean isHidden(TLRPC.TL_dialog dialog){
        boolean hidden= MessagesController.getInstance().tabsController.hiddenDialogs.get((int) dialog.id);
        //Log.v("ramin", "hidden? "+hidden+", did: "+dialog.id);
        return hidden;
    }
    public boolean isFavor(TLRPC.TL_dialog dialog){
        return MessagesController.getInstance().tabsController.favoriteDialogs.get((int) dialog.id);
    }
    public void hideDialog(TLRPC.TL_dialog dialog, boolean hide){
        TabsController controller=MessagesController.getInstance().tabsController;
        if(hide)
            controller.addToHiddens(dialog);
        else controller.removeFromHiddens(dialog);
    }
    public void makeFavor(TLRPC.TL_dialog dialog, boolean favor){
        TabsController controller=MessagesController.getInstance().tabsController;
        if(favor)
            controller.addToFavorites(dialog);
        else controller.removeFromFavorites(dialog);
    }




    public class DialogsUpdaterAdapter extends DialogsAdapter{
        public DialogsUpdaterAdapter(Context context, int type, boolean hiddens) {
            super(context, type, hiddens);
            Log.v("ramin", "created type "+type+", isHidden "+hiddens);
        }


        @Override
        public void setDialogsType(int dialogsType) {
            super.setDialogsType(dialogsType);
            Log.v("ramin", "new did type "+dialogsType+", isHidden "+isHiddenDialogs);
            notifyDataSetChanged();
        }
    }
}
