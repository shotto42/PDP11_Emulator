/* ControlSliceVisualization.java: logic to show a single bit of a control in different shapes

   Copyright (c) 2012-2016, Joerg Hoppe
   j_hoppe@t-online.de, www.retrocmp.com

   Permission is hereby granted, free of charge, to any person obtaining a
   copy of this software and associated documentation files (the "Software"),
   to deal in the Software without restriction, including without limitation
   the rights to use, copy, modify, merge, publish, distribute, sublicense,
   and/or sell copies of the Software, and to permit persons to whom the
   Software is furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in
   all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
   JOERG HOPPE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

   26-Apr-2016  JH		apply optional RescaleOp() on images after load, to modify brightness/contrast
   21-Apr-2016  JH      dec/inc of knobs changed from "left/right mouse button" to
                        "click coordinate left/right of image center"
   17-May-2012  JH      created


   A ControlSliceVisualization is
   - a set of images
   - for a part of a Blinkenlight API control

   There are multiple images for different "states"
   (LEDs in different degree of luminance, digit display showing digits 0..9,
   several views onto a rotary knob ...)

   Images process chain:
   1. once: load scaled images into unscaled_img
   2. once: processStateImages after load into unscaled_img
   3. on every resize: resize unscaled_img to scaled_images
   4. on every event: drawImage(scaled_images)
 */

package blinkenbone.panelsim;

import java.awt.Point;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import blinkenbone.blinkenlight_api.Control;

public abstract class ControlSliceVisualization {

	// static global vars, because lot of StateImage are constructed
	// with these parameters always constant.

	public String name;

	// viusal control on panel
	public PanelControl panelControl;

	// Blinkenlight API Control for the slice
	public Control blinkenlightApiControl;

	public ArrayList<ControlSliceStateImage> stateImages;

	public int controlSlicePosition; // which slice in blinkenlightApiControl?
	// normally a bit number in a LED row

	public int minState; // lowest state of all state images
	public int maxState; // heighest state of all state images

	public static int defaultAveragingInterval_ms = 100; // 1/10 sec
	// low pass for states in millisecs. is default for constructor

	// if a mouse clicked onto an state image: last
	public ControlSliceStateImage clickedStateImage ;
	public Point clickedStateImagePoint ; // relative coordinates of cick event inside  clickedStateImage

	// after load a image from disk, these Rescaleop Params are applied
	// this allows to modify brightness & contrast.
	public float imageRescaleopScale ;
	public float imageRescaleopOffset ;

	// public ControlSliceStateImage visibleStateImage; // which of the state
	// images is to display?
	// null = invisible

	public ControlSliceVisualization(String name, PanelControl panelControl, Control control,
			int bitpos) {
		stateImages = new ArrayList<ControlSliceStateImage>();
		this.name = name;
		this.panelControl = panelControl;
		this.blinkenlightApiControl = control;
		this.controlSlicePosition = bitpos;
		// visibleStateImage = null; // invisible
		minState = Integer.MAX_VALUE;
		maxState = Integer.MIN_VALUE;
		stateExact = 0;
		stateExactValid = true;
		averagingInterval_us = 1000 * defaultAveragingInterval_ms;

		imageRescaleopScale = 1 ; // default: no change
		imageRescaleopOffset = 0 ;
	}

	/*
	 * Subsystem to trace multiple state changes and return the average in the
	 * sampling interval (needed for smooting highspeed output controls ... CPU
	 * LEDs!)
	 *
	 * For averaging the states, a queue of (timestamp, state) holds the last
	 * change events. The "average state" is calculated from events in the time
	 * <averagingInterval> before "Now = System.nanoTime()
	 * See
	 * http://stackoverflow.com/questions/510462/is-system-nanotime-completely
	 * -useless
	 * nanotime is QueryPerfomanceCounter on Win
	 *
	 * See java.util.LinkedList
	 * - on BlinkenlightAPi update:
	 * offer(state, now)
	 * on getState(),
	 * - remove all queue entries older then (now - averagingInterval)
	 * - for each state-setting- entry in the queue:
	 * calc state duration, sum up state weighed with duration:
	 */

	int averagingInterval_us;

	private class StateHistoryEntry {
		int state;
		long timeStampBegin_us; // time of state change
		long timeStampEnd_us; // end of this of state == start of next

		public StateHistoryEntry(long aTimestamp_us, int aState) {
			this.state = aState;
			this.timeStampBegin_us = aTimestamp_us;
			this.timeStampEnd_us = 0; // currently valid.
		}
	}

	// History FIFO
	// If the BlinkenlightApi client (SimH) sends states with
	// clientSendPeriod_us,
	// it will contain averagingInterval_us / clientSendPeriod_us == 500.000 /
	// 1000 = 500 entries
	private LinkedList<StateHistoryEntry> stateHistory = new LinkedList<StateHistoryEntry>();

	private int stateExact;
	private Boolean stateExactValid = true;

	FileWriter logwriter = null;
	long logwriterStarttime_us = 0;

	public void setStateExact(int state) {
		stateExact = state;
		stateExactValid = true;
	}

	@SuppressWarnings("unused")
	public void setStateAveraging(int state) {
		stateExactValid = false;
		// save in history

		// start logging
		if (false && logwriter == null && state > 0)
			try {
				logwriter = new FileWriter("/tmp/cssi.log");
				logwriterStarttime_us = System.nanoTime() / 1000;
				logwriter.write("LOG START\n");
			} catch (IOException io) {
				io.printStackTrace();
			}
		StateHistoryEntry she = stateHistory.peekLast();
		if (she == null || she.state != state)
			synchronized (stateHistory) {
				// add new entry only if state changed!
				long now_us = System.nanoTime() / 1000;
				if (she != null)
					// terminate current state, if queue not empty
					she.timeStampEnd_us = now_us;
				// register new state
				stateHistory.offer(new StateHistoryEntry(now_us, state));
			}
		// getState() must be called periodically
		long averageMaxFill = averagingInterval_us / 1000; // send period max 1
															// ms = 1000 us
		assert (stateHistory.size() < 2 * averageMaxFill);

		logText(String.format("setStateAveraging(%d)", state));

	}

	private void logText(String txt) {
		if (logwriter == null)
			return;
		try {
			// dump the history
			logwriter.write(String.format("[%d us] %s\n", System.nanoTime() / 1000
					- logwriterStarttime_us, txt));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void logStateHistory(String info) {
		if (logwriter == null)
			return;
		try {
			// dump the history
			logText(String.format("State history of '%s':", name));

			StateHistoryEntry she;
			Iterator<StateHistoryEntry> iter = stateHistory.iterator();
			while (iter.hasNext()) {
				she = iter.next();
				logwriter.write(String.format("  %,d - %,d state=%d\n", she.timeStampBegin_us
						- logwriterStarttime_us, she.timeStampEnd_us - logwriterStarttime_us,
						she.state));
			}
			logwriter.write(String.format("                                %s\n", info));
			logwriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getState() {
		if (stateExactValid || stateHistory.size() == 0)
			// // no average, or last call was to "setExactState(): return exact
			// state
			return stateExact;
		// problem: if states are outside the range 0..max_state, the average
		// state
		// may not be a valid state with an image.
		int result;
		StateHistoryEntry she;

		long now_us = System.nanoTime() / 1000;
		she = stateHistory.peekLast();
		if (she != null)
			// terminate current state (which has no end time yet)
			she.timeStampEnd_us = now_us;

		synchronized (stateHistory) {

			// discard old history entries
			long intervalStart_us = now_us - averagingInterval_us;
			while ((she = stateHistory.peekFirst()) != null
					&& she.timeStampEnd_us < intervalStart_us)
				stateHistory.poll();

			long sumStateDurations = 0; // all states *
			long sumDurations_us = 0; // time of all states
			Iterator<StateHistoryEntry> iter = stateHistory.iterator();
			while (iter.hasNext()) {
				she = iter.next();
				// oldest state max fall only partly into averaging interval
				long stateStarttime_us = Math.max(intervalStart_us, she.timeStampBegin_us);
				long stateDuration_us = she.timeStampEnd_us - stateStarttime_us;
				sumStateDurations += she.state * stateDuration_us;
				sumDurations_us += stateDuration_us;
			}
			// !! sumDurations_us == (now_us - intervalStart_us !!
			if (sumDurations_us > 0)
				result = (int) (sumStateDurations / sumDurations_us); //
			else
				result = 0;
		}

		logText(String.format("getState():"));
		logStateHistory(String.format("averageState = %d", result));

		if (logwriter != null && stateHistory.size() == 1
				&& stateHistory.peekFirst().state == 0) {
			// stop logging, if history contains only one "state = 0"
			try {
				logwriter.write("LOG END\n");
				logwriter.close();
			} catch (IOException io) {
				io.printStackTrace();
			}
			logwriter = null;
		}

		return result;
	}

	// find the biggest defined state < current state
	// stop at lowest state
	public int getNextLowerState() {
		ControlSliceStateImage bestCssi = null;
		for (ControlSliceStateImage cssi : stateImages)
			if (cssi.state < stateExact)
				if (bestCssi == null || bestCssi.state < cssi.state)
					bestCssi = cssi;
		if (bestCssi == null)
			return minState; // not found
		else
			return bestCssi.state;
	}

	// find the lowest defined state > current state
	// stop at highest state
	public int getNextHigherState() {
		ControlSliceStateImage bestCssi = null;
		for (ControlSliceStateImage cssi : stateImages)
			if (cssi.state > stateExact)
				if (bestCssi == null || bestCssi.state > cssi.state)
					bestCssi = cssi;
		if (bestCssi == null)
			return maxState; // not found
		else
			return bestCssi.state;
	}
	
	// find the biggest defined state < current state
	// after lowest state highest state 
	public int getNextRotateToLowerState() {
		ControlSliceStateImage bestCssi = null;
		for (ControlSliceStateImage cssi : stateImages)
			if (cssi.state < stateExact)
				if (bestCssi == null || bestCssi.state < cssi.state)
					bestCssi = cssi;
		if (bestCssi == null)
			return maxState; // not found: roll over to highest
		else
			return bestCssi.state;
	}

	// find the biggest defined state < current state
	// after lowest state highest state 
	public int getNextRotateToHigherState() {
		ControlSliceStateImage bestCssi = null;
		for (ControlSliceStateImage cssi : stateImages)
			if (cssi.state > stateExact)
				if (bestCssi == null || bestCssi.state > cssi.state)
					bestCssi = cssi; 
		if (bestCssi == null)
			return minState; // not found: roll over to lowest
		else
			return bestCssi.state;
	}


	
	// only one image (state,variant) is painted, only visible images may be target of mouse clicks
	public ControlSliceStateImage getClickableStateImage() {
		for (ControlSliceStateImage cssi : stateImages) {
			if (cssi.clickable)
				return cssi;
		}
		return null; // not found
	}
	/*
	 * which of the state images is to display? null = invisible
	 * used for mouse click points, should be implemented with "image.clickable"
	 
	 */
	@Deprecated
	public ControlSliceStateImage getVisibleStateImage() {
		return getStateImage(getState());
	}

	/*
	 * add a state image ControlSliceStateImage. resourceManager,
	 * unscaledBackgroundWidth scaledBackgroundWidth must have been set.
	 */
	public ControlSliceStateImage addStateImage(String imageFilename, int state, int variant) {

		// System.out.printf("%s\n", imageFilename);
		ControlSliceStateImage cssi = new ControlSliceStateImage(imageFilename, state, variant,
				imageRescaleopScale, imageRescaleopOffset );

		stateImages.add(cssi);
		if (minState > cssi.state)
			minState = cssi.state;
		if (maxState < cssi.state)
			maxState = cssi.state;
		return cssi;
	}

	// form without variant
	public ControlSliceStateImage addStateImage(String imageFilename, int state) {
		return addStateImage(imageFilename, state, 0) ;
	}
	
	
	/*
	 * add a deep copy of another StateImage
	 */
	public ControlSliceStateImage addStateImage(ControlSliceStateImage original, int newstate) {
		ControlSliceStateImage cssi = new ControlSliceStateImage(original, newstate);
		stateImages.add(cssi);
		if (minState > cssi.state)
			minState = cssi.state;
		if (maxState < cssi.state)
			maxState = cssi.state;
		return cssi;
	}

	/*
	 * search a state image by state code.
     * ignore "variant" code of state image
	 */
	public ControlSliceStateImage getStateImage(int state) {
		for (ControlSliceStateImage cssi : stateImages) {
			if (cssi.state == state)
				return cssi;
		}
		return null; // not found
	}


	/*
	 * search a state image by state code and variant code
	 */
	public ControlSliceStateImage getStateImage(int state, int variant) {
		for (ControlSliceStateImage cssi : stateImages) {
			if (cssi.state == state && cssi.variant == variant)
				return cssi;
		}
		return null; // not found
	}


	// template images, used to produce scaled and dimmed "stateImages[] ;
	// method to load and calc "stateImages.img []


	public abstract void loadStateImages();

	/*
	 * load all state images, scale and process them
	 *
	 * uses abstract methods loadUnscaledStateImages() and fixupStateImages()
	 */
	public void createStateImages() {
		stateImages.clear();
		loadStateImages();
		fixupStateImages();
	}

	// ZB: alle States einer dimmbaren LED sind dasselbe "originalbild")

	/*
	 * nach "laden" der grossen Rohbilder können abgeleitete Klassen states
	 * fabrizieren. zB: LEDs stufenweise dunkler rechnen input und output
	 * unscaliert!
	 */
	public abstract void fixupStateImages();

}
