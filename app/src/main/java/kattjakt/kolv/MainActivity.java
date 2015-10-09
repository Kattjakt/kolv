package kattjakt.kolv;

import android.app.Activity;
import android.app.LauncherActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private BluetoothService bluetoothService;
    private BluetoothAdapter bluetoothAdapter;


    private ProgressDialog progressDialog;
    private ArrayList<BluetoothDevice> list = new ArrayList<>();

    public class MessageTypes {
        static final int TOAST_CONNECTION_FAILURE = 0;
        static final int TOAST_CONNECTION_SUCCESS = 1;
        static final int SHOW_CONNECTION_STATUS = 2;
        static final int HIDE_CONNECTION_STATUS = 3;
        static final int HARDWARE_STATE = 4;

    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MessageTypes.TOAST_CONNECTION_FAILURE) {
                progressDialog.dismiss();
                Toast toast = Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_LONG);
                toast.show();
            }

            if (msg.what == MessageTypes.TOAST_CONNECTION_SUCCESS) {
                progressDialog.dismiss();
                Toast toast = Toast.makeText(MainActivity.this, "Successfully connected", Toast.LENGTH_LONG);
                toast.show();
            }

            if (msg.what == MessageTypes.SHOW_CONNECTION_STATUS) {
                TextView connectedText = (TextView) findViewById(R.id.connected);
                connectedText.setVisibility(View.VISIBLE);
                connectedText.setText(msg.obj.toString());
            }

            if (msg.what == MessageTypes.HIDE_CONNECTION_STATUS) {
                TextView connectedText = (TextView) findViewById(R.id.connected);
                connectedText.setVisibility(View.INVISIBLE);
            }

            if (msg.what == MessageTypes.HARDWARE_STATE) {
                List<String> args = (List<String>)msg.obj;

                int id = 0;
                switch (args.get(0)) {
                    case "headlight": id = R.id.headlight; break;
                    case "blinkers_left": id = R.id.blinkers_left; break;
                    case "blinkers_right": id = R.id.blinkers_right; break;
                }

                boolean checked = (args.get(1).equals("1") ? true : false);

                Log.d("CONNECTED_THREAD", "Set value [" + checked + "] for: " + args.get(0));

                CheckBox checkBox = (CheckBox)findViewById(id);
                checkBox.setChecked(checked);

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothService = new BluetoothService(handler);

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

                progressDialog = new ProgressDialog(MainActivity.this, android.R.style.Theme_Material_Light_Dialog);
                progressDialog.setCancelable(false);
                progressDialog.setMessage("Ansluter till " + MAC);
                progressDialog.setProgressStyle(android.R.attr.progressBarStyleSmall);
                progressDialog.show();

                bluetoothService.stop();
                bluetoothService.connect(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC));
            }
        });

        CheckBox headlight = (CheckBox)findViewById(R.id.headlight);
        headlight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
             @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            bluetoothService.write("headlight(" + (isChecked ? "on" : "off") + ")");
        }
    }
        );

        CheckBox blinkers_left = (CheckBox)findViewById(R.id.blinkers_left);
        blinkers_left.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
             @Override
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                 bluetoothService.write("blinkers_left(" + (isChecked ? "on" : "off") + ")");
             }
         }
        );

        CheckBox blinkers_right = (CheckBox)findViewById(R.id.blinkers_right);
        blinkers_right.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
             @Override
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                 bluetoothService.write("blinkers_right(" + (isChecked ? "on" : "off") + ")");
             }
         }
        );

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
