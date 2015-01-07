package com.instamenu.app;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.instamenu.R;

public class SplashFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, Button.OnClickListener {
    private static final String ARG_NETWORK = "network";
    private static final String ARG_LOCATION = "location";

    // layout
    View layout_network, layout_location_agree, layout_location_retry;

    // network, location
    private boolean mNetworkAvailable = false;// 동의 되어있어도 network는 항상 not available할 때도 있다.
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;// connection 관련한건데 아직 안씀.
    private boolean mLocationUpdates = false;// 사실상 동의서다.(pref에 저장된 동의 여부이므로)
    private LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates = false;
    private boolean mLocationUpdated = false;
    private boolean mRequestingLocationFailed = false;
    private double latitude;// = 37.5129273;
    private double longitude;// = 126.9247538;

    private SplashFragmentCallbakcs mCallbacks;

    public static SplashFragment newInstance(boolean network, boolean location) {
        SplashFragment fragment = new SplashFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_NETWORK, network);
        args.putBoolean(ARG_LOCATION, location);
        fragment.setArguments(args);
        return fragment;
    }
    public SplashFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get from pref(정확히는 pref(main)->args(frag)->get here.
        if (getArguments() != null) {
            mNetworkAvailable = getArguments().getBoolean(ARG_NETWORK);
            mLocationUpdates = getArguments().getBoolean(ARG_LOCATION);
        }

        // init loc
        buildGoogleApiClient();
        //createLocationRequest();
    }

    //---- location
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(((Activity) mCallbacks).getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        // default 60m. 어차피 1번이므로 상관없다.
        //mLocationRequest.setInterval(100000);
        // default 60/6 = 10m. 이것도 상관없다.
        //mLocationRequest.setFastestInterval(100000);
        // 100m 정도 오차. 시간은 1초 정도. network만 쓰는게 아니라, balnaced인데, 실내에선 gps만 켜면 gps가 안잡혀서 그래서 안되는듯. 결국, msg도 그런거 고려해서 띄워주기.
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        // high도 마찬가지로 실내에선 gps만 켜놓으면 안잡힌다. 하지만 나머지에 대해서는 속도 차이 모르겠고, 정확도가 사실 중요하다. 오히려 실외에서 gps만 있을 때 high는 잡아도 balanced는 못잡을 수 있으므로 이걸로 간다.
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setNumUpdates(1);
        mLocationRequest.setExpirationDuration(10000);// 혹시나 이게 자체적으로 stop을 유발하는건 아닌지... 확인하고 싶지만 방법을 아직 못찾았다.
    }

    // 편의를 위해 이렇게 만들고 사용한다.
    public void switchView(View view) {
        // hide all the views.
        layout_network.setVisibility(View.GONE);
        layout_location_agree.setVisibility(View.GONE);
        layout_location_retry.setVisibility(View.GONE);

        // null일 때도 있다.(모두 hide시키고 싶을 때)
        if(view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void updateUI() {
        if(mLocationUpdates == false) {// 동의 UI
            switchView(layout_location_agree);
        } else {
            if(mRequestingLocationUpdates == true) {
                switchView(null);
            } else {
                if(mRequestingLocationFailed == true) {
                    switchView(layout_location_retry);
                } else {// stop 자체에서 할까 하다가 그래도 통일성 맞추려고 여기로 넣었다.(intent 쓰는 것이겠지만 ui랑 완전히 다르다고 볼 것 까진 없기 때문에)
                    if(mLocationUpdated == true) { // 굳이 이렇게 다시 나눌 필요는 없지만...
                        onLocationUpdated();
                    }
                }
            }
        }
    }

    protected void startLocationUpdates() {
        mRequestingLocationUpdates = true;
        mRequestingLocationFailed = false;

        updateUI();

        createLocationRequest();// 할 때마다 생성해야 num = 1인 것이 문제되지 않는다.
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        // callback 같은거 특별히 없는듯. 그냥 handler 달고 해야될듯.
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                if(mLocationUpdated == false) { // 물론 updated되고 나면 바로 updateUI로 가고 intent 실행되겠지만, 이거랑 시간이 비슷할 경우도 가정하지 않을 수 없다.
                    mRequestingLocationFailed = true;

                    stopLocationUpdates();
                }
            }
        }, 10000);
    }

    protected void stopLocationUpdates() {
        /* 위치 받는중 종료시 나오는 exception. 해결하기.
        12-30 01:17:47.106    2826-2826/com.instamenu E/AndroidRuntime﹕ FATAL EXCEPTION: main
    Process: com.instamenu, PID: 2826
    java.lang.IllegalStateException: GoogleApiClient is not connected yet.
            at com.google.android.gms.internal.jx.a(Unknown Source)
            at com.google.android.gms.common.api.c.b(Unknown Source)
            at com.google.android.gms.internal.nf.removeLocationUpdates(Unknown Source)
            at com.instamenu.app.SplashFragment.stopLocationUpdates(SplashFragment.java:149)
            at com.instamenu.app.SplashFragment$1.run(SplashFragment.java:142)
            at android.os.Handler.handleCallback(Handler.java:733)
            at android.os.Handler.dispatchMessage(Handler.java:95)
            at android.os.Looper.loop(Looper.java:146)
            at android.app.ActivityThread.main(ActivityThread.java:5698)
            at java.lang.reflect.Method.invokeNative(Native Method)
            at java.lang.reflect.Method.invoke(Method.java:515)
            at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1291)
            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1107)
            at dalvik.system.NativeStart.main(Native Method)
         */
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        mRequestingLocationUpdates = false;

        updateUI();
    }

    @Override
    public void onStart() {
        super.onStart();

        if(!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mLocationUpdates == true && mRequestingLocationFailed == false) {
            if(mGoogleApiClient.isConnected() && mRequestingLocationUpdates == false) {
                startLocationUpdates();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mLocationUpdates == true && mRequestingLocationFailed == false) {
            if(mGoogleApiClient.isConnected() && mRequestingLocationUpdates == true) {
                stopLocationUpdates();
            }
        }
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();// 왜인진 모르겠지만 일단 isConnected 확인 안해도 된다고 생각한다.

        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if(mLocationUpdates == true && mRequestingLocationFailed == false) {
            if(mRequestingLocationUpdates == false) {
                startLocationUpdates();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        mLocationUpdated = true;

        stopLocationUpdates();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_splash_network_btn:
                break;
            case R.id.fragment_splash_location_agree_btn:
                // set var and save it to pref.
                mLocationUpdates = true;// callback으로 날려야 한다.
                onLocationAgree();
                // start request.
                if(mGoogleApiClient.isConnected()) {
                    startLocationUpdates();
                }

                break;
            case R.id.fragment_splash_location_retry_btn:
                if(mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
                    startLocationUpdates();
                }

                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, container, false);

        // init layout
        layout_network = view.findViewById(R.id.fragment_splash_network);
        layout_location_agree = view.findViewById(R.id.fragment_splash_location_agree);
        layout_location_retry = view.findViewById(R.id.fragment_splash_location_retry);

        ((Button) view.findViewById(R.id.fragment_splash_network_btn)).setOnClickListener(this);
        ((Button) view.findViewById(R.id.fragment_splash_location_agree_btn)).setOnClickListener(this);
        ((Button) view.findViewById(R.id.fragment_splash_location_retry_btn)).setOnClickListener(this);

        if(mLocationUpdates == false) {
            updateUI();// 전체를 안하고 이렇게 false일 때만 하는 이유는, 나머지에 대해서는 이 onCreate이후에 알아서 되기 때문이다.
        }

        return view;
    }

    public void onLocationUpdated() {
        if (mCallbacks != null) {
            mCallbacks.onSplashLocationUpdated(latitude, longitude);
        }
    }

    public void onLocationAgree() {
        if (mCallbacks != null) {
            mCallbacks.onSplashLocationAgreed();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (SplashFragmentCallbakcs) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public interface SplashFragmentCallbakcs {
        public void onSplashLocationUpdated(double latitude, double longitude);
        public void onSplashLocationAgreed();
    }
}
