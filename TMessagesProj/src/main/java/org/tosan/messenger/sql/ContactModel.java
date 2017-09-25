package org.tosan.messenger.sql;

import java.util.List;

import co.uk.rushorm.core.RushObject;
import co.uk.rushorm.core.annotations.RushList;

public class ContactModel extends RushObject {

    public int uid;
    public String displayName;
    public String username;

    public String pic = "" ;
    public String status = "" ;
    public String phone = "" ;
    public long updatedAt;

    @RushList(classType = ContactChangeModel.class)
    public List<ContactChangeModel> contactChanges;

}
