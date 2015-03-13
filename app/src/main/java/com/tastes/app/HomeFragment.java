package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import com.devspark.robototextview.widget.RobotoEditText;
import com.tastes.R;
import com.tastes.content.Image;
import com.tastes.content.Tag;
import com.tastes.util.ByteLengthFilter;
import com.tastes.util.DefaultFilter;
import com.tastes.util.LogWrapper;
import com.tastes.util.QueryWrapper;
import com.tastes.widget.ImageAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.tastes.widget.SwipeRefreshLayout_;

import org.apache.http.conn.HttpHostConnectException;

import java.util.List;

public class HomeFragment extends Fragment implements GridView.OnItemClickListener/*, Button.OnClickListener*/ {
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";

    private double latitude;
    private double longitude;

    private QueryWrapper queryWrapper;

    ImageLoader imageLoader;

    EditText edit;
    SwipeRefreshLayout refresh;
    GridView grid;
    ImageAdapter adapter;

    View emptyView;

    boolean isKeyboard = false;

    private MainActivity mActivity;
    private HomeFragmentCallbacks mCallbacks;

    public static HomeFragment newInstance(double latitude, double longitude) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        fragment.setArguments(args);
        return fragment;
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
        }

        queryWrapper = new QueryWrapper();

        imageLoader = ImageLoader.getInstance();
    }
/*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
*/
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        /*
        ********************** filter ok 하고서 이쪽으로 온다. 즉, view 설정 등이 또 되는데.....
         */
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_home, container, false);

        // tree observer 이용한 visible view height(keyboard 위쪽) 재는 방식 쓰려 했으나 adjustPan일 경우 가능하고, 다시말해 view들이 움직이게 된다는 말이어서 실패했다.
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // 하지만 이렇게 사용하긴 하도록 한다.
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                view.getWindowVisibleDisplayFrame(r);
                int screenHeight = view.getRootView().getHeight();

                int keypadHeight = screenHeight - r.bottom;

                // 0.15 ratio is perhaps enough to determine keypad height.
                if (keypadHeight > screenHeight * 0.15) {
                    isKeyboard = true;

                    //((MainActivity) getActivity()).setViewPagerEnabled(false);
                } else {
                    if(isKeyboard == true) {
                        //((MainActivity) getActivity()).setViewPagerEnabled(true);

                        clearEdit();

                        isKeyboard = false;
                    }
                }
            }
        });

        edit = (RobotoEditText) view.findViewById(R.id.fragment_home_edit);
        edit.setText(Tag.HEADER);// 이것 때문에 어차피 hint는 무시된다.
        edit.setFilters(new InputFilter[]{new DefaultFilter(), new ByteLengthFilter(50)});
        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_DONE:
                        confirmTag();

                        return true;
                }

                return false;
            }
        });
        edit.setSelection(1);
        edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus == true) {// 이건 자동으로 된다. 괜히 했다가 keyboard 중복 열리는 문제 생긴다.
                    //showKeyboard();
                } else {
                    hideKeyboard();
                }
            }
        });

        refresh = (SwipeRefreshLayout) view.findViewById(R.id.fragment_home_refresh);
        /* 아직 안된다. 보류.
        ((SwipeRefreshLayout_) refresh).setOnActionDown(new Runnable() {
            @Override
            public void run() {
                clearEdit();
            }
        });
        */

        emptyView = view.findViewById(R.id.fragment_home_empty);

        grid = (GridView) view.findViewById(R.id.fragment_home_grid);

        adapter = new ImageAdapter(getActivity(), inflater, imageLoader);

        grid.setAdapter(adapter);
        grid.setOnItemClickListener(this);
        grid.setEmptyView(emptyView);
        grid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {// 최대 3번 called라서 여기에서 하기로 했다.
                if(scrollState == SCROLL_STATE_IDLE) {
                    if(grid.getChildCount() > 0 && grid.getChildAt(0).getTop() == 0) {
                        refresh.setEnabled(true);
                    }
                } else {
                    refresh.setEnabled(false);

                    if(scrollState == SCROLL_STATE_TOUCH_SCROLL) {// 스크롤 하는 도중 keyboard open할 수도 있지만 그건 놔둬도 괜찮을 듯 하다.
                        clearEdit();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        //((Button) view.findViewById(R.id.fragment_home_filter)).setOnClickListener(this);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() { // 다른 곳에서 set false를 해줘야 되는듯 하다.
                clearEdit();// pull.

                if(mActivity.isLocationUpdated()) {
                    setView();
                } else {
                    if(mActivity.isRequestingLocationFailed()) {
                        mActivity.startLocationUpdates();
                    }
                }
            }
        });
        // 첫번째 색 말고는 안먹힘. 일단 빼둔다.
        //refresh.setColorSchemeResources(R.color.orange, R.color.orange_dark, R.color.gray, R.color.gray_dark);
        setRefreshing(true);

        return view;
    }

    public void clearEdit() {
        if(edit != null) {
            edit.clearFocus();
        }
    }

    public void setRefreshing(boolean refreshing) {
        clearEdit();// 아직 true일 땐 쓸일 없지만 분화해놨다가 나중에 다시 통일하는것 보다 이게 낫다.

        if(refreshing) {
            refresh.post(new Runnable() {
                @Override
                public void run() {
                    refresh.setRefreshing(true);
                }
            });
        } else {
            if(refresh.isRefreshing()) {
                // refresh 해제.
                refresh.setRefreshing(false);
            }
        }
    }

    // splash가 activity가 아닌 상황에서는 home이 먼저 등록될 수 있다.(웬만하면) 구조를 통째로 바꾸기보다 일단 method하나만 만든다.
    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

        setView();// 특별히 location 관련 check 할 필요는 없을 것 같다.
    }

    public void setEmptyView(String string) {
        TextView text = (TextView) emptyView.findViewById(R.id.fragment_home_empty_txt);
        text.setText(string);
    }

    public void notifyLocationFailure() {
        setEmptyView(getString(R.string.location_retry));

        adapter.setImages(null);// server에서 받아오지 않아도 null이다.

        setRefreshing(false);
    }

    public void setView() {
        LogWrapper.e("HOME", "set view");
        // get tags from pref.
        //=> pull to refresh 등에서는 pref에서 tags 받아올 필요 없을 거라고 생각할수도 있지만, tag를 1번만 받아오는 구조라면, 나중에 display나 filter 등에서 넘어올 때
        // 다른 정보를 더 받아와야 되는 등, 점점더 골치아파진다.(왜냐하면 이 home frag는 한번 실행되면 계속 남아있으므로 pref도 그대로일 것이기 때문.)
        final List<String> tags = mActivity != null ? mActivity.getFilters() : null;
        // get images from server with tags.
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            List<Image> images = null;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    images = queryWrapper.getImages(tags, latitude, longitude);
                } catch (HttpHostConnectException e) {
                    LogWrapper.e("Loc", e.getMessage());
                    //e.printStackTrace();
                    return false;
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);

                if(isAdded()) {// 재현 자주 되지는 않지만 attatched안됐는데 getString 등 한다고 error 난다. onPost에서는 주로 이렇게 해주라는 의견들이 있다.
                    if(success) {// item update는 사실 network가 안돌아갈 때까지 해줄 필요는 없다고 생각한다. 그럼 괜히 화면만 비어서 이상하다.(그래도 논의해보기.)
                        // manually set empty view string - view가 refresh 위에 있어서 string이 아무때나 보일 수 있다.
                        setEmptyView(getString(R.string.img_empty));
                    } else {
                        //Toast.makeText(getActivity(), getString(R.string.upload_network), Toast.LENGTH_SHORT).show();
                        setEmptyView(getString(R.string.network_retry));
                    }

                    // set to adapter.
                    adapter.setImages(images);

                    setRefreshing(false);
                }
            }
        };

        // 11부터는 serial이 default라서.
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.HONEYCOMB) task.execute();
        else task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;

        try {
            mCallbacks = (HomeFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void searchTag(String tag) {
        if(mCallbacks != null) {
            mCallbacks.onHomeSearchTag(tag);
        }
    }

    public void confirmTag() {
        String tag = edit.getText().subSequence(Tag.HEADER.length(), edit.length()).toString();

        Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);

        // 최대한 msg는 없앤다.
        if(tag == null || tag.isEmpty()) { // 빈 내용이면 그냥 놔두면 된다.
            //YoYo.with(Techniques.Shake).playOn(edit);
            edit.startAnimation(shake);
            //Toast.makeText(getActivity(), "내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
        } else {
            clearEdit();// keyboard close부터.

            searchTag(tag);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        clearEdit();

        if(mCallbacks != null) {
            //mCallbacks.onHomeItemClicked((Image) adapter.getItem(position));
            mCallbacks.onHomeItemClicked(adapter.getImages(), position);
        }
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
    }

    public interface HomeFragmentCallbacks {
        public void onHomeSearchTag(String tag);
        public void onHomeItemClicked(List<Image> images, int position);
    }
}
