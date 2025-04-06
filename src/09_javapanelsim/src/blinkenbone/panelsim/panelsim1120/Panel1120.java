/* Panel1120.java: A JPanel, which displays the Blinkenlight panel
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


   26-Apr-2016  JH		apply optional RescaleOp() on images after load, to modify brightness/contrast
   21-Apr-2016  JH      dec/inc of knobs changed from "left/right mouse button" to
                        "click coordinate left/right of image center"
   01-Apr-2016  JH      created


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

package blinkenbone.panelsim.panelsim1120;

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
import blinkenbone.panelsim.DimmableLightbulbControlSliceVisualization;
import blinkenbone.panelsim.MultiStateControlSliceVisualization;
import blinkenbone.panelsim.ResourceManager;
import blinkenbone.panelsim.TwoStateControlSliceVisualization;
import blinkenbone.panelsim.panelsim1120.Panel1120Control.Panel1120ControlType;
import blinkenbone.rpcgen.rpc_blinkenlight_api;

public class Panel1120 extends JPanel implements Observer {

	// public static String version = "v 1.01" ;
	private static final long serialVersionUID = 1L;

	/*
	 * frame between panel border and background image
	 */
	private int borderTopBottom = 15;
	private int borderLeftRight = 30;
	private Color borderColor = Color.gray;

	public Panel blinkenlightApiPanel; // the Blinkenlight API panel

	public ArrayList<Panel1120Control> controls;

	// links to well defined Blinkenlight Api controls
	Panel1120Control keyswitch; // optical: OFF,POWER,LOCK.
	// API 2 controls: 1 bit POWER, 1 bit lOCK
	Control control_power; // update programmatically
	Control control_panel_lock;

	Panel1120Control switches_SR;
	Panel1120Control switch_LOAD_ADDR;
	Panel1120Control switch_EXAM;
	Panel1120Control switch_CONT;
	Panel1120Control switch_HALT; // OFF = ENABLE
	Panel1120Control switch_SCYCLE; // OFF = SINST
	Panel1120Control switch_START;
	Panel1120Control switch_DEPOSIT;
	Panel1120Control lamps_ADDRESS;
	Panel1120Control lamps_DATA;
	Panel1120Control lamp_RUN;
	Panel1120Control lamp_BUS;
	Panel1120Control lamp_FETCH;
	Panel1120Control lamp_EXEC;
	Panel1120Control lamp_SOURCE;
	Panel1120Control lamp_DESTINATION;
	Panel1120Control lamps_ADDRESS_CYCLE;

	// the background
	TwoStateControlSliceVisualization backgroundVisualization;

	private ResourceManager resourceManager;

	private int scaledBackgroundWidth; // width of background image, after
										// load()

	/*
	 *
	 */
	public Panel1120(ResourceManager resourceManager) {
		controls = new ArrayList<Panel1120Control>();
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
		return new String("PDP-11/20 panel simulation (Blinkenlight API server interface) "
				+ Panelsim1120_app.version);
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

		p = new Panel("11/20"); // from blinkenlightd.conf, also compiled
								// into SimH
		p.info = "Photorealistic simulation of a PDP-11/20 panel. Java.";
		p.default_radix = 8;

		/*
		 * Build List of controls.
		 */

		/*
		 * Keyswitch: One Java control, but two API controls
		 * Update GUI -> API in code, not by special class
		 */
		controls.add(keyswitch = new Panel1120Control(Panel1120ControlType.PDP11_KEYSWITCH,
				null, null, p));
		control_power = new Control("POWER", ControlType.input_switch, 1);
		p.addControl(control_power);
		control_panel_lock = new Control("PANEL_LOCK", ControlType.input_switch, 1);
		p.addControl(control_panel_lock);

		controls.add(switches_SR = new Panel1120Control(Panel1120ControlType.PDP11_SWITCH,
				new Control("SR", ControlType.input_switch, 18), null, p));
		// API name as 11/70: LOAD ADRS
		controls.add(switch_LOAD_ADDR = new Panel1120Control(Panel1120ControlType.PDP11_KEY,
				new Control("LOAD_ADDR", ControlType.input_switch, 1), null, p));
		controls.add(switch_EXAM = new Panel1120Control(Panel1120ControlType.PDP11_KEY,
				new Control("EXAM", ControlType.input_switch, 1), null, p));
		controls.add(switch_CONT = new Panel1120Control(Panel1120ControlType.PDP11_KEY,
				new Control("CONT", ControlType.input_switch, 1), null, p));
		controls.add(switch_HALT = new Panel1120Control(Panel1120ControlType.PDP11_SWITCH,
				new Control("HALT", ControlType.input_switch, 1), null, p));
		controls.add(switch_SCYCLE = new Panel1120Control(Panel1120ControlType.PDP11_SWITCH,
				new Control("SCYCLE", ControlType.input_switch, 1), null, p));
		controls.add(switch_START = new Panel1120Control(Panel1120ControlType.PDP11_KEY,
				new Control("START", ControlType.input_switch, 1), null, p));
		controls.add(switch_DEPOSIT = new Panel1120Control(Panel1120ControlType.PDP11_KEY,
				new Control("DEPOSIT", ControlType.input_switch, 1), null, p));

		controls.add(lamps_ADDRESS = new Panel1120Control(Panel1120ControlType.PDP11_LAMP, null,
				new Control("ADDRESS", ControlType.output_lamp, 18), p));
		controls.add(lamps_DATA = new Panel1120Control(Panel1120ControlType.PDP11_LAMP, null,
				new Control("DATA", ControlType.output_lamp, 16), p));
		controls.add(lamp_RUN = new Panel1120Control(Panel1120ControlType.PDP11_LAMP, null,
				new Control("RUN", ControlType.output_lamp, 1), p));
		controls.add(lamp_BUS = new Panel1120Control(Panel1120ControlType.PDP11_LAMP, null,
				new Control("BUS", ControlType.output_lamp, 1), p));
		controls.add(lamp_FETCH = new Panel1120Control(Panel1120ControlType.PDP11_LAMP, null,
				new Control("FETCH", ControlType.output_lamp, 1), p));
		controls.add(lamp_EXEC = new Panel1120Control(Panel1120ControlType.PDP11_LAMP, null,
				new Control("EXEC", ControlType.output_lamp, 1), p));
		controls.add(lamp_SOURCE = new Panel1120Control(Panel1120ControlType.PDP11_LAMP, null,
				new Control("SOURCE", ControlType.output_lamp, 1), p));
		controls.add(lamp_DESTINATION = new Panel1120Control(Panel1120ControlType.PDP11_LAMP,
				null, new Control("DESTINATION", ControlType.output_lamp, 1), p));
		controls.add(lamps_ADDRESS_CYCLE = new Panel1120Control(Panel1120ControlType.PDP11_LAMP,
				null, new Control("ADDRESS_CYCLE", ControlType.output_lamp, 2), p));

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
		for (Panel1120Control panelcontrol : controls) {
			panelcontrol.visualization.clear();
		}

		// All coordinates must have been loaded: loadImageCoordinates()
		// first file: "inactive", 2nd file" active" state

		// keyswitch: positions 0,1,2
		MultiStateControlSliceVisualization msvc;
		// add knob states. no direct link to API controls
		msvc = new MultiStateControlSliceVisualization(keyswitch, "keyswitch");
		keyswitch.visualization.add(msvc);
		msvc.addStateImageFilename("keyswitch_off.png", 0); // off
		msvc.addStateImageFilename("keyswitch_power.png", 1); // power
		msvc.addStateImageFilename("keyswitch_lock.png", 2); // lock

        // "down" = 0, "up" = 1
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr17_down.png",
				"switch_sr17_up.png", switches_SR, switches_SR.inputcontrol, 17));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr16_down.png",
				"switch_sr16_up.png", switches_SR, switches_SR.inputcontrol, 16));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr15_down.png",
				"switch_sr15_up.png", switches_SR, switches_SR.inputcontrol, 15));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr14_down.png",
				"switch_sr14_up.png", switches_SR, switches_SR.inputcontrol, 14));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr13_down.png",
				"switch_sr13_up.png", switches_SR, switches_SR.inputcontrol, 13));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr12_down.png",
				"switch_sr12_up.png", switches_SR, switches_SR.inputcontrol, 12));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr11_down.png",
				"switch_sr11_up.png", switches_SR, switches_SR.inputcontrol, 11));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr10_down.png",
				"switch_sr10_up.png", switches_SR, switches_SR.inputcontrol, 10));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr09_down.png",
				"switch_sr09_up.png", switches_SR, switches_SR.inputcontrol, 9));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr08_down.png",
				"switch_sr08_up.png", switches_SR, switches_SR.inputcontrol, 8));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr07_down.png",
				"switch_sr07_up.png", switches_SR, switches_SR.inputcontrol, 7));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr06_down.png",
				"switch_sr06_up.png", switches_SR, switches_SR.inputcontrol, 6));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr05_down.png",
				"switch_sr05_up.png", switches_SR, switches_SR.inputcontrol, 5));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr04_down.png",
				"switch_sr04_up.png", switches_SR, switches_SR.inputcontrol, 4));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr03_down.png",
				"switch_sr03_up.png", switches_SR, switches_SR.inputcontrol, 3));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr02_down.png",
				"switch_sr02_up.png", switches_SR, switches_SR.inputcontrol, 2));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr01_down.png",
				"switch_sr01_up.png", switches_SR, switches_SR.inputcontrol, 1));
		switches_SR.visualization.add(new TwoStateControlSliceVisualization("switch_sr00_down.png",
				"switch_sr00_up.png", switches_SR, switches_SR.inputcontrol, 0));

		// command switches
		switch_LOAD_ADDR.visualization.add(new TwoStateControlSliceVisualization(
				"switch_load_addr_up.png", "switch_load_addr_down.png", switch_LOAD_ADDR,
				switch_LOAD_ADDR.inputcontrol, 0));
		switch_EXAM.visualization.add(new TwoStateControlSliceVisualization("switch_exam_up.png",
				"switch_exam_down.png", switch_EXAM, switch_EXAM.inputcontrol, 0));
		switch_CONT.visualization.add(new TwoStateControlSliceVisualization("switch_cont_up.png",
				"switch_cont_down.png", switch_CONT, switch_CONT.inputcontrol, 0));
		switch_HALT.visualization.add(new TwoStateControlSliceVisualization(
				"switch_enable_halt_up.png", "switch_enable_halt_down.png", switch_HALT,
				switch_HALT.inputcontrol, 0));
		switch_SCYCLE.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sinst_scycle_up.png", "switch_sinst_scycle_down.png", switch_SCYCLE,
				switch_SCYCLE.inputcontrol, 0));
		switch_START.visualization.add(new TwoStateControlSliceVisualization("switch_start_up.png",
				"switch_start_down.png", switch_START, switch_START.inputcontrol, 0));
		// DEPOSIT: aktive = up, inaktive=down
		switch_DEPOSIT.visualization.add(new TwoStateControlSliceVisualization("switch_dep_down.png",
				"switch_dep_up.png", switch_DEPOSIT, switch_DEPOSIT.inputcontrol, 0));

		// create all lightbulbs with a low pass of 1/4 sec.
		// Critical are the "running light" and "idle pattern" use cases.
		DimmableLightbulbControlSliceVisualization.defaultAveragingInterval_ms = 250;

		// lightbulbs darker and more contrast
		// DimmableLightbulbVisualization.defaultImageRescaleopScale = 1.3f ; // more contrast
		// DimmableLightbulbVisualization.defaultImageRescaleopOffset = -110 ; // darker


		// "DimmableLightbulb" use variable transparency, so they do not disturb the
		// background if they are "dim"
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr17_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 17, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr16_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 16, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr15_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 15, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr14_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 14, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr13_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 13, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr12_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 12, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr11_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 11, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr10_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 10, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr09_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 9, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr08_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 8, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr07_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 7, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr06_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 6, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr05_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 5, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr04_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 4, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr03_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 3, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr02_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 2, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr01_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 1, true));
		lamps_ADDRESS.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_addr00_on.png",
						lamps_ADDRESS, lamps_ADDRESS.outputcontrol, 0, true));

		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data15_on.png", lamps_DATA, lamps_DATA.outputcontrol, 15, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data14_on.png", lamps_DATA, lamps_DATA.outputcontrol, 14, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data13_on.png", lamps_DATA, lamps_DATA.outputcontrol, 13, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data12_on.png", lamps_DATA, lamps_DATA.outputcontrol, 12, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data11_on.png", lamps_DATA, lamps_DATA.outputcontrol, 11, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data10_on.png", lamps_DATA, lamps_DATA.outputcontrol, 10, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data09_on.png", lamps_DATA, lamps_DATA.outputcontrol, 9, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data08_on.png", lamps_DATA, lamps_DATA.outputcontrol, 8, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data07_on.png", lamps_DATA, lamps_DATA.outputcontrol, 7, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data06_on.png", lamps_DATA, lamps_DATA.outputcontrol, 6, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data05_on.png", lamps_DATA, lamps_DATA.outputcontrol, 5, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data04_on.png", lamps_DATA, lamps_DATA.outputcontrol, 4, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data03_on.png", lamps_DATA, lamps_DATA.outputcontrol, 3, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data02_on.png", lamps_DATA, lamps_DATA.outputcontrol, 2, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data01_on.png", lamps_DATA, lamps_DATA.outputcontrol, 1, true));
		lamps_DATA.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_data00_on.png", lamps_DATA, lamps_DATA.outputcontrol, 0, true));

		lamp_RUN.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_run_on.png",
				lamp_RUN, lamp_RUN.outputcontrol, 0, true));
		lamp_BUS.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_bus_on.png",
				lamp_BUS, lamp_BUS.outputcontrol, 0, true));
		lamp_FETCH.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_fetch_on.png", lamp_FETCH, lamp_FETCH.outputcontrol, 0, true));
		lamp_EXEC.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_exec_on.png",
				lamp_EXEC, lamp_EXEC.outputcontrol, 0, true));
		lamp_SOURCE.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_source_on.png", lamp_SOURCE, lamp_SOURCE.outputcontrol, 0, true));
		lamp_DESTINATION.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_destination_on.png",
						lamp_DESTINATION, lamp_DESTINATION.outputcontrol, 0, true));
		lamps_ADDRESS_CYCLE.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_address1_on.png",
						lamps_ADDRESS_CYCLE, lamps_ADDRESS_CYCLE.outputcontrol, 1, true));
		lamps_ADDRESS_CYCLE.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_address0_on.png",
						lamps_ADDRESS_CYCLE, lamps_ADDRESS_CYCLE.outputcontrol, 0, true));

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
		ControlSliceStateImage.resourceImageFilePathPrefix = "blinkenbone/panelsim/panelsim1120/images/";
		ControlSliceStateImage.resourceImageFileNamePrefix = "pdp1120_size="
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
		for (Panel1120Control panelcontrol : controls)
			for (ControlSliceVisualization csv : panelcontrol.visualization) {
				// System.out.println("csv.name = " + csv.name) ;
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
	public ControlSliceStateImage getTestStateImage(Panel1120Control panelcontrol,
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
			for (Panel1120Control panelcontrol : controls)
				for (ControlSliceVisualization csv : panelcontrol.visualization) {
					cssi = null;
					// selftest over Blinkenlight API:
					if (blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST
							|| blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST) {
						cssi = getTestStateImage(panelcontrol, csv, blinkenlightApiPanel.mode);
					} else if (blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_POWERLESS
							&& panelcontrol.type == Panel1120ControlType.PDP11_LAMP) {
						cssi = null; // all lamps OFF = invisible
					} else {
						// show regular state
						// do not paint lamps or switches in OFF state,
						// background shows them already
						// cssi = null;
						// if (csv.getState() != 0)
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
		for (Panel1120Control panelcontrol : controls)
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
		for (Panel1120Control panelcontrol : controls)
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

		Panel1120Control panelcontrol = (Panel1120Control) csv.panelControl;
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
			/*
			 * if (mouseButton == MouseEvent.BUTTON1) {
			 * // LEFT click in Knob Image = decrement, no roll-around
			 * if (csv.getState() > csv.minState)
			 * csv.setStateExact(csv.getNextLowerState());
			 * } else if (mouseButton == MouseEvent.BUTTON3) {
			 * // RIGHT click = increment, no roll-around
			 * if (csv.getState() < csv.maxState)
			 * csv.setStateExact(csv.getNextHigherState());
			 * }
			 */
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
		Panel1120Control panelcontrol = (Panel1120Control) csv.panelControl;
		switch (panelcontrol.type) {
		case PDP11_KEY:
			// deactivate button (= unpress) because mouse button is released
			csv.setStateExact(0);
			inputImageState2BlinkenlightApiControlValues(); // calc new control
															// value
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
		// default keyswitch position can be set over commandline
		for (Panel1120Control panelcontrol : controls) {
			if (panelcontrol == keyswitch)
				keyswitch.visualization.get(0).setStateExact(keyswitch.resetState);
			else
				for (ControlSliceVisualization csv : panelcontrol.visualization) {
					csv.setStateExact(0);
				}
		}
		// calc new control values
		inputImageState2BlinkenlightApiControlValues();
	}

	/*
	 * decode input (Switches) image controls into BlinkenLight API control
	 * values.
	 */
	public void inputImageState2BlinkenlightApiControlValues() {

		for (Panel1120Control panelcontrol : controls) {
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
		for (Panel1120Control panelcontrol : controls) {
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
							assert (csv.getClass() == DimmableLightbulbControlSliceVisualization.class);
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
