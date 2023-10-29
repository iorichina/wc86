package iorichina.wccar;

import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {
    public static String getLocalHostAddress() {
//        try {
//            return InetAddress.getLocalHost().getHostAddress();
//        } catch (UnknownHostException e) {
//            logger.warn("[getLocalHostAddress][UnknownHostException]", e);
//        }

        List<NetworkInterface> nis;
        try {
            nis = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            Log.e("[getLocalHostAddress][SocketException]", e.toString());
            return null;
        }

        List<InetAddress> addresses = new ArrayList<>();

        for (NetworkInterface ni : nis) {
            try {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }/*
                if (isVMMac(ni.getHardwareAddress())) {
                    continue;
                }*/

                addresses.addAll(Collections.list(ni.getInetAddresses()));
            } catch (SocketException e) {
                Log.e("[getLocalHostAddress][NetworkInterface][SocketException]{}", e.toString());
            }
        }

        InetAddress local = findValidateIp(addresses);

        return null == local ? null : local.getHostAddress();
    }

    public static InetAddress findValidateIp(List<InetAddress> addresses) {
        InetAddress local = null;
        for (InetAddress address : addresses) {
            Log.d("address:{}", address.getHostAddress());
            Log.d("address-isLoopbackAddress:{}", String.valueOf(address.isLoopbackAddress()));
            Log.d("address-isSiteLocalAddress:{}", String.valueOf(address.isSiteLocalAddress()));
            Log.d("address-getHostName:{}", address.getHostName());
            if (address instanceof Inet4Address) {
                if (address.isLoopbackAddress() || address.isSiteLocalAddress()) {
                    if (local == null) {
                        local = address;
                    } else if (address.isSiteLocalAddress() && !address.isLoopbackAddress()) {
                        // site local address has higher priority than other address
                        local = address;
                    } else if (local.isSiteLocalAddress() && address.isSiteLocalAddress()) {
                        // site local address with a host name has higher
                        // priority than one without host name
                        if (local.getHostName().equals(local.getHostAddress())
                                && !address.getHostName().equals(address.getHostAddress())) {
                            local = address;
                        }
                    }
                } else {
                    if (local == null) {
                        local = address;
                    }
                }
            }
        }
        return local;
    }

    public static long map(long x, long in_min, long in_max, long out_min, long out_max) {
        long dividend = out_max - out_min;
        long divisor = in_max - in_min;
        long delta = x - in_min;

        return (delta * dividend + (divisor / 2)) / divisor + out_min;
    }
}
