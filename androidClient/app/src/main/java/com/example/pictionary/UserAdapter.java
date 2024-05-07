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

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends ArrayAdapter<User> {

    private final Context context;

    public UserAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        this.context = context;
    }

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
