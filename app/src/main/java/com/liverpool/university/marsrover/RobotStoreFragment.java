package com.liverpool.university.marsrover;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import eass.semantics.EASSAgent;

import EV3.BluetoothRobot;
import lejos.hardware.BrickInfo;

/**
 * Holds the bluetooth event and prevents loss of bluetooth connection during destruction of
 * app. (E.G. Rotation Changes)
 *
 * Passes data to the EV3 Connection Thread
 * Created by joecollenette on 02/07/2015.
 */
public class RobotStoreFragment extends Fragment
{
	//Fragment name for retrieval.
	public static final String TAG_NAME = "TAG_ROBOT_FRAG";
	private BluetoothRobot btRobot;
	private Thread robotThread;
	private BrickInfo activeRobot;

	public RobotStoreFragment()
	{
		setRetainInstance(true); //Prevent destruction of this fragment (and its objects)
		btRobot = new BluetoothRobot();
	}

	public void setSettings(float obstacle, int blackMax, int waterMin, int waterMax, int waterRMax, int waterGMax)
	{
		btRobot.changeSettings(obstacle, blackMax, waterMin, waterMax, waterRMax, waterGMax);
	}

	public void setRule(int pos, BluetoothRobot.RobotRule rule)
	{
		btRobot.changedRule(pos, rule);
	}

	public BluetoothRobot.RobotRule getRule(int pos)
	{
		return btRobot.getAllRules()[pos];
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
	}

	public BluetoothRobot.BeliefSet getBeliefSet()
	{
		return btRobot.getBeliefSet();
	}

	public EASSAgent getReasoningEngine() {
		return btRobot.getReasoningEngine();
	}

	public Exception getConnectionException()
	{
		return btRobot.getGeneratedException();
	}

	public String getConnectionMessages()
	{
		return btRobot.getMessages();
	}

	public void setConnection(boolean connection, SharedPreferences prefs)
	{

		if (connection)
		{
			btRobot.setBTAddress(prefs.getString("Robot_Address", "0.0.0.0"));
			robotThread = new Thread(btRobot);
			robotThread.start();
		}
		else
		{
			btRobot.setDisconnecting();
		}
	}

	public void disconnect() {
        if (btRobot.isConnected()) {
            btRobot.disconnect();
        }
	}

	public BluetoothRobot.ConnectStatus getConnectionStatus()
	{
		return btRobot.connectionStatus();
	}

	public void sendAction(BluetoothRobot.RobotAction action, boolean mustprocess)
	{
		btRobot.addAction(action, mustprocess);
	}

	public void setMode(BluetoothRobot.RobotMode mode)
	{
		btRobot.setMode(mode);
	}

	public void setRunning(boolean running)
	{
		btRobot.setRunning(running);
	}

	public void updateNavSettings(long delayMills, int speed)
	{
		btRobot.updateManual(delayMills, speed);
	}

	public boolean getRunning()
	{
		return btRobot.getRunning();
	}

	public long getTimeUntil()
	{
		return btRobot.getTimeToAction();
	}

	public int getFoundColour()
	{
		return btRobot.getColourFound();
	}

	public void setChanged(boolean education_set) {
		btRobot.setChanged(education_set);
	}

	public void setActiveRobot(BrickInfo active)
	{
		activeRobot = active;
	}

	public BrickInfo getActiveRobot()
	{
		return activeRobot;
	}
}
