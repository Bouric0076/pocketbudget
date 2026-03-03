package com.example.smsreader;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private TextView txtResults;
    private Button btnFetchSms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResults = findViewById(R.id.txtResults);
        btnFetchSms = findViewById(R.id.btnFetchSms);

        btnFetchSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndFetchSms();
            }
        });
    }

    // Task 1 & 2: Implement SMS permission request screen & Handle runtime permissions
    private void checkPermissionAndFetchSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            displaySms(fetchMpesaSms());
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                displaySms(fetchMpesaSms());
            } else {
                Toast.makeText(this, "Permission denied to read SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Task 3 & 4: Fetch SMS messages using ContentResolver & Filter by sender ID
    private List<String> fetchMpesaSms() {
        List<String> mpesaMessages = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();

        // The URI for the SMS inbox
        Uri smsUri = Uri.parse("content://sms/inbox");

        // We only need the body of the message
        String[] projection = new String[]{"body"};

        // Filter where the sender address matches "MPESA"
        String selection = "address = ?";
        String[] selectionArgs = new String[]{"MPESA"};

        Cursor cursor = contentResolver.query(smsUri, projection, selection, selectionArgs, "date DESC");

        if (cursor != null) {
            int bodyIndex = cursor.getColumnIndex("body");
            while (cursor.moveToNext()) {
                String body = cursor.getString(bodyIndex);
                mpesaMessages.add(body);
            }
            cursor.close();
        }
        return mpesaMessages;
    }

    // Deliverable: Working module that returns a list of SMS strings
    private void displaySms(List<String> messages) {
        if (messages.isEmpty()) {
            txtResults.setText("No M-Pesa messages found.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            sb.append("Message ").append(i + 1).append(":\n");
            sb.append(messages.get(i)).append("\n\n");
        }
        txtResults.setText(sb.toString());
    }
}