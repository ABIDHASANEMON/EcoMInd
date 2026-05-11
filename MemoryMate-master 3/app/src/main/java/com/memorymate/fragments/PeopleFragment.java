package com.memorymate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.memorymate.R;
import com.memorymate.activities.AddPersonActivity;
import com.memorymate.adapters.PeopleAdapter;
import com.memorymate.models.Person;
import com.memorymate.utils.CloudSyncManager;
import com.memorymate.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class PeopleFragment extends Fragment implements PeopleAdapter.OnPersonDeletedListener {

    private RecyclerView recyclerView;
    private PeopleAdapter adapter;
    private List<Person> peopleList;
    private DatabaseHelper databaseHelper;
    private CloudSyncManager cloudSyncManager;

    public PeopleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view);
        Button btnAddPerson = view.findViewById(R.id.btn_add_person);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize list
        peopleList = new ArrayList<>();

        // Initialize database and cloud sync
        databaseHelper = new DatabaseHelper(getContext());
        cloudSyncManager = CloudSyncManager.getInstance(getContext());

        // Setup add button
        btnAddPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), AddPersonActivity.class);
                startActivity(intent);
            }
        });

        // Load people
        loadPeople();

        return view;
    }

    private void loadPeople() {
        // Clear existing list
        peopleList.clear();

        // Get people from database
        List<Person> people = databaseHelper.getAllPeople();

        if (!people.isEmpty()) {
            peopleList.addAll(people);
        }

        // Initialize adapter with delete listener
        adapter = new PeopleAdapter(getContext(), peopleList, this);
        recyclerView.setAdapter(adapter);
    }

    // Sync people to cloud
    private void syncPeopleToCloud() {
        if (cloudSyncManager != null) {
            cloudSyncManager.syncPeopleToCloud();
            Log.d("PeopleFragment", "People synced to cloud");
        }
    }

    @Override
    public void onPersonDeleted() {
        // Refresh the list when a person is deleted
        loadPeople();
        // Sync to cloud
        syncPeopleToCloud();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPeople(); // Refresh list when returning from AddPersonActivity
    }
}