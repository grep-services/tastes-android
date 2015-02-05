package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.tastes.R;
import com.tastes.content.Tag;
import com.tastes.util.ByteLengthFilter;
import com.tastes.util.DefaultFilter;
import com.tastes.widget.SwipeDismissListViewTouchListener;
import com.tastes.widget.TagAdapter;

import java.util.ArrayList;
import java.util.List;

// TODO: tag가 내용 포함할지는 몰라도, 변경된 사항들 중(tag 추가, 삭제, switch 변경) 실제적인 true가 없을 경우는 refresh에서 제외하는 기능도 추가하도록 한다.
public class FilterFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String ARG_TAGS = "tags";
    private static final String ARG_SWITCHES = "switches";

    private float lastTranslate = 0.0f;

    // 처음 받는 용이다. adpater와 계속 동기화되긴 솔직히 힘들다.(switch 변경까지 생각해보면 안된다고 생각하는게 맞다.)
    private List<String> tags;
    private List<String> switches;

    SharedPreferences preferences;

    View header;
    CheckBox check;
    ListView list;
    TagAdapter adapter;
    EditText edit;
    Button button;

    //private final String HEADER = "";

    private FilterFragmentCallbacks mCallbacks;

    public static FilterFragment newInstance(List<String> tags, List<String> switches) {
        FilterFragment fragment = new FilterFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_TAGS, tags != null ? (ArrayList<String>) tags : null);
        args.putStringArrayList(ARG_SWITCHES, switches != null ? (ArrayList<String>) switches : null);
        fragment.setArguments(args);
        return fragment;
    }

    public FilterFragment() {
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
        list.setSelection(adapter.getCount());// 이건 말그대로 filter create시에만 한다. 나머지는 놔둔다.(reopen 등등.)
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
        list.setEmptyView(view.findViewById(R.id.fragment_filter_empty));

        header = view.findViewById(R.id.fragment_filter_header);
        header.setOnClickListener(this);

        check = (CheckBox) header.findViewById(R.id.fragment_filter_header_check);
        check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                adapter.setSwitches(isChecked);
            }
        });

        edit = (EditText) view.findViewById(R.id.fragment_filter_edit);
        edit.setText(Tag.HEADER);// 이것 때문에 어차피 hint는 무시된다.
        edit.setFilters(new InputFilter[]{new DefaultFilter(), new ByteLengthFilter(50)});
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
        edit.setSelection(1);
        /* 버벅거리는 것이 있는데 이것 때문인지... 빼본다.
        edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(adapter.isEmpty() == false) {
                    list.setSelection(hasFocus ? adapter.getCount() : 0);
                }
            }
        });
        */

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
            case R.id.fragment_filter_header:
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.fragment_filter_header_check);
                checkBox.setChecked(!checkBox.isChecked());

                break;
            case R.id.fragment_filter_add:
                setButtonEnabled(false);

                confirmTag();

                break;
            /*
            case R.id.list_row_tag:
                Toast.makeText(getActivity(), ((TextView)v).getText(), Toast.LENGTH_SHORT).show();

                break;
                */
        }
    }

    public void confirmTag() {
        String tag = edit.getText().subSequence(Tag.HEADER.length(), edit.length()).toString();

        Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);

        // 최대한 msg는 없앤다.
        if(tag == null || tag.isEmpty()) { // 빈 내용이면 그냥 놔두면 된다.
            //YoYo.with(Techniques.Shake).playOn(edit);
            edit.startAnimation(shake);
            //Toast.makeText(getActivity(), "내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
        } else if(adapter.getTags() != null && adapter.getTags().contains(tag)) { // 중복 되어도, msg보다는 차라리 list selection 시켜주든가 한다.
            // list scroll 해보기.
            list.setSelection(adapter.getTags().indexOf(tag));

            //YoYo.with(Techniques.Shake).playOn(edit);
            edit.startAnimation(shake);
            //Toast.makeText(getActivity(), "이미 존재하는 이름입니다.", Toast.LENGTH_SHORT).show();
        } else {
            adapter.addTag(tag);

            hideKeyboard();

            edit.getText().replace(Tag.HEADER.length(), edit.length(), "", 0, 0);
            edit.clearFocus();

            list.setSelection(adapter.getCount());
        }

        setButtonEnabled(true);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        adapter.toggleTag(view);
    }

    public interface FilterFragmentCallbacks {
        public void onCloseFilter(List<String> tags, List<String> switches);
    }
}