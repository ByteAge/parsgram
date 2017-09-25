package org.tosan.messenger.sql;

import co.uk.rushorm.core.RushObject;

public class DialogModel extends RushObject {

    public static final int FAVOR=0, HIDDEN=1, LOCKED=2, NONE=-1;

    public long did;
    public int type;
    public int passwordType;
    public String password;

    public DialogModel() {
    }

    public DialogModel(long did, int type) {
        this.did = did;
        this.type = type;
    }

}
