package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.instamenu.R;
import com.instamenu.content.Image;
import com.instamenu.util.ByteLengthFilter;
import com.instamenu.util.DefaultFilter;
import com.instamenu.util.QueryWrapper;
import com.instamenu.widget.TagAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class ItemFragment extends Fragment implements Button.OnClickListener {

    /*
    id를 받아와도 되고, obj를 받아와도 되지만,
    짧은 순간인데 새로 받을 필요까진 없다.
    obj를 intent로 serialization해서 받아온다.
    다만 새로고침을 할 수는 있게 해둔다.
     */
    private static final String ARG_IMAGE = "image";

    private Image image;

    private QueryWrapper queryWrapper;

    ImageLoader imageLoader;

    ViewGroup container_;
    ImageView imageView;

    private final String HEADER = "# ";

    //ListView list;
    //TagAdapter adapter;

    //Button btn;
    //EditText edit;

    private ItemFragmentCallbacks mCallbacks;

    public static ItemFragment newInstance(Image image) {
        ItemFragment fragment = new ItemFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IMAGE, image);
        fragment.setArguments(args);
        return fragment;
    }
    public ItemFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            image = (Image) getArguments().getSerializable(ARG_IMAGE);
        }

        queryWrapper = new QueryWrapper();

        imageLoader = ImageLoader.getInstance();

        setHasOptionsMenu(true);
    }

    public int getPixel(int dp) {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        float density = dm.density;

        return (int)(dp * density);
    }

    public TextView getText(String tag, int left, int top) {
        TextView text = new TextView(getActivity());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = left;
        layoutParams.topMargin = top;
        text.setLayoutParams(layoutParams);
        int p = getPixel(16);
        text.setPadding(p, p, p, p);
        text.setText(HEADER + tag);
        text.setTextSize(24);
        text.setTextColor(getResources().getColor(android.R.color.white));
        text.setSingleLine(true);
        text.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        text.setId(R.id.fragment_item_tag);
        text.setOnClickListener(this);

        return text;
    }

    public void fillContainer(ViewGroup container, Image image) {
        // TODO: 일단 image not null일 것이기 때문에 그냥 간다. 하지만 확실히 query wrapper까지 확인해서 null 가능성 확인해보도록 한다.
        for(int i = 0; i < image.tags.size(); i++) {
            String position = image.positions.get(i);
            float ratioX = Float.valueOf(position.split("\\|")[0]);
            float ratioY = Float.valueOf(position.split("\\|")[1]);
            TextView text = getText(image.tags.get(i), (int)(container.getMeasuredWidth() * ratioX), (int)(container.getMeasuredHeight() * ratioY));
            container.addView(text);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        // set image
        imageView = (ImageView) view.findViewById(R.id.fragment_item_image);
        imageLoader.displayImage("http://54.65.1.56:3639"+image.origin, imageView);

        // set container
        container_ = (ViewGroup) view.findViewById(R.id.fragment_item_container);
        container_.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(container_.getWidth() > 0 && container_.getHeight() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        container_.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        container_.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }

                    fillContainer(container_, image);
                }
            }
        });
        //fillContainer(container_, image);
        /*
        list = (ListView) view.findViewById(R.id.fragment_item_list);

        // setting header
        View header = inflater.inflate(R.layout.list_header, null, false);

        imageView = (ImageView)header.findViewById(R.id.list_header_image);
        imageLoader.displayImage("http://54.65.1.56:3639"+image.origin, imageView);

        ((TextView) header.findViewById(R.id.list_header_address)).setText(image.address);

        list.addHeaderView(header);

        // setting adapter
        // *************** => adapter에 add한 것만 따로 보관하는걸 만들어야 한다. 왜냐면, item에서의 tags는 pref가 아니라 외부 tags들이므로.... 암튼 그렇다.
        adapter = new TagAdapter(inflater, image.tags);
        list.setAdapter(adapter);

        // test
        //adapter.addTag("i_am_tag_1");

        edit = (EditText) view.findViewById(R.id.fragment_item_edit);

        ((Button) view.findViewById(R.id.fragment_item_share)).setOnClickListener(this);
        ((Button) view.findViewById(R.id.fragment_item_add)).setOnClickListener(this);
        */
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (ItemFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void actionShareClicked() {
        if (mCallbacks != null) {
            mCallbacks.onItemActionShareClicked();
        }
    }

    /*
    public void addTag() {
        if (mCallbacks != null) {
            mCallbacks.onItemAddTag(adapter.getTags_(), adapter.getSwitches());
        }
    }

    // doesn't check duplication, only format.
    public boolean checkTag(String tag) {
        if(tag == null) return false;
        if(tag.trim().equals("")) return false;
        if(tag.trim().contains(" ")) return false;
        if(adapter.getTags() != null && adapter.getTags().contains(tag)) return false;

        return true;
    }
    */

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_item_share:
                actionShareClicked();

                break;
            case R.id.fragment_item_tag:
                Toast.makeText(getActivity(), ((TextView)v).getText(), Toast.LENGTH_SHORT).show();

                break;
            /*
            case R.id.fragment_item_add:
                final String tag = edit.getText().toString();
                if(checkTag(tag) == true) {
                    // add to edittext
                    adapter.addTag(tag.trim());
                    // add to pref
                    addTag();// TODO: list가 아니라 낱개로 바꾸는게 나을듯 하다.
                    // send to server
                    AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            List<String> tags = new ArrayList<String>();
                            tags.add(tag);

                            queryWrapper.tagImage(image.id, tags);

                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void unused) {
                            super.onPostExecute(unused);
                        }
                    };

                    // 11부터는 serial이 default라서.
                    if(Build.VERSION.SDK_INT< Build.VERSION_CODES.HONEYCOMB) task.execute();
                    else task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    Toast.makeText(getActivity(), "Type right tag format", Toast.LENGTH_SHORT).show();
                }

                break;
            */
        }
    }

    public interface ItemFragmentCallbacks {
        public void onItemActionShareClicked();
        //public void onItemAddTag(List<String> tags, List<String> switches);
    }
}