package com.liverpool.university.marsrover;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import Adapters.MarsListAdapter;
import EV3.BluetoothRobot;


public class MainPage extends AppCompatActivity implements BaseBluetoothFragment.BTEvents
{

	private ListView drawerList;
	private MarsListAdapter drawerAdapter;

	private ActionBarDrawerToggle drawerToggle;
	private DrawerLayout drawerLayout;
	private String title;

	private ConnectPageFragment connectPage;
	private NavigationPageFrag navigationPage;
	private SettingsPageFragment settingsPage;
	private RulesPageFragment rulesPage;
	private RobotStoreFragment robotStore;
	private AboutPageFragment aboutPage;
	private TaskPageFragment taskPage;
	private int pageNo;

	private void addDrawerItems()
	{
		ArrayList<Pair<String, Drawable>> items = new ArrayList<Pair<String, Drawable>>();
		items.add(new Pair<String, Drawable>("Connect", getResources().getDrawable(R.drawable.connect)));
		items.add(new Pair<String, Drawable>("Navigation", getResources().getDrawable(R.drawable.location)));
		items.add(new Pair<String, Drawable>("Rules", getResources().getDrawable(R.drawable.rules)));
		items.add(new Pair<String, Drawable>("Tasks", getResources().getDrawable(R.drawable.tasks)));
		items.add(new Pair<String, Drawable>("Settings", getResources().getDrawable(R.drawable.settings)));
		items.add(new Pair<String, Drawable>("About", getResources().getDrawable(R.drawable.about)));

		drawerAdapter = new MarsListAdapter(this, items);
		drawerList.setAdapter(drawerAdapter);
	}

	private void changeView(int pos)
	{
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		switch(drawerAdapter.getItem(pos).first)
		{
			case "Settings":
				ft = ft.replace(R.id.mainLayout, settingsPage);
				break;
			case "Navigation":
				robotStore.setMode(BluetoothRobot.RobotMode.MANUAL);
				ft = ft.replace(R.id.mainLayout, navigationPage);
				break;
			case "Connect":
				ft = ft.replace(R.id.mainLayout, connectPage);
				break;
			case "Rules":
				ft = ft.replace(R.id.mainLayout, rulesPage);
				break;
			case "Tasks":
				ft = ft.replace(R.id.mainLayout, taskPage);
				break;
			case "About":
				ft = ft.replace(R.id.mainLayout, aboutPage);
				break;
		}
		ft.commit();
		pageNo = pos;
		getSupportActionBar().setTitle(title + " - " + drawerAdapter.getItem(pageNo).first);
		drawerLayout.closeDrawers();
	}

	private void setupDrawer()
	{
		drawerToggle = new ActionBarDrawerToggle(this,drawerLayout,
				R.string.drawer_open, R.string.drawer_close)
		{

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView)
			{
				super.onDrawerOpened(drawerView);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view)
			{
				super.onDrawerClosed(view);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};
		drawerToggle.setDrawerIndicatorEnabled(true);
		drawerLayout.setDrawerListener(drawerToggle);
		drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				changeView(position);
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null)
		{
			pageNo = savedInstanceState.getInt("pageNo");
		}
		else
		{
			pageNo = 0;
		}
		setContentView(R.layout.activity_main);
		drawerList = (ListView)findViewById(R.id.navList);
		addDrawerItems();

		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
		title = getTitle().toString();

		setupDrawer();

		connectPage = new ConnectPageFragment();
		navigationPage = new NavigationPageFrag();
		settingsPage = new SettingsPageFragment();
		rulesPage = new RulesPageFragment();
		aboutPage = new AboutPageFragment();
		taskPage = new TaskPageFragment();

		FragmentManager fm = getSupportFragmentManager();
		robotStore = (RobotStoreFragment)fm.findFragmentByTag(RobotStoreFragment.TAG_NAME);

		if (robotStore == null)
		{
			robotStore = new RobotStoreFragment();
		}
		changeView(pageNo);
		drawerLayout.closeDrawers();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onSaveInstanceState(Bundle bundle)
	{
		bundle.putInt("pageNo", pageNo);
		super.onSaveInstanceState(bundle);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (drawerToggle.onOptionsItemSelected(item))
		{
			return true;
		}

		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void settingsChanged(float obstacle, int blackMax, int waterMax)
	{
		robotStore.setSettings(obstacle, blackMax, waterMax);
	}

	@Override
	public void ruleChanged(int pos, BluetoothRobot.RobotRule rule)
	{
		robotStore.setRule(pos, rule);
	}

	@Override
	public int getFoundColour()
	{
		return robotStore.getFoundColour();
	}

	@Override
	public BluetoothRobot.RobotRule getRule(int pos)
	{
		return robotStore.getRule(pos);
	}

	@Override
	public BluetoothRobot.BeliefSet getBelief()
	{
		return robotStore.getBeliefSet();
	}

	@Override
	public BluetoothRobot.ConnectStatus getConnectionStatus()
	{
		return robotStore.getConnectionStatus();
	}

	@Override
	public void setConnection(boolean doConnect)
	{
		robotStore.setConnection(doConnect, getSharedPreferences("Settings", MODE_PRIVATE));
	}

	@Override
	public String getConnectionMessages()
	{
		return robotStore.getConnectionMessages();
	}

	@Override
	public Exception getConnectionException()
	{
		return robotStore.getConnectionException();
	}

	@Override
	public void sendAction(BluetoothRobot.RobotAction action)
	{
		robotStore.sendAction(action);
	}

	@Override
	public void setMode(BluetoothRobot.RobotMode mode)
	{
		robotStore.setMode(mode);
	}

	@Override
	public void setRunning(boolean running)
	{
		robotStore.setRunning(running);
	}

	@Override
	public void updateNavSettings(long delayMills, int speed)
	{
		robotStore.updateNavSettings(delayMills, speed);
	}

	@Override
	public long getTimeTil()
	{
		return robotStore.getTimeUntil();
	}

	@Override
	public boolean getRunning()
	{
		return robotStore.getRunning();
	}
}