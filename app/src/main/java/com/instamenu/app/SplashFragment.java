package com.instamenu.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.instamenu.R;
import com.instamenu.util.LocationUtils;

public class SplashFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, Button.OnClickListener {
    private static final String ARG_LOCATION = "location";

    // layout
    View layout_location_agree, layout_location_retry;

    // network, location
    private GoogleApiClient mGoogleApiClient;
    private boolean mLocationUpdates = false;// 사실상 동의서다.(pref에 저장된 동의 여부이므로)
    private LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates = false;
    private boolean mLocationUpdated = false;
    private boolean mRequestingLocationFailed = false;
    private double latitude;// = 37.5129273;
    private double longitude;// = 126.9247538;

    private Handler mHandler;
    private Runnable mRunnable;

    private SplashFragmentCallbakcs mCallbacks;

    public static SplashFragment newInstance(boolean location) {
        SplashFragment fragment = new SplashFragment();
        Bundle args = new Bundle();
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
            //mLocationUpdates = getArguments().getBoolean(ARG_LOCATION);
            mLocationUpdates = false;
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

    // connection failed됐을 때 activity로부터 call되어 다시 connect 시도 하게 되는 module.
    public void connect() {
        if(mGoogleApiClient.isConnecting() == false && mGoogleApiClient.isConnected() == false) {
            mGoogleApiClient.connect();
        }
    }

    /*
    내가 만든거다. google location update 예제 자체가 너무 좀 이상하게 되어있었다.
    일단 google play service available이 api client보다 더 넓은 범위가 아니라 api client connected에 종속된 것이라는 실험 결과(?)가 나왔고
    전체적으로 어디서 검사해야 할 지 확실치 않았기 때문에 api client connected 검사하는 쪽이 있으면 전부 해주기로 했다.
    그렇게 하면 전체적으로 봤을 때 예제랑 다른 점은 home(back) 즉 onResume 등에서의 stop을 할 때 client api connection만 검사하는 것이 있었는데 괜찮을지 확인해보면 된다.
     */
    private boolean servicesAvailable() {
        if(mGoogleApiClient.isConnected() == true) {
            if(servicesConnected() == true) {
                return true;
            }
        }

        return false;
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("location-api", "google play service available");

            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(), 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getFragmentManager(), getResources().getString(R.string.app_name));
            }
            return false;
        }
    }

    /*
    available check나 connection failed나 자기 처리 module이 있으므로, 여기서는 그냥
     */
    protected void startLocationUpdates() {
        if(servicesAvailable() == true) {
            if(mRequestingLocationUpdates == false) {
                mRequestingLocationUpdates = true;
                mRequestingLocationFailed = false;

                updateUI();

                createLocationRequest();// 할 때마다 생성해야 num = 1인 것이 문제되지 않는다.
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

                // callback 같은거 특별히 없는듯. 그냥 handler 달고 해야될듯.
                mHandler = new Handler();

                mRunnable = new Runnable(){
                    @Override
                    public void run() {
                        if(mLocationUpdated == false) { // 물론 updated되고 나면 바로 updateUI로 가고 intent 실행되겠지만, 이거랑 시간이 비슷할 경우도 가정하지 않을 수 없다.
                            mRequestingLocationFailed = true;

                            stopLocationUpdates();
                        }
                    }
                };

                mHandler.postDelayed(mRunnable, 10000);
            }
        }
    }

    protected void stopLocationUpdates() {
        if(servicesAvailable() == true) {
            if(mRequestingLocationUpdates == true) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

                // callback이 없는 경우가 있을지 - stop은 무조건 start 이후 나올 수가 있으며 그것도 정확한 1:1 매칭이므로 무조건 callback이 있다고 볼 수 있다.
                mHandler.removeCallbacks(mRunnable);// 여기서 꺼야 재시작 하는 경우 failed가 true가 되지 않아서 항상 onResume에서 다시 시작될 수 있게 해준다.

                mRequestingLocationUpdates = false;

                updateUI();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mLocationUpdates == true && mRequestingLocationFailed == false) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mLocationUpdates == true && mRequestingLocationFailed == false) {
            stopLocationUpdates();
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
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(getActivity(), LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
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
            case R.id.fragment_splash_location_agree_btn:
                // set var and save it to pref.
                mLocationUpdates = true;// callback으로 날려야 한다.
                onLocationAgree();
                // start request.
                startLocationUpdates();

                break;
            case R.id.fragment_splash_location_retry_btn:
                startLocationUpdates();

                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, container, false);

        // init layout
        layout_location_agree = view.findViewById(R.id.fragment_splash_location_agree);
        layout_location_retry = view.findViewById(R.id.fragment_splash_location_retry);

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

    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, getActivity(), LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getFragmentManager(), getResources().getString(R.string.app_name));
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    public interface SplashFragmentCallbakcs {
        public void onSplashLocationUpdated(double latitude, double longitude);
        public void onSplashLocationAgreed();
    }
}
