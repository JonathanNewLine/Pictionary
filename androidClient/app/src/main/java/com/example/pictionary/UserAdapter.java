package com.example.pictionary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * This class represents a custom ArrayAdapter for User objects.
 */
public class UserAdapter extends ArrayAdapter<User> {
    private final Context context;

    /**
     * Constructor for the UserAdapter class.
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when instantiating views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated.
     */
    public UserAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        this.context = context;
    }

    /**
     * Provides a view for an adapter view (ListView, GridView, etc.)
     * @param position The position of the item within the adapter's data set of the item whose view we want.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    @SuppressLint({"ViewHolder", "SetTextI18n"})
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity)context).getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.user_view, parent, false);

        TextView usernameTitle = view.findViewById(R.id.textListViewUsername);
        TextView pointsTitle = view.findViewById(R.id.textListViewPoints);

        User temp = getItem(position);

        assert temp != null;
        usernameTitle.setText(temp.getUsername());
        pointsTitle.setText("Points: " + temp.getPoints());

        return view;
    }
}