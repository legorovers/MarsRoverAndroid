package com.liverpool.university.marsrover;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import EV3.BluetoothRobot;
import ail.syntax.Literal;


public class SettingsPageFragment extends BaseBluetoothFragment implements View.OnClickListener
{
	
	public SettingsPageFragment()
	{
		// Required empty public constructor
	}

	private StringBuilder beliefSet = new StringBuilder();
	private Handler beliefHandle = new Handler();

	//Thread to update current beliefs
	private Runnable getBeliefSet = new Runnable()
	{
		@Override
		public void run()
		{
			if (getView() != null)
			{
				BluetoothRobot.BeliefSet currentBeliefs = btEvents.getBelief();

				((TextView) getView().findViewById(R.id.txtSDistance)).setText(String.format("Distance - %f", currentBeliefs.distance));
				((TextView) getView().findViewById(R.id.txtSColour)).setText(String.format("RGB - %d %d %d", Color.red(currentBeliefs.colour)
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
				((TextView) getView().findViewById(R.id.txtSBeliefs)).setText(beliefSet.toString());
			}
			beliefHandle.postDelayed(getBeliefSet, 100);
		}
	};

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



	@Override
	public void onClick(View v)
	{
		SharedPreferences prefs = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat("Obstacle", Float.parseFloat(((TextView)getView().findViewById(R.id.txtObs)).getText().toString()));
		editor.putInt("bMax", Integer.parseInt(((TextView) getView().findViewById(R.id.txtBlackMax)).getText().toString()));
		editor.putInt("wMin", Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterMin)).getText().toString()));
		editor.putInt("wMax", Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterMax)).getText().toString()));
		editor.putInt("wRMax", Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterRMax)).getText().toString()));
        editor.putInt("wGMax", Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterGMax)).getText().toString()));
        editor.commit();

		btEvents.settingsChanged(Float.parseFloat(((TextView) getView().findViewById(R.id.txtObs)).getText().toString())
				, Integer.parseInt(((TextView) getView().findViewById(R.id.txtBlackMax)).getText().toString())
				, Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterMin)).getText().toString())
				, Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterMax)).getText().toString())
                , Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterRMax)).getText().toString())
                , Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterGMax)).getText().toString()));

		Toast saved = Toast.makeText(getView().getContext(), "Settings Saved", Toast.LENGTH_SHORT);
		saved.show();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_settings, container, false);
		view.findViewById(R.id.cmdSet).setOnClickListener(this);
		SharedPreferences prefs = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
		((TextView)view.findViewById(R.id.txtObs)).setText(String.valueOf(prefs.getFloat("Obstacle", 0.4f)));
		((TextView)view.findViewById(R.id.txtBlackMax)).setText(String.valueOf(prefs.getInt("bMax", 50)));
		((TextView)view.findViewById(R.id.txtWaterMin)).setText(String.valueOf(prefs.getInt("wMin", 50)));
		((TextView)view.findViewById(R.id.txtWaterMax)).setText(String.valueOf(prefs.getInt("wMax", 100)));
		((TextView)view.findViewById(R.id.txtWaterRMax)).setText(String.valueOf(prefs.getInt("wRMax", 50)));
        ((TextView)view.findViewById(R.id.txtWaterGMax)).setText(String.valueOf(prefs.getInt("wGMax", 50)));

		return view;
	}
}
