package com.example.la_buddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PriceMenuSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Debug Toast
        Toast.makeText(getContext(), "Menu is trying to open...", Toast.LENGTH_SHORT).show();

        try {
            // Ensure this filename is exactly what you have in res/layout
            return inflater.inflate(R.layout.layout_price_menu, container, false);
        } catch (Exception e) {
            Toast.makeText(getContext(), "XML Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }
}