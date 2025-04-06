/* Panel1140.java: A JPanel, which displays the Blinkenlight panel
 					as stack of ControlImagesescription

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


   28-Feb-2016  JH      Visualization load logic separated from image load.
   21-Feb-2016  JH      added PANEL_MODE_POWERLESS
   20-Sep-2015  JH      created


     A JPanel, which displays the Blinkenlight panel as stack of ControlImages.

   http://www.java2s.com/Code/Java/Swing-JFC/Panelwithbackgroundimage.htm

   Also functions to integrate the	Blinkenlight API panel & control structs
   with ControlImages

    Generating ControlImages with Photosho
    - make a picture of the panel with all controls in "passive" state
     (LEDs off, switches neutral)
     Use this as "background layer" in photoshop
    - make a photo of the panel with all controls in "active state"
     (LEDs ON, switches ACTIVE).
     - mark every active control and copy it into a separate Photoshop layer
     - Finally you have one layer for every control (showing the active state)
     and one background layer.
     - Save this as "stack_big.psd" or so
     - set "ControlImage.ref_width" in code to the image width (maybe 4000 or so).
     - activate the "info" panel. Click into the center of single every layer
       , in "transform mode" the Info panel show the left top edge coordinates.
     - write thise down, and use them in "new ControlImage()" as
       parameters "ref_left, ref_top"

     - Resize to whole picture to target resolution  (1024)
     - Use the script "Save layers as files"
       Use target format PNG-24, transparencey ON, cut layers
       Copy files to "resource/images", use filenames in  "new ControlImage()"
       - Eclipse: "Refresh" & "Clear Project" !
*/

package blinkenbone.panelsim.panelsim1140;

/*
 * as stack of ControlImages
 *
 * http://www.java2s.com/Code/Java/Swing-JFC/Panelwithbackgroundimage.htm
 *
 *
 * Also functions to integrate
 * the
 * 	Blinkenlight API panel & control structs
 * with
 *   ControlImages
 *
 *   Generating ControlImages with Photosho
 *   - make a picture of the panel with all controls in "passive" state
 *    (LEDs off, switches neutral)
 *    Use this as "background layer" in photoshop
 *   - make a photo of the panel with all controls in "active state"
 *    (LEDs ON, switches ACTIVE).
 *    - mark every active control and copy it into a separate Photoshop layer
 *    - Finally you have one layer for every control (showing the active state)
 *    and one background layer.
 *    - Save this as "stack_big.psd" or so
 *    - set "ControlImage.ref_width" in code to the image width (maybe 4000 or so).
 *    - activate the "info" panel. Click into the center of single every layer
 *      , in "transform mode" the Info panel show the left top edge coordinates.
 *    - write thise down, and use them in "new ControlImage()" as
 *      parameters "ref_left, ref_top"
 *
 *    - Resize to whole picture to target resolution  (1024)
 *    - Use the script "Save layers as files"
 *      Use target format PNG-24, transparencey ON, cut layers
 *      Copy files to "resource/images", use filenames in  "new ControlImage()"
 *      - Eclipse: "Refresh" & "Clear Project" !
 *      -
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;

import com.martiansoftware.jsap.JSAPResult;

import blinkenbone.blinkenlight_api.Control;
import blinkenbone.blinkenlight_api.Control.ControlType;
import blinkenbone.blinkenlight_api.Panel;
import blinkenbone.panelsim.ControlSliceStateImage;
import blinkenbone.panelsim.ControlSliceVisualization;
import blinkenbone.panelsim.DimmableLedControlSliceVisualization;
import blinkenbone.panelsim.ResourceManager;
import blinkenbone.panelsim.TwoStateControlSliceVisualization;
import blinkenbone.panelsim.panelsim1140.Panel1140Control.Panel1140ControlType;
import blinkenbone.rpcgen.rpc_blinkenlight_api;

public class Panel1140 extends JPanel implements Observer {

	// public static String version = "v 1.01" ;
	private static final long serialVersionUID = 1L;

	/*
	 * frame between panel border and background image
	 */
	private int borderTopBottom = 15;
	private int borderLeftRight = 30;
	private Color borderColor = Color.gray;

	public Panel blinkenlightApiPanel; // the Blinkenlight API panel

	public ArrayList<Panel1140Control> controls;

	// links to well defined Blinkenlight Api controls
	Panel1140Control switches_SR;
	Panel1140Control switch_LOAD_ADRS;
	Panel1140Control switch_EXAM;
	Panel1140Control switch_CONT;
	Panel1140Control switch_HALT;
	Panel1140Control switch_START;
	Panel1140Control switch_DEPOSIT;
	Panel1140Control leds_ADDRESS;
	Panel1140Control leds_DATA;
	Panel1140Control led_RUN;
	Panel1140Control led_BUS;
	Panel1140Control led_USER;
	Panel1140Control led_PROCESSOR;
	Panel1140Control led_CONSOLE;
	Panel1140Control led_VIRTUAL;

	// the background
	TwoStateControlSliceVisualization backgroundVisualization;

	private ResourceManager resourceManager;

	private int scaledBackgroundWidth; // width of background image, after
										// load()

	/*
	 *
	 */
	public Panel1140(ResourceManager resourceManager) {
		controls = new ArrayList<Panel1140Control>();
		this.resourceManager = resourceManager;
		// Create the Blinkenlight API panel
		this.blinkenlightApiPanel = constructBlinkenlightApiPanel();
		// blinkenlightApiPanel calls update() on change of output controls
		this.blinkenlightApiPanel.addObserver(this);
	}

	/*
	 * load visualizations for control slices
	 * scaled images are assigned later.
	 */
	public void init(JSAPResult commandlineParameters) {

		// ! controlImages is null in WindowsBuilder designer!
		loadControlVisualizations();
		// knows which pictures and controls to load

		// do state initialization
		clearUserinput();

		// sync Blinkenlight API controls and control visualizations the first
		// time
		outputBlinkenlightApiControlValues2ImageState();

		// what ever the switches are after load: update the Controls
		inputImageState2BlinkenlightApiControlValues(); // calc new control

	}

	/*
	 * to be displayed in application title
	 */
	public String getApplicationTitle() {
		return new String("PDP-11/40 OEM panel simulation (Blinkenlight API server interface) "
				+ Panelsim1140_app.version);
	}

	/*
	 * return user-selectable Widths
	 */
	public Integer[] getSupportedWidths() {
		return new Integer[] { 800, 1000, 1200, 1340, 1580, 1850, 2400, 2950 };
	}

	/*
	 * Create the Blinkenlight API panel control objects. They are accessed over
	 * the RPC interface
	 *
	 * The control names MUST be in synch with SimH and other simulation
	 * programs!
	 */
	private Panel constructBlinkenlightApiPanel() {
		Panel p;

		p = new Panel("11/40"); // from blinkenlightd.conf, also compiled
								// into SimH
		p.info = "Photorealistic simulation of a PDP-11/40 (KY11-D) panel. Java.";
		p.default_radix = 8;

		/*
		 * Build List of controls: interconnected lists of panel 11/70 controls
		 * and BlinkenlightAPI controls Control definitions exact like in
		 * blinkenlightd.conf! SimH relies on those!.
		 *
		 * Knob feed back LEds are hard wired to the knob positions.
		 * this is done in the "KNOB_LEED_FEEDBACK" control
		 */

		controls.add(switches_SR = new Panel1140Control(Panel1140ControlType.PDP11_SWITCH,
				new Control("SR", ControlType.input_switch, 18), null, p));
		controls.add(switch_LOAD_ADRS = new Panel1140Control(Panel1140ControlType.PDP11_KEY,
				new Control("LOAD ADRS", ControlType.input_switch, 1), null, p));
		controls.add(switch_EXAM = new Panel1140Control(Panel1140ControlType.PDP11_KEY,
				new Control("EXAM", ControlType.input_switch, 1), null, p));
		controls.add(switch_CONT = new Panel1140Control(Panel1140ControlType.PDP11_KEY,
				new Control("CONT", ControlType.input_switch, 1), null, p));
		controls.add(switch_HALT = new Panel1140Control(Panel1140ControlType.PDP11_SWITCH,
				new Control("HALT", ControlType.input_switch, 1), null, p));
		controls.add(switch_START = new Panel1140Control(Panel1140ControlType.PDP11_KEY,
				new Control("START", ControlType.input_switch, 1), null, p));
		controls.add(switch_DEPOSIT = new Panel1140Control(Panel1140ControlType.PDP11_KEY,
				new Control("DEPOSIT", ControlType.input_switch, 1), null, p));

		controls.add(leds_ADDRESS = new Panel1140Control(Panel1140ControlType.PDP11_LAMP, null,
				new Control("ADDRESS", ControlType.output_lamp, 18), p));
		controls.add(leds_DATA = new Panel1140Control(Panel1140ControlType.PDP11_LAMP, null,
				new Control("DATA", ControlType.output_lamp, 16), p));
		controls.add(led_RUN = new Panel1140Control(Panel1140ControlType.PDP11_LAMP, null,
				new Control("RUN", ControlType.output_lamp, 1), p));
		controls.add(led_BUS = new Panel1140Control(Panel1140ControlType.PDP11_LAMP, null,
				new Control("BUS", ControlType.output_lamp, 1), p));
		controls.add(led_USER = new Panel1140Control(Panel1140ControlType.PDP11_LAMP, null,
				new Control("USER", ControlType.output_lamp, 1), p));
		controls.add(led_PROCESSOR = new Panel1140Control(Panel1140ControlType.PDP11_LAMP, null,
				new Control("PROCESSOR", ControlType.output_lamp, 1), p));
		controls.add(led_CONSOLE = new Panel1140Control(Panel1140ControlType.PDP11_LAMP, null,
				new Control("CONSOLE", ControlType.output_lamp, 1), p));
		controls.add(led_VIRTUAL = new Panel1140Control(Panel1140ControlType.PDP11_LAMP, null,
				new Control("VIRTUAL", ControlType.output_lamp, 1), p));

		return p;
	}

	/*
	 * Load all images for known resolution
	 */
	public void loadControlVisualizations() {

		/*
		 * load all visualizations into global list. mark them with the
		 * Blinkenlight API control and bit pos
		 */
		backgroundVisualization = new TwoStateControlSliceVisualization("background.png", null,
				null, 0);

		// clear visualization of all controls
		for (Panel1140Control panelcontrol : controls) {
			panelcontrol.visualization.clear();
		}

		// All coordinates must have been loaded: loadImageCoordinates()
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_17_up.png", switches_SR, switches_SR.inputcontrol, 17));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_16_up.png", switches_SR, switches_SR.inputcontrol, 16));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_15_up.png", switches_SR, switches_SR.inputcontrol, 15));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_14_up.png", switches_SR, switches_SR.inputcontrol, 14));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_13_up.png", switches_SR, switches_SR.inputcontrol, 13));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_12_up.png", switches_SR, switches_SR.inputcontrol, 12));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_11_up.png", switches_SR, switches_SR.inputcontrol, 11));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_10_up.png", switches_SR, switches_SR.inputcontrol, 10));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_09_up.png", switches_SR, switches_SR.inputcontrol, 9));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_08_up.png", switches_SR, switches_SR.inputcontrol, 8));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_07_up.png", switches_SR, switches_SR.inputcontrol, 7));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_06_up.png", switches_SR, switches_SR.inputcontrol, 6));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_05_up.png", switches_SR, switches_SR.inputcontrol, 5));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_04_up.png", switches_SR, switches_SR.inputcontrol, 4));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_03_up.png", switches_SR, switches_SR.inputcontrol, 3));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_02_up.png", switches_SR, switches_SR.inputcontrol, 2));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_01_up.png", switches_SR, switches_SR.inputcontrol, 1));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sr_00_up.png", switches_SR, switches_SR.inputcontrol, 0));

		// command switches
		switch_LOAD_ADRS.visualization
				.add(new TwoStateControlSliceVisualization("switch_load_adrs_down.png",
						switch_LOAD_ADRS, switch_LOAD_ADRS.inputcontrol, 0));
		switch_EXAM.visualization.add(new TwoStateControlSliceVisualization(
				"switch_exam_down.png", switch_EXAM, switch_EXAM.inputcontrol, 0));
		switch_CONT.visualization.add(new TwoStateControlSliceVisualization(
				"switch_cont_down.png", switch_CONT, switch_CONT.inputcontrol, 0));
		switch_HALT.visualization.add(new TwoStateControlSliceVisualization(
				"switch_halt_down.png", switch_HALT, switch_HALT.inputcontrol, 0));
		switch_START.visualization
				.add(new TwoStateControlSliceVisualization("switch_start_down.png",
						switch_START, switch_START.inputcontrol, 0));
		switch_DEPOSIT.visualization
				.add(new TwoStateControlSliceVisualization("switch_dep_up.png",
						switch_DEPOSIT, switch_DEPOSIT.inputcontrol, 0));

		DimmableLedControlSliceVisualization.defaultAveragingInterval_ms = 100;
		// create all LEDs with a low pass of 1/10 sec.
		// Critical are the "running light" and "idle pattern" use cases.

		// dimmable LEDs use variable transparency, so they do not dsiaturb the
		// background if
		// they are "dim"
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_17.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 17, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_16.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 16, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_15.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 15, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_14.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 14, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_13.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 13, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_12.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 12, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_11.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 11, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_10.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 10, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_09.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 9, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_08.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 8, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_07.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 7, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_06.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 6, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_05.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 5, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_04.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 4, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_03.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 3, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_02.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 2, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_01.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 1, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_address_00.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 0, true));

		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_15.png",
				leds_DATA, leds_DATA.outputcontrol, 15, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_14.png",
				leds_DATA, leds_DATA.outputcontrol, 14, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_13.png",
				leds_DATA, leds_DATA.outputcontrol, 13, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_12.png",
				leds_DATA, leds_DATA.outputcontrol, 12, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_11.png",
				leds_DATA, leds_DATA.outputcontrol, 11, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_10.png",
				leds_DATA, leds_DATA.outputcontrol, 10, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_09.png",
				leds_DATA, leds_DATA.outputcontrol, 9, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_08.png",
				leds_DATA, leds_DATA.outputcontrol, 8, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_07.png",
				leds_DATA, leds_DATA.outputcontrol, 7, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_06.png",
				leds_DATA, leds_DATA.outputcontrol, 6, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_05.png",
				leds_DATA, leds_DATA.outputcontrol, 5, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_04.png",
				leds_DATA, leds_DATA.outputcontrol, 4, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_03.png",
				leds_DATA, leds_DATA.outputcontrol, 3, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_02.png",
				leds_DATA, leds_DATA.outputcontrol, 2, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_01.png",
				leds_DATA, leds_DATA.outputcontrol, 1, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_00.png",
				leds_DATA, leds_DATA.outputcontrol, 0, true));

		led_RUN.visualization.add(new DimmableLedControlSliceVisualization("led_run_on.png",
				led_RUN, led_RUN.outputcontrol, 0, true));
		led_BUS.visualization.add(new DimmableLedControlSliceVisualization("led_bus_on.png",
				led_BUS, led_BUS.outputcontrol, 0, true));
		led_USER.visualization.add(new DimmableLedControlSliceVisualization("led_user_on.png",
				led_USER, led_USER.outputcontrol, 0, true));
		led_PROCESSOR.visualization.add(new DimmableLedControlSliceVisualization("led_proc_on.png",
				led_PROCESSOR, led_PROCESSOR.outputcontrol, 0, true));
		led_CONSOLE.visualization.add(new DimmableLedControlSliceVisualization("led_console_on.png",
				led_CONSOLE, led_CONSOLE.outputcontrol, 0, true));
		led_VIRTUAL.visualization.add(new DimmableLedControlSliceVisualization("led_virtual_on.png",
				led_VIRTUAL, led_VIRTUAL.outputcontrol, 0, true));

	}

	/*
	 * load the scaled images for visualizations
	 * "<size> in filenames is replaced by scaledBackgroundWidth
	 */
	public void loadControlVisualizationImages(int scaledBackgroundWidth) {

		if (scaledBackgroundWidth > 0 && this.scaledBackgroundWidth == scaledBackgroundWidth)
			return; // no change in width
		this.scaledBackgroundWidth = scaledBackgroundWidth;

		// use this resourceManger for all images
		ControlSliceStateImage.resourceManager = resourceManager;
		// all coodinates are made with this base resolution
		// ControlSliceStateImage.scaledBackgroundWidth = scaledBackgroundWidth;
		ControlSliceStateImage.resourceImageFilePathPrefix = "blinkenbone/panelsim/panelsim1140/images/";
		ControlSliceStateImage.resourceImageFileNamePrefix = "pdp1140industrial_size="
				+ scaledBackgroundWidth + "_";

		// full file name is something like
		// "pdpPDP8I_size=1200_coordinates.csv"
		ControlSliceStateImage.loadImageInfos("coordinates.csv");

		// background: load image
		backgroundVisualization.createStateImages();
		backgroundVisualization.setStateExact(1); // always visible

		/*
		 * all visualisations: loadImages
		 */
		for (Panel1140Control panelcontrol : controls)
			for (ControlSliceVisualization csv : panelcontrol.visualization) {
				csv.createStateImages();
			}

		// adjust JPanel size to background size + frame border around
		// background size = size of single state image
		// if (controlVisualization != null) { ??????????????????
		Dimension size = new Dimension(
				backgroundVisualization.getVisibleStateImage().scaledStateImage.getWidth()
						+ 2 * borderLeftRight,
				backgroundVisualization.getVisibleStateImage().scaledStateImage.getHeight()
						+ 2 * borderTopBottom);
		if (size != null) {
			setPreferredSize(size);
			setMinimumSize(size);
			setMaximumSize(size);
			setSize(size);
			// setLayout(null);
		}

		// now all old images catalogs can be cleaned up. Do it now, not when
		// the system is running.
		System.gc();
	}

	/*
	 * selftest: show all control images this is done by setting the panel into
	 * one of the test modes.
	 * incomplete capsuled from blinkenlight API panel
	 */

	public void setSelftest(int selftestmode) {
		blinkenlightApiPanel.setMode(selftestmode);
		// one of rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_*
	}

	public int getSelftest() {
		return blinkenlightApiPanel.mode;
	}

	/*
	 * Get state image for one bit of a control in test mode
	 * generated combined state image
	 * null if no image available / useful
	 */
	public ControlSliceStateImage getTestStateImage(Panel1140Control panelcontrol,
			ControlSliceVisualization csv, int testmode) {

		switch (panelcontrol.type) {
		case PDP11_LAMP: // show brightest led/lamp image in all test modes
			return csv.getStateImage(csv.maxState);
		case PDP11_SWITCH: //
		case PDP11_KEY: //
			switch (testmode) {
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST:
				return csv.getStateImage(csv.getState()); // no switch change in
															// "lamptest", why
															// is this
															// necessary?
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST:
				// total test: show as "pressed"
				return csv.getStateImage(csv.maxState);
			}
			break;
		}
		return null;
	}

	/*
	 * Draw the stack of images onto a Graphics. To be used in a JPanel
	 * "paintComponent(Graphics g)"
	 *
	 * draw order as defined.: background first!
	 *
	 * double buffering?
	 * http://docs.oracle.com/javase/tutorial/extra/fullscreen/doublebuf.html
	 */
	public void paintComponent(Graphics g) {
		boolean incomplete = false;
		Graphics2D g2d = (Graphics2D) g; // for transparency with AlphaComposite

		ControlSliceStateImage cssi;

		java.awt.Composite originalComposite = g2d.getComposite(); // save
																	// initial
																	// transparency

		// fill panel => frame around background image
		setForeground(borderColor);
		g2d.fillRect(0, 0, getWidth(), getHeight());

		// always draw background, is always visible
		if (backgroundVisualization != null
				&& (cssi = backgroundVisualization.getVisibleStateImage()) != null
				&& cssi.scaledStateImage != null) {
			g.drawImage(cssi.scaledStateImage, borderLeftRight, borderTopBottom, null);

		} else
			incomplete = true;

		// incomplete |= (controlVisualization == null); ??????????????
		if (!incomplete) {
			for (Panel1140Control panelcontrol : controls)
				for (ControlSliceVisualization csv : panelcontrol.visualization) {
					cssi = null;
					// selftest over Blinkenlight API:
					if (blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST
							|| blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST) {
						cssi = getTestStateImage(panelcontrol, csv, blinkenlightApiPanel.mode);
					} else if (blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_POWERLESS
							&& panelcontrol.type == Panel1140ControlType.PDP11_LAMP) {
						cssi = null; // all lamps OFF = invisible
					} else {
						// show regular state
						// do not paint lamps or switches in OFF state,
						// background shows them already
						cssi = null;
						if (csv.getState() != 0)
							cssi = csv.getStateImage(csv.getState());
						// switches in state 0: image NUll -> do not paint ->
						// show background
					}
					if (cssi != null) {
						if (cssi.alphaComposite != null)
							g2d.setComposite(cssi.alphaComposite); // image has
																	// transparency
						else
							g2d.setComposite(originalComposite); // image has no
																	// transparency
						g2d.drawImage(cssi.scaledStateImage,
								cssi.scaledPosition.x + borderLeftRight,
								cssi.scaledPosition.y + borderTopBottom, null);
					}
					// csv.newAveragingState(); // new sampling interval
				}
		}
		if (incomplete) {
			g2d.setComposite(originalComposite); // image has no transparency
			// no display, for WindowsBuilder designer
			super.paintComponent(g);
		}
	}

	/*
	 * find ControlVisualization at position (x,y) on the screen (after Mouse
	 * click). mouse must be clicked on currently visible state image.
	 * Called highspeed on mouse drag!
	 */
	private ControlSliceVisualization getControlVisualizationAt(Point clickpoint) {
		// System.out.printf("clickpoint=(%d,%d)%n", clickpoint.x,
		// clickpoint.y);

		// map clickpoint to background image coordinates
		clickpoint.x -= borderLeftRight;
		clickpoint.y -= borderTopBottom;
		for (Panel1140Control panelcontrol : controls)
			for (ControlSliceVisualization csv : panelcontrol.visualization) {
				ControlSliceStateImage cssi = csv.getVisibleStateImage();
				if (cssi != null && cssi.scaledRectangle.contains(clickpoint)
				// image transparency at clickpoint must be > 50%
						&& cssi.getPixelAt(clickpoint).getAlpha() > 128)
					return csv;
			}
		// no visible state image was clicked
		// but there may be the picture of an "inactive" control
		// be painted onto the background.
		//
		// Check, wether any state image of a ControlSliceVisualization
		// could be under the click point
		for (Panel1140Control panelcontrol : controls)
			for (ControlSliceVisualization csv : panelcontrol.visualization) {
				for (ControlSliceStateImage cssi : csv.stateImages) {
					if (cssi.scaledRectangle.contains(clickpoint)
							// image transparency at clickpoint must be > 50%
							&& cssi.getPixelAt(clickpoint).getAlpha() > 128)
						return csv;
				}
			}
		return null;
	}

	/*
	 * process mouse down/up: find control image, and set it visible/invisible
	 * The blinkenlight API control value is calculated from the visible
	 * state.
	 */

	// this csv is under the mouse and the button is pressed
	ControlSliceVisualization currentMouseControlSliceVisualization = null;

	// mouseButton is MouseEventBUTTON1/2/3
	public void mouseDown(Point clickpoint, int mouseButton) {
		ControlSliceVisualization csv = getControlVisualizationAt(clickpoint);
		if (csv != null) {
			mouseDown(csv, mouseButton);
		}
		currentMouseControlSliceVisualization = csv;
		// null, if clicked to empty space
	}

	public void mouseDown(ControlSliceVisualization csv, int mouseButton) {

		Panel1140Control panelcontrol = (Panel1140Control) csv.panelControl;
		switch (panelcontrol.type) {
		case PDP11_SWITCH:
			// toggle between state 1 and 0 on separate clicks
			if (csv.getState() == 0)
				csv.setStateExact(1);
			else
				csv.setStateExact(0);
			break;
		case PDP11_KEY:
			// activate only while mouse pressed down
			csv.setStateExact(1);
			break;
		default:
			;
		}
		// calc new control value
		inputImageState2BlinkenlightApiControlValues();
	}

	public void mouseUp(Point clickpoint, int mouseButton) {
		ControlSliceVisualization csv = getControlVisualizationAt(clickpoint);
		if (csv != null) {
			mouseUp(csv, mouseButton);
		}
		currentMouseControlSliceVisualization = null;
		// no csv under pressed mouse
	}

	public void mouseUp(ControlSliceVisualization csv, int mouseButton) {
		// System.out.printf("mouseUp(%s)%n", csv.name);
		Panel1140Control panelcontrol = (Panel1140Control) csv.panelControl;
		switch (panelcontrol.type) {
		case PDP11_KEY:
			// deactivate button (= unpress) because mouse button is released
			csv.setStateExact(0);
			inputImageState2BlinkenlightApiControlValues(); // calc new control value
			break;
		default:
			;
		}
	}

	/*
	 * Mouse is moved with pressed button:
	 * generatoe Mosue/Down/Up events for csv under cursor
	 */
	public void mouseDragged(Point clickpoint, int mouseButton) {
		// everything here is HIGHSPEED
		ControlSliceVisualization csv = getControlVisualizationAt(clickpoint);
		if (csv != currentMouseControlSliceVisualization)
			// mouse moved with pressed button to another image
			// csv == null: moved away from csv's.
			// currentMouseControlSliceVisualization == null: moved into first
			// csv
			if (currentMouseControlSliceVisualization != null)
				// leave old image
				mouseUp(currentMouseControlSliceVisualization, mouseButton);

		if (csv != null && csv != currentMouseControlSliceVisualization) {
			// moved into another csv
			mouseDown(csv, mouseButton); // enter new image
			// now currentMouseControlSliceVisualization = csv
		}
		currentMouseControlSliceVisualization = csv;
	}

	/*
	 * true, if any panel output values (LED) changed against "values_previous"
	 */
	public boolean anyOutputsChanged() {
		return (blinkenlightApiPanel.getControlValueChanges(/* is_input */false) > 0);
	}

	/*
	 * Reset switches to "Normal" position
	 *
	 * Here DATA switches and command switches, with exception of HALT, are
	 * reset to 0. Tough Command switches are "momentary action" (always
	 * flipping back to 0), they can hang sometimes.
	 */
	public void clearUserinput() {
		for (Panel1140Control panelcontrol : controls)
			for (ControlSliceVisualization csv : panelcontrol.visualization) {
				csv.setStateExact(0);
			}
		// calc new control values
		inputImageState2BlinkenlightApiControlValues();
	}

	/*
	 * decode input (Switches) image controls into BlinkenLight API control
	 * values.
	 */
	public void inputImageState2BlinkenlightApiControlValues() {

		for (Panel1140Control panelcontrol : controls) {
			Control c = panelcontrol.inputcontrol;
			if (c != null)
				synchronized (c) {
					// RPC server may not change control values in parallel
					c.value = 0; // init value
					// is a switch or switch bank.compose value of "active"
					// bit images
					c.value = 0; // init value
					for (ControlSliceVisualization csv : panelcontrol.visualization) {
						// all bit slices of this switch control
						if (csv.blinkenlightApiControl == c && csv.getState() != 0) {
							// control has a visible state. Switches are
							// only ON/invisible, so any state give a "bit set"
							c.value |= (1 << csv.controlSlicePosition);
						}
					}
				}
		}
	}

	/*
	 * set the visible state of control slices according to BlinkenLight API
	 * control values.
	 *
	 * Here all output controls are LEDs with 8 brightnes states. At first, set
	 * bit 1 as state 7, and 0 as state 0 (very dim)
	 *
	 * Deletes the "changed" status by setting value_previous = value
	 *
	 * state are averaged over time! So
	 * outputBlinkenlightApiControlValues2ImageState() has to be faster then
	 * display update, and display has to use newAveragingState()
	 */
	public void outputBlinkenlightApiControlValues2ImageState() {
		// loop for all BlinkenLight API control
		for (Panel1140Control panelcontrol : controls) {
			Control c = panelcontrol.outputcontrol;
			if (c != null)
				synchronized (c) {
					// RPC server may not change control values in parallel
					for (ControlSliceVisualization csv : panelcontrol.visualization)
						synchronized (csv) {
							// set visibility for all images of this control
							boolean visible = false;
							// all other: LED image shows a single bit of
							// value
							// is bit "control_bitpos" set in value?
							if ((c.value & (1L << csv.controlSlicePosition)) > 0)
								visible = true;
							// all outputs are LEDs
							assert (csv.getClass() == DimmableLedControlSliceVisualization.class);
							// LEDs: : select between off and max
							if (visible)
								csv.setStateAveraging(csv.maxState);
							else
								csv.setStateAveraging(0);
						}
					c.value_previous = c.value;
				}
		}
	}

	/*
	 * Let the panel appear as "powerless":
	 * all lamps go dark, the power button should go to the "off" state.
	 * Lamps are painted as OFF in paintComponent() !
	 */
	private void setPowerMode(int mode) {
		// no power switch to set
	}

	/*
	 * called by observable blinkenlightApiPanel on change of output controls
	 *
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (o == blinkenlightApiPanel) {
			// panel mode "POWERLESS" should change control states, LAMPTESTs do
			// not.
			// override outputcontrol states and visual input controls
			setPowerMode(blinkenlightApiPanel.mode);
			outputBlinkenlightApiControlValues2ImageState();
		}
	}
}
