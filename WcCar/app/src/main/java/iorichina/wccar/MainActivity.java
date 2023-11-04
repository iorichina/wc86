package iorichina.wccar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import org.java_websocket.server.WebSocketServer;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {
    JoystickView joystickVertical;
    JoystickView joystickHorizontal;
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
        ImageButton goMotion = findViewById(R.id.goMotion);
        goMotion.setOnClickListener(var1 -> {
            Intent intent = new Intent();
            intent.setClass(this, MotionActivity.class);
            startActivity(intent);
        });

        joystickVertical = findViewById(R.id.joystickVertical);
        joystickHorizontal = findViewById(R.id.joystickHorizontal);
        TextView ipText = findViewById(R.id.ipText0);
        btnAction = findViewById(R.id.btnAction0);
        btnCut = findViewById(R.id.stop0);
        btnCut.setVisibility(View.INVISIBLE);

        btnAction.setOnClickListener(view -> {
            if (null != client) {
                client.close();
            }
            client = new MainWebSocketClient(activity, joystickVertical, joystickHorizontal, ipText.getText().toString());
            client.connect();
            ipText.setVisibility(View.INVISIBLE);
            btnAction.setVisibility(View.INVISIBLE);
            btnCut.setVisibility(View.VISIBLE);
        });
        btnCut.setOnClickListener(view -> {
            if (null != client) {
                client.close();
            }
            btnCut.setVisibility(View.INVISIBLE);
            ipText.setVisibility(View.VISIBLE);
            btnAction.setVisibility(View.VISIBLE);
        });

    }
}