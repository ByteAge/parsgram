package org.tosan.messenger.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.tosan.messenger.sql.ContactChangeModel;
import org.tosan.messenger.sql.ContactModel;


public class ContactModelCell extends FrameLayout {

    public SimpleTextView nameTextView;
    public SimpleTextView statusTextView;
    public BackupImageView avatarImageView;
    private AvatarDrawable avatarDrawable;
    private int statusOnlineColor;

    public ContactModelCell(@NonNull Context context, int padding) {
        super(context);

        int checkbox=0;

        avatarDrawable = new AvatarDrawable();
        statusOnlineColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);

        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(24));
        addView(avatarImageView, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 0 : 7 + padding, 8, LocaleController.isRTL ? 7 + padding : 0, 0));


        nameTextView = new SimpleTextView(context);
        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameTextView.setTextSize(17);
        nameTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 28 + (checkbox == 2 ? 18 : 0) : (68 + padding), 11.5f, LocaleController.isRTL ? (68 + padding) : 28 + (checkbox == 2 ? 18 : 0), 0));

        statusTextView = new SimpleTextView(context);
        statusTextView.setTextSize(14);
        statusTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        statusTextView.setTextColor(statusOnlineColor);
        addView(statusTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 28 : (68 + padding), 34.5f, LocaleController.isRTL ? (68 + padding) : 28, 0));

    }


    public void setData(ContactModel contactModel){
        setData(contactModel.contactChanges.get(0), contactModel);
//        String name=contactModel.displayName;
//        String sub="";
//        if(contactModel.contactChanges!=null && !contactModel.contactChanges.isEmpty())
//            sub= contactModel.contactChanges.size()+" تغییر";
//        else sub="هیچ تغییری نیست";
//
//
//        nameTextView.setText(name);
//        statusTextView.setText(sub);
//
//        TLRPC.User user=MessagesController.getInstance().getUser(contactModel.uid);
//        TLObject photo=null;
//        if(user!=null){
//            avatarDrawable.setInfo(user);
//            if(user.photo!=null)
//                photo=user.photo.photo_small;
//        }else{
//            TLRPC.Chat chat=MessagesController.getInstance().getChat(contactModel.uid);
//            if(chat!=null){
//                if(chat.photo!=null)
//                    photo=chat.photo.photo_small;
//                avatarDrawable.setInfo(chat);
//            }
//        }
//        avatarImageView.setImage(photo, "50_50", avatarDrawable);
//        invalidate();
    }
    public void setData(ContactChangeModel change, ContactModel contactModel){
        String name= contactModel.displayName;
        String sub="";
        if(change.type==ContactChangeModel.PHOTO)
            sub="عکس خود را تغییر داد.";
        else if(change.type==ContactChangeModel.PHONE)
            sub="شماره خود را به "+change.phone+" تغییر داد.";
        else if(change.type==ContactChangeModel.USERNAME){
//            sub="نام کاربری خود را به "+change.username+" تغییر داد.";
            sub="نام جدید: "+change.username;
        }
        else if(change.type==ContactChangeModel.STATUS)
            sub="وضعیت خود را به "+change.status+" تغییر داد.";


        nameTextView.setText(name);
        statusTextView.setText(sub);

        TLRPC.User user=MessagesController.getInstance().getUser(change.userId);
        TLObject photo=change.getFileLocation(true);
        if(photo==null)
            photo=change.getFileLocation(false);
        if(user!=null){
            avatarDrawable.setInfo(user);
//            if(user.photo!=null && photo==null)
//                photo=user.photo.photo_small;
        }else{
            TLRPC.Chat chat=MessagesController.getInstance().getChat(change.userId);
            if(chat!=null){
//                if(chat.photo!=null && photo==null)
//                    photo=chat.photo.photo_small;
                avatarDrawable.setInfo(chat);
            }
        }
        avatarImageView.setImage(photo, "50_50", avatarDrawable);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
