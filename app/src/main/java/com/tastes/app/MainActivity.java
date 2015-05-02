package com.tastes.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.commonsware.cwac.camera.PictureTransaction;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.tastes.R;
import com.tastes.content.Image;
import com.tastes.util.Constants;
import com.tastes.util.LocationUtils;
import com.tastes.util.LogWrapper;
import com.tastes.util.QueryWrapper;
//import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.apache.http.conn.HttpHostConnectException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationSource, LocationListener, SplashFragment.SplashFragmentCallbakcs, ViewPagerFragment.ViewPagerFragmentCallbacks, /*CameraHostProvider, */CameraFragment_.CameraFragmentCallbacks, GalleryFragment.GalleryFragmentCallbacks, PictureFragment.PictureFragmentCallbacks, DisplayFragment.DisplayFragmentCallbacks, HomeFragment.HomeFragmentCallbacks, ProfileFragment.ProfileFragmentCallbacks, MapFragment_.MapFragmentCallbacks, FilterFragment.FilterFragmentCallbacks, ItemFragment.ItemFragmentCallbacks, PasscodeFragment.PasscodeFragmentCallbakcs {

    private QueryWrapper queryWrapper;

    private SharedPreferences preferences;
    //private boolean defaultTag;
    private List<String> tags;
    private List<String> switches;
    private String strTags;
    private String strSwitches;
    private static final String TAG_DEFAULT = "tastes";

    private boolean flag_fragment_splash = true;// 위치 잡히면 frag remove하면서 false되고, 나머지에 대해서는 true가 되어서 back key 때 무조건 finish되게 한다.
    private boolean flag_fragment_home = false;// except for home, default back key processing.
    private boolean flag_fragment_profile = false;
    private boolean flag_fragment_item = false;
    private boolean flag_fragment_display = false;// 보낼 때 true가 된다.
    private boolean flag_taking_camera = false;// fragment 관련이 아니라, cam frag에서 taking 중일 때만 true되어서 back을 막아주는 역할을 하는 flag이다.
    private boolean flag_fragment_gallery = false;// back pressed 때문에 필요.
    private boolean flag_fragment_filter = false;
    private boolean flag_fragment_map = false;

    //private SlidingMenu slidingMenu;

    private SplashFragment splashFragment;
    private CameraFragment_ cameraFragment;
    private GalleryFragment galleryFragment;
    private DisplayFragment displayFragment;
    private HomeFragment homeFragment;
    private ProfileFragment profileFragment;
    private ItemFragment itemFragment;
    private FilterFragment filterFragment;
    private ViewPagerFragment viewPagerFragment;
    private MapFragment_ mapFragment;

    // location
    private GoogleApiClient mGoogleApiClient;
    private boolean mLocationUpdates = false;// 사실상 동의서다.(pref에 저장된 동의 여부이므로)
    private LocationRequest mLocationRequest;
    private LocationManager mLocationManager;
    private OnLocationChangedListener mLocationListener = null;// for map current test
    private boolean mRequestingLocationUpdates = false;
    private boolean mLocationUpdated = false;
    private boolean mRequestingLocationFailed = false;
    private double latitude;
    private double longitude;

    private Handler mHandler;
    private Runnable mRunnable;

    private static final int LOCATION_TIMEOUT = 5000;

    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     * The user requests an address by pressing the Fetch Address button. This may happen
     * before GoogleApiClient connects. This activity uses this boolean to keep track of the
     * user's intent. If the value is true, the activity tries to fetch the address as soon as
     * GoogleApiClient connects.
     */
    //protected boolean mAddressRequested;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;

    private static final boolean ROTATE_TAG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init receiver
        mResultReceiver = new AddressResultReceiver(new Handler());

        // init loc
        buildGoogleApiClient();
        setLocationManager();

        // init vars.
        queryWrapper = new QueryWrapper();

        // init preferences
        initPreferences();

        // init imageloader before other fragments' init.
        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(this)
                //.threadPoolSize(3) Default
                //.threadPriority(Thread.NORM_PRIORITY - 2) Default
                .denyCacheImageMultipleSizesInMemory() // image size 변할 상황 없으므로 deny.
                // 쓰려면 100mb정도는 써야 1page의 vp를 담당할 수 있을텐데, 가볍게 가자는 취지로 일단 하지 않는다.(options에서도 false)
                //.memoryCache(new LruMemoryCache(100 * 1024 * 1024)) default는 lru에다가 size는 app available size의 1/8이라 한다.
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(configuration);

        cameraFragment = CameraFragment_.newInstance();

        //galleryFragment = GalleryFragment.newInstance(/*latitude, longitude*/);

        homeFragment = HomeFragment.newInstance(latitude, longitude, mLocationUpdated);

        //filterFragment = FilterFragment.newInstance(/*defaultTag, */tags, switches);

        viewPagerFragment = ViewPagerFragment.newInstance();
        replaceFragment(R.id.container, viewPagerFragment);

        splashFragment = SplashFragment.newInstance(mLocationUpdates);
        //splashFragment = SplashFragment.newInstance(false);
        addFragment(splashFragment);
    }

    public CameraFragment_ getCameraFragment() {
        return cameraFragment;
    }
/*
    public GalleryFragment getGalleryFragment() {
        return galleryFragment;
    }
*/
    public DisplayFragment getDisplayFragment() {
        return displayFragment;
    }

    public HomeFragment getHomeFragment() {
        return homeFragment;
    }
/*
    public FilterFragment getFilterFragment() {
        return filterFragment;
    }
*/
    //---- location
    private void setLocationManager() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void addGpsListener() {
        mLocationManager.addGpsStatusListener(gpsListener);
    }

    private void removeGpsListener() {
        mLocationManager.removeGpsStatusListener(gpsListener);
    }

    GpsStatus.Listener gpsListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            LogWrapper.e("GPS", "gps " + event);
            switch(event) {
                //TODO: START에서의 GPS ENABLED CHECK에서 USER AGREEMENT 얻기 전에 FAILURE로 넘어간다.(거의)
                //TODO: 그래서, 아무래도 START에서 GPS ENABLED CHECK를 할지 말지를 정해주는 VAR를 넣어야 될 것 같다.
                case GpsStatus.GPS_EVENT_STARTED://TODO: SWITCH 변환 없이 그냥 바꾸는 것(GPS, WIFI, HIGH 등 서로 변환)은 이쪽으로 오지 않는다.
                    startLocationUpdates(true);// 어차피 requesting check는 내부에서 한다.

                    break;
                //TODO: FAILURE이 되어도 HOME NOT VIEWING이라서 TOAST가 뜨지 않는다. => 하지만 어쩔 수 없다. 정상.
                //TODO: 하지만, GPS ON EVENT 뒤에서야만 OFF EVENT가 발생된다. DEFAULT ON에서는 OFF가 CATCH되지 않는다.
                case GpsStatus.GPS_EVENT_STOPPED:
                    locationUpdatesFailure(false);// pause처럼 그냥 stop하기에는 failure 변수가 set되지 않는다.
                    // 아마도 settings 등 background 때문에 pause랑 겹치면 여기만 실행될 듯 하다.

                    break;
                    /*case GpsStatus.GPS_EVENT_FIRST_FIX:
                        //isGpsFixed = true;

                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        //if(mLo)
                        //isGpsFixed = (SystemClock.elapsedRealtime() - mLastLocationTime) < LOCATION_TIMEOUT;

                        break;*/
            }
        }
    };

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mLocationListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        mLocationListener = null;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        //TODO: 실내 많을 것이라는 생각에 좀 그랬지만, 그래도 TIMEOUT 늘리더라도 GPS부터 하게하는게 실제 사용성 상 더 좋다. 어차피 오래 안쓴다.
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER); 너무 넓음.
        //mLocationRequest.setNumUpdates(5);
        //mLocationRequest.setSmallestDisplacement(3);
        mLocationRequest.setInterval(0);// 그냥 놔두면 바로 안된다.
        //mLocationRequest.setExpirationDuration(); update 하나의 duration을 의미하는 것이다. 현재는 별 필요 없다.
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
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("location-api", "google play service available");

            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getFragmentManager(), getResources().getString(R.string.app_name));
            }
            return false;
        }
    }

    public boolean isLocationEnabled() {
        boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return gpsEnabled || networkEnabled;
    }

    // 기존 method들 위해.
    protected void startLocationUpdates() {
        startLocationUpdates(false);
    }
    /*
    available check나 connection failed나 자기 처리 module이 있으므로, 여기서는 그냥
     */
    protected void startLocationUpdates(boolean isGpsTurning) {
        if(servicesAvailable() == true) {
            if(mRequestingLocationUpdates == false) {
                mLocationUpdated = false;
                mRequestingLocationUpdates = true;
                mRequestingLocationFailed = false;

                if(isLocationEnabled() || isGpsTurning) {
                    onPreLocationUpdate();

                    createLocationRequest();// 할 때마다 생성해야 num = 1인 것이 문제되지 않는다.
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

                    // callback 같은거 특별히 없는듯. 그냥 handler 달고 해야될듯.
                    mHandler = new Handler();

                    mRunnable = new Runnable(){
                        @Override
                        public void run() {
                            // 그리고 어차피 이건 기본 조건으로서 있으면 좋은 것이다.
                            if(!mLocationUpdated) {// map 없는 상황에서는 이미 remove 되었을 것이지만, 있는 상황에서는 여기가 있어야만 들어가지 않게 된다.
                                locationUpdatesFailure(true);
                            }
                        }
                    };

                    //TODO: gps turning중인 경우는, dialog로 agreement가 떠서, failure 좀 겹칠 수 있다. 하지만 onresult에서 처리하기 쉽지 않으므로 일단 pass.
                    mHandler.postDelayed(mRunnable, LOCATION_TIMEOUT);
                } else {
                    locationUpdatesFailure(false);
                }
            }
        }
    }

    public void locationUpdatesFailure(boolean remove) {// TODO: map에서 잘 받던 도중 gps off 시에도 쓸 수 있도록 따로 만든다.
        //TODO: 여기서, 그냥 포기하지 말고, LAST KNOWN LOCATION이라도 받아본다.
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(location != null) {
            //TODO: onChanged랑 같지만 remove 달라서 새로 씀.
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            mLocationUpdated = true;

            if(mapFragment == null) {// map not null이면 계속 간다.
                stopLocationUpdates(remove, true);
            } else {// stop 없는 update.
                //TODO: 사실 LAST까지 SOURCE로 인해 되는지는 잘 모르겠다. 만약 LAST를 받는 것이라면, 아마 이 FAILURE쪽으로 넘어오지 않았을 것이다.
                //onLocationUpdated();
            }
        } else {
            mLocationUpdated = false;// main에서의 실패에서는 중복되지만 그냥 넘어간다.
            mRequestingLocationFailed = true;

            stopLocationUpdates(remove, true);// map에서는 이를 통해 결국 notify로 넘어간다.
        }
        /*
        mLocationUpdated = false;// main에서의 실패에서는 중복되지만 그냥 넘어간다.
        mRequestingLocationFailed = true;

        stopLocationUpdates(remove, true);// map에서는 이를 통해 결국 notify로 넘어간다.
        */
    }

    protected void stopLocationUpdates(boolean remove, boolean updates) {// updates를 할건지, 그냥 stop할건지.(map에서 나갈때는 그냥 stop일 것이다.)
        if(servicesAvailable() == true) {
            if(mRequestingLocationUpdates == true) {
                if(remove) {
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

                    // callback이 없는 경우가 있을지 - stop은 무조건 start 이후 나올 수가 있으며 그것도 정확한 1:1 매칭이므로 무조건 callback이 있다고 볼 수 있다.
                    if(mHandler != null) {
                        mHandler.removeCallbacks(mRunnable);// 여기서 꺼야 재시작 하는 경우 failed가 true가 되지 않아서 항상 onResume에서 다시 시작될 수 있게 해준다.
                    }
                }

                mRequestingLocationUpdates = false;

                if(updates) {
                    onLocationUpdated();
                }
            }
        }
    }

    public void onPreLocationUpdate() {
        if(mapFragment != null) {
            mapFragment.onPreLocationUpdate();
        } else {
            if(profileFragment != null) {
                profileFragment.onPreLocationUpdate();
            } else {
                if(homeFragment != null) {
                    homeFragment.onPreLocationUpdate();
                }
            }
        }
    }

    public void onLocationUpdated() {
        // 당장 필요한건 update success시 home setview 하는 것 뿐일듯 하다.... display도 있을듯.
        if(mLocationUpdated) {
            if(mapFragment != null) {// 일반 fragment들에 넘어가면 안된다. map에만 전달.
                //TODO: 현재 일단 제외 상태다. 하지만 BLOCK은 유지할 필요 있다.
                //mapFragment.setCurrentLocation(latitude, longitude);
            } else {// 여기 있는 것들은 이것들 내부에서 recall하는 일 없는 한(없다.) 1회만 실행된다.(아직은 이 방식 유지)
                /*if(galleryFragment != null) {
                    galleryFragment.setLocation(latitude, longitude);
                }*/
                if(displayFragment != null) {
                    displayFragment.setLocation(latitude, longitude);
                }
                /*
                여러개 있으면 최상위만 될것이고, 최상위 back되어도 2순위가 되는게 아니라 null일 것이다.
                하지만 사실 home이든 profile이든 단 1번만 위치 받으면 되며, profile은 현재 none-location 상태로 2개 이상 존재할 수 없는 구조이다.
                따라서 현재로서는 아무 문제가 없다. 추후 필요하다면 profileFragment ref 관리 data structure 만들어서 관리하던가 하도록 한다.
                -> 이제 단1번만 받으면 되는게 아니라 여러번 받을 수 있으므로, 같이 위치 받는 일은 없는게 좋다.
                 */
                if(profileFragment != null) {
                    profileFragment.setLocation(latitude, longitude);
                } else {
                    if(homeFragment != null) {// null인 상황은 애초에 onLocationChanged에서 저장되는 latlng가 자동으로 homeFrag 생성시 전달되어 해결될 것이다.
                        homeFragment.setLocation(latitude, longitude);
                    }
                }
            }
        } else {
            if(mRequestingLocationFailed) {
                if(mapFragment != null) {
                    mapFragment.notifyLocationFailure();
                } else {
                    if(profileFragment != null) {
                        profileFragment.notifyLocationFailure();
                    } else {
                        if(homeFragment != null) {
                            homeFragment.notifyLocationFailure();
                        }
                    }
                }
            }
        }
    }

    public boolean isLocationUpdated() {
        return mLocationUpdated;
    }

    public boolean isRequestingLocationUpdates() {
        return mRequestingLocationUpdates;
    }

    public boolean isRequestingLocationFailed() {
        return mRequestingLocationFailed;
    }

    public LatLng getLocation() {// 일단 map에서 cur 받을 때 쓰기위해.
        return new LatLng(latitude, longitude);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mLocationUpdates == true/* && mRequestingLocationFailed == false*/) {
            addGpsListener();
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mLocationUpdates == true/* && mRequestingLocationFailed == false*/) {
            stopLocationUpdates(true, false);//TODO: UPDATES 없게 하는게 맞는지 확인해보기.
            removeGpsListener();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();// 왜인진 모르겠지만 일단 isConnected 확인 안해도 된다고 생각한다.

        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if(mLocationUpdates == true/* && mRequestingLocationFailed == false*/) {
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
                connectionResult.startResolutionForResult(this, LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

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

        if(mapFragment == null) {// map not null이면 계속 간다.
            stopLocationUpdates(true, true);
        } else {// stop 없는 update.
            //TODO: 현재는 할 필요 없다.
            //onLocationUpdated();//TODO: map을 위한 것 따로 만들어도 되겠지만 일단 간다.
        }

        if(mLocationListener != null) {
            mLocationListener.onLocationChanged(location);
        }
    }

    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

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

    // from map, home(, profile), item
    public void notifyAddress(Address address, String errorMessage) {// 그리고 아직 err msg는 안쓰고 그냥 address null인 것을 이용한다.
        // 이런 구조의 의미는, home 중 item 중 map 까지 켜지는 경우도 있을 수 있다는 것이다.
        if(mapFragment != null) {
            mapFragment.setPointerAddress(address);
        } else {
            /*
            if(itemFragment != null) {
                itemFragment.setAddress(address);
            } else */{
                if(profileFragment != null) {
                    profileFragment.setAddress(address);
                } else {
                    if(homeFragment != null) {
                        homeFragment.setAddress(address);
                    }
                }
            }
        }
    }

    /**
     * Runs when user clicks the Fetch Address button. Starts the service to fetch the address if
     * GoogleApiClient is connected.
     */
    public void requestAddress(Location location) {
        // We only start the service to fetch the address if GoogleApiClient is connected.
        if (mGoogleApiClient.isConnected()) {
            startIntentService(location);
        } else {
            //TODO: coords로 하라고 알려준다.
            //Toast.makeText(MainActivity.this, "address failed - not connected", Toast.LENGTH_SHORT).show();
            notifyAddress(null, getString(R.string.msg_google_api_not_available));
        }
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected void startIntentService(Location location) {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, AddressService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            Address result = resultData.getParcelable(Constants.RESULT_DATA_KEY);
            String extra = resultData.getString(Constants.RESULT_DATA_EXTRA);
            //displayAddressOutput();

            // 현재는 resultCode 크게 상관없다.
            notifyAddress(result, extra);

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            //mAddressRequested = false;
            //updateUIWidgets();
        }
    }

    public void replaceFragment(int containerViewId, Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.replace(containerViewId, fragment);// replace.

        fragmentTransaction.commit();
    }

    public void addFragment(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.add(R.id.container, fragment);// with null tag
        fragmentTransaction.addToBackStack(null);// name 필요없다.

        fragmentTransaction.commit();
    }

    public void addFragment(Fragment fragment, int enter, int exit) {// for frag anim. 크게 수정될 일 없기에 일단 통합 안하고 놔둔다.
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.setCustomAnimations(enter, exit, enter, exit);
        fragmentTransaction.add(R.id.container, fragment);// with null tag
        fragmentTransaction.addToBackStack(null);// name 필요없다.

        fragmentTransaction.commit();
    }

    public void popFragment() {
        FragmentManager fragmentManager = getFragmentManager();

        if(fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        }
    }

    //---- pref - 일단 분리해서 만들어둔다.
    public ArrayList<String> getFilters() {
        // get switch-on tags.
        ArrayList<String> filters = null;
/*
        if(defaultTag) {
            if(filters == null) {
                filters = new ArrayList<String>();
            }

            filters.add("tastes");
        }
*/
        if(tags != null) {
            for(int i = 0; i < tags.size(); i++) {
                if(filters == null) {
                    filters = new ArrayList<String>();
                }

                if(switches.get(i).equals("true")) {
                    filters.add(tags.get(i));
                }
            }
        }

        return filters;
    }

    public ArrayList<String> getList(String string) {
        if(string != null) {
            return new ArrayList<String>(Arrays.asList(string.split("\\|")));
        } else {
            return null;
        }
    }

    public String getString(List<String> list) {
        String string = null;

        if(list != null) {
            for(String item : list) {
                if(string == null) {
                    string = new String(item);
                } else {
                    string += ("|" + item);
                }
            }
        }

        return string;
    }

    public void initPreferences() {
        preferences = getSharedPreferences("tastes", Context.MODE_PRIVATE);

        //defaultTag = preferences.getBoolean("DefaultTag", true);

        mLocationUpdates = preferences.getBoolean("LocationUpdates", false);
        //mLocationUpdates = false;

        strTags = preferences.getString("Tags", null);
        tags = getList(strTags);

        strSwitches = preferences.getString("Switches", null);
        switches = getList(strSwitches);

        //LogWrapper.e("INIT PREF", strTags+","+strSwitches);
        initDefaultTag();
    }

    // home, filter 등에서 사용되기 전에 바로 이렇게 해준다.
    public void initDefaultTag() {
        if(tags == null) {// null이면 바로 추가.
            tags = new ArrayList<String>();
            switches = new ArrayList<String>();

            tags.add(TAG_DEFAULT);
            switches.add("true");
        } else {// 아니면 검색후 삽입(첫번째에)
            if(tags.contains(TAG_DEFAULT) == false) {
                tags.add(0, TAG_DEFAULT);
                switches.add(0, "true");
            }
        }
    }

    public void setPreferences(/*boolean defaultTag, */List<String> tags, List<String> switches) {
        SharedPreferences.Editor editor = preferences.edit();

        // default부터 해준다. - 결국 이것이 filter가 여닫히지 않으면 저장안되겠지만 init자체에서의 defaultTag의 default가 true이므로 일단 문제없다.
        //this.defaultTag = defaultTag;
        //editor.putBoolean("DefaultTag", defaultTag);

        // tag만으로 비교해도 된다.
        if(tags != null) {
            // 그냥 했더니 filter의 tag adapter의 lists 변할때마다 같이 변해서 일이 안된다.
            this.tags = new ArrayList<String>(tags);
            this.switches = new ArrayList<String>(switches);
            strTags = getString(this.tags);
            strSwitches = getString(this.switches);
            editor.putString("Tags", strTags);
            editor.putString("Switches", strSwitches);
        } else {
            if(this.tags != null) {
                strTags = null; // 삭제해주지 않아도 쓸 데는 없는것 같지만.
                strSwitches = null; // 삭제해주지 않아도 쓸 데는 없는것 같지만.
                editor.remove("Tags");
                editor.remove("Switches");
            }
        }

        editor.commit();
    }

    public void setPreferences(boolean locationUpdates) {
        mLocationUpdates = locationUpdates;// local에는 보관할 필요 없지만, 그냥 해둔다.

        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean("LocationUpdates", locationUpdates);

        editor.commit();
    }

    public void addPreferences(List<String> tags, List<String> switches) {
        if (tags != null && switches != null) {
            if (this.tags == null) {
                this.tags = new ArrayList<String>();
            }

            if (this.switches == null) {
                this.switches = new ArrayList<String>();
            }

            for (int i = 0; i < tags.size(); i++) {
                if (this.tags.contains(tags.get(i)) == false) {
                    this.tags.add(tags.get(i));
                    this.switches.add(switches.get(i));
                } else {
                    int index = this.tags.indexOf(tags.get(i));
                    if (this.switches.get(index).equals(switches.get(i)) == false) {
                        this.switches.set(index, switches.get(i));
                    }
                }
            }

            String strTags = getString(this.tags);
            String strSwitches = getString(this.switches);

            // 사실 null check 따로 해줄 필요는 없다.(둘 다 동시에 null 또는 not null이 아니라면 그 자체로 문제가 되므로.)
            if(strSwitches != null && strSwitches.equals(this.strSwitches) == false) {
                SharedPreferences.Editor editor = preferences.edit();

                this.strSwitches = strSwitches;

                editor.putString("Switches", this.strSwitches);

                if(strTags != null && strTags.equals(this.strTags) == false) {
                    this.strTags = strTags;

                    editor.putString("Tags", this.strTags);
                }

                editor.commit();
            }
        }

        //LogWrapper.e("ADD PREF", strTags+","+strSwitches);
    }

    public boolean checkTag(String tag) {
        return (tags != null && tags.contains(tag));
    }

    // profile에서만 쓰이는 것인데, 일단 refresh까지 해준다.(사실 camera-display-home에서는 tag 추가가 안되므로 형평성 안맞을 수 있으나, 그건 복잡성 줄이기 위한 조치이므로 그냥 둔다.
    public void addTag(String tag) {
        // 오류 최소화하기 위해 이렇게까지도 한다.
        List<String> tags_ = tags != null ? new ArrayList<String>(tags) : new ArrayList<String>();
        List<String> switches_ = switches != null ? new ArrayList<String>(switches) : new ArrayList<String>();

        tags_.add(tag);
        switches_.add("true");

        setTag(tags_, switches_);
    }

    // addTag from profile 및 filter close에서 쓰이기 위해 따로 뺐다.
    public void setTag(List<String> tags, List<String> switches) {
        // set to pref.
        setPreferences(/*defaultTag, */tags, switches);
        // update home
            /*
            위치는 어차피 home 자체적으로 표시되고 해결되게 되어있다.
            따라서 여기서는 위치가 없으면 pass, 있으면 진행하면 된다.
            (사실 onRefres에서 위치가 있는 부분만 떼온 것이나 다름없다고 보면 된다.)
             */
        //if(mLocationUpdated) {//TODO: 이것도 HOME의 LOC AVAILABLE CHECK로 바꿔야 한다.
        if(homeFragment.isLocationAvailable()) {
            homeFragment.setRefreshing(true);
            homeFragment.setView();
        }
    }

    //---- splash
    public boolean isSplashViewing() {
        return flag_fragment_splash;
    }

    @Override
    public void onSplashFinished() {
        // 닫기 전에 flag부터 false로 해준다.
        flag_fragment_splash = false;

        // camera도 tools 복귀시켜주고 flip도 해준다.
        cameraFragment.setToolsVisible(true);
        //cameraFragment.flip();// 느리면 더 앞으로 당긴다.

        // fragment닫아야 한다.
        popFragment();
    }

    @Override
    public void onSplashLocationAgreed() {
        // 일단 debug동안은 disable.
        setPreferences(true);
        // start request.
        startLocationUpdates();
    }

    //---- vp
    @Override
    public void onViewPagerPageSelected(int position) {
        switch(position) {
            /*case 0:// Gallery
                flag_fragment_gallery = true;

                break;*/
            case 0:// Camera
                // camera flag는 없으므로 pass.
                /*if(flag_fragment_home) {
                    flag_fragment_home = false;

                    homeFragment.clearEdit();
                } else {
                    flag_fragment_gallery = false;
                }*/
                flag_fragment_home = false;

                homeFragment.clearEdit();

                break;
            case 1:// Home
                flag_fragment_home = true;
/*
                if(flag_fragment_filter == true) {
                    flag_fragment_filter = false;

                    filterFragment.closeFilter();

                    //filterFragment.clearEdit(); 이것도 일단 안해도 돌아간다.

                    //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                }
*/
                break;
            /*case 2:
                flag_fragment_home = false;
                flag_fragment_filter = true;

                //homeFragment.clearEdit(); 일단 안해도 돌아간다.

                //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                break;*/
        }
    }
/*
    public void setViewPagerEnabled(boolean enabled) {
        viewPagerFragment.setEnabled(enabled);
    }
*/
    //---- camera
    @Override
    public void onTakePicture(String mode_flash) {
        flag_taking_camera = true;

        // =====> button disabled하는 code 필요.
        cameraFragment.setShotButtonEnabled(false);

        PictureTransaction pictureTransaction = new PictureTransaction(cameraFragment.getHost());

        pictureTransaction.flashMode(mode_flash);

        cameraFragment.takePicture(pictureTransaction);

        /*
        찍기 전에 started 상태가 되어야 된다는 말이, 어차피 초기에는 started이므로 찍은 후 restart하면 되는건데, onSave에서 했더니 flash 등에서의 term에서 back시 npe 생긴다.
        여기서 한다 하더라도 back시 display가 뜨시 않은 상태일 수 있고 다시말해 cam frag만 있는 상태에서 finish 될 수도 있지만, 이걸 아직 막을 수 있는 방법이...
        예를 들어 찍는 순간 flag 켜고, onBack에서 그거 보고서 막고, onSave때 끈다면 가능은 할 것 같다.
         */
        //cameraFragment.stopPreview_();
    }

    @Override
    public void onSaveImage(boolean mirror, byte[] image, int rotation) {
        // 필요한지 test 한번 해봤는데, 필요하다. ㅡㅡ... 꼭 해봐야 아는가...
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // ====> shot button 때 필요할수도....
                cameraFragment.setShotButtonEnabled(true);

                // 여기서 해야 되는 것 같다. 아무튼, default를 resize로 하고, display에서만 바꿔주는게 맞을 듯 하다. 반대로 하면 vp에서 계속 change되어서 더 느릴듯 하다.
                //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            }
        });

        /*
        take는 했는데 save 전에 back 하면(flash가 길어서 그럴 수 있다.) restart 중에 npe 나서 take에다가 restart를 넣었었다.
        그런데 s3는 되고 s5는 preview stop되어 있는걸 보니 안되겠어서 다시 여기로 옮겼다.
        어차피 flag에 의해서 back은 안되므로 npe 걱정은 없을 것이다.(kill도 확인은 해보기.)
         */
        cameraFragment.restartPreview();

        if(ROTATE_TAG) {
            if(rotation == -90 || rotation == 90) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }

        // set flag
        flag_fragment_display = true;
        //DisplayFragment.imageToShow = image;
        displayFragment = DisplayFragment.newInstance(mirror, image, ROTATE_TAG ? rotation : 0, latitude, longitude, mLocationUpdated);// 여기서도 보내야 location process 잘 맞아떨어진다.
        addFragment(displayFragment);

        flag_taking_camera = false;// 최대한 늦게 하는게, 답답할 수도 있겠지만 잘못된 방향으로 흘러가는걸 막아줄 수 있다.
    }

    //---- gallery
    @Override
    public void onGalleryItemClicked(Image image, int rotation) {
        // set flag
        flag_fragment_display = true;

        // image에서 받아와서 하자니, 작은 갭이 있어서 이렇게 직접 한다.
        double latitude_ = image.distance > -2 ? image.latitude : latitude;
        double longitude_ = image.distance > -2 ? image.longitude : longitude;
        boolean isLocationAvailable = image.distance > -2 || mLocationUpdated;

        displayFragment = DisplayFragment.newInstance(false, image.origin, Long.valueOf(image.time), rotation, latitude_, longitude_, isLocationAvailable);// 여기서도 보내야 location process 잘 맞아떨어진다.

        addFragment(displayFragment);

        /*Fragment fragment = PictureFragment.newInstance(position, rotation, latitude, longitude, mLocationUpdated);

        addFragment(fragment);*/
    }

    //---- picture
    @Override
    public void onPictureForwardClicked(Image image, boolean isLocationAvailable) {
        // set rotation : 이것도 일단 아무 값이나 둔다.
        int rotation = 0;
        // set flag
        flag_fragment_display = true;
        //DisplayFragment.imageToShow = image;
        displayFragment = DisplayFragment.newInstance(false, image.origin, Long.valueOf(image.time), ROTATE_TAG ? rotation : 0, image.latitude, image.longitude, isLocationAvailable);// 여기서도 보내야 location process 잘 맞아떨어진다.

        addFragment(displayFragment);
    }

    //---- display
    public boolean isDisplayViewing() {
        return flag_fragment_display;
    }

    @Override
    public void onDisplayForwardClicked(double latitude, double longitude, boolean isLocationAvailable) {
        flag_fragment_map = true;

        mapFragment = MapFragment_.newInstance(latitude, longitude, isLocationAvailable, false);

        addFragment(mapFragment);
    }

    @Override
    public void onDisplayUpload(final byte[] file, final long time, final double latitude, final double longitude, final List<String> tags, final List<String> positions/*, final List<String> orientations*/) {
        // send to server
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                homeFragment.setRefreshing(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    queryWrapper.addImage(file, time, latitude, longitude, tags, positions/*, orientations*/);
                } catch (HttpHostConnectException e) {
                    LogWrapper.e("Loc", e.getMessage());
                    return false;
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);

                if(success) {
                    homeFragment.setLocation(latitude, longitude);
                } else {
                    //TODO : 나중에는 home에다가 Image obj를 이용한 retry form 만들어주도록 한다.
                    homeFragment.notifyNetworkFailure();
                }
            }
        };

        // 11부터는 serial이 default라서.
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.HONEYCOMB) task.execute();
        else task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        //TODO: 여기가 중요. DISPLAY를 끄기 전에 확실히 UPLOAD를 한다.
        //onBackPressed();// 다만, 이게 display의 finish가 맞아야 한다.
    }

    public void showToast(int resId) {
        Toast toast = Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    //---- home
    public boolean isHomeViewing() {
        return flag_fragment_home;
    }

    @Override
    public void onHomeSearchTag(String tag) {
        LatLng location = homeFragment.getLocation();// 직접 받을수도 있지만 일단 통일성/확장성 위해.

        flag_fragment_profile = true;

        profileFragment = ProfileFragment.newInstance(tag, location.latitude, location.longitude, homeFragment.isLocationAvailable());

        addFragment(profileFragment);
    }

    @Override
    public void onHomeItemClicked(List<Image> images, int position) {
        flag_fragment_item = true;

        itemFragment = ItemFragment.newInstance(images, position);

        addFragment(itemFragment);
    }

    @Override
    public void onHomeLocationClicked(double latitude, double longitude, boolean isLocationAvailable) {
        flag_fragment_map = true;

        mapFragment = MapFragment_.newInstance(latitude, longitude, isLocationAvailable, false);

        addFragment(mapFragment);
    }

    //---- profile
    @Override
    public void onProfileActionAddClicked(String tag) {
        //filterFragment.addTag(tag);
        addTag(tag);
    }

    @Override
    public void onProfileLocationClicked(double latitude, double longitude, boolean isLocationAvailable) {
        flag_fragment_map = true;

        mapFragment = MapFragment_.newInstance(latitude, longitude, isLocationAvailable, false);

        addFragment(mapFragment);
    }

    @Override
    public void onProfileItemClicked(List<Image> images, int position) {
        flag_fragment_item = true;

        itemFragment = ItemFragment.newInstance(images, position);

        addFragment(itemFragment);
    }

    //---- map
    @Override
    public void onMapOKClicked(double latitude, double longitude) {
        //TODO: keyboard back은 좀 막을 수 있다면 막아보도록 한다. 하지만 워낙 빠르기도 하고, 다른 부분들도 그대로이므로 일단 놔두고 나중에 한꺼번에 한다.
        onBackPressed();// 먼저 해야, loc stop without update가 되어서 loc change 안나오고 그래야 home loc reset안된다.

        // display에서 온건지 home/profile에서 온건지 구분해야 한다.
        if(displayFragment != null) {
            // 1. show toast.
            //showToast(R.string.upload_wait);//TODO: 다른 서비스들 봐도 그냥 안내 없이 올린다. 그렇게 하기.
            // 2. set display location - //TODO: upload와 같이 하고 싶지만 upload도 일단 독립적으로 둬보는게 좋을듯하다.
            displayFragment.setLocation(latitude, longitude);
            // 3. upload via display frag
            displayFragment.upload();
            // 4. move to home(refresh는 async post에서 될 것)
            viewPagerFragment.setCurrentPage(1);
            // 5. close display frag. => display의 upload가 main의 callback으로 오고, 거기서 back이 된다. => 그럴 필요 없다. home으로 돌려놓고 바로 close하면 된다.
            onBackPressed();

            if(galleryFragment != null) {
                onBackPressed();// close gallery
                onBackPressed();// close picture - item, profile이 gallery 다음 check 대상이지만, 아직 item, profile이 켜진 상태에서 picture이 열릴 경우는 없으므로 괜찮다.
            }
        } else {
            if(profileFragment != null) {
                profileFragment.setRefreshing(true);// 이게 필요없는 것들도 있지만(직접 refresh한다거나, display에서 넘어간다거나 등) 여긴 필요하다.
                profileFragment.setLocation(latitude, longitude);
            } else {// profile에서 뜬 map의 설정시에는 home은 안 건드린다는 말과 같다.(당장은 profile에서 map 뜨지도 않으므로 상관없다.)
                if(homeFragment != null) {
                    homeFragment.setRefreshing(true);// 이게 필요없는 것들도 있지만(직접 refresh한다거나, display에서 넘어간다거나 등) 여긴 필요하다.
                    homeFragment.setLocation(latitude, longitude);
                }
            }
        }

        //onBackPressed();// 뭐가 됐든 back은 필요하다.(display 등은 2번이므로 묶어야 keyboard back 등에 의한 꼬임 발생 안할 것 같긴 하지만...)
    }

    @Override
    public void onMapClosed() {
        stopLocationUpdates(true, false);
    }

    //---- filter
    @Override
    public void onFilterTagClicked(String tag) {
        LatLng location = homeFragment.getLocation();

        flag_fragment_profile = true;

        profileFragment = ProfileFragment.newInstance(tag, location.latitude, location.longitude, homeFragment.isLocationAvailable());

        addFragment(profileFragment);
    }

    public boolean isTagsChanged(/*boolean defaultTag, */List<String> tags, List<String> switches) {
        boolean result = false;

        //if(this.defaultTag != defaultTag) {
        //    result = true;
        //} else {
            if(tags == null || (tags != null && tags.isEmpty())) { // filter 비었을 때
                if(this.tags == null || (this.tags != null && this.tags.isEmpty())) { // pref도 비었으면 not changed
                    result = false;
                } else { // pref는 안비었으면 filter를 지운거고 changed.
                    result = true;
                }
            } else { // filter 안비었을 때
                if(this.tags == null || (this.tags != null && this.tags.isEmpty())) { // tag 비었으면 filter 추가된거고 changed.
                    result = true;
                } else { // tag도 안비었으면
                    if(tags.size() != this.tags.size()) { // size 다르면 자세히 비교할 필요도 없이 changed.
                        result = true;
                    } else { // size 같으면 일단 비교.(filter에서 추가 삭제 반복하면 내용만 다를수도 있다.)
                        for(int i = 0; i < tags.size(); i++) {
                            if(tags.get(i).equals(this.tags.get(i)) == false) { // tag 하나라도 다르면 바로 changed.
                                result = true;

                                break;
                            } else { // tag 같아도 switch 다르면 changed.
                                if(switches.get(i).equals(this.switches.get(i)) == false) {
                                    result = true;

                                    break;
                                }
                            }
                        }
                    }
                }
            }
        //}

        return result;
    }

    @Override
    public void onCloseFilter(/*boolean defaultTag, */List<String> tags, List<String> switches) {
        // 나중에는 변경된 사항들만 변화시킬 수 있는 logic을 생각해보도록 한다.
        if(isTagsChanged(/*defaultTag, */tags, switches) == true) { // updates preferences, homefragment when only changes exist.
            // set to pref.
            setPreferences(/*defaultTag, */tags, switches);
            // update home
            /*
            위치는 어차피 home 자체적으로 표시되고 해결되게 되어있다.
            따라서 여기서는 위치가 없으면 pass, 있으면 진행하면 된다.
            (사실 onRefres에서 위치가 있는 부분만 떼온 것이나 다름없다고 보면 된다.)
             */
            //if(mLocationUpdated) {//TODO: 이것도 HOME의 LOC AVAILABLE CHECK로 바꿔야 한다.
            if(homeFragment.isLocationAvailable()) {
                homeFragment.setRefreshing(true);
                homeFragment.setView();
            }
        }
    }

    //---- item
    @Override
    public void onItemActionShareClicked() {

    }

    @Override
    public void onItemActionTagClicked(String tag) {
        LatLng location = homeFragment.getLocation();

        flag_fragment_profile = true;

        profileFragment = ProfileFragment.newInstance(tag, location.latitude, location.longitude, homeFragment.isLocationAvailable());

        addFragment(profileFragment);
    }

    @Override
    public void onItemMoreClicked(final int id) {

        Fragment fragment = PasscodeFragment.newInstance(false, "1234");
        addFragment(fragment);
        /*
        new MaterialDialog.Builder(this)
                .items(R.array.items)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        switch (i) {// 일단 1개뿐이다. 더 생겨도, 굳이 array 받아와서 str 비교까지 해야 될 필요가 있을지는 모르겠다.
                            case 0:
                                onBackPressed();//TODO: 일단 ITEM을 잘 CLOSE하긴 한다. 아마 DIALOG는 ITEM FRAG 안에 있어서 상관없는듯 하다.

                                AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
                                    @Override
                                    protected void onPreExecute() {
                                        super.onPreExecute();

                                        if(profileFragment != null) {
                                            profileFragment.setRefreshing(true);
                                        } else {
                                            if(homeFragment != null) {
                                                homeFragment.setRefreshing(true);
                                            }
                                        }
                                    }

                                    @Override
                                    protected Boolean doInBackground(Void... params) {
                                        try {
                                            queryWrapper.deleteImage(id);
                                        } catch (HttpHostConnectException e) {
                                            LogWrapper.e("Delete", e.getMessage());
                                            return false;
                                        }

                                        return true;
                                    }

                                    @Override
                                    protected void onPostExecute(Boolean success) {
                                        super.onPostExecute(success);

                                        if(success) {
                                            if(profileFragment != null) {
                                                profileFragment.setView();
                                            } else {
                                                if(homeFragment != null) {
                                                    homeFragment.setView();
                                                }
                                            }
                                        } else {// 여긴 사실 그냥 toast 해도 된다. 하지만 refresh false 등도 있고, 앞으로 확장성 생각해서 이렇게 해둔다.
                                            if(profileFragment != null) {
                                                profileFragment.notifyNetworkFailure();
                                            } else {
                                                if(homeFragment != null) {
                                                    homeFragment.notifyNetworkFailure();
                                                }
                                            }
                                        }
                                    }
                                };

                                // 11부터는 serial이 default라서.
                                if(Build.VERSION.SDK_INT< Build.VERSION_CODES.HONEYCOMB) task.execute();
                                else task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }
                })
                .show();*/
    }

    @Override
    public void onItemActionDistanceClicked(double latitude, double longitude) {
        flag_fragment_map = true;

        mapFragment = MapFragment_.newInstance(latitude, longitude, true, true);

        addFragment(mapFragment);
    }

    /*
    @Override
    public void onItemAddTag(List<String> tags, List<String> switches) {
        addPreferences(tags, switches);
    }
    */

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();
        if(fragmentManager.getBackStackEntryCount() > 0) {
            if(flag_fragment_splash == true) {
                finish();
            } else {
                if(flag_fragment_map == true) {
                    mapFragment.onClosed();

                    flag_fragment_map = false;

                    mapFragment = null;
                } else if(flag_fragment_display == true) {
                    flag_fragment_display = false;// 미리 false로 하고 pop한다.

                    displayFragment = null;// 쓸 일 없으면 null 처리를 해주는게 나을듯 하다.

                    //fragmentManager.popBackStack();

                    if (ROTATE_TAG) {
                        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        }
                    }

                    //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                } else if(flag_fragment_gallery) {
                    flag_fragment_gallery = false;

                    galleryFragment = null;
                } else if(flag_fragment_item) {
                    flag_fragment_item = false;

                    itemFragment = null;
                } else if(flag_fragment_profile) {
                    flag_fragment_profile = false;

                    profileFragment = null;
                } else if(flag_fragment_filter) {// filter -> profile일 때 back하면 profile부터 꺼지도록 profile 뒤에.
                    flag_fragment_filter = false;

                    filterFragment = null;
                }

                //fragmentManager.popBackStack();// 이건 다 해준다.(display에서 저렇게 중간에서 해줬었는데 괜찮은지 확인해보기.)
                fragmentManager.popBackStackImmediate();// display-map 간혹 안닫히는거 이렇게 하면 될지 ㅡ 일단 해놓고 보기.
            }
        } else {
            /*if(flag_fragment_gallery) {// gallery to camera.
                viewPagerFragment.setCurrentPage(1);
            } else */if(flag_fragment_home == true) { // isVisible 아직은 안정확함.
                viewPagerFragment.setCurrentPage(0);
            }/* else if(flag_fragment_filter == true) {
                viewPagerFragment.setCurrentPage(1);
            }*/ else { // camera라 하더라도
                if(flag_taking_camera == false) { // 사진 찍는 중은 skip한다.
                    finish();
                }
            }
        }
    }

    public void mOnClick(View view) {
        switch(view.getId()) {
            /*
            case R.id.fragment_camera_flash:
                break;
                */
            /*
            case R.id.fragment_camera_flip:
                break;
            case R.id.fragment_camera_video:
                break;
                */
            case R.id.fragment_camera_gallery:
                flag_fragment_gallery = true;

                galleryFragment = GalleryFragment.newInstance(/*latitude, longitude, mLocationUpdated*/);

                addFragment(galleryFragment);

                break;
            case R.id.fragment_camera_home:
                viewPagerFragment.setCurrentPage(1);

                break;
            //case R.id.fragment_display_close:
            case R.id.fragment_home_camera:
            case R.id.fragment_gallery_close:
            case R.id.fragment_profile_back:
            case R.id.fragment_item_close:
            case R.id.fragment_picture_close:
                onBackPressed();

                break;
            case R.id.fragment_home_filter:
                //viewPagerFragment.setCurrentPage(2);
                //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                flag_fragment_filter = true;

                filterFragment = FilterFragment.newInstance(/*defaultTag, */tags, switches);

                addFragment(filterFragment, R.anim.slide_up, R.anim.slide_down);

                break;
            case R.id.fragment_filter_ok:// close에 비해서 저장이 추가된 방식.
                filterFragment.closeFilter();
            case R.id.fragment_filter_close:
                //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

                onBackPressed();

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:
                        connect();

                        break;

                    // If any other result was returned by Google Play services
                    default:

                        break;
                }

                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode

                break;
        }
    }
}