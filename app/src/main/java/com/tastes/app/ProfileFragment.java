package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.devspark.robototextview.widget.RobotoTextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.tastes.R;
import com.tastes.content.Image;
import com.tastes.content.Tag;
import com.tastes.util.LogWrapper;
import com.tastes.util.QueryWrapper;
import com.tastes.widget.ImageAdapter;

import org.apache.http.conn.HttpHostConnectException;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment implements GridView.OnItemClickListener, Button.OnClickListener {
    private static final String ARG_TAG = "tag";
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_AVAILABLE = "available";

    private String tag;
    private double latitude;
    private double longitude;

    private boolean isLocationAvailable;// main의 updated에만 의존할 수 없다. 그 이유는, map에서 manually set 가능하기 때문이다.

    private QueryWrapper queryWrapper;

    ImageLoader imageLoader;

    TextView text;
    Button button;// add btn
    TextView locationText;
    Button locationButton;

    SwipeRefreshLayout refresh;
    GridView grid;
    ImageAdapter adapter;

    View emptyView;

    private MainActivity mActivity;
    private ProfileFragmentCallbacks mCallbacks;

    public static ProfileFragment newInstance(String tag, double latitude, double longitude, boolean isLocationAvailable) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAG, tag);
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        args.putBoolean(ARG_AVAILABLE, isLocationAvailable);
        fragment.setArguments(args);
        return fragment;
    }

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            tag = getArguments().getString(ARG_TAG);
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
            isLocationAvailable = getArguments().getBoolean(ARG_AVAILABLE);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
        ********************** filter ok 하고서 이쪽으로 온다. 즉, view 설정 등이 또 되는데.....
         */
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        text = (RobotoTextView) view.findViewById(R.id.fragment_profile_title);
        text.setText(Tag.HEADER + tag);

        button = (Button) view.findViewById(R.id.fragment_profile_add);
        //button.setVisibility(((MainActivity) getActivity()).getFilterFragment().checkTag(tag) ? View.GONE : View.VISIBLE);
        button.setVisibility(((MainActivity) getActivity()).checkTag(tag) ? View.GONE : View.VISIBLE);
        button.setOnClickListener(this);
        //button.setVisibility(View.GONE);//TODO: 일단, loc icon 때문에, gone시켜둔다.

        locationText = (TextView) view.findViewById(R.id.fragment_profile_location_text);

        locationButton = (Button) view.findViewById(R.id.fragment_profile_location_button);
        locationButton.setOnClickListener(this);

        refresh = (SwipeRefreshLayout) view.findViewById(R.id.fragment_profile_refresh);

        emptyView = view.findViewById(R.id.fragment_profile_empty);

        grid = (GridView) view.findViewById(R.id.fragment_profile_grid);

        adapter = new ImageAdapter(getActivity(), inflater, imageLoader);

        grid.setAdapter(adapter);
        grid.setOnItemClickListener(this);
        grid.setEmptyView(emptyView);
        grid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(grid.getChildCount() > 0 && grid.getChildAt(0).getTop() < 0) {
                    refresh.setEnabled(false);
                } else {
                    refresh.setEnabled(true);
                }
            }
        });

        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() { // 다른 곳에서 set false를 해줘야 되는듯 하다.
                if(!mActivity.isRequestingLocationUpdates()) {// request중이라면, 알아서 setrefresh까지 되어있을 것이다.
                    if(isLocationAvailable) {
                        setRefreshing(true);

                        requestAddress(latitude, longitude);// set loc 처럼. 그리고 arg로 안받는 이유는, 일단 이렇게 해보기 위해서이다.

                        setView();
                    } else {
                        mActivity.startLocationUpdates();
                    }
                }
            }
        });
        // 첫번째 색 말고는 안먹힘. 일단 빼둔다.
        //refresh.setColorSchemeResources(R.color.orange, R.color.orange_dark, R.color.gray, R.color.gray_dark);

        if(!mActivity.isRequestingLocationUpdates()) {// request중이라면, 알아서 setrefresh까지 되어있을 것이다.
            if(isLocationAvailable) {
                setRefreshing(true);

                requestAddress(latitude, longitude);// set loc 처럼. 그리고 arg로 안받는 이유는, 일단 이렇게 해보기 위해서이다.

                setView();
            } else {
                mActivity.startLocationUpdates();
            }
        }

        return view;
    }

    public void setRefreshing(boolean refreshing) {
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

        isLocationAvailable = true;

        requestAddress(latitude, longitude);

        setView();// 특별히 location 관련 check 할 필요는 없을 것 같다.
    }

    public void requestAddress(double latitude, double longitude) {
        Location location  = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        mActivity.requestAddress(location);
    }

    public void setAddress(Address address) {
        if(address != null) {
            String result = null;

            List<String> stringList = new ArrayList<String>();

            //stringList.add(address.getCountryName());
            stringList.add(address.getAdminArea());// 도, 주 정도.
            stringList.add(address.getLocality());// 시 정도.
            stringList.add(address.getSubLocality());// null 확률 있음.(중국으로 치면 현 정도)
            stringList.add(address.getThoroughfare());// 동 정도.
            //stringList.add(address.getSubThoroughfare());// null 확률 있음.(동 이하) => 번지수 될때 있음. 버림.

            for(String string : stringList) {
                if(string != null) {
                    if(result == null) {
                        result = string;
                    } else {
                        result += " " + string;
                    }
                }
            }

            locationText.setText(result);
        }
    }

    public void setEmptyView(int resId) {
        if(isAdded()) {
            TextView text = (TextView) emptyView.findViewById(R.id.fragment_profile_empty_txt);
            text.setText(getString(resId));
        }
    }

    // 이건 setRefreshing이 필요없는 곳에서도 쓰인다. -> 없는듯.
    /*
    public void setLocationFailure() {
        showToast(R.string.location_retry);// TODO: profile visible일 때만 show할지는 고민해보기. -> invisible일 때 여기로 올 일이 일단 없을 것 같다.

        setEmptyView(R.string.img_empty);// 이제 통일.
        //setEmptyView(R.string.location_retry);

        //adapter.setImages(null);// server에서 받아오지 않아도 null이다.
    }
    */

    public void notifyLocationFailure() {
        showToast(R.string.location_retry);// TODO: profile visible일 때만 show할지는 고민해보기. -> invisible일 때 여기로 올 일이 일단 없을 것 같다.

        if(getView() != null) {
            setEmptyView(R.string.img_empty);// 이제 통일.
            //setEmptyView(R.string.location_retry);

            //그대로 남겨두는 것 나쁘지 않다.(어차피 다른 것 더 누르면 바로 null된다.)
            //adapter.setImages(null);// server에서 받아오지 않아도 null이다.

            setRefreshing(false);
        }
    }

    public void notifyNetworkFailure() {
        showToast(R.string.network_retry);// TODO: profile visible일 때만 show할지는 고민해보기. -> invisible일 때 여기로 올 일이 일단 없을 것 같다.

        if(getView() != null) {
            setEmptyView(R.string.img_empty);// 이제 통일.
            //setEmptyView(R.string.network_retry);

            //그대로 남겨두는 것 나쁘지 않다.(어차피 다른 것 더 누르면 바로 null된다.)
            //adapter.setImages(null);// server에서 받아오지 않아도 null이다.

            setRefreshing(false);
        }
    }

    public void onPreLocationUpdate() {
        setRefreshing(true);
    }

    public void showToast(int resId) {
        if(getActivity() != null) {
            ((MainActivity) getActivity()).showToast(resId);
        }
    }

    public void setView() {
        LogWrapper.e("PROFILE", "set view");
        // get tags from pref.
        //=> pull to refresh 등에서는 pref에서 tags 받아올 필요 없을 거라고 생각할수도 있지만, tag를 1번만 받아오는 구조라면, 나중에 display나 filter 등에서 넘어올 때
        // 다른 정보를 더 받아와야 되는 등, 점점더 골치아파진다.(왜냐하면 이 home frag는 한번 실행되면 계속 남아있으므로 pref도 그대로일 것이기 때문.)
        //final List<String> tags = mActivity != null ? mActivity.getFilters() : null;
        // tag를 계속 그대로 이용하려면 그냥 query wrapper의 method를 새로 만들면 그만이지만, 일단 분화 안하는게 변경 가능성 때문에 좋을 것 같다.
        final List<String> tags = new ArrayList<String>();
        tags.add(tag);
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

                //if(isAdded()) {// 재현 자주 되지는 않지만 attatched안됐는데 getString 등 한다고 error 난다. onPost에서는 주로 이렇게 해주라는 의견들이 있다.
                    if(success) {// item update는 사실 network가 안돌아갈 때까지 해줄 필요는 없다고 생각한다. 그럼 괜히 화면만 비어서 이상하다.(그래도 논의해보기.)
                        // manually set empty view string - view가 refresh 위에 있어서 string이 아무때나 보일 수 있다.
                        setEmptyView(R.string.img_empty);
                        // set to adapter.
                        adapter.setImages(images);

                        setRefreshing(false);
                    } else {
                        //Toast.makeText(getActivity(), getString(R.string.upload_network), Toast.LENGTH_SHORT).show();
                        //setEmptyView(getString(R.string.network_retry));
                        notifyNetworkFailure();
                    }
                //}
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
            mCallbacks = (ProfileFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void actionAddClicked() {
        if(mCallbacks != null) {
            mCallbacks.onProfileActionAddClicked(tag);
        }
    }

    public void locationClicked(double latitude, double longitude, boolean isLocationAvailable) {
        if(mCallbacks != null) {
            mCallbacks.onProfileLocationClicked(latitude, longitude, isLocationAvailable);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mCallbacks != null) {
            //mCallbacks.onHomeItemClicked((Image) adapter.getItem(position));
            mCallbacks.onProfileItemClicked(adapter.getImages(), position);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_profile_add:
                button.setVisibility(View.GONE);// enabled까지 다루진 않아도 되지 않을까 싶다.

                actionAddClicked();

                break;
            case R.id.fragment_profile_location_button:
                locationClicked(latitude, longitude, isLocationAvailable);

                break;
        }
    }

    public interface ProfileFragmentCallbacks {
        public void onProfileActionAddClicked(String tag);
        public void onProfileLocationClicked(double latitude, double longitude, boolean isLocationAvailable);
        public void onProfileItemClicked(List<Image> images, int position);
    }
}
