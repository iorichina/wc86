package iorichina.wccar;

import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import org.java_websocket.WebSocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MainGoListener {
    final FragmentActivity activity;

    final ImageView goLeft;
    final ImageView goRight;
    final ImageView goBack;
    final ImageView goForward;
    final ConcurrentHashMap<WebSocket, WebSocket> webSockets;
    int speed;
    int direction;
    long update;
    GoLeftListener goLeftListener;
    GoRightListener goRightListener;
    GoBackListener goBackListener;
    GoForwardListener goForwardListener;

    public MainGoListener(
            FragmentActivity activity,
            ImageView goLeft,
            ImageView goRight,
            ImageView goBack,
            ImageView goForward
    ) {
        this.activity = activity;
        this.goLeft = goLeft;
        goLeftListener = new GoLeftListener(this);
        goLeft.setOnTouchListener(goLeftListener);
        this.goRight = goRight;
        goRightListener = new GoRightListener(this);
        goRight.setOnTouchListener(goRightListener);
        this.goBack = goBack;
        goBackListener = new GoBackListener(this);
        goBack.setOnTouchListener(goBackListener);
        this.goForward = goForward;
        goForwardListener = new GoForwardListener(this);
        goForward.setOnTouchListener(goForwardListener);
        this.webSockets = new ConcurrentHashMap<>();
    }

    static class GoLeftListener implements View.OnTouchListener {
        MainGoListener main;
        Thread goLeftThread;

        public GoLeftListener(MainGoListener main) {
            super();
            this.main = main;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Thread thread = goLeftThread;
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }

                main.direction = 0;
                main.move();
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                main.direction = -180;
//                main.move();

                Thread thread = main.goRightListener.goRightThread;
                if (null != thread && thread.isAlive()) {
                    thread.interrupt();
                }

                thread = goLeftThread;
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }

                //slide
                goLeftThread = new Thread(() -> {
                    AtomicInteger delay = new AtomicInteger(50);
                    while (!Thread.interrupted()) {
                        view.post(() -> {
                            if (main.direction > -40) {
                                main.direction -= 20;
                                main.direction = Math.max(main.direction, -100);
                            } else if (main.direction > -100) {
                                main.direction -= 10;
                                main.direction = Math.max(main.direction, -100);
                            } else {
                                delay.set(500);
                            }
                            main.move();
                        });
                        try {
                            Thread.sleep(delay.get());
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                });
                goLeftThread.start();
                return true;
            }

            return true;
        }
    }

    static class GoRightListener implements View.OnTouchListener {
        MainGoListener main;
        Thread goRightThread;

        public GoRightListener(MainGoListener main) {
            super();
            this.main = main;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Thread thread = goRightThread;
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }

                main.direction = 0;
                main.move();
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                main.direction = 180;
//                main.move();

                Thread thread = main.goLeftListener.goLeftThread;
                if (null != thread && thread.isAlive()) {
                    thread.interrupt();
                }

                thread = goRightThread;
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }

                //slide
                goRightThread = new Thread(() -> {
                    AtomicInteger delay = new AtomicInteger(50);
                    while (!Thread.interrupted()) {
                        view.post(() -> {
                            if (main.direction < 40) {
                                main.direction += 20;
                                main.direction = Math.min(main.direction, 100);
                            } else if (main.direction < 100) {
                                main.direction += 10;
                                main.direction = Math.min(main.direction, 100);
                            } else {
                                delay.set(500);
                            }
                            main.move();
                        });
                        try {
                            Thread.sleep(delay.get());
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                });
                goRightThread.start();
                return true;
            }

            return true;
        }
    }

    static class GoBackListener implements View.OnTouchListener {
        MainGoListener main;
        Thread goBackThread;

        public GoBackListener(MainGoListener main) {
            super();
            this.main = main;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Thread thread = goBackThread;
                if (null != thread && thread.isAlive()) {
                    thread.interrupt();
                }
                main.speed = 0;
                main.move();
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Thread thread = main.goForwardListener.goForwardThread;
                if (null != thread && thread.isAlive()) {
                    thread.interrupt();
                }

                thread = goBackThread;
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }

                //slide
                goBackThread = new Thread(() -> {
                    AtomicInteger delay = new AtomicInteger(50);
                    while (!Thread.interrupted()) {
                        view.post(() -> {
                            if (main.speed > -100) {
                                main.speed -= 10;
                                main.speed = Math.max(main.speed, -100);
                            } else {
                                delay.set(500);
                            }
                            main.move();
                        });
                        try {
                            Thread.sleep(delay.get());
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                });
                goBackThread.start();
            }

            return true;
        }
    }

    static class GoForwardListener implements View.OnTouchListener {
        MainGoListener main;
        Thread goForwardThread;

        public GoForwardListener(MainGoListener main) {
            super();
            this.main = main;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Thread thread = goForwardThread;
                if (null != thread && thread.isAlive()) {
                    thread.interrupt();
                }

                int speed = main.speed;
                if (speed > 80) {
                    speed -= 20;
                    main.speed = speed;
                    main.move();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }

                //slide
                if (main.speed > 0) {
                    goForwardThread = new Thread(() -> {
                        AtomicInteger delay = new AtomicInteger(50);
                        while (!Thread.interrupted()) {
                            view.post(() -> {
                                if (main.speed > 0) {
                                    main.speed -= 10;
                                    main.speed = Math.max(main.speed, 0);
                                } else {
                                    goForwardThread.interrupt();
                                    return;
                                }
                                main.move();
                            });
                            try {
                                Thread.sleep(delay.get());
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    });
                    goForwardThread.start();
                }
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Thread thread = main.goBackListener.goBackThread;
                if (null != thread && thread.isAlive()) {
                    thread.interrupt();
                }

                thread = goForwardThread;
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }

                //slide
                goForwardThread = new Thread(() -> {
                    AtomicInteger delay = new AtomicInteger(50);
                    while (!Thread.interrupted()) {
                        view.post(() -> {
                            if (main.speed < 100) {
                                main.speed += 5;
                                main.speed = Math.min(main.speed, 100);
                            } else {
                                delay.set(500);
                            }
                            main.move();
                        });
                        try {
                            Thread.sleep(delay.get());
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                });
                goForwardThread.start();
            }

            return true;
        }
    }

    public void addWebSocket(WebSocket webSocket) {
        this.webSockets.put(webSocket, webSocket);
    }

    public void removeWebSocket(WebSocket webSocket) {
        this.webSockets.remove(webSocket);
    }

    public void move(int speed, int direction) {
        this.speed = speed;
        this.direction = direction;
        move();
    }

    public void move() {
        update = System.nanoTime();
        Pair<Integer, Integer> of = ofVertical(speed, direction);
        int speed = of.first;
        int direction = of.second;
        String msg = "Go " + speed + "," + direction;
        if (webSockets.isEmpty()) {
            msg = "Skip " + msg;
            Log.d("Ctrl", msg);
            Toast.makeText(activity, "Ctrl: " + msg, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("Ctrl", msg);
        // do whatever you want
        go(speed, direction);
    }

    //speed [-100, 100](back, forward) map to [68,75]&[105,108], direction [-100, 100](left, right) map to [10, 170]
    public Pair<Integer, Integer> ofVertical(int speed, int direction) {
        //forward
        if (speed > 0) {
            speed = (int) Utils.map(speed, 0, 100, 15, 40);
        }
        //backward
        else if (speed < 0) {
            speed = -(int) Utils.map(-speed, 0, 100, 15, 23);
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
