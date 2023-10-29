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

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {
    Button btnAction;
    ImageView btnCut;
    WebSocketServer server;
    FragmentActivity activity = this;
    MainWebSocketClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //goto joystick view
        ImageButton goJoystick = findViewById(R.id.goJoystick);
        goJoystick.setOnClickListener(var1 -> {
            Intent intent = new Intent();
            intent.setClass(this, JoystickActivity.class);
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
            client.stop();
        });

    }
}