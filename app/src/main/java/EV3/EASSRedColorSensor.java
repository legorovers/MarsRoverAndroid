package EV3;// ----------------------------------------------------------------------------
// Copyright (C) 2015 Strategic Facilities Technology Council 
//
// This file is part of the Engineering Autonomous Space Software (EASS) Library.
// 
// The EASS Library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
// 
// The EASS Library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with the EASS Library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
// 
// To contact the authors:
// http://www.csc.liv.ac.uk/~lad
//
//----------------------------------------------------------------------------

import java.io.PrintStream;

import ail.syntax.Literal;
import ail.syntax.NumberTermImpl;
import lejos.remote.ev3.RemoteRequestEV3;
import lejos.remote.ev3.RemoteRequestSampleProvider;

//import java.rmi.RemoteException;

/**
 * Encapsulation of an Red Light Sensor to be used with an EASS EV3 environment.
 * @author louiseadennis
 *
 */
public class EASSRedColorSensor implements EASSSensor {
	PrintStream out;
	RemoteRequestSampleProvider sensor;
	
	public EASSRedColorSensor(RemoteRequestEV3 brick, String portName) throws Exception
	{ //was remote exception
		try {
			sensor = (RemoteRequestSampleProvider) brick.createSampleProvider(portName, "lejos.hardware.sensor.EV3ColorSensor", "Red");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see eass.mas.ev3.lr_evolution.EASSSensor#addPercept(eass.mas.ev3.lr_evolution.EASSEV3Environment)
	 */
	@Override
	public void addPercept(EASSEV3Environment env) {
		try {
			float[] sample = new float[1];
			sensor.fetchSample(sample, 0);
			float colorvalue = sample[0];
			System.out.println("light level is " + colorvalue);
			if (out != null) {
				out.println("light level is " + colorvalue);
			}
			Literal color = new Literal("light");
			color.addTerm(new NumberTermImpl(colorvalue));
			env.addUniquePercept("light", color);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see eass.mas.ev3.lr_evolution.EASSSensor#setPrintStream(java.io.PrintStream)
	 */
	@Override
	public void setPrintStream(PrintStream s) {
		out = s;
	}
	
	/*
	 * (non-Javadoc)
	 * @see eass.mas.ev3.lr_evolution.EASSSensor#close()
	 */
	@Override
	public void close() {
		try {
			sensor.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	@Override
	public float getSample()
	{
		float[] sample = new float[1];
		sensor.fetchSample(sample, 1);
		return sample[0];
	}
}