package kattjakt.kolv;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Leo on 2015-10-02.
 */
public class ManageConnectThread extends Thread {
    public ManageConnectThread() {

    }

    public void send(BluetoothSocket socket, int data) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4);
        output.write(data);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(output.toByteArray());
    }

}
