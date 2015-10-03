package kattjakt.kolv;

import android.app.Activity;
import android.app.LauncherActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter btadapter = BluetoothAdapter.getDefaultAdapter();

        // Check if the unit has a bluetooth chip
        if (btadapter == null) {
            //Toast toast = Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG);
            //toast.show();
        }

        // If bluetooth is disabled, prompt to enable it
        if (!btadapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, 1);
        }

        Set<BluetoothDevice> pairedDevices = btadapter.getBondedDevices();

        ListView sidebar = (ListView) findViewById(R.id.sidebar);

        ArrayList<String> list = new ArrayList<String>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_expandable_list_item_1,
                list
        );

        BluetoothDevice arduino = null;
        if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                Log.d("BLUETOOTH_DEVICE", device.getAddress() + ", " + device.getName() + ", ");
                list.add(device.getName());
                adapter.notifyDataSetChanged();
                arduino = device;
            }
        }

        sidebar.setAdapter(adapter);

        ConnectThread t = new ConnectThread(arduino);
        t.start();



        final Button button = (Button) findViewById(R.id.BTConnect);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                v.animate();
                EditText text = (EditText)findViewById(R.id.editText);
                ConnectThread.write(text.getText().toString());
                text.setText("");
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
