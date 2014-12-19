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
import android.widget.ListView;
import android.widget.Toast;

import com.instamenu.R;
import com.instamenu.widget.TagAdapter;

public class FilterFragment extends Fragment implements Button.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    ListView list;
    TagAdapter adapter;
    EditText edit;
    Button btn;

    private FilterFragmentCallbacks mCallbacks;

    public static FilterFragment newInstance(String param1, String param2) {
        FilterFragment fragment = new FilterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public FilterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);

        list = (ListView) view.findViewById(R.id.fragment_filter_list);

        // setting adapter
        adapter = new TagAdapter(inflater);
        list.setAdapter(adapter);

        edit = (EditText) view.findViewById(R.id.fragment_filter_edit);

        btn = (Button) view.findViewById(R.id.fragment_filter_btn);
        btn.setOnClickListener(this);

        return view;
    }

    /*
    public void onButtonPressed(Uri uri) {
        if (mCallbacks != null) {
            mCallbacks.onFragmentInteraction(uri);
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

    public void initActionBar() {
        if (mCallbacks != null) {
            mCallbacks.onFilterInitActionBar();
        }
    }

    public void actionHomeClicked() {
        if (mCallbacks != null) {
            mCallbacks.onFilterActionHomeClicked();
        }
    }

    public void actionOKClicked() {
        if (mCallbacks != null) {
            mCallbacks.onFilterActionOKClicked();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        initActionBar();

        menu.clear();
        inflater.inflate(R.menu.filter, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                actionHomeClicked();

                break;
            case R.id.action_ok:
                actionOKClicked();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // doesn't check duplication, only format.
    public boolean checkTag(String tag) {
        if(tag == null) return false;
        if(tag.trim().equals("")) return false;
        if(tag.trim().contains(" ")) return false;

        return true;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_filter_btn:
                String tag = edit.getText().toString();
                if(checkTag(tag) == true) {
                    adapter.addTag(tag.trim());
                } else {
                    Toast.makeText(getActivity(), "Type right tag format", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    public interface FilterFragmentCallbacks {
        public void onFilterInitActionBar();
        public void onFilterActionHomeClicked();
        public void onFilterActionOKClicked();
    }
}