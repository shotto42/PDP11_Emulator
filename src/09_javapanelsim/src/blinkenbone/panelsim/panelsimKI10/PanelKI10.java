/* PanelKI10.java: A JPanel, which displays the Blinkenlight panel
 					as stack of ControlImagesDescription

   Copyright (c) 2014-2016, Joerg Hoppe
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

   21-Apr-2016  JH      dec/inc of knobs changed from "left/right mouse button" to
                        "click coordinate left/right of image center"
   28-Feb-2016  JH      Visualization load logic separated from image load.
   20-Feb-2016  JH      added PANEL_MODE_POWERLESS
   15-Oct-2014  JH      created


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
package blinkenbone.panelsim.panelsimKI10;

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
import blinkenbone.panelsim.DimmableLightbulbControlSliceVisualization;
import blinkenbone.panelsim.LampButtonControlSliceVisualization;
import blinkenbone.panelsim.MultiStateControlSliceVisualization;
import blinkenbone.panelsim.ResourceManager;
import blinkenbone.panelsim.TwoStateControlSliceVisualization;
import blinkenbone.panelsim.panelsimKI10.PanelKI10Control.KI10ControlType;
import blinkenbone.rpcgen.rpc_blinkenlight_api;

public class PanelKI10 extends JPanel implements Observer {
	/**
	 *
	 */
	// public static String version = "v 1.01" ;
	private static final long serialVersionUID = 1L;

	/*
	 * frame between panel border and background image
	 */
	private int borderTopBottom = 0;
	private int borderLeftRight = 0;
	private Color borderColor = Color.gray;

	public Panel blinkenlightApiPanel; // the Blinkenlight API panel

	public ArrayList<PanelKI10Control> controls;

	// links to well defined Blinkenlight Api controls
	PanelKI10Control lamp_OVERTEMP;
	PanelKI10Control lamp_CKT_BRKR_TRIPPED;
	PanelKI10Control lamp_DOORS_OPEN;
	PanelKI10Control knob_MARGIN_SELECT;
	PanelKI10Control knob_IND_SELECT;
	PanelKI10Control knob_SPEED_CONTROL_COARSE;
	PanelKI10Control knob_SPEED_CONTROL_FINE;
	PanelKI10Control knob_MARGIN_VOLTAGE;
	PanelKI10Control VOLTMETER;
	PanelKI10Control enable_HOURMETER;
	PanelKI10Control button_FM_MANUAL;
	PanelKI10Control buttons_FM_BLOCK;
	PanelKI10Control buttons_SENSE;
	PanelKI10Control button_MI_PROG_DIS;
	PanelKI10Control button_MEM_OVERLAP_DIS;
	PanelKI10Control button_SINGLE_PULSE;
	PanelKI10Control button_MARGIN_ENABLE;
	PanelKI10Control buttons_MANUAL_MARGIN_ADDRESS;
	PanelKI10Control buttons_READ_IN_DEVICE;
	PanelKI10Control button_LAMP_TEST;
	PanelKI10Control button_CONSOLE_LOCK;
	PanelKI10Control button_CONSOLE_DATALOCK;
	PanelKI10Control button_POWER;

	PanelKI10Control leds_PI_ACTIVE;
	PanelKI10Control leds_IOB_PI_REQUEST;
	PanelKI10Control leds_PI_IN_PROGRESS;
	PanelKI10Control leds_PI_REQUEST;
	PanelKI10Control led_PI_ON;
	PanelKI10Control led_PI_OK_8;
	PanelKI10Control leds_MODE;
	PanelKI10Control led_KEY_PG_FAIL;
	PanelKI10Control led_KEY_MAINT;
	PanelKI10Control leds_STOP;
	PanelKI10Control led_RUN;
	PanelKI10Control led_POWER;
	PanelKI10Control leds_PROGRAM_COUNTER;
	PanelKI10Control leds_INSTRUCTION;
	PanelKI10Control led_PROGRAM_DATA;
	PanelKI10Control led_MEMORY_DATA;
	PanelKI10Control leds_DATA;

	PanelKI10Control button_PAGING_EXEC;
	PanelKI10Control button_PAGING_USER;
	PanelKI10Control buttons_ADDRESS;
	PanelKI10Control button_ADDRESS_CLEAR;
	PanelKI10Control button_ADDRESS_LOAD;
	PanelKI10Control buttons_DATA;
	PanelKI10Control button_DATA_CLEAR;
	PanelKI10Control button_DATA_LOAD;
	PanelKI10Control button_SINGLE_INST;
	PanelKI10Control button_SINGLE_PULSER;
	PanelKI10Control button_STOP_PAR;
	PanelKI10Control button_STOP_NXM;
	PanelKI10Control button_REPEAT;
	PanelKI10Control button_FETCH_INST;
	PanelKI10Control button_FETCH_DATA;
	PanelKI10Control button_WRITE;
	PanelKI10Control button_ADDRESS_STOP;
	PanelKI10Control button_ADDRESS_BREAK;
	PanelKI10Control button_READ_IN;
	PanelKI10Control button_START;
	PanelKI10Control button_CONT;
	PanelKI10Control button_STOP;
	PanelKI10Control button_RESET;
	PanelKI10Control button_XCT;
	PanelKI10Control button_EXAMINE_THIS;
	PanelKI10Control button_EXAMINE_NEXT;
	PanelKI10Control button_DEPOSIT_THIS;
	PanelKI10Control button_DEPOSIT_NEXT;

	// the background
	TwoStateControlSliceVisualization backgroundVisualization;

	private ResourceManager resourceManager;

	private int scaledBackgroundWidth; // width of background image, after
										// load()

	/*
	 *
	 */
	public PanelKI10(ResourceManager resourceManager) {
		controls = new ArrayList<PanelKI10Control>();
		this.resourceManager = resourceManager;
		// Create the Blinkenlight API panel
		this.blinkenlightApiPanel = constructBlinkenlightApiPanel();
		// blinkenlightApiPanel calls update() on change of output controls
		this.blinkenlightApiPanel.addObserver(this);
	}

	/*
	 * load images for background width "scaledWith()
	 */
	public void init(JSAPResult commandlineParameters) {
		// ! controlImages is null in WindowsBuilder designer!
		loadControlVisualizations();
		// knows which pictures and controls to load

		// define default states from command line
		// Evaluation in panelPDP8I.clearUserinput()
		button_POWER.resetState = commandlineParameters.getInt("power");

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
		return new String("PDP-10/KI10 panel simulation (Blinkenlight API server interface) "
				+ PanelsimKI10_app.version);
	}

	/*
	 * return user-selectable Widths
	 */
	public Integer[] getSupportedWidths() {
		return new Integer[] { 1200, 1340, 1580, 1900, 2500, 2950 };
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

		p = new Panel("PDP10-KI10"); // from blinkenlichd.conf, also compiled
										// into SimH
		p.info = "Photorealistic simulation of a PDP-10/KI10 panel. Java.";
		p.default_radix = 8;

		/*
		 * Build List of controls: interconnected lists of panel KI10 controls
		 * and BlinkenlightAPI controls Control definitions exact like in
		 * blinkenlightd.conf! SimH relies on those!.
		 *
		 * Mechanical Types of KI10 controls: "KEY" : button without lock-in
		 * "SWITCH" : button with lock-in. Must be handled here! lamp VU meter,
		 * counter, knob ?
		 */
		controls.add(lamp_OVERTEMP = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("OVERTEMP", ControlType.output_lamp, 1), p));

		controls.add(lamp_CKT_BRKR_TRIPPED = new PanelKI10Control(KI10ControlType.PDP10_LAMP,
				null, new Control("CKT_BRKR_TRIPPED", ControlType.output_lamp, 1), p));
		controls.add(lamp_DOORS_OPEN = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("DOORS_OPEN", ControlType.output_lamp, 1), p));

		// 8 position rotary switch, encoded 0..7
		controls.add(knob_MARGIN_SELECT = new PanelKI10Control(KI10ControlType.PDP10_KNOB,
				new Control("MARGIN_SELECT", ControlType.input_knob, 3), null, p));
		// 4 position rotary switch. encoded 0..3
		controls.add(knob_IND_SELECT = new PanelKI10Control(KI10ControlType.PDP10_KNOB,
				new Control("IND_SELECT", ControlType.input_knob, 2), null, p));
		// 6 position rotary switch, encoded 0..5
		controls.add(knob_SPEED_CONTROL_COARSE = new PanelKI10Control(
				KI10ControlType.PDP10_KNOB,
				new Control("SPEED_CONTROL_COARSE", ControlType.input_knob, 3), null, p));
		// analog potentiometer. encoded as 0..100%
		controls.add(knob_SPEED_CONTROL_FINE = new PanelKI10Control(KI10ControlType.PDP10_KNOB,
				new Control("SPEED_CONTROL_FINE", ControlType.input_knob, 7), null, p));
		// autotransformer. 0..255, as 45 = 4.5 Volt .. 160 = 16.0 Volt
		controls.add(knob_MARGIN_VOLTAGE = new PanelKI10Control(KI10ControlType.PDP10_KNOB,
				new Control("MARGIN_VOLTAGE", ControlType.input_knob, 8), null, p));
		// analog VU meter, encoded as 14..120%
		controls.add(VOLTMETER = new PanelKI10Control(KI10ControlType.PDP10_OUTPUT, null,
				new Control("VOLTMETER", ControlType.output_pointer_instrument, 7), p));
		// enable counter for operating hours, 0/1
		controls.add(enable_HOURMETER = new PanelKI10Control(KI10ControlType.PDP10_OUTPUT, null,
				new Control("HOURMETER", ControlType.output_other, 1), p));

		// push button feedback
		controls.add(button_FM_MANUAL = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("FM_MANUAL_SW", ControlType.input_switch, 1),
				new Control("FM_MANUAL_FB", ControlType.output_lamp, 1), p));
		// 2 bit push button feedback
		controls.add(buttons_FM_BLOCK = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("FM_BLOCK_SW", ControlType.input_switch, 2),
				new Control("FM_BLOCK_FB", ControlType.output_lamp, 2), p));
		// 6 bit locking push buttons: 1..6
		controls.add(buttons_SENSE = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("SENSE_SW", ControlType.input_switch, 6),
				new Control("SENSE_FB", ControlType.output_lamp, 6), p));
		// 1 bit push button feedback
		controls.add(button_MI_PROG_DIS = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("MI_PROG_DIS_SW", ControlType.input_switch, 1),
				new Control("MI_PROG_DIS_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button feedback
		controls.add(button_MEM_OVERLAP_DIS = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("MEM_OVERLAP_DIS_SW", ControlType.input_switch, 1),
				new Control("MEM_OVERLAP_DIS_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button feedback
		controls.add(button_SINGLE_PULSE = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("SINGLE_PULSE_SW", ControlType.input_switch, 1),
				new Control("SINGLE_PULSE_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button feedback
		controls.add(button_MARGIN_ENABLE = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("MARGIN_ENABLE_SW", ControlType.input_switch, 1),
				new Control("MARGIN_ENABLE_FB", ControlType.output_lamp, 1), p));
		// 5 bit push button: 0..4
		controls.add(buttons_MANUAL_MARGIN_ADDRESS = new PanelKI10Control(
				KI10ControlType.PDP10_KEY,
				new Control("MANUAL_MARGIN_ADDRESS_SW", ControlType.input_switch, 5),
				new Control("MANUAL_MARGIN_ADDRESS_FB", ControlType.output_lamp, 5), p));
		// toggle, aber selber rastend!
		// 7 bit locking push buttons: READ_IN_DEVICE 3..9
		controls.add(buttons_READ_IN_DEVICE = new PanelKI10Control(KI10ControlType.PDP10_SWITCH,
				new Control("READ_IN_DEVICE_SW", ControlType.input_switch, 7),
				new Control("READ_IN_DEVICE_FB", ControlType.output_lamp, 7), p));
		// 1 bit push button feedback
		// No feed back lamp!
		controls.add(button_LAMP_TEST = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("LAMP_TEST_SW", ControlType.input_switch, 1), null, p));
		// 1 bit push button feedback
		// feedback is hard wired to switch in cod and not settable over
		// blinkenlight API!
		controls.add(button_CONSOLE_LOCK = new PanelKI10Control(KI10ControlType.PDP10_SWITCH,
				new Control("CONSOLE_LOCK_SW", ControlType.input_switch, 1), null, p));
		// 1 bit push button feedback
		// feedback is hard wired to switch in code and not settable over
		// blinkenlight API!
		controls.add(button_CONSOLE_DATALOCK = new PanelKI10Control(
				KI10ControlType.PDP10_SWITCH,
				new Control("CONSOLE_DATALOCK_SW", ControlType.input_switch, 1), null, p));
		// 1 bit push button
		// NOT settable over blinkenlight API!
		controls.add(button_POWER = new PanelKI10Control(KI10ControlType.PDP10_SWITCH,
				new Control("POWERBUTTON_SW", ControlType.input_switch, 1), null, p));

		// ---------- LEDs on lower panel --------------------------
		// 7 LEDs, cable label "PION"
		controls.add(leds_PI_ACTIVE = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("PI_ACTIVE", ControlType.output_lamp, 7), p));
		// 7 LEDs, cable label "BIOPI"
		controls.add(leds_IOB_PI_REQUEST = new PanelKI10Control(KI10ControlType.PDP10_LAMP,
				null, new Control("IOB_PI_REQUEST", ControlType.output_lamp, 7), p));
		// 7 LEDs, cable label "PIH"
		controls.add(leds_PI_IN_PROGRESS = new PanelKI10Control(KI10ControlType.PDP10_LAMP,
				null, new Control("PI_IN_PROGRESS", ControlType.output_lamp, 7), p));
		// 7 LEDs, cable label "PIR"
		controls.add(leds_PI_REQUEST = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("PI_REQUEST", ControlType.output_lamp, 7), p));
		// 1 LED, cable label "PION"
		controls.add(led_PI_ON = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("PI_ON", ControlType.output_lamp, 1), p));
		// 1 LED, cable label "PIOK"
		controls.add(led_PI_OK_8 = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("PI_OK_8", ControlType.output_lamp, 1), p));

		// 4 LED, cable label "MODE".
		// 0x01 = EXEC_MODE_KERNEL, 0x02 = EXEC_MODE_SUPER, 0x04 =
		// USER_MODE_CONCEAL, 0x08 = USER_MODE_PUBLIC
		controls.add(leds_MODE = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("MODE", ControlType.output_lamp, 4), p));
		// 1 LED, cable label "KEY"
		controls.add(led_KEY_PG_FAIL = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("KEY_PG_FAIL", ControlType.output_lamp, 1), p));
		// 1 LED, cable label "KEY"
		controls.add(led_KEY_MAINT = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("KEY_MAINT", ControlType.output_lamp, 1), p));
		// 3 LED, cable label "STOP"
		// 0x01 = STOP_MEM, 0x02 = STOP_PROG, 0x04 = STOP_MAN
		controls.add(leds_STOP = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("STOP", ControlType.output_lamp, 3), p));
		// 1 LED, cable label "RUN"
		controls.add(led_RUN = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("RUN", ControlType.output_lamp, 1), p));
		// 1 LED, cable label "PWR"
		controls.add(led_POWER = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("POWER", ControlType.output_lamp, 1), p));

		// 18 LEDs, cable label "PC"
		controls.add(leds_PROGRAM_COUNTER = new PanelKI10Control(KI10ControlType.PDP10_LAMP,
				null, new Control("PROGRAM_COUNTER", ControlType.output_lamp, 18), p));
		// 36 LEDs, cable label "OPCODE"
		controls.add(leds_INSTRUCTION = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("INSTRUCTION", ControlType.output_lamp, 36), p));
		// 1 LEDs, cable label "PRGDAT"
		controls.add(led_PROGRAM_DATA = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("PROGRAM_DATA", ControlType.output_lamp, 1), p));
		// 1 LEDs, cable label "MEMDAT"
		controls.add(led_MEMORY_DATA = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("MEMORY_DATA", ControlType.output_lamp, 1), p));
		// 36 LEDs, cable label "DATA"
		controls.add(leds_DATA = new PanelKI10Control(KI10ControlType.PDP10_LAMP, null,
				new Control("DATA", ControlType.output_lamp, 36), p));

		// ----- buttons on lower panel --------------
		// "In the upper half of the operator panel are four rows of indicators,
		// and below them are three
		// rows of two-position keys and switches. Physically both are
		// pushbuttons, but the keys are
		// momentary contact whereas the switches are alternate action.""
		// 1 bit push button
		controls.add(button_PAGING_EXEC = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("PAGING_EXEC_SW", ControlType.input_switch, 1),
				new Control("PAGING_EXEC_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_PAGING_USER = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("PAGING_USER_SW", ControlType.input_switch, 1),
				new Control("PAGING_USER_FB", ControlType.output_lamp, 1), p));
		// 22 bit push button, for bits 35..14
		controls.add(buttons_ADDRESS = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("ADDRESS_SW", ControlType.input_switch, 22),
				new Control("ADDRESS_FB", ControlType.output_lamp, 22), p));
		// 1 bit push button
		controls.add(button_ADDRESS_CLEAR = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("ADDRESS_CLEAR_SW", ControlType.input_switch, 1),
				new Control("ADDRESS_CLEAR_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_ADDRESS_LOAD = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("ADDRESS_LOAD_SW", ControlType.input_switch, 1),
				new Control("ADDRESS_LOAD_FB", ControlType.output_lamp, 1), p));
		// 36 bit push button
		controls.add(buttons_DATA = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("DATA_SW", ControlType.input_switch, 36),
				new Control("DATA_FB", ControlType.output_lamp, 36), p));
		// 1 bit push button
		controls.add(button_DATA_CLEAR = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("DATA_CLEAR_SW", ControlType.input_switch, 1),
				new Control("DATA_CLEAR_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_DATA_LOAD = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("DATA_LOAD_SW", ControlType.input_switch, 1),
				new Control("DATA_LOAD_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_SINGLE_INST = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("SINGLE_INST_SW", ControlType.input_switch, 1),
				new Control("SINGLE_INST_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_SINGLE_PULSER = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("SINGLE_PULSER_SW", ControlType.input_switch, 1),
				new Control("SINGLE_PULSER_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_STOP_PAR = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("STOP_PAR_SW", ControlType.input_switch, 1),
				new Control("STOP_PAR_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_STOP_NXM = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("STOP_NXM_SW", ControlType.input_switch, 1),
				new Control("STOP_NXM_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_REPEAT = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("REPEAT_SW", ControlType.input_switch, 1),
				new Control("REPEAT_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_FETCH_INST = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("FETCH_INST_SW", ControlType.input_switch, 1),
				new Control("FETCH_INST_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_FETCH_DATA = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("FETCH_DATA_SW", ControlType.input_switch, 1),
				new Control("FETCH_DATA_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_WRITE = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("WRITE_SW", ControlType.input_switch, 1),
				new Control("WRITE_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_ADDRESS_STOP = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("ADDRESS_STOP_SW", ControlType.input_switch, 1),
				new Control("ADDRESS_STOP_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_ADDRESS_BREAK = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("ADDRESS_BREAK_SW", ControlType.input_switch, 1),
				new Control("ADDRESS_BREAK_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_READ_IN = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("READ_IN_SW", ControlType.input_switch, 1),
				new Control("READ_IN_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_START = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("START_SW", ControlType.input_switch, 1),
				new Control("START_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_CONT = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("CONT_SW", ControlType.input_switch, 1),
				new Control("CONT_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_STOP = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("STOP_SW", ControlType.input_switch, 1),
				new Control("STOP_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_RESET = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("RESET_SW", ControlType.input_switch, 1),
				new Control("RESET_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_XCT = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("XCT_SW", ControlType.input_switch, 1),
				new Control("XCT_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_EXAMINE_THIS = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("EXAMINE_THIS_SW", ControlType.input_switch, 1),
				new Control("EXAMINE_THIS_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_EXAMINE_NEXT = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("EXAMINE_NEXT_SW", ControlType.input_switch, 1),
				new Control("EXAMINE_NEXT_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_DEPOSIT_THIS = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("DEPOSIT_THIS_SW", ControlType.input_switch, 1),
				new Control("DEPOSIT_THIS_FB", ControlType.output_lamp, 1), p));
		// 1 bit push button
		controls.add(button_DEPOSIT_NEXT = new PanelKI10Control(KI10ControlType.PDP10_KEY,
				new Control("DEPOSIT_NEXT_SW", ControlType.input_switch, 1),
				new Control("DEPOSIT_NEXT_FB", ControlType.output_lamp, 1), p));

		// these buttons light always if pressed
		button_CONSOLE_LOCK.wiredFeedback = true;
		button_CONSOLE_DATALOCK.wiredFeedback = true;
		button_POWER.wiredFeedback = true;

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
		for (PanelKI10Control ki10c : controls) {
			ki10c.visualization.clear();
		}

		/*** Define all the backgrounds, used for different controls ***/
		TwoStateControlSliceVisualization backgroundAlarms = new TwoStateControlSliceVisualization(
				"background_alarms.png.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundButtonsRow1 = //
		new TwoStateControlSliceVisualization("buttons_row1.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundButtonsRow2 = //
		new TwoStateControlSliceVisualization("buttons_row2.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundButtonsRow3Left = //
		new TwoStateControlSliceVisualization("background_buttons_row3_left.png.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundButtonsRow3Right = //
		new TwoStateControlSliceVisualization("background_buttons_row3_right", null, null, 0);
		TwoStateControlSliceVisualization backgroundLampsRow4 = //
		new TwoStateControlSliceVisualization("background_lamps_row4.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundLampsRow5 = //
		new TwoStateControlSliceVisualization("background_lamps_row5.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundLampsRow6 = //
		new TwoStateControlSliceVisualization("background_lamps_row6.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundLampsRow7 = //
		new TwoStateControlSliceVisualization("background_lamps_row7.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundLampsRow8 = //
		new TwoStateControlSliceVisualization("background_lamps_row8.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundButtonsRow9 = //
		new TwoStateControlSliceVisualization("background_buttons_row9.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundButtonsRow10 = //
		new TwoStateControlSliceVisualization("background_buttons_row10.png", null, null, 0);
		TwoStateControlSliceVisualization backgroundButtonsRow11 = //
		new TwoStateControlSliceVisualization("background_buttons_row11.png", null, null, 0);

		// All coordinates must have been are loaded: loadImageCoordinates()

		/***
		 * THIS CODE IS generated by the Image2Java" program, which reads the
		 * info .CSV in the image directory
		 ***/
		DimmableLedControlSliceVisualization.defaultAveragingInterval_ms = 100;
		// lamps have a low pass of 1/10 sec

		lamp_CKT_BRKR_TRIPPED.visualization
				.add(new DimmableLedControlSliceVisualization("lamp_CKT_BRKR_TRIPPED_on.png",
						lamp_CKT_BRKR_TRIPPED, lamp_CKT_BRKR_TRIPPED.outputcontrol, 0, false));
		lamp_DOORS_OPEN.visualization.add(new DimmableLedControlSliceVisualization("lamp_DOORS_OPEN_on.png",
				lamp_DOORS_OPEN, lamp_DOORS_OPEN.outputcontrol, 0, false));
		lamp_OVERTEMP.visualization.add(new DimmableLedControlSliceVisualization("lamp_OVERTEMP_on.png",
				lamp_OVERTEMP, lamp_OVERTEMP.outputcontrol, 0, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA00_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 0, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA01_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 1, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA02_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 2, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA03_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 3, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA04_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 4, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA05_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 5, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA06_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 6, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA07_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 7, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA08_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 8, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA09_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 9, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA10_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 10, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA11_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 11, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA12_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 12, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA13_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 13, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA14_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 14, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA15_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 15, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA16_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 16, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA17_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 17, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA18_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 18, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA19_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 19, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA20_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 20, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA21_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 21, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA22_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 22, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA23_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 23, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA24_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 24, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA25_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 25, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA26_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 26, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA27_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 27, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA28_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 28, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA29_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 29, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA30_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 30, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA31_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 31, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA32_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 32, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA33_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 33, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA34_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 34, false));
		leds_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_DATA35_on.png", leds_DATA,
				leds_DATA.outputcontrol, 35 - 35, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION00_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 0, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION01_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 1, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION02_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 2, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION03_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 3, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION04_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 4, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION05_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 5, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION06_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 6, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION07_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 7, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION08_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 8, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION09_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 9, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION10_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 10, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION11_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 11, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION12_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 12, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION13_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 13, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION14_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 14, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION15_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 15, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION16_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 16, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION17_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 17, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION18_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 18, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION19_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 19, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION20_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 20, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION21_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 21, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION22_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 22, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION23_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 23, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION24_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 24, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION25_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 25, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION26_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 26, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION27_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 27, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION28_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 28, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION29_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 29, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION30_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 30, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION31_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 31, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION32_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 32, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION33_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 33, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION34_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 34, false));
		leds_INSTRUCTION.visualization
				.add(new DimmableLedControlSliceVisualization("led_INSTRUCTION35_on.png", leds_INSTRUCTION,
						leds_INSTRUCTION.outputcontrol, 35 - 35, false));
		leds_IOB_PI_REQUEST.visualization
				.add(new DimmableLedControlSliceVisualization("led_IOB_PI_REQUEST1_on.png",
						leds_IOB_PI_REQUEST, leds_IOB_PI_REQUEST.outputcontrol, 1, false));
		leds_IOB_PI_REQUEST.visualization
				.add(new DimmableLedControlSliceVisualization("led_IOB_PI_REQUEST2_on.png",
						leds_IOB_PI_REQUEST, leds_IOB_PI_REQUEST.outputcontrol, 2, false));
		leds_IOB_PI_REQUEST.visualization
				.add(new DimmableLedControlSliceVisualization("led_IOB_PI_REQUEST3_on.png",
						leds_IOB_PI_REQUEST, leds_IOB_PI_REQUEST.outputcontrol, 3, false));
		leds_IOB_PI_REQUEST.visualization
				.add(new DimmableLedControlSliceVisualization("led_IOB_PI_REQUEST4_on.png",
						leds_IOB_PI_REQUEST, leds_IOB_PI_REQUEST.outputcontrol, 4, false));
		leds_IOB_PI_REQUEST.visualization
				.add(new DimmableLedControlSliceVisualization("led_IOB_PI_REQUEST5_on.png",
						leds_IOB_PI_REQUEST, leds_IOB_PI_REQUEST.outputcontrol, 5, false));
		leds_IOB_PI_REQUEST.visualization
				.add(new DimmableLedControlSliceVisualization("led_IOB_PI_REQUEST6_on.png",
						leds_IOB_PI_REQUEST, leds_IOB_PI_REQUEST.outputcontrol, 6, false));
		leds_IOB_PI_REQUEST.visualization
				.add(new DimmableLedControlSliceVisualization("led_IOB_PI_REQUEST7_on.png",
						leds_IOB_PI_REQUEST, leds_IOB_PI_REQUEST.outputcontrol, 7, false));
		led_KEY_MAINT.visualization.add(new DimmableLedControlSliceVisualization("led_KEY_MAINT_on.png",
				led_KEY_MAINT, led_KEY_MAINT.outputcontrol, 0, false));
		led_KEY_PG_FAIL.visualization.add(new DimmableLedControlSliceVisualization("led_KEY_PG_FAIL.png",
				led_KEY_PG_FAIL, led_KEY_PG_FAIL.outputcontrol, 0, false));
		led_MEMORY_DATA.visualization.add(new DimmableLedControlSliceVisualization("led_MEMORY_DATA_on.png",
				led_MEMORY_DATA, led_MEMORY_DATA.outputcontrol, 0, false));
		leds_MODE.visualization.add(new DimmableLedControlSliceVisualization("led_MODE0_on.png", leds_MODE,
				leds_MODE.outputcontrol, 0, false));
		leds_MODE.visualization.add(new DimmableLedControlSliceVisualization("led_MODE1_on.png", leds_MODE,
				leds_MODE.outputcontrol, 1, false));
		leds_MODE.visualization.add(new DimmableLedControlSliceVisualization("led_MODE2_on.png", leds_MODE,
				leds_MODE.outputcontrol, 2, false));
		leds_MODE.visualization.add(new DimmableLedControlSliceVisualization("led_MODE3_on.png", leds_MODE,
				leds_MODE.outputcontrol, 3, false));
		leds_PI_ACTIVE.visualization.add(new DimmableLedControlSliceVisualization("led_PI_ACTIVE1_on.png",
				leds_PI_ACTIVE, leds_PI_ACTIVE.outputcontrol, 1, false));
		leds_PI_ACTIVE.visualization.add(new DimmableLedControlSliceVisualization("led_PI_ACTIVE2_on.png",
				leds_PI_ACTIVE, leds_PI_ACTIVE.outputcontrol, 2, false));
		leds_PI_ACTIVE.visualization.add(new DimmableLedControlSliceVisualization("led_PI_ACTIVE3_on.png",
				leds_PI_ACTIVE, leds_PI_ACTIVE.outputcontrol, 3, false));
		leds_PI_ACTIVE.visualization.add(new DimmableLedControlSliceVisualization("led_PI_ACTIVE4_on.png",
				leds_PI_ACTIVE, leds_PI_ACTIVE.outputcontrol, 4, false));
		leds_PI_ACTIVE.visualization.add(new DimmableLedControlSliceVisualization("led_PI_ACTIVE5_on.png",
				leds_PI_ACTIVE, leds_PI_ACTIVE.outputcontrol, 5, false));
		leds_PI_ACTIVE.visualization.add(new DimmableLedControlSliceVisualization("led_PI_ACTIVE6_on.png",
				leds_PI_ACTIVE, leds_PI_ACTIVE.outputcontrol, 6, false));
		leds_PI_ACTIVE.visualization.add(new DimmableLedControlSliceVisualization("led_PI_ACTIVE7_on.png",
				leds_PI_ACTIVE, leds_PI_ACTIVE.outputcontrol, 7, false));
		leds_PI_IN_PROGRESS.visualization
				.add(new DimmableLedControlSliceVisualization("led_PI_IN_PROGRESS1_on.png",
						leds_PI_IN_PROGRESS, leds_PI_IN_PROGRESS.outputcontrol, 1, false));
		leds_PI_IN_PROGRESS.visualization
				.add(new DimmableLedControlSliceVisualization("led_PI_IN_PROGRESS2_on.png",
						leds_PI_IN_PROGRESS, leds_PI_IN_PROGRESS.outputcontrol, 2, false));
		leds_PI_IN_PROGRESS.visualization
				.add(new DimmableLedControlSliceVisualization("led_PI_IN_PROGRESS3_on.png",
						leds_PI_IN_PROGRESS, leds_PI_IN_PROGRESS.outputcontrol, 3, false));
		leds_PI_IN_PROGRESS.visualization
				.add(new DimmableLedControlSliceVisualization("led_PI_IN_PROGRESS4_on.png",
						leds_PI_IN_PROGRESS, leds_PI_IN_PROGRESS.outputcontrol, 4, false));
		leds_PI_IN_PROGRESS.visualization
				.add(new DimmableLedControlSliceVisualization("led_PI_IN_PROGRESS5_on.png",
						leds_PI_IN_PROGRESS, leds_PI_IN_PROGRESS.outputcontrol, 5, false));
		leds_PI_IN_PROGRESS.visualization
				.add(new DimmableLedControlSliceVisualization("led_PI_IN_PROGRESS6_on.png",
						leds_PI_IN_PROGRESS, leds_PI_IN_PROGRESS.outputcontrol, 6, false));
		leds_PI_IN_PROGRESS.visualization
				.add(new DimmableLedControlSliceVisualization("led_PI_IN_PROGRESS7_on.png",
						leds_PI_IN_PROGRESS, leds_PI_IN_PROGRESS.outputcontrol, 7, false));
		led_PI_OK_8.visualization.add(new DimmableLedControlSliceVisualization("led_PI_OK_8_on.png",
				led_PI_OK_8, led_PI_OK_8.outputcontrol, 0, false));
		led_PI_ON.visualization.add(new DimmableLedControlSliceVisualization("led_PI_ON_on.png", led_PI_ON,
				led_PI_ON.outputcontrol, 0, false));
		leds_PI_REQUEST.visualization.add(new DimmableLedControlSliceVisualization("led_PI_REQUEST1_on.png",
				leds_PI_REQUEST, leds_PI_REQUEST.outputcontrol, 1, false));
		leds_PI_REQUEST.visualization.add(new DimmableLedControlSliceVisualization("led_PI_REQUEST2_on.png",
				leds_PI_REQUEST, leds_PI_REQUEST.outputcontrol, 2, false));
		leds_PI_REQUEST.visualization.add(new DimmableLedControlSliceVisualization("led_PI_REQUEST3_on.png",
				leds_PI_REQUEST, leds_PI_REQUEST.outputcontrol, 3, false));
		leds_PI_REQUEST.visualization.add(new DimmableLedControlSliceVisualization("led_PI_REQUEST4_on.png",
				leds_PI_REQUEST, leds_PI_REQUEST.outputcontrol, 4, false));
		leds_PI_REQUEST.visualization.add(new DimmableLedControlSliceVisualization("led_PI_REQUEST5_on.png",
				leds_PI_REQUEST, leds_PI_REQUEST.outputcontrol, 5, false));
		leds_PI_REQUEST.visualization.add(new DimmableLedControlSliceVisualization("led_PI_REQUEST6_on.png",
				leds_PI_REQUEST, leds_PI_REQUEST.outputcontrol, 6, false));
		leds_PI_REQUEST.visualization.add(new DimmableLedControlSliceVisualization("led_PI_REQUEST7_on.png",
				leds_PI_REQUEST, leds_PI_REQUEST.outputcontrol, 7, false));
		led_POWER.visualization.add(new DimmableLedControlSliceVisualization("led_POWER_on.png", led_POWER,
				led_POWER.outputcontrol, 0, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER18_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 18, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER19_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 19, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER20_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 20, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER21_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 21, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER22_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 22, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER23_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 23, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER24_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 24, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER25_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 25, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER26_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 26, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER27_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 27, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER28_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 28, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER29_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 29, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER30_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 30, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER31_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 31, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER32_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 32, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER33_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 33, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER34_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 34, false));
		leds_PROGRAM_COUNTER.visualization.add(new DimmableLedControlSliceVisualization(
				"led_PROGRAM_COUNTER35_on.png", leds_PROGRAM_COUNTER,
				leds_PROGRAM_COUNTER.outputcontrol, 35 - 35, false));
		led_PROGRAM_DATA.visualization
				.add(new DimmableLedControlSliceVisualization("led_PROGRAM_DATA_on.png", led_PROGRAM_DATA,
						led_PROGRAM_DATA.outputcontrol, 0, false));
		led_RUN.visualization.add(new DimmableLedControlSliceVisualization("led_RUN_on.png", led_RUN,
				led_RUN.outputcontrol, 0, false));
		leds_STOP.visualization.add(new DimmableLedControlSliceVisualization("led_STOP0_on.png", leds_STOP,
				leds_STOP.outputcontrol, 0, false));
		leds_STOP.visualization.add(new DimmableLedControlSliceVisualization("led_STOP1_on.png", leds_STOP,
				leds_STOP.outputcontrol, 1, false));
		leds_STOP.visualization.add(new DimmableLedControlSliceVisualization("led_STOP2_on.png", leds_STOP,
				leds_STOP.outputcontrol, 2, false));

		LampButtonControlSliceVisualization.defaultAveragingInterval_ms = 250;
		// buttons lights have a low pass of 1/4 sec

		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 14, buttons_ADDRESS, //
						"sw_ADDRESS14_down_off.png", "sw_ADDRESS14_down_on.png",
						"sw_ADDRESS14_up_off.png", "sw_ADDRESS14_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 15, buttons_ADDRESS, //
						"sw_ADDRESS15_down_off.png", "sw_ADDRESS15_down_on.png",
						"sw_ADDRESS15_up_off.png", "sw_ADDRESS15_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 16, buttons_ADDRESS, //
						"sw_ADDRESS16_down_off.png", "sw_ADDRESS16_down_on.png",
						"sw_ADDRESS16_up_off.png", "sw_ADDRESS16_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 17, buttons_ADDRESS, //
						"sw_ADDRESS17_down_off.png", "sw_ADDRESS17_down_on.png",
						"sw_ADDRESS17_up_off.png", "sw_ADDRESS17_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 18, buttons_ADDRESS, //
						"sw_ADDRESS18_down_off.png", "sw_ADDRESS18_down_on.png",
						"sw_ADDRESS18_up_off.png", "sw_ADDRESS18_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 19, buttons_ADDRESS, //
						"sw_ADDRESS19_down_off.png", "sw_ADDRESS19_down_on.png",
						"sw_ADDRESS19_up_off.png", "sw_ADDRESS19_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 20, buttons_ADDRESS, //
						"sw_ADDRESS20_down_off.png", "sw_ADDRESS20_down_on.png",
						"sw_ADDRESS20_up_off.png", "sw_ADDRESS20_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 21, buttons_ADDRESS, //
						"sw_ADDRESS21_down_off.png", "sw_ADDRESS21_down_on.png",
						"sw_ADDRESS21_up_off.png", "sw_ADDRESS21_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 22, buttons_ADDRESS, //
						"sw_ADDRESS22_down_off.png", "sw_ADDRESS22_down_on.png",
						"sw_ADDRESS22_up_off.png", "sw_ADDRESS22_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 23, buttons_ADDRESS, //
						"sw_ADDRESS23_down_off.png", "sw_ADDRESS23_down_on.png",
						"sw_ADDRESS23_up_off.png", "sw_ADDRESS23_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 24, buttons_ADDRESS, //
						"sw_ADDRESS24_down_off.png", "sw_ADDRESS24_down_on.png",
						"sw_ADDRESS24_up_off.png", "sw_ADDRESS24_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 25, buttons_ADDRESS, //
						"sw_ADDRESS25_down_off.png", "sw_ADDRESS25_down_on.png",
						"sw_ADDRESS25_up_off.png", "sw_ADDRESS25_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 26, buttons_ADDRESS, //
						"sw_ADDRESS26_down_off.png", "sw_ADDRESS26_down_on.png",
						"sw_ADDRESS26_up_off.png", "sw_ADDRESS26_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 27, buttons_ADDRESS, //
						"sw_ADDRESS27_down_off.png", "sw_ADDRESS27_down_on.png",
						"sw_ADDRESS27_up_off.png", "sw_ADDRESS27_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 28, buttons_ADDRESS, //
						"sw_ADDRESS28_down_off.png", "sw_ADDRESS28_down_on.png",
						"sw_ADDRESS28_up_off.png", "sw_ADDRESS28_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 29, buttons_ADDRESS, //
						"sw_ADDRESS29_down_off.png", "sw_ADDRESS29_down_on.png",
						"sw_ADDRESS29_up_off.png", "sw_ADDRESS29_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 30, buttons_ADDRESS, //
						"sw_ADDRESS30_down_off.png", "sw_ADDRESS30_down_on.png",
						"sw_ADDRESS30_up_off.png", "sw_ADDRESS30_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 31, buttons_ADDRESS, //
						"sw_ADDRESS31_down_off.png", "sw_ADDRESS31_down_on.png",
						"sw_ADDRESS31_up_off.png", "sw_ADDRESS31_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 32, buttons_ADDRESS, //
						"sw_ADDRESS32_down_off.png", "sw_ADDRESS32_down_on.png",
						"sw_ADDRESS32_up_off.png", "sw_ADDRESS32_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 33, buttons_ADDRESS, //
						"sw_ADDRESS33_down_off.png", "sw_ADDRESS33_down_on.png",
						"sw_ADDRESS33_up_off.png", "sw_ADDRESS33_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 34, buttons_ADDRESS, //
						"sw_ADDRESS34_down_off.png", "sw_ADDRESS34_down_on.png",
						"sw_ADDRESS34_up_off.png", "sw_ADDRESS34_up_on.png"));
		buttons_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_ADDRESS", 35 - 35, buttons_ADDRESS, //
						"sw_ADDRESS35_down_off.png", "sw_ADDRESS35_down_on.png",
						"sw_ADDRESS35_up_off.png", "sw_ADDRESS35_up_on.png"));
		button_ADDRESS_BREAK.visualization.add( //
				new LampButtonControlSliceVisualization("button_ADDRESS_BREAK", 0, button_ADDRESS_BREAK, //
						"sw_ADDRESS_BREAK_down_off.png", "sw_ADDRESS_BREAK_down_on.png",
						"sw_ADDRESS_BREAK_up_off.png", "sw_ADDRESS_BREAK_up_on.png"));
		button_ADDRESS_CLEAR.visualization.add( //
				new LampButtonControlSliceVisualization("button_ADDRESS_CLEAR", 0, button_ADDRESS_CLEAR, //
						"sw_ADDRESS_CLEAR_down_off.png", "sw_ADDRESS_CLEAR_down_on.png",
						"sw_ADDRESS_CLEAR_up_off.png", "sw_ADDRESS_CLEAR_up_on.png"));
		button_ADDRESS_LOAD.visualization.add( //
				new LampButtonControlSliceVisualization("button_ADDRESS_LOAD", 0, button_ADDRESS_LOAD, //
						"sw_ADDRESS_LOAD_down_off.png", "sw_ADDRESS_LOAD_down_on.png",
						"sw_ADDRESS_LOAD_up_off.png", "sw_ADDRESS_LOAD_up_on.png"));
		button_ADDRESS_STOP.visualization.add( //
				new LampButtonControlSliceVisualization("button_ADDRESS_STOP", 0, button_ADDRESS_STOP, //
						"sw_ADDRESS_STOP_down_off.png", "sw_ADDRESS_STOP_down_on.png",
						"sw_ADDRESS_STOP_up_off.png", "sw_ADDRESS_STOP_up_on.png"));
		button_CONSOLE_DATALOCK.visualization.add( //
				new LampButtonControlSliceVisualization("button_CONSOLE_DATALOCK", 0,
						button_CONSOLE_DATALOCK, //
						"sw_CONSOLE_DATALOCK_down_off.png", "sw_CONSOLE_DATALOCK_down_on.png",
						"sw_CONSOLE_DATALOCK_up_off.png", "sw_CONSOLE_DATALOCK_up_on.png"));
		button_CONSOLE_LOCK.visualization.add( //
				new LampButtonControlSliceVisualization("button_CONSOLE_LOCK", 0, button_CONSOLE_LOCK, //
						"sw_CONSOLE_LOCK_down_off.png", "sw_CONSOLE_LOCK_down_on.png",
						"sw_CONSOLE_LOCK_up_off.png", "sw_CONSOLE_LOCK_up_on.png"));
		button_CONT.visualization.add( //
				new LampButtonControlSliceVisualization("button_CONT", 0, button_CONT, //
						"sw_CONT_down_off.png", "sw_CONT_down_on.png", "sw_CONT_up_off.png",
						"sw_CONT_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 0, buttons_DATA, //
						"sw_DATA00_down_off.png", "sw_DATA00_down_on.png",
						"sw_DATA00_up_off.png", "sw_DATA00_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 1, buttons_DATA, //
						"sw_DATA01_down_off.png", "sw_DATA01_down_on.png",
						"sw_DATA01_up_off.png", "sw_DATA01_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 2, buttons_DATA, //
						"sw_DATA02_down_off.png", "sw_DATA02_down_on.png",
						"sw_DATA02_up_off.png", "sw_DATA02_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 3, buttons_DATA, //
						"sw_DATA03_down_off.png", "sw_DATA03_down_on.png",
						"sw_DATA03_up_off.png", "sw_DATA03_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 4, buttons_DATA, //
						"sw_DATA04_down_off.png", "sw_DATA04_down_on.png",
						"sw_DATA04_up_off.png", "sw_DATA04_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 5, buttons_DATA, //
						"sw_DATA05_down_off.png", "sw_DATA05_down_on.png",
						"sw_DATA05_up_off.png", "sw_DATA05_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 6, buttons_DATA, //
						"sw_DATA06_down_off.png", "sw_DATA06_down_on.png",
						"sw_DATA06_up_off.png", "sw_DATA06_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 7, buttons_DATA, //
						"sw_DATA07_down_off.png", "sw_DATA07_down_on.png",
						"sw_DATA07_up_off.png", "sw_DATA07_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 8, buttons_DATA, //
						"sw_DATA08_down_off.png", "sw_DATA08_down_on.png",
						"sw_DATA08_up_off.png", "sw_DATA08_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 9, buttons_DATA, //
						"sw_DATA09_down_off.png", "sw_DATA09_down_on.png",
						"sw_DATA09_up_off.png", "sw_DATA09_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 10, buttons_DATA, //
						"sw_DATA10_down_off.png", "sw_DATA10_down_on.png",
						"sw_DATA10_up_off.png", "sw_DATA10_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 11, buttons_DATA, //
						"sw_DATA11_down_off.png", "sw_DATA11_down_on.png",
						"sw_DATA11_up_off.png", "sw_DATA11_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 12, buttons_DATA, //
						"sw_DATA12_down_off.png", "sw_DATA12_down_on.png",
						"sw_DATA12_up_off.png", "sw_DATA12_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 13, buttons_DATA, //
						"sw_DATA13_down_off.png", "sw_DATA13_down_on.png",
						"sw_DATA13_up_off.png", "sw_DATA13_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 14, buttons_DATA, //
						"sw_DATA14_down_off.png", "sw_DATA14_down_on.png",
						"sw_DATA14_up_off.png", "sw_DATA14_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 15, buttons_DATA, //
						"sw_DATA15_down_off.png", "sw_DATA15_down_on.png",
						"sw_DATA15_up_off.png", "sw_DATA15_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 16, buttons_DATA, //
						"sw_DATA16_down_off.png", "sw_DATA16_down_on.png",
						"sw_DATA16_up_off.png", "sw_DATA16_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 17, buttons_DATA, //
						"sw_DATA17_down_off.png", "sw_DATA17_down_on.png",
						"sw_DATA17_up_off.png", "sw_DATA17_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 18, buttons_DATA, //
						"sw_DATA18_down_off.png", "sw_DATA18_down_on.png",
						"sw_DATA18_up_off.png", "sw_DATA18_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 19, buttons_DATA, //
						"sw_DATA19_down_off.png", "sw_DATA19_down_on.png",
						"sw_DATA19_up_off.png", "sw_DATA19_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 20, buttons_DATA, //
						"sw_DATA20_down_off.png", "sw_DATA20_down_on.png",
						"sw_DATA20_up_off.png", "sw_DATA20_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 21, buttons_DATA, //
						"sw_DATA21_down_off.png", "sw_DATA21_down_on.png",
						"sw_DATA21_up_off.png", "sw_DATA21_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 22, buttons_DATA, //
						"sw_DATA22_down_off.png", "sw_DATA22_down_on.png",
						"sw_DATA22_up_off.png", "sw_DATA22_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 23, buttons_DATA, //
						"sw_DATA23_down_off.png", "sw_DATA23_down_on.png",
						"sw_DATA23_up_off.png", "sw_DATA23_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 24, buttons_DATA, //
						"sw_DATA24_down_off.png", "sw_DATA24_down_on.png",
						"sw_DATA24_up_off.png", "sw_DATA24_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 25, buttons_DATA, //
						"sw_DATA25_down_off.png", "sw_DATA25_down_on.png",
						"sw_DATA25_up_off.png", "sw_DATA25_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 26, buttons_DATA, //
						"sw_DATA26_down_off.png", "sw_DATA26_down_on.png",
						"sw_DATA26_up_off.png", "sw_DATA26_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 27, buttons_DATA, //
						"sw_DATA27_down_off.png", "sw_DATA27_down_on.png",
						"sw_DATA27_up_off.png", "sw_DATA27_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 28, buttons_DATA, //
						"sw_DATA28_down_off.png", "sw_DATA28_down_on.png",
						"sw_DATA28_up_off.png", "sw_DATA28_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 29, buttons_DATA, //
						"sw_DATA29_down_off.png", "sw_DATA29_down_on.png",
						"sw_DATA29_up_off.png", "sw_DATA29_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 30, buttons_DATA, //
						"sw_DATA30_down_off.png", "sw_DATA30_down_on.png",
						"sw_DATA30_up_off.png", "sw_DATA30_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 31, buttons_DATA, //
						"sw_DATA31_down_off.png", "sw_DATA31_down_on.png",
						"sw_DATA31_up_off.png", "sw_DATA31_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 32, buttons_DATA, //
						"sw_DATA32_down_off.png", "sw_DATA32_down_on.png",
						"sw_DATA32_up_off.png", "sw_DATA32_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 33, buttons_DATA, //
						"sw_DATA33_down_off.png", "sw_DATA33_down_on.png",
						"sw_DATA33_up_off.png", "sw_DATA33_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 34, buttons_DATA, //
						"sw_DATA34_down_off.png", "sw_DATA34_down_on.png",
						"sw_DATA34_up_off.png", "sw_DATA34_up_on.png"));
		buttons_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_DATA", 35 - 35, buttons_DATA, //
						"sw_DATA35_down_off.png", "sw_DATA35_down_on.png",
						"sw_DATA35_up_off.png", "sw_DATA35_up_on.png"));
		button_DATA_CLEAR.visualization.add( //
				new LampButtonControlSliceVisualization("button_DATA_CLEAR", 0, button_DATA_CLEAR, //
						"sw_DATA_CLEAR_down_off.png", "sw_DATA_CLEAR_down_on.png",
						"sw_DATA_CLEAR_up_off.png", "sw_DATA_CLEAR_up_on.png"));
		button_DATA_LOAD.visualization.add( //
				new LampButtonControlSliceVisualization("button_DATA_LOAD", 0, button_DATA_LOAD, //
						"sw_DATA_LOAD_down_off.png", "sw_DATA_LOAD_down_on.png",
						"sw_DATA_LOAD_up_off.png", "sw_DATA_LOAD_up_on.png"));
		button_DEPOSIT_NEXT.visualization.add( //
				new LampButtonControlSliceVisualization("button_DEPOSIT_NEXT", 0, button_DEPOSIT_NEXT, //
						"sw_DEPOSIT_NEXT_down_off.png", "sw_DEPOSIT_NEXT_down_on.png",
						"sw_DEPOSIT_NEXT_up_off.png", "sw_DEPOSIT_NEXT_up_on.png"));
		button_DEPOSIT_THIS.visualization.add( //
				new LampButtonControlSliceVisualization("button_DEPOSIT_THIS", 0, button_DEPOSIT_THIS, //
						"sw_DEPOSIT_THIS_down_off.png", "sw_DEPOSIT_THIS_down_on.png",
						"sw_DEPOSIT_THIS_up_off.png", "sw_DEPOSIT_THIS_up_on.png"));
		button_EXAMINE_NEXT.visualization.add( //
				new LampButtonControlSliceVisualization("button_EXAMINE_NEXT", 0, button_EXAMINE_NEXT, //
						"sw_EXAMINE_NEXT_down_off.png", "sw_EXAMINE_NEXT_down_on.png",
						"sw_EXAMINE_NEXT_up_off.png", "sw_EXAMINE_NEXT_up_on.png"));
		button_EXAMINE_THIS.visualization.add( //
				new LampButtonControlSliceVisualization("button_EXAMINE_THIS", 0, button_EXAMINE_THIS, //
						"sw_EXAMINE_THIS_down_off.png", "sw_EXAMINE_THIS_down_on.png",
						"sw_EXAMINE_THIS_up_off.png", "sw_EXAMINE_THIS_up_on.png"));
		button_FETCH_DATA.visualization.add( //
				new LampButtonControlSliceVisualization("button_FETCH_DATA", 0, button_FETCH_DATA, //
						"sw_FETCH_DATA_down_off.png", "sw_FETCH_DATA_down_on.png",
						"sw_FETCH_DATA_up_off.png", "sw_FETCH_DATA_up_on.png"));
		button_FETCH_INST.visualization.add( //
				new LampButtonControlSliceVisualization("button_FETCH_INST", 0, button_FETCH_INST, //
						"sw_FETCH_INST_down_off.png", "sw_FETCH_INST_down_on.png",
						"sw_FETCH_INST_up_off.png", "sw_FETCH_INST_up_on.png"));
		buttons_FM_BLOCK.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_FM_BLOCK", 0, buttons_FM_BLOCK, //
						"sw_FM_BLOCK0_down_off.png", "sw_FM_BLOCK0_down_on.png",
						"sw_FM_BLOCK0_up_off.png", "sw_FM_BLOCK0_up_on.png"));
		buttons_FM_BLOCK.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_FM_BLOCK", 1, buttons_FM_BLOCK, //
						"sw_FM_BLOCK1_down_off.png", "sw_FM_BLOCK1_down_on.png",
						"sw_FM_BLOCK1_up_off.png", "sw_FM_BLOCK1_up_on.png"));
		button_FM_MANUAL.visualization.add( //
				new LampButtonControlSliceVisualization("button_FM_MANUAL", 0, button_FM_MANUAL, //
						"sw_FM_MANUAL_down_off.png", "sw_FM_MANUAL_down_on.png",
						"sw_FM_MANUAL_up_off.png", "sw_FM_MANUAL_up_on.png"));
		// "button_LAMP_TEST" has no light bulb: "on" becomes "off"
		button_LAMP_TEST.visualization.add( //
				new LampButtonControlSliceVisualization("button_LAMP_TEST", 0, button_LAMP_TEST, //
						"sw_LAMP_TEST_down_off.png", "sw_LAMP_TEST_down_off.png",
						"sw_LAMP_TEST_up_off.png", "sw_LAMP_TEST_up_off.png"));
		buttons_MANUAL_MARGIN_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_MANUAL_MARGIN_ADDRESS", 0,
						buttons_MANUAL_MARGIN_ADDRESS, //
						"sw_MANUAL_MARGIN_ADDRESS0_down_off.png",
						"sw_MANUAL_MARGIN_ADDRESS0_down_on.png",
						"sw_MANUAL_MARGIN_ADDRESS0_up_off.png",
						"sw_MANUAL_MARGIN_ADDRESS0_up_on.png"));
		buttons_MANUAL_MARGIN_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_MANUAL_MARGIN_ADDRESS", 1,
						buttons_MANUAL_MARGIN_ADDRESS, //
						"sw_MANUAL_MARGIN_ADDRESS1_down_off.png",
						"sw_MANUAL_MARGIN_ADDRESS1_down_on.png",
						"sw_MANUAL_MARGIN_ADDRESS1_up_off.png",
						"sw_MANUAL_MARGIN_ADDRESS1_up_on.png"));
		buttons_MANUAL_MARGIN_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_MANUAL_MARGIN_ADDRESS", 2,
						buttons_MANUAL_MARGIN_ADDRESS, //
						"sw_MANUAL_MARGIN_ADDRESS2_down_off.png",
						"sw_MANUAL_MARGIN_ADDRESS2_down_on.png",
						"sw_MANUAL_MARGIN_ADDRESS2_up_off.png",
						"sw_MANUAL_MARGIN_ADDRESS2_up_on.png"));
		buttons_MANUAL_MARGIN_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_MANUAL_MARGIN_ADDRESS", 3,
						buttons_MANUAL_MARGIN_ADDRESS, //
						"sw_MANUAL_MARGIN_ADDRESS3_down_off.png",
						"sw_MANUAL_MARGIN_ADDRESS3_down_on.png",
						"sw_MANUAL_MARGIN_ADDRESS3_up_off.png",
						"sw_MANUAL_MARGIN_ADDRESS3_up_on.png"));
		buttons_MANUAL_MARGIN_ADDRESS.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_MANUAL_MARGIN_ADDRESS", 4,
						buttons_MANUAL_MARGIN_ADDRESS, //
						"sw_MANUAL_MARGIN_ADDRESS4_down_off.png",
						"sw_MANUAL_MARGIN_ADDRESS4_down_on.png",
						"sw_MANUAL_MARGIN_ADDRESS4_up_off.png",
						"sw_MANUAL_MARGIN_ADDRESS4_up_on.png"));
		button_MARGIN_ENABLE.visualization.add( //
				new LampButtonControlSliceVisualization("button_MARGIN_ENABLE", 0, button_MARGIN_ENABLE, //
						"sw_MARGIN_ENABLE_down_off.png", "sw_MARGIN_ENABLE_down_on.png",
						"sw_MARGIN_ENABLE_up_off.png", "sw_MARGIN_ENABLE_up_on.png"));
		button_MEM_OVERLAP_DIS.visualization.add( //
				new LampButtonControlSliceVisualization("button_MEM_OVERLAP_DIS", 0, button_MEM_OVERLAP_DIS, //
						"sw_MEM_OVERLAP_DIS_down_off.png", "sw_MEM_OVERLAP_DIS_down_on.png",
						"sw_MEM_OVERLAP_DIS_up_off.png", "sw_MEM_OVERLAP_DIS_up_on.png"));
		button_MI_PROG_DIS.visualization.add( //
				new LampButtonControlSliceVisualization("button_MI_PROG_DIS", 0, button_MI_PROG_DIS, //
						"sw_MI_PROG_DIS_down_off.png", "sw_MI_PROG_DIS_down_on.png",
						"sw_MI_PROG_DIS_up_off.png", "sw_MI_PROG_DIS_up_on.png"));
		button_PAGING_EXEC.visualization.add( //
				new LampButtonControlSliceVisualization("button_PAGING_EXEC", 0, button_PAGING_EXEC, //
						"sw_PAGING_EXEC_down_off.png", "sw_PAGING_EXEC_down_on.png",
						"sw_PAGING_EXEC_up_off.png", "sw_PAGING_EXEC_up_on.png"));
		button_PAGING_USER.visualization.add( //
				new LampButtonControlSliceVisualization("button_PAGING_USER", 0, button_PAGING_USER, //
						"sw_PAGING_USER_down_off.png", "sw_PAGING_USER_down_on.png",
						"sw_PAGING_USER_up_off.png", "sw_PAGING_USER_up_on.png"));
		button_POWER.visualization.add( //
				new LampButtonControlSliceVisualization("button_POWER", 0, button_POWER, //
						"sw_POWER_down_off.png", "sw_POWER_down_on.png", "sw_POWER_up_off.png",
						"sw_POWER_up_on.png"));
		button_READ_IN.visualization.add( //
				new LampButtonControlSliceVisualization("button_READ_IN", 0, button_READ_IN, //
						"sw_READ_IN_down_off.png", "sw_READ_IN_down_on.png",
						"sw_READ_IN_up_off.png", "sw_READ_IN_up_on.png"));
		buttons_READ_IN_DEVICE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_READ_IN_DEVICE", 9 - 3,
						buttons_READ_IN_DEVICE, //
						"sw_READ_IN_DEVICE3_down_off.png", "sw_READ_IN_DEVICE3_down_on.png",
						"sw_READ_IN_DEVICE3_up_off.png", "sw_READ_IN_DEVICE3_up_on.png"));
		buttons_READ_IN_DEVICE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_READ_IN_DEVICE", 9 - 4,
						buttons_READ_IN_DEVICE, //
						"sw_READ_IN_DEVICE4_down_off.png", "sw_READ_IN_DEVICE4_down_on.png",
						"sw_READ_IN_DEVICE4_up_off.png", "sw_READ_IN_DEVICE4_up_on.png"));
		buttons_READ_IN_DEVICE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_READ_IN_DEVICE", 9 - 5,
						buttons_READ_IN_DEVICE, //
						"sw_READ_IN_DEVICE5_down_off.png", "sw_READ_IN_DEVICE5_down_on.png",
						"sw_READ_IN_DEVICE5_up_off.png", "sw_READ_IN_DEVICE5_up_on.png"));
		buttons_READ_IN_DEVICE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_READ_IN_DEVICE", 9 - 6,
						buttons_READ_IN_DEVICE, //
						"sw_READ_IN_DEVICE6_down_off.png", "sw_READ_IN_DEVICE6_down_on.png",
						"sw_READ_IN_DEVICE6_up_off.png", "sw_READ_IN_DEVICE6_up_on.png"));
		buttons_READ_IN_DEVICE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_READ_IN_DEVICE", 9 - 7,
						buttons_READ_IN_DEVICE, //
						"sw_READ_IN_DEVICE7_down_off.png", "sw_READ_IN_DEVICE7_down_on.png",
						"sw_READ_IN_DEVICE7_up_off.png", "sw_READ_IN_DEVICE7_up_on.png"));
		buttons_READ_IN_DEVICE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_READ_IN_DEVICE", 9 - 8,
						buttons_READ_IN_DEVICE, //
						"sw_READ_IN_DEVICE8_down_off.png", "sw_READ_IN_DEVICE8_down_on.png",
						"sw_READ_IN_DEVICE8_up_off.png", "sw_READ_IN_DEVICE8_up_on.png"));
		buttons_READ_IN_DEVICE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_READ_IN_DEVICE", 9 - 9,
						buttons_READ_IN_DEVICE, //
						"sw_READ_IN_DEVICE9_down_off.png", "sw_READ_IN_DEVICE9_down_on.png",
						"sw_READ_IN_DEVICE9_up_off.png", "sw_READ_IN_DEVICE9_up_on.png"));
		button_REPEAT.visualization.add( //
				new LampButtonControlSliceVisualization("button_REPEAT", 0, button_REPEAT, //
						"sw_REPEAT_down_off.png", "sw_REPEAT_down_on.png",
						"sw_REPEAT_up_off.png", "sw_REPEAT_up_on.png"));
		button_RESET.visualization.add( //
				new LampButtonControlSliceVisualization("button_RESET", 0, button_RESET, //
						"sw_RESET_down_off.png", "sw_RESET_down_on.png", "sw_RESET_up_off.png",
						"sw_RESET_up_on.png"));
		buttons_SENSE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_SENSE", 6 - 1, buttons_SENSE, //
						"sw_SENSE1_down_off.png", "sw_SENSE1_down_on.png",
						"sw_SENSE1_up_off.png", "sw_SENSE1_up_on.png"));
		buttons_SENSE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_SENSE", 6 - 2, buttons_SENSE, //
						"sw_SENSE2_down_off.png", "sw_SENSE2_down_on.png",
						"sw_SENSE2_up_off.png", "sw_SENSE2_up_on.png"));
		buttons_SENSE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_SENSE", 6 - 3, buttons_SENSE, //
						"sw_SENSE3_down_off.png", "sw_SENSE3_down_on.png",
						"sw_SENSE3_up_off.png", "sw_SENSE3_up_on.png"));
		buttons_SENSE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_SENSE", 6 - 4, buttons_SENSE, //
						"sw_SENSE4_down_off.png", "sw_SENSE4_down_on.png",
						"sw_SENSE4_up_off.png", "sw_SENSE4_up_on.png"));
		buttons_SENSE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_SENSE", 6 - 5, buttons_SENSE, //
						"sw_SENSE5_down_off.png", "sw_SENSE5_down_on.png",
						"sw_SENSE5_up_off.png", "sw_SENSE5_up_on.png"));
		buttons_SENSE.visualization.add( //
				new LampButtonControlSliceVisualization("buttons_SENSE", 6 - 6, buttons_SENSE, //
						"sw_SENSE6_down_off.png", "sw_SENSE6_down_on.png",
						"sw_SENSE6_up_off.png", "sw_SENSE6_up_on.png"));
		button_SINGLE_INST.visualization.add( //
				new LampButtonControlSliceVisualization("button_SINGLE_INST", 0, button_SINGLE_INST, //
						"sw_SINGLE_INST_down_off.png", "sw_SINGLE_INST_down_on.png",
						"sw_SINGLE_INST_up_off.png", "sw_SINGLE_INST_up_on.png"));
		button_SINGLE_PULSE.visualization.add( //
				new LampButtonControlSliceVisualization("button_SINGLE_PULSE", 0, button_SINGLE_PULSE, //
						"sw_SINGLE_PULSE_down_off.png", "sw_SINGLE_PULSE_down_on.png",
						"sw_SINGLE_PULSE_up_off.png", "sw_SINGLE_PULSE_up_on.png"));
		button_SINGLE_PULSER.visualization.add( //
				new LampButtonControlSliceVisualization("button_SINGLE_PULSER", 0, button_SINGLE_PULSER, //
						"sw_SINGLE_PULSER_down_off.png", "sw_SINGLE_PULSER_down_on.png",
						"sw_SINGLE_PULSER_up_off.png", "sw_SINGLE_PULSER_up_on.png"));
		button_START.visualization.add( //
				new LampButtonControlSliceVisualization("button_START", 0, button_START, //
						"sw_START_down_off.png", "sw_START_down_on.png", "sw_START_up_off.png",
						"sw_START_up_on.png"));
		button_STOP.visualization.add( //
				new LampButtonControlSliceVisualization("button_STOP", 0, button_STOP, //
						"sw_STOP_down_off.png", "sw_STOP_down_on.png", "sw_STOP_up_off.png",
						"sw_STOP_up_on.png"));
		button_STOP_NXM.visualization.add( //
				new LampButtonControlSliceVisualization("button_STOP_NXM", 0, button_STOP_NXM, //
						"sw_STOP_NXM_down_off.png", "sw_STOP_NXM_down_on.png",
						"sw_STOP_NXM_up_off.png", "sw_STOP_NXM_up_on.png"));
		button_STOP_PAR.visualization.add( //
				new LampButtonControlSliceVisualization("button_STOP_PAR", 0, button_STOP_PAR, //
						"sw_STOP_PAR_down_off.png", "sw_STOP_PAR_down_on.png",
						"sw_STOP_PAR_up_off.png", "sw_STOP_PAR_up_on.png"));
		button_WRITE.visualization.add( //
				new LampButtonControlSliceVisualization("button_WRITE", 0, button_WRITE, //
						"sw_WRITE_down_off.png", "sw_WRITE_down_on.png", "sw_WRITE_up_off.png",
						"sw_WRITE_up_on.png"));
		button_XCT.visualization.add( //
				new LampButtonControlSliceVisualization("button_XCT", 0, button_XCT, //
						"sw_XCT_down_off.png", "sw_XCT_down_on.png", "sw_XCT_up_off.png",
						"sw_XCT_up_on.png"));
		/***** end paste *****/
		
		MultiStateControlSliceVisualization msvc;
		// add knob states
		msvc = new MultiStateControlSliceVisualization(knob_IND_SELECT,
				knob_IND_SELECT.inputcontrol);
		knob_IND_SELECT.visualization.add(msvc);
		msvc.addStateImageFilename("knob_IND_SELECT_0.png", 0);
		msvc.addStateImageFilename("knob_IND_SELECT_0.png", 0);
		msvc.addStateImageFilename("knob_IND_SELECT_1.png", 1);
		msvc.addStateImageFilename("knob_IND_SELECT_2.png", 2);
		msvc.addStateImageFilename("knob_IND_SELECT_3.png", 3);

		msvc = new MultiStateControlSliceVisualization(knob_MARGIN_SELECT,
				knob_MARGIN_SELECT.inputcontrol);
		knob_MARGIN_SELECT.visualization.add(msvc);
		msvc.addStateImageFilename("knob_MARGIN_SELECT_0.png", 0);
		msvc.addStateImageFilename("knob_MARGIN_SELECT_1.png", 1);
		msvc.addStateImageFilename("knob_MARGIN_SELECT_2.png", 2);
		msvc.addStateImageFilename("knob_MARGIN_SELECT_3.png", 3);
		msvc.addStateImageFilename("knob_MARGIN_SELECT_4.png", 4);
		msvc.addStateImageFilename("knob_MARGIN_SELECT_5.png", 5);
		msvc.addStateImageFilename("knob_MARGIN_SELECT_6.png", 6);
		msvc.addStateImageFilename("knob_MARGIN_SELECT_7.png", 7);

		msvc = new MultiStateControlSliceVisualization(knob_SPEED_CONTROL_COARSE,
				knob_SPEED_CONTROL_COARSE.inputcontrol);
		knob_SPEED_CONTROL_COARSE.visualization.add(msvc);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_COARSE_0.png", 0);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_COARSE_1.png", 1);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_COARSE_2.png", 2);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_COARSE_3.png", 3);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_COARSE_4.png", 4);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_COARSE_5.png", 5);

		msvc = new MultiStateControlSliceVisualization(knob_SPEED_CONTROL_FINE,
				knob_SPEED_CONTROL_FINE.inputcontrol);
		knob_SPEED_CONTROL_FINE.visualization.add(msvc);
		// should give values 0..99 instead 0..7!
		msvc.addStateImageFilename("knob_SPEED_CONTROL_FINE_0.png", 0);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_FINE_1.png", 20);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_FINE_2.png", 40);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_FINE_3.png", 60);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_FINE_4.png", 80);
		msvc.addStateImageFilename("knob_SPEED_CONTROL_FINE_5.png", 99);

		// should give values 0..99 instead 0..7!
		msvc = new MultiStateControlSliceVisualization(knob_MARGIN_VOLTAGE,
				knob_MARGIN_VOLTAGE.inputcontrol);
		knob_MARGIN_VOLTAGE.visualization.add(msvc);
		msvc.addStateImageFilename("knob_VOLTAGE_0.png", 0);
		msvc.addStateImageFilename("knob_VOLTAGE_1.png", 20);
		msvc.addStateImageFilename("knob_VOLTAGE_2.png", 40);
		msvc.addStateImageFilename("knob_VOLTAGE_3.png", 60);
		msvc.addStateImageFilename("knob_VOLTAGE_4.png", 80);
		msvc.addStateImageFilename("knob_VOLTAGE_5.png", 99);
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
		ControlSliceStateImage.resourceImageFilePathPrefix = "blinkenbone/panelsim/panelsimKI10/images/";
		ControlSliceStateImage.resourceImageFileNamePrefix = "pdp10ki10_size="
				+ scaledBackgroundWidth + "_";

		// full file name is something like
		// "pdp10ki10_size=1200_coordinates.csv"
		ControlSliceStateImage.loadImageInfos("coordinates.csv");

		// background: load image
		backgroundVisualization.createStateImages();
		backgroundVisualization.setStateExact(1); // always visible

		/*
		 * all visualisations: 1. check: all controls != null 2. loadImages
		 */
		for (PanelKI10Control panelcontrol : controls)
			for (ControlSliceVisualization csv : panelcontrol.visualization) {
				if (csv.blinkenlightApiControl == null) {
					System.out.printf(
							"Panel definition error; blinkenlightApiControl for controlVisualization '%s' not found",
							csv.name);
				}
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
	}

	/*
	 * selftest: show all control images this is done by setting the panel into
	 * one of the test modes
	 */
	private boolean selftest;

	public void setSelftest(boolean selftest) {
		this.selftest = selftest;
		if (selftest)
			blinkenlightApiPanel
					.setMode(rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST);
		else
			blinkenlightApiPanel
					.setMode(rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_NORMAL);

		repaint();
	}

	public boolean getSelftest() {
		return selftest;
	}

	/*
	 * Get state image for one bit of a control in test mode
	 * generated combined state image
	 * null if no image available / useful
	 */
	public ControlSliceStateImage getTestStateImage(PanelKI10Control ki10c,
			ControlSliceVisualization csv, int testmode) {

		switch (ki10c.type) {
		case PDP10_LAMP: // show brightest led/lamp image in all test modes
			return csv.getStateImage(csv.maxState);
		case PDP10_KEY:
		case PDP10_SWITCH: //
			switch (testmode) {
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST:
				// lamptest: light button lamp, without changing "up/down" state
				switch (csv.getState()) {
				case LampButtonControlSliceVisualization.stateButtonDownLampOn:
				case LampButtonControlSliceVisualization.stateButtonDownLampOff:
					return csv.getStateImage(LampButtonControlSliceVisualization.stateButtonDownLampOn);
				case LampButtonControlSliceVisualization.stateButtonUpLampOn:
				case LampButtonControlSliceVisualization.stateButtonUpLampOff:
					return csv.getStateImage(LampButtonControlSliceVisualization.stateButtonUpLampOn);
				}
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST:
				// total test: show as "pressed and on"
				return csv.getStateImage(LampButtonControlSliceVisualization.stateButtonDownLampOn);
			}
			break;
		case PDP10_KNOB:
			switch (testmode) {
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST:
				return csv.getStateImage(csv.getState()); // why is this
															// necessary?
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST:
				return csv.getStateImage(csv.maxState);
			}
		case PDP10_INPUT:
		case PDP10_OUTPUT:
			// TODO
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

		ControlSliceStateImage cssi;

		// fill panel => frame around background image
		setForeground(borderColor);
		g.fillRect(0, 0, getWidth(), getHeight());

		// always draw background, is always visible
		if (backgroundVisualization != null
				&& (cssi = backgroundVisualization.getVisibleStateImage()) != null
				&& cssi.scaledStateImage != null) {
			g.drawImage(cssi.scaledStateImage, borderLeftRight, borderTopBottom, null);

		} else
			incomplete = true;

		/*
		 * TODO: Background logic:
		 */

		// incomplete |= (controlVisualization == null); ??????????????
		if (!incomplete) {
			for (PanelKI10Control ki10c : controls)
				for (ControlSliceVisualization csv : ki10c.visualization) {
					cssi = null;
					// selftest over Blinkenlight API:
					if (blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST
							|| blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST) {
						cssi = getTestStateImage(ki10c, csv, blinkenlightApiPanel.mode);
					} else {
						// show regular state
						// do not paint lamps in OFF state, background show them
						// // already (on 11/40, blackground is all black an
						// dimm lamps
						// must be painted even if OFF)
						if (ki10c.type == KI10ControlType.PDP10_LAMP && csv.getState() == 0)
							cssi = null;
						else
							cssi = csv.getStateImage(csv.getState());
					}
					if (cssi != null)
						g.drawImage(cssi.scaledStateImage,
								cssi.scaledPosition.x + borderLeftRight,
								cssi.scaledPosition.y + borderTopBottom, null);
					// csv.newAveragingState(); // new sampling interval
				}
		}
		if (incomplete)
			// no display, for WindowsBuilder designer
			super.paintComponent(g);
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
		for (PanelKI10Control ki10c : controls)
			for (ControlSliceVisualization csv : ki10c.visualization) {
				ControlSliceStateImage cssi = csv.getVisibleStateImage();
				if (cssi != null && stateImageClicked(clickpoint, csv, cssi))
					return csv;
			}
		// no visible state image was clicked
		// but there may be the picture of an "inactive" control
		// be painted onto the background.
		//
		// Check, wether any state image of a ControlSliceVisualization
		// could be under the click point
		for (PanelKI10Control ki10c : controls)
			for (ControlSliceVisualization csv : ki10c.visualization)
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
		Control c = csv.blinkenlightApiControl;
		PanelKI10Control ki10c = (PanelKI10Control) c.parent;
		switch (ki10c.type) {
		case PDP10_KNOB:
			if (csv.clickedStateImagePoint.x < 0) {
				// LEFT click in Knob Image = decrement, no roll-around
				if (csv.getState() > csv.minState)
					csv.setStateExact(csv.getNextLowerState());
			} else {
				// RIGHT click = increment, no roll-around
				if (csv.getState() < csv.maxState)
					csv.setStateExact(csv.getNextHigherState());
			}
			break;
		case PDP10_SWITCH:
			// toggle activate state ("press" state) of button. preserve
			// lamp state
			// toggle between state 1 and 0 (not found, so invisible)
			switch (csv.getState()) {
			case LampButtonControlSliceVisualization.stateButtonDownLampOn:
				if (ki10c.wiredFeedback)
					// up = off
					csv.setStateExact(LampButtonControlSliceVisualization.stateButtonUpLampOff);
				else
					csv.setStateExact(LampButtonControlSliceVisualization.stateButtonUpLampOn);
				break;
			case LampButtonControlSliceVisualization.stateButtonDownLampOff:
				csv.setStateExact(LampButtonControlSliceVisualization.stateButtonUpLampOff);
				break;
			case LampButtonControlSliceVisualization.stateButtonUpLampOn:
				csv.setStateExact(LampButtonControlSliceVisualization.stateButtonDownLampOn);
				break;
			case LampButtonControlSliceVisualization.stateButtonUpLampOff:
				if (ki10c.wiredFeedback)
					// down = on
					csv.setStateExact(LampButtonControlSliceVisualization.stateButtonDownLampOn);
				else
					csv.setStateExact(LampButtonControlSliceVisualization.stateButtonDownLampOff);
				break;
			}
			break;
		case PDP10_KEY:
			// activate button (= press down) while mouse pressed
			switch (csv.getState()) {
			case LampButtonControlSliceVisualization.stateButtonDownLampOn:
			case LampButtonControlSliceVisualization.stateButtonDownLampOff:
				break;
			case LampButtonControlSliceVisualization.stateButtonUpLampOn:
				csv.setStateExact(LampButtonControlSliceVisualization.stateButtonDownLampOn);
				break;
			case LampButtonControlSliceVisualization.stateButtonUpLampOff:
				if (ki10c.wiredFeedback)
					// down = on
					csv.setStateExact(LampButtonControlSliceVisualization.stateButtonDownLampOn);
				else
					csv.setStateExact(LampButtonControlSliceVisualization.stateButtonDownLampOff);
				break;
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
		Control c = csv.blinkenlightApiControl;
		PanelKI10Control ki10c = (PanelKI10Control) c.parent;
		switch (ki10c.type) {
		case PDP10_KEY:
			// deactivate button (= unpress) because mouse button is
			// released
			switch (csv.getState()) {
			case LampButtonControlSliceVisualization.stateButtonDownLampOn:
				if (ki10c.wiredFeedback)
					// up = off
					csv.setStateExact(LampButtonControlSliceVisualization.stateButtonUpLampOff);
				csv.setStateExact(LampButtonControlSliceVisualization.stateButtonUpLampOn);
				break;
			case LampButtonControlSliceVisualization.stateButtonDownLampOff:
				csv.setStateExact(LampButtonControlSliceVisualization.stateButtonUpLampOff);
				break;
			case LampButtonControlSliceVisualization.stateButtonUpLampOn:
			case LampButtonControlSliceVisualization.stateButtonUpLampOff:
				break;
			}
		default:
			;
		}
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
				// leave old iamge
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
		for (PanelKI10Control ki10c : controls)
			for (ControlSliceVisualization csv : ki10c.visualization) {
				csv.setStateExact(0);
			}
		// calc new control values
		inputImageState2BlinkenlightApiControlValues();
	}

	/*
	 * decode input (Switches) image controls into BlinkenLight API control
	 * values.
	 *
	 * This is used only for switches, which have only one state (active) So
	 * every ControlSlice gives a single bit, if it has a visible state image
	 */
	public void inputImageState2BlinkenlightApiControlValues() {
		for (PanelKI10Control ki10c : controls) {
			Control c = ki10c.inputcontrol;
			if (c != null)
				synchronized (c) {
					// RPC server may not change control values in parallel
					c.value = 0; // init value
					for (ControlSliceVisualization csv : ki10c.visualization)
						if (csv.getClass() == LampButtonControlSliceVisualization.class) {
							// control is a Lampbutton, and csv.state must
							// be checked for Button/up/down.
							// Button down -> Bit set!
							if (csv.getState() == LampButtonControlSliceVisualization.stateButtonDownLampOn
									|| csv.getState() == LampButtonControlSliceVisualization.stateButtonDownLampOff)
								c.value |= (1L << csv.controlSlicePosition);
						} else if (csv.getClass() == MultiStateControlSliceVisualization.class) {
							// knobs: value is simply the state
							c.value = csv.getState();
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
		for (PanelKI10Control ki10c : controls) {
			Control c = ki10c.outputcontrol;
			if (c != null)
				synchronized (c) {
					// RPC server may not change control values in parallel
					for (ControlSliceVisualization csv : ki10c.visualization)
						synchronized (csv) {
							boolean visible = false;
							// set visibility for all images of this control
							// is bit "control_bitpos" set in value?
							if ((c.value & (1L << csv.controlSlicePosition)) > 0)
								visible = true;
							// all outputs are LEDS, or LampButtons, or ....
							if (csv.getClass() == DimmableLedControlSliceVisualization.class) {
								// LEDs: : select between off and max
								if (visible)
									csv.setStateAveraging(csv.maxState);
								else
									csv.setStateAveraging(0);
							} else if (csv.getClass() == LampButtonControlSliceVisualization.class) {
								if (visible) { // button lamp ON
									if (csv.getState() == LampButtonControlSliceVisualization.stateButtonDownLampOff)
										csv.setStateExact(
												LampButtonControlSliceVisualization.stateButtonDownLampOn);
									if (csv.getState() == LampButtonControlSliceVisualization.stateButtonUpLampOff)
										csv.setStateExact(
												LampButtonControlSliceVisualization.stateButtonUpLampOn);
								} else { // button lamp OFF
									if (csv.getState() == LampButtonControlSliceVisualization.stateButtonDownLampOn)
										csv.setStateExact(
												LampButtonControlSliceVisualization.stateButtonDownLampOff);
									if (csv.getState() == LampButtonControlSliceVisualization.stateButtonUpLampOn)
										csv.setStateExact(
												LampButtonControlSliceVisualization.stateButtonUpLampOff);
								}
							} // else if knob ....
						}
					c.value_previous = c.value;
					/*
					 * // special logic: if the POWER LED is set from remote,
					 * // also the POWER button goes into "down_on state
					 * if (c == led_POWER.outputcontrol) {
					 * // set the lowest bit of the power lamp
					 * ControlSliceVisualization csv =
					 * button_POWER.visualization.get(0);
					 * if (c.value != 0)
					 * csv.setStateExact(LampButtonVisualization.
					 * stateButtonDownLampOn);
					 * else
					 * csv.setStateExact(LampButtonVisualization.
					 * stateButtonUpLampOff);
					 *
					 * }
					 */
				}
		}
	}

	/*
	 * Let the panel appear as "powerless":
	 * all lamps go dark, the power button goes to the "off" state.
	 */
	private void setPowerMode(int mode) {
		ControlSliceVisualization powerButtonCsv = button_POWER.visualization.get(0);
		// default: do not change power button state
		int newPowerbuttonMode = powerButtonCsv.getState();

		if (mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_POWERLESS) {
			// all BlinkenLight API output controls to state 0
			// Alternatively, lamps can be painted as OFF in paintComponent()
			for (PanelKI10Control panelcontrol : controls) {
				Control c = panelcontrol.outputcontrol;
				if (c != null)
					c.value = 0; // atomic, not synchronized
			}
			// Power button visual OFF
			newPowerbuttonMode = LampButtonControlSliceVisualization.stateButtonUpLampOff;
		} else {
			// make sure power button is ON in normal & test modes
			newPowerbuttonMode = LampButtonControlSliceVisualization.stateButtonDownLampOn;
		}
		// power button state: change only if user does not operate it
		if (currentMouseControlSliceVisualization != powerButtonCsv
				&& newPowerbuttonMode != powerButtonCsv.getState()) {
			// power button state changed
			powerButtonCsv.setStateExact(newPowerbuttonMode);
			// update API power switch control
			inputImageState2BlinkenlightApiControlValues();
		}
	}

	/*
	 * called by observable blinkenlightApiPanel on change of output controls or
	 * mode
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
