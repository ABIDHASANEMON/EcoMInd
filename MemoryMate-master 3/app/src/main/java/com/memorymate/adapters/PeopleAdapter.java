package com.memorymate.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.memorymate.R;
import com.memorymate.models.Person;
import com.memorymate.utils.CloudSyncManager;
import com.memorymate.utils.DatabaseHelper;

import java.io.File;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.ViewHolder> {

    private Context context;
    private List<Person> peopleList;
    private DatabaseHelper databaseHelper;
    private OnPersonDeletedListener deleteListener;

    public interface OnPersonDeletedListener {
        void onPersonDeleted();
    }

    public PeopleAdapter(Context context, List<Person> peopleList, OnPersonDeletedListener listener) {
        this.context = context;
        this.peopleList = peopleList;
        this.databaseHelper = new DatabaseHelper(context);
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_person, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Person person = peopleList.get(position);

        holder.tvName.setText(person.getName());
        holder.tvRelationship.setText(person.getRelationship());
        holder.tvPhone.setText(person.getPhoneNumber());

        // Load image from stored file path (permanent storage)
        if (person.getPhotoPath() != null && !person.getPhotoPath().isEmpty()) {
            try {
                File imageFile = new File(person.getPhotoPath());
                if (imageFile.exists()) {
                    holder.ivPhoto.setImageURI(Uri.fromFile(imageFile));
                } else {
                    holder.ivPhoto.setImageResource(R.drawable.ic_people);
                }
            } catch (Exception e) {
                holder.ivPhoto.setImageResource(R.drawable.ic_people);
            }
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_people);
        }

        // Call button
        holder.ivCall.setOnClickListener(v -> makeCall(person.getPhoneNumber()));

        // Delete button
        holder.ivDelete.setOnClickListener(v -> showDeleteConfirmation(person, position));
    }

    private void makeCall(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "Cannot make call", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation(Person person, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Person")
                .setMessage("Are you sure you want to delete " + person.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deletePerson(person, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePerson(Person person, int position) {
        // Delete from database (also deletes image file)
        databaseHelper.deletePerson(person.getId());

        // Remove from list
        peopleList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, peopleList.size());

        // Sync to cloud
        CloudSyncManager.getInstance(context).syncPeopleToCloud();

        Toast.makeText(context, person.getName() + " deleted", Toast.LENGTH_SHORT).show();

        // Notify fragment to refresh if list is empty
        if (deleteListener != null) {
            deleteListener.onPersonDeleted();
        }
    }

    @Override
    public int getItemCount() {
        return peopleList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivPhoto;
        TextView tvName, tvRelationship, tvPhone;
        ImageView ivCall, ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            tvName = itemView.findViewById(R.id.tv_name);
            tvRelationship = itemView.findViewById(R.id.tv_relationship);
            tvPhone = itemView.findViewById(R.id.tv_phone);
            ivCall = itemView.findViewById(R.id.iv_call);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}