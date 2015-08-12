package com.liverpool.university.marsrover;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import EV3.BluetoothRobot;

/**
 * Created by joecollenette on 30/07/2015.
 */
public class TaskPageFragment extends BaseBluetoothFragment implements AdapterView.OnItemSelectedListener, View.OnClickListener
{
	private Runnable getBeliefSet = new Runnable()
	{
		@Override
		public void run()
		{
			if (getView() != null)
			{
				BluetoothRobot.BeliefSet currentBeliefs = btEvents.getBelief();

				((TextView) getView().findViewById(R.id.txtDistance)).setText(String.format("Distance - %f", currentBeliefs.distance));
				((TextView) getView().findViewById(R.id.txtColour)).setText(String.format("RGB - %d %d %d", Color.red(currentBeliefs.colour)
						, Color.green(currentBeliefs.colour)
						, Color.blue(currentBeliefs.colour)));

				beliefSet.setLength(0);
				beliefSet.append("Beliefs - [");
				for (int i = 0; i < currentBeliefs.states.size(); i++)
				{
					if (i > 0)
					{
						beliefSet.append(", ");
					}
					beliefSet.append(currentBeliefs.states.get(i).toString());
				}
				beliefSet.append("]");
				((TextView) getView().findViewById(R.id.txtBeliefs)).setText(beliefSet.toString());
			}
			beliefHandle.postDelayed(getBeliefSet, 100);
		}
	};

	private StringBuilder beliefSet = new StringBuilder();
	private Handler beliefHandle = new Handler();

	private void updateCMD(View view)
	{
		if (!btEvents.getRunning())
		{
			((Button)view.findViewById(R.id.cmdTask)).setText("Start");
		}
		else
		{
			((Button)view.findViewById(R.id.cmdTask)).setText("Stop");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_task_page, container, false);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.task_list));
		((Spinner)view.findViewById(R.id.cboTasks)).setAdapter(adapter);
		((Spinner)view.findViewById(R.id.cboTasks)).setOnItemSelectedListener(this);
		view.findViewById(R.id.cmdTask).setOnClickListener(this);
		updateCMD(view);
		return view;
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		beliefHandle.postDelayed(getBeliefSet, 100);

	}

	@Override
	public void onDetach()
	{
		beliefHandle.removeCallbacks(getBeliefSet);
		super.onDetach();
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
	{
		switch (i)
		{
			case 0:
				btEvents.setMode(BluetoothRobot.RobotMode.AVOID);
				break;
			case 1:
				btEvents.setMode(BluetoothRobot.RobotMode.LINE);
				break;
			case 2:
				btEvents.setMode(BluetoothRobot.RobotMode.WATER);
				break;
		}
		updateCMD(getView());
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView)
	{

	}

	@Override
	public void onClick(View view)
	{
		btEvents.setRunning(!btEvents.getRunning());
		updateCMD(getView());
	}

	public void doUpdates(boolean update)
	{
		if (!update)
		{
			beliefHandle.removeCallbacks(getBeliefSet);
		}
		else
		{
			beliefHandle.post(getBeliefSet);
		}
	}
}
