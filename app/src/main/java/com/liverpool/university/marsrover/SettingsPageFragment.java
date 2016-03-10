package com.liverpool.university.marsrover;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsPageFragment extends BaseBluetoothFragment implements View.OnClickListener
{
	
	public SettingsPageFragment()
	{
		// Required empty public constructor
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
		editor.putInt("wNBMax", Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterNBMax)).getText().toString()));
		editor.commit();

		btEvents.settingsChanged(Float.parseFloat(((TextView) getView().findViewById(R.id.txtObs)).getText().toString())
				, Integer.parseInt(((TextView) getView().findViewById(R.id.txtBlackMax)).getText().toString())
				, Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterMin)).getText().toString())
				, Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterMax)).getText().toString())
				, Integer.parseInt(((TextView) getView().findViewById(R.id.txtWaterNBMax)).getText().toString()));

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
		((TextView)view.findViewById(R.id.txtWaterMin)).setText(String.valueOf(prefs.getInt("wNBMax", 50)));

		return view;
	}
}
