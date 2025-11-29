package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.NotificationAdapter;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.NotificationItem;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.NotificationViewModel;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class NotificationFragment extends Fragment implements NotificationAdapter.OnItemLongClickListener {

    private NotificationViewModel notificationViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        NotificationAdapter adapter = new NotificationAdapter(new ArrayList<>(), this);
        notificationRecyclerView.setAdapter(adapter);

        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        notificationViewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            adapter.setNotifications(notifications);
        });

        Button clearAllButton = view.findViewById(R.id.clear_all_button);
        clearAllButton.setOnClickListener(v -> showClearAllConfirmationDialog());

        notificationViewModel.loadNotifications();
    }

    @Override
    public void onItemLongClicked(NotificationItem item) {
        showDeleteConfirmationDialog(item);
    }

    private void showClearAllConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to clear all notifications?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    notificationViewModel.clearAllNotifications();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showDeleteConfirmationDialog(NotificationItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    notificationViewModel.deleteNotification(item);
                })
                .setNegativeButton("No", null)
                .show();
    }
}
