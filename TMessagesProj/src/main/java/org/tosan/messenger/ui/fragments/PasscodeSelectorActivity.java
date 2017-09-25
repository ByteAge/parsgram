package org.tosan.messenger.ui.fragments;

import org.telegram.ui.PasscodeActivity;


public class PasscodeSelectorActivity extends PasscodeActivity {

    public interface FreshPasscodeDelegate{
        void onNewPassword(String password, int passcodeType);
    }


    /**
     * @param type 0 = settings, 2 = verify password, 1 = new passcode
     */
    public PasscodeSelectorActivity(int type, final FreshPasscodeDelegate freshPasscodeDelegate ) {
        super(type);
        setDelegate(new PasscodeDelegate() {
            @Override
            public boolean isOverridingPassword() {
                return false;
            }

            @Override
            public int getPasswordType() {
                return 0;
            }

            @Override
            public void done(String password, int passcodeType) {
                freshPasscodeDelegate.onNewPassword(password, passcodeType);
                finishFragment();
            }

            @Override
            public boolean checkPasscode(String passcode) {
                return false;
            }
        });
    }


}
