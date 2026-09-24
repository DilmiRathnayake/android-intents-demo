package uk.ac.wlv.intentsdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.os.BundleCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;

import uk.ac.wlv.intentsdemo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_ATTACHMENT = "attachment_uri";
    private static final String KEY_CAMERA_URI = "camera_uri";

    private ActivityMainBinding binding;
    private Uri attachmentUri;    // image that will be attached to the email
    private Uri pendingCameraUri; // where the camera app is asked to save its photo

    // ------------------------------------------------------------------
    // Activity Result API (replaces the deprecated startActivityForResult).
    // Launchers must be registered before the activity is STARTED.
    // ------------------------------------------------------------------

    /** System photo picker: no storage permission needed. */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) setAttachment(uri);
            });

    /** Camera app writes a full-size photo into the Uri we give it. */
    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), saved -> {
                if (saved && pendingCameraUri != null) setAttachment(pendingCameraUri);
            });

    /** Contacts app. The result Uri carries a one-off read grant for the chosen row only. */
    private final ActivityResultLauncher<Intent> pickContact =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    readContact(result.getData().getData());
                }
            });

    private final ActivityResultLauncher<String> requestCallPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) placeCall();
                else snack("Call permission denied. Use Dial instead.");
            });

    /** Explicit intent that returns a value. */
    private final ActivityResultLauncher<Intent> secondActivity =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null) {
                    snack("Reply: " + data.getStringExtra(SecondActivity.EXTRA_REPLY));
                }
            });

    // ------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets();

        if (savedInstanceState != null) {
            attachmentUri = BundleCompat.getParcelable(savedInstanceState, KEY_ATTACHMENT, Uri.class);
            pendingCameraUri = BundleCompat.getParcelable(savedInstanceState, KEY_CAMERA_URI, Uri.class);
        }
        if (attachmentUri != null) showAttachment(attachmentUri);

        setUpEmail();
        setUpPhone();
        setUpWebAndMaps();
        setUpShareAndExplicit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_ATTACHMENT, attachmentUri);
        outState.putParcelable(KEY_CAMERA_URI, pendingCameraUri);
    }

    // ==================================================================
    // EMAIL  (ACTION_SEND / ACTION_SENDTO)
    // ==================================================================
    private void setUpEmail() {
        binding.btnGallery.setOnClickListener(v -> pickImage.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        binding.btnCamera.setOnClickListener(v -> launchCamera());
        binding.btnRemove.setOnClickListener(v -> clearAttachment());
        binding.btnSendEmail.setOnClickListener(v -> sendEmail());
    }

    private void launchCamera() {
        try {
            File dir = new File(getCacheDir(), "images");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
            File photo = File.createTempFile("photo_", ".jpg", dir);
            pendingCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
            takePicture.launch(pendingCameraUri);
        } catch (IOException e) {
            snack("Could not create a file for the photo.");
        } catch (ActivityNotFoundException e) {
            snack("No camera app found on this device.");
        }
    }

    private void sendEmail() {
        String[] recipients = text(binding.etEmailTo).split("\\s*[,;]\\s*");
        for (String r : recipients) {
            if (!Patterns.EMAIL_ADDRESS.matcher(r).matches()) {
                binding.tilEmailTo.setError("Enter a valid email address");
                return;
            }
        }
        binding.tilEmailTo.setError(null);

        Intent intent;
        if (attachmentUri == null) {
            // "mailto:" limits the chooser to real email apps
            intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        } else {
            // mailto: cannot carry an attachment, so use ACTION_SEND + EXTRA_STREAM
            intent = new Intent(Intent.ACTION_SEND).setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
            // ClipData + grant flag make sure the email app is allowed to read the image
            intent.setClipData(ClipData.newRawUri("attachment", attachmentUri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.putExtra(Intent.EXTRA_EMAIL, recipients);
        intent.putExtra(Intent.EXTRA_SUBJECT, text(binding.etSubject));
        intent.putExtra(Intent.EXTRA_TEXT, text(binding.etBody));
        launch(Intent.createChooser(intent, "Send email with"));
    }

    private void setAttachment(Uri uri) {
        attachmentUri = uri;
        showAttachment(uri);
    }

    private void showAttachment(Uri uri) {
        binding.cardPreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(binding.ivPreview);
    }

    private void clearAttachment() {
        attachmentUri = null;
        binding.cardPreview.setVisibility(View.GONE);
    }

    // ==================================================================
    // PHONE, SMS, CONTACTS
    // ==================================================================
    private void setUpPhone() {
        binding.btnPickContact.setOnClickListener(v -> {
            // Picking a *phone* row (not a contact) hands us the number without READ_CONTACTS
            Intent pick = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            try {
                pickContact.launch(pick);
            } catch (ActivityNotFoundException e) {
                snack("No contacts app found.");
            }
        });

        binding.btnAddContact.setOnClickListener(v -> {
            Intent insert = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
            insert.putExtra(ContactsContract.Intents.Insert.PHONE, text(binding.etPhone));
            launch(insert);
        });

        binding.btnSms.setOnClickListener(v -> {
            String number = requirePhone();
            if (number == null) return;
            Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null));
            sms.putExtra("sms_body", text(binding.etSms));
            launch(sms);
        });

        binding.btnDial.setOnClickListener(v -> {
            String number = requirePhone();
            if (number != null) launch(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)));
        });

        binding.btnCall.setOnClickListener(v -> {
            if (requirePhone() == null) return;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                placeCall();
            } else {
                requestCallPermission.launch(Manifest.permission.CALL_PHONE);
            }
        });
    }

    private void readContact(Uri contactUri) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        };
        try (Cursor c = getContentResolver().query(contactUri, projection, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                binding.etPhone.setText(c.getString(0));
                binding.tvContactName.setText("Selected: " + c.getString(1));
                binding.tvContactName.setVisibility(View.VISIBLE);
            }
        }
    }

    @SuppressLint("MissingPermission") // checked in btnCall / granted by requestCallPermission
    private void placeCall() {
        String number = requirePhone();
        if (number == null) return;
        try {
            launch(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)));
        } catch (SecurityException e) {
            snack("Call permission is missing.");
        }
    }

    /** Returns a cleaned phone number, or null (and shows an error) if it looks invalid. */
    private String requirePhone() {
        String number = text(binding.etPhone).replaceAll("[\\s()\\-]", "");
        if (number.isEmpty() || !Patterns.PHONE.matcher(number).matches()) {
            binding.tilPhone.setError("Enter a valid phone number");
            return null;
        }
        binding.tilPhone.setError(null);
        return number;
    }

    // ==================================================================
    // WEB, MAPS
    // ==================================================================
    private void setUpWebAndMaps() {
        binding.btnBrowser.setOnClickListener(v -> {
            String url = text(binding.etUrl);
            if (url.isEmpty()) {
                binding.tilUrl.setError("Enter a website address");
                return;
            }
            binding.tilUrl.setError(null);
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
            launch(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        binding.btnMaps.setOnClickListener(v -> {
            String place = text(binding.etPlace);
            Uri geo = place.isEmpty()
                    ? Uri.parse("geo:6.9271,79.8612?z=12")
                    : Uri.parse("geo:0,0?q=" + Uri.encode(place));
            launch(new Intent(Intent.ACTION_VIEW, geo));
        });
    }

    // ==================================================================
    // SHARE (chooser) + EXPLICIT INTENT
    // ==================================================================
    private void setUpShareAndExplicit() {
        binding.btnShare.setOnClickListener(v -> {
            String name = text(binding.etName);
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "Check this out");
            send.putExtra(Intent.EXTRA_TEXT, "Hello from " + (name.isEmpty() ? "IntentsDemo" : name));
            launch(Intent.createChooser(send, "Share via"));
        });

        binding.btnExplicit.setOnClickListener(v -> {
            Intent intent = new Intent(this, SecondActivity.class); // names the exact component
            intent.putExtra(SecondActivity.EXTRA_USER_NAME, text(binding.etName));
            secondActivity.launch(intent);
        });
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /** startActivity that never crashes when no app can handle the intent. */
    private void launch(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            snack("No app found that can do this.");
        }
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private static String text(TextView view) {
        CharSequence t = view.getText();
        return t == null ? "" : t.toString().trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Edge-to-edge: keep the header below the status bar and content above nav bar / keyboard. */
    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            binding.header.setPadding(dp(24), bars.top + dp(24), dp(24), dp(24));
            binding.content.setPadding(dp(16), dp(16), dp(16), bars.bottom + dp(16));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}