package com.instamenu.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.instamenu.R;
import com.instamenu.content.Image;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marine1079 on 2014-11-30.
 */
public class ImageAdapter extends BaseAdapter {

    List<Image> images;

    LayoutInflater inflater;
    ImageLoader imageLoader;

    public ImageAdapter(LayoutInflater inflater, ImageLoader imageLoader) {
        this(inflater, imageLoader, new ArrayList<Image>());
    }

    public ImageAdapter(LayoutInflater inflater, ImageLoader imageLoader, List<Image> images) {
        this.inflater = inflater;
        this.imageLoader = imageLoader;
        this.images = images;
    }

    public void setImages(List<Image> images) {
        if(images == null) {
            images = new ArrayList<Image>();
        }

        this.images = images;

        notifyDataSetChanged();
    }

    public void addImage(Image image) {
        if(images == null) {
            images = new ArrayList<Image>();
        }

        images.add(image);

        notifyDataSetChanged();// 되는지 보기.
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public Object getItem(int position) {
        return images.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public static class ViewHolder {
        public ImageView image;
        public TextView distance;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.image = (ImageView) convertView.findViewById(R.id.grid_item_image);
            viewHolder.distance = (TextView) convertView.findViewById(R.id.grid_item_distance);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //viewHolder.image.setImageResource(R.drawable.ic_launcher);
        imageLoader.displayImage("http://54.65.1.56:3639"+images.get(position).thumbnail, viewHolder.image);
        viewHolder.distance.setText(String.valueOf(images.get(position).distance));

        return convertView;
    }
}