package EV3;

import android.os.SystemClock;

import lejos.remote.ev3.RemoteRequestEV3;
import lejos.remote.ev3.RemoteRequestPilot;
import lejos.remote.ev3.RemoteRequestRegulatedMotor;
import lejos.robotics.RegulatedMotor;

public class Robot extends BasicRobot
{
    RemoteRequestRegulatedMotor motorL;
    RemoteRequestRegulatedMotor motorR;
    RemoteRequestRegulatedMotor motor;
    RemoteRequestPilot pilot;

	EASSInfraRedSensor irSensor;
    EASSUltrasonicSensor uSensor;
	EASSRGBColorSensor cSensor;

    private StringBuilder messages;

    private boolean closed = false;
    private boolean straight = false;

    int distance_port = 2;
    int color_port = 3;

    int slow_turn = 70;
    int fast_turn = 80;
    int travel_speed = 10;

    boolean home_edition = false;

    public Robot()
    {
		messages = new StringBuilder();
    }

    public void connectToRobot(String address) throws Exception
    {
		messages.setLength(0);

        connect(address);
		closed = false;
        RemoteRequestEV3 brick = getBrick();


        String distance_portstring = "S" + distance_port;
        String color_portstring = "S" + color_port;

        try {
            if (home_edition) {
                messages.append("Connecting to Infra Red Sensor" + '\n');
                irSensor = new EASSInfraRedSensor(brick, distance_portstring);
                messages.append("Connected to Sensor " + '\n');
                setSensor(distance_port, irSensor);
            } else {
                messages.append("Connecting to Ultrasonic Sensor" + '\n');
                uSensor = new EASSUltrasonicSensor(brick, distance_portstring);
                messages.append("Connected to Sensor " + '\n');
                setSensor(distance_port, uSensor);
            }
        } catch (Exception e) {
            brick.disConnect();
            throw e;
        }


        try {
            messages.append("Connecting to Colour Sensor " + '\n');
            cSensor = new EASSRGBColorSensor(brick, color_portstring);
            messages.append("Connected to Sensor " + '\n');
            setSensor(color_port, cSensor);
        } catch (Exception e) {
            uSensor.close();
            brick.disConnect();
            throw e;
        }

        try {
            messages.append("Creating Pilot " + '\n');
            // Creating motors as well as pilot in order to allow turning on the spot.
            motorR = (RemoteRequestRegulatedMotor) brick.createRegulatedMotor("B", 'L');
            motorL = (RemoteRequestRegulatedMotor) brick.createRegulatedMotor("C", 'L');
            motorR.setSpeed(200);
            motorL.setSpeed(200);
            if (home_edition) {
                pilot = (RemoteRequestPilot) brick.createPilot(4, 9, "C", "B " + '\n');
            } else {
                pilot = (RemoteRequestPilot) brick.createPilot(6, 10, "C", "B" + '\n');
            }
            messages.append("Created Pilot " + '\n');
        } catch (Exception e) {
            if (home_edition) {
                irSensor.close();
            } else {
                uSensor.close();
            }
            cSensor.close();
            brick.disConnect();
            throw e;
        }

        /* try {
            messages.append("Contacting Medium Motor " + '\n');
            motor = (RemoteRequestRegulatedMotor) brick.createRegulatedMotor("A", 'M');
            messages.append("Created Medium Motor " + '\n');
        } catch (Exception e) {
            //uSensor.close();
            //cSensor.close();
            motorR.close();
            motorL.close();
            brick.disConnect();
            throw e;
        } */
    }

    public void close() {
		messages.setLength(0);
        if (! closed) {
            super.disconnected = true;
            try {
                motor.stop();
				//messages.append("   Closing Jaw Motor " + '\n');
				motor.close();

                SystemClock.sleep(10);
				motorR.stop();
				motorL.stop();
				messages.append("   Closing Right Motor " + '\n');
				motorR.close();

                SystemClock.sleep(10);
				messages.append("   Closing Left Motor " + '\n');
				motorL.close();

                SystemClock.sleep(10);
                pilot.stop();
                messages.append("   Closing Pilot " + '\n');
                pilot.close();
                SystemClock.sleep(10);
            } catch (Exception e) {

            }
            messages.append("   Closing Remaining Sensors" + '\n');
			super.close();
        }
        closed = true;
    }


    /**
     * Get the medium motor that control the dinosaur's jaws.
     * @return
     */
    public RegulatedMotor getMotor() {
        return motor;
    }

    public void setTravelSpeed(int travelSpeed)
	{
		travel_speed = travelSpeed;
	}

    /**
     * Move forward
     */
    public void forward() {
        pilot.setLinearSpeed(travel_speed);
        if (!straight) {
            straight = true;
        }

        pilot.forward();
    }

    /**
     * Move forward a short distance.
     */
    public void short_forward() {
        pilot.setLinearSpeed(travel_speed);
        if (!straight) {
            straight = true;
        }

        pilot.travel(10);
    }


    /**
     * Move backward
     */
    public void backward() {
        pilot.setLinearSpeed(travel_speed);
        if (!straight) {
            straight = true;
        }
        pilot.backward();
    }

    /**
     * Move backward a short distance.
     */
    public void short_backward() {
        pilot.setLinearSpeed(travel_speed);
        if (!straight) {
            straight = true;
        }
        pilot.travel(-10);
    }

    /**
     * Stop.
     */
    public void stop() {
        pilot.stop();
    }

    /**
     * Turn left on the spot.
     */
    public void left() {
        motorR.setSpeed(fast_turn);
        motorL.setSpeed(fast_turn);
        motorR.backward();
        motorL.forward();
        straight = false;
    }

    /**
     * Turn left through an angle (approx 90 on the wheeled robots).
     */
    public void short_left() {
        pilot.setAngularSpeed(travel_speed);
        pilot.rotate(-90);
        straight = false;
    }

    /**
     * Turn left through an angle (approx 30 on the wheeled robots).
     */
    public void very_short_left() {
        pilot.setAngularSpeed(travel_speed);
        pilot.rotate(-30);
        straight = false;
    }

    /**
     * Move left around stopped wheel.
     */
    public void forward_left() {
        motorL.setSpeed(slow_turn);
        motorL.forward();
        motorR.stop();
        straight = false;
    }

    /**
     * Turn right on the spot.
     */
    public void right() {
        motorR.setSpeed(fast_turn);
        motorL.setSpeed(fast_turn);
        motorR.forward();
        motorL.backward();
        straight = false;
    }

    /**
     * Turn a short distance right (approx 90 on a wheeled robot)
     */
    public void short_right() {
        pilot.setAngularSpeed(travel_speed);
        pilot.rotate(90);
        straight = false;
    }

    /**
     * Turn a short distance right (approx 30 on a wheeled robot)
     */
    public void very_short_right() {
        pilot.setAngularSpeed(travel_speed);
        pilot.rotate(30);
        straight = false;
    }



    /**
     * Turn right around stopped left whell.
     */
    public void forward_right() {
        motorR.setSpeed(slow_turn);
        motorR.forward();
        motorL.stop();
        straight = false;
    }

    /**
     * Snap jaws to scare something.
     */
    public void scare() {
        int pos = motor.getTachoCount();
        motor.rotateTo(pos + 20);
        motor.waitComplete();
        motor.rotateTo(pos);
        motor.waitComplete();
        motor.rotateTo(pos + 20);
        motor.waitComplete();
        motor.rotateTo(pos);
        motor.waitComplete();
    }

    /**
     * Rotate right motor some angle.
     * @param d
     */
    public void turn(int d) {
        motorR.rotate(d);
    }

    public EASSRGBColorSensor getRGBSensor() {
        return cSensor;
    }

	public EASSInfraRedSensor getirSensor()
	{
		return irSensor;
	}

    public EASSUltrasonicSensor getusSensor()
    {
        return uSensor;
    }

    public boolean isHome_edition() {
        return home_edition;

    }

    public void setChanged(boolean education_set) {
        if (education_set) {
            home_edition = false;
        } else {
            home_edition = true;
        }
    }

	public String getMessages()
	{
		return messages.toString();
	}

	public boolean isMoving()
	{
		return pilot.isMoving();
	}

    public boolean isMovingStraight() {
        return pilot.isMoving() && straight;
    }

    public boolean isTurning() {
        return pilot.isMoving() && !straight;
    }
}
