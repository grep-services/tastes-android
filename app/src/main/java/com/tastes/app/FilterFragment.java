package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
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

import com.devspark.robototextview.widget.RobotoEditText;
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

    private static final String ARG_DEFAULT_TAG = "defaultTag";
    private static final String ARG_TAGS = "tags";
    private static final String ARG_SWITCHES = "switches";

    private float lastTranslate = 0.0f;

    //private boolean defaultTag;
    // 처음 받는 용이다. adpater와 계속 동기화되긴 솔직히 힘들다.(switch 변경까지 생각해보면 안된다고 생각하는게 맞다.)
    private List<String> tags;
    private List<String> switches;

    SharedPreferences preferences;

    View layerView;

    View header;
    CheckBox check;
    ListView list;
    TagAdapter adapter;
    EditText edit;
    Button button;

    boolean isKeyboard = false;

    //private final String HEADER = "";

    private static final String TAG_DEFAULT = "tastes";

    private FilterFragmentCallbacks mCallbacks;

    public static FilterFragment newInstance(/*boolean defaultTag, */List<String> tags, List<String> switches) {
        FilterFragment fragment = new FilterFragment();
        Bundle args = new Bundle();
        //args.putBoolean(ARG_DEFAULT_TAG, defaultTag);
        // 이렇게 해야 main의 것과 연결 안되며, 동시에 adapter로 null대신 obj전달해 ref 유지할 수 있다.
        args.putStringArrayList(ARG_TAGS, tags != null ? new ArrayList<String>(tags) : new ArrayList<String>());
        args.putStringArrayList(ARG_SWITCHES, switches != null ? new ArrayList<String>(switches) : new ArrayList<String>());
        fragment.setArguments(args);
        return fragment;
    }

    public FilterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 여기서부터 꼬인다. 그냥 넘겨받지 말고 main activity에꺼 가져쓴다.
        if (getArguments() != null) {
            //defaultTag = getArguments().getBoolean(ARG_DEFAULT_TAG);
            tags = getArguments().getStringArrayList(ARG_TAGS);
            switches = getArguments().getStringArrayList(ARG_SWITCHES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_filter, container, false);

        // tree observer 이용한 visible view height(keyboard 위쪽) 재는 방식 쓰려 했으나 adjustPan일 경우 가능하고, 다시말해 view들이 움직이게 된다는 말이어서 실패했다.
        /*
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // 하지만 이렇게 사용하긴 하도록 한다.
            @Override
            public void onGlobalLayout() {
                if(getActivity() != null) {// home에서 뜬 keyboard도 여기서 다뤄지면서 이 frag null 될 때(home to cam) 등에서 npe 나서 이렇게 했다.
                    Rect r = new Rect();
                    view.getWindowVisibleDisplayFrame(r);
                    int screenHeight = view.getRootView().getHeight();

                    int keypadHeight = screenHeight - r.bottom;

                    // 0.15 ratio is perhaps enough to determine keypad height.
                    if (keypadHeight > screenHeight * 0.15) {
                        isKeyboard = true;

                        //((MainActivity) getActivity()).setViewPagerEnabled(false);
                    } else {
                        if(isKeyboard == true) {
                            //((MainActivity) getActivity()).setViewPagerEnabled(true);

                            clearEdit();

                            isKeyboard = false;
                        }
                    }
                }
            }
        });
        */

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
        //list.setEmptyView(view.findViewById(R.id.fragment_filter_empty));
        list.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);// 이게 last item visible이면 다 위로 올리고, 아니면 그대로 덮는다.

        header = view.findViewById(R.id.fragment_filter_header);
        header.setOnClickListener(this);

        // 이건 항상 false가 낫다. 따라서 pref 저장하지 않는다.
        check = (CheckBox) header.findViewById(R.id.fragment_filter_header_check);
        //check.setChecked(defaultTag);
        check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                adapter.setSwitches(isChecked);
                //defaultTag = isChecked;
            }
        });

        layerView = view.findViewById(R.id.fragment_filter_layer);
        layerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    clearEdit();
                }

                return true;
            }
        });

        edit = (RobotoEditText) view.findViewById(R.id.fragment_filter_edit);
        edit.setAlpha(0.5f);
        edit.setText(Tag.HEADER + getString(R.string.add_tag));// 이것 때문에 어차피 hint는 무시된다.
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
        edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus == true) {// 이건 자동으로 된다. 괜히 했다가 keyboard 중복 열리는 문제 생긴다.
                    edit.setAlpha(1.0f);

                    edit.setText(Tag.HEADER);

                    layerView.setVisibility(View.VISIBLE);
                } else {
                    hideKeyboard();

                    layerView.setVisibility(View.GONE);

                    edit.setText(Tag.HEADER + getString(R.string.add_tag));

                    edit.setAlpha(0.5f);
                }
            }
        });

        button = (Button) view.findViewById(R.id.fragment_filter_add);
        button.setOnClickListener(this);

        return view;
    }

    public void clearEdit() {
        if(edit != null) {
            edit.clearFocus();
        }
    }
/*
    public void setDefaultTag(boolean defaultTag) {
        if(check != null) {
            //TODO: check 했는데 왜 home에서는 나오지만 실제 filter에서는 unchecked로 나오는지 알아보기.
            check.setChecked(defaultTag);
        }
    }

    public void setTags(List<String> tags, List<String> switches) {
        this.tags = tags;
        this.switches = switches;

        // adapter가 null이란 것은, 한번도 안 열었다는 것이고(vp...), 어쨌든 열릴 때 adapter set 하면서 tags, switches 모두 반영된다.
        if(adapter != null) {
            adapter.setTags(tags, switches);
        }
    }
*/
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

    public void tagClicked(String tag) {
        if (mCallbacks != null) {
            mCallbacks.onFilterTagClicked(tag);
        }
    }

    public void closeFilter() {
        if (mCallbacks != null) {
            //mCallbacks.onCloseFilter(defaultTag, adapter.getTags(), adapter.getSwitches());
            /*
            adpater에서 받아오는 덕분에 전달은 제대로 됐었지만 다시 filter로 돌아올 때는 그냥 local var를 썼기에 여기서는 제대로 된 값을 못 받아왔었다.
            그래서 이제 local var를 제대로 사용하기로 하고, adapter에 보내는 ref도 거기서 새로 정의되는 것이 아니라 ref 그대로 사용되게 된다.
            따라서 여기서도 adpater의 값을 받아와서 쓰는 것이 아니라 그냥 local var를 사용하면 된다.
             */
            mCallbacks.onCloseFilter(/*defaultTag, */tags, switches);
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
            case R.id.list_row_tag:
                String tag = (String) v.getTag();

                tagClicked(tag);

                break;
        }
    }

    /*
    filter에서 바로 profile로 갈 수도 있으므로 main이 아니라 filter에서 확인해야 한다.
    그리고 이 fragment 자체에서도 쓰인다.
     */
    public boolean checkTag(String tag) {
        return (adapter.getTags() != null && adapter.getTags().contains(tag));
    }

    /*
    profile에서 이 method를 call할땐 이미 check는 했을 것이다.
    이 fragment 내부적으로도 쓰이는데, 그 때 역시 check도 했을 것임을 가정한다.(실제로 그 process로 되어있다.)
     */
    public void addTag(String tag) {
        adapter.addTag(tag);
    }

    public void confirmTag() {
        String tag = edit.getText().subSequence(Tag.HEADER.length(), edit.length()).toString();

        Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);

        // 최대한 msg는 없앤다.
        if(tag == null || tag.isEmpty()) { // 빈 내용이면 그냥 놔두면 된다.
            //YoYo.with(Techniques.Shake).playOn(edit);
            edit.startAnimation(shake);
            //Toast.makeText(getActivity(), "내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
        } else if(checkTag(tag)) { // 중복 되어도, msg보다는 차라리 list selection 시켜주든가 한다.
            // list scroll 해보기.
            list.setSelection(adapter.getTags().indexOf(tag));

            //YoYo.with(Techniques.Shake).playOn(edit);
            edit.startAnimation(shake);
            //Toast.makeText(getActivity(), "이미 존재하는 이름입니다.", Toast.LENGTH_SHORT).show();
        } else {
            clearEdit();// keyboard close부터.

            addTag(tag);

            list.setSelection(adapter.getCount());// transcriptmode always는 아니라서 이렇게 해줘야만 last item invisible일 때도 scroll to bottom 된다.

            edit.getText().replace(Tag.HEADER.length(), edit.length(), "", 0, 0);// refresh는 차라리 맨 나중에.
        }

        setButtonEnabled(true);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        adapter.toggleTag(view);
    }

    public interface FilterFragmentCallbacks {
        public void onFilterTagClicked(String tag);
        public void onCloseFilter(/*boolean defaultFilter, */List<String> tags, List<String> switches);
    }
}