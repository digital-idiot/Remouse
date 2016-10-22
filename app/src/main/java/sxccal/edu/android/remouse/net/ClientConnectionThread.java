package sxccal.edu.android.remouse.net;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashSet;

import sxccal.edu.android.remouse.ConnectionFragment;

import static sxccal.edu.android.remouse.ConnectionFragment.handler;

/**
 * Client to Server connection
 * @author Sayantan Majumdar
 * @author Sudipto Bhattacharjee
 */
public class ClientConnectionThread implements Runnable {

    private Context mContext;
    private Activity mActivity;
    private HashSet<String> mLocalDevices = new HashSet<>();

    private static final int SOCKET_TIMEOUT = 5000;
    private static final int UDP_PORT = 1235;
    static byte[] sServerPublicKey;

    public ClientConnectionThread(Context context, Activity activity) {
        mContext = context;
        mActivity = activity;
    }

    @Override
    public void run() {
        try {
            WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock lock = wifi.createMulticastLock("remouseMulticastLock");

            long startTime = System.currentTimeMillis(), currentTime;

            do {
                lock.acquire();
                DatagramSocket datagramSocket = new DatagramSocket(UDP_PORT);
                datagramSocket.setBroadcast(true);
                DatagramPacket datagramPacket = new DatagramPacket(new byte[Client.PUBLIC_KEY.length],
                        Client.PUBLIC_KEY.length);
                try {
                    datagramSocket.setSoTimeout(SOCKET_TIMEOUT);
                    datagramSocket.receive(datagramPacket);
                } catch(SocketTimeoutException e) {
                    lock.release();
                    datagramSocket.close();
                    break;
                }
                lock.release();

                sServerPublicKey = datagramPacket.getData();

                InetAddress inetAddress = datagramPacket.getAddress();
                mLocalDevices.add(inetAddress.toString().substring(1));

                currentTime = System.currentTimeMillis();
                datagramSocket.close();
             } while((currentTime - startTime) < SOCKET_TIMEOUT);
            handler.sendEmptyMessage(0);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mLocalDevices.isEmpty()) {
                        Toast.makeText(mActivity, "No local devices found!", Toast.LENGTH_LONG).show();
                    } else {
                        ConnectionFragment.addItems(mLocalDevices);
                    }
                }
            });
        }catch(IOException e) { e.printStackTrace(); }
    }
}
