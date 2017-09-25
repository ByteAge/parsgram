package org.tosan.messenger.sql;

import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;

import co.uk.rushorm.core.RushObject;


public class ContactChangeModel extends RushObject {

    public static final int USERNAME=3, PHOTO=1, PHONE=2, STATUS=4;

    public int userId;
    public int type;
    public String time;

    public String phone;
    public String photoSmall;
    public String photoBig;
    public String username;
    public String status;



    public static String fileLocationToString(TLRPC.FileLocation fileLocation){
        String str=fileLocation.dc_id+"#";
        str+=fileLocation.local_id+"#";
        str+=fileLocation.volume_id+"#";
        str+=fileLocation.secret;
        return str;
    }

    public TLRPC.FileLocation getFileLocation(boolean small){
        if(small && TextUtils.isEmpty(photoSmall))
            return null;
        if(!small && TextUtils.isEmpty(photoBig))
            return null;
        TLRPC.TL_fileLocation fileLocation=new TLRPC.TL_fileLocation();
        String[] args=small? photoSmall.split("#") : photoBig.split("#");

        fileLocation.dc_id=Integer.parseInt(args[0]);
        fileLocation.local_id=Integer.parseInt(args[1]);
        fileLocation.volume_id=Long.parseLong(args[2]);
        fileLocation.secret=Long.parseLong(args[3]);

        return fileLocation;
    }

}
