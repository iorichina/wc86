package iorichina.wccar;

import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import org.java_websocket.WebSocket;

import java.util.concurrent.ConcurrentHashMap;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainListener {
    final FragmentActivity activity;
    final JoystickView joystickVertical;
    final JoystickView joystickHorizontal;
    final ConcurrentHashMap<WebSocket, WebSocket> webSockets;
    public final Vertical vertical;
    public final Horizontal horizontal;
    int direction;
    int speed;

    public MainListener(FragmentActivity activity, JoystickView joystickVertical, JoystickView joystickHorizontal) {
        this.activity = activity;
        this.joystickVertical = joystickVertical;
        this.joystickHorizontal = joystickHorizontal;
        this.webSockets = new ConcurrentHashMap<>();
        this.vertical = new Vertical(this);
        this.horizontal = new Horizontal(this);
    }

    public void addWebSocket(WebSocket webSocket) {
        this.webSockets.put(webSocket, webSocket);
    }

    public void removeWebSocket(WebSocket webSocket) {
        this.webSockets.remove(webSocket);
    }

    /**
     * left or right
     */
    public static class Vertical implements JoystickView.OnMoveListener {
        MainListener mainListener;
        long update;

        public Vertical(MainListener mainListener) {
            this.mainListener = mainListener;
        }

        /**
         * @param angle    current angle 0~360°
         * @param strength current strength 0~100%
         */
        @Override
        public void onMove(int angle, int strength) {
            long diff = System.nanoTime() - update;
            int direction = ofMove(angle, strength);
            if (direction == mainListener.direction && diff < 2_000_000_000L) {
                return;
            }

            mainListener.direction = direction;
            if (diff < 50_000_000L && (strength != 0)) {
                return;
            }

            update = System.nanoTime();
            String msg = "Go direction=" + direction + " by Move to angle:" + angle + ", strength:" + strength;
            if (mainListener.webSockets.isEmpty()) {
                msg = "Skip " + msg;
                Log.d("VerticalJoystick", msg);
//                Toast.makeText(mainListener.activity, "VerticalJoystick: " + msg, Toast.LENGTH_SHORT).show();
                return;
            }

            // do whatever you want
            mainListener.go(mainListener.speed, mainListener.direction);
            Log.d("VerticalJoystick", msg);
        }

        /**
         * @param angle    current angle 90/270(°)
         * @param strength current strength 0~100(%)
         */
        public Integer ofMove(int angle, int strength) {
            int direction = 0;//-100~100
            //left
            if (angle < 180) {
                if (strength < 30) {
                    direction = -(int) Utils.map(strength, 0, 100, 0, 15);
                } else if (strength < 60) {
                    direction = -(int) Utils.map(strength, 0, 100, 0, 40);
                } else {
                    direction = -strength;
                }
            }
            //right
            else {
                if (strength < 30) {
                    direction = (int) Utils.map(strength, 0, 100, 0, 15);
                } else if (strength < 60) {
                    direction = (int) Utils.map(strength, 0, 100, 0, 40);
                } else {
                    direction = strength;
                }
            }
            return direction;
        }

    }

    /**
     * forward or backward
     */
    public static class Horizontal implements JoystickView.OnMoveListener {
        MainListener mainListener;
        long update;

        public Horizontal(MainListener mainListener) {
            this.mainListener = mainListener;
        }

        /**
         * @param angle    current angle 0~360°
         * @param strength current strength 0~100%
         */
        @Override
        public void onMove(int angle, int strength) {
            long diff = System.nanoTime() - update;
            int speed = ofMove(angle, strength);
            if (speed == mainListener.speed && diff < 2_000_000_000L) {
                return;
            }
            mainListener.speed = speed;
            if (diff < 50_000_000L && (strength != 0)) {
                return;
            }

            update = System.nanoTime();
            String msg = "Go speed=" + speed + " by Move to angle:" + angle + ", strength:" + strength;
            if (mainListener.webSockets.isEmpty()) {
                msg = "Skip " + msg;
                Log.d("HorizontalJoystick", msg);
//                Toast.makeText(mainListener.activity, "Joystick: " + msg, Toast.LENGTH_SHORT).show();
                return;
            }

            // do whatever you want
            mainListener.go(mainListener.speed, mainListener.direction);
            Log.d("HorizontalJoystick", msg);
        }

        /**
         * @param angle    current angle 0/180(°)
         * @param strength current strength 0~100(%)
         */
        public Integer ofMove(int angle, int strength) {
            //speed [-100, 100](back, forward) map to [68,75]&[105,108], direction [-100, 100](left, right) map to [10, 170]
            int speed = 0;//-100~100
            //forward
            if (angle < 180) {
                if (strength < 30) {
                    speed = (int) Utils.map(strength, 0, 100, 15, 25);
                } else if (strength < 60) {
                    speed = (int) Utils.map(strength, 0, 100, 15, 35);
                } else {
                    speed = (int) Utils.map(strength, 0, 100, 15, 50);
                }
            } else {
                speed = -(int) Utils.map(strength, 0, 100, 15, 20);
            }
            return speed;
        }

    }

    public void go(int speed, int direction) {
        for (WebSocket webSocket : webSockets.keySet()) {
            if (!webSocket.isOpen()) {
                continue;
            }
            String text = "#" + speed + "," + direction;
            webSocket.send(text);
            Log.d("WebSocket", "go " + text);
        }
    }
}
