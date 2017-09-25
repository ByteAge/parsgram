package org.tosan.messenger.sql;


import co.uk.rushorm.core.RushObject;

public class CatModel extends RushObject {

    public String name;
    public String dialogs;
    /* Lists must have @RushList annotation with classType,
        	listType can also be added the default is ArrayList */
//    @RushList(classType = DialogModel.class)
//    public List<DialogModel> childModels;;


}
