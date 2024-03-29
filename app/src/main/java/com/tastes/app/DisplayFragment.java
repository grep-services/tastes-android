package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.devspark.robototextview.widget.RobotoEditText;
import com.tastes.R;
import com.tastes.content.Tag;
import com.tastes.util.ByteLengthFilter;
import com.tastes.util.DefaultFilter;
import com.tastes.util.LogWrapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import cn.Ragnarok.BitmapFilter;

public class DisplayFragment extends Fragment implements Button.OnClickListener, View.OnTouchListener {

    private static final String ARG_MIRROR = "mirror";// for ffc
    private static final String ARG_IMAGE = "image";
    private static final String ARG_PATH = "path";// for gallery
    private static final String ARG_TIME = "time";
    private static final String ARG_ROTATION = "rotation";
    //private static final String ARG_ADDRESS = "address";
    // 미리 arg로 받아둬야 하는 이유는 map을 열 때 초기값으로 줄 수 있게 하기 위함이다.(loc 선택 후 다시 loc vars가 update되는 것이 아니라 map init으로만 사용된다.)
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_AVAILABLE = "available";

    private boolean internal;

    private boolean mirror;
    private String path;
    private long time;
    private int rotation;//TODO: 이제는 PORT에서 회전되어야 할 값(기존에는 0 중심으로 +- 90이었다.)으로 쓰인다.
    //private String address;
    private double latitude;
    private double longitude;
    private boolean isLocationAvailable;// from gallery 가능. 그리고 main null이라도 map에서 다시 역으로 넘어와서 upload될 수 있음.

    private byte[] image = null;

    Bitmap origin;
    Bitmap[] filters;
    private final static int[] indexes = {BitmapFilter.GRAY_STYLE, BitmapFilter.PIXELATE_STYLE, BitmapFilter.TV_STYLE, BitmapFilter.OLD_STYLE, BitmapFilter.LIGHT_STYLE, BitmapFilter.LOMO_STYLE, BitmapFilter.HDR_STYLE, BitmapFilter.SOFT_GLOW_STYLE};;

    //ImageView imageView;
    ViewPager pager;

    Button buttonForward, buttonClose;

    private boolean forwardClicked = false;

    List<String> tags;
    List<String> switches;
    List<String> positions;// x|y형태로 저장해서 나중에 comma로 다시 연결한다.
    //List<String> orientations;

    ViewGroup toolbar, container_;

    View focusedView = null;
    float originX, originY;
    float X, Y;
    boolean isMoving = false;
    boolean isKeyboard = false;
    int mSlop;

    boolean tagDefault = false;
    private static final String TAG_DEFAULT = "tastes";

    //private final String HEADER = "";

    private DisplayFragmentCallbacks mCallbacks;

    public static DisplayFragment newInstance(boolean mirror, byte[] image, int rotation, double latitude, double longitude, boolean isLocationAvailable) {
        return DisplayFragment.newInstance(mirror, image, null, -1, rotation, latitude, longitude, isLocationAvailable);
    }

    public static DisplayFragment newInstance(boolean mirror, String path, long time, int rotation, double latitude, double longitude, boolean isLocationAvailable) {
        return DisplayFragment.newInstance(mirror, null, path, time, rotation, latitude, longitude, isLocationAvailable);
    }

    public static DisplayFragment newInstance(boolean mirror, byte[] image, String path, long time, int rotation, double latitude, double longitude, boolean isLocationAvailable) {
        DisplayFragment fragment = new DisplayFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_MIRROR, mirror);
        args.putByteArray(ARG_IMAGE, image);
        args.putString(ARG_PATH, path);
        args.putLong(ARG_TIME, time);
        args.putInt(ARG_ROTATION, rotation);
        //args.putString(ARG_ADDRESS, address);
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        args.putBoolean(ARG_AVAILABLE, isLocationAvailable);

        fragment.setArguments(args);
        return fragment;
    }

    public DisplayFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mirror = getArguments().getBoolean(ARG_MIRROR);
            image = getArguments().getByteArray(ARG_IMAGE);
            path = getArguments().getString(ARG_PATH);
            time = getArguments().getLong(ARG_TIME);
            rotation = getArguments().getInt(ARG_ROTATION);
            //address = getArguments().getString(ARG_ADDRESS);
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
            isLocationAvailable = getArguments().getBoolean(ARG_AVAILABLE);
        }

        internal = (image == null);// 혹시라도 path가 안넘어올 수도 있으므로...

        ViewConfiguration vc = ViewConfiguration.get(getActivity());
        mSlop = (int)(vc.getScaledTouchSlop() * 0.5); // 50퍼센트 정도가 적당하다. 대략 16dp 로 계산되는거 같다만...

        //getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display, container, false);

        if(image == null && path == null) {// path도 추가하면 될 것 같다.
            Toast.makeText(getActivity(), getString(R.string.upload_image), Toast.LENGTH_SHORT).show();

            ((MainActivity) getActivity()).onBackPressed();// 일단 이렇게 해본다.
        } else { // 굳이 else 써야되는진 몰겠지만 finish 확실히 sync한지 모르므로. 나중에는 test도 해본다.
            // setting bitmap first.
            BitmapFactory.Options opts = new BitmapFactory.Options();

            opts.inPurgeable = true;
            opts.inInputShareable = true;
            opts.inMutable = false;
            //opts.inSampleSize = 2; // 해보고 좀 아니다 싶으면 뺀다. -> 일단 별 차이 없어서 놔뒀다.

            if(!internal) {
                origin = BitmapFactory.decodeByteArray(image, 0, image.length, opts);
            } else {
                Bitmap bitmap_ = BitmapFactory.decodeFile(path);// opts도 같이 적용해본다.

                int originWidth = bitmap_.getWidth();
                int originHeight = bitmap_.getHeight();
                int targetWidth, targetHeight;

                // 아직 가로 사진을 쓰지 않기 때문에 괜히 가로 사진 선택하면 결국 확대되어서 보일 것이다. 돌리든지 막든지 나중에 정하도록 한다.
                if(originWidth > originHeight) {
                    targetHeight = 720;
                    targetWidth = originWidth * targetHeight / originHeight;
                } else {
                    targetWidth = 720;
                    targetHeight = originHeight * targetWidth / originWidth;
                }

                origin = Bitmap.createScaledBitmap(bitmap_, targetWidth, targetHeight, true);
            }

            int width = origin.getWidth();
            int height = origin.getHeight();
            LogWrapper.e("DISPLAY", "size : " + width + ", " + height);

            // matrix 처리 - mirror는 scale, 그리고 rotation for gallery (from camera는 일단 제외)
            if(mirror || rotation != 0) {
                Matrix matrix = new Matrix();

                if(mirror) {
                    matrix.setScale(-1, 1);
                }

                if(rotation != 0) {
                    matrix.setRotate(rotation, (float) width / 2, (float) height / 2);
                }

                origin = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
            }
/*
            if(mirror) {
                // mirrorffc가 안되니깐 이렇게라도 해야된다.
                Matrix matrix = new Matrix();
                matrix.setScale(-1, 1);

                if(rotation == -90 || rotation == 90) {
                    matrix.setRotate(rotation, (float) width / 2, (float) height / 2);
                }

                origin = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
            } else {
                if(rotation == -90 || rotation == 90) {
                    Matrix matrix = new Matrix();
                    matrix.setRotate(rotation, (float) width / 2, (float) height / 2);

                    origin = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
                }
            }
*/
            filters = new Bitmap[indexes.length];
            for(int i = 0; i < indexes.length; i++) {
                // 사진을 찍을 때마다 filters를 set null 해줘야 한다.
                filters[i] = null;
            }

            // set pager
            pager = (ViewPager) view.findViewById(R.id.fragment_display_pager);
            pager.setAdapter(new PagerAdapter_());
            pager.setCurrentItem(0);

            /*
            waitView = view.findViewById(R.id.fragment_display_wait);
            locationView = view.findViewById(R.id.fragment_display_location);
            networkView = view.findViewById(R.id.fragment_display_network);
            */

            buttonForward = (Button) view.findViewById(R.id.fragment_display_forward);
            buttonClose = (Button) view.findViewById(R.id.fragment_display_close);

            /*
            locationView.setOnClickListener(this);
            networkView.setOnClickListener(this);
            */

            buttonForward.setOnClickListener(this);
            buttonClose.setOnClickListener(this);

            toolbar = (ViewGroup) view.findViewById(R.id.fragment_display_toolbar);

            container_ = (ViewGroup) view.findViewById(R.id.fragment_display_container);
            container_.setOnTouchListener(this);
            //view.setOnTouchListener(this);

            // tree observer 이용한 visible view height(keyboard 위쪽) 재는 방식 쓰려 했으나 adjustPan일 경우 가능하고, 다시말해 view들이 움직이게 된다는 말이어서 실패했다.
            container_.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // 하지만 이렇게 사용하긴 하도록 한다.
                @Override
                public void onGlobalLayout() {
                    Rect r = new Rect();
                    container_.getWindowVisibleDisplayFrame(r);
                    int screenHeight = container_.getRootView().getHeight();

                    int keypadHeight = screenHeight - r.bottom;

                    // 0.15 ratio is perhaps enough to determine keypad height.
                    if (keypadHeight > screenHeight * 0.15) {
                        isKeyboard = true;
                    } else {
                        if(isKeyboard == true) {
                            confirmView();

                            isKeyboard = false;
                        }
                    }

                    // 이렇게 하면 종료시 getActivity() null check 필요없고, 기타 default tag의 중복 등록 역시 없어지게 된다.
                    if(!tagDefault) {
                        setEdit(TAG_DEFAULT, -10000, -10000, false);

                        tagDefault = true;
                    }
                }
            });
        }

        return view;
    }
/*
    public void notifyNetworkFailure() {
        //showView(toolbar);// 나중에는 networkView를 만들고 그것을 띄운다.
        showView(networkView);
    }

    public boolean isWaiting() {
        return (waitView.getVisibility() == View.VISIBLE);
    }

    public void notifyLocationFailure() {
        showView(locationView);
    }
    */
/*
    public void setLocation(double latitude, double longitude) {
        if(!internal) {// gallery에서 온 것에 대해서는 할 필요 없으므로.
            this.latitude = latitude;
            this.longitude = longitude;

            if(isWaiting()) {
            // wait는 두 종류 있는데, 두번째 종류는 이미 location set된 상태이므로 다시 request location 할 일이 없으므로 여기로 오지 않을 것이다.
            // 그러므로 그냥 첫번째 wait(request location)이라 생각하고 upload를 call하면 된다.
                upload();
            }
        }
    }
*/
    // from main도 있고 from map도 있으므로 upload는 따로 from map일 때에 알아서 call해주도록 한다.
    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

        isLocationAvailable = true;

        //upload();
    }

    /*
    첫 한번씩은 필터를 만들어 쓴다. 사진을 찍을 때마다 필터 배열은 초기화된다.
    이것보다 더 좋은 방식은 사진을 찍을 때마다 필터 배열을 thread로 만드는 것일 것이다.
    하지만 그것도 아직 thread 완료 되기 전 swipe시 막을 process 생각해보지 못했고
    일단 오래 걸리면서 의미 없어 보이는 필터 빼면 어느정도 해결은 되므로 넘어간다.
     */
    private class PagerAdapter_ extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            Bitmap bitmap;//TODO: 문제 생기면 recycle부터 신경써본다.

            switch(position) {
                case 0:
                    bitmap = origin;

                    break;
                default:
                    if(filters[position - 1] == null) {// 만약 filter마다 손볼게 따로 있다면(lomo size 등) 그건 따로 한다.
                        filters[position - 1] = BitmapFilter.changeStyle(origin, indexes[position - 1]);
                    }

                    bitmap = filters[position - 1];

                    break;
            }

            ImageView view = new ImageView(getActivity());
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            view.setLayoutParams(layoutParams);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            view.setImageBitmap(bitmap);

            container.addView(view);

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return indexes.length + 1;// default(not filter)도 포함.
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (DisplayFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public int addTag(String tag, float ratioX, float ratioY) {
        if(tags == null) tags = new ArrayList<String>();

        tags.add(tag);

        if(switches == null) switches = new ArrayList<String>();

        switches.add("true");

        if(positions == null) positions = new ArrayList<String>();

        positions.add(ratioX + "|" + ratioY);

        // 일단 position은 고정하고, orientation은 저장하지 않는걸로 간다.
/*
        if(rotation == -90 || rotation == 90) {// orientation 안바꿔도 될 때까지는 이렇게 해야 home에서(port) 정상적으로 보인다.
            // 그리고 이렇게 분류하는 것도 찍은 그대로 나오게 하기 위해서이다.(나중을 생각해도 이게 맞다.)
            if(rotation == -90) {
                positions.add((1 - ratioY) + "|" + ratioX);
            } else {
                positions.add(ratioY + "|" + (1 - ratioX));
            }
        } else {
            positions.add(ratioX + "|" + ratioY);
        }

        if(orientations == null) orientations = new ArrayList<String>();

        orientations.add(String.valueOf(((rotation * -1) + 360) % 360));// minus를 string에 쓰긴 그렇고, -90은 90, 90은 270(-90)이므로 이렇게 했다.
*/
        return tags.indexOf(tag);
    }

    public void setTag(int index, String tag) {
        tags.set(index, tag);
    }

    public void removeTag(int index) { // index가 왔다는건 list도 있다는 것이다.
        tags.remove(index);
        switches.remove(index);
        positions.remove(index);
        //orientations.remove(index);
    }

    public boolean checkTag(String tag) {
        if(tags != null) {
            if(tags.contains(tag)) {
                return true;
            }
        }

        return false;
    }

    // matcher가 있어도 empty 검사는 해야 한다.(지우는 유일한 수단이기도 하고.)
    public void confirmView() { // check 뿐만 아니라 처리까지 여기서 해야 중복 shake 등도 할 수 있다.
        if(focusedView != null) {
            EditText edit = (EditText) focusedView;
            String string = edit.getText().subSequence(Tag.HEADER.length(), edit.getText().length()).toString();
            //int leftMargin = ((RelativeLayout.LayoutParams) focusedView.getLayoutParams()).leftMargin;
            //int topMargin = ((RelativeLayout.LayoutParams) focusedView.getLayoutParams()).topMargin;
            float ratioX = focusedView.getX() / (float) container_.getMeasuredWidth();
            float ratioY = focusedView.getY() / (float) container_.getMeasuredHeight();

            if(string != null) {
                if(string.isEmpty()) {
                    // 그냥 공백이었을 경우는 추가가 아직 안된 상태이므로 tag가 없다. 그냥 focusedView만 삭제되도록 false return만 하면 된다.
                    // 하지만 기존에 있던걸 지운 경우는 이렇게 list에서 tag를 삭제해준다.
                    if (focusedView.getTag() != null) {
                        removeTag((Integer) focusedView.getTag());
                    }

                    removeFocusedView();
                } else if(checkTag(string)) {
                    // 같은 태그가 있다면 tag 자체를 list에서 삭제하거나 기존 view를 삭제할 필요는 없고 그냥 focusedView만 지워지게 false return한다.
                    // 다만 자기가 자기를 클릭했다가 지워지면 안된다. 그걸 해결하려면, checkTag 자체가 이미 tag를 갖고 있다는 말이고 다시말해 index를 알 수 있다는 것을 이용한다.
                    if(focusedView.getTag() != null) { // null 이면 검사할 필요도 없다. 그냥 다른 것이다.(아직 입력 중인 view일 것이기 때문.)
                        int index = (Integer) focusedView.getTag();
                        if(tags.indexOf(string) == index) { // index가 같으면 자기이므로 true 넘긴다.(분명 다를수도 있다. 이미 있는 것을 특정 tag(이미 존재하는)로 똑같이 바꿀 수도 있기 때문이다.)
                            clearFocusedView();
                        } else { // 다른 view를 고쳤는데 중복될 경우.
                            alertFocusedView();
                        }
                    } else { // 입력중인 view.
                        alertFocusedView();
                    }
                } else {
                    if(focusedView.getTag() != null) { // 이건 그냥 다른것을 선택해서 뭔가를 입력한 경우.(check는 이미 되었다고 보면 된다.)
                        // edit를 넣기. 그래야만 태그 편집했는데 기존 태그가 추가되는 것 같은 경우가 사라진다.
                        int index = (Integer) focusedView.getTag();
                        setTag(index, string);
                    } else { // 이건 새로운 것을 입력한 경우.(이것도 check는 되었다고 보면 된다.)
                        //int index = addTag(string, leftMargin, topMargin);
                        int index = addTag(string, ratioX, ratioY);
                        focusedView.setTag(index); // for removing later.
                    }

                    clearFocusedView();
                }
            }
        }
    }

    public void setToolbarEnabled(boolean enabled) {
        /*
        toolbar touch 막아도 되겠지만 복잡하고, 거기다가 있어서 좋을 것도 없이 헷갈린다.
        생각해보니 안없애면 태그 입력 취소/확인 이라고 생각할 수도 있다. 어떻게든 없앤다.
        버튼만 없애도 안되고 background 바꾸고 listener 취소해도 click안되는 문제 또 생기고
        api level 생각 버리고 xy translation 하려 해도 안되고 결국은 margin으로 해결했다.
        이런 방법까지 써야 되는지...
         */

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) toolbar.getLayoutParams();
        layoutParams.topMargin = enabled ? 0 : (int)(toolbar.getHeight() * -1);// 해보니 여백 있어서 그냥 딱 1배만큼 올려도 된다.
        toolbar.setLayoutParams(layoutParams);
    }

    public void requestViewFocused(View view) {
        setToolbarEnabled(false);

        ((EditText) view).requestFocus();
        focusedView = view;
    }

    public void clearFocusedView() {
        if(focusedView != null) {
            setToolbarEnabled(true);

            ((EditText)focusedView).clearFocus();
            focusedView = null;
        }
    }

    // clear와 같지만 viewgroup에서 삭제해준다.
    public void removeFocusedView() {
        if(focusedView != null) {
            setToolbarEnabled(true);

            ((EditText)focusedView).clearFocus();
            container_.removeView(focusedView);
            focusedView = null;
        }
    }

    public void alertFocusedView() {
        /* layout params가 set 되어 있지 않으면 안된다. */
        if(focusedView != null) {
            //YoYo.with(Techniques.Shake).playOn(focusedView);
            Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
            focusedView.startAnimation(shake);
        }
    }

    public int getPixel(int dp) {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        float density = dm.density;

        return (int)(dp * density);
    }

    /*
    이걸 쓰면, api11부터이므로 다른 계획들에도 영향을 미친다. 하지만 일단 이대로 간다.
    먼저, margin이 안되는 이유는, 잘 모르겠지만 set x, y로 하면 해결이 된다.
    하지만 x, y는 margin이 없어서 adjustpan에서 문제가 된다.
    adjustpan을 막으면서 x, y를 쓰려면 생성시에 margin을 주면 될듯 했는데, 그것도 역시 다른건 되도 생성시 끄트머리 생성은 문제가 된다.
    결국 api level 포기하면서 x, y를 써도, 사실 adjustpan이 좀 걸리긴 하지만 일단은 이정도에서 fix한다.
     */
    public void setPosition(View view, float x, float y) {
        //RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();

        //layoutParams.leftMargin = (int) x;
        //layoutParams.topMargin = (int) y;

        //view.setLayoutParams(layoutParams);

        if(x == -10000 && y == -10000) {// flag - center - touch로는 여기에 다다를 수 없다.
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            view.setLayoutParams(layoutParams);
        } else {
            view.setX(x);
            view.setY(y);
        }
    }

    public void setEdit(final String string, float x, float y, boolean focused) {
        final EditText edit = getEdit(string);

        container_.addView(edit);

        setPosition(edit, x, y);

        if(focused) {// focuse를 받게 해야 하는 정상적인 방식.
            requestViewFocused(edit);
        } else {// focuse를 안받게 한다는 말은, focus를 받았다가 다시 confirm되는 과정까지 다 인위적으로 해줘야 한다는 말이다.(touch에서의 confirm까지.)
            edit.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if(edit.getWidth() > 0 && edit.getHeight() > 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            edit.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            edit.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }

                        float ratioX = edit.getX() / (float) container_.getMeasuredWidth();
                        float ratioY = edit.getY() / (float) container_.getMeasuredHeight();

                        int index = addTag(string, ratioX, ratioY);

                        edit.setTag(index); // for removing later.
                    }
                }
            });
        }
    }

    public EditText getEdit(String string) {
        //EditText edit = new EditText(getActivity());
        RobotoEditText edit = new RobotoEditText(getActivity());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        edit.setLayoutParams(layoutParams);
        edit.setBackgroundDrawable(null);//TODO: 안되면 color transparent라도 한다.
        int p = getPixel(16);
        edit.setPadding(p, p, p, p);
        edit.setIncludeFontPadding(false);// noto top bottom padding 없애기 위해.
        edit.setInputType(InputType.TYPE_CLASS_TEXT); // 왜그런진 몰라도 setText앞에 와야 한다.
        edit.setText(string == null ? Tag.HEADER : Tag.HEADER + string);
        //edit.setHint("Tag"); text가 있으므로 hint는 자연히 무시된다. 혼재하게 할 수 있으나 일단 지운다.
        //edit.setHintTextColor(getResources().getColor(R.color.text_inverse));
        edit.setFilters(new InputFilter[]{new DefaultFilter(/*edit, */), new ByteLengthFilter(50)});
        edit.setTextSize(18);
        edit.setTextColor(getResources().getColor(R.color.text_inverse));
        //edit.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        edit.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN);// extracted는 비추되어서 이걸로 썼다.
        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_DONE:
                        confirmView();

                        return true;
                }

                return false;
            }
        });
        edit.setOnTouchListener(this);
        edit.setOnFocusChangeListener(new EditText.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus == true) {
                    showKeyboard(v);// TODO: 필요한지 확인해보기.
                } else {
                    hideKeyboard(v);
                }
            }
        });

        return edit;
    }

    public void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        imm.showSoftInput(view, 0);
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // 일단 쓰지 않는다. 어차피 exception으로 걸리고, 괜히 먼저 해놨다가 location 걸린 후에 또다시 network로 걸리면 사용자 혼란만 더 생긴다.
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mobile.isConnected() || wifi.isConnected();
    }

    /*
    public void showView(View view) {
        // 이게 있으면 괜히 실행 취소 같아 보인다. 끄고 싶으면 알아서 back을 누르는 식으로 유도하는게 나을 것 같다.
        toolbar.setVisibility(View.GONE);
        waitView.setVisibility(View.GONE);
        locationView.setVisibility(View.GONE);
        networkView.setVisibility(View.GONE);

        view.setVisibility(View.VISIBLE);
    }
    */

    public void forwardClicked(double latitude, double longitude, boolean isLocationAvailable) {
        if (mCallbacks != null) {
            mCallbacks.onDisplayForwardClicked(latitude, longitude, isLocationAvailable);
        }
    }
    /*
    // caller에서 task 실행하게 해야 frag 닫혀도 나머지 process 처리할 수 있다.
    public void actionOKClicked(byte[] file, long time, double latitude, double longitude, List<String> tags, List<String> positions, List<String> orientations, List<String> switches) {
        if (mCallbacks != null) {
            mCallbacks.onDisplayActionOKClicked(file, time, latitude, longitude, tags, positions, orientations, switches);
        }
    }
    */

    // 따로 빼야 location 완료 등에서도 연결될 수 있다.
    public void upload(String passcode) {
        if (mCallbacks != null) {
            int position = pager.getCurrentItem();

            if(mirror || internal || position > 0) {// 반전이든 필터든 필요한 만큼 적용된다.(ffc면 무조건, ffc든 아니든 0아니면 그것도.)
                Bitmap bitmap;

                switch(position) {
                    case 0:
                        bitmap = origin;

                        break;
                    default:
                        bitmap = filters[pager.getCurrentItem() - 1];
                }

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);// jpeg format이 확실히 맞을지는 확인해봐야 할 듯.
                image = stream.toByteArray();
            }

            //actionOKClicked(image, internal ? time : System.currentTimeMillis(), latitude, longitude, tags, positions, orientations, switches);
            //TODO: caller에서 task 실행하게 해야 frag 닫혀도 나머지 process 처리할 수 있다. 그리고 나중에 image obj로 바꿔서 간다.(for retry in home)
            mCallbacks.onDisplayUpload(image, internal ? time : System.currentTimeMillis(), latitude, longitude, tags, positions/*, orientations*/, passcode);
        }
    }

    public void setForwardClicked(boolean clicked) {
        forwardClicked = clicked;
    }

    @Override
    public void onClick(View v) {
        MainActivity mainActivity = (MainActivity) getActivity();

        switch(v.getId()) {
            case R.id.fragment_display_forward:
                if(!forwardClicked) {
                    forwardClicked = true;// TODO: enabled도 잘 안될만큼, 최대한 빠른 click 올 수 있다고 생각하고, 바로 set한다.

                    // check tag existence
                    if(tags == null || tags.isEmpty()) {// 애초에 remove될 때 null시키면 되겠지만 여러번 체크하는 것도 그렇고 일단 이게 최소 변경이므로 이렇게 간다.
                        mainActivity.showToast(R.string.upload_tag);

                        break;
                    }

                    //TODO: 여기서 해주나 server 보내서 확인하나 시간은 거의 차이 없으나, 종료되지 않는다는 점이 다르다.
                    if(!isNetworkAvailable()) {// 일단 여기서도 해준다. toast 형식이므로 어쨌든 해줄만큼 더 해주는게 낫다.
                        mainActivity.showToast(R.string.network_retry);

                        break;
                    }

                    forwardClicked(latitude, longitude, isLocationAvailable);
                }

                break;
            case R.id.fragment_display_close:
                mainActivity.onBackPressed();// adjustpan 때문에 disable 등등 다 안돼서 background로라도 처리해야 했고 그러려면 listener도 frag에서 처리해야 했다.

                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v instanceof Button) {
            return false; // 이건 아마 layer로 넘기려는 것이고
        }

        if(toolbar.getVisibility() != View.VISIBLE) {
            return true; // 이건 block 시 touch disable하는 것.
        }

        // x, y로 움직이는 것들은, layout parameters 설정이 제대로 안되어 있으면 adjustPan에 적용되지 않아서 일단 제외하고 margin 방식으로 갈아탔다.
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                /*
                if(v instanceof Button) {
                    YoYo.with(Techniques.Pulse).playOn(v);// duration 안해도 되는지 확인해보기.

                    break;
                }
                */

                if(focusedView == null) { // 키보드 떠있을때는 내려가고 다시 move 가능하게 하기위해 이렇게 한다.
                    originX = event.getRawX();
                    originY = event.getRawY();

                    if(v instanceof EditText) {
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                        //X = layoutParams.leftMargin;
                        //Y = layoutParams.topMargin;
                        X = v.getX();
                        Y = v.getY();
                    } else {// vp도 포함될 수 있다.
                        pager.onTouchEvent(event);
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if(focusedView == null) {
                    float difX = event.getRawX() - originX;
                    float difY = event.getRawY() - originY;

                    if(Math.abs(difX) > mSlop || Math.abs(difY) > mSlop) {
                        isMoving = true;
                    }

                    if(v instanceof EditText) {
                        setPosition(v, X + difX, Y + difY);
                    } else { // 빈 화면을 잡고 끌었을 경우(물론 focused view가 null인 경우 속에서(편집 중인 것이 없을 때)) swipe시켜줘야 한다.
                        pager.onTouchEvent(event);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:// make, moving, cancel(view를 선택해도 cancel일 수 있다.), select.
                /*
                if(v instanceof Button) {
                    return false;// not consume
                }
                */
                if(focusedView == null) { // make : 일단 focused null일 때. 그리고 viewgroup 선택했을 때.(not et)
                    if(!(v instanceof EditText)) {
                        if(isMoving == true) { // swipe.
                            pager.onTouchEvent(event);

                            isMoving = false;
                        } else {
                            setEdit(null, event.getX() - getPixel(16), event.getY() - getPixel(16), true);// edit height는 아직 안나오는데, 구할 필요까지는 없을 것 같다.
                        }
                    } else {
                        if(isMoving == true) {// move : focused null이지만, et 선택했을 때. 그리고 move true일 때.
                            // position 수정해준다.
                            int index = (Integer) v.getTag();
                            //int leftMargin = ((RelativeLayout.LayoutParams) v.getLayoutParams()).leftMargin;
                            //int topMargin = ((RelativeLayout.LayoutParams) v.getLayoutParams()).topMargin;
                            float ratioX = v.getX() / (float) container_.getMeasuredWidth();
                            float ratioY = v.getY() / (float) container_.getMeasuredHeight();
                            positions.set(index, new String(ratioX+"|"+ratioY));

                            isMoving = false;
                        } else { // else가 select(focused null이지만, et 선택했을 때. 그리고 move false일 때.)이지만 gesture에서 처리한다.(double tap 위해서.) => gesture는 일단 버리고, event 넘긴다.(not consumed)
                            requestViewFocused(v);

                            return false;// 그냥 true 되게 놔둬도 상관없는 것 같지만 이게 맞을 것 같으므로 이렇게 한다.
                        }
                    }
                } else {
                    if(v != focusedView) {// cancel : focused not null일 경우 어디를 누르든 cancel. 자기자신을 눌렀을 경우만 빼고.
                        confirmView();
                    } else {// 자기 자신 click : event 넘긴다.
                        return false;
                    }
                }

                break;
        }

        return true;
    }

    public interface DisplayFragmentCallbacks {
        public void onDisplayForwardClicked(double latitude, double longitude, boolean isLocationAvailable);
        public void onDisplayUpload(byte[] file, long time, double latitude, double longitude, List<String> tags, List<String> positions/*, List<String> orientations*/, String passcode);
        //public void onDisplayActionOKClicked(byte[] file, long time, double latitude, double longitude, List<String> tags, List<String> positions, List<String> orientations, List<String> switches);
    }
}