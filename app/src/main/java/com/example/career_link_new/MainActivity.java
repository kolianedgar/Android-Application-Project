package com.example.career_link_new;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private static final String AES_KEY = "aB2#xY8qW4zH9!pD3^sF6gV5rT7@jK1v";
    private static final Key symmetricKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;    // recommended
    private static final int GCM_TAG_LENGTH = 128;  // bits

    private final String disconnected = "Disconnected!";
    private final String connected = "Connected!";
    private EditText nameEditText, messageEditText;
    private Button connectButton, sendButton;
    private TextView chatTextView;
    private ScrollView chatScrollView;

    private String userName;

    private DatabaseReference databaseReference;

    private ChildEventListener childEventListener;

    private ConnectivityManager.NetworkCallback networkCallback;

    private Queue<Message> messageQueue = new LinkedList<>();
    private boolean isConnected = false;
    private boolean isChatConnected = false;

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = findViewById(R.id.nameEditText);
        messageEditText = findViewById(R.id.messageEditText);
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.sendButton);
        chatTextView = findViewById(R.id.chatTextView);
        chatScrollView = findViewById(R.id.chatScrollView);

        messageEditText.setEnabled(false);
        sendButton.setEnabled(false);

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://careerlink-6ce4f-default-rtdb.europe-west1.firebasedatabase.app"
        );

        databaseReference = database.getReference("messages");

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String user = snapshot.child("userName").getValue(String.class);
                String encrypted = snapshot.child("message").getValue(String.class);

                if (user == null || encrypted == null) return;

                String message;
                try {
                    message = decrypt(encrypted);
                } catch (Exception e) {
                    return;
                }

                appendMessageToChat(user, message);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (!isConnected) {
                        Toast.makeText(MainActivity.this,
                                "Network connected", Toast.LENGTH_SHORT).show();
                    }
                    isConnected = true;
                    flushQueuedMessages();
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (isConnected) {
                        Toast.makeText(MainActivity.this,
                                "Network disconnected", Toast.LENGTH_LONG).show();
                    }
                    isConnected = false;
                });
            }
        };

        cm.registerNetworkCallback(networkRequest, networkCallback);

        connectButton.setOnClickListener(v -> {
            if (!isChatConnected) {
                userName = nameEditText.getText().toString().trim();
                if (userName.isEmpty()) return;

                writeMessageToDatabase(userName, connected, success -> {
                    if (success) {
                        databaseReference.addChildEventListener(childEventListener);
                        nameEditText.setEnabled(false);
                        connectButton.setText("Disconnect");
                        chatTextView.setText("");
                        messageEditText.setEnabled(true);
                        sendButton.setEnabled(true);
                        isChatConnected = true;
                    }
                });

            } else {
                writeMessageToDatabase(userName, disconnected, success -> {
                    databaseReference.removeEventListener(childEventListener);
                    nameEditText.setText("");
                    nameEditText.setEnabled(true);
                    connectButton.setText("Connect");
                    messageEditText.setEnabled(false);
                    sendButton.setEnabled(false);
                    isChatConnected = false;
                });
            }
        });

        sendButton.setOnClickListener(view -> {
            String message = messageEditText.getText().toString ();

            if(message.isEmpty()) return;

            writeMessageToDatabase(userName, message, success -> {
                if(success){
                    messageEditText.setText("");
                }else{
                    Toast.makeText(getApplicationContext(),"Error Occurred!", Toast.LENGTH_LONG).show();
                }
            });

        });

        ImageButton logout_btn = findViewById(R.id.logout_button);
        logout_btn.setEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("autoLogin", Context.MODE_PRIVATE);

        logout_btn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("key", 0);
            editor.apply();

            mAuth.signOut();
            Intent redirect_login = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(redirect_login);
        });
        /* 🔒 Disable back button */
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Back button disabled
                    }
                });
    }

    private void writeMessageToDatabase(String userName, String message, MessageWriteCallback callback) {

        if (userName == null || userName.isEmpty() || message == null) {
            if (callback != null) callback.isSuccess(false);
            return;
        }

        String encryptedMessage = encrypt(message);

        HashMap<String, Object> messageHashMap = new HashMap<>();
        messageHashMap.put("userName", userName);
        messageHashMap.put("message", encryptedMessage);
        messageHashMap.put("timestamp", System.currentTimeMillis());

        if (isConnected) {

            String key = databaseReference.push().getKey();
            if (key == null) {
                if (callback != null) callback.isSuccess(false);
                return;
            }

            databaseReference.child(key)
                    .setValue(messageHashMap)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.isSuccess(true);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.isSuccess(false);
                    });

        } else {
            // OFFLINE MODE
            messageQueue.offer(new Message(userName, message));

            if (callback != null) {
                callback.isSuccess(false);
            }

            runOnUiThread(() -> messageEditText.setText(""));
        }
    }

    private void flushQueuedMessages() {
        while (!messageQueue.isEmpty() && isConnected) {
            Message msg = messageQueue.poll();
            if (msg == null) return;

            writeMessageToDatabase(
                    msg.getUserName(),
                    msg.getMessage(),
                    success -> {
                        if (!success) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Failed to send queued message",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (networkCallback != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey, spec);

            byte[] cipherText = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            // Concatenate IV + ciphertext
            ByteBuffer byteBuffer =
                    ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            return "";
        }
    }


    private String decrypt(String encryptedText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, symmetricKey, spec);

            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            return "";
        }
    }


    private void appendMessageToChat(String userName, String message) {
        String userNameText =  userName + " : ";
        String fullText = userNameText + message;

        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf(userNameText);
        int endIndex = startIndex + userNameText.length();
        spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if(!chatTextView.getText().toString().isEmpty()){
            chatTextView.append("\n");
        }
        chatTextView.append(spannableString + "\n");

        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    interface MessageWriteCallback {
        void isSuccess(boolean success);
    }

    static class Message {
        private final String userName;
        private final String message;

        public Message(String userName, String message) {
            this.userName = userName;
            this.message = message;
        }

        public String getUserName() {
            return this.userName;
        }

        public String getMessage() {
            return this.message;
        }
    }
}