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
import android.content.res.Configuration;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.mms.exif.ExifInterface;
import com.commonsware.cwac.camera.PictureTransaction;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.tastes.R;
import com.tastes.content.Image;
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

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SplashFragment.SplashFragmentCallbakcs, ViewPagerFragment.ViewPagerFragmentCallbacks, /*CameraHostProvider, */CameraFragment_.CameraFragmentCallbacks, DisplayFragment.DisplayFragmentCallbacks, HomeFragment.HomeFragmentCallbacks, FilterFragment.FilterFragmentCallbacks, ItemFragment.ItemFragmentCallbacks {

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
    private boolean flag_fragment_display = false;// 보낼 때 true가 된다.
    private boolean flag_taking_camera = false;// fragment 관련이 아니라, cam frag에서 taking 중일 때만 true되어서 back을 막아주는 역할을 하는 flag이다.
    private boolean flag_fragment_filter = false;

    //private SlidingMenu slidingMenu;

    private SplashFragment splashFragment;
    private CameraFragment_ cameraFragment;
    private DisplayFragment displayFragment;
    private HomeFragment homeFragment;
    private FilterFragment filterFragment;
    private ViewPagerFragment viewPagerFragment;

    // location
    private GoogleApiClient mGoogleApiClient;
    private boolean mLocationUpdates = false;// 사실상 동의서다.(pref에 저장된 동의 여부이므로)
    private LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates = false;
    private boolean mLocationUpdated = false;
    private boolean mRequestingLocationFailed = false;
    private double latitude;
    private double longitude;

    private Handler mHandler;
    private Runnable mRunnable;

    private static final int LOCATION_TIMEOUT = 5000;

    private static final boolean ROTATE_TAG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init loc
        buildGoogleApiClient();

        // init vars.
        queryWrapper = new QueryWrapper();

        // init preferences
        initPreferences();

        // init imageloader before other fragments' init.
        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(this)
                //.threadPoolSize(3) Default
                //.threadPriority(Thread.NORM_PRIORITY - 2) Default
                .denyCacheImageMultipleSizesInMemory() // image size 변할 상황 없으므로 deny.
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(configuration);

        cameraFragment = CameraFragment_.newInstance();

        homeFragment = HomeFragment.newInstance(latitude, longitude);

        filterFragment = FilterFragment.newInstance(/*defaultTag, */tags, switches);

        viewPagerFragment = ViewPagerFragment.newInstance();
        replaceFragment(R.id.container, viewPagerFragment);

        //splashFragment = SplashFragment.newInstance(mLocationUpdates);
        splashFragment = SplashFragment.newInstance(false);
        addFragment(splashFragment);

        /*
        filterFragment = FilterFragment.newInstance(defaultTag, tags, switches);
        replaceFragment(R.id.menu, filterFragment);

        setSlidingMenu();

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if(getFragmentManager().getBackStackEntryCount() == 0 && flag_fragment_home == true) {
                    slidingMenu.setSlidingEnabled(true);
                } else {
                    slidingMenu.setSlidingEnabled(false);
                }
            }
        });
        */
    }

    public CameraFragment_ getCameraFragment() {
        return cameraFragment;
    }

    public HomeFragment getHomeFragment() {
        return homeFragment;
    }

    public FilterFragment getFilterFragment() {
        return filterFragment;
    }

    //---- location
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
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

    /*
    available check나 connection failed나 자기 처리 module이 있으므로, 여기서는 그냥
     */
    protected void startLocationUpdates() {
        if(servicesAvailable() == true) {
            if(mRequestingLocationUpdates == false) {
                mRequestingLocationUpdates = true;
                mRequestingLocationFailed = false;

                //updateUI();

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

                mHandler.postDelayed(mRunnable, LOCATION_TIMEOUT);
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

                onLocationUpdated();
            }
        }
    }

    public void onLocationUpdated() {
        // 당장 필요한건 update success시 home setview 하는 것 뿐일듯 하다.... display도 있을듯.
        if(mLocationUpdated) {
            if(displayFragment != null) {
                displayFragment.setLocation(latitude, longitude);
            }
            if(homeFragment != null) {// null인 상황은 애초에 onLocationChanged에서 저장되는 latlng가 자동으로 homeFrag 생성시 전달되어 해결될 것이다.
                homeFragment.setLocation(latitude, longitude);
            }
        } else {
            if(mRequestingLocationFailed) {
                if(displayFragment != null) {// display가 살아있고
                    if(displayFragment.isWaiting()) {// wait 중이라면
                        displayFragment.notifyLocationFailure();// failure를 알린다.
                    }
                }
                if(homeFragment != null) {
                    homeFragment.notifyLocationFailure();
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

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mLocationUpdates == true && mRequestingLocationFailed == false) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mLocationUpdates == true && mRequestingLocationFailed == false) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
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

        stopLocationUpdates();
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

    /*
    public void setSlidingMenu() {
        slidingMenu = new SlidingMenu(this);
        slidingMenu.setMode(SlidingMenu.RIGHT);
        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        //slidingMenu.setTouchModeBehind(SlidingMenu.TOUCHMODE_FULLSCREEN);
        slidingMenu.setBehindWidthRes(R.dimen.navigation_drawer_width);
        slidingMenu.setFadeDegree(0.35f);
        slidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
        slidingMenu.setMenu(R.layout.frame_menu);
        slidingMenu.setOnOpenListener(new SlidingMenu.OnOpenListener() {
            @Override
            public void onOpen() {
                // change input mode.(display mode will be changed at there.) => resize하면 느려진다. 그냥 default pan으로 간다.
                // toolbar icon animation.
            }
        });
        slidingMenu.setOnCloseListener(new SlidingMenu.OnCloseListener() {
            @Override
            public void onClose() {
                // method가 filter의 것을 override한 형식이므로 filter의 것을 invoke해주는 방식으로 간다.(중간 단계)
                // null check 해야될지도.
                filterFragment.closeFilter();
                // toolbar icon animation.
            }
        });
    }

    public SlidingMenu getSlidingMenu() {
        return slidingMenu;
    }
    */

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
            case 0:// Camera
                // camera flag는 없으므로 pass.
                flag_fragment_home = false;

                //slidingMenu.setSlidingEnabled(false);

                break;
            case 1:// Home
                flag_fragment_home = true;

                if(flag_fragment_filter == true) {
                    flag_fragment_filter = false;

                    filterFragment.closeFilter();
                }

                //slidingMenu.setSlidingEnabled(true);

                break;
            case 2:
                flag_fragment_home = false;
                flag_fragment_filter = true;

                break;
        }
    }

    public void setViewPagerEnabled(boolean enabled) {
        viewPagerFragment.setEnabled(enabled);
    }

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
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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
        displayFragment = DisplayFragment.newInstance(mirror, image, ROTATE_TAG ? rotation : 0, latitude, longitude);// 여기서도 보내야 location process 잘 맞아떨어진다.
        addFragment(displayFragment);

        flag_taking_camera = false;// 최대한 늦게 하는게, 답답할 수도 있겠지만 잘못된 방향으로 흘러가는걸 막아줄 수 있다.
    }

    //---- display
    @Override
    public void onDisplayActionOKClicked(final byte[] file, final long time, final double latitude, final double longitude, final List<String> tags, final List<String> positions, final List<String> orientations, final List<String> switches) {
        // send to server
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    queryWrapper.addImage(file, time, latitude, longitude, tags, positions, orientations);
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

                if(success) {
                    /*
                    // tag 자체 filtering - tastes tag는 서버에는 switch와 함께 올라가더라도, 여기서는 빼 줘야 중복 안된다. - 나중에는 list 자체에 포함시켜서 이렇게 안꼬이게 한다.
                    if(tags != null && tags.contains("tastes")) {
                        int index = tags.indexOf("tastes");

                        defaultTag = switches.get(index).equals("true");// 사실 항상 true일 것이다 ㅡ display에서는 모두 true이므로.
                        filterFragment.setDefaultTag(defaultTag);

                        tags.remove(index);
                        switches.remove(index);
                    }
                    // add to pref.
                    addPreferences(tags, switches);
                    // set filter also.
                    filterFragment.setTags(MainActivity.this.tags, MainActivity.this.switches); // filter가 이제 새로 뜨는게 아니라서 변경 생길 때마다 해줘야된다.(ok는 변경을 무조건 수반한다고 볼 수 있다.)
                    */
                    // and then, just go to home frag. => set nav frag to 1(home) and, just finish(back).
                    homeFragment.setView();

                    if(flag_fragment_display == true) { // false란 건 중간에 미리 껐다는 것이므로 그냥 둔다.
                        viewPagerFragment.setCurrentPage(1);

                        onBackPressed();
                    }
                } else {
                    if(flag_fragment_display == true) { // false란 건 중간에 미리 껐다는 것이므로 그냥 둔다.
                        //displayFragment.showWait(false);
                        displayFragment.notifyNetworkFailure();
                    }

                    // display가 닫혔든 말든 msg(toast)는 표시해주는 것이 좋을 것 같다.
                    //Toast.makeText(MainActivity.this, getString(R.string.upload_network), Toast.LENGTH_SHORT).show();
                }
            }
        };

        // 11부터는 serial이 default라서.
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.HONEYCOMB) task.execute();
        else task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //---- home
    /*
    @Override
    public void onHomeActionFilterClicked() {
        slidingMenu.toggle();
    }
    */

    @Override
    public void onHomeItemClicked(List<Image> images, int position) {
        Fragment fragment = ItemFragment.newInstance(images, position);
        addFragment(fragment);
    }

    //---- filter
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
            if(mLocationUpdated) {
                homeFragment.setRefreshing(true);
                homeFragment.setView();
            }
        }
    }

    //---- item
    @Override
    public void onItemActionShareClicked() {

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
                if(flag_fragment_display == true) {
                    flag_fragment_display = false;// 미리 false로 하고 pop한다.

                    displayFragment = null;// 쓸 일 없으면 null 처리를 해주는게 나을듯 하다.

                    fragmentManager.popBackStack();

                    if(ROTATE_TAG) {
                        if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        }
                    }

                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                } else {
                    fragmentManager.popBackStack();
                }
            }
        } else {
            if(flag_fragment_home == true) { // isVisible 아직은 안정확함.
                /*
                if(slidingMenu.isMenuShowing()) {
                    slidingMenu.toggle();
                } else {
                    viewPagerFragment.setCurrentPage(0);
                }
                */
                viewPagerFragment.setCurrentPage(0);
            } else if(flag_fragment_filter == true) {
                viewPagerFragment.setCurrentPage(1);
            } else { // camera라 하더라도
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
            case R.id.fragment_camera_home:
                viewPagerFragment.setCurrentPage(1);

                break;
            //case R.id.fragment_display_close:
            case R.id.fragment_home_camera:
            case R.id.fragment_item_close:
            //case R.id.fragment_filter_cancel:
                onBackPressed();

                break;
            case R.id.fragment_home_filter:
                viewPagerFragment.setCurrentPage(2);

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
