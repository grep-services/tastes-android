package com.tastes.app;

import android.app.Activity;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tastes.R;
import com.tastes.util.LogWrapper;

import java.util.ArrayList;

/**
 * address를 사용하기는 하지만 외부로부터 받지도 않고 전달하지도 않는다.
 * 여기서 사용하는 address는 여기서 사용하고, 외부(home 등)에서 사용하는건 거기서 알아서 사용한다.
 * 그렇게 하면 간혹 연결 도중 network 문제 등으로 address 못 받을 수 있겠지만
 * 전달을 위해 넘기다 보면 구조가 복잡해질 수 있다.
 *
 * -> 그런데 그 구조 복잡해지는 것 보다 address 받는 속도 문제가 더 클 수 있다고 본다.(당장 map에서 back후 home을 봐도 그럴 것이다.)
 * 따라서 address도 local에 있더라도 main에서 global하게 관리되는 느낌으로 간다.(각 frag에서도 최대한 주고받을 수 있으면 주고받는다.)
 *
 * 또한, 애초에 main->home->map 등으로 coords가 전달되는 등 그 updated flag의 sync가 잘 안맞을 수 있다는 생각에 arg로 둘까 했으나
 * 일단 그냥 가본다.
 *
 * 그리고, MapFragment를 상속받지 않으면 결국 nested구조로 가거나 view recycle문제 생기는 방식으로 가야 되어서
 * 상속받는 구조로 가고 onCreateView에서 좀 제어해주도록 했다.
 */
public class MapFragment_ extends MapFragment implements OnMapReadyCallback, View.OnClickListener {
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_AVAILABLE = "available";
    private static final String ARG_READONLY = "readonly";

    private double pointerLatitude;
    private double pointerLongitude;
    private double currentLatitude;
    private double currentLongitude;

    private boolean isLocationAvailable;// main updated 안됐어도 map manually set 된 것 넘어올 수 있다.
    private boolean readOnly;

    //private String address;// 이건 받는 arg는 아닐 것이므로 이렇게 따로 둔다.

    private MapView view;
    private GoogleMap map;
    private Marker currentMarker, pointerMarker;

    private Button currentButton;
    private Button okButton, closeButton;

    private boolean setManually;

    private boolean isFirstSet = true;

    private MainActivity mActivity;
    private MapFragmentCallbacks mCallbacks;

    public static MapFragment_ newInstance(double latitude, double longitude, boolean isLocationAvailable, boolean readOnly) {
        MapFragment_ fragment = new MapFragment_();
        Bundle args = new Bundle();
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        args.putBoolean(ARG_AVAILABLE, isLocationAvailable);
        args.putBoolean(ARG_READONLY, readOnly);
        fragment.setArguments(args);
        return fragment;
    }
    public MapFragment_() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pointerLatitude = getArguments().getDouble(ARG_LATITUDE);
            pointerLongitude = getArguments().getDouble(ARG_LONGITUDE);
            isLocationAvailable = getArguments().getBoolean(ARG_AVAILABLE);
            readOnly = getArguments().getBoolean(ARG_READONLY);
        }

        //TODO: 지금 받고 있는 중 아니라면, 새로 받는다.(웬만하면 받는중 아닐것이다.) => 어쨌든 CUR는 무조건 SET되므로(또는 FAILURE MSG) READY에서 INIT 필요없다.
        //TODO: 더 빨리 해도 되지만, 어차피 LOC SET 될 때 다시 map을 참조하므로 created 된 뒤여야 될 것 같다.(정확히는 ready겠는데, 그건 각 method에서 null check할 듯.)
        if(!mActivity.isRequestingLocationUpdates()) {
            mActivity.startLocationUpdates();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_map, container, false);

        View mapView = super.onCreateView(inflater, container, savedInstanceState);

        root.addView(mapView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        /*
        view = (MapView) root.findViewById(R.id.fragment_map_view);
        view.onCreate(savedInstanceState);
        view.getMapAsync(this);
        */
        /*
        GoogleMapOptions options = new GoogleMapOptions().liteMode(true);
        view = new MapView(getActivity(), options);
        root.addView(view, 0);
        view.getMapAsync(this);

        MapsInitializer.initialize(getActivity());//TODO: example에서는 google play not available exception 쓰던데 실제로 해보면 not thrown이라고 안된다.
        */

        // and next place it, for exemple, on bottom right (as Google Maps app)
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);// disable 해야 bottom이 된다.
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        layoutParams.setMargins(getPixel(12), 0, 0, getPixel(12));

        currentButton = (Button) root.findViewById(R.id.fragment_map_current);
        //currentButton.setBackgroundDrawable(locationButton.getBackground());
        currentButton.setOnClickListener(this);
        currentButton.setEnabled(mActivity.isLocationUpdated());//TODO: start가 updated false로 만든다. 그리고 changed가 true로 만든다.

        okButton = (Button) root.findViewById(R.id.fragment_map_ok);
        if(readOnly) {
            okButton.setVisibility(View.GONE);
        } else {
            okButton.setOnClickListener(this);
        }
        closeButton = (Button) root.findViewById(R.id.fragment_map_close);
        closeButton.setOnClickListener(this);

        if(mActivity.isDisplayViewing()) {
            closeButton.setBackgroundResource(R.drawable.back);
        }

        getMapAsync(this);

        return root;
    }

    public int getPixel(int dp) {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        float density = dm.density;

        return (int)(dp * density);
    }

    public void setCurrentLocation(double latitude, double longitude) {
        if(getView() != null && map != null) {// view 뿐만 아니라 map 도 check 필요하다. cur, pnt 등에서 해주자니, bool flag 등 걸리는 것들이 많다.
            // 현위치 효과를 준다.
            setCurrentMarker(latitude, longitude);

            if(setManually) {// start(처음, 그이후 여러 background), button
                // 처음, 그중에서 pointer가 있을 경우 - 를 빼고는 다 해준다.
                if(!(isFirstSet && isLocationAvailable)) {//TODO: pointer말고 이걸 한 이유는, 혹시라도 이거 바로 뒤에 ready 될까봐. 어차피 avilable이면 pointer 있으므로.
                    if(!readOnly) {// 그래도 item에서는 pointer 바뀌면 안된다.
                        setPointerMarker(latitude, longitude);
                    }

                    setCameraView(latitude, longitude);
                }

                if(isFirstSet) {
                    isFirstSet = false;
                }

                setManually = false;
            }

            currentButton.setEnabled(true);// clicked뿐만 아니라 onCreate에서도 온다. 그리고 현재 method는 기본적으로 updated true 동반이다.
        }
    }

    public void notifyLocationFailure() {
        //TODO: clicked에 의해서만 toast를 띄울 수도 있다고 생각함. => 일단 현재 이 NOTI 자체가 MANUALLY 에서만 적용됨.
        if(mActivity != null) {
            mActivity.showToast(R.string.location_retry);
        }

        if(getView() != null) {
            if(currentMarker != null) {
                currentMarker.remove();

                currentMarker = null;
            }

            if(setManually) {
                setManually = false;
            }

            currentButton.setEnabled(true);
        }
    }

    public void onPreLocationUpdate() {
        setManually = true;

        if(currentButton != null) {// 어차피 이게 없다는 말은 아직 onCreateView 전이란 말이므로 거기서 될 것이다.
            currentButton.setEnabled(false);
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {//TODO: 이것 이전에 MAP을 쓰는 것들에 대한 NULL CHECK 필요할듯.(MARKER SET 등)
        map = googleMap;

        if(!readOnly) {
            googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    setPointerMarker(latLng.latitude, latLng.longitude);

                    //setCameraView(latLng.latitude, latLng.longitude);
                }
            });
        }

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.equals(currentMarker)) {
                    return true;
                }

                return false;
            }
        });

        //googleMap.setMyLocationEnabled(true);
        //googleMap.setLocationSource(mActivity);

        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setCompassEnabled(false);// for other devices(such as s3)

        if(isLocationAvailable) {// location off 상태의 home에서 set된 loc 올 수도 있다.
            setPointerMarker(pointerLatitude, pointerLongitude);

            setCameraView(pointerLatitude, pointerLongitude);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_map_current://TODO: 실내 GPS상황도 알아서 TIME DIFF로 처리되어야만, 여기에서 괜히 같은 CUR CAM SET하지 않는다.
                // pre와 겹치지만, 아래 if때문에 일단 그냥 놔둔다.
                setManually = true;// button enabled랑 같이 갈 수 있지만 btn state 다변화를 위해 이렇게 분리.
                currentButton.setEnabled(false);

                // TODO: update로 판가름하기에는 update는 시작시에 false되는 관계로 언제 unavailable되었는지 확인하기 힘들다. 따라서 requesting으로 확인한다.
                if(mActivity.isRequestingLocationUpdates()) {//TODO: GPS STATUS CHECK후 UNAVAILABLE하면 그쪽에서 STOP하므로 여기서는 무조건 CUR AVAILABLE이다.
                    setCurrentLocation(currentLatitude, currentLongitude);// 새로 만들어도 되지만 일단 이렇게.
                } else {
                    mActivity.startLocationUpdates();
                }

                break;
            case R.id.fragment_map_ok:
                if(isLocationAvailable) {
                    LatLng location = pointerMarker.getPosition();

                    okClicked(location.latitude, location.longitude);
                } else {
                    showToast(R.string.location_empty);
                }

                break;
            case R.id.fragment_map_close:
                mActivity.onBackPressed();

                break;
        }
    }

    public void showToast(int resId) {
        if(getActivity() != null) {
            ((MainActivity) getActivity()).showToast(resId);
        }
    }

    public void setPointerAddress(Address address) {
        if(address != null) {
            String result;

            ArrayList<String> addressFragments = new ArrayList<String>();

            for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {// index이므로 <=까지 해줘야 한다.
                addressFragments.add(address.getAddressLine(i));
            }

            Log.i("HOME", getString(R.string.msg_address_found));

            // 왜 blank 아니라 \n인지 모르겠지만 어차피 blank도 여러개 해도 하나만 되는 등(trim 같은 느낌) 일관성 없어서(다른 문자열들은 또 된다.) 되어 있던 separator 그대로 간다.
            result = TextUtils.join(System.getProperty("line.separator"), addressFragments);

            pointerMarker.setTitle(result);
        } else {
            LatLng location = pointerMarker.getPosition();

            pointerMarker.setTitle(location.latitude + ", " + location.longitude);
        }

        pointerMarker.showInfoWindow();
    }

    public void requestPointerAddress(double latitude, double longitude) {
        Location location  = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        mActivity.requestAddress(location);
    }

    public void setCurrentMarker(double latitude, double longitude) {
        currentLatitude = latitude;
        currentLongitude = longitude;

        if(currentMarker == null) {
            currentMarker = map.addMarker(new MarkerOptions()
                    //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.current))
                    .position(new LatLng(latitude, longitude)));
        } else {
            currentMarker.setPosition(new LatLng(latitude, longitude));
        }

        LogWrapper.e("Map", latitude + ", " + longitude);
    }

    public void setPointerMarker(double latitude, double longitude) {
        pointerLatitude = latitude;
        pointerLongitude = longitude;

        if (!isLocationAvailable) {// 미리 main 에서 받아온 경우도 있다. 아무튼 여기서 해주는게 각 위치에서 해주는 것보다 낫다.
            isLocationAvailable = true;
        }

        if(pointerMarker == null) {
            pointerMarker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .draggable(!readOnly));// title은 나중에 추가한다 치고, draggable 넣어두면 편할듯. 하지만 알아차리기 힘들듯. => readOnly에 따라 다르게.
        } else {
            pointerMarker.setPosition(new LatLng(latitude, longitude));
        }

        requestPointerAddress(latitude, longitude);
    }

    public void setCameraView(double latitude, double longitude) {
        CameraPosition position = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(17f).build();

        map.moveCamera(CameraUpdateFactory.newCameraPosition(position));// 이게 더 strict하다.
        //map.animateCamera(CameraUpdateFactory.newCameraPosition(position));
    }

    public void okClicked(double latitude, double longitude) {
        if (mCallbacks != null) {
            mCallbacks.onMapOKClicked(latitude, longitude);
        }
    }

    public void onClosed() {
        //TODO: 여기서 처리할 게 더 있을 수 있으므로 이렇게 CALLBACK 형식으로 했다.

        if (mCallbacks != null) {
            mCallbacks.onMapClosed();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;

        try {
            mCallbacks = (MapFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public interface MapFragmentCallbacks {
        public void onMapOKClicked(double latitude, double longitude);
        public void onMapClosed();
    }
}