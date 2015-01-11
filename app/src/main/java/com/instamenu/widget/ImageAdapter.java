package com.instamenu.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.instamenu.R;
import com.instamenu.content.Image;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marine1079 on 2014-11-30.
 */
public class ImageAdapter extends BaseAdapter {

    Context context;

    List<Image> images;

    LayoutInflater inflater;
    ImageLoader imageLoader;

    public ImageAdapter(Context context, LayoutInflater inflater, ImageLoader imageLoader) {
        this(context, inflater, imageLoader, new ArrayList<Image>());
    }

    public ImageAdapter(Context context, LayoutInflater inflater, ImageLoader imageLoader, List<Image> images) {
        this.context = context;
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

    public List<Image> getImages() {
        return images;
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

        final ViewHolder viewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.image = (ImageView) convertView.findViewById(R.id.grid_item_image);
            viewHolder.distance = (TextView) convertView.findViewById(R.id.grid_item_distance);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.stub)
                .showImageForEmptyUri(R.drawable.fail)
                .showImageOnFail(R.drawable.fail)
                //.resetViewBeforeLoading()// iv null set 하는건데, gc는 한꺼번에 하므로, 이렇게 조금이라도 더 하는게 좋을 것 같다. -> 뭔지 잘 모르겠지만 빼둠.
                .cacheInMemory(true)
                .cacheOnDisk(true)
                //.imageScaleType(ImageScaleType.IN_SAMPLE_INT)// set to target size(original img won't scaled)(default : 1/2)
                .bitmapConfig(Bitmap.Config.RGB_565)// default보다 2배 덜쓴다 한다.
                .build();

        imageLoader.displayImage("http://54.65.1.56:3639"+images.get(position).thumbnail, viewHolder.image, options, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                viewHolder.distance.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                viewHolder.distance.setVisibility(View.VISIBLE);
            }
        });
        viewHolder.distance.setText(String.valueOf(images.get(position).distance) + context.getResources().getString(R.string.distance_unit));

        return convertView;
    }
}