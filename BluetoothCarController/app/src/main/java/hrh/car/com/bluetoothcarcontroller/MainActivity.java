package hrh.car.com.bluetoothcarcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public static String TAG = MainActivity.class.getName();

    TextView myLabel;
    EditText myTextbox, id;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    boolean status = false;
    boolean sf = false;
    boolean bf = false;
    boolean f1 = false;
    boolean f2 = false;
    String prevsent = "";

    //Acclerometer
    private TextView currentX, currentY, currentZ;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    volatile boolean stopWorker;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        } else {
            // fai! we dont have an accelerometer!
        }

        Button openButton = (Button) findViewById(R.id.open);
        Button sendButton = (Button) findViewById(R.id.send);
        Button closeButton = (Button) findViewById(R.id.close);
        myLabel = (TextView) findViewById(R.id.label);
        myTextbox = (EditText) findViewById(R.id.entry);
        id = (EditText) findViewById(R.id.btid);

        Button back = (Button) findViewById(R.id.back);
        back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        //textEvent.setText("ACTION_DOWN");
                        Log.d(TAG, "Down");
                        bf = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        // textEvent.setText("ACTION_UP");          // you need this!
                        Log.d(TAG, "up");
                        bf = false;
                        break;
                    default:
                        //  textEvent.setText("Unknown!");
                }

                return true;
            }
        });

        Button acclerate = (Button) findViewById(R.id.acclerate);
        acclerate.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        //textEvent.setText("ACTION_DOWN");

                        Log.d(TAG, "Down");
                        sf = true;

                        break;
                    /*  case MotionEvent.ACTION_MOVE:
                      // textEvent.setText("ACTION_MOVE");
						  Log.d(TAG, "move");
					   break;*/
                    case MotionEvent.ACTION_UP:
                        // textEvent.setText("ACTION_UP");          // you need this!
                        Log.d(TAG, "up");
                        sf = false;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        // textEvent.setText("ACTION_CANCEL");
                        Log.d(TAG, "cancel");
                        break;
                    default:
                        //  textEvent.setText("Unknown!");
                }

                return true;
            }
        });

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                findBT();
            }
        });

        //Send Button
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    sendData();
                } catch (IOException ex) {
                }
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    closeBT();
                } catch (IOException ex) {
                }
            }
        });
    }

    public void initializeViews() {
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);
    }

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            myLabel.setText("No bluetooth adapter available");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }


	     /*Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		    if(pairedDevices.size() > 0)
		    {
		        for(BluetoothDevice device : pairedDevices)
		        {
		            if(device.getName().equals("MattsBlueTooth"))
		            {
		                mmDevice = device;
		                break;
		            }
		        }
		    }*/

        Log.d(TAG, "c" + id.getText());
        if (id.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter your bluetooth ID to connect!", Toast.LENGTH_LONG).show();
        } else {

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            mmDevice = mBluetoothAdapter.getRemoteDevice(id.getText().toString());//id.getText().toString()
            if (pairedDevices.contains(mmDevice)) {
                myLabel.setText("Bluetooth Device Found, address: " + mmDevice.getAddress());
                try {
                    openBT();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "BT is pairing");
            } else {
                myLabel.setText("Couden't find the Bluetooth Device.");
            }
            // myLabel.setText("Bluetooth Device Found");
        }
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        myLabel.setText("Bluetooth Opened");
        status = true;
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            myLabel.setText(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

    void sendData2(String mess) throws IOException {
        String msg = mess;
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
    }

    void sendData() throws IOException {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
    }

    void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub

        // Clean current values
        displayCleanValues();

        deltaX = event.values[0];
        deltaY = event.values[1];
        deltaZ = event.values[2];

        // Display the current x,y,z accelerometer values
        displayCurrentValues();

        // if the change is below 2, it is just plain noise
        if (deltaX < 2)
            deltaX = 0;

        if (deltaY < -3) {
            //deltaY = 0;
            f1 = true;
            Log.d(TAG, "L");
        } else if (deltaY > 3) {
            Log.d(TAG, "R");
            f2 = true;

        } else {
            f1 = false;
            f2 = false;

        }

        if (deltaZ < 2)
            deltaZ = 0;


        if (status) {
            if (bf && f2) {
                Log.d(TAG, "BF && F2");
                if (prevsent.equals("5")) {

                } else {
                    try {
                        sendData2("5");
                        prevsent = "5";
                    } catch (IOException ex) {
                    }
                }
            } else if (bf && f1) {
                Log.d(TAG, "BF && F1");
                if (prevsent.equals("6")) {

                } else {
                    try {
                        sendData2("6");
                        prevsent = "6";
                    } catch (IOException ex) {
                    }
                }
            } else if (sf && f2) {
                Log.d(TAG, "SF && F2");
                if (prevsent.equals("8")) {

                } else {
                    try {
                        sendData2("8");
                        prevsent = "8";
                    } catch (IOException ex) {
                    }
                }
            } else if (sf && f1) {
                Log.d(TAG, "SF && F1");
                if (prevsent.equals("9")) {

                } else {
                    try {
                        sendData2("9");
                        prevsent = "9";
                    } catch (IOException ex) {
                    }
                }
            } else if (f2) {
                Log.d(TAG, "F2");
                if (prevsent.equals("2")) {

                } else {
                    try {
                        sendData2("2");
                        prevsent = "2";
                    } catch (IOException ex) {
                    }

                }
            } else if (f1) {
                Log.d(TAG, "F1");
                if (prevsent.equals("3")) {

                } else {
                    try {
                        sendData2("3");
                        prevsent = "3";
                    } catch (IOException ex) {
                    }
                }
            } else if (bf) {
                Log.d(TAG, "BF");
                if (prevsent.equals("4")) {

                } else {
                    try {
                        sendData2("4");
                        prevsent = "4";
                    } catch (IOException ex) {
                    }
                }
            } else if (sf) {
                Log.d(TAG, "SF");
                if (prevsent.equals("7")) {

                } else {
                    try {
                        sendData2("7");
                        prevsent = "7";
                    } catch (IOException ex) {
                    }
                }
            } else {
                Log.d(TAG, "Nothing");
                if (prevsent.equals("1")) {

                } else {
                    try {
                        sendData2("1");
                        prevsent = "1";
                    } catch (IOException ex) {
                    }
                }
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY));
        currentZ.setText(Float.toString(deltaZ));
    }

}
