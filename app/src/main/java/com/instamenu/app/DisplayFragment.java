package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.instamenu.R;
import com.instamenu.util.ByteLengthFilter;
import com.instamenu.util.DefaultFilter;
import com.instamenu.util.LogWrapper;
import com.instamenu.util.QueryWrapper;
import com.instamenu.widget.TagAdapter;

import java.util.ArrayList;
import java.util.List;

public class DisplayFragment extends Fragment implements Button.OnClickListener, View.OnTouchListener {

    private static final String ARG_ADDRESS = "address";
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";

    private String address;
    private double latitude;
    private double longitude;

    private QueryWrapper queryWrapper;

    static byte[] imageToShow = null;

    ImageView imageView;

    Button buttonOk, buttonClose;

    View waitView;

    List<String> tags;
    List<String> switches;
    List<String> positions;// x|y형태로 저장해서 나중에 comma로 다시 연결한다.

    //private GestureDetector gestureDetector;

    ViewGroup container_;

    View focusedView = null;
    //View focusedView_ = null;// for gesture detector. view 전달할 수가 없어서 이렇게 했다.
    float originX, originY;
    float X, Y;
    boolean isMoving = false;
    //boolean isMoving_ = false;// for gesture detector. onTouch에서 set되지만 gesture detector에서 해제된다.
    boolean isKeyboard = false;

    private final String HEADER = "";

    private DisplayFragmentCallbacks mCallbacks;

    public static DisplayFragment newInstance(String address, double latitude, double longitude) {
        DisplayFragment fragment = new DisplayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ADDRESS, address);
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
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
            address = getArguments().getString(ARG_ADDRESS);
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
        }

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        queryWrapper = new QueryWrapper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display, container, false);

        if(imageToShow == null) {
            Toast.makeText(getActivity(), "There is no image.", Toast.LENGTH_SHORT).show();

            //getActivity().finish();
            ((MainActivity) getActivity()).onBackPressed();// 일단 이렇게 해본다.
        } else { // 굳이 else 써야되는진 몰겠지만 finish 확실히 sync한지 모르므로. 나중에는 test도 해본다.
            // setting image
            imageView = (ImageView) view.findViewById(R.id.fragment_display_image);

            BitmapFactory.Options opts=new BitmapFactory.Options();

            opts.inPurgeable=true;
            opts.inInputShareable=true;
            opts.inMutable=false;
            opts.inSampleSize=2;

            Bitmap origin = BitmapFactory.decodeByteArray(imageToShow, 0, imageToShow.length, opts);
            //int size = Math.min(origin.getWidth(), origin.getHeight());
            int w = origin.getWidth();
            int h = origin.getHeight();
            //Bitmap square = Bitmap.createBitmap(origin, 0, 0, size, size);
            //imageView.setImageBitmap(square);
            imageView.setImageBitmap(origin);

            waitView = view.findViewById(R.id.fragment_display_wait);

            buttonOk = (Button) view.findViewById(R.id.fragment_display_ok);
            buttonClose = (Button) view.findViewById(R.id.fragment_display_close);

            //buttonOk.setOnTouchListener(this);
            //buttonClose.setOnTouchListener(this);

            buttonOk.setOnClickListener(this);
            /*
            gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) { // double tap과 tap 구별 가능한 method.
                    if(focusedView == null) { // 일단 null이어야 한다. 즉, 입력중이 아니어야 한다. 그래야 자신도, 남도 선택 안할 수 있다.(onTouch와 구조 같음)
                        if(!isMoving_) { // null일 경우에도 moving은 제외해야 한다.
                            Toast.makeText(getActivity(), "clicked "+((EditText) focusedView_).getText().toString(), Toast.LENGTH_SHORT).show();
                        } else {
                            isMoving_ = false;
                        }
                    }

                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    requestViewFocused(focusedView_);// 이렇게 하면 focusedView_가 진짜 focusedView가 된다.

                    return true;
                }
            });
            */
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
                            /*
                            if(confirmView()) {
                                clearFocusedView();
                            } else {
                                removeFocusedView();
                            }
                            */
                            confirmView();

                            isKeyboard = false;
                        }
                    }
                }
            });
        }

        return view;
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

    // caller에서 task 실행하게 해야 frag 닫혀도 나머지 process 처리할 수 있다.
    public void actionOKClicked(byte[] file, long time, String address, double latitude, double longitude, List<String> tags, List<String> positions, List<String> switches) {
        if (mCallbacks != null) {
            mCallbacks.onDisplayActionOKClicked(imageToShow, System.currentTimeMillis(), address, latitude, longitude, tags, positions, switches);
        }
    }

    public int addTag(String tag, float ratioX, float ratioY) {
        if(tags == null) tags = new ArrayList<String>();

        tags.add(tag);

        if(switches == null) switches = new ArrayList<String>();

        switches.add("true");

        if(positions == null) positions = new ArrayList<String>();

        positions.add(ratioX+"|"+ratioY);

        return tags.indexOf(tag);
    }

    public void setTag(int index, String tag) {
        tags.set(index, tag);
    }

    public void removeTag(int index) { // index가 왔다는건 list도 있다는 것이다.
        tags.remove(index);
        switches.remove(index);
        positions.remove(index);
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
            String string = edit.getText().subSequence(HEADER.length(), edit.getText().length()).toString();
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

                    //return false;
                    removeFocusedView();
                } else if(checkTag(string)) {
                    // 같은 태그가 있다면 tag 자체를 list에서 삭제하거나 기존 view를 삭제할 필요는 없고 그냥 focusedView만 지워지게 false return한다.
                    // 다만 자기가 자기를 클릭했다가 지워지면 안된다. 그걸 해결하려면, checkTag 자체가 이미 tag를 갖고 있다는 말이고 다시말해 index를 알 수 있다는 것을 이용한다.
                    if(focusedView.getTag() != null) { // null 이면 검사할 필요도 없다. 그냥 다른 것이다.(아직 입력 중인 view일 것이기 때문.)
                        int index = (Integer) focusedView.getTag();
                        if(tags.indexOf(string) == index) { // index가 같으면 자기이므로 true 넘긴다.(분명 다를수도 있다. 이미 있는 것을 특정 tag(이미 존재하는)로 똑같이 바꿀 수도 있기 때문이다.)
                            //return true;
                            clearFocusedView();
                        } else { // 다른 view를 고쳤는데 중복될 경우.
                            alertFocusedView();
                        }
                    } else { // 입력중인 view.
                        alertFocusedView();
                    }

                    //return false;
                    //alertFocusedView();
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

                    //return true;
                    clearFocusedView();
                }
            }
        }

        //return false;
    }

    public void requestViewFocused(View view) {
        ((EditText) view).requestFocus();
        focusedView = view;
    }

    public void clearFocusedView() {
        if(focusedView != null) {
            ((EditText)focusedView).clearFocus();
            focusedView = null;
        }
    }

    // clear와 같지만 viewgroup에서 삭제해준다.
    public void removeFocusedView() {
        if(focusedView != null) {
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

        view.setX(x);
        view.setY(y);
    }

    public EditText getEdit() {
        EditText edit = new EditText(getActivity());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        edit.setLayoutParams(layoutParams);
        edit.setBackgroundDrawable(null);//TODO: 안되면 color transparent라도 한다.
        int p = getPixel(16);
        edit.setPadding(p, p, p, p);
        edit.setInputType(InputType.TYPE_CLASS_TEXT); // 왜그런진 몰라도 setText앞에 와야 한다.
        edit.setHint("Tag");
        edit.setHintTextColor(getResources().getColor(R.color.text_inverse));
        edit.setText(HEADER);
        //edit.setEms(HEADER.length());
        edit.setFilters(new InputFilter[]{new DefaultFilter(/*edit, */HEADER), new ByteLengthFilter(50)});
        edit.setTextSize(24);
        edit.setTextColor(getResources().getColor(R.color.text_inverse));
        //edit.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        edit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_DONE:
                        /*
                        if (confirmView()) {
                            clearFocusedView();
                        } else {
                            removeFocusedView();
                        }
                        */
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
                    showKeyboard(v);
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

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_display_ok:
                // check tag existence
                if(tags == null) {
                    Toast.makeText(getActivity(), "Add a tag", Toast.LENGTH_SHORT).show();

                    break;
                }

                buttonClose.setVisibility(View.GONE);// 이게 있으면 괜히 실행 취소 같아 보인다. 끄고 싶으면 알아서 back을 누르는 식으로 유도하는게 나을 것 같다.
                buttonOk.setVisibility(View.GONE);
                waitView.setVisibility(View.VISIBLE);

                actionOKClicked(imageToShow, System.currentTimeMillis(), address, latitude, longitude, tags, positions, switches);

                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v instanceof Button) {
            return false;
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
                    if(v instanceof EditText) {
                        originX = event.getRawX();
                        originY = event.getRawY();
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                        //X = layoutParams.leftMargin;
                        //Y = layoutParams.topMargin;
                        X = v.getX();
                        Y = v.getY();
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if(focusedView == null) {
                    if(v instanceof EditText) {
                        float difX = event.getRawX() - originX;
                        float difY = event.getRawY() - originY;

                        if(Math.abs(difX) > 20 || Math.abs(difY) > 20) {
                            isMoving = true;
                            //isMoving_ = true;// gesture에서 해제하기 위한 set.

                            setPosition(v, X + difX, Y + difY);
                        }
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
                        EditText edit = getEdit();

                        container_.addView(edit);

                        // edit height는 아직 안나오는데, 구할 필요까지는 없을 것 같다.
                        setPosition(edit, event.getX() - getPixel(16), event.getY() - getPixel(16));

                        requestViewFocused(edit);
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
                        /*
                        if(confirmView()) {
                            clearFocusedView();
                        } else {
                            removeFocusedView();
                        }
                        */
                        confirmView();
                    } else {// 자기 자신 click : event 넘긴다.
                        return false;
                    }
                }

                break;
        }

        /*
        if(v instanceof EditText) {// up에서 처리하니깐 안되서 여기서 하기로 했다.
            focusedView_ = v;
            gestureDetector.onTouchEvent(event);

            return false;
        } else
        */

        return true;
    }

    public interface DisplayFragmentCallbacks {
        public void onDisplayActionOKClicked(byte[] file, long time, String address, double latitude, double longitude, List<String> tags, List<String> positions, List<String> switches);
    }
}