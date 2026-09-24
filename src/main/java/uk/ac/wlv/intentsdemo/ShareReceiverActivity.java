package uk.ac.wlv.intentsdemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import com.bumptech.glide.Glide;

import uk.ac.wlv.intentsdemo.databinding.ActivityShareReceiverBinding;

/** Started by another app's Share sheet (matched through the intent-filter in the manifest). */
public class ShareReceiverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        ActivityShareReceiverBinding binding = ActivityShareReceiverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String type = intent.getType();

        if (!Intent.ACTION_SEND.equals(intent.getAction()) || type == null) {
            binding.tvType.setText("Nothing received");
            binding.tvReceived.setText("Open this screen from another app's Share menu.");
            return;
        }

        binding.tvType.setText("Received as " + type);

        if (type.startsWith("text/")) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            StringBuilder sb = new StringBuilder();
            if (subject != null) sb.append(subject).append("\n\n");
            sb.append(shared != null ? shared : "(no text)");
            binding.tvReceived.setText(sb);
        } else if (type.startsWith("image/")) {
            Uri image = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
            if (image != null) {
                binding.tvReceived.setVisibility(View.GONE);
                binding.ivReceived.setVisibility(View.VISIBLE);
                Glide.with(this).load(image).into(binding.ivReceived);
            } else {
                binding.tvReceived.setText("(no image)");
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}