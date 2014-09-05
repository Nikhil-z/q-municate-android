package com.quickblox.q_municate.ui.friends;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.quickblox.q_municate.R;
import com.quickblox.q_municate.db.DatabaseManager;
import com.quickblox.q_municate.db.tables.FriendTable;
import com.quickblox.q_municate.model.User;
import com.quickblox.q_municate.qb.helpers.QBFriendListHelper;
import com.quickblox.q_municate.ui.views.RoundedImageView;
import com.quickblox.q_municate.utils.Consts;
import com.quickblox.q_municate.utils.TextViewHelper;

public class FriendsListCursorAdapter extends CursorTreeAdapter {

    public static final String HEADER_COLUMN_ID = "_id";
    public static final String HEADER_COLUMN_STATUS_NAME = "status_name";
    public static final String HEADER_COLUMN_HEADER_NAME = "header_name";

    private LayoutInflater layoutInflater;
    private String searchCharacters;
    private Context context;
    private Resources resources;
    private FriendsListFragment.FriendSelectListener friendSelectListener;
    private int relationStatusNoneId;

    public FriendsListCursorAdapter(Context context, Cursor cursor, FriendsListFragment.FriendSelectListener friendSelectListener) {
        super(cursor, context, true);
        layoutInflater = LayoutInflater.from(context);
        this.context = context;
        this.friendSelectListener = friendSelectListener;
        resources = context.getResources();
        relationStatusNoneId = DatabaseManager.getRelationStatusIdByName(context, QBFriendListHelper.RELATION_STATUS_NONE);
    }

    public void setSearchCharacters(String searchCharacters) {
        this.searchCharacters = searchCharacters;
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        int relationStatusId = groupCursor.getInt(groupCursor.getColumnIndex(HEADER_COLUMN_ID));

        if (relationStatusId == relationStatusNoneId) {
            return DatabaseManager.getAllFriends(context, relationStatusId);
        } else {
            return DatabaseManager.getAllFriends(context);
        }
    }

    @Override
    public View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
        View mView = layoutInflater.inflate(R.layout.view_section_title_friends_list, null);
        TextView tvGrp = (TextView) mView.findViewById(R.id.list_title_textview);
        tvGrp.setText(cursor.getString(cursor.getColumnIndex(HEADER_COLUMN_HEADER_NAME)));
        return mView;
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
        TextView tvGrp = (TextView) view.findViewById(R.id.list_title_textview);
        tvGrp.setText(cursor.getString(cursor.getColumnIndex(HEADER_COLUMN_HEADER_NAME)));
    }

    @Override
    public View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
        View view = layoutInflater.inflate(R.layout.list_item_friend, null, true);

        ViewHolder viewHolder = new ViewHolder();

        viewHolder.avatarImageView = (RoundedImageView) view.findViewById(R.id.avatar_imageview);
        viewHolder.fullNameTextView = (TextView) view.findViewById(R.id.name_textview);
        viewHolder.statusTextView = (TextView) view.findViewById(R.id.status_textview);
        viewHolder.addFriendImageView = (ImageView) view.findViewById(R.id.add_friend_imagebutton);
        viewHolder.acceptFriendImageView = (ImageView) view.findViewById(R.id.accept_friend_imagebutton);
        viewHolder.rejectFriendImageView = (ImageView) view.findViewById(R.id.reject_friend_imagebutton);
        viewHolder.onlineImageView = (ImageView) view.findViewById(R.id.online_imageview);

        view.setTag(viewHolder);

        return view;
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        final User user = DatabaseManager.getFriendFromCursor(cursor);

        int relationStatusId = cursor.getInt(cursor.getColumnIndex(FriendTable.Cols.RELATION_STATUS_ID));

        viewHolder.fullNameTextView.setText(user.getFullName());

        checkRelationStatus(viewHolder, relationStatusId, user);

        String avatarUrl = getAvatarUrlForUser(user);
        displayAvatarImage(avatarUrl, viewHolder.avatarImageView);

        initListeners(viewHolder, user.getUserId());

        if (!TextUtils.isEmpty(searchCharacters)) {
            TextViewHelper.changeTextColorView(context, viewHolder.fullNameTextView, searchCharacters);
        }
    }

    private void initListeners(ViewHolder viewHolder, final int userId) {
        viewHolder.addFriendImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                friendSelectListener.onAddUserClicked(userId);
            }
        });

        viewHolder.acceptFriendImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                friendSelectListener.onAcceptUserClicked(userId);
            }
        });

        viewHolder.rejectFriendImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                friendSelectListener.onRejectUserClicked(userId);
            }
        });
    }

    private void setStatusVisibility(ViewHolder viewHolder, boolean status) {
        if (status) {
            viewHolder.onlineImageView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.onlineImageView.setVisibility(View.INVISIBLE);
        }
    }

    private void checkRelationStatus(ViewHolder viewHolder, int relationStatusId, User user) {
        String status = null;
        String relationStatus = DatabaseManager.getRelationStatusNameById(context, relationStatusId);

        if (relationStatus.equals(QBFriendListHelper.RELATION_STATUS_NONE)) {
            viewHolder.acceptFriendImageView.setVisibility(View.VISIBLE);
            viewHolder.rejectFriendImageView.setVisibility(View.VISIBLE);
            status = resources.getString(R.string.frl_pending_status);
        } else if (relationStatus.equals(QBFriendListHelper.RELATION_STATUS_BOTH) || relationStatus.equals(QBFriendListHelper.RELATION_STATUS_FROM)) {
            viewHolder.acceptFriendImageView.setVisibility(View.GONE);
            viewHolder.rejectFriendImageView.setVisibility(View.GONE);
            setStatusVisibility(viewHolder, user.isOnline());
            status = user.getOnlineStatus();
        }

        viewHolder.statusTextView.setText(status);
    }

    private void displayAvatarImage(String uri, ImageView imageView) {
        ImageLoader.getInstance().displayImage(uri, imageView, Consts.UIL_USER_AVATAR_DISPLAY_OPTIONS);
    }

    private String getAvatarUrlForUser(User user) {
        return user.getAvatarUrl();
    }

    private static class ViewHolder {

        public RoundedImageView avatarImageView;
        public TextView fullNameTextView;
        public TextView statusTextView;
        public ImageView addFriendImageView;
        public ImageView acceptFriendImageView;
        public ImageView rejectFriendImageView;
        public ImageView onlineImageView;
    }
}