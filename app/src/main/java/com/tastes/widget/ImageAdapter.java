package com.tastes.widget;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.devspark.robototextview.widget.RobotoTextView;
import com.google.android.gms.maps.model.LatLng;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.tastes.R;
import com.tastes.app.MainActivity;
import com.tastes.content.Image;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.tastes.util.LogWrapper;

import org.apache.http.conn.HttpHostConnectException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by marine1079 on 2014-11-30.
 */
public class ImageAdapter extends BaseAdapter {

    Context context;

    List<Image> images;

    LayoutInflater inflater;
    ImageLoader imageLoader;

    //boolean internal;// 내부 gallery에서 받는지, 외부 server에서 받는지.
    Cursor cursor;

    // 그냥 쓸 수도 있지만, location은 최대한 arg로 쓰는게 좋다.
    //double latitude;
    //double longitude;

    DisplayImageOptions options;

    public ImageAdapter(Context context, LayoutInflater inflater, ImageLoader imageLoader) {
        this(context, inflater, imageLoader, new ArrayList<Image>(), null/*, 0, 0*/);
    }

    public ImageAdapter(Context context, LayoutInflater inflater, ImageLoader imageLoader, Cursor cursor/*, double latitude, double longitude*/) {
        this(context, inflater, imageLoader, null, cursor/*, latitude, longitude*/);
    }

    public ImageAdapter(Context context, LayoutInflater inflater, ImageLoader imageLoader, List<Image> images, Cursor cursor/*, double latitude, double longitude*/) {
        this.context = context;
        this.inflater = inflater;
        this.imageLoader = imageLoader;
        this.images = images;
        //this.internal = internal;
        this.cursor = cursor;
        //this.latitude = latitude;
        //this.longitude = longitude;

        options = new DisplayImageOptions.Builder()
                //.showImageOnLoading(R.drawable.stub)
                .resetViewBeforeLoading(true)// iv null set 하는건데, gc는 한꺼번에 하므로, 이렇게 조금이라도 더 하는게 좋을 것 같다. -> 뭔지 잘 모르겠지만 빼둠.
                .showImageForEmptyUri(R.drawable.fail)
                .showImageOnFail(R.drawable.fail)
                        //TODO: STRONG REF만 CACHE하는 LRU MEM CACHE로 해보도록.
                .cacheInMemory(false)// images, cursor의 null 상태가 서로 확실히 not 관계는 아니지만, 어차피 둘다 null일 때는 이쪽으로 올 일이 없다고 생각.
                .cacheOnDisk(false)
                .imageScaleType(ImageScaleType.EXACTLY) // 속도, 메모리 절약 위해.(not stretched. computed later at center crop)
                //.bitmapConfig(Bitmap.Config.RGB_565)// default보다 2배 덜쓴다 한다. -> 너무 누렇게 나온다.
                //.displayer(new FadeInBitmapDisplayer(500)) // 여긴 넣어두는게 자연스럽게 쌓이는 것 같아 보일 것 같다.
                //.considerExifParams(true) 되면 쓰겠지만 안되서 직접 만들었다.
                .build();
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
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
        return images != null ? images.size() : cursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        return images != null ? images.get(position) : position;//TODO: ((Object) position) 솔직히 쓸 일 없는 것 같다.(문제되는지 주시.)
    }

    @Override
    public long getItemId(int position) {
        return 0;//TODO: 이건 쓸 일이 없는 것 같다.(문제되는지 주시.)
    }

    public static class ViewHolder {
        public ImageView image;
        public TextView distance;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

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

        if(images != null) {
            final Image image = images.get(position);
            String uri = "http://54.65.1.56:3639" + image.thumbnail;

            imageLoader.displayImage(uri, viewHolder.image, options, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    viewHolder.distance.setVisibility(View.GONE);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    viewHolder.distance.setVisibility(View.VISIBLE);
                    viewHolder.distance.setText(String.valueOf(image.distance) + context.getResources().getString(R.string.distance_unit));
                }
            });
        } else {
            cursor.moveToPosition((getCount() - 1) - position);

            final String origin = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));

            //int rotation = rotateImageIfNeeded(origin, viewHolder.image);
            //convertView.setTag(R.id.image_adapter_tag_rotation, rotation);

            final long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            String thumbnail = null;// 일단 먼저 uri부터 채울 시도를 한다.
            //Uri uri_ = null;// 무조건 not null 아니다.
            final Cursor cursor_ = MediaStore.Images.Thumbnails.queryMiniThumbnail(context.getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            if(cursor_ != null && cursor_.moveToFirst()) {// 실질적으로 thumbnail이 있으면.
                String path = cursor_.getString(cursor_.getColumnIndex(MediaStore.Images.Thumbnails._ID));
                final Uri uri_ = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, path);
                thumbnail = uri_.toString();

                imageLoader.displayImage(thumbnail, viewHolder.image, options, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        //super.onLoadingComplete(imageUri, view, loadedImage);
                        int rotation = rotateImageIfNeeded(origin, viewHolder.image);
                        viewHolder.image.setTag(rotation);
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        Bitmap bitmap = getBitmapFromInfo(id, origin);

                        if (bitmap != null) {// 그래도 null인 경우는 이렇게 check해줘야 비어있지 않고 failure img로 set된다.
                            viewHolder.image.setImageBitmap(bitmap);
                        }
                    }
                });
                //viewHolder.image.setImageURI(uri_);
            } else {// 없으면 새로 만들도록 한다.(display의 fail로 안넘어가길래 이렇게 뺐다. 중복은 이게 최소다.)
                Bitmap bitmap = getBitmapFromInfo(id, origin);

                if (bitmap != null) {// 그래도 null인 경우는 이렇게 check해줘야 비어있지 않고 failure img로 set된다.
                    viewHolder.image.setImageBitmap(bitmap);
                } else {// 물론 not null이겠지만, path 잘못되어서 null일 가능성도 있다. display와 달리 수동으로 img set.
                    viewHolder.image.setImageDrawable(context.getResources().getDrawable(R.drawable.fail));
                }
            }
            cursor_.close();
        }

        return convertView;
    }

    public int rotateImageIfNeeded(String path, View view) {
        int rotation = 0;

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            int width = options.outWidth;
            int height = options.outHeight;
            int factor = width > height ? 90 : 0;

            ExifInterface exifInterface = new ExifInterface(path);

            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

            if(orientation != -1) {
                int degree = 0;
                int temp;

                switch(orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        temp = width; width = height; height = temp;

                        degree = 90;

                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;

                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        temp = width; width = height; height = temp;

                        degree = 270;

                        break;
                }

                factor = width > height ? 90 : 0;// TODO: exif 있을 때에는 factor를 수정해줘야 한다.(가로 세로가 크기 뿐 아니라 degree에 의해서 함께 결정되므로)

                rotation = degree + factor;// TODO: 가로에 대해서는 90을 더해서 돌려준다.(세로는 그냥 맞춰서 돌려준다는 뜻도 있음.)
            } else {
                rotation = factor;// TODO: EXIF 없는 스샷 등은 방향으로만 돌려준다.(세로는 그냥 놔두면 된다는 뜻도 있음.)
            }

            view.setRotation(rotation);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rotation;
    }

    // id, path 등 여러 information들로부터 thumbnail을 최대한 뽑는다.
    public Bitmap getBitmapFromInfo(long id, String path) {
        Bitmap bitmap = null;

        bitmap = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), id, MediaStore.Images.Thumbnails.MICRO_KIND, null);

        if (bitmap == null) {
            bitmap = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            if(bitmap == null) {
                bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path), 240, 240);
            }
        }

        return bitmap;
    }
}