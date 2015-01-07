package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.instamenu.R;
import com.instamenu.util.ByteLengthFilter;
import com.instamenu.util.DefaultFilter;
import com.instamenu.widget.SwipeDismissListViewTouchListener;
import com.instamenu.widget.TagAdapter;

import java.util.ArrayList;
import java.util.List;

public class FilterFragment_ extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String ARG_TAGS = "tags";
    private static final String ARG_SWITCHES = "switches";

    private float lastTranslate = 0.0f;

    // 처음 받는 용이다. adpater와 계속 동기화되긴 솔직히 힘들다.(switch 변경까지 생각해보면 안된다고 생각하는게 맞다.)
    private List<String> tags;
    private List<String> switches;

    SharedPreferences preferences;

    ListView list;
    TagAdapter adapter;
    EditText edit;
    Button button;

    private final String HEADER = "";

    private FilterFragmentCallbacks mCallbacks;

    public static FilterFragment_ newInstance(List<String> tags, List<String> switches) {
        FilterFragment_ fragment = new FilterFragment_();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_TAGS, tags != null ? (ArrayList<String>) tags : null);
        args.putStringArrayList(ARG_SWITCHES, switches != null ? (ArrayList<String>) switches : null);
        fragment.setArguments(args);
        return fragment;
    }
    public FilterFragment_() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            tags = getArguments().getStringArrayList(ARG_TAGS);
            switches = getArguments().getStringArrayList(ARG_SWITCHES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);

        list = (ListView) view.findViewById(R.id.fragment_filter_list);

        // setting adapter
        adapter = new TagAdapter(this, inflater, tags, switches);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        list,
                        new SwipeDismissListViewTouchListener.OnDismissCallback() {
                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    //adapter.remove(adapter.getItem(position));
                                    adapter.removeTag(position);
                                }
                                adapter.notifyDataSetChanged();
                            }
                        });
        list.setOnTouchListener(touchListener);
        list.setOnScrollListener(touchListener.makeScrollListener());

        edit = (EditText) view.findViewById(R.id.fragment_filter_edit);
        edit.setFilters(new InputFilter[]{new DefaultFilter(HEADER), new ByteLengthFilter(20)});
        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_DONE:
                        confirmTag();

                        return true;
                }

                return false;
            }
        });

        button = (Button) view.findViewById(R.id.fragment_filter_add);
        button.setOnClickListener(this);

        return view;
    }

    public void setTags(List<String> tags, List<String> switches) {
        this.tags = tags;
        this.switches = switches;

        adapter.setTags(tags, switches);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (FilterFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void closeFilter() {
        if (mCallbacks != null) {
            mCallbacks.onCloseFilter(adapter.getTags(), adapter.getSwitches());
        }
    }

    public void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        imm.showSoftInput(edit, 0);
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
    }

    public void setButtonEnabled(boolean enabled) {
        button.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_filter_add:
                setButtonEnabled(false);

                confirmTag();

                break;
            case R.id.list_row_tag:
                Toast.makeText(getActivity(), ((TextView)v).getText(), Toast.LENGTH_SHORT).show();

                break;
        }
    }

    public void confirmTag() {
        String tag = edit.getText().subSequence(HEADER.length(), edit.length()).toString();

        // 최대한 msg는 없앤다.
        if(tag == null || tag.isEmpty()) { // 빈 내용이면 그냥 놔두면 된다.
            //Toast.makeText(getActivity(), "내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
        } else if(adapter.getTags() != null && adapter.getTags().contains(tag)) { // 중복 되어도, msg보다는 차라리 list selection 시켜주든가 한다.
            // list scroll 해보기.
            list.setSelection(adapter.getTags().indexOf(tag));
            // TODO: 그리고 한번 깜빡이는 animation 넣어본다.
            //Toast.makeText(getActivity(), "이미 존재하는 이름입니다.", Toast.LENGTH_SHORT).show();
        } else {
            adapter.addTag(tag);

            hideKeyboard();

            edit.getText().replace(HEADER.length(), edit.length(), "", 0, 0);
            edit.clearFocus();

            list.setSelection(adapter.getCount());
        }

        setButtonEnabled(true);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // 차라리 이걸 switch랑 연결시켜서 쓸까
        adapter.toggleTag(position);
    }

    public interface FilterFragmentCallbacks {
        public void onCloseFilter(List<String> tags, List<String> switches);
    }
}