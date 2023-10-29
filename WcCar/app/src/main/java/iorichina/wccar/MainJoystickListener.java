package iorichina.wccar;

import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import org.java_websocket.WebSocket;

import java.util.concurrent.ConcurrentHashMap;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainJoystickListener implements JoystickView.OnMoveListener {
    final FragmentActivity activity;
    final JoystickView joystick;
    final ConcurrentHashMap<WebSocket, WebSocket> webSockets;
    int angle;
    int strength;
    long update;

    public MainJoystickListener(FragmentActivity activity, JoystickView joystick) {
        this.activity = activity;
        this.joystick = joystick;
        this.webSockets = new ConcurrentHashMap<>();
    }

    public void addWebSocket(WebSocket webSocket) {
        this.webSockets.put(webSocket, webSocket);
    }

    public void removeWebSocket(WebSocket webSocket) {
        this.webSockets.remove(webSocket);
    }

    /**
     * @param angle    current angle 0~360°
     * @param strength current strength 0~100%
     */
    @Override
    public void onMove(int angle, int strength) {
        long diff = System.nanoTime() - update;
        if (diff < 50_000_000L && (angle != 0 && strength != 0)) {
            return;
        }
        if (angle == this.angle && strength == this.strength && diff < 2_000_000_000L) {
            return;
        }
        this.angle = angle;
        this.strength = strength;
        update = System.nanoTime();
        Pair<Integer, Integer> of = ofVertical(angle, strength);
        //speed [-100, 100](back, forward) map to [68,75]&[105,108], direction [-100, 100](left, right) map to [10, 170]
        int speed = of.first;
        int direction = of.second;
        String msg = "Go " + speed + "," + direction + " by Move to angle:" + angle + ", strength:" + strength;
        if (webSockets.isEmpty()) {
            msg = "Skip " + msg;
            Log.d("Joystick", msg);
            Toast.makeText(activity, "Joystick: " + msg, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("Joystick", msg);
        // do whatever you want
        go(speed, direction);
    }

    /**
     * @param angle    current angle 0~360°
     * @param strength current strength 0~100%
     */
    public Pair<Integer, Integer> ofVertical(int angle, int strength) {
        //  speed = map(-speed, 0, 100, 105, 108);
        //  speed = map(-speed, -100, 0, 68, 75);
        int speed = 0;//-100~100
        //  direction = map(-direction, 0, 100, 10, 90);
        //  direction = map(-direction, -100, 0, 90, 170);
        int direction = 0;//-100~100
        //forward+left
        if (angle <= 90) {
            speed = (int) Utils.map(strength, 0, 100, 15, 30);
            direction = -2 * (50 - joystick.getNormalizedY());
        }
        //backward+left
        else if (angle <= 180) {
            speed = -(int) Utils.map(strength, 0, 100, 15, 20);
            direction = -2 * (50 - joystick.getNormalizedY());
        }
        //backward+right
        else if (angle <= 270) {
            speed = -(int) Utils.map(strength, 0, 100, 15, 20);
            direction = 2 * (joystick.getNormalizedY() - 50);
        }
        //forward+right
        else {
            speed = (int) Utils.map(strength, 0, 100, 15, 30);
            direction = 2 * (joystick.getNormalizedY() - 50);
        }
        return Pair.create(speed, direction);
    }

    public Pair<Integer, Integer> ofHorizon(int angle, int strength) {
        int speed = 0;//-100~100
        int direction = 0;//-100~100
        if (angle <= 90) {
            speed = strength;
            direction = 2 * (joystick.getNormalizedY() - 50);
        } else if (angle <= 180) {
            speed = strength;
            direction = -2 * (50 - joystick.getNormalizedY());
        } else if (angle <= 270) {
            speed = -strength;
            direction = -2 * (50 - joystick.getNormalizedY());
        } else {
            speed = -strength;
            direction = 2 * (joystick.getNormalizedY() - 50);
        }
        return Pair.create(speed, direction);
    }

    public void go(int speed, int direction) {
        for (WebSocket webSocket : webSockets.keySet()) {
            if (!webSocket.isOpen()) {
                continue;
            }
            webSocket.send("#" + speed + "," + direction);
        }
    }
}
