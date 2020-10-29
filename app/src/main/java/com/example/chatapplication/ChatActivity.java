package com.example.chatapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.chatapplication.databinding.ActivityChatBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatActivity extends AppCompatActivity implements TextWatcher {
    private ActivityChatBinding mBinding;
    private String name;
    private WebSocket webSocket;
    private static final String SERVER_PATH = "ws://echo.websocket.org";
    private static final int IMAGE_REQUEST_ID = 1256;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        name = getIntent().getStringExtra("name");
        initiateSocketConnection();
    }

    private void initiateSocketConnection() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(SERVER_PATH).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);

                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Socket connection successful!", Toast.LENGTH_SHORT).show();
                    initializeView();
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);

                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(text);
                        jsonObject.put("isSent", false);

                        messageAdapter.addItem(jsonObject);
                        mBinding.recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                });


            }
        });
    }

    private void initializeView() {
        mBinding.messageEdit.addTextChangedListener(this);
        messageAdapter = new MessageAdapter(getLayoutInflater());
        mBinding.recyclerView.setAdapter(messageAdapter);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }


    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        String string = editable.toString().trim();

        if (string.isEmpty()){
            resetMessageEdit();
        }else {
            mBinding.sendBtn.setVisibility(View.VISIBLE);
            mBinding.pickImgBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void resetMessageEdit() {
        mBinding.messageEdit.removeTextChangedListener(this);

        mBinding.messageEdit.setText("");
        mBinding.sendBtn.setVisibility(View.INVISIBLE);
        mBinding.pickImgBtn.setVisibility(View.VISIBLE);

        mBinding.messageEdit.addTextChangedListener(this);

        mBinding.sendBtn.setOnClickListener(v -> {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("name",name);
                jsonObject.put("message", mBinding.messageEdit.getText().toString());

                webSocket.send(jsonObject.toString());

                jsonObject.put("isSent", true);

                messageAdapter.addItem(jsonObject);
                mBinding.recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());


                resetMessageEdit();

            } catch (JSONException e) {
                e.printStackTrace();
            }

        });
        mBinding.pickImgBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

            startActivityForResult(Intent.createChooser(intent, "Pick Image "), IMAGE_REQUEST_ID);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST_ID && resultCode == RESULT_OK){
            if (data != null && data.getData() != null){
                try {
                    InputStream inputStream =
                            getContentResolver().openInputStream(data.getData());
                    Bitmap image = BitmapFactory.decodeStream(inputStream);
                    sendImage(image);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                }
            }

        }
    }

    private void sendImage(Bitmap image) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);

        String base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            jsonObject.put("image", base64String);

            webSocket.send(jsonObject.toString());

            jsonObject.put("isSent", true);

            messageAdapter.addItem(jsonObject);
            mBinding.recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}