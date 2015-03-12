package com.tastes.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.devspark.robototextview.widget.RobotoTextView;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.tastes.R;
import com.tastes.content.Image;
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
            viewHolder.distance = (RobotoTextView) convertView.findViewById(R.id.grid_item_distance);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.stub)
                .showImageForEmptyUri(R.drawable.fail)
                .showImageOnFail(R.drawable.fail)
                //.resetViewBeforeLoading()// iv null set 하는건데, gc는 한꺼번에 하므로, 이렇게 조금이라도 더 하는게 좋을 것 같다. -> 뭔지 잘 모르겠지만 빼둠.
                .cacheInMemory(true) // 이건 작으니까 일단 해제하지 않고 놔둬본다.
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY) // 속도, 메모리 절약 위해.(not stretched. computed later at center crop)
                .bitmapConfig(Bitmap.Config.RGB_565)// default보다 2배 덜쓴다 한다. -> 너무 누렇게 나온다.
                //.displayer(new FadeInBitmapDisplayer(500)) // 여긴 넣어두는게 자연스럽게 쌓이는 것 같아 보일 것 같다.
                .build();

        imageLoader.displayImage("http://54.65.1.56:3639"+images.get(position).thumbnail, viewHolder.image, options, new SimpleImageLoadingListener() {
        //imageLoader.displayImage("http://54.65.1.56:3639"+images.get(position).thumbnail, new ImageViewAware(viewHolder.image, false), options, new SimpleImageLoadingListener() {
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