package com.liverpool.university.marsrovergoal;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;


import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Iterator;


import ail.syntax.Literal;
import ail.syntax.Goal;

import EV3.BluetoothRobot;


/**
 * Fragement which allows movement of the EV3
 *
 * Created by joecollenette on 02/07/2015.
 */
public class NavigationPageFrag extends BaseBluetoothFragment implements AdapterView.OnItemSelectedListener
{
	//Thread which updates UI with newest beliefs
	private Runnable getBeliefSet = new Runnable()
	{
		long time;
		@Override
		public void run()
		{
			if (getView() != null)
			{
				BluetoothRobot.BeliefSet currentBeliefs = btEvents.getBelief();
				// List<Literal> currentBeliefs = BluetoothRobot.getBeliefs();

				((TextView) getView().findViewById(R.id.txtNDistance)).setText(String.format("Distance - %f", currentBeliefs.distance));
				((TextView) getView().findViewById(R.id.txtNColour)).setText(String.format("RGB - %d %d %d", Color.red(currentBeliefs.colour)
						, Color.green(currentBeliefs.colour)
						, Color.blue(currentBeliefs.colour)));

				beliefSet.setLength(0);
				beliefSet.append("Beliefs - [");
				boolean start = true;
				for (Literal l: getReasoningEngine().getBB().getAll())
				{
					if (!start)
					{
						beliefSet.append(", ");
					} else {
						start = false;
					}
					beliefSet.append(l.toString());
				}
				beliefSet.append("]");

                // BluetoothRobot.GoalSet currentGoals = btEvents.getGoals();
                goalSet.setLength(0);
                goalSet.append("Goals - [");
                Iterator<Goal> goals = getReasoningEngine().getPrintGoals();
                start = true;
                while (goals.hasNext()) {
                    if (!start) {
                        goalSet.append(", ");
                    } else {
                        start = false;
                    }

                    goalSet.append(goals.next().getFunctor());
                }
                goalSet.append("]");
                ((TextView) getView().findViewById(R.id.txtNGoals)).setText(goalSet.toString());


				((TextView) getView().findViewById(R.id.txtNBeliefs)).setText(beliefSet.toString());
				time = btEvents.getTimeTil();
				String formatStr = new SimpleDateFormat("mm:ss", Locale.ENGLISH).format(time);
				((TextView) getView().findViewById(R.id.txtTimeTil)).setText("Time until next action - " + formatStr);


			}
			beliefHandle.postDelayed(getBeliefSet, 100);
		}
	};

	private StringBuilder beliefSet = new StringBuilder();
	private Handler beliefHandle = new Handler();

    private StringBuilder goalSet = new StringBuilder();

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
		View view = inflater.inflate(R.layout.fragment_navigation, container, false);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.delay_items));
		((Spinner)view.findViewById(R.id.cboDelay)).setAdapter(adapter);

		ArrayAdapter<String> sadapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.speed_items));
		((Spinner)view.findViewById(R.id.cboSpeed)).setAdapter(sadapter);


		//Set up events for buttons.
		view.findViewById(R.id.cmdForward).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.FORWARD));
		view.findViewById(R.id.cmdFor_A_Bit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.FORWARD_A_BIT));
		view.findViewById(R.id.cmdStop).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.STOP));
		view.findViewById(R.id.cmdBack_A_Bit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.BACKWARD_A_BIT));
		view.findViewById(R.id.cmdBack).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.BACKWARD));
		view.findViewById(R.id.cmdLeft).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.LEFT));
		view.findViewById(R.id.cmdLeft_A_Bit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.LEFT_A_BIT));
		view.findViewById(R.id.cmdRight_A_Bit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.RIGHT_A_BIT));
		view.findViewById(R.id.cmdRight).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.RIGHT));

		view.findViewById(R.id.cmdTForward).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.FORWARD));
		view.findViewById(R.id.cmdTFABit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.FORWARD_A_BIT));
		view.findViewById(R.id.cmdTStop).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.STOP));
		view.findViewById(R.id.cmdTBABit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.BACKWARD_A_BIT));
		view.findViewById(R.id.cmdTBack).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.BACKWARD));
		view.findViewById(R.id.cmdTLeft).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.LEFT));
		view.findViewById(R.id.cmdTLABit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.LEFT_A_BIT));
		view.findViewById(R.id.cmdTRABit).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.RIGHT_A_BIT));
		view.findViewById(R.id.cmdTRight).setOnClickListener(new ActionClicked(BluetoothRobot.RobotAction.RIGHT));

		//Change between icons and text on buttons
		((CheckBox)view.findViewById(R.id.chkImages)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				if (b) {
					getView().findViewById(R.id.tblIconButtons).setVisibility(View.VISIBLE);
					getView().findViewById(R.id.tblStrButtons).setVisibility(View.GONE);
				} else {
					getView().findViewById(R.id.tblIconButtons).setVisibility(View.GONE);
					getView().findViewById(R.id.tblStrButtons).setVisibility(View.VISIBLE);
				}
			}
		});

		((Spinner)view.findViewById(R.id.cboDelay)).setOnItemSelectedListener(this);
		((Spinner)view.findViewById(R.id.cboSpeed)).setOnItemSelectedListener(this);

		((Spinner)view.findViewById(R.id.cboSpeed)).setSelection(1);
		beliefHandle.postDelayed(getBeliefSet, 100);
		return view;
	}

	//Event for both combo boxes
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
