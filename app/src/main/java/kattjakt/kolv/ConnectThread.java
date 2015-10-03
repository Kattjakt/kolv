package kattjakt.kolv;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Leo on 2015-10-02.
 */
public class ConnectThread extends Thread {
    private static BluetoothSocket socket;
    private static BluetoothDevice device;

    private final UUID SSP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    //private final UUID SSP_UUID = UUID.fromString("00000000-0000-1000-8000-00805f9b34fb");


    public static void write(String data) {
        Log.d("CONNECT_THREAD", "Successfully connected to device");
        OutputStream outputStream = null;
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            Log.d("CONNECT_THREAD", "Failed to write to socket");
            return;
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream(4);

        try {
            output.write(data.getBytes());
            outputStream.write(output.toByteArray());
        } catch (IOException e) {
            Log.d("CONNECT_THREAD", "Failed to write data: " + e.toString());
        }
    }

    public static void connect(BluetoothDevice device) {

    }


    public ConnectThread(BluetoothDevice device) {
        Log.d("CONNECT_THREAD", "Creating RFCOMM socket... ");
        this.device = device;
        try {
            this.socket = device.createInsecureRfcommSocketToServiceRecord(SSP_UUID);
            Log.d("CONNECT_THREAD", "Successfully created RFCOMM socket");
        } catch (IOException e) {
            Log.d("CONNECT_THREAD", "Could not create RFCOMM socket: " + e.toString());
            return;
        }
    }

    public void run() {
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        try {
            socket.connect();
        } catch(IOException connectException) {

            Log.d("CONNECT_THREAD", "Could not connect: " + connectException.toString());
            try {
                socket.close();
            } catch(IOException closeException) {
                Log.d("CONNECT_THREAD", "Could not close socket: " + closeException.toString());
            }
            return;
        }
        this.write("#####");
    }

    public void cancel() {
        try {
            socket.close();
        } catch(IOException e) {
            Log.d("CONNECT_THREAD", "Could not close: " + e.toString());
        }
    }
}
