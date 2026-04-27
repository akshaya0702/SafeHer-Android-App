package com.example.womensafety_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {
    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private ChatAdapter adapter;
    private List<ChatModel> chatList;
    private boolean awaitingSafetyConfirmation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        rvChat = findViewById(R.id.rv_chat);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        chatList = new ArrayList<>();
        adapter = new ChatAdapter(chatList);

        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);
        rvChat.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                rvChat.postDelayed(() -> rvChat.scrollToPosition(chatList.size() - 1), 100);
            }
        });

        ImageView btnCancel3 = findViewById(R.id.btn_cancel2); // Unga XML-la irukura ID
        btnCancel3.setOnClickListener(v -> {
            finish(); // Idhu current profile page-ah close panni thirumba Home-ku kootitu pogum
        });


        // Send Button Click Logic
        btnSend.setOnClickListener(v -> {
            String userMsg = etMessage.getText().toString().trim();
            if (!userMsg.isEmpty()) {
                // 1. User message-ah add panrom
                chatList.add(new ChatModel(userMsg, true));
                adapter.notifyItemInserted(chatList.size() - 1);
                etMessage.setText(""); // Box-ah clear panrom
                rvChat.scrollToPosition(chatList.size() - 1);

                // 2. Bot badhil sollanum
                handleBotResponse(userMsg);
            }
        });
    }


    private void handleBotResponse(String userMsg) {
        final String cleanMsg = userMsg.toLowerCase().trim();

        // 1. Show "Typing..."
        ChatModel typingModel = new ChatModel("SafeHer AI is thinking...", false);
        chatList.add(typingModel);
        adapter.notifyItemInserted(chatList.size() - 1);
        rvChat.scrollToPosition(chatList.size() - 1);

        new Handler().postDelayed(() -> {
            // Remove "Typing..."
            chatList.remove(typingModel);
            adapter.notifyDataSetChanged();

            String botReply;

            // 1️⃣ Safety Status Check (AI Context Memory)
            if (awaitingSafetyConfirmation) {
                if (cleanMsg.contains("no") || cleanMsg.contains("not safe") || cleanMsg.contains("bayama")) {
                    botReply = "⚠️ EMERGENCY ALERT! I am notifying your contacts. Stay calm and head to a public area immediately!";
                    triggerEmergencySOS(); // Link to SOS
                } else {
                    botReply = "Thank goodness! 🙏 I'm relieved you're safe. I'll keep monitoring. Need any safety tips?";
                    awaitingSafetyConfirmation = false;
                }
            }
            // 2️⃣ Danger/Panic Detection
            else if (cleanMsg.contains("scared") || cleanMsg.contains("danger") || cleanMsg.contains("panic") || cleanMsg.contains("help")) {
                botReply = "I'm right here with you. 🛡️ Please don't panic. \n\nQuick question: **Are you safe right now?** (Yes/No)";
                awaitingSafetyConfirmation = true;
            }
            // 3️⃣ Guidance
            else if (cleanMsg.contains("tips") || cleanMsg.contains("sollu") || cleanMsg.contains("advice")) {
                botReply = "Safety Protocol: \n1. Keep your phone's volume high. 📱\n2. Use the 'Fake Call' feature if someone is following you. 🎭\n3. Walk towards bright lights/crowded shops. 📍";
            }
            // 4️⃣ Default Smart Response
            else {
                botReply = "Hello! I am your SafeHer AI Shield. 🛡️ I can detect if you're in trouble, provide emotional support, or trigger SOS. How are you feeling?";
            }

            // Final message add
            chatList.add(new ChatModel(botReply, false));
            adapter.notifyItemInserted(chatList.size() - 1);
            rvChat.scrollToPosition(chatList.size() - 1);

        }, 1500); // 1.5s delay for realistic feel
    }

    private void triggerEmergencySOS() {
        Intent intent = new Intent(this, SosActionActivity.class);
        startActivity(intent);
    }
}