package com.liverpool.university.marsrover;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import EV3.BluetoothRobot;
import lejos.hardware.BrickFinder;
import lejos.hardware.BrickInfo;


/**
 * Fragment which connects signals the main ui thread to either connect or disconnect
 * A placeholder fragment containing a simple view.
 */
public class ConnectPageFragment extends BaseBluetoothFragment implements View.OnClickListener
{
	//Update UI based on current connection status
	private Runnable getConStatus = new Runnable()
	{
		@Override
		public void run()
		{
			BluetoothRobot.ConnectStatus status = btEvents.getConnectionStatus();
			if (btEvents.getConnectionException() != null)
			{
				((TextView)getView().findViewById(R.id.txtMessages)).setText(btEvents.getConnectionException().getMessage());
				getView().findViewById(R.id.cmdConnect).setEnabled(true);
				connectHandler.removeCallbacks(getConStatus);
			}
			else if (status == BluetoothRobot.ConnectStatus.CONNECTING || status == BluetoothRobot.ConnectStatus.DISCONNECTING)
			{
				((TextView)getView().findViewById(R.id.txtMessages)).setText(btEvents.getConnectionMessages());
				connectHandler.postDelayed(getConStatus, 100);
			}
			else
			{
				if (status == BluetoothRobot.ConnectStatus.CONNECTED)
				{
					((Button)getView().findViewById(R.id.cmdConnect)).setText("Disconnect");
					((TextView)getView().findViewById(R.id.txtMessages)).append("\n Connected");
					((Spinner)getView().findViewById(R.id.spnRobots)).setEnabled(false);
				}
				else
				{
					((Button)getView().findViewById(R.id.cmdConnect)).setText("Connect");
					((TextView)getView().findViewById(R.id.txtMessages)).append("\n Disconnected");
					((Spinner)getView().findViewById(R.id.spnRobots)).setEnabled(true);
				}
				getView().findViewById(R.id.cmdConnect).setEnabled(true);
				connectHandler.removeCallbacks(getConStatus);
			}
		}
	};

	private Handler connectHandler;
	private BrickInfo[] bricks;

	public ConnectPageFragment()
	{

	}

	//Class to getListOfRobotNames
	private class getRobots extends AsyncTask<Void, Integer, BrickInfo[]>
	{
		@Override
		protected BrickInfo[] doInBackground(Void... voids) {
			return BrickFinder.discover();
		}

		@Override
		protected void onPostExecute(BrickInfo[] brickInfos) {
			bricks = brickInfos;
			if (getView() != null) {
				String[] names = new String[brickInfos.length ];

				for (int i = 0; i < brickInfos.length; i++) {
					names[i] = brickInfos[i].getName();
				}
				ArrayAdapter<String> nameAdapter = new ArrayAdapter<String>(getActivity(), R.layout.combo_list_item, R.id.txtView, names);
				((Spinner) getView().findViewById(R.id.spnRobots)).setAdapter(nameAdapter);
				if (btEvents.getConnectionStatus() == BluetoothRobot.ConnectStatus.CONNECTED)
				{
					String toFind = btEvents.getActiveRobot().getName();
					int position = nameAdapter.getPosition(toFind);
					((Spinner) getView().findViewById(R.id.spnRobots)).setSelection(position);
				}
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		connectHandler = new Handler();
		final SharedPreferences prefs = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
		connectHandler = new Handler();
		View view = inflater.inflate(R.layout.fragment_connect_page, container, false);

		((Spinner)view.findViewById(R.id.spnRobots)).setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.combo_list_item, R.id.txtView, new String[]{"Finding Robots"}));
		((Spinner)view.findViewById(R.id.spnRobots)).setEnabled(btEvents.getConnectionStatus() != BluetoothRobot.ConnectStatus.CONNECTED);

		view.findViewById(R.id.cmdConnect).setOnClickListener(this);
		if (btEvents.getConnectionStatus() == BluetoothRobot.ConnectStatus.CONNECTED)
		{
			((Button)view.findViewById(R.id.cmdConnect)).setText("Disconnect");
		}
		else
		{
			((Button)view.findViewById(R.id.cmdConnect)).setText("Connect");
		}


		((CheckBox)view.findViewById(R.id.checkBoxSet)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				if (b) {
					btEvents.setChanged(true);
				} else {
					btEvents.setChanged(false);
				}
			}
		});
		new getRobots().execute();
		((Spinner)view.findViewById(R.id.spnRobots)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
				if (bricks != null && bricks.length > 0) {
					btEvents.setActiveRobot(bricks[i]);
					SharedPreferences.Editor changePref = prefs.edit();
					changePref.putString("Robot_Address", bricks[i].getIPAddress());
					changePref.commit();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {

			}
		});
		return view;
	}

	@Override
	public void onClick(View v)
	{
		ConnectivityManager conMan = (ConnectivityManager)getActivity().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = conMan.getActiveNetworkInfo();
		if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI)
		{
			btEvents.setConnection(btEvents.getConnectionStatus() != BluetoothRobot.ConnectStatus.CONNECTED);
			getView().findViewById(R.id.cmdConnect).setEnabled(false);
			connectHandler.postDelayed(getConStatus, 100);
		}
		else
		{
			((TextView)getView().findViewById(R.id.txtMessages)).setText("You must be connected to the robot via WiFi");
		}

	}

	
}
