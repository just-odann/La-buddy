package com.example.la_buddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class PriceMenuSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Inflate the layout using the exact filename of your XML
        View view = inflater.inflate(R.layout.layout_price_menu, container, false);

        try {
            // 2. Initialize the Close Button
            // Changed from ImageButton to MaterialButton to match your XML
            MaterialButton btnClose = view.findViewById(R.id.btnClose);

            if (btnClose != null) {
                btnClose.setOnClickListener(v -> {
                    // This method closes the BottomSheetDialogFragment
                    dismiss();
                });
            } else {
                // Helpful for debugging if the ID is missing in XML
                Toast.makeText(getContext(), "Close button not found", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "UI Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Makes the container background transparent to respect your custom background drawable
        View view = getView();
        if (view != null) {
            view.post(() -> {
                View parent = (View) view.getParent();
                if (parent != null) {
                    parent.setBackgroundResource(android.R.color.transparent);
                }
            });
        }
    }
}