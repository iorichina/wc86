package iorichina.wccar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.java_websocket.server.WebSocketServer;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class JoystickActivity extends AppCompatActivity {
    JoystickView joystick;
    Button btnAction;
    Button btnCut;
    WebSocketServer server;
    FragmentActivity activity = this;
    MainWebSocketClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joystick);
        //goto back main view
        ImageButton goMain = findViewById(R.id.goMain);
        goMain.setOnClickListener(var1 -> {
            Intent intent = new Intent();
            intent.setClass(this, MainActivity.class);
            startActivity(intent);
        });
        joystick = findViewById(R.id.joystickView);
        TextView ipText = findViewById(R.id.ipText);
        btnAction = findViewById(R.id.btnAction);
        btnCut = findViewById(R.id.btnCut);

        btnAction.setOnClickListener(view -> {
            if (null != client) {
                client.close();
            }
            client = new MainWebSocketClient(activity, joystick, ipText.getText().toString());
            client.connect();
        });
        btnCut.setOnClickListener(view -> {
            client.stop();
        });

//        server = new MainWebSocketServer(this, joystick);
//        try {
//            server.start();
//            String hostAddress = Utils.getLocalHostAddress();
//            ipText.setText(hostAddress + ":" + server.getAddress().getPort());
//        } catch (Exception e) {
//            String msg = "MainWebSocketServer start Error " + e;
//            Log.d("WebSocketServer", msg);
//            Toast.makeText(activity, "WebSocketServer: " + msg, Toast.LENGTH_SHORT).show();
//        }
    }
}