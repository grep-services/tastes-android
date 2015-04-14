package com.tastes.widget;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
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
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.tastes.R;
import com.tastes.app.MainActivity;
import com.tastes.content.Image;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

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
    double latitude;
    double longitude;

    DisplayImageOptions options;

    public ImageAdapter(Context context, LayoutInflater inflater, ImageLoader imageLoader) {
        this(context, inflater, imageLoader, new ArrayList<Image>(), null, 0, 0);
    }

    public ImageAdapter(Context context, LayoutInflater inflater, ImageLoader imageLoader, Cursor cursor, double latitude, double longitude) {
        this(context, inflater, imageLoader, null, cursor, latitude, longitude);
    }

    public ImageAdapter(Context context, LayoutInflater inflater, ImageLoader imageLoader, List<Image> images, Cursor cursor, double latitude, double longitude) {
        this.context = context;
        this.inflater = inflater;
        this.imageLoader = imageLoader;
        this.images = images;
        //this.internal = internal;
        this.cursor = cursor;
        this.latitude = latitude;
        this.longitude = longitude;

        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.stub)
                .showImageForEmptyUri(R.drawable.fail)
                .showImageOnFail(R.drawable.fail)
                        //.resetViewBeforeLoading()// iv null set 하는건데, gc는 한꺼번에 하므로, 이렇게 조금이라도 더 하는게 좋을 것 같다. -> 뭔지 잘 모르겠지만 빼둠.
                        //TODO: STRONG REF만 CACHE하는 LRU MEM CACHE로 해보도록.
                .cacheInMemory(false)// images, cursor의 null 상태가 서로 확실히 not 관계는 아니지만, 어차피 둘다 null일 때는 이쪽으로 올 일이 없다고 생각.
                .cacheOnDisk(false)
                .imageScaleType(ImageScaleType.EXACTLY) // 속도, 메모리 절약 위해.(not stretched. computed later at center crop)
                .bitmapConfig(Bitmap.Config.RGB_565)// default보다 2배 덜쓴다 한다. -> 너무 누렇게 나온다.
                        //.displayer(new FadeInBitmapDisplayer(500)) // 여긴 넣어두는게 자연스럽게 쌓이는 것 같아 보일 것 같다.
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
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder viewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.image = (ImageView) convertView.findViewById(R.id.grid_item_image);
            viewHolder.distance = (RobotoTextView) convertView.findViewById(R.id.grid_item_distance);

            convertView.setTag(R.id.image_adapter_tag_holder, viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag(R.id.image_adapter_tag_holder);
        }

        if(images != null) {
            final Image image = images.get(position);
            String uri = "http://54.65.1.56:3639" + image.thumbnail;

            imageLoader.displayImage(uri, viewHolder.image, options, new SimpleImageLoadingListener() {
                //imageLoader.displayImage("http://54.65.1.56:3639"+images.get(position).thumbnail, new ImageViewAware(viewHolder.image, false), options, new SimpleImageLoadingListener() {
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
            final long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            String thumbnail = null;// 일단 먼저 uri부터 채울 시도를 한다.
            //Uri uri_ = null;// 무조건 not null 아니다.
            final Cursor cursor_ = MediaStore.Images.Thumbnails.queryMiniThumbnail(context.getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            if(cursor_ != null && cursor_.moveToFirst()) {// 실질적으로 thumbnail이 있으면.
                String path = cursor_.getString(cursor_.getColumnIndex(MediaStore.Images.Thumbnails._ID));
                Uri uri_ = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, path);
                thumbnail = uri_.toString();

                imageLoader.displayImage(thumbnail, viewHolder.image, options, new SimpleImageLoadingListener() {
                    //imageLoader.displayImage("http://54.65.1.56:3639"+images.get(position).thumbnail, new ImageViewAware(viewHolder.image, false), options, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        //super.onLoadingFailed(imageUri, view, failReason);
                        Bitmap bitmap = getBitmapFromInfo(id, origin);

                        if (bitmap != null) {// 그래도 null인 경우는 이렇게 check해줘야 비어있지 않고 failure img로 set된다.
                            viewHolder.image.setImageBitmap(bitmap);
                        }
                    }
                });
            } else {// 없으면 새로 만들도록 한다.(display의 fail로 안넘어가길래 이렇게 뺐다. 중복은 이게 최소다.)
                Bitmap bitmap = getBitmapFromInfo(id, origin);

                if (bitmap != null) {// 그래도 null인 경우는 이렇게 check해줘야 비어있지 않고 failure img로 set된다.
                    viewHolder.image.setImageBitmap(bitmap);
                } else {// 물론 not null이겠지만, path 잘못되어서 null일 가능성도 있다. display와 달리 수동으로 img set.
                    viewHolder.image.setImageDrawable(context.getResources().getDrawable(R.drawable.fail));
                }
            }
            cursor_.close();

            String time = String.valueOf(System.currentTimeMillis());
            // laglng 없을수도 있다.(시작하고 바로 켜면 주로 그럴듯.)
            double latitude_ = latitude;
            double longitude_ = longitude;
            long distance = -2;

            try {
                ExifInterface exifInterface = new ExifInterface(origin);

                float[] latlng = new float[2];

                if(exifInterface.getLatLong(latlng)) {// 현재는 위치 있는것만 표시하는게 아니라 있는건 있는대로 표시하는 방식이다.
                    latitude_ = latlng[0];
                    longitude_ = latlng[1];

                    distance = -1;//TODO: BOOL 만들지 말고, DIST 표시는 안하지만 LATLNG AVAILABLE 하다는 표시로 단다.
                }

                String datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                if (datetime != null) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    Date date = format.parse(datetime);
                    time = String.valueOf(date.getTime());//TODO: check existence first
                }
            } catch (IOException e) {// for exif
                e.printStackTrace();
            } catch (ParseException e) {// for parse date
                e.printStackTrace();
            } catch (Exception e) {// 끝까지 exception 잡아줘야 crash 안된다.(시작시 바로 종료하면 cursor때문에 npe난다.)
                e.printStackTrace();
            }

            final Image image = new Image(origin, thumbnail, time, latitude_, longitude_, distance);

            convertView.setTag(R.id.image_adapter_tag_object, image);
        }

        return convertView;
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