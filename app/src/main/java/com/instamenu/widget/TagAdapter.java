package com.instamenu.widget;

import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.instamenu.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by marine1079 on 2014-11-30.
 */
public class TagAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {

    List<String> tags;
    List<String> switches;

    boolean add = false;
    List<String> tags_;// for only add.(item frag)(not used for representing list)

    boolean switch_ = false;

    LayoutInflater inflater;

    public TagAdapter(LayoutInflater inflater) {
        this(inflater, null, null);

        switch_ = false;
    }

    public TagAdapter(LayoutInflater inflater, List<String> tags) {
        this(inflater, tags, null);

        add = true;

        switch_ = false;
    }

    public TagAdapter(LayoutInflater inflater, List<String> tags, List<String> switches) {
        this.inflater = inflater;
        // ref 그대로 갖고오면 main activity의 list들까지 변하게 된다. 이렇게 복제한다.
        this.tags = tags != null ? new ArrayList<String>(tags) : null;
        this.switches = switches != null ? new ArrayList<String>(switches) : null;

        switch_ = true;
    }

    public void addTag(String tag) {
        addTag(tag, "true");
    }

    public void addTag(String tag, String switch_) {
        if(tags == null) {
            tags = new ArrayList<String>();
        }

        if(switches == null) {
            switches = new ArrayList<String>();
        }

        tags.add(tag);
        switches.add(switch_);

        if(add == true) {
            if(tags_ == null) {
                tags_ = new ArrayList<String>();
            }

            tags_.add(tag);
        }

        notifyDataSetChanged();// 되는지 보기.
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getSwitches() {
        return switches;
    }

    public List<String> getTags_() {
        return tags_;
    }

    @Override
    public int getCount() {
        return tags != null ? tags.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return tags.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(buttonView.getTag() != null) {
            int index = (Integer) buttonView.getTag();
            switches.set(index, isChecked ? "true" : "false");
        }
    }

    public static class ViewHolder {
        public TextView tag;
        public SwitchCompat switch_;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.list_row_tag, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.tag = (TextView) convertView.findViewById(R.id.list_row_tag);
            if(switch_ == true) {
                viewHolder.switch_ = (SwitchCompat) convertView.findViewById(R.id.list_row_switch);
                viewHolder.switch_.setVisibility(View.VISIBLE);
            }

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.tag.setText(tags.get(position));
        if(switch_ == true) { // 아직 scroll시 움직임은 남아있다. 하지만 아직 답이 없으므로 그대로 간다.
            viewHolder.switch_.setTag(position);// for accessing to switch list.
            viewHolder.switch_.setOnCheckedChangeListener(null);// programmatically checked는 굳이 switch list change 해줄 필요 없으므로 이렇게 간다.
            viewHolder.switch_.setChecked(switches.get(position).equals("true"));
            viewHolder.switch_.setOnCheckedChangeListener(this);// 굳이 fragment까지 갈 필요 없다. 여기서 switch list 제어해주면 되므로.
        }

        return convertView;
    }
}