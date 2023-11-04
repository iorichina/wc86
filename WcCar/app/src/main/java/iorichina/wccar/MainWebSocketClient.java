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

    JoystickView joystickVertical;
    JoystickView joystickHorizontal;
    MainListener doubleJoystickListener;

    JoystickView joystick;
    JoystickListener joystickListener;

    ImageView goLeft;
    ImageView goRight;
    ImageView goBack;
    ImageView goForward;
    MotionListener motionListener;

    public MainWebSocketClient(FragmentActivity activity
            , JoystickView joystickVertical
            , JoystickView joystickHorizontal
            , String ip
    ) {
        super(URI.create(ip));
        this.activity = activity;
        this.joystickVertical = joystickVertical;
        this.joystickHorizontal = joystickHorizontal;
        this.doubleJoystickListener = new MainListener(activity, joystickVertical, joystickHorizontal);
        joystickVertical.setOnMoveListener(doubleJoystickListener.vertical);
        joystickHorizontal.setOnMoveListener(doubleJoystickListener.horizontal);
    }

    public MainWebSocketClient(FragmentActivity activity, JoystickView joystick, String ip) {
        super(URI.create(ip));
        this.activity = activity;
        this.joystick = joystick;
        this.joystickListener = new JoystickListener(activity, joystick);
        joystick.setOnMoveListener(joystickListener);
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
        this.motionListener = new MotionListener(activity, goLeft, goRight, goBack, goForward);
    }

    public void stop() {
        if (null != doubleJoystickListener) {
            doubleJoystickListener.go(0, 0);
        }
        if (null != joystickListener) {
            joystickListener.go(0, 0);
        }
        if (null != motionListener) {
            motionListener.move(0, 0);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        String msg = "Client Open at " + getURI();
        Log.d("WebSocketClient", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketClient: " + msg, Toast.LENGTH_SHORT).show());
        if (null != this.doubleJoystickListener) {
            this.doubleJoystickListener.addWebSocket(this);
        }
        if (null != this.joystickListener) {
            this.joystickListener.addWebSocket(this);
        }
        if (null != this.motionListener) {
            this.motionListener.addWebSocket(this);
        }
    }

    @Override
    public void onMessage(String message) {
        String msg = "Client " + getURI() + " Message [" + message + "]";
        Log.d("WebSocketClient", msg);
//        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketClient: " + msg, Toast.LENGTH_SHORT).show());

//        if ("joystickClient".equals(message)) {
//            activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketClient: joystickClient", Toast.LENGTH_SHORT).show());
//        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (null != this.doubleJoystickListener) {
            this.doubleJoystickListener.removeWebSocket(this);
        }
        if (null != this.joystickListener) {
            this.joystickListener.removeWebSocket(this);
        }
        if (null != this.motionListener) {
            this.motionListener.removeWebSocket(this);
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
