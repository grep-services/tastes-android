package com.instamenu.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.instamenu.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by marine1079 on 2014-11-30.
 */
public class TagAdapter extends BaseAdapter {

    List<String> tags;
    HashMap<String, String> switches;// 굳이 str?

    LayoutInflater inflater;

    public TagAdapter(LayoutInflater inflater) {
        this(inflater, new ArrayList<String>(), null);
    }

    public TagAdapter(LayoutInflater inflater, List<String> tags) {
        this(inflater, tags, null);
    }

    public TagAdapter(LayoutInflater inflater, List<String> tags, HashMap<String, String> switches) {
        this.inflater = inflater;
        this.tags = tags;
        this.switches = switches;
    }

    public void addTag(String tag) {
        if(tags == null) {
            tags = new ArrayList<String>();
        }

        tags.add(tag);

        notifyDataSetChanged();// 되는지 보기.
    }

    @Override
    public int getCount() {
        return tags.size();
    }

    @Override
    public Object getItem(int position) {
        return tags.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public static class ViewHolder {
        public TextView tag;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.list_row_tag, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.tag = (TextView) convertView.findViewById(R.id.list_row_tag);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.tag.setText(tags.get(position));

        return convertView;
    }
}