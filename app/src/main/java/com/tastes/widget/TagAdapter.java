package com.tastes.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
//import android.support.v7.widget.SwitchCompat;
import android.widget.TextView;

import com.devspark.robototextview.widget.RobotoTextView;
import com.tastes.R;
import com.tastes.content.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marine1079 on 2014-11-30.
 */
public class TagAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    List<String> tags;
    List<String> switches;

    boolean add = false;
    List<String> tags_;// for only add.(item frag)(not used for representing list)

    boolean switch_ = false;

    LayoutInflater inflater;
    View.OnClickListener listener;

    //private final String HEADER = "#";

    public TagAdapter(LayoutInflater inflater) {
        this(null, inflater, null, null);

        switch_ = false;
    }

    public TagAdapter(LayoutInflater inflater, List<String> tags) {
        this(null, inflater, tags, null);

        add = true;

        switch_ = false;
    }

    public TagAdapter(View.OnClickListener listener, LayoutInflater inflater, List<String> tags, List<String> switches) {
        this.listener = listener;
        this.inflater = inflater;

        initTags(tags, switches);
    }

    // noti 할 필요 없어서 이렇게 했다.
    public void initTags(List<String> tags, List<String> switches) {
        // ref 그대로 갖고오면 main activity의 list들까지 변하게 된다. 이렇게 복제한다. => 이미 filter는 main의 것을 받아와서 새로 정의하므로 괜찮다.
        //this.tags = tags != null ? new ArrayList<String>(tags) : null;
        //this.switches = switches != null ? new ArrayList<String>(switches) : null;
        this.tags = tags;
        this.switches = switches;

        switch_ = true;
    }

    public void setTags(List<String> tags, List<String> switches) {
        initTags(tags, switches);

        notifyDataSetChanged();// 되는지 보기.
    }

    public void setSwitches(boolean enabled) {
        if(tags != null) {
            for(int i = 0; i< switches.size(); i++) {
                switches.set(i, enabled ? "true" : "false");
            }

            notifyDataSetChanged();// 되는지 보기.
        }
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

    public void toggleTag(int index) {
        if(switches.get(index).equals("true")) {
            switches.set(index, "false");
        } else {
            switches.set(index, "true");
        }

        notifyDataSetChanged();
    }

    public void toggleTag(View view) {
        CheckBox switch_ = ((ViewHolder) view.getTag()).switch_;
        //Switch switch_ = ((ViewHolder) view.getTag()).switch_;

        switch_.setChecked(!switch_.isChecked());
    }

    public void removeTag(int index) { // index 있다는 말은 null check 필요없다는 말이다.
        if(index < getCount()) {
            tags.remove(index);
            switches.remove(index);

            // dismiss method 쪽에서 noti를 따로 하기 때문에 일단 빼놨다.(연속 삭제 때문인 듯.)
            //notifyDataSetChanged();
        }
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

    @Override
    public void onClick(View v) {
        CheckBox switch_ = ((ViewHolder) v.getTag()).switch_;
        //Switch switch_ = ((ViewHolder) v.getTag()).switch_;

        switch_.setChecked(!switch_.isChecked());
    }

    public static class ViewHolder {
        public RobotoTextView tag;
        //public Switch switch_;
        public CheckBox switch_;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.list_row_tag, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.tag = (RobotoTextView) convertView.findViewById(R.id.list_row_tag);
            viewHolder.tag.setOnClickListener(listener);

            if(switch_ == true) {
                //viewHolder.switch_ = (Switch) convertView.findViewById(R.id.list_row_switch);
                viewHolder.switch_ = (CheckBox) convertView.findViewById(R.id.list_row_check);
                viewHolder.switch_.setVisibility(View.VISIBLE);
            }

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.tag.setText(Tag.HEADER + tags.get(position));
        viewHolder.tag.setTag(tags.get(position));// listener 등록과 이것 사이가 error 발생 시킬 수 있을지는 두고본다.

        if(switch_ == true) { // 아직 scroll시 움직임은 남아있다. 하지만 아직 답이 없으므로 그대로 간다.
            viewHolder.switch_.setTag(position);// for accessing to switch list.
            viewHolder.switch_.setOnCheckedChangeListener(null);// programmatically checked는 굳이 switch list change 해줄 필요 없으므로 이렇게 간다.
            viewHolder.switch_.setChecked(switches.get(position).equals("true"));
            viewHolder.switch_.setOnCheckedChangeListener(this);// 굳이 fragment까지 갈 필요 없다. 여기서 switch list 제어해주면 되므로.
        }

        return convertView;
    }
}