package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.instamenu.R;
import com.instamenu.util.LogWrapper;
import com.instamenu.util.QueryWrapper;
import com.instamenu.widget.TagAdapter;

import java.util.ArrayList;
import java.util.List;

public class DisplayFragment extends Fragment implements Button.OnClickListener {

    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";

    private double latitude;
    private double longitude;

    private QueryWrapper queryWrapper;

    static byte[] imageToShow = null;

    ImageView imageView;

    ListView list;
    TagAdapter adapter;

    Button btn;
    EditText edit;

    private DisplayFragmentCallbacks mCallbacks;

    public static DisplayFragment newInstance(double latitude, double longitude) {
        DisplayFragment fragment = new DisplayFragment();
        Bundle args = new Bundle();
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

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
        }

        queryWrapper = new QueryWrapper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display, container, false);

        if(imageToShow == null) {
            Toast.makeText(getActivity(), "There is no image.", Toast.LENGTH_SHORT).show();

            getActivity().finish();
        } else { // 굳이 else 써야되는진 몰겠지만 finish 확실히 sync한지 모르므로. 나중에는 test도 해본다.
            list = (ListView) view.findViewById(R.id.fragment_display_list);

            // setting header
            View header = inflater.inflate(R.layout.list_header, null, false);

            imageView = (ImageView)header.findViewById(R.id.list_header_image);

            BitmapFactory.Options opts=new BitmapFactory.Options();

            opts.inPurgeable=true;
            opts.inInputShareable=true;
            opts.inMutable=false;
            opts.inSampleSize=2;

            Bitmap origin = BitmapFactory.decodeByteArray(imageToShow, 0, imageToShow.length, opts);

            int size = Math.min(origin.getWidth(), origin.getHeight());

            Bitmap square = Bitmap.createBitmap(origin, 0, 0, size, size);

            imageView.setImageBitmap(square);

            list.addHeaderView(header);

            // setting adapter
            adapter = new TagAdapter(inflater);
            list.setAdapter(adapter);

            // test
            //adapter.addTag("i_am_tag_1");

            edit = (EditText) view.findViewById(R.id.fragment_display_edit);

            btn = (Button) view.findViewById(R.id.fragment_display_btn);
            btn.setOnClickListener(this);
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

    public void initActionBar() {
        if (mCallbacks != null) {
            mCallbacks.onDisplayInitActionBar();
        }
    }

    public void actionHomeClicked() {
        if (mCallbacks != null) {
            mCallbacks.onDisplayActionHomeClicked();
        }
    }

    public void actionOKClicked() {
        if (mCallbacks != null) {
            mCallbacks.onDisplayActionOKClicked(adapter.getTags(), adapter.getSwitches());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        initActionBar();

        menu.clear();
        inflater.inflate(R.menu.display, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                actionHomeClicked();

                break;
            case R.id.action_ok:
                // send to server
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        queryWrapper.addImage(imageToShow, "addr", latitude, longitude, adapter.getTags());

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        super.onPostExecute(unused);

                        // add to pref, move to home.
                        actionOKClicked();
                    }
                };

                // 11부터는 serial이 default라서.
                if(Build.VERSION.SDK_INT< Build.VERSION_CODES.HONEYCOMB) task.execute();
                else task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // doesn't check duplication, only format.
    public boolean checkTag(String tag) {
        if(tag == null) return false;
        if(tag.trim().equals("")) return false;
        if(tag.trim().contains(" ")) return false;
        if(adapter.getTags() != null && adapter.getTags().contains(tag)) return false;

        return true;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_display_btn:
                String tag = edit.getText().toString();
                if(checkTag(tag) == true) {
                    adapter.addTag(tag.trim());
                } else {
                    Toast.makeText(getActivity(), "Type right tag format", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    public interface DisplayFragmentCallbacks {
        public void onDisplayInitActionBar();
        public void onDisplayActionHomeClicked();
        public void onDisplayActionOKClicked(List<String> tags, List<String> switches);
    }
}