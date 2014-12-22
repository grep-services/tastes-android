package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
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
import com.instamenu.content.Image;
import com.instamenu.widget.TagAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;

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

    ImageLoader imageLoader;

    ImageView imageView;

    ListView list;
    TagAdapter adapter;

    Button btn;
    EditText edit;

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

        imageLoader = ImageLoader.getInstance();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        list = (ListView) view.findViewById(R.id.fragment_item_list);

        // setting header
        View header = inflater.inflate(R.layout.list_header, null, false);

        imageView = (ImageView)header.findViewById(R.id.list_header_image);
        imageLoader.displayImage("http://54.65.1.56:3639"+image.origin, imageView);

        list.addHeaderView(header);

        // setting adapter
        // *************** => adapter에 add한 것만 따로 보관하는걸 만들어야 한다. 왜냐면, item에서의 tags는 pref가 아니라 외부 tags들이므로.... 암튼 그렇다.
        adapter = new TagAdapter(inflater, image.tags);
        list.setAdapter(adapter);

        // test
        //adapter.addTag("i_am_tag_1");

        edit = (EditText) view.findViewById(R.id.fragment_item_edit);

        btn = (Button) view.findViewById(R.id.fragment_item_btn);
        btn.setOnClickListener(this);

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

    public void initActionBar() {
        if (mCallbacks != null) {
            mCallbacks.onItemInitActionBar();
        }
    }

    public void actionHomeClicked() {
        if (mCallbacks != null) {
            mCallbacks.onItemActionHomeClicked();
        }
    }

    public void actionShareClicked() {
        if (mCallbacks != null) {
            mCallbacks.onItemActionShareClicked();
        }
    }

    public void addTag() {
        if (mCallbacks != null) {
            mCallbacks.onItemAddTag(adapter.getTags_(), adapter.getSwitches());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        initActionBar();

        menu.clear();
        inflater.inflate(R.menu.item, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                actionHomeClicked();

                break;
            case R.id.action_share:
                actionShareClicked();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // doesn't check duplication, only format.
    public boolean checkTag(String tag) {
        if(tag == null) return false;
        if(tag.trim().equals("")) return false;
        if(tag.trim().contains(" ")) return false;
        if(adapter.getTags().contains(tag)) return false;

        return true;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_item_btn:
                String tag = edit.getText().toString();
                if(checkTag(tag) == true) {
                    adapter.addTag(tag.trim());

                    addTag();
                } else {
                    Toast.makeText(getActivity(), "Type right tag format", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    public interface ItemFragmentCallbacks {
        public void onItemInitActionBar();
        public void onItemActionHomeClicked();
        public void onItemActionShareClicked();
        public void onItemAddTag(List<String> tags, List<String> switches);
    }
}