package com.liverpool.university.marsrover;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.util.TimeUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import EV3.BluetoothRobot;


public class NavigationPageFrag extends BaseBluetoothFragment implements AdapterView.OnItemSelectedListener
{

	private Runnable updateTimeTil = new Runnable()
	{
		long time;

		@Override
		public void run()
		{
			try
			{
				time = btEvents.getTimeTil();
				String formatStr = new SimpleDateFormat("mm:ss", Locale.ENGLISH).format(time);
						((TextView) getView().findViewById(R.id.txtTimeTil)).setText("Time until next action - " + formatStr);
				timeTilHandler.post(updateTimeTil);
			}
			catch (Exception e)
			{

			}
		}
	};

	private class ActionClicked implements View.OnClickListener
	{
		private BluetoothRobot.RobotAction action;

		public ActionClicked(BluetoothRobot.RobotAction _action)
		{
			action = _action;
		}

		@Override
		public void onClick(View v)
		{
			btEvents.sendAction(action);
		}
	};
	
	private Handler timeTilHandler;

	private int getVisibility(boolean vis)
	{
		if (vis)
		{
			return View.VISIBLE;
		}
		else
		{
			return View.GONE;
		}
	}

	public NavigationPageFrag()
	{
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		timeTilHandler = new Handler();
		View view = inflater.inflate(R.layout.fragment_navigation, container, false);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.delay_items));
		((Spinner)view.findViewById(R.id.cboDelay)).setAdapter(adapter);

		ArrayAdapter<String> sadapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.speed_items));
		((Spinner)view.findViewById(R.id.cboSpeed)).setAdapter(sadapter);

		view.findViewById(R.id.cmdForward).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.FORWARD));
		view.findViewById(R.id.cmdFor_A_Bit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.FORWARD_A_BIT));
		view.findViewById(R.id.cmdStop).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.STOP));
		view.findViewById(R.id.cmdBack_A_Bit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.BACK_A_BIT));
		view.findViewById(R.id.cmdBack).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.BACKWORD));
		view.findViewById(R.id.cmdLeft).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.LEFT));
		view.findViewById(R.id.cmdLeft_A_Bit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.LEFT_A_BIT));
		view.findViewById(R.id.cmdRight_A_Bit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.RIGHT_A_BIT));
		view.findViewById(R.id.cmdRight).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.RIGHT));

		((Spinner)view.findViewById(R.id.cboDelay)).setOnItemSelectedListener(this);
		((Spinner)view.findViewById(R.id.cboSpeed)).setOnItemSelectedListener(this);

		((Spinner)view.findViewById(R.id.cboSpeed)).setSelection(1);
		timeTilHandler.postDelayed(updateTimeTil, 100);
		return view;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		long delayMills = 0;
		switch (((Spinner)getView().findViewById(R.id.cboDelay)).getSelectedItemPosition())
		{
			case 0:
				delayMills = 0;
				break;
			case 1:
				delayMills = 500;
				break;
			case 2:
				delayMills = 1300;
				break;
			case 3:
				delayMills = 180000;
				break;
		}

		int speed = (((Spinner)getView().findViewById(R.id.cboSpeed)).getSelectedItemPosition() + 1) * 5;
		btEvents.updateNavSettings(delayMills, speed);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{

	}

}
