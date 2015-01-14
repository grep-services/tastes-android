package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.CameraHostProvider;
import com.commonsware.cwac.camera.CameraUtils;
import com.commonsware.cwac.camera.DeviceProfile;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;
import com.instamenu.R;
import com.instamenu.content.Image;
import com.instamenu.util.QueryWrapper;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends ActionBarActivity implements SplashFragment.SplashFragmentCallbakcs, ViewPagerFragment.ViewPagerFragmentCallbacks, /*CameraHostProvider, */CameraFragment_.CameraFragmentCallbacks, DisplayFragment.DisplayFragmentCallbacks, HomeFragment.HomeFragmentCallbacks, FilterFragment.FilterFragmentCallbacks, ItemFragment.ItemFragmentCallbacks {

    private QueryWrapper queryWrapper;

    private SharedPreferences preferences;
    private List<String> tags;
    private List<String> switches;
    private String strTags;
    private String strSwitches;
    private boolean mLocationUpdates;

    private boolean flag_fragment_splash = true;// 위치 잡히면 frag remove하면서 false되고, 나머지에 대해서는 true가 되어서 back key 때 무조건 finish되게 한다.
    private boolean flag_fragment_home = false;// except for home, default back key processing.
    private boolean flag_fragment_display = false;// 보낼 때 true가 된다.

    private SlidingMenu slidingMenu;

    private FilterFragment filterFragment;
    private ViewPagerFragment viewPagerFragment;
    private CameraFragment_ cameraFragment;
    private HomeFragment homeFragment;

    private boolean use_ffc = false;
    // 일단, continuous가 대세다. 이걸 더 발전시킨다. take전에 autofocus는 답답함 늘릴 수 있다. 차라리 tap to autofocus나 추가한다.
    private boolean use_autofocus = false;
    private String mode_focus = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;// 이건 거의 바뀔 일 없을듯.
    private boolean use_singleshot = true;
    private boolean use_fullbleed = true;
    private String mode_flash = null;

    // location
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        setSlidingMenu();

        filterFragment = FilterFragment.newInstance(tags, switches);
        replaceFragment(R.id.menu, filterFragment);

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

        viewPagerFragment = ViewPagerFragment.newInstance(latitude, longitude);
        replaceFragment(R.id.container, viewPagerFragment);

        Fragment fragment = SplashFragment.newInstance(true, mLocationUpdates);
        addFragment(fragment);
    }

    public void setSlidingMenu() {
        slidingMenu = new SlidingMenu(this);
        slidingMenu.setMode(SlidingMenu.RIGHT);
        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        slidingMenu.setBehindWidthRes(R.dimen.navigation_drawer_width);
        slidingMenu.setFadeDegree(0.35f);
        slidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
        slidingMenu.setMenu(R.layout.frame_menu);
        slidingMenu.setOnOpenListener(new SlidingMenu.OnOpenListener() {
            @Override
            public void onOpen() {
                // change input mode.(display mode will be changed at there.)
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
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
        preferences = getSharedPreferences("instamenu", Context.MODE_PRIVATE);

        mLocationUpdates = preferences.getBoolean("LocationUpdates", false);

        strTags = preferences.getString("Tags", null);
        tags = getList(strTags);

        strSwitches = preferences.getString("Switches", null);
        switches = getList(strSwitches);

        //LogWrapper.e("INIT PREF", strTags+","+strSwitches);
    }

    public void setPreferences(List<String> tags, List<String> switches) {
        SharedPreferences.Editor editor = preferences.edit();

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

            if(strSwitches.equals(this.strSwitches) == false) {
                SharedPreferences.Editor editor = preferences.edit();

                this.strSwitches = strSwitches;

                editor.putString("Switches", this.strSwitches);

                if(strTags.equals(this.strTags) == false) {
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
    public void onSplashLocationUpdated(double latitude, double longitude) {
        // 위경도 설정부터 하고
        this.latitude = latitude;
        this.longitude = longitude;

        // 닫기 전에 flag부터 false로 해준다.
        flag_fragment_splash = false;

        // camera도 tools 복귀시켜준다.
        cameraFragment.setToolsVisible(true);

        // home도 location set 해준다.
        homeFragment.setLocation(latitude, longitude);
        homeFragment.setView();// swipe할 때 해주자니 사실 1번 이후로는 필요없기 때문에 여기서 하는게 맞다고 생각했다.

        // fragment닫아야 한다.
        popFragment();
    }

    @Override
    public void onSplashLocationAgreed() {
        // 일단 debug동안은 disable.
        //setPreferences(true);
    }

    //---- vp
    @Override
    public void onViewPagerPageSelected(int position) {
        switch(position) {
            case 0:// Camera
                flag_fragment_home = false;

                slidingMenu.setSlidingEnabled(false);

                break;
            case 1:// Home
                flag_fragment_home = true;

                slidingMenu.setSlidingEnabled(true);

                break;
        }
    }

    @Override
    public void onSetFragments() {
        // vp에서 저 fragment들을 설정하는 시각을 여기서 정확히 예측하느니, callback으로 이렇게 한다.
        cameraFragment = viewPagerFragment.getCameraFragment();
        homeFragment = viewPagerFragment.getHomeFragment();
    }

    //---- camera

    @Override
    public void onTakePicture(String mode_flash) {
        // =====> button disabled하는 code 필요.
        cameraFragment.setShotButtonEnabled(false);

        PictureTransaction pictureTransaction = new PictureTransaction(cameraFragment.getHost());

        // 음... 근데 이건 flash on 해도 자동으로 안켜지는 방식인가... 나중에 찾아보기.(근데 갤5로 보면 default camera나 snapchat도 그렇다.)
        pictureTransaction.flashMode(mode_flash);

        cameraFragment.takePicture(pictureTransaction);
    }

    @Override
    public void onSaveImage(byte[] image) {
        // 필요한지 test 한번 해봤는데, 필요하다. ㅡㅡ... 꼭 해봐야 아는가...
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // ====> shot button 때 필요할수도....
                cameraFragment.setShotButtonEnabled(true);
            }
        });

        cameraFragment.restartPreview();

        //DisplayFragment.imageToShow = image;
        addFragment(DisplayFragment.newInstance(image, null, latitude, longitude));
    }

    //---- display
    @Override
    public void onDisplayActionOKClicked(final byte[] file, final long time, final String address, final double latitude, final double longitude, final List<String> tags, final List<String> positions, final List<String> switches) {
        // set flag
        flag_fragment_display = true;
        // send to server
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                queryWrapper.addImage(file, time, address, latitude, longitude, tags, positions);

                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);

                // add to pref.
                addPreferences(tags, switches);
                // set filter also.
                filterFragment.setTags(MainActivity.this.tags, MainActivity.this.switches); // filter가 이제 새로 뜨는게 아니라서 변경 생길 때마다 해줘야된다.(ok는 변경을 무조건 수반한다고 볼 수 있다.)
                // and then, just go to home frag. => set nav frag to 1(home) and, just finish(back).
                homeFragment.setView();

                if(flag_fragment_display == true) { // false란 건 중간에 미리 껐다는 것이므로 그냥 둔다.
                    viewPagerFragment.setCurrentPage(1);

                    onBackPressed();
                }
            }
        };

        // 11부터는 serial이 default라서.
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.HONEYCOMB) task.execute();
        else task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //---- home
    @Override
    public void onHomeActionFilterClicked() {
        slidingMenu.toggle();
    }

    @Override
    public void onHomeItemClicked(List<Image> images, int position) {
        Fragment fragment = ItemFragment.newInstance(images, position);
        addFragment(fragment);
    }

    //---- filter
    public boolean isTagsChanged(List<String> tags, List<String> switches) {
        boolean result = false;

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

        return result;
    }

    @Override
    public void onCloseFilter(List<String> tags, List<String> switches) {
        // 나중에는 변경된 사항들만 변화시킬 수 있는 logic을 생각해보도록 한다.
        if(isTagsChanged(tags, switches) == true) { // updates preferences, homefragment when only changes exist.
            // set to pref.
            setPreferences(tags, switches);
            // update home
            homeFragment.setView();
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
                }

                fragmentManager.popBackStack();
            }
        } else {
            if(flag_fragment_home == true) { // isVisible 아직은 안정확함.
                if(slidingMenu.isMenuShowing()) {
                    slidingMenu.toggle();
                } else {
                    viewPagerFragment.setCurrentPage(0);
                }
            } else {
                finish();
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
            case R.id.fragment_display_close:
            case R.id.fragment_home_camera:
            case R.id.fragment_item_close:
            //case R.id.fragment_filter_cancel:
                onBackPressed();

                break;
        }
    }
}
