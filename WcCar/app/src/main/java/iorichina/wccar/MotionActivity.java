package iorichina.wccar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import org.java_websocket.server.WebSocketServer;

public class MotionActivity extends AppCompatActivity {
    Button btnAction;
    ImageView btnCut;
    WebSocketServer server;
    FragmentActivity activity = this;
    MainWebSocketClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motion);
        //goto main view
        ImageButton goMain = findViewById(R.id.goMain2);
        goMain.setOnClickListener(var1 -> {
            Intent intent = new Intent();
            intent.setClass(this, MainActivity.class);
            startActivity(intent);
        });

        ImageView goLeft = findViewById(R.id.goLeft);
        ImageView goRight = findViewById(R.id.goRight);
        ImageView goBack = findViewById(R.id.goBack);
        ImageView goForward = findViewById(R.id.goForward);
        TextView ipText = findViewById(R.id.ipText2);
        btnAction = findViewById(R.id.btnAction2);
        btnCut = findViewById(R.id.stop);

        btnAction.setOnClickListener(view -> {
            if (null != client) {
                client.close();
            }
            client = new MainWebSocketClient(activity, goLeft, goRight, goBack, goForward, ipText.getText().toString());
            client.connect();
        });
        btnCut.setOnClickListener(view -> {
            if (null != client) {
                client.close();
            }
        });

    }
}