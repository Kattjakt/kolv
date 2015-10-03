package kattjakt.kolv;

import android.app.Activity;
import android.app.LauncherActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends Activity {

    private BluetoothService bluetoothService;
    private BluetoothAdapter bluetoothAdapter;

    ArrayList<BluetoothDevice> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothService = new BluetoothService();

        // Check if the unit has a bluetooth chip
        if (bluetoothAdapter == null) {
            Toast toast = Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG);
            toast.show();
        }

        // If bluetooth is disabled, prompt to enable it
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, 1);
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ListView sidebar = (ListView) findViewById(R.id.devicelist);
        /*ArrayList<String> list = new ArrayList<>();
         ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                list
        );*/


        ArrayAdapter adapter = new ArrayAdapter<BluetoothDevice>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                list
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(list.get(position).getName());
                text2.setText(list.get(position).getAddress());
                return view;
            }
        };
        
        sidebar.setAdapter(adapter);
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d("BLUETOOTH_DEVICE", "Found device: " + device.getAddress() + ", " + device.getName() + ", ");
                list.add(device);
                adapter.notifyDataSetChanged();
            }
        }


        sidebar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                String MAC = parent.getAdapter().getItem(position).toString();
                bluetoothService.connect(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC));
            }
        });


        final Button button = (Button) findViewById(R.id.BTConnect);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                v.animate();
                EditText text = (EditText) findViewById(R.id.editText);
                bluetoothService.write(text.getText().toString());
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
