package org.tosan.messenger.ui.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.LetterSectionCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.PhotoViewer;
import org.tosan.messenger.sql.ContactChangeModel;
import org.tosan.messenger.sql.ContactModel;
import org.tosan.messenger.ui.ContactModelCell;

import co.uk.rushorm.core.RushCore;


public class ContactChangesActivity extends BaseFragment{

    private ChangesAdapter listViewAdapter;
    private RecyclerListView listView;
    private EmptyTextProgressView emptyView;

    public ContactModel contactModel;
    public ContactsChangesActivity delegate;

    public ContactChangesActivity(ContactModel contactModel) {
        this.contactModel = contactModel;
    }

    @Override
    public View createView(final Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
//        String title = "تغییرات " + contactModel.displayName;
        String title=contactModel.displayName;
        actionBar.setTitle(title);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        actionBar.createMenu().addItem(11, R.drawable.ic_ab_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(actionBar.getContext())
                        .setTitle("پارس گرام")
                        .setMessage("آیا از حذف تغییرات این مخاطب اطمینان دارید؟")
                        .setPositiveButton("بلی", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                RushCore.getInstance().delete(contactModel.contactChanges);
                                contactModel.contactChanges.clear();
                                contactModel.save();
                                if(delegate!=null)
                                    delegate.onChangesCleared();
                                finishFragment();
                            }
                        })
                        .setNegativeButton("خیر", null)
                        .show();
            }
        });


        listViewAdapter=new ChangesAdapter();
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        emptyView = new EmptyTextProgressView(context);
        emptyView.setShowAtCenter(true);
        emptyView.showTextView();
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context);
        listView.setEmptyView(emptyView);
        listView.setSectionsType(1);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listViewAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(final View view, final int position) {
                final ContactChangeModel change=contactModel.contactChanges.get(position);
                final CharSequence[] args;
                if(change.type == ContactChangeModel.PHOTO){
                    args=new CharSequence[]{
                            "ارسال پیام",
                            "مشاهده عکس تغییر کرده",
                            "حذف",
                    };
                }else{
                    args=new CharSequence[]{
                            "ارسال پیام",
                            "حذف",
                    };
                }
                new AlertDialog.Builder(context)
                        .setTitle(contactModel.displayName)
                        .setItems(args, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    Bundle args=new Bundle();
                                    args.putInt("user_id", contactModel.uid);
                                    presentFragment(new ChatActivity(args));
                                }else if(args.length==3 && i==1){
                                    // show changed picture
                                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                                    TLRPC.FileLocation fl=change.getFileLocation(false);
                                    PhotoViewer.getInstance().openPhoto(change.getFileLocation(false), new PhotoViewer.EmptyPhotoViewerProvider(){
                                        @Override
                                        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
                                            int count = listView.getChildCount();
                                                ImageReceiver imageReceiver = null;
                                                if(view instanceof ContactModelCell){
                                                    ContactModelCell cell= (ContactModelCell) view;
                                                    imageReceiver=cell.avatarImageView.getImageReceiver();
                                                    if (imageReceiver != null) {
                                                        int coords[] = new int[2];
                                                        cell.avatarImageView.getLocationInWindow(coords);
                                                        PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                                                        object.viewX = coords[0];
                                                        object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? 0 : AndroidUtilities.statusBarHeight);
                                                        object.parentView = listView;
                                                        object.imageReceiver = imageReceiver;
                                                        object.thumb = imageReceiver.getBitmap();
                                                        object.radius = imageReceiver.getRoundRadius();
                                                        if (view instanceof ContactModelCell) {
                                                            object.dialogId = -contactModel.uid;
                                                        }
//                                                    if (pinnedMessageView != null && pinnedMessageView.getTag() == null || reportSpamView != null && reportSpamView.getTag() == null) {
                                                        object.clipTopAddition = AndroidUtilities.dp(48);
//                                                    }
                                                        return object;
                                                    }

                                            }
                                            return null;
                                        }
                                    });
                                }else{
                                    // delete change
                                    new AlertDialog.Builder(context)
                                            .setTitle(LocaleController.getString("AppName", R.string.AppName))
                                            .setMessage("آیا مطمئنید که میخواهید این تغییر را حذف کنید؟")
                                            .setNegativeButton("خیر", null)
                                            .setPositiveButton("بلی", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    contactModel.contactChanges.remove(position);
                                                    change.delete();
                                                    if(contactModel.contactChanges.isEmpty()){
                                                        contactModel.contactChanges.clear();
                                                        contactModel.save();
                                                        if(delegate!=null)
                                                            delegate.onChangesCleared();
                                                        finishFragment();
                                                    }else{
                                                        listViewAdapter.notifyDataSetChanged();
                                                    }
                                                }
                                            })
                                            .show();
                                }
                            }
                        })
                        .show();

            }
        });

        return fragmentView;
    }

    public class ChangesAdapter extends RecyclerListView.SectionsAdapter{

        @Override
        public void onBindViewHolder(int section, int position, RecyclerView.ViewHolder holder) {

            ContactChangeModel change=contactModel.contactChanges.get(position);
            ContactModelCell cell= (ContactModelCell) holder.itemView;
            cell.setData(change, contactModel);
        }

        @Override
        public View getSectionHeaderView(int section, View view) {
            if (view == null) {
                view = new LetterSectionCell(listView.getContext());
            }
            LetterSectionCell cell = (LetterSectionCell) view;
            cell.setLetter("");
            return view;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            return new ChangeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_change, parent, false));
            return new RecyclerView.ViewHolder(new ContactModelCell(parent.getContext(), 16)) {};
        }

        @Override
        public String getLetter(int position) {
            return null;
        }

        @Override
        public int getPositionForScrollProgress(float progress) {
            return (int) (contactModel.contactChanges.size()*progress);
        }

        @Override
        public int getSectionCount() {
            return 1;
        }

        @Override
        public int getCountForSection(int section) {
            return contactModel.contactChanges.size();
        }

        @Override
        public boolean isEnabled(int section, int row) {
            return true;
        }

        @Override
        public int getItemViewType(int section, int position) {
            return 0;
        }

        @Override
        public Object getItem(int section, int position) {
            return contactModel.contactChanges.get(position);
        }
    }

}
