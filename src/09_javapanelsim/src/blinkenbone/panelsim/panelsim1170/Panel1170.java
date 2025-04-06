/* Panel1170.java: A JPanel, which displays the Blinkenlight panel
 					as stack of ControlImagesescription

   Copyright (c) 2015-2016, Joerg Hoppe
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

   03-Aug-2016  JH      POWER/LOCK key switch did not turn on POWER mode
   21-Apr-2016  JH      dec/inc of knobs changed from "left/right mouse button" to
                        "click coordinate left/right of image center"
   20-Apr-2016  JH      added POWER/LOCK keyswitch
   28-Feb-2016  JH      Visualization load logic separated from image load.
   21-Feb-2016  JH      added PANEL_MODE_POWERLESS
   20-Sep-2015  JH      created


     A JPanel, which displays the Blinkenlight panel as stack of ControlImages

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

package blinkenbone.panelsim.panelsim1170;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
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
import blinkenbone.panelsim.MultiStateControlSliceVisualization;
import blinkenbone.panelsim.ResourceManager;
import blinkenbone.panelsim.TwoStateControlSliceVisualization;
import blinkenbone.panelsim.panelsim1170.Panel1170Control.Panel1170ControlType;
import blinkenbone.panelsim.panelsimPDP8I.PanelPDP8IControl;
import blinkenbone.rpcgen.rpc_blinkenlight_api;

public class Panel1170 extends JPanel implements Observer {

	// public static String version = "v 1.01" ;
	private static final long serialVersionUID = 1L;

	/*
	 * frame between panel border and background image
	 */
	private int borderTopBottom = 0;
	private int borderLeftRight = 0;
	private Color borderColor = Color.gray;

	public Panel blinkenlightApiPanel; // the Blinkenlight API panel

	public ArrayList<Panel1170Control> controls;

	// links to well defined Blinkenlight Api controls
	Panel1170Control keyswitch; // optical: OFF,POWER,LOCK.
	// API 2 controls: 1 bit POWER, 1 bit LOCK
	Control control_power; // update programmatically
	Control control_panel_lock;

	Panel1170Control switches_SR;
	Panel1170Control switch_LAMPTEST; // not on physical panel
	Panel1170Control switch_LOAD_ADRS;
	Panel1170Control switch_EXAM;
	Panel1170Control switch_DEPOSIT;
	Panel1170Control switch_CONT;
	Panel1170Control switch_HALT;
	Panel1170Control switch_S_BUS_CYCLE;
	Panel1170Control switch_START;

	Panel1170Control leds_ADDRESS;
	Panel1170Control leds_DATA;
	Panel1170Control led_PARITY_HIGH;
	Panel1170Control led_PARITY_LOW;
	Panel1170Control led_PAR_ERR;
	Panel1170Control led_ADRS_ERR;
	Panel1170Control led_RUN;
	Panel1170Control led_PAUSE;
	Panel1170Control led_MASTER;
	Panel1170Control leds_MMR0_MODE;
	Panel1170Control led_DATA_SPACE;
	Panel1170Control led_ADDRESSING_16;
	Panel1170Control led_ADDRESSING_18;
	Panel1170Control led_ADDRESSING_22;

	// these are hardwired to the ADDR/DATA_SELECT knob
	// can be set over API, but are defined on each knob change
	Panel1170Control leds_ADDR_SELECT; // 8 leds for 8 positions, not on
										// physical panel
	Panel1170Control leds_DATA_SELECT; // 4 leds for 8 positions, not on
										// physical panel

	Panel1170Control knob_ADDR_SELECT;
	Panel1170Control knob_DATA_SELECT;

	Panel1170Control switch_panel_lock; // dummy: Blinkenlight control without
										// visual

	// the background
	TwoStateControlSliceVisualization backgroundVisualization;

	private ResourceManager resourceManager;

	private int scaledBackgroundWidth; // width of background image, after
										// load()

	/*
	 *
	 */
	public Panel1170(ResourceManager resourceManager) {
		controls = new ArrayList<Panel1170Control>();
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

		// define default states from command line
		// Evaluation in panel1170.clearUserinput()
		knob_ADDR_SELECT.resetState = commandlineParameters.getInt("addr_select");
		knob_DATA_SELECT.resetState = commandlineParameters.getInt("data_select");
		keyswitch.resetState = commandlineParameters.getInt("keyswitch");

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
		return new String("PDP-11/70 panel simulation (Blinkenlight API server interface) "
				+ Panelsim1170_app.version);
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

		p = new Panel("11/70"); // from blinkenlightd.conf, also compiled
								// into SimH
		p.info = "Photorealistic simulation of a PDP-11/70 panel. Java.";
		p.default_radix = 8;

		/*
		 * Build List of controls: interconnected lists of panel 11/70 controls
		 * and BlinkenlightAPI controls Control definitions exact like in
		 * blinkenlightd.conf! SimH relies on those!.
		 *
		 * Knob feed back LEds are hard wired to the knob positions.
		 * this is done in the "KNOB_LEED_FEEDBACK" control
		 */

		/*
		 * Keyswitch: One Java control, but two API controls
		 * Update GUI -> API in code, not by special class
		 */
		controls.add(keyswitch = new Panel1170Control(Panel1170ControlType.PDP11_KEYSWITCH,
				null, null, p));
		control_power = new Control("POWER", ControlType.input_switch, 1);
		p.addControl(control_power);
		control_panel_lock = new Control("PANEL_LOCK", ControlType.input_switch, 1);
		p.addControl(control_panel_lock);

		controls.add(switches_SR = new Panel1170Control(Panel1170ControlType.PDP11_SWITCH,
				new Control("SR", ControlType.input_switch, 22), null, p));
		controls.add(switch_LAMPTEST = new Panel1170Control(Panel1170ControlType.PDP11_KEY,
				null, null, p)); // LAMPTEST ohne Blinkenlight API control
		// controls.add(switch_LAMPTEST = new
		// Panel1170Control(Panel1170ControlType.PDP11_KEY,
		// new Control("LAMPTEST", ControlType.input_switch, 1), null, p));
		controls.add(switch_LOAD_ADRS = new Panel1170Control(Panel1170ControlType.PDP11_KEY,
				new Control("LOAD_ADRS", ControlType.input_switch, 1), null, p));
		controls.add(switch_EXAM = new Panel1170Control(Panel1170ControlType.PDP11_KEY,
				new Control("EXAM", ControlType.input_switch, 1), null, p));
		controls.add(switch_DEPOSIT = new Panel1170Control(Panel1170ControlType.PDP11_KEY,
				new Control("DEPOSIT", ControlType.input_switch, 1), null, p));
		controls.add(switch_CONT = new Panel1170Control(Panel1170ControlType.PDP11_KEY,
				new Control("CONT", ControlType.input_switch, 1), null, p));
		controls.add(switch_HALT = new Panel1170Control(Panel1170ControlType.PDP11_SWITCH,
				new Control("HALT", ControlType.input_switch, 1), null, p));
		controls.add(
				switch_S_BUS_CYCLE = new Panel1170Control(Panel1170ControlType.PDP11_SWITCH,
						new Control("S_BUS_CYCLE", ControlType.input_switch, 1), null, p));
		controls.add(switch_START = new Panel1170Control(Panel1170ControlType.PDP11_KEY,
				new Control("START", ControlType.input_switch, 1), null, p));

		controls.add(leds_ADDRESS = new Panel1170Control(Panel1170ControlType.PDP11_LAMP, null,
				new Control("ADDRESS", ControlType.output_lamp, 22), p));
		controls.add(leds_DATA = new Panel1170Control(Panel1170ControlType.PDP11_LAMP, null,
				new Control("DATA", ControlType.output_lamp, 16), p));
		controls.add(led_PARITY_HIGH = new Panel1170Control(Panel1170ControlType.PDP11_LAMP,
				null, new Control("PARITY_HIGH", ControlType.output_lamp, 1), p));
		controls.add(led_PARITY_LOW = new Panel1170Control(Panel1170ControlType.PDP11_LAMP,
				null, new Control("PARITY_LOW", ControlType.output_lamp, 1), p));
		controls.add(led_PAR_ERR = new Panel1170Control(Panel1170ControlType.PDP11_LAMP, null,
				new Control("PAR_ERR", ControlType.output_lamp, 1), p));
		controls.add(led_ADRS_ERR = new Panel1170Control(Panel1170ControlType.PDP11_LAMP, null,
				new Control("ADRS_ERR", ControlType.output_lamp, 1), p));
		controls.add(led_RUN = new Panel1170Control(Panel1170ControlType.PDP11_LAMP, null,
				new Control("RUN", ControlType.output_lamp, 1), p));
		controls.add(led_PAUSE = new Panel1170Control(Panel1170ControlType.PDP11_LAMP, null,
				new Control("PAUSE", ControlType.output_lamp, 1), p));
		controls.add(led_MASTER = new Panel1170Control(Panel1170ControlType.PDP11_LAMP, null,
				new Control("MASTER", ControlType.output_lamp, 1), p));
		controls.add(leds_MMR0_MODE = new Panel1170Control(Panel1170ControlType.PDP11_LAMP,
				null, new Control("MMR0_MODE", ControlType.output_lamp, 2), p));
		controls.add(led_DATA_SPACE = new Panel1170Control(Panel1170ControlType.PDP11_LAMP,
				null, new Control("DATA_SPACE", ControlType.output_lamp, 1), p));
		controls.add(led_ADDRESSING_16 = new Panel1170Control(Panel1170ControlType.PDP11_LAMP,
				null, new Control("ADDRESSING_16", ControlType.output_lamp, 1), p));
		controls.add(led_ADDRESSING_18 = new Panel1170Control(Panel1170ControlType.PDP11_LAMP,
				null, new Control("ADDRESSING_18", ControlType.output_lamp, 1), p));
		controls.add(led_ADDRESSING_22 = new Panel1170Control(Panel1170ControlType.PDP11_LAMP,
				null, new Control("ADDRESSING_22", ControlType.output_lamp, 1), p));
		// knob feedback LEDs are panel controls without Blinkenlight API
		// controls
		controls.add(leds_ADDR_SELECT = new Panel1170Control(Panel1170ControlType.PDP11_LAMP,
				null, null, p));
		controls.add(leds_DATA_SELECT = new Panel1170Control(Panel1170ControlType.PDP11_LAMP,
				null, null, p));

		controls.add(knob_ADDR_SELECT = new Panel1170Control(Panel1170ControlType.PDP11_KNOB,
				new Control("ADDR_SELECT", ControlType.input_knob, 3), null, p));
		controls.add(knob_DATA_SELECT = new Panel1170Control(Panel1170ControlType.PDP11_KNOB,
				new Control("DATA_SELECT", ControlType.input_knob, 2), null, p));

		// dummy: Blinkenlight control without visual. state
		controls.add(switch_panel_lock = new Panel1170Control(Panel1170ControlType.PDP11_SWITCH,
				new Control("PANEL_LOCK", ControlType.input_knob, 1), null, p));
		// switch_panel_lock.value = 0 ;

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
		backgroundVisualization = new TwoStateControlSliceVisualization("background.png", null, null,
				0);

		// clear visualization of all controls
		for (Panel1170Control panelcontrol : controls) {
			panelcontrol.visualization.clear();
		}

		// All coordinates must have been loaded: loadImageCoordinates()

		// keyswitch: positions 0,1,2
		MultiStateControlSliceVisualization msvc;
		// add knob states. no direct link to API controls
		msvc = new MultiStateControlSliceVisualization(keyswitch, "keyswitch");
		keyswitch.visualization.add(msvc);
		msvc.addStateImageFilename("keyswitch_off.png", 0); // off
		msvc.addStateImageFilename("keyswitch_power.png", 1); // power
		msvc.addStateImageFilename("keyswitch_lock.png", 2); // lock

		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_21_up.png",
				switches_SR, switches_SR.inputcontrol, 21));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_20_up.png",
				switches_SR, switches_SR.inputcontrol, 20));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_19_up.png",
				switches_SR, switches_SR.inputcontrol, 19));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_18_up.png",
				switches_SR, switches_SR.inputcontrol, 18));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_17_up.png",
				switches_SR, switches_SR.inputcontrol, 17));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_16_up.png",
				switches_SR, switches_SR.inputcontrol, 16));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_15_up.png",
				switches_SR, switches_SR.inputcontrol, 15));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_14_up.png",
				switches_SR, switches_SR.inputcontrol, 14));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_13_up.png",
				switches_SR, switches_SR.inputcontrol, 13));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_12_up.png",
				switches_SR, switches_SR.inputcontrol, 12));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_11_up.png",
				switches_SR, switches_SR.inputcontrol, 11));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_10_up.png",
				switches_SR, switches_SR.inputcontrol, 10));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_09_up.png",
				switches_SR, switches_SR.inputcontrol, 9));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_08_up.png",
				switches_SR, switches_SR.inputcontrol, 8));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_07_up.png",
				switches_SR, switches_SR.inputcontrol, 7));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_06_up.png",
				switches_SR, switches_SR.inputcontrol, 6));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_05_up.png",
				switches_SR, switches_SR.inputcontrol, 5));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_04_up.png",
				switches_SR, switches_SR.inputcontrol, 4));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_03_up.png",
				switches_SR, switches_SR.inputcontrol, 3));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_02_up.png",
				switches_SR, switches_SR.inputcontrol, 2));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_01_up.png",
				switches_SR, switches_SR.inputcontrol, 1));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr_00_up.png",
				switches_SR, switches_SR.inputcontrol, 0));

		switch_LAMPTEST.visualization.add(new TwoStateControlSliceVisualization(
				"switch_lamptest_up.png", switch_LAMPTEST, null, 0));
		switch_LOAD_ADRS.visualization
				.add(new TwoStateControlSliceVisualization("switch_load_adrs_down.png",
						switch_LOAD_ADRS, switch_LOAD_ADRS.inputcontrol, 0));
		switch_EXAM.visualization.add(new TwoStateControlSliceVisualization("switch_exam_down.png",
				switch_EXAM, switch_EXAM.inputcontrol, 0));
		switch_DEPOSIT.visualization.add(new TwoStateControlSliceVisualization("switch_dep_up.png",
				switch_DEPOSIT, switch_DEPOSIT.inputcontrol, 0));
		switch_CONT.visualization.add(new TwoStateControlSliceVisualization("switch_cont_down.png",
				switch_CONT, switch_CONT.inputcontrol, 0));
		switch_HALT.visualization.add(new TwoStateControlSliceVisualization(
				"switch_enable_halt_down.png", switch_HALT, switch_HALT.inputcontrol, 0));
		switch_S_BUS_CYCLE.visualization
				.add(new TwoStateControlSliceVisualization("switch_sinst_sbuscycle_down.png",
						switch_S_BUS_CYCLE, switch_S_BUS_CYCLE.inputcontrol, 0));
		switch_START.visualization.add(new TwoStateControlSliceVisualization("switch_start_down.png",
				switch_START, switch_START.inputcontrol, 0));

		DimmableLedControlSliceVisualization.defaultAveragingInterval_ms = 100;
		// create all LEDs with a low pass of 1/10 sec.
		// Critical are the "running light" and "idle pattern" use cases.

		// dimmable LEDs use variable transparency, so they do not dsiaturb the
		// background if
		// they are "dim"
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_21_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 21, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_20_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 20, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_19_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 19, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_18_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 18, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_17_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 17, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_16_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 16, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_15_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 15, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_14_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 14, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_13_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 13, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_12_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 12, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_11_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 11, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_10_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 10, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_09_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 9, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_08_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 8, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_07_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 7, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_06_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 6, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_05_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 5, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_04_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 4, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_03_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 3, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_02_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 2, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_01_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 1, true));
		leds_ADDRESS.visualization.add(new DimmableLedControlSliceVisualization("led_addr_00_on.png",
				leds_ADDRESS, leds_ADDRESS.outputcontrol, 0, true));

		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_15_on.png",
				leds_DATA, leds_DATA.outputcontrol, 15, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_14_on.png",
				leds_DATA, leds_DATA.outputcontrol, 14, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_13_on.png",
				leds_DATA, leds_DATA.outputcontrol, 13, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_12_on.png",
				leds_DATA, leds_DATA.outputcontrol, 12, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_11_on.png",
				leds_DATA, leds_DATA.outputcontrol, 11, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_10_on.png",
				leds_DATA, leds_DATA.outputcontrol, 10, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_09_on.png",
				leds_DATA, leds_DATA.outputcontrol, 9, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_08_on.png",
				leds_DATA, leds_DATA.outputcontrol, 8, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_07_on.png",
				leds_DATA, leds_DATA.outputcontrol, 7, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_06_on.png",
				leds_DATA, leds_DATA.outputcontrol, 6, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_05_on.png",
				leds_DATA, leds_DATA.outputcontrol, 5, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_04_on.png",
				leds_DATA, leds_DATA.outputcontrol, 4, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_03_on.png",
				leds_DATA, leds_DATA.outputcontrol, 3, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_02_on.png",
				leds_DATA, leds_DATA.outputcontrol, 2, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_01_on.png",
				leds_DATA, leds_DATA.outputcontrol, 1, true));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_data_00_on.png",
				leds_DATA, leds_DATA.outputcontrol, 0, true));

		led_PARITY_HIGH.visualization.add(new DimmableLedControlSliceVisualization("led_parity_high_on.png",
				led_PARITY_HIGH, led_PARITY_HIGH.outputcontrol, 0, true));
		led_PARITY_LOW.visualization.add(new DimmableLedControlSliceVisualization("led_parity_low_on.png",
				led_PARITY_LOW, led_PARITY_LOW.outputcontrol, 0, true));
		led_PAR_ERR.visualization.add(new DimmableLedControlSliceVisualization("led_par_err_on.png",
				led_PAR_ERR, led_PAR_ERR.outputcontrol, 0, true));
		led_ADRS_ERR.visualization.add(new DimmableLedControlSliceVisualization("led_adrs_err_on.png",
				led_ADRS_ERR, led_ADRS_ERR.outputcontrol, 0, true));
		led_RUN.visualization.add(new DimmableLedControlSliceVisualization("led_run_on.png", led_RUN,
				led_RUN.outputcontrol, 0, true));
		led_PAUSE.visualization.add(new DimmableLedControlSliceVisualization("led_pause_on.png", led_PAUSE,
				led_PAUSE.outputcontrol, 0, true));
		led_MASTER.visualization.add(new DimmableLedControlSliceVisualization("led_master_on.png",
				led_MASTER, led_MASTER.outputcontrol, 0, true));
		// MMR0: 0 = Kernel, 1= off, 2 = Super, 3 = User. NOT binary encoded,
		// but 1:n decoder
		leds_MMR0_MODE.visualization.add(new DimmableLedControlSliceVisualization("led_kernel_on.png",
				leds_MMR0_MODE, leds_MMR0_MODE.outputcontrol, 0, true));
		leds_MMR0_MODE.visualization.add(new DimmableLedControlSliceVisualization("led_super_on.png",
				leds_MMR0_MODE, leds_MMR0_MODE.outputcontrol, 2, true));
		leds_MMR0_MODE.visualization.add(new DimmableLedControlSliceVisualization("led_user_on.png",
				leds_MMR0_MODE, leds_MMR0_MODE.outputcontrol, 3, true));

		led_DATA_SPACE.visualization.add(new DimmableLedControlSliceVisualization("led_data_on.png",
				led_DATA_SPACE, led_DATA_SPACE.outputcontrol, 0, true));
		led_ADDRESSING_16.visualization
				.add(new DimmableLedControlSliceVisualization("led_addressing_16_on.png", led_ADDRESSING_16,
						led_ADDRESSING_16.outputcontrol, 0, true));
		led_ADDRESSING_18.visualization
				.add(new DimmableLedControlSliceVisualization("led_addressing_18_on.png", led_ADDRESSING_18,
						led_ADDRESSING_18.outputcontrol, 0, true));
		led_ADDRESSING_22.visualization
				.add(new DimmableLedControlSliceVisualization("led_addressing_22_on.png", led_ADDRESSING_22,
						led_ADDRESSING_22.outputcontrol, 0, true));

		// these are hardwired to the ADDR/DATA_SELECT knob
		// can be set over API, but are defined on each knob change
		// encoding fits the visual knob control state, NOT the physical knob
		// state
		// (because LEDs are feedback from visual controls and are not set over
		// Blinkenlight API)
		// led.state := (1 << knob.state)
		leds_ADDR_SELECT.visualization.add(new TwoStateControlSliceVisualization(
				"led_prog_phy_on.png", leds_ADDR_SELECT, leds_ADDR_SELECT.outputcontrol, 0));
		leds_ADDR_SELECT.visualization.add(new TwoStateControlSliceVisualization(
				"led_cons_phy_on.png", leds_ADDR_SELECT, leds_ADDR_SELECT.outputcontrol, 1));
		leds_ADDR_SELECT.visualization.add(new TwoStateControlSliceVisualization(
				"led_kernel_d_on.png", leds_ADDR_SELECT, leds_ADDR_SELECT.outputcontrol, 2));
		leds_ADDR_SELECT.visualization.add(new TwoStateControlSliceVisualization(
				"led_super_d_on.png", leds_ADDR_SELECT, leds_ADDR_SELECT.outputcontrol, 3));
		leds_ADDR_SELECT.visualization.add(new TwoStateControlSliceVisualization("led_user_d_on.png",
				leds_ADDR_SELECT, leds_ADDR_SELECT.outputcontrol, 4));
		leds_ADDR_SELECT.visualization.add(new TwoStateControlSliceVisualization("led_user_i_on.png",
				leds_ADDR_SELECT, leds_ADDR_SELECT.outputcontrol, 5));
		leds_ADDR_SELECT.visualization.add(new TwoStateControlSliceVisualization(
				"led_super_i_on.png", leds_ADDR_SELECT, leds_ADDR_SELECT.outputcontrol, 6));
		leds_ADDR_SELECT.visualization.add(new TwoStateControlSliceVisualization(
				"led_kernel_i_on.png", leds_ADDR_SELECT, leds_ADDR_SELECT.outputcontrol, 7));

		leds_DATA_SELECT.visualization.add(new TwoStateControlSliceVisualization(
				"led_bus_reg_on.png", leds_DATA_SELECT, leds_DATA_SELECT.outputcontrol, 0));
		leds_DATA_SELECT.visualization.add(new TwoStateControlSliceVisualization(
				"led_data_paths_on.png", leds_DATA_SELECT, leds_DATA_SELECT.outputcontrol, 1));
		leds_DATA_SELECT.visualization.add(new TwoStateControlSliceVisualization("led_u_adrs_on.png",
				leds_DATA_SELECT, leds_DATA_SELECT.outputcontrol, 2));
		leds_DATA_SELECT.visualization
				.add(new TwoStateControlSliceVisualization("led_display_register_on.png",
						leds_DATA_SELECT, leds_DATA_SELECT.outputcontrol, 3));

		// knobs: for rotation, state 0..7 must be circular.
		// encoding of physical panel is NOT! correct in
		// inputImageState2BlinkenlightApiControlValues()
		// add knob states
		msvc = new MultiStateControlSliceVisualization(knob_ADDR_SELECT,
				knob_ADDR_SELECT.inputcontrol);
		knob_ADDR_SELECT.visualization.add(msvc);
		msvc.addStateImageFilename("knob_addr_7_prog_phy.png", 0);
		msvc.addStateImageFilename("knob_addr_4_cons_phy.png", 1);
		msvc.addStateImageFilename("knob_addr_6_kernel_d.png", 2);
		msvc.addStateImageFilename("knob_addr_3_super_d.png", 3);
		msvc.addStateImageFilename("knob_addr_1_user_d.png", 4);
		msvc.addStateImageFilename("knob_addr_0_user_i.png", 5);
		msvc.addStateImageFilename("knob_addr_2_super_i.png", 6);
		msvc.addStateImageFilename("knob_addr_5_kernel_i.png", 7);

		// DATA knob has 8 positions, but encodes 0,1,2,3,0,1,2,3
		msvc = new MultiStateControlSliceVisualization(knob_DATA_SELECT,
				knob_DATA_SELECT.inputcontrol);
		knob_DATA_SELECT.visualization.add(msvc);
		msvc.addStateImageFilename("knob_data_2a_bus_reg.png", 0);
		msvc.addStateImageFilename("knob_data_3a_data_paths.png", 1);
		msvc.addStateImageFilename("knob_data_0a_u_adrs.png", 2);
		msvc.addStateImageFilename("knob_data_1a_display_register.png", 3);
		msvc.addStateImageFilename("knob_data_2b_bus_reg.png", 4);
		msvc.addStateImageFilename("knob_data_3b_data_paths.png", 5);
		msvc.addStateImageFilename("knob_data_0b_u_adrs.png", 6);
		msvc.addStateImageFilename("knob_data_1b_display_register.png", 7);
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
		ControlSliceStateImage.resourceImageFilePathPrefix = "blinkenbone/panelsim/panelsim1170/images/";
		ControlSliceStateImage.resourceImageFileNamePrefix = "pdp1170_size="
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
		for (Panel1170Control panelcontrol : controls)
			for (ControlSliceVisualization csv : panelcontrol.visualization) {
				// lamptest switch und knob feedback LEDs have no
				// blinkenlightApiControl!
				// if (csv.blinkenlightApiControl == null) {
				// System.out.printf("Panel definition error;
				// blinkenlightApiControl for controlVisualization '%s' not
				// found",
				// csv.name);
				// }
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
	public ControlSliceStateImage getTestStateImage(Panel1170Control panelcontrol,
			ControlSliceVisualization csv, int testmode) {

		switch (panelcontrol.type) {
		case PDP11_LAMP: // show brightest led/lamp image in all test modes
			return csv.getStateImage(csv.maxState);
		case PDP11_SWITCH: //
		case PDP11_KEY: //
		case PDP11_KEYSWITCH: //
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
		case PDP11_KNOB:
			switch (testmode) {
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST:
				return csv.getStateImage(csv.getState()); // why is this
															// necessary?
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST:
				return csv.getStateImage(csv.maxState);
			}
		}
		return null;
	}

	/*
	 * update the feedback LEDs for a knob position
	 */
	void syncKnobsFeedbackLeds() {

		int knobState;
		// knobs have only one visualization slice

		knobState = knob_ADDR_SELECT.visualization.get(0).getState();
		// setze die LED mit bitposition == knobstate
		for (ControlSliceVisualization ledCsv : leds_ADDR_SELECT.visualization)
			if (ledCsv.controlSlicePosition == knobState)
				ledCsv.setStateAveraging(ledCsv.maxState);
			else
				ledCsv.setStateAveraging(0);

		// DATA_SELECT: 4 LEDs for 8 positions
		knobState = knob_DATA_SELECT.visualization.get(0).getState() & 3;
		// setze die LED mit bitposition == knobstate
		for (ControlSliceVisualization ledCsv : leds_DATA_SELECT.visualization)
			if (ledCsv.controlSlicePosition == knobState)
				ledCsv.setStateAveraging(ledCsv.maxState);
			else
				ledCsv.setStateAveraging(0);
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
			for (Panel1170Control panelcontrol : controls)
				for (ControlSliceVisualization csv : panelcontrol.visualization) {
					cssi = null;
					// selftest over Blinkenlight API:
					if (blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST
							|| blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST) {
						cssi = getTestStateImage(panelcontrol, csv, blinkenlightApiPanel.mode);
					} else if (blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_POWERLESS
							&& panelcontrol.type == Panel1170ControlType.PDP11_LAMP) {
						cssi = null; // all lamps OFF = invisible
					} else {
						// show regular state
						// do not paint lamps or switches in OFF state,
						// background shows them already
						cssi = null;
						if (panelcontrol.type == Panel1170ControlType.PDP11_KNOB
								|| panelcontrol.type == Panel1170ControlType.PDP11_KEYSWITCH
								|| csv.getState() != 0)
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
	 * checks, whether image "cssi" of control slice "csv" was clicked
	 * if true, save event in csv.
	 */
	private boolean stateImageClicked(Point clickpoint, ControlSliceVisualization csv,
			ControlSliceStateImage cssi) {
		if (cssi.scaledRectangle.contains(clickpoint)
				// image transparency at clickpoint must be > 50%
				&& cssi.getPixelAt(clickpoint).getAlpha() > 128) {
			csv.clickedStateImage = cssi;
			if (csv.clickedStateImagePoint == null)
				csv.clickedStateImagePoint = new Point(); // only one needed
			csv.clickedStateImagePoint.x = (int) (clickpoint.x
					- cssi.scaledRectangle.getCenterX());
			csv.clickedStateImagePoint.y = (int) (clickpoint.y
					- cssi.scaledRectangle.getCenterY());
			return true;
		} else
			return false;
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
		for (Panel1170Control panelcontrol : controls)
			for (ControlSliceVisualization csv : panelcontrol.visualization) {
				ControlSliceStateImage cssi = csv.getVisibleStateImage();
				if (cssi != null && stateImageClicked(clickpoint, csv, cssi))
					return csv;
			}
		// no visible state image was clicked
		// but there may be the picture of an "inactive" control
		// be painted onto the background.
		//
		// Check, whether any state image of a ControlSliceVisualization
		// could be under the click point
		for (Panel1170Control panelcontrol : controls)
			for (ControlSliceVisualization csv : panelcontrol.visualization)
				for (ControlSliceStateImage cssi : csv.stateImages)
					if (stateImageClicked(clickpoint, csv, cssi))
						return csv;
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

		// System.out.printf("mouseDown(%s)%n", csv.name);
		// die "inputs" = buttons der KI10Controls sind ans csv gehängt!
		// und nicht die "outputs"!
		Panel1170Control panelcontrol = (Panel1170Control) csv.panelControl;
		switch (panelcontrol.type) {
		case PDP11_KNOB:
			if (csv.clickedStateImagePoint.x < 0) {
				// LEFT click in Knob Image = decrement,
				if (csv.getState() > csv.minState)
					csv.setStateExact(csv.getNextLowerState());
				else
					csv.setStateExact(csv.maxState); // rotate over 0
			} else {
				// RIGHT click = increment
				if (csv.getState() < csv.maxState)
					csv.setStateExact(csv.getNextHigherState());
				else
					csv.setStateExact(csv.minState); // rotate over 0
			}
			syncKnobsFeedbackLeds();
			break;
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
			if (panelcontrol == switch_LAMPTEST)
				setSelftest(rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST);
			break;
		case PDP11_KEYSWITCH:
			if (csv.clickedStateImagePoint.x < 0) {
				// click left of knob Image = decrement, no roll-around
				if (csv.getState() > csv.minState)
					csv.setStateExact(csv.getNextLowerState());
			} else {
				// click right of knob Image = increment, no roll-around
				if (csv.getState() < csv.maxState)
					csv.setStateExact(csv.getNextHigherState());
			}
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
		Panel1170Control panelcontrol = (Panel1170Control) csv.panelControl;
		switch (panelcontrol.type) {
		case PDP11_KEY:
			// deactivate button (= unpress) because mouse button is released
			csv.setStateExact(0);
			break;
		default:
			;
		}
		if (panelcontrol == switch_LAMPTEST)
			setSelftest(rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_NORMAL);
		// calc new control value
		inputImageState2BlinkenlightApiControlValues();
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
		for (Panel1170Control panelcontrol : controls)
			// default knob position can be set over command line
			if (panelcontrol == knob_ADDR_SELECT)
				knob_ADDR_SELECT.visualization.get(0)
						.setStateExact(knob_ADDR_SELECT.resetState);
			else if (panelcontrol == knob_DATA_SELECT)
				knob_DATA_SELECT.visualization.get(0)
						.setStateExact(knob_DATA_SELECT.resetState);
			else if (panelcontrol == keyswitch)
				keyswitch.visualization.get(0).setStateExact(keyswitch.resetState);
			else
				for (ControlSliceVisualization csv : panelcontrol.visualization) {
					csv.setStateExact(0);
				}
		syncKnobsFeedbackLeds();
		// calc new control values
		inputImageState2BlinkenlightApiControlValues();
	}

	/*
	 * decode input (Switches) image controls into BlinkenLight API control
	 * values.
	 * Decodes the knob states:
	 * Knob label visual control Blinkenlight API (same as physical panel)
	 * ----- -------- -------------- ------------
	 * ADDR prog phy 0 7
	 * cons phy 1 4
	 * kernel d 2 6
	 * super d 3 3
	 * user d 4 1
	 * user i 5 0
	 * super i 6 2
	 * kernel i 7 5
	 *
	 * DATA bus reg 0 3 a
	 * data paths 1 2 a
	 * u adrs 2 0 a
	 * display reg 3 1 a
	 * bus reg 4 3 b
	 * data paths 5 2 b
	 * u adrs 6 0 b
	 * display reg 7 1 b
	 *
	 * LED control:
	 */
	public void inputImageState2BlinkenlightApiControlValues() {

		for (Panel1170Control panelcontrol : controls) {
			Control c = panelcontrol.inputcontrol;
			if (c != null)
				synchronized (c) {
					// RPC server may not change control values in parallel
					c.value = 0; // init value
					if (panelcontrol == knob_ADDR_SELECT) {
						// knob has only 1 slice
						ControlSliceVisualization csv = panelcontrol.visualization.get(0);
						switch (csv.getState()) {
						case 0:
							c.value = 7;
							break;
						case 1:
							c.value = 4;
							break;
						case 2:
							c.value = 6;
							break;
						case 3:
							c.value = 3;
							break;
						case 4:
							c.value = 1;
							break;
						case 5:
							c.value = 0;
							break;
						case 6:
							c.value = 2;
							break;
						case 7:
							c.value = 5;
							break;
						}
					} else if (panelcontrol == knob_DATA_SELECT) {
						// knob has only 1 slice
						ControlSliceVisualization csv = panelcontrol.visualization.get(0);
						switch (csv.getState()) {
						case 0:
							c.value = 3;
							break;
						case 1:
							c.value = 2;
							break;
						case 2:
							c.value = 0;
							break;
						case 3:
							c.value = 1;
							break;
						case 4:
							c.value = 3;
							break;
						case 5:
							c.value = 2;
							break;
						case 6:
							c.value = 0;
							break;
						case 7:
							c.value = 1;
							break;
						}
					} else {
						// is a switch or switch bank.compose value of "active"
						// bit images
						c.value = 0; // init value
						for (ControlSliceVisualization csv : panelcontrol.visualization) {
							// all bit slices of this switch control
							if (csv.blinkenlightApiControl == c && csv.getState() != 0) {
								// control has a visible state. Switches are
								// only
								// ON/invisible, so any state give a "bit set"
								c.value |= (1 << csv.controlSlicePosition);
							}
						}
					}
				}
		}
		{
			// special case: the keyswitch has 2 API controls
			// keyswitch: 1 visualisation with 3 states
			control_power.value = control_panel_lock.value = 0; // default:
																// OFF
			switch (keyswitch.visualization.get(0).getState()) {
			case 1: // POWER
				control_power.value = 1;
				break;
			case 2: // LOCK
				control_power.value = control_panel_lock.value = 1;
				break;
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
		for (Panel1170Control panelcontrol : controls) {
			Control c = panelcontrol.outputcontrol;
			if (c != null)
				synchronized (c) {
					// RPC server may not change control values in parallel
					for (ControlSliceVisualization csv : panelcontrol.visualization)
						synchronized (csv) {
							// set visibility for all images of this control
							boolean visible = false;
							if (panelcontrol == leds_MMR0_MODE) {
								// one LED for each value
								if (c.value == csv.controlSlicePosition)
									visible = true;
							} else {
								// all other: LED image shows a single bit of
								// value
								// is bit "control_bitpos" set in value?
								if ((c.value & (1L << csv.controlSlicePosition)) > 0)
									visible = true;
							}
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
		ControlSliceVisualization keySwitchCsv = keyswitch.visualization.get(0);

		// default: do not change power button state
		int newKeySwitchState = keySwitchCsv.getState();

		if (mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_POWERLESS) {
			// all BlinkenLight API output controls to state 0
			// Alternatively, lamps can be painted as OFF in paintComponent()
			for (Panel1170Control panelcontrol : controls) {
				Control c = panelcontrol.outputcontrol;
				if (c != null)
					c.value = 0; // atomic, not synchronized
			}
			// Power button visual OFF
			newKeySwitchState = 0;
		} else {
			// make sure power button is ON in normal & test modes
			newKeySwitchState = 1;
		}
		// power button state: change only if user does not operate it
		if (currentMouseControlSliceVisualization != keySwitchCsv
				&& newKeySwitchState != keySwitchCsv.getState()) {
			// power button state changed
			keySwitchCsv.setStateExact(newKeySwitchState);
			// update API power switch control
			inputImageState2BlinkenlightApiControlValues();
		}

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
