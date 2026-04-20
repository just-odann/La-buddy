package com.example.la_buddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PriceMenuSheet extends BottomSheetDialogFragment {

    // Removed recyclerViewPrices since it's not in your XML
    private ImageButton btnClose;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Inflate the layout
        View view = inflater.inflate(R.layout.layout_price_menu, container, false);

        try {
            // 2. Initialize the Close Button
            // This now matches the <ImageButton android:id="@+id/btnClose" ... /> at the bottom of your XML
            btnClose = view.findViewById(R.id.btnClose);

            if (btnClose != null) {
                btnClose.setOnClickListener(v -> dismiss());
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "UI Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // This keeps the "BottomSheet" look clean by making the container background transparent
        View view = getView();
        if (view != null) {
            view.post(() -> {
                View parent = (View) view.getParent();
                parent.setBackgroundResource(android.R.color.transparent);
            });
        }
    }
}