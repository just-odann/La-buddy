package com.example.la_buddy;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // --- STAGE 1: RECEIVED ---
        View step1 = findViewById(R.id.step1);
        ((ImageView) step1.findViewById(R.id.stageIcon)).setImageResource(R.drawable.ic_clipboard);
        ((TextView) step1.findViewById(R.id.stepTitle)).setText("Received");
        ((TextView) step1.findViewById(R.id.stepStatusText)).setText("Received: 09:15 AM");

        // --- STAGE 2: WASHING ---
        View step2 = findViewById(R.id.step2);
        ((ImageView) step2.findViewById(R.id.stageIcon)).setImageResource(R.drawable.ic_washing_machine);
        ((TextView) step2.findViewById(R.id.stepTitle)).setText("Washing");
        ((TextView) step2.findViewById(R.id.stepStatusText)).setText("Washing: 09:45 AM");

        // --- STAGE 3: DRYING ---
        View step3 = findViewById(R.id.step3);
        ((ImageView) step3.findViewById(R.id.stageIcon)).setImageResource(R.drawable.ic_drying);
        ((TextView) step3.findViewById(R.id.stepTitle)).setText("Drying");
        ((TextView) step3.findViewById(R.id.stepStatusText)).setText("Drying: 09:35 AM");

        // --- STAGE 4: FOLDING ---
        View step4 = findViewById(R.id.step4);
        ((ImageView) step4.findViewById(R.id.stageIcon)).setImageResource(R.drawable.ic_folding);
        ((TextView) step4.findViewById(R.id.stepTitle)).setText("Folding");
        ((TextView) step4.findViewById(R.id.stepStatusText)).setText("Current Stage: Folding");
        step4.findViewById(R.id.line).setBackgroundColor(getResources().getColor(R.color.la_buddy_green));

        // --- STAGE 5: READY ---
        View step5 = findViewById(R.id.step5);
        ((ImageView) step5.findViewById(R.id.stageIcon)).setImageResource(R.drawable.ic_check_circle);
        View dot5 = step5.findViewById(R.id.statusIcon);
        TextView title5 = step5.findViewById(R.id.stepTitle);
        TextView status5 = step5.findViewById(R.id.stepStatusText);
        View line5 = step5.findViewById(R.id.line);
        dot5.setBackgroundResource(R.drawable.dot_green);
        title5.setText("Ready");
        title5.setTextColor(getResources().getColor(R.color.la_buddy_green));
        status5.setText("Order is Ready for Pickup");

        // Hide the line below the final dot
        line5.setVisibility(View.GONE);
    }
}