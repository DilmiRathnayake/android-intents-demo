package uk.ac.wlv.intentsdemo;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import uk.ac.wlv.intentsdemo.databinding.ActivitySecondBinding;

public class SecondActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "USER_NAME";
    public static final String EXTRA_REPLY = "REPLY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        ActivitySecondBinding binding = ActivitySecondBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Data sent by MainActivity through the explicit intent's extras
        String extra = getIntent().getStringExtra(EXTRA_USER_NAME);
        final String name = (extra == null || extra.trim().isEmpty()) ? "friend" : extra.trim();

        binding.tvInitial.setText(name.substring(0, 1).toUpperCase(Locale.getDefault()));
        binding.tvGreeting.setText("Hello, " + name + "!");

        // Send a result back to whoever started us
        binding.btnReply.setOnClickListener(v -> {
            Intent data = new Intent().putExtra(EXTRA_REPLY, "Nice to meet you, " + name + "!");
            setResult(RESULT_OK, data);
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}