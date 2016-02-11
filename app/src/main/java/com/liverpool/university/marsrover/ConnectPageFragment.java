package com.liverpool.university.marsrover;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import EV3.BluetoothRobot;


/**
 * Fragment which connects signals the main ui thread to either connect or disconnect
 * A placeholder fragment containing a simple view.
 */
public class ConnectPageFragment extends BaseBluetoothFragment implements View.OnClickListener
{
	private class TextUpdatedListener implements TextWatcher
	{
		private View view;

		public TextUpdatedListener(View _view)
		{
			view = _view;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{

		}

		@Override
		public void afterTextChanged(Editable s)
		{
			SharedPreferences pref = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = pref.edit();
			switch (view.getId())
			{
				case R.id.txtBT1:
					editor.putInt("BT1", Integer.parseInt(s.toString()));
					break;
				case R.id.txtBT2:
					editor.putInt("BT2", Integer.parseInt(s.toString()));
					break;
				case R.id.txtBT3:
					editor.putInt("BT3", Integer.parseInt(s.toString()));
					break;
				case R.id.txtBT4:
					editor.putInt("BT4", Integer.parseInt(s.toString()));
					break;
			}
			editor.commit();
		}
	}

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
				}
				else
				{
					((Button)getView().findViewById(R.id.cmdConnect)).setText("Connect");
					((TextView)getView().findViewById(R.id.txtMessages)).append("\n Disconnected");
				}
				getView().findViewById(R.id.cmdConnect).setEnabled(true);
				connectHandler.removeCallbacks(getConStatus);
			}
		}
	};

	private Handler connectHandler;

	public ConnectPageFragment()
	{

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		connectHandler = new Handler();
		SharedPreferences prefs = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
		connectHandler = new Handler();
		View view = inflater.inflate(R.layout.fragment_connect_page, container, false);

		((TextView)view.findViewById(R.id.txtBT1)).setText(Integer.toString(prefs.getInt("BT1", 10)));
		((TextView)view.findViewById(R.id.txtBT1)).addTextChangedListener(new TextUpdatedListener(view.findViewById(R.id.txtBT1)));

		((TextView)view.findViewById(R.id.txtBT2)).setText(Integer.toString(prefs.getInt("BT2", 0)));
		((TextView)view.findViewById(R.id.txtBT2)).addTextChangedListener(new TextUpdatedListener(view.findViewById(R.id.txtBT2)));

		((TextView)view.findViewById(R.id.txtBT3)).setText(Integer.toString(prefs.getInt("BT3", 1)));
		((TextView)view.findViewById(R.id.txtBT3)).addTextChangedListener(new TextUpdatedListener(view.findViewById(R.id.txtBT3)));

		((TextView)view.findViewById(R.id.txtBT4)).setText(Integer.toString(prefs.getInt("BT4", 1)));
		((TextView)view.findViewById(R.id.txtBT4)).addTextChangedListener(new TextUpdatedListener(view.findViewById(R.id.txtBT4)));

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
		return view;
	}

	@Override
	public void onClick(View v)
	{
		ConnectivityManager conMan = (ConnectivityManager)getActivity().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = conMan.getActiveNetworkInfo();
		if (info != null && info.getType() == ConnectivityManager.TYPE_BLUETOOTH)
		{
			btEvents.setConnection(btEvents.getConnectionStatus() != BluetoothRobot.ConnectStatus.CONNECTED);
			getView().findViewById(R.id.cmdConnect).setEnabled(false);
			connectHandler.postDelayed(getConStatus, 100);
		}
		else
		{
			((TextView)getView().findViewById(R.id.txtMessages)).setText("You must be connected to the robot via bluetooth. WiFi and mobile networks must be switched off");
		}

	}

	
}
