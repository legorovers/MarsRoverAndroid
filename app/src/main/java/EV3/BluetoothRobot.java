package EV3;

import android.graphics.Color;
import android.graphics.PointF;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by joecollenette on 02/07/2015.
 */
public class BluetoothRobot implements Runnable
{
	public enum ConnectStatus {CONNECTED, DISCONNECTED, CONNECTING, DISCONNECTING}
	
	public enum BeliefStates {WATER, OBSTACLE, PATH}

	public enum RobotMode {MANUAL, AVOID, WATER, LINE}

	public enum RobotAction
	{
		NOTHING(0),
		FORWARD(1),
		FORWARD_A_BIT(2),
		STOP(3),
		BACK_A_BIT(4),
		BACKWORD(5),
		LEFT(6),
		LEFT_A_BIT(7),
		RIGHT(8),
		RIGHT_A_BIT(9);

		int value;
		RobotAction(int i)
		{
			value = i;
		}

		public int toInt()
		{
			return value;
		}

		public static RobotAction fromInt(int i)
		{
			for (int j = 0; j < a.length; j++)
			{
				if (a[j].toInt() == i)
				{
					return a[j];
				}
			}
			return NOTHING;
		}
	}


	private static RobotAction[] a = RobotAction.values();
	
	public static class BeliefSet
	{
		public float distance;
		public int colour;
		public ArrayList<BeliefStates> states = new ArrayList<BeliefStates>();
	}

	public static class RobotRule
	{
		private boolean on;
		private RobotAction[] actions;
		private int onAppeared;

		public RobotRule()
		{
			on = false;
			onAppeared = 0;
			actions = new RobotAction[]{RobotAction.NOTHING, RobotAction.NOTHING, RobotAction.NOTHING};
		}
		
		public RobotRule(boolean _on, int appeared, RobotAction action1, RobotAction action2, RobotAction action3)
		{
			on = _on;
			onAppeared = appeared;
			actions = new RobotAction[]{action1, action2, action3};
		}

		public boolean getEnabled()
		{
			return on;
		}

		public int getOnAppeared()
		{
			return onAppeared;
		}

		public RobotAction getAction(int pos)
		{
			return actions[pos];
		}
	}

    private Robot robot;
    private Exception generatedException;
	private String btAddress;
	private LinkedBlockingDeque<Pair<RobotAction, Long>> actions;
	private ConnectStatus status = ConnectStatus.DISCONNECTED;

	private RobotRule[] rules;
	private RobotMode mode;
	private boolean running;
	
	private BeliefSet state;
	private boolean obstacleChanged;
	private boolean pathChanged;
	private boolean waterChanged;
	private boolean pathFound;
	private boolean beliefsUpdated;

	private float objectDetected = 0.4f;
	private int blackMax = 50;
	private int waterMax = 100;
	private Random rTurn = new Random();
	//private float pathLight = 0.09f;
	//private PointF waterLightRange = new PointF(0.06f, 0.09f);
	private int speed = 10;
	private long delay = 0;
	private long untilAction = 0;

	private void updateBeliefs(float distance, int colour)
	{
		boolean curObs = state.states.contains(BeliefStates.OBSTACLE);
		if (Float.compare(distance, objectDetected) < 0)
		{
			if (!state.states.contains(BeliefStates.OBSTACLE))
			{
				state.states.add(BeliefStates.OBSTACLE);
			}
		}
		else
		{
			if (state.states.contains(BeliefStates.OBSTACLE))
			{
				state.states.remove(BeliefStates.OBSTACLE);
			}
		}
		obstacleChanged = curObs != state.states.contains(BeliefStates.OBSTACLE);
		int red = Color.red(colour);
		int blue = Color.blue(colour);
		int green = Color.green(colour);

		//pathChanged = state.states.contains(BeliefStates.PATH) != (Float.compare(light, pathLight) < 0);
		boolean curPath = state.states.contains(BeliefStates.PATH);
		if ((red < blackMax) && (blue < blackMax) && (green < blackMax))
		{
			if (!state.states.contains(BeliefStates.PATH))
			{
				state.states.add(BeliefStates.PATH);
			}
		}
		else
		{
			if (state.states.contains(BeliefStates.PATH))
			{
				state.states.remove(BeliefStates.PATH);
			}
		}
		pathChanged = curPath != state.states.contains(BeliefStates.PATH);

		//waterChanged = state.states.contains(BeliefStates.WATER) != ((Float.compare(light, waterLightRange.x) > 0) && (Float.compare(light, waterLightRange.y) < 0));
		boolean curWater = state.states.contains(BeliefStates.WATER);
		if (((blue > green) && (blue > red)) && ((red < waterMax) && (blue < waterMax) && (green < waterMax)))
		{
			if (!state.states.contains(BeliefStates.WATER))
			{
				state.states.add(BeliefStates.WATER);
			}
		}
		else
		{
			if (state.states.contains(BeliefStates.WATER))
			{
				state.states.remove(BeliefStates.WATER);
			}
		}
		waterChanged = curWater != state.states.contains(BeliefStates.WATER);

	}

	private void checkRules()
	{
		for (int i = 0; i < rules.length; i++)
		{
			RobotRule rule = rules[i];
			boolean doActions = false;
			if (rule.getEnabled())
			{
				switch (rule.getOnAppeared())
				{
					case 0:
						doActions = obstacleChanged && state.states.contains(BeliefStates.OBSTACLE);
						break;
					case 1:
						doActions = obstacleChanged && !state.states.contains(BeliefStates.OBSTACLE);
						break;
				}
				if (doActions)
				{
					for (int j = rule.actions.length - 1; j >= 0; j--)
					{
						actions.addFirst(new Pair<RobotAction, Long>(rule.getAction(j), SystemClock.uptimeMillis() + delay));
					}
				}
			}
		}
	}

	private void doAction()
	{
		if (actions.peek() != null)
		{
			Pair<RobotAction, Long> actionPair = actions.peek();
			if ((actionPair.second + delay > SystemClock.uptimeMillis() && status != ConnectStatus.DISCONNECTING))
			{
				untilAction = (actionPair.second + delay) - SystemClock.uptimeMillis();
			}
			else
			{
				actionPair = actions.poll();
				untilAction = 0;
				if (status == ConnectStatus.CONNECTED)
				{
					switch (actionPair.first)
					{
						case FORWARD:
							robot.forward();
							break;
						case FORWARD_A_BIT:
							robot.short_forward();
							break;
						case STOP:
							robot.stop();
							break;
						case BACK_A_BIT:
							robot.short_backward();
							break;
						case BACKWORD:
							robot.backward();
							break;
						case LEFT:
							robot.left();
							break;
						case LEFT_A_BIT:
							robot.short_left();
							break;
						case RIGHT:
							robot.right();
							break;
						case RIGHT_A_BIT:
							robot.short_right();
							break;
					}
				}
			}
		}
	}

	private void doAvoid()
	{
		if (running)
		{
			if (state.states.contains(BeliefStates.OBSTACLE))
			{
				robot.stop();
				robot.short_left();
			}
			else
			{
				if (!robot.isMoving())
				{
					robot.forward();
				}
			}
		}
		else
		{
			robot.stop();
		}
	}

	private void doLine()
	{
		if (running)
		{
			if (!state.states.contains(BeliefStates.PATH))
			{
				robot.forward_right();
			}
			else
			{
				robot.forward_left();
			}
		}
		else
		{
			robot.stop();
		}
	}

	private void doWater()
	{
		if (running)
		{
			if (!state.states.contains(BeliefStates.WATER))
			{
				if (!pathFound && state.states.contains(BeliefStates.PATH))
				{
					pathFound = true;
				}
				else if (pathFound)
				{
					doLine();
				}
				else if (!state.states.contains(BeliefStates.OBSTACLE))
				{
					robot.forward();
				}
				else
				{
					robot.turn(45 + rTurn.nextInt(135));
				}
			}
			else
			{
				running = false;
				robot.stop();
			}
		}
		else
		{
			robot.stop();
		}
	}

    @Override
    public void run()
    {
        try
        {
			generatedException = null;
			status = ConnectStatus.CONNECTING;
			robot.connectToRobot(btAddress);
			status = ConnectStatus.CONNECTED;
			float disInput;
			int curSpeed = speed;
			while (status == ConnectStatus.CONNECTED)
			{
				beliefsUpdated = false;
				disInput = robot.getuSensor().getSample();
				float[] rgb = robot.getRGBSensor().getRGBSample();
				//Log.w("Colour Values", "R - " + (int) (rgb[0] * 850) + " G - " + (int) (rgb[1] * 1026) + " B - " + (int) (rgb[2] * 1815));
				state.colour = Color.rgb((int)(rgb[0] * 850), (int)(rgb[1] * 1026), (int)(rgb[2] * 1815));
				state.distance = disInput;
				updateBeliefs(disInput, state.colour);
				beliefsUpdated = true;
				if (curSpeed != speed)
				{
					robot.setTravelSpeed(speed);
					curSpeed = speed;
				}

				switch(mode)
				{
					case MANUAL:
						checkRules();
						doAction();
						break;
					case LINE:
						doLine();
						break;
					case AVOID:
						doAvoid();
						break;
					case WATER:
						doWater();
						break;
				}
			}

			state.states.clear();
			state.colour = 0;
			state.distance = 0;
			robot.close();
			status = ConnectStatus.DISCONNECTED;

        }
        catch (Exception e)
        {
			status = ConnectStatus.DISCONNECTING;
			if (robot != null && robot.isConnected())
			{
				robot.close();
			}
			status = ConnectStatus.DISCONNECTED;
            generatedException = e;
        }
    }

	public BluetoothRobot()
	{
		actions = new LinkedBlockingDeque<Pair<RobotAction, Long>>();

		rules = new RobotRule[]{
				new RobotRule(),
				new RobotRule(),
				new RobotRule()
		};
		
		state = new BeliefSet();
		robot = new Robot();
		mode = RobotMode.MANUAL;
		running = false;
	}

	public void addAction(RobotAction action)
	{
		actions.add(new Pair<RobotAction, Long>(action, SystemClock.uptimeMillis()));
	}

	public RobotRule[] getAllRules()
	{
		return rules;
	}

    public Exception getGeneratedException()
    {
        return generatedException;
    }

	public void setBTAddress(String address)
	{
		btAddress = address;
	}

	public ConnectStatus connectionStatus()
	{
		return status;
	}

	public void disconnect()
	{
		setRunning(false);
		status = ConnectStatus.DISCONNECTING;
	}

	public void close()
	{
		if (robot != null)
		{
			robot.close();
		}
	}

	public String getMessages()
	{
		if (robot != null)
		{
			return robot.getMessages();
		}
		else
		{
			return "No Current Messages";
		}
	}

	public void changeSettings(float objectRange, int blackMaximum, int waterMaximum)
	{
		objectDetected = objectRange;
		blackMax = blackMaximum;
		waterMax = waterMaximum;
		//waterLightRange = new PointF(waterLower, waterUpper);
		//pathLight = pathRange;
	}

	public void changedRule(int pos, RobotRule rule)
	{
		rules[pos] = rule;
	}

	public BeliefSet getBeliefSet()
	{
		while (!beliefsUpdated && status == ConnectStatus.CONNECTED)
		{
			//Wait til beliefs are valid for the current run
		}
		return state;
	}

	public void setMode(RobotMode _mode)
	{
		running = false;
		pathFound = false;
		mode = _mode;
	}

	public long getTimeToAction()
	{
		return untilAction;
	}

	public void setRunning(boolean _running)
	{
		actions.clear();
		running = _running;
	}

	public void updateManual(long delayMills, int _speed)
	{
		delay = delayMills;
		speed = _speed;
	}

	public boolean getRunning()
	{
		return running;
	}

	public int getColourFound()
	{
		return state.colour;
	}
}
