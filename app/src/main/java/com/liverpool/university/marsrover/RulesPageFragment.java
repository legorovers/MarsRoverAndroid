package com.liverpool.university.marsrover;


import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import EV3.BluetoothRobot;
import ail.syntax.Literal;

public class RulesPageFragment extends BaseBluetoothFragment
{

	//Thread to update current beliefs
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
				((TextView) getView().findViewById(R.id.txtBeliefs)).setText(beliefSet.toString());
			}
			beliefHandle.postDelayed(getBeliefSet, 100);
		}
	};

	private StringBuilder beliefSet = new StringBuilder();
	private Handler beliefHandle = new Handler();

	private class itemSelected implements AdapterView.OnItemSelectedListener
	{

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
		{
			updateRule();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{

		}
	}

	private void updateRule()
	{
		BluetoothRobot.RobotRule rule = new BluetoothRobot.RobotRule(
				((CheckBox)getView().findViewById(R.id.chkRule)).isChecked(),
				BluetoothRobot.BeliefStates.fromInt(((Spinner) getView().findViewById(R.id.cboType)).getSelectedItemPosition()),
				((Spinner) getView().findViewById(R.id.cboObstacle)).getSelectedItemPosition(),
				BluetoothRobot.RobotAction.fromInt(((Spinner) getView().findViewById(R.id.cboAction1)).getSelectedItemPosition()),
				BluetoothRobot.RobotAction.fromInt(((Spinner) getView().findViewById(R.id.cboAction2)).getSelectedItemPosition()),
				BluetoothRobot.RobotAction.fromInt(((Spinner) getView().findViewById(R.id.cboAction3)).getSelectedItemPosition()));
		btEvents.ruleChanged(((Spinner)getView().findViewById(R.id.cboRule)).getSelectedItemPosition(), rule);
	}

	public RulesPageFragment()
	{
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_rules_page, container, false);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.rule_list));
		((Spinner)view.findViewById(R.id.cboRule)).setAdapter(adapter);

		ArrayAdapter<String> tadapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.rule_type));
		((Spinner)view.findViewById(R.id.cboType)).setAdapter(tadapter);

		ArrayAdapter<String> Aadapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.rule_type_options));
		((Spinner)view.findViewById(R.id.cboObstacle)).setAdapter(Aadapter);

		ArrayAdapter<String> A1adapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.rule_options));
		((Spinner)view.findViewById(R.id.cboAction1)).setAdapter(A1adapter);
		ArrayAdapter<String> A2adapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.rule_options));
		((Spinner)view.findViewById(R.id.cboAction2)).setAdapter(A2adapter);
		ArrayAdapter<String> A3adapter = new ArrayAdapter<String>(view.getContext(), R.layout.combo_list_item, R.id.txtView, getResources().getStringArray(R.array.rule_options));
		((Spinner)view.findViewById(R.id.cboAction3)).setAdapter(A3adapter);


		((Spinner)view.findViewById(R.id.cboRule)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				BluetoothRobot.RobotRule rule = btEvents.getRule(position);

				((CheckBox) getView().findViewById(R.id.chkRule)).setChecked(rule.getEnabled());
				((Spinner) getView().findViewById(R.id.cboType)).setSelection(rule.getType().toInt());
				((Spinner) getView().findViewById(R.id.cboObstacle)).setSelection(rule.getOnAppeared());
				((Spinner) getView().findViewById(R.id.cboAction1)).setSelection(rule.getAction(0).toInt());
				((Spinner) getView().findViewById(R.id.cboAction2)).setSelection(rule.getAction(1).toInt());
				((Spinner) getView().findViewById(R.id.cboAction3)).setSelection(rule.getAction(2).toInt());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{

			}
		});

		((CheckBox)view.findViewById(R.id.chkRule)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				String toChange = "Off";
				if (isChecked)
				{
					toChange = "On";
				}
				buttonView.setText(toChange);
				updateRule();
			}
		});

		((Spinner)view.findViewById(R.id.cboType)).setOnItemSelectedListener(new itemSelected());
		((Spinner)view.findViewById(R.id.cboObstacle)).setOnItemSelectedListener(new itemSelected());
		((Spinner)view.findViewById(R.id.cboAction1)).setOnItemSelectedListener(new itemSelected());
		((Spinner)view.findViewById(R.id.cboAction2)).setOnItemSelectedListener(new itemSelected());
		((Spinner)view.findViewById(R.id.cboAction3)).setOnItemSelectedListener(new itemSelected());
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
