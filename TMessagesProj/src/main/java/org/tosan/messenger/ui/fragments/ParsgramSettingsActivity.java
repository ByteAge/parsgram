package org.tosan.messenger.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.AvatarUpdater;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.PhotoViewer;
import org.tosan.messenger.Tosan;
import org.tosan.messenger.ui.SecretaryTextActivity;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Ramin Boodaghi on 9/8/2017.
 */

public class ParsgramSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private LinearLayoutManager layoutManager;
    private BackupImageView avatarImage;
    private TextView nameTextView;
    private TextView onlineTextView;
    private AvatarUpdater avatarUpdater = new AvatarUpdater();
    private View extraHeightView;
    private View shadowView;
    private AvatarDrawable avatarDrawable;

    private int extraHeight;

    private int overscrollRow;
    private int emptyRow;
    private int uiSectionRow=-1;
    private int showTabsRow;
    private int parsgramInfoRow;
    private int rowCount;
    private int hidePhoneRow;
    private int showRealMemsCountRow;
    private int secretaryRow;
    private int stickerAndVoiceConfirmationRow;

    private SparseArray<String> keysArray=new SparseArray<>();
    private SparseBooleanArray keysDefaultBools=new SparseBooleanArray();

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        avatarUpdater.parentFragment = this;
        avatarUpdater.delegate = new AvatarUpdater.AvatarUpdaterDelegate() {
            @Override
            public void didUploadedPhoto(TLRPC.InputFile file, TLRPC.PhotoSize small, TLRPC.PhotoSize big) {
                TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                req.file = file;
                ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        if (error == null) {
                            TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                            if (user == null) {
                                user = UserConfig.getCurrentUser();
                                if (user == null) {
                                    return;
                                }
                                MessagesController.getInstance().putUser(user, false);
                            } else {
                                UserConfig.setCurrentUser(user);
                            }
                            TLRPC.TL_photos_photo photo = (TLRPC.TL_photos_photo) response;
                            ArrayList<TLRPC.PhotoSize> sizes = photo.photo.sizes;
                            TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 100);
                            TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 1000);
                            user.photo = new TLRPC.TL_userProfilePhoto();
                            user.photo.photo_id = photo.photo.id;
                            if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            if (bigSize != null) {
                                user.photo.photo_big = bigSize.location;
                            } else if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            MessagesStorage.getInstance().clearUserPhotos(user.id);
                            ArrayList<TLRPC.User> users = new ArrayList<>();
                            users.add(user);
                            MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
                                    UserConfig.saveConfig(true);
                                }
                            });
                        }
                    }
                });
            }
        };
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.featuredStickersDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.userInfoDidLoaded);

        rowCount = 0;
        overscrollRow = rowCount++;
        emptyRow = rowCount++;
//        uiSectionRow = rowCount++;
        showTabsRow = rowCount++;
        hidePhoneRow = rowCount++;
        showRealMemsCountRow = rowCount++;
        stickerAndVoiceConfirmationRow = rowCount++;
        secretaryRow = rowCount++;
        parsgramInfoRow = rowCount++;

        keysArray.put(showTabsRow, Tosan.key_enable_tabs);
        keysArray.put(hidePhoneRow, Tosan.key_hide_phone);
        keysArray.put(showRealMemsCountRow, Tosan.keY_real_members_count);
        keysArray.put(stickerAndVoiceConfirmationRow, Tosan.key_document_confirm_send);
        keysArray.put(secretaryRow, Tosan.key_secretary);
        keysDefaultBools.put(showTabsRow, true);
        keysDefaultBools.put(hidePhoneRow, false);
        keysDefaultBools.put(showRealMemsCountRow, true);
        keysDefaultBools.put(stickerAndVoiceConfirmationRow, false);
        keysDefaultBools.put(secretaryRow, false);

        MessagesController.getInstance().loadFullUser(UserConfig.getCurrentUser(), classGuid, true);

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (avatarImage != null) {
            avatarImage.setImageDrawable(null);
        }
        MessagesController.getInstance().cancelLoadFullUser(UserConfig.getClientUserId());
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.featuredStickersDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.userInfoDidLoaded);
        avatarUpdater.clear();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_avatar_actionBarSelectorBlue), false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_avatar_actionBarIconBlue), false);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAddToContainer(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){
            @Override
            public void onItemClick(int id) {
                finishFragment();
            }
        });
        extraHeight = 88;
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context) {
            @Override
            protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
                if (child == listView) {
                    boolean result = super.drawChild(canvas, child, drawingTime);
                    if (parentLayout != null) {
                        int actionBarHeight = 0;
                        int childCount = getChildCount();
                        for (int a = 0; a < childCount; a++) {
                            View view = getChildAt(a);
                            if (view == child) {
                                continue;
                            }
                            if (view instanceof ActionBar && view.getVisibility() == VISIBLE) {
                                if (((ActionBar) view).getCastShadows()) {
                                    actionBarHeight = view.getMeasuredHeight();
                                }
                                break;
                            }
                        }
                        parentLayout.drawHeaderShadow(canvas, actionBarHeight);
                    }
                    return result;
                } else {
                    return super.drawChild(canvas, child, drawingTime);
                }
            }
        };
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setGlowColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, final int position) {
                final SharedPreferences prefs=Tosan.prefs;
                // if secretary is enabled, toggle without confirmation.
                boolean canToggleSecretary=position==secretaryRow && prefs.getBoolean(keysArray.get(secretaryRow), keysDefaultBools.get(secretaryRow));
                if (canToggleSecretary || position == showTabsRow || position == hidePhoneRow || position == showRealMemsCountRow || position == stickerAndVoiceConfirmationRow) {
                    // on-off rows
                    String key=keysArray.get(position);
                    boolean on = prefs.getBoolean(keysArray.get(position), keysDefaultBools.get(position));
                    prefs.edit().putBoolean(key, !on).apply();

                    TextCheckCell cell= (TextCheckCell) view;
                    cell.setChecked(!on);

                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.tosanConfigsChanged, key, !on);
                }else if(position==secretaryRow){
                    // user wants to enable Smart-Secretary
                    SecretaryTextActivity activity=new SecretaryTextActivity();
                    activity.delegate=new ParsgramSettingsActivity(){
                        @Override
                        public void onSecretaryTextSet(CharSequence txt) {
                            if(txt.toString().trim().length()>0){
                                prefs.edit().putBoolean(Tosan.key_secretary, true).putString(Tosan.key_secretary_message, txt.toString()).apply();
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.tosanConfigsChanged, Tosan.key_secretary, true);
                            }
                        }
                    };
                    presentFragment(activity);
                }
            }
        });
        frameLayout.addView(actionBar);

        extraHeightView = new View(context);
        extraHeightView.setPivotY(0);
        extraHeightView.setBackgroundColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
        frameLayout.addView(extraHeightView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 88));

        shadowView = new View(context);
        shadowView.setBackgroundResource(R.drawable.header_shadow);
        frameLayout.addView(shadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3));

        avatarImage = new BackupImageView(context);
        avatarImage.setRoundRadius(AndroidUtilities.dp(21));
        avatarImage.setPivotX(0);
        avatarImage.setPivotY(0);
        frameLayout.addView(avatarImage, LayoutHelper.createFrame(42, 42, Gravity.TOP | Gravity.LEFT, 64, 0, 0, 0));
        avatarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                if (user != null && user.photo != null && user.photo.photo_big != null) {
                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                    PhotoViewer.getInstance().openPhoto(user.photo.photo_big, ParsgramSettingsActivity.this);
                }
            }
        });

        nameTextView = new TextView(context);
        nameTextView.setTextColor(Theme.getColor(Theme.key_profile_title));
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setGravity(Gravity.LEFT);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setPivotX(0);
        nameTextView.setPivotY(0);
        frameLayout.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 48, 0));

        onlineTextView = new TextView(context);
        onlineTextView.setTextColor(Theme.getColor(Theme.key_avatar_subtitleInProfileBlue));
        onlineTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        onlineTextView.setLines(1);
        onlineTextView.setMaxLines(1);
        onlineTextView.setSingleLine(true);
        onlineTextView.setEllipsize(TextUtils.TruncateAt.END);
        onlineTextView.setGravity(Gravity.LEFT);
        frameLayout.addView(onlineTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 48, 0));

        needLayout();

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (layoutManager.getItemCount() == 0) {
                    return;
                }
                int height = 0;
                View child = recyclerView.getChildAt(0);
                if (child != null) {
                    if (layoutManager.findFirstVisibleItemPosition() == 0) {
                        height = AndroidUtilities.dp(88) + (child.getTop() < 0 ? child.getTop() : 0);
                    }
                    if (extraHeight != height) {
                        extraHeight = height;
                        needLayout();
                    }
                }
            }
        });

        return fragmentView;
    }


    public void onSecretaryTextSet(CharSequence txt){

    }
    @Override
    public void updatePhotoAtIndex(int index) {

    }

    @Override
    public boolean allowCaption() {
        return true;
    }

    @Override
    public boolean scaleToFill() {
        return false;
    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        if (fileLocation == null) {
            return null;
        }
        TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
        if (user != null && user.photo != null && user.photo.photo_big != null) {
            TLRPC.FileLocation photoBig = user.photo.photo_big;
            if (photoBig.local_id == fileLocation.local_id && photoBig.volume_id == fileLocation.volume_id && photoBig.dc_id == fileLocation.dc_id) {
                int coords[] = new int[2];
                avatarImage.getLocationInWindow(coords);
                PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                object.viewX = coords[0];
                object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? 0 : AndroidUtilities.statusBarHeight);
                object.parentView = avatarImage;
                object.imageReceiver = avatarImage.getImageReceiver();
                object.dialogId = UserConfig.getClientUserId();
                object.thumb = object.imageReceiver.getBitmap();
                object.size = -1;
                object.radius = avatarImage.getImageReceiver().getRoundRadius();
                object.scale = avatarImage.getScaleX();
                return object;
            }
        }
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
    }

    @Override
    public void willHidePhotoViewer() {
        avatarImage.getImageReceiver().setVisible(true, true);
    }

    @Override
    public boolean isPhotoChecked(int index) {
        return false;
    }

    @Override
    public void setPhotoChecked(int index, VideoEditedInfo videoEditedInfo) {
    }

    @Override
    public boolean cancelButtonPressed() {
        return true;
    }

    @Override
    public void sendButtonPressed(int index, VideoEditedInfo videoEditedInfo) {
    }

    @Override
    public int getSelectedCount() {
        return 0;
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        avatarUpdater.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (avatarUpdater != null && avatarUpdater.currentPicturePath != null) {
            args.putString("path", avatarUpdater.currentPicturePath);
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        if (avatarUpdater != null) {
            avatarUpdater.currentPicturePath = args.getString("path");
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer) args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                updateUserData();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        updateUserData();
        fixLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    private void needLayout() {
        FrameLayout.LayoutParams layoutParams;
        int newTop = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();
        if (listView != null) {
            layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
            if (layoutParams.topMargin != newTop) {
                layoutParams.topMargin = newTop;
                listView.setLayoutParams(layoutParams);
                extraHeightView.setTranslationY(newTop);
            }
        }

        if (avatarImage != null) {
            float diff = extraHeight / (float) AndroidUtilities.dp(88);
            extraHeightView.setScaleY(diff);
            shadowView.setTranslationY(newTop + extraHeight);

            avatarImage.setScaleX((42 + 18 * diff) / 42.0f);
            avatarImage.setScaleY((42 + 18 * diff) / 42.0f);
            float avatarY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f * (1.0f + diff) - 21 * AndroidUtilities.density + 27 * AndroidUtilities.density * diff;
            avatarImage.setTranslationX(-AndroidUtilities.dp(47) * diff);
            avatarImage.setTranslationY((float) Math.ceil(avatarY));
            nameTextView.setTranslationX(-21 * AndroidUtilities.density * diff);
            nameTextView.setTranslationY((float) Math.floor(avatarY) - (float) Math.ceil(AndroidUtilities.density) + (float) Math.floor(7 * AndroidUtilities.density * diff));
            onlineTextView.setTranslationX(-21 * AndroidUtilities.density * diff);
            onlineTextView.setTranslationY((float) Math.floor(avatarY) + AndroidUtilities.dp(22) + (float )Math.floor(11 * AndroidUtilities.density) * diff);
            nameTextView.setScaleX(1.0f + 0.12f * diff);
            nameTextView.setScaleY(1.0f + 0.12f * diff);
        }
    }

    private void fixLayout() {
        if (fragmentView == null) {
            return;
        }
        fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (fragmentView != null) {
                    needLayout();
                    fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    private void updateUserData() {
        TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
        TLRPC.FileLocation photo = null;
        TLRPC.FileLocation photoBig = null;
        if (user.photo != null) {
            photo = user.photo.photo_small;
            photoBig = user.photo.photo_big;
        }
        avatarDrawable = new AvatarDrawable(user, true);

        avatarDrawable.setColor(Theme.getColor(Theme.key_avatar_backgroundInProfileBlue));
        if (avatarImage != null) {
            avatarImage.setImage(photo, "50_50", avatarDrawable);
            avatarImage.getImageReceiver().setVisible(!PhotoViewer.getInstance().isShowingImage(photoBig), false);

            nameTextView.setText(UserObject.getUserName(user));
            onlineTextView.setText(LocaleController.getString("Online", R.string.Online));

            avatarImage.getImageReceiver().setVisible(!PhotoViewer.getInstance().isShowingImage(photoBig), false);
        }
    }

    private void sendLogs() {
        try {
            ArrayList<Uri> uris = new ArrayList<>();
            File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            File[] files = dir.listFiles();
            for (File file : files) {
                uris.add(Uri.fromFile(file));
            }

            if (uris.isEmpty()) {
                return;
            }
            Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, "");
            i.putExtra(Intent.EXTRA_SUBJECT, "last logs");
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            getParentActivity().startActivityForResult(Intent.createChooser(i, "Select email application."), 500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    if (position == overscrollRow) {
                        ((EmptyCell) holder.itemView).setHeight(AndroidUtilities.dp(88));
                    } else {
                        ((EmptyCell) holder.itemView).setHeight(AndroidUtilities.dp(16));
                    }
                    break;
                }
                case 2: {
//                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
//                    if (position == textSizeRow) {
//                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
//                        int size = preferences.getInt("fons_size", AndroidUtilities.isTablet() ? 18 : 16);
//                        textCell.setTextAndValue(LocaleController.getString("TextSize", R.string.TextSize), String.format("%d", size), true);
//                    } else if (position == languageRow) {
//                        textCell.setTextAndValue(LocaleController.getString("Language", R.string.Language), LocaleController.getCurrentLanguageName(), true);
//                    } else if (position == themeRow) {
//                        textCell.setTextAndValue(LocaleController.getString("Theme", R.string.Theme), Theme.getCurrentThemeName(), true);
//                    } else if (position == contactsSortRow) {
//                        String value;
//                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
//                        int sort = preferences.getInt("sortContactsBy", 0);
//                        if (sort == 0) {
//                            value = LocaleController.getString("Default", R.string.Default);
//                        } else if (sort == 1) {
//                            value = LocaleController.getString("FirstName", R.string.SortFirstName);
//                        } else {
//                            value = LocaleController.getString("LastName", R.string.SortLastName);
//                        }
//                        textCell.setTextAndValue(LocaleController.getString("SortBy", R.string.SortBy), value, true);
//                    } else if (position == notificationRow) {
//                        textCell.setText(LocaleController.getString("NotificationsAndSounds", R.string.NotificationsAndSounds), true);
//                    } else if (position == backgroundRow) {
//                        textCell.setText(LocaleController.getString("ChatBackground", R.string.ChatBackground), true);
//                    } else if (position == sendLogsRow) {
//                        textCell.setText("Send Logs", true);
//                    } else if (position == clearLogsRow) {
//                        textCell.setText("Clear Logs", true);
//                    } else if (position == askQuestionRow) {
//                        textCell.setText(LocaleController.getString("AskAQuestion", R.string.AskAQuestion), true);
//                    } else if (position == privacyRow) {
//                        textCell.setText(LocaleController.getString("PrivacySettings", R.string.PrivacySettings), true);
//                    } else if (position == dataRow) {
//                        textCell.setText(LocaleController.getString("DataSettings", R.string.DataSettings), true);
//                    } else if (position == switchBackendButtonRow) {
//                        textCell.setText("Switch Backend", true);
//                    } else if (position == telegramFaqRow) {
//                        textCell.setText(LocaleController.getString("TelegramFAQ", R.string.TelegramFaq), true);
//                    } else if (position == contactsReimportRow) {
//                        textCell.setText(LocaleController.getString("ImportContacts", R.string.ImportContacts), true);
//                    } else if (position == stickersRow) {
//                        int count = StickersQuery.getUnreadStickerSets().size();
//                        textCell.setTextAndValue(LocaleController.getString("StickersName", R.string.StickersName), count != 0 ? String.format("%d", count) : "", true);
//                    } else if (position == privacyPolicyRow) {
//                        textCell.setText(LocaleController.getString("PrivacyPolicy", R.string.PrivacyPolicy), true);
//                    } else if (position == emojiRow) {
//                        textCell.setText(LocaleController.getString("Emoji", R.string.Emoji), true);
//                    }
//                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    SharedPreferences preferences = Tosan.prefs;
                    if(position==showTabsRow){
                        textCell.setTextAndCheck(LocaleController.getString("ShowTabs", R.string.ShowTabs), preferences.getBoolean(Tosan.key_enable_tabs, keysDefaultBools.get(showTabsRow)), true);
                    }else if(position==hidePhoneRow){
                        textCell.setTextAndCheck(LocaleController.getString("HidePhoneNumber", R.string.HidePhoneNumber), preferences.getBoolean(Tosan.key_hide_phone, keysDefaultBools.get(hidePhoneRow)), true);
                    }else if(position==showRealMemsCountRow){
                        textCell.setTextAndCheck(LocaleController.getString("ShowExactMembersCount", R.string.ShowExactMembersCount), preferences.getBoolean(Tosan.keY_real_members_count, keysDefaultBools.get(showRealMemsCountRow)), true);
                    }else if(position==stickerAndVoiceConfirmationRow){
                        textCell.setTextAndCheck(LocaleController.getString("ConfirmStickerAndVoice", R.string.ConfirmStickerAndVoice), preferences.getBoolean(Tosan.key_document_confirm_send, keysDefaultBools.get(stickerAndVoiceConfirmationRow)), true);
                    }else if(position==secretaryRow){
                        textCell.setTextAndCheck(LocaleController.getString("SmartSecretary", R.string.SmartSecretary), preferences.getBoolean(Tosan.key_secretary, keysDefaultBools.get(secretaryRow)), true);
                    }
//                    if (position == enableAnimationsRow) {
//                        textCell.setTextAndCheck(LocaleController.getString("EnableAnimations", R.string.EnableAnimations), preferences.getBoolean("view_animations", true), false);
//                    } else if (position == sendByEnterRow) {
//                        textCell.setTextAndCheck(LocaleController.getString("SendByEnter", R.string.SendByEnter), preferences.getBoolean("send_by_enter", false), true);
//                    } else if (position == saveToGalleryRow) {
//                        textCell.setTextAndCheck(LocaleController.getString("SaveToGallerySettings", R.string.SaveToGallerySettings), MediaController.getInstance().canSaveToGallery(), false);
//                    } else if (position == autoplayGifsRow) {
//                        textCell.setTextAndCheck(LocaleController.getString("AutoplayGifs", R.string.AutoplayGifs), MediaController.getInstance().canAutoplayGifs(), true);
//                    } else if (position == raiseToSpeakRow) {
//                        textCell.setTextAndCheck(LocaleController.getString("RaiseToSpeak", R.string.RaiseToSpeak), MediaController.getInstance().canRaiseToSpeak(), true);
//                    } else if (position == customTabsRow) {
//                        textCell.setTextAndValueAndCheck(LocaleController.getString("ChromeCustomTabs", R.string.ChromeCustomTabs), LocaleController.getString("ChromeCustomTabsInfo", R.string.ChromeCustomTabsInfo), MediaController.getInstance().canCustomTabs(), false, true);
//                    } else if (position == directShareRow) {
//                        textCell.setTextAndValueAndCheck(LocaleController.getString("DirectShare", R.string.DirectShare), LocaleController.getString("DirectShareInfo", R.string.DirectShareInfo), MediaController.getInstance().canDirectShare(), false, true);
//                    } else if (position == dumpCallStatsRow) {
//                        textCell.setTextAndCheck("Dump detailed call stats", preferences.getBoolean("dbg_dump_call_stats", false), true);
//                    }
                    break;
                }
                case 4: {
                    if (position == uiSectionRow) {
                        ((HeaderCell) holder.itemView).setText(LocaleController.getString("UiSettings", R.string.UiSettings));
                    } /*else if (position == supportSectionRow2) {
                        ((HeaderCell) holder.itemView).setText(LocaleController.getString("Support", R.string.Support));
                    } else if (position == messagesSectionRow2) {
                        ((HeaderCell) holder.itemView).setText(LocaleController.getString("MessagesSettings", R.string.MessagesSettings));
                    } else if (position == numberSectionRow) {
                        ((HeaderCell) holder.itemView).setText(LocaleController.getString("Info", R.string.Info));
                    }*/
                    break;
                }
                case 6: {
//                    TextDetailSettingsCell textCell = (TextDetailSettingsCell) holder.itemView;
//
//                    if (position == numberRow) {
//                        TLRPC.User user = UserConfig.getCurrentUser();
//                        String value;
//                        if (user != null && user.phone != null && user.phone.length() != 0) {
//                            value = PhoneFormat.getInstance().format("+" + user.phone);
//                        } else {
//                            value = LocaleController.getString("NumberUnknown", R.string.NumberUnknown);
//                        }
//                        textCell.setTextAndValue(value, LocaleController.getString("Phone", R.string.Phone), true);
//                    } else if (position == usernameRow) {
//                        TLRPC.User user = UserConfig.getCurrentUser();
//                        String value;
//                        if (user != null && !TextUtils.isEmpty(user.username)) {
//                            value = "@" + user.username;
//                        } else {
//                            value = LocaleController.getString("UsernameEmpty", R.string.UsernameEmpty);
//                        }
//                        textCell.setTextAndValue(value, LocaleController.getString("Username", R.string.Username), true);
//                    } else if (position == bioRow) {
//                        TLRPC.TL_userFull userFull = MessagesController.getInstance().getUserFull(UserConfig.getClientUserId());
//                        String value;
//                        if (userFull == null) {
//                            value = LocaleController.getString("Loading", R.string.Loading);
//                        } else if (userFull != null && !TextUtils.isEmpty(userFull.about)) {
//                            value = userFull.about;
//                        } else {
//                            value = LocaleController.getString("UserBioEmpty", R.string.UserBioEmpty);
//                        }
//                        textCell.setTextAndValue(value, LocaleController.getString("UserBio", R.string.UserBio), false);
//                    }
//                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == showTabsRow || position == secretaryRow || hidePhoneRow ==position || position == showRealMemsCountRow || position==stickerAndVoiceConfirmationRow;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case 0:
                    view = new EmptyCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 2:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 5:
                    view = new TextInfoCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    ((TextInfoCell) view).setText(LocaleController.formatString("ParsgramVersionInfo", R.string.ParsgramVersionInfo, BuildConfig.VERSION_NAME));
                    break;
                case 6:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            /**
             *  0 = empty
             *  1 = section with shadow
             *  2 = title - value
             *  3 = text check
             *  4 = section
             *  5 = version
             *  6 = multi-line
             *
             */

            if (position == emptyRow || position == overscrollRow) {
                return 0;
            }else if(position==parsgramInfoRow){
                return 5;
            }
            /*if (position == settingsSectionRow || position == supportSectionRow || position == messagesSectionRow || position == contactsSectionRow) {
                return 1;
            } */else if (position == showTabsRow || position == hidePhoneRow || position == secretaryRow || position == showRealMemsCountRow || position==stickerAndVoiceConfirmationRow) {
                return 3;
            } /*else if (position == notificationRow || position == themeRow || position == backgroundRow || position == askQuestionRow || position == sendLogsRow || position == privacyRow || position == clearLogsRow || position == switchBackendButtonRow || position == telegramFaqRow || position == contactsReimportRow || position == textSizeRow || position == languageRow || position == contactsSortRow || position == stickersRow || position == privacyPolicyRow || position == emojiRow || position == dataRow) {
                return 2;
            } else if (position == versionRow) {
                return 5;
            } else if (position == numberRow || position == usernameRow || position == bioRow) {
                return 6;
            }*/ else if (position == uiSectionRow) {
                return 4;
            } else {
                return 2;
            }
        }
    }

    @Override
    public ThemeDescription[] getThemeDescriptions() {
        return new ThemeDescription[]{
                new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{EmptyCell.class, TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, TextInfoCell.class, TextDetailSettingsCell.class}, null, null, null, Theme.key_windowBackgroundWhite),
                new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray),

                new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue),
                new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue),
                new ThemeDescription(extraHeightView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_avatar_actionBarIconBlue),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_avatar_actionBarSelectorBlue),
                new ThemeDescription(nameTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_profile_title),
                new ThemeDescription(onlineTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_avatar_subtitleInProfileBlue),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem),

                new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector),

                new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider),

                new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow),

                new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText),

                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchThumb),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchThumbChecked),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked),

                new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader),

                new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),

                new ThemeDescription(listView, 0, new Class[]{TextInfoCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText5),

                new ThemeDescription(avatarImage, 0, null, null, new Drawable[]{Theme.avatar_photoDrawable, Theme.avatar_broadcastDrawable}, null, Theme.key_avatar_text),
                new ThemeDescription(avatarImage, 0, null, null, new Drawable[]{avatarDrawable}, null, Theme.key_avatar_backgroundInProfileBlue),
        };
    }
}
