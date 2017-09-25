package org.tosan.messenger.ui.fragments;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.style.CharacterStyle;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.query.MessagesQuery;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ShareAlert;
import org.telegram.ui.Components.SizeNotifierFrameLayout;

import java.util.ArrayList;

public class AdvanceForwardFragment extends BaseFragment {

    private MessageObject originalMessage;
    private ChatActivityEnterView chatActivityEnterView;
    //private RelativeLayout popupContainer;

    public AdvanceForwardFragment(MessageObject originalMessage) {
        this.originalMessage = originalMessage;
    }



    @Override
    public boolean dismissDialogOnPause(Dialog dialog) {
        return true;
    }

    @Override
    public View createView(Context context) {

        final RelativeLayout popupContainer = new RelativeLayout(context);
        final Drawable bg= Theme.getCachedWallpaper();
        SizeNotifierFrameLayout contentView = new SizeNotifierFrameLayout(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                if(bg!=null){
                    bg.setBounds(0,0, canvas.getWidth(),canvas.getHeight());
                    bg.draw(canvas);
                }
                super.onDraw(canvas);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                //int widthMode = MeasureSpec.getMode(widthMeasureSpec);
                //int heightMode = MeasureSpec.getMode(heightMeasureSpec);
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);
                //Log.d("height "+heightSize);

                setMeasuredDimension(widthSize, heightSize);

                int keyboardSize = getKeyboardHeight();

                if (keyboardSize <= AndroidUtilities.dp(20)) {
                    heightSize -= chatActivityEnterView.getEmojiPadding();
                }

                int childCount = getChildCount();
                //Log.d("child count "+childCount+" heightSize "+heightSize);
                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() == GONE) {
                        continue;
                    }
                    if (chatActivityEnterView.isPopupView(child)) {
                        //Log.d("popup view view");
                        child.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, MeasureSpec.EXACTLY));
                    } else if (chatActivityEnterView.isRecordCircle(child)) {
                        //Log.d("record view");
                        measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                    } else {
                        //Log.d("else view "+child);
                        child.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(Math.max(AndroidUtilities.dp(10), heightSize + AndroidUtilities.dp(2)), MeasureSpec.EXACTLY));
                    }
                }
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                final int count = getChildCount();

                int paddingBottom = getKeyboardHeight() <= AndroidUtilities.dp(20) ? chatActivityEnterView.getEmojiPadding() : 0;

                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() == GONE) {
                        continue;
                    }
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                    int width = child.getMeasuredWidth();
                    int height = child.getMeasuredHeight();

                    if(child==chatActivityEnterView){
                        //Log.d("enter hieght "+height);
                        child.layout(0,getHeight()-height,  width,getHeight());
                        continue;
                    }

                    int childLeft;
                    int childTop;

                    int gravity = lp.gravity;
                    if (gravity == -1) {
                        gravity = Gravity.TOP | Gravity.LEFT;
                    }

                    final int absoluteGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                    switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                        case Gravity.CENTER_HORIZONTAL:
                            childLeft = (r - l - width) / 2 + lp.leftMargin - lp.rightMargin;
                            break;
                        case Gravity.RIGHT:
                            childLeft = r - width - lp.rightMargin;
                            break;
                        case Gravity.LEFT:
                        default:
                            childLeft = lp.leftMargin;
                    }

                    switch (verticalGravity) {
                        case Gravity.TOP:
                            childTop = lp.topMargin;
                            break;
                        case Gravity.CENTER_VERTICAL:
                            childTop = ((b - paddingBottom) - t - height) / 2 + lp.topMargin - lp.bottomMargin;
                            break;
                        case Gravity.BOTTOM:
                            childTop = ((b - paddingBottom) - t) - height - lp.bottomMargin;
                            break;
                        default:
                            childTop = lp.topMargin;
                    }
                    if (chatActivityEnterView.isPopupView(child)) {
                        childTop = paddingBottom != 0 ? getMeasuredHeight() - paddingBottom : getMeasuredHeight();
                    } else if (chatActivityEnterView.isRecordCircle(child)) {
                        childTop = popupContainer.getTop() + popupContainer.getMeasuredHeight() - child.getMeasuredHeight() - lp.bottomMargin;
                        childLeft = popupContainer.getLeft() + popupContainer.getMeasuredWidth() - child.getMeasuredWidth() - lp.rightMargin;
                    }
                    child.layout(childLeft, childTop, childLeft + width, childTop + height);
                }

                notifyHeightChanged();
            }
        };
        RelativeLayout relativeLayout = new RelativeLayout(context);
        contentView.addView(relativeLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        relativeLayout.addView(popupContainer, LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, -1, 0, 0, 0, 0, RelativeLayout.CENTER_IN_PARENT));

        chatActivityEnterView=new ChatActivityEnterView(getParentActivity(), contentView, null, false);
        popupContainer.addView(chatActivityEnterView, LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_BOTTOM));
        chatActivityEnterView.setForceShowSendButton(true, false);


        ChatMessageCell cell=new ChatMessageCell(context);
        cell.setMessageObject(originalMessage, false, false);
        cell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
            @Override
            public void didPressedUserAvatar(ChatMessageCell cell, TLRPC.User user) {

            }

            @Override
            public void didPressedViaBot(ChatMessageCell cell, String username) {

            }

            @Override
            public void didPressedChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId) {

            }

            @Override
            public void didPressedCancelSendButton(ChatMessageCell cell) {

            }

            @Override
            public void didLongPressed(ChatMessageCell cell) {

            }

            @Override
            public void didPressedReplyMessage(ChatMessageCell cell, int id) {

            }

            @Override
            public void didPressedUrl(MessageObject messageObject, CharacterStyle url, boolean longPress) {

            }

            @Override
            public void needOpenWebView(String url, String title, String description, String originalUrl, int w, int h) {

            }

            @Override
            public void didPressedImage(ChatMessageCell cell) {

            }

            @Override
            public void didPressedShare(ChatMessageCell cell) {

            }

            @Override
            public void didPressedOther(ChatMessageCell cell) {

            }

            @Override
            public void didPressedBotButton(ChatMessageCell cell, TLRPC.KeyboardButton button) {

            }

            @Override
            public void didPressedInstantButton(ChatMessageCell cell, int type) {

            }

            @Override
            public boolean needPlayMessage(MessageObject messageObject) {
                return false;
            }

            @Override
            public boolean canPerformActions() {
                return false;
            }
        });
        popupContainer.addView(cell, 0,LayoutHelper.createRelative(-2,-2, RelativeLayout.ALIGN_PARENT_TOP));

        //Log.d("content type "+originalMessage.contentType);
        if(originalMessage.contentType==0 && originalMessage.messageOwner.media!=null){
            chatActivityEnterView.setFieldText(originalMessage.messageOwner.media.caption);
        }else{
            chatActivityEnterView.setFieldText(originalMessage.messageText);
        }

        actionBar.setBackgroundColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_avatar_actionBarSelectorBlue), false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_avatar_actionBarIconBlue), false);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAddToContainer(true);
        actionBar.setTitle(LocaleController.getString("AdvancedForward", R.string.AdvancedForward));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){
            @Override
            public void onItemClick(int id) {
                if(id==-1)
                    finishFragment();
            }
        });

        chatActivityEnterView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {
            @Override
            public void onMessageSend(CharSequence message) {
                chatActivityEnterView.setFieldText(message);
                TLRPC.MessageMedia media=originalMessage.messageOwner.media;
                String f= message==null?"":message.toString();
                ArrayList<TLRPC.MessageEntity> entities=MessagesQuery.getEntities(new CharSequence[] {f});
                if(entities!=null && !entities.isEmpty())
                    originalMessage.messageOwner.entities= entities;
                if(media!=null){
                    media.caption=f;
                    if(media.photo!=null)
                        media.photo.caption=f;
                    if(media.document!=null)
                        media.document.caption=f;
                    if(media.video_unused!=null)
                        media.video_unused.caption=f;
                }else {

                }
                showDialog(new ShareAlert(getParentActivity(), originalMessage, null, false, null, false, true){
//                        @Override
//                        public void dismiss() {
//                            super.dismiss();
//                            finishFragment();
//                        }

                    @Override
                    public void onDone() {
                        finishFragment();
                    }
                });
            }

            @Override
            public void needSendTyping() {

            }

            @Override
            public void onTextChanged(CharSequence text, boolean bigChange) {

            }

            @Override
            public void onAttachButtonHidden() {

            }

            @Override
            public void onAttachButtonShow() {

            }

            @Override
            public void onWindowSizeChanged(int size) {

            }

            @Override
            public void onStickersTab(boolean opened) {

            }

            @Override
            public void onMessageEditEnd(boolean loading) {

            }

            @Override
            public void didPressedAttachButton() {

            }

            @Override
            public void needStartRecordVideo(int state) {

            }

            @Override
            public void needChangeVideoPreviewState(int state, float seekProgress) {

            }

            @Override
            public void onSwitchRecordMode(boolean video) {

            }

            @Override
            public void onPreAudioVideoRecord() {

            }

            @Override
            public void needStartRecordAudio(int state) {

            }

            @Override
            public void needShowMediaBanHint() {

            }
        });


        return fragmentView=contentView;
    }
}
