package com.liverpool.university.marsrover;

import android.app.Activity;
import android.support.v4.app.Fragment;

import EV3.BluetoothRobot;

/**
 * Created by joecollenette on 30/07/2015.
 */
public class BaseBluetoothFragment extends Fragment
{
	public interface BTEvents
	{
		BluetoothRobot.RobotRule getRule(int pos);
		void ruleChanged(int pos, BluetoothRobot.RobotRule rule);
		int getFoundColour();
		BluetoothRobot.BeliefSet getBelief();
		BluetoothRobot.ConnectStatus getConnectionStatus();
		void setConnection(boolean doConnect);
		String getConnectionMessages();
		Exception getConnectionException();
		void sendAction(BluetoothRobot.RobotAction action);
		void setMode(BluetoothRobot.RobotMode mode);
		void setRunning(boolean running);
		void updateNavSettings(long delayMills, int speed);
		long getTimeTil();
		boolean getRunning();
		void settingsChanged(float obstacle, int blackMax, int waterMax);
	}

	protected BTEvents btEvents;

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		btEvents = (BTEvents)activity;
	}
}
