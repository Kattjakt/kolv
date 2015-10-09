package kattjakt.kolv;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Leo on 2015-10-03.
 */
public class BluetoothService {

    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private static final UUID SSP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private Handler handler;

    private class State {
        static final int CONNECTED = 2;
        static final int CONNECTING = 1;
        static final int NONE = 0;
    }

    private int state = State.NONE;

    public BluetoothService(Handler handler) {
        this.handler = handler;
    }

    public int getState() {
        return this.state;
    }

    public synchronized void connect(BluetoothDevice device) {
        if (state == State.CONNECTING && connectThread != null) {
            Log.d("BLUETOOTH_SERVICE", "Killing thread with 'CONNECTING' state ...");
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            Log.d("BLUETOOTH_SERVICE", "Disconnecting ...");
            connectedThread.cancel();
            connectedThread = null;
        }

        Log.d("BLUETOOTH_SERVICE", "Connecting to: " + device.getName() + "(" + device.getAddress() + ")");

        this.state = State.CONNECTING;

        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public synchronized void connected(BluetoothDevice device, BluetoothSocket socket) {
        Log.d("BLUETOOTH_SERVICE", "Successfully connected to: " + device.getName());

        if (connectedThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        this.state = State.CONNECTED;

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Add Toast
        handler.sendEmptyMessage(MainActivity.MessageTypes.TOAST_CONNECTION_SUCCESS);

        Message message = new Message();
        message.obj = device.getName();
        message.what = MainActivity.MessageTypes.SHOW_CONNECTION_STATUS;
        handler.sendMessage(message);

        write("Connected");
    }

    public void connectionFailed() {
        Log.d("BLUETOOTH_SERVICE", "Connection failed");

        connectThread.cancel();
        connectThread = null;

        this.state = State.NONE;

        // Add toast
        handler.sendEmptyMessage(MainActivity.MessageTypes.TOAST_CONNECTION_FAILURE);
    }

    public void connectionLost() {
        Log.d("BLUETOOTH_SERVICE", "Connection lost");
        this.state = State.NONE;

        handler.sendEmptyMessage(MainActivity.MessageTypes.HIDE_CONNECTION_STATUS);
    }

    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        this.state = State.NONE;
    }

    public void write(String s) {
        if (this.getState() == State.CONNECTED) {
            s += "\n";
            this.connectedThread.write_raw(s.getBytes());
        }
    }

    public void handle(String s) {
        int argstart = s.indexOf('(');
        int argend   = s.indexOf(')');

        String command = s.substring(0, argstart);
        String args_raw = s.substring(argstart + 1, argend);

        List<String> args = Arrays.asList(args_raw.split(","));
        if (command.equals("State")) {
            Message message = new Message();
            message.obj = args;
            message.what = MainActivity.MessageTypes.HARDWARE_STATE;
            handler.sendMessage(message);
        }

    }

    public class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private BluetoothDevice device;

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.d("CONNECT_THREAD", "Failed to close socket: " + e.toString());
            }
        }

        public void run() {
            setName("CONNECT_THREAD");
            adapter.cancelDiscovery();

            try {
                socket.connect();
             } catch(IOException connectException) {
                Log.d("CONNECT_THREAD", "Could not connect: " + connectException.toString());
                Log.d("CONNECT_THREAD", "Trying fallback ...");

                try {
                    socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                    socket.connect();

                    Log.d("CONNECT_THREAD", "Fallback succeded!");

                } catch (Exception e) {
                    Log.d("CONNECT_THREAD", "Fallback failed as well");

                    connectionFailed();

                    try {
                        socket.close();
                    } catch(IOException closeException) {
                        Log.d("CONNECT_THREAD", "Could not close socket: " + closeException.toString());
                    }

                    return;
                }
            }
            connected(device, socket);
        }

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            try {
                this.socket = device.createRfcommSocketToServiceRecord(SSP_UUID);
                Log.d("CONNECT_THREAD", "Successfully created RFCOMM socket");
            } catch (IOException e) {
                Log.d("CONNECT_THREAD", "Could not create RFCOMM socket: " + e.toString());
            }
            return;

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
                Log.d("CONNECTED_THREAD", "Failed to close socket: " + ioexception.toString());
                return;
            }
        }

        public void run() {
            byte data[] = new byte[1024];
            String total = "";
            try {
                do {
                    // Get the total number of bytes received
                    int bytes = inputStream.read(data);

                    // Remove trailing uninitialized characters
                    String decoded = new String(data, "UTF-8").replaceAll("\u0000.*", "");

                    // Remove trailing characters from last read
                    String received = decoded.substring(0, bytes);
                    for (char c : received.toCharArray()) {
                        if (c == '\n') {
                            Log.d("CONNECTED_THREAD", "Received data: " + total);
                            handle(total);
                            total = "";
                        } else {
                            total += c;
                        }
                    }
                } while (true);
            } catch (IOException ioexception) {
                connectionLost();

            }
        }

        public void write_raw(byte data[]) {
            try {
                outputStream.write(data);
            } catch (IOException e) {
                Log.d("CONNECTED_THREAD", "Failed to send data: " + e.toString());
                return;
            }
            String decoded = new String(data);
            Log.d("CONNECTED_THREAD", "Successfully sent data: " + decoded);
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
