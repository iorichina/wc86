package iorichina.wccar;

import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainWebSocketServer extends WebSocketServer {
    final FragmentActivity activity;
    final JoystickView joystick;
    final MainJoystickListener joystickMoveListener;

    public MainWebSocketServer(FragmentActivity activity, JoystickView joystick) {
        super(new InetSocketAddress(8789));
        this.activity = activity;
        this.joystick = joystick;
        this.joystickMoveListener = new MainJoystickListener(activity, joystick);
        joystick.setOnMoveListener(joystickMoveListener);
    }

    @Override
    public void onWebsocketPing(WebSocket conn, Framedata f) {
        super.onWebsocketPing(conn, f);
        String msg = "Client " + conn + " Ping";
        Log.d("WebSocketServer", msg);
    }

    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {
        super.onWebsocketPong(conn, f);
        String msg = "Client " + conn + " Pong";
        Log.d("WebSocketServer", msg);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String msg = "Client " + conn + " Open " + handshake;
        Log.d("WebSocketServer", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketServer: " + msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String msg = "Client " + conn + " Close by " + reason + "(" + code + ")";
        Log.d("WebSocketServer", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketServer: " + msg, Toast.LENGTH_SHORT).show());
        this.joystickMoveListener.removeWebSocket(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String msg = "Client " + conn + " Message [" + message + "]";
        Log.d("WebSocketServer", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketServer: " + msg, Toast.LENGTH_SHORT).show());

        if ("joystickClient".equals(message)) {
            this.joystickMoveListener.addWebSocket(conn);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        String msg = "Client " + conn + " Error " + ex;
        Log.d("WebSocketServer", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketServer: " + msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onStart() {
        String hostAddress = Utils.getLocalHostAddress();
        String msg = "Server Start at " + hostAddress + ":" + getAddress().getPort();
        Log.d("WebSocketServer", msg);
        activity.runOnUiThread(() -> Toast.makeText(activity, "WebSocketServer: " + msg, Toast.LENGTH_SHORT).show());
    }
}
