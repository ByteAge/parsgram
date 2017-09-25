package org.tosan.messenger.ui.fragments;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.PasscodeView;

/**
 * Created by RaminBT on 21/04/2017.
 */

public class PasscodeVerifyFragment extends BaseFragment {

    PasscodeView passcodeView;
    private int passcodeType;
    private String password;
    private VerifyDelegate delegate;

    public PasscodeVerifyFragment(int passcodeType, String password, VerifyDelegate delegate) {
        this.passcodeType = passcodeType;
        this.password = password;
        this.delegate = delegate;
    }

    @Override
    public View createView(Context context) {

        actionBar.setVisibility(View.GONE);

        fragmentView=new FrameLayout(context);
        FrameLayout fl= (FrameLayout) fragmentView;

        passcodeView=new PasscodeView(context);
        passcodeView.setDelegate(new PasscodeView.PasscodeViewDelegate() {
            @Override
            public void didAcceptedPassword() {
                delegate.onVerify();
                finishFragment();
            }

            @Override
            public boolean checkPasscode(String w) {
                return w.equals(password);
            }

            @Override
            public int getPasswordType() {
                return passcodeType;
            }

            @Override
            public boolean useFingerPrint() {
                return false;
            }
        });
        fl.addView(passcodeView, -1,-1);

        return fragmentView;
    }


    @Override
    public void onResume() {
        super.onResume();

        if(passcodeView==null)
            return;
        passcodeView.onShow();
        passcodeView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(passcodeView==null)
            return;
        passcodeView.onPause();
    }

    public interface VerifyDelegate{
        void onVerify();
    }
}
