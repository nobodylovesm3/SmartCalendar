package com.nb3.smartcalendar.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.nb3.smartcalendar.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class Bluetooth extends Testing{

    Button btnOn;
    Button btnOff;
    Button btnBack;

    TextView Display;
    Handler h;
    //TextView txtArduino;


    // Bluetooth connection code
    final int RECEIVE_MESSAGE = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    private ConnectedThread mConnectedThread;
    private static final String TAG = "bluetooth2";

    // SPP UUID Service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC Access of Bluetooth module
    // IMPORTANT: You will need to change the address as each bluetooth module has a different MAC ADDRESS"
    private static String address = "98:D3:35:00:A2:BF";

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relay_switch);

        btnOn = (Button)findViewById(R.id.buttonOn);
        btnOff = (Button)findViewById(R.id.buttonOff);
        btnBack = (Button)findViewById(R.id.btn_back);

        Display = (TextView)findViewById(R.id.textView);

        //txtArduino = (TextView)findViewById(R.id.Arduinotxt);

        //----------------------------------------------
        // Handler code
        //----------------------------------------------
        h = new Handler()
        {
            public void handleMessage(android.os.Message msg)
            {
                switch(msg.what)
                {
                    case RECEIVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);

                        sb.append(strIncom);                                                // append string
                        int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                        if (endOfLineIndex > 0)
                        {                                            // if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                            sb.delete(0, sb.length());                                      // and clear
                            //txtArduino.setText("Data from Arduino: " + sbprint);            // update TextView
                            btnOff.setEnabled(true);
                            btnOn.setEnabled(true);
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            };
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();


        //--------------------------------------------
        // On and off button press
        //--------------------------------------------
        btnOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Display.setText("On");

                // Send "1" via Bluetooth
                mConnectedThread.write("1");
                // Send Command to the Arduino board
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Display.setText("Off");

                // Send "0" via Bluetooth
                mConnectedThread.write("0");
                // Send Command to the Arduino board
            }
        });

        //----------------------------------------------------
        // Back to previous activity
        // http://stackoverflow.com/questions/4038479/android-go-back-to-previous-activity
        //-----------------------------------------------------
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent myIntent = new Intent(Bluetooth.this, DateSetup.class);
                Bluetooth.this.startActivity(myIntent);
            }
        });

    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        if(Build.VERSION.SDK_INT >= 10)
        {
            try
            {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return(BluetoothSocket) m.invoke(device, MY_UUID);
            }
            catch (Exception e)
            {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try
        {
            btSocket = createBluetoothSocket(device);
        }
        catch (IOException e)
        {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try
        {
            btSocket.connect();
            Log.d(TAG, "....Connection ok...");
        } catch (IOException e)
        {
            try
            {
                btSocket.close();
            }
            catch (IOException e2)
            {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }


    @Override
    public void onPause()
    {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try
        {
            btSocket.close();
        }
        catch (IOException e2)
        {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }


    private void checkBTState()
    {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null)
        {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output stream
            // Member streams are final
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {}

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true)
            {
                try
                {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    h.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                }
                catch (IOException e)
                {
                    break;
                }
            }
        }

        public void write(String message)
        {
            Log.d(TAG, "... Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();

            try
            {
                mmOutStream.write(msgBuffer);
            }
            catch(IOException e)
            {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }

    }

}