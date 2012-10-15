package com.tophathacker.BluetoothOBD;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import android.os.Bundle;
import android.os.PowerManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	EditText txtInput;
	EditText txtConsole;
	
	//Bluetooth stuff
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // ==> hard coded device's MAC address <==
    private String address = "00:19:5D:26:B1:CC";
    //end Bluetooth stuff
    
    //for wakelock
	PowerManager pm;
	PowerManager.WakeLock wl;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //for wakelock
        pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
        
        
        setContentView(R.layout.activity_main);
        txtInput = (EditText) findViewById(R.id.txtInput);
        txtConsole = (EditText) findViewById(R.id.txtConsole);
        
        txtInput.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
            	if(s.toString().contains("\n"))
            	{
            		if(outStream!=null)
            		{
            			String str = txtInput.getText() + "\r";
            			try {
            				outStream.flush();
							outStream.write(str.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
            		}
            		txtInput.setText("");
            		readToBreak();
            	}
            }

			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
			}

			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
			}
        });
        

    	createBluetooth();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void createBluetooth()
    {
    	bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
                Toast.makeText(this,"Bluetooth is not available.",Toast.LENGTH_LONG).show();
                //finish();
                return;
        }
        if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this,"Please enable your BT and re-run this program.",Toast.LENGTH_LONG).show();
                //finish();
                return;
        }
    }
    
    
    //this is run even the first time the program starts.
    @Override
    public void onResume() {
            super.onResume();
            
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

            try
            {
            	btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            	//do nothing?                    
            }
            
            //had to turn off discovery... had some issues
            //       should turn back on after the program is done?
            bluetoothAdapter.cancelDiscovery();

            try
            {
            	btSocket.connect();
            	wl.acquire();
            	Toast.makeText(this, "Adapter connected", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
            	Toast.makeText(this,"Adapter is not available.",Toast.LENGTH_LONG).show();
                try
                {
                	btSocket.close();
                } catch (IOException e2) {
                	//do nothing ?
                }
            }

            // Create a data stream so we can talk to device
            
            try
            {
            	outStream = btSocket.getOutputStream();
            	inStream = btSocket.getInputStream();
            } catch (IOException e) {
            	//do nothing
            }
    }

    @Override
    public void onPause() {
            super.onPause();
            wl.release();
            //Bluetooth goes to sleep if you leave the program
            if (outStream != null) {
                try
                {
                    outStream.flush();
                    inStream.close();
                    outStream.close();
                } catch (IOException e) {
                	//
                }
            }

            try
            {
            	btSocket.close();
            } catch (IOException e2) {
                // do nothing
            }
            
            //this is to exit if you pause (minimize) the window
            // remove if you want to leave it run
            finish();
    }
    
	@TargetApi(9)
	public void readToBreak() {
		Charset charset = Charset.forName("UTF-8");
		if(inStream!=null)  // not sure if this is needed.. was trying something, might be useless.
		{
			boolean keepGoing = true;
    		while(keepGoing)
			{
		    	try {
	    			byte[] buffer = new byte[inStream.available()];
	    			inStream.read(buffer);
					txtConsole.getText().append(new String(buffer, charset));
	    			if(buffer.length>0)
		    			if(buffer[buffer.length-1]=='>')
		    				keepGoing = false; //got the last char, break loop
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.v("OBD", "ReadError");
					e.printStackTrace();
				}
			}
			try {
				inStream.reset();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
