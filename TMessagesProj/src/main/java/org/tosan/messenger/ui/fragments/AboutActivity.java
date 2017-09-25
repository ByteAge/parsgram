package org.tosan.messenger.ui.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;


public class AboutActivity extends BaseFragment {

    @Override
    public View createView(final Context context) {

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("AboutParsgram", R.string.AboutParsgram));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        View inflated = fragmentView = LayoutInflater.from(context).inflate(R.layout.activity_about, null);
        TextView version= (TextView) inflated.findViewById(R.id.versionTV);

        version.setText(BuildConfig.VERSION_NAME);

        View view=inflated.findViewById(R.id.rateUs);
        view.setBackground(Theme.getSelectorDrawable(true));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String packName=context.getPackageName();
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    try {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packName)));
                    }catch (ActivityNotFoundException ignored){}
                }
            }
        });
        view=inflated.findViewById(R.id.parsgramChannel);
        view.setBackground(Theme.getSelectorDrawable(true));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Browser.openUrl(context, "https://t.me/joinchat/AAAAAELcZvYQklCyTUVfdw");
            }
        });
        view=inflated.findViewById(R.id.callUs);
        view.setBackground(Theme.getSelectorDrawable(true));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String to="abednoorizadeh73@gmail.com";
                Intent intent = new Intent(Intent.ACTION_SEND, Uri.fromParts(
                        "mailto",to, null));
                intent.putExtra(Intent.EXTRA_SUBJECT, "");
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2){
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
                }else{
                    intent.putExtra(Intent.EXTRA_EMAIL, to);
                }
                intent.setType("text/plain");
                try {
                    context.startActivity(Intent.createChooser(intent, "ارسال ایمیل"));
                }catch (ActivityNotFoundException e){}
            }
        });

        return fragmentView;
    }
}
