package com.example.career_link_new;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    /* 🔐 Encryption */
    private static final String AES_KEY = "REPLACE-WITH-YOUR-AES-KEY";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final Key symmetricKey =
            new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");

    /* UI */
    private EditText messageEditText;
    private TextView chatTextView;
    private ScrollView chatScrollView;
    private Button sendButton;

    /* Firebase */
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    /* User */
    private String userName = "";

    private boolean isConnected = false;
    private boolean wasConnected = false;

    /* Offline queue */
    private final Queue<Message> messageQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* 🔒 Auth check */
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        /* UI */
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatTextView = findViewById(R.id.chatTextView);
        chatScrollView = findViewById(R.id.chatScrollView);
        sendButton.setEnabled(false);

        /* Firebase DB */
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://careerlink-6ce4f-default-rtdb.europe-west1.firebasedatabase.app"
        );
        databaseReference = database.getReference("messages");

        /* Load name + send connected message */
        loadUserFullName();

        /* Messages listener */
        /* Listener */
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String s) {
                String type = snapshot.child("type").getValue(String.class);
                String user = snapshot.child("userName").getValue(String.class);
                String encrypted = snapshot.child("message").getValue(String.class);

                if (user == null || encrypted == null) return;

                String text = decrypt(encrypted);
                appendMessageToChat(user, text);
            }

            public void onChildChanged(@NonNull DataSnapshot d, @Nullable String s) {
            }

            public void onChildRemoved(@NonNull DataSnapshot d) {
            }

            public void onChildMoved(@NonNull DataSnapshot d, @Nullable String s) {
            }

            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        databaseReference.addChildEventListener(childEventListener);

        /* Send */
        sendButton.setOnClickListener(v -> {
            String msg = messageEditText.getText().toString().trim();
            if (msg.isEmpty() || userName.isEmpty()) return;
            writeMessage(userName, msg);
            messageEditText.setText("");
        });

        /* Logout */
        ImageButton logout = findViewById(R.id.logout_button);
        logout.setOnClickListener(v -> {
            sendStatusMessage("disconnected");

            mAuth.signOut();
            redirectToLogin();
        });

        /* Network */
        registerNetworkCallback();

        /* Disable back */
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {}
                });
    }

    /* ---------------- NETWORK ---------------- */

    private void registerNetworkCallback() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        /* Network */
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (!wasConnected) {
                        Toast.makeText(MainActivity.this,
                                "Network connected", Toast.LENGTH_SHORT).show();
                    }
                    wasConnected = true;
                    isConnected = true;
                    flushQueuedMessages();
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (wasConnected) {
                        Toast.makeText(
                                MainActivity.this,
                                "Network disconnected",
                                Toast.LENGTH_LONG
                        ).show();

                        sendStatusMessage("network disconnected");
                    }
                    wasConnected = false;
                    isConnected = false;
                });
            }
        };

        cm.registerNetworkCallback(request, networkCallback);
    }

    /* ---------------- MESSAGES ---------------- */

    private void writeMessage(String user, String msg) {
        if (!isConnected) {
            messageQueue.add(new Message(user, msg));
            Toast.makeText(this, "Message queued (offline)", Toast.LENGTH_SHORT).show();
            return;
        }
        sendNow(user, msg);
    }

    private void sendNow(String user, String msg) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("type", "chat");
        map.put("userName", user);
        map.put("message", encrypt(msg));
        map.put("timestamp", System.currentTimeMillis());

        databaseReference.push().setValue(map)
                .addOnFailureListener(e ->
                        messageQueue.add(new Message(user, msg))
                );
    }

    private void flushQueuedMessages() {
        while (!messageQueue.isEmpty()) {
            Message m = messageQueue.poll();
            assert m != null;
            sendNow(m.userName, m.message);
        }
    }

    /* ---------------- USER ---------------- */

    private void loadUserFullName() {
        assert mAuth.getCurrentUser() != null;
        String uid = mAuth.getCurrentUser().getUid();

        FirebaseDatabase.getInstance(
                        "https://careerlink-6ce4f-default-rtdb.europe-west1.firebasedatabase.app"
                ).getReference("users").child(uid).child("fullName")
                .get().addOnSuccessListener(s -> {
                    if (!s.exists()) return;

                    String full = s.getValue(String.class);
                    if (full == null) return;

                    userName = full.split(" ")[0]; // first name
                    sendButton.setEnabled(true);

                    sendConnectedMessage(userName);
                });
    }

    private void sendConnectedMessage(String name) {
        writeMessage(name, "connected!");
    }

    /* ---------------- UI ---------------- */

    private void appendMessageToChat(String user, String msg) {
        String line = user + " : " + msg;
        SpannableString ss = new SpannableString(line);
        ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                0, user.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        chatTextView.append(ss + "\n");
        chatScrollView.post(() ->
                chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    /* ---------------- ENCRYPTION ---------------- */

    private String encrypt(String text) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher c = Cipher.getInstance(AES_MODE);
            c.init(Cipher.ENCRYPT_MODE, symmetricKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] enc = c.doFinal(text.getBytes(StandardCharsets.UTF_8));
            ByteBuffer bb = ByteBuffer.allocate(iv.length + enc.length);
            bb.put(iv).put(enc);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) { return ""; }
    }

    private String decrypt(String enc) {
        try {
            byte[] d = Base64.getDecoder().decode(enc);
            ByteBuffer bb = ByteBuffer.wrap(d);

            byte[] iv = new byte[GCM_IV_LENGTH];
            bb.get(iv);

            byte[] cipher = new byte[bb.remaining()];
            bb.get(cipher);

            Cipher c = Cipher.getInstance(AES_MODE);
            c.init(Cipher.DECRYPT_MODE, symmetricKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(c.doFinal(cipher), StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }

    /* ---------------- UTILS ---------------- */

    private void redirectToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void sendStatusMessage(String text) {
        if (userName == null || userName.isEmpty()) return;

        writeMessage(userName, text);
    }

    static class Message {
        final String userName;
        final String message;
        Message(String u, String m) { userName = u; message = m; }
    }
}
