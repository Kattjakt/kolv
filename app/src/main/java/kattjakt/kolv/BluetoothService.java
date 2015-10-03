package kattjakt.kolv;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Leo on 2015-10-03.
 */
public class BluetoothService {

    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private final UUID SSP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private class State {
        static final int CONNECTED = 2;
        static final int CONNECTING = 1;
        static final int NONE = 0;
    }

    private int state = State.NONE;

    public int getState() {
        return this.state;
    }

    public void start() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        state = State.NONE;
    }

    public void connect(BluetoothDevice device) {
        Log.d("BLUETOOTH_SERVICE", "Connecting to device ...");
        this.state = State.CONNECTING;

        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public void connected(BluetoothDevice device, BluetoothSocket socket) {
        Log.d("BLUETOOTH_SERVICE", "Successfully connected to: " + device.getName());
        this.state = State.CONNECTED;

        connectThread.cancel();
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    public void connectionFailed() {
        Log.d("BLUETOOTH_SERVICE", "Connection failed");
        this.state = State.NONE;
    }

    public void connectionLost() {
        Log.d("BLUETOOTH_SERVICE", "Connection lost");
        this.state = State.NONE;
    }

    public class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private BluetoothDevice device;

        public void cancel() {
            try {
                socket.close();
                return;
            } catch (IOException e) {
                return;
            }
        }

        public void run() {
            setName("CONNECT_THREAD");
            adapter.cancelDiscovery();

            try {
                socket.connect();
            } catch(IOException connectException) {
                Log.d("CONNECT_THREAD", "Could not connect: " + connectException.toString());
                connectionFailed();
                try {
                    socket.close();
                } catch(IOException closeException) {
                    Log.d("CONNECT_THREAD", "Could not close socket: " + closeException.toString());
                }

                // start();

                return;
            }

            connected(device, socket);
        }

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            try {
                this.socket = device.createInsecureRfcommSocketToServiceRecord(SSP_UUID);
                Log.d("CONNECT_THREAD", "Successfully created RFCOMM socket");
            } catch (IOException e) {
                Log.d("CONNECT_THREAD", "Could not create RFCOMM socket: " + e.toString());
                return;
            }

        }
    }

    public class ConnectedThread extends Thread {
        private InputStream inputStream;
        private OutputStream outputStream;
        private BluetoothSocket socket;

        public void cancel() {
            try {
                socket.close();
                return;
            } catch (IOException ioexception) {
                return;
            }
        }

        public void run() {
            byte data[] = new byte[1024];
            try {
                do {
                    int i = inputStream.read(data);
                    Log.d("CONNECTED_THREAD", "Recieved data: " + data);
                } while (true);
            } catch (IOException ioexception) {
                connectionLost();
            }
        }

        public void write(byte data[]) {
            try {
                outputStream.write(data);
            } catch (IOException e) {
                Log.d("CONNECTED_THREAD", "Failed to send data: " + e);
                return;
            }
            Log.d("CONNECTED_THREAD", "Successfully sent data: " + data.toString());
        }

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            try {
                this.inputStream = socket.getInputStream();
                this.outputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("CONNECTED_THREAD", "Failed to create input/output streams");
            }
            Log.d("CONNECTED_THREAD", "Successfully created input/output streams");
        }
    }
}
