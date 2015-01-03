package com.instamenu.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.commonsware.cwac.camera.CameraFragment;
import com.commonsware.cwac.camera.CameraView;
import com.instamenu.R;

/*
당장 필요한건, 찍을 때 autofocus 되고, flash나 ffc는 나중이면 된다.
추가로, singleshot이고, fullbleed(아마 비율 조정하는것인듯)이며, zoom은 나중에 고려해본다.

autofocus : true로 가지만, take picture과 연결되어야 한다. ==> true
singleshot ==> true
fullbleed : 뭔지 잘은 모르겠지만 ==> true
ffc : 일단 camera number는 activiy에서 세어서 넘겨줄텐데, 당장은 1개로 간다. ==> false
flash : 나중에. ==> false.
zoom ==> 일단 배제. 이건 boolean으로 다룰 만한 것도 아니다.

toggle되는 설정들 없으므로 아직 host customize하진 않고 simple에서의 builder 설정으로 바로 가도록 한다.
*/
public class CameraFragment_ extends CameraFragment {

    private View tools;// for splash

    private CameraView cameraView;

    private CameraFragmentCallbacks mCallbacks = null;// callback. 변경 ui는 fragment에 있고, 변경 대상 vars는 activity에 있다. activity가 implement하게 하고, 그 method를 call한다.
    // 근데 이거 그런방법은 없는가? callback 그대로 쓰는 등...
    private MainActivity mActivity = null;// 쓰일 곳이 한군데이긴 하지만, attatch에 붙여서 사용하는게 더 정리되어 보인다.
    private Button shotButton;

    public static CameraFragment_ newInstance() {
        CameraFragment_ fragment = new CameraFragment_();

        return fragment;
    }

    public CameraFragment_() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // null 처리 해줄 수도 있지만, null 되면 어차피 finish밖에 답이 없고(exception 처리도 어차피 연속으로 문제 발생시킬 여지 있음), 따라서 not null 보장하고 가는게 최선이다.
        setHost(mActivity.getCameraHost());
    }

    public void setToolsVisible(boolean visible) {
        if(tools != null) {
            tools.setVisibility(visible == true ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_, container, false);

        cameraView = (CameraView)view.findViewById(R.id.fragment_camera_preview);

        tools = view.findViewById(R.id.fragment_camera_toolbars);

        if(mActivity.isSplashViewing()) setToolsVisible(false);// 해제는 splash에서 한다.

        setCameraView(cameraView);

        shotButton = (Button) view.findViewById(R.id.fragment_camera_shot);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;

        try {
            mCallbacks = (CameraFragmentCallbacks) activity; // 이거 사용할 때는(button에 의해서든 뭐든, null check부터.)
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement CameraConfigurationListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallbacks = null;
    }

    public void setShotButtonEnabled(boolean enabled) {
        shotButton.setEnabled(enabled);

        // false일 때가 티가 나도 되지만, 기능만 disable되어도 상관없다. 순식간인데 괜히 button back 바뀌면 혼란스럽기만 할 수도 있다.
    }

    /*
    사실 현재는 button의 listener 자체가 activity에 구현되어 있기 때문에 이런 callback 방식이 필요없으나
    구조상 이런게 쓰일 곳이 있을거고 activity reference를 받아두기 위해서(host set할 때) attatch당시를 catch하려 했던 용도도 있다.
     */
    public interface CameraFragmentCallbacks {

        public void onFlashModeChanged(String mode_flash);
    }
}