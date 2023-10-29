package iorichina.wccar;

import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainWebSocketClient extends WebSocketClient {

    final FragmentActivity activity;

    JoystickView joystick;
    MainJoystickListener joystickMoveListener;

    ImageView goLeft;
    ImageView goRight;
    ImageView goBack;
    ImageView goForward;
    MainGoListener goListener;

    public MainWebSocketClient(FragmentActivity activity, JoystickView joystick, String ip) {
        super(URI.create(ip));
        this.activity = activity;
        this.joystick = joystick;
        this.joystickMoveListener = new MainJoystickListener(activity, joystick);
        joystick.setOnMoveListener(joystickMoveListener);
    }

    public MainWebSocketClient(
            FragmentActivity activity,
            ImageView goLeft,
            ImageView goRight,
            ImageView goBack,
            ImageView goForward,
            String ip) {
        super(URI.create(ip));
        this.activity = activity;
        this.goLeft = goLeft;
        this.goRight = goRight;
        this.goBack = goBack;
        this.goForward = goForward;
        this.goListener = new MainGoListener(activity, goLeft, goRight, goBack, goForward);
    }

    public void stop() {
        if (null != joystickMoveListener) {
            joystickMoveListener.go(0, 0);
        }
        if (null != goListener) {
            goListener.move(0, 0);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        String msg = "Client Open at " + getURI();
        Log.d("WebSocketClient", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketClient: " + msg, Toast.LENGTH_SHORT).show());
        if (null != this.joystickMoveListener) {
            this.joystickMoveListener.addWebSocket(this);
        }
        if (null != this.goListener) {
            this.goListener.addWebSocket(this);
        }
    }

    @Override
    public void onMessage(String message) {
        String msg = "Client " + getURI() + " Message [" + message + "]";
        Log.d("WebSocketClient", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketClient: " + msg, Toast.LENGTH_SHORT).show());

        if ("joystickClient".equals(message)) {
            activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketClient: joystickClient", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (null != this.joystickMoveListener) {
            this.joystickMoveListener.removeWebSocket(this);
        }
        if (null != this.goListener) {
            this.goListener.removeWebSocket(this);
        }
        String msg = "Client " + getURI() + " Close by " + reason + "(" + code + ")";
        Log.d("WebSocketClient", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketClient: " + msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onError(Exception ex) {
        String msg = "Client " + getURI() + " Error " + ex;
        Log.d("WebSocketClient", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketClient: " + msg, Toast.LENGTH_SHORT).show());
    }

}
