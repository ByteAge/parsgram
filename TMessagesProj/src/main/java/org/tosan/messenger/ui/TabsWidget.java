package org.tosan.messenger.ui;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.CallSuper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.tosan.messenger.Tosan;


public class TabsWidget extends LinearLayout implements NotificationCenter.NotificationCenterDelegate {

    private LayoutParams params;
    private CoutableImageView currentTab;

    private ColorDrawable background;
    private ArgbEvaluator colorEvaluator=new ArgbEvaluator();

    private boolean paintUnreads=true;
    private Paint unreadsPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int unreadsBgColor, unreadsTextColor;
    private boolean shareTab;

    public TabsWidget(Context context) {
        this(context, false);
    }
    public TabsWidget(Context context, boolean insideShareTabs) {
        super(context);
        shareTab=insideShareTabs;
        params= LayoutHelper.createLinear(0, 40, 1f);

        background=new ColorDrawable();
        setBackground(background);
    }

    public int getCurrentTab(){
        return shareTab ? 0 :  Tosan.prefs.getInt(Tosan.key_last_tab_index, 0);
    }



    public void colorsUpdated(){
        int tabColor= shareTab ? Theme.getColor(Theme.key_dialogTextBlack) : Theme.getColor(Theme.key_actionBarDefaultIcon);
        int acColor=  shareTab ? Theme.getColor(Theme.key_dialogBackground) : Theme.getColor(Theme.key_actionBarDefault);

        unreadsBgColor = (int) colorEvaluator.evaluate(.4f, Theme.getColor(Theme.key_actionBarDefaultSelector), Color.BLACK);
        unreadsTextColor=tabColor;

        ColorFilter filter=new PorterDuffColorFilter(tabColor, PorterDuff.Mode.SRC_IN);
        ColorFilter alphaFilter=new PorterDuffColorFilter((Integer) colorEvaluator.evaluate(.5f, tabColor, acColor), PorterDuff.Mode.SRC_IN);

        for (int i = 0; i < getChildCount(); i++) {
            View child=getChildAt(i);
            if(child instanceof CoutableImageView){

                CoutableImageView iv= (CoutableImageView) child;
                iv.counterTV.setTextColor(unreadsTextColor);

                if(iv == currentTab){
                    iv.imageView.setColorFilter(filter);
                }else iv.imageView.setColorFilter(alphaFilter);

                iv.counterTV.setTextColor(unreadsTextColor);
                iv.counterBGD.setColor(unreadsBgColor);
                iv.counterBGD.setStroke(AndroidUtilities.dp(.7f), unreadsTextColor);
            }
        }
        background.setColor(acColor);
    }

    public CoutableImageView addTab(int iconRes){
//        Bitmap img;
//        img= BitmapFactory.decodeResource(getResources(), iconRes);

        CoutableImageView iv=new CoutableImageView(getContext(), iconRes);
        iv.setOnClickListener(onTabClicked);
        if(!shareTab)
            iv.setOnLongClickListener(onTabLongClicked);
        iv.setBackground(shareTab ? Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), 0) : Theme.createSelectorDrawable(Theme.getColor(Theme.key_actionBarWhiteSelector), 2));
        addView(iv, params) ;
        return iv;
    }

    @CallSuper
    public void selectTab(int index){
        if(!shareTab)
            Tosan.prefs.edit().putInt(Tosan.key_last_tab_index, index).apply();
    }


    private OnClickListener onTabClicked=new OnClickListener() {
        @Override
        public void onClick(View view) {
        }
    };
    private OnLongClickListener onTabLongClicked=new OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            return true;
        }
    };

    public void countsUpdated(){
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
//        if(id == NotificationCenter.dialogsNeedReload){
            countsUpdated();
//        }
    }


    public class CoutableImageView extends FrameLayout {

        public ImageView imageView;
        public TextView counterTV;
        public GradientDrawable counterBGD;

        public CoutableImageView(Context context, int iconRes) {
            super(context);
        }
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            counterBGD.setCornerRadius(counterTV.getMeasuredHeight()/2);
        }

    }
}
