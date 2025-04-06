/* PanelPDP8I.java: A JPanel, which displays the Blinkenlight panel
 					as stack of ControlImagesDescription

   Copyright (c) 2016-2016, Joerg Hoppe
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
   20-Feb-2016  JH      added PANEL_MODE_POWERLESS
   09-Feb-2016  JH      created


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

package blinkenbone.panelsim.panelsimPDP8I;

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
import java.awt.AlphaComposite;
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

import sun.java2d.pipe.SpanShapeRenderer.Composite;
import blinkenbone.blinkenlight_api.Control;
import blinkenbone.blinkenlight_api.Control.ControlType;
import blinkenbone.blinkenlight_api.Panel;
import blinkenbone.panelsim.ControlSliceStateImage;
import blinkenbone.panelsim.ControlSliceVisualization;
import blinkenbone.panelsim.DimmableLightbulbControlSliceVisualization;
import blinkenbone.panelsim.LampButtonControlSliceVisualization;
import blinkenbone.panelsim.MultiStateControlSliceVisualization;
import blinkenbone.panelsim.ResourceManager;
import blinkenbone.panelsim.TwoStateControlSliceVisualization;
import blinkenbone.panelsim.panelsimKI10.PanelKI10Control;
import blinkenbone.panelsim.panelsimPDP8I.PanelPDP8IControl.PanelPDP8IControlType;
import blinkenbone.rpcgen.rpc_blinkenlight_api;

public class PanelPDP8I extends JPanel implements Observer {

	// public static String version = "v 1.01" ;
	private static final long serialVersionUID = 1L;

	/*
	 * frame between panel border and background image
	 */
	private int borderTopBottom = 0;
	private int borderLeftRight = 0;
	private Color borderColor = Color.gray;

	public Panel blinkenlightApiPanel; // the Blinkenlight API panel

	public ArrayList<PanelPDP8IControl> controls;

	// links to well defined Blinkenlight Api controls
	PanelPDP8IControl switches_data_field;
	PanelPDP8IControl switches_inst_field;
	PanelPDP8IControl switches_sr; // main switch registers
	PanelPDP8IControl switch_start;
	PanelPDP8IControl switch_load_add;
	PanelPDP8IControl switch_dep;
	PanelPDP8IControl switch_exam;
	PanelPDP8IControl switch_cont;
	PanelPDP8IControl switch_stop;
	PanelPDP8IControl switch_sing_step;
	PanelPDP8IControl switch_sing_inst;

	PanelPDP8IControl keyswitch_power;
	PanelPDP8IControl keyswitch_panel_lock;

	PanelPDP8IControl lamps_data_field;
	PanelPDP8IControl lamps_inst_field;
	PanelPDP8IControl lamps_program_counter;
	PanelPDP8IControl lamps_memory_address;
	PanelPDP8IControl lamps_memory_buffer;
	PanelPDP8IControl lamp_link;
	PanelPDP8IControl lamps_accumulator;
	PanelPDP8IControl lamps_multiplier_quotient;
	PanelPDP8IControl lamp_and;
	PanelPDP8IControl lamp_tad;
	PanelPDP8IControl lamp_isz;
	PanelPDP8IControl lamp_dca;
	PanelPDP8IControl lamp_jms;
	PanelPDP8IControl lamp_jmp;
	PanelPDP8IControl lamp_iot;
	PanelPDP8IControl lamp_opr;
	PanelPDP8IControl lamp_fetch;
	PanelPDP8IControl lamp_execute;
	PanelPDP8IControl lamp_defer;
	PanelPDP8IControl lamp_word_count;
	PanelPDP8IControl lamp_current_address;
	PanelPDP8IControl lamp_break;
	PanelPDP8IControl lamp_ion;
	PanelPDP8IControl lamp_pause;
	PanelPDP8IControl lamp_run;
	PanelPDP8IControl lamps_step_counter;

	PanelPDP8IControl switch_panel_lock; // dummy: Blinkenlight control without
											// visual

	// the background
	TwoStateControlSliceVisualization backgroundVisualization;

	private ResourceManager resourceManager;

	private int scaledBackgroundWidth; // width of background image, after
										// load()

	/*
	 *
	 */
	public PanelPDP8I(ResourceManager resourceManager) {
		controls = new ArrayList<PanelPDP8IControl>();
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
		// Evaluation in panelPDP8I.clearUserinput()
		keyswitch_power.resetState = commandlineParameters.getInt("power") ;

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
		return new String("PDP-8/I panel simulation (Blinkenlight API server interface) "
				+ PanelsimPDP8I_app.version);
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

		p = new Panel("PDP8I"); 
		p.info = "Photorealistic simulation of a PDP-8/I panel. Java.";
		p.default_radix = 8;

		/*
		 * Build List of controls: interconnected lists of panel 11/70 controls
		 * and BlinkenlightAPI controls Control definitions exact like in
		 * blinkenlightd.conf! SimH relies on those!.
		 *
		 * Knob feed back LEds are hard wired to the knob positions.
		 * this is done in the "KNOB_LEED_FEEDBACK" control
		 */
		// compare controls names with Simh and PiDP 8 server
		controls.add(
				switches_data_field = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_SWITCH,
						new Control("DF", ControlType.input_switch, 3), null, p));
		controls.add(
				switches_inst_field = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_SWITCH,
						new Control("IF", ControlType.input_switch, 3), null, p));
		controls.add(switches_sr = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_SWITCH,
				new Control("SR", ControlType.input_switch, 12), null, p));

		controls.add(switch_start = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_KEY,
				new Control("Start", ControlType.input_switch, 1), null, p));
		controls.add(switch_load_add = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_KEY,
				new Control("Load Add", ControlType.input_switch, 1), null, p));
		controls.add(switch_dep = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_KEY,
				new Control("Dep", ControlType.input_switch, 1), null, p));
		controls.add(switch_exam = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_KEY,
				new Control("Exam", ControlType.input_switch, 1), null, p));
		controls.add(switch_cont = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_KEY,
				new Control("Cont", ControlType.input_switch, 1), null, p));
		controls.add(switch_stop = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_KEY,
				new Control("Stop", ControlType.input_switch, 1), null, p));

		controls.add(switch_sing_step = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_SWITCH,
				new Control("Sing Step", ControlType.input_switch, 1), null, p));
		controls.add(switch_sing_inst = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_SWITCH,
				new Control("Sing Inst", ControlType.input_switch, 1), null, p));

		controls.add(keyswitch_power = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_SWITCH,
				new Control("POWER", ControlType.input_switch, 1), null, p));
		controls.add(
				keyswitch_panel_lock = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_SWITCH,
						new Control("PANEL LOCK", ControlType.input_switch, 1), null, p));

		controls.add(lamps_data_field = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP,
				null, new Control("Data Field", ControlType.output_lamp, 3), p));
		controls.add(lamps_inst_field = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP,
				null, new Control("Inst Field", ControlType.output_lamp, 3), p));
		controls.add(
				lamps_program_counter = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP,
						null, new Control("Program Counter", ControlType.output_lamp, 12), p));
		controls.add(
				lamps_memory_address = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP,
						null, new Control("Memory Address", ControlType.output_lamp, 12), p));
		controls.add(
				lamps_memory_buffer = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP,
						null, new Control("Memory Buffer", ControlType.output_lamp, 12), p));
		controls.add(lamp_link = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Link", ControlType.output_lamp, 1), p));
		controls.add(lamps_accumulator = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP,
				null, new Control("Accumulator", ControlType.output_lamp, 12), p));
		controls.add(lamps_step_counter = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP,
				null, new Control("Step Counter", ControlType.output_lamp, 5), p));
		controls.add(lamps_multiplier_quotient = new PanelPDP8IControl(
				PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Multiplier Quotient", ControlType.output_lamp, 12), p));

		controls.add(lamp_and = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("And", ControlType.output_lamp, 1), p));
		controls.add(lamp_tad = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Tad", ControlType.output_lamp, 1), p));
		controls.add(lamp_isz = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Isz", ControlType.output_lamp, 1), p));
		controls.add(lamp_dca = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Dca", ControlType.output_lamp, 1), p));
		controls.add(lamp_jms = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Jms", ControlType.output_lamp, 1), p));
		controls.add(lamp_jmp = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Jmp", ControlType.output_lamp, 1), p));
		controls.add(lamp_iot = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Iot", ControlType.output_lamp, 1), p));
		controls.add(lamp_opr = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Opr", ControlType.output_lamp, 1), p));

		controls.add(lamp_fetch = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Fetch", ControlType.output_lamp, 1), p));
		controls.add(lamp_execute = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Execute", ControlType.output_lamp, 1), p));
		controls.add(lamp_defer = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Defer", ControlType.output_lamp, 1), p));
		controls.add(lamp_word_count = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP,
				null, new Control("Word Count", ControlType.output_lamp, 1), p));
		controls.add(
				lamp_current_address = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP,
						null, new Control("Current Address", ControlType.output_lamp, 1), p));
		controls.add(lamp_break = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Break", ControlType.output_lamp, 1), p));

		controls.add(lamp_ion = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Ion", ControlType.output_lamp, 1), p));
		controls.add(lamp_pause = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Pause", ControlType.output_lamp, 1), p));
		controls.add(lamp_run = new PanelPDP8IControl(PanelPDP8IControlType.PDP8_LAMP, null,
				new Control("Run", ControlType.output_lamp, 1), p));

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
		for (PanelPDP8IControl panelcontrol : controls) {
			panelcontrol.visualization.clear();
		}

		// All coordinates must have been loaded: loadImageCoordinates()

		// Attention: On PDP8 the MSb is labeld "0" and the LSb has the highest
		// number!
		// image names contain PDP8 bit codes.
		switches_data_field.visualization.add(new TwoStateControlSliceVisualization(
				"switch_data_field_0_off.png", "switch_data_field_0_on.png",
				switches_data_field, switches_data_field.inputcontrol, 2));
		switches_data_field.visualization.add(new TwoStateControlSliceVisualization(
				"switch_data_field_1_off.png", "switch_data_field_1_on.png",
				switches_data_field, switches_data_field.inputcontrol, 1));
		switches_data_field.visualization.add(new TwoStateControlSliceVisualization(
				"switch_data_field_2_off.png", "switch_data_field_2_on.png",
				switches_data_field, switches_data_field.inputcontrol, 0));

		switches_inst_field.visualization.add(new TwoStateControlSliceVisualization(
				"switch_inst_field_0_off.png", "switch_inst_field_0_on.png",
				switches_inst_field, switches_inst_field.inputcontrol, 2));
		switches_inst_field.visualization.add(new TwoStateControlSliceVisualization(
				"switch_inst_field_1_off.png", "switch_inst_field_1_on.png",
				switches_inst_field, switches_inst_field.inputcontrol, 1));
		switches_inst_field.visualization.add(new TwoStateControlSliceVisualization(
				"switch_inst_field_2_off.png", "switch_inst_field_2_on.png",
				switches_inst_field, switches_inst_field.inputcontrol, 0));

		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_00_off.png",
				"switch_sr_00_on.png", switches_sr, switches_sr.inputcontrol, 11));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_01_off.png",
				"switch_sr_01_on.png", switches_sr, switches_sr.inputcontrol, 10));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_02_off.png",
				"switch_sr_02_on.png", switches_sr, switches_sr.inputcontrol, 9));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_03_off.png",
				"switch_sr_03_on.png", switches_sr, switches_sr.inputcontrol, 8));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_04_off.png",
				"switch_sr_04_on.png", switches_sr, switches_sr.inputcontrol, 7));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_05_off.png",
				"switch_sr_05_on.png", switches_sr, switches_sr.inputcontrol, 6));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_06_off.png",
				"switch_sr_06_on.png", switches_sr, switches_sr.inputcontrol, 5));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_07_off.png",
				"switch_sr_07_on.png", switches_sr, switches_sr.inputcontrol, 4));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_08_off.png",
				"switch_sr_08_on.png", switches_sr, switches_sr.inputcontrol, 3));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_09_off.png",
				"switch_sr_09_on.png", switches_sr, switches_sr.inputcontrol, 2));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_10_off.png",
				"switch_sr_10_on.png", switches_sr, switches_sr.inputcontrol, 1));
		switches_sr.visualization.add(new TwoStateControlSliceVisualization("switch_sr_11_off.png",
				"switch_sr_11_on.png", switches_sr, switches_sr.inputcontrol, 0));

		switch_start.visualization.add(new TwoStateControlSliceVisualization("switch_start_off.png",
				"switch_start_on.png", switch_start, switch_start.inputcontrol, 0));
		switch_load_add.visualization.add(new TwoStateControlSliceVisualization(
				"switch_load_add_off.png", "switch_load_add_on.png", switch_load_add,
				switch_load_add.inputcontrol, 0));
		switch_dep.visualization.add(new TwoStateControlSliceVisualization("switch_dep_off.png",
				"switch_dep_on.png", switch_dep, switch_dep.inputcontrol, 0));
		switch_exam.visualization.add(new TwoStateControlSliceVisualization("switch_exam_off.png",
				"switch_exam_on.png", switch_exam, switch_exam.inputcontrol, 0));
		switch_cont.visualization.add(new TwoStateControlSliceVisualization("switch_cont_off.png",
				"switch_cont_on.png", switch_cont, switch_cont.inputcontrol, 0));
		switch_stop.visualization.add(new TwoStateControlSliceVisualization("switch_stop_off.png",
				"switch_stop_on.png", switch_stop, switch_stop.inputcontrol, 0));
		switch_sing_step.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sing_step_off.png", "switch_sing_step_on.png", switch_sing_step,
				switch_sing_step.inputcontrol, 0));
		switch_sing_inst.visualization.add(new TwoStateControlSliceVisualization(
				"switch_sing_inst_off.png", "switch_sing_inst_on.png", switch_sing_inst,
				switch_sing_inst.inputcontrol, 0));

		keyswitch_power.visualization.add(new TwoStateControlSliceVisualization(
				"keylock_power_off.png", "keylock_power_on.png", keyswitch_power,
				keyswitch_power.inputcontrol, 0));
		keyswitch_panel_lock.visualization.add(new TwoStateControlSliceVisualization(
				"keylock_panel_lock_off.png", "keylock_panel_lock_on.png", keyswitch_panel_lock,
				keyswitch_panel_lock.inputcontrol, 0));

		DimmableLightbulbControlSliceVisualization.defaultAveragingInterval_ms = 333;
		// create all lightbulbs with a low pass of 1/3 sec.
		// this is quite slow, but we need to remove flicker.

		// dimmable lamps use variable transparency, so they do not disturb the
		// background if they are "dim"
		lamps_data_field.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_data_field_0_on.png",
						lamps_data_field, lamps_data_field.outputcontrol, 2, true));
		lamps_data_field.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_data_field_1_on.png",
						lamps_data_field, lamps_data_field.outputcontrol, 1, true));
		lamps_data_field.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_data_field_2_on.png",
						lamps_data_field, lamps_data_field.outputcontrol, 0, true));

		lamps_inst_field.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_inst_field_0_on.png",
						lamps_inst_field, lamps_inst_field.outputcontrol, 2, true));
		lamps_inst_field.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_inst_field_1_on.png",
						lamps_inst_field, lamps_inst_field.outputcontrol, 1, true));
		lamps_inst_field.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_inst_field_2_on.png",
						lamps_inst_field, lamps_inst_field.outputcontrol, 0, true));

		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_00_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 11, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_01_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 10, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_02_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 9, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_03_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 8, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_04_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 7, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_05_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 6, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_06_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 5, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_07_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 4, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_08_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 3, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_09_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 2, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_10_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 1, true));
		lamps_program_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_program_counter_11_on.png",
						lamps_program_counter, lamps_program_counter.outputcontrol, 0, true));

		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_00_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 11, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_01_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 10, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_02_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 9, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_03_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 8, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_04_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 7, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_05_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 6, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_06_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 5, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_07_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 4, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_08_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 3, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_09_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 2, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_10_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 1, true));
		lamps_memory_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_address_11_on.png",
						lamps_memory_address, lamps_memory_address.outputcontrol, 0, true));

		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_00_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 11, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_01_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 10, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_02_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 9, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_03_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 8, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_04_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 7, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_05_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 6, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_06_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 5, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_07_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 4, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_08_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 3, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_09_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 2, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_10_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 1, true));
		lamps_memory_buffer.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_memory_buffer_11_on.png",
						lamps_memory_buffer, lamps_memory_buffer.outputcontrol, 0, true));

		lamp_link.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_link_on.png",
				lamp_link, lamp_link.outputcontrol, 0, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_00_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 11, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_01_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 10, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_02_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 9, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_03_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 8, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_04_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 7, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_05_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 6, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_06_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 5, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_07_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 4, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_08_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 3, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_09_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 2, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_10_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 1, true));
		lamps_accumulator.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_accumulator_11_on.png",
						lamps_accumulator, lamps_accumulator.outputcontrol, 0, true));

		lamps_step_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_step_counter_0_on.png",
						lamps_step_counter, lamps_step_counter.outputcontrol, 4, true));
		lamps_step_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_step_counter_1_on.png",
						lamps_step_counter, lamps_step_counter.outputcontrol, 3, true));
		lamps_step_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_step_counter_2_on.png",
						lamps_step_counter, lamps_step_counter.outputcontrol, 2, true));
		lamps_step_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_step_counter_3_on.png",
						lamps_step_counter, lamps_step_counter.outputcontrol, 1, true));
		lamps_step_counter.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_step_counter_4_on.png",
						lamps_step_counter, lamps_step_counter.outputcontrol, 0, true));

		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_00_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 11, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_01_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 10, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_02_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 9, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_03_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 8, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_04_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 7, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_05_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 6, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_06_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 5, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_07_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 4, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_08_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 3, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_09_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 2, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_10_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 1, true));
		lamps_multiplier_quotient.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_multiplier_quotient_11_on.png", lamps_multiplier_quotient,
				lamps_multiplier_quotient.outputcontrol, 0, true));

		lamp_and.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_and_on.png",
				lamp_and, lamp_and.outputcontrol, 0, true));
		lamp_tad.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_tad_on.png",
				lamp_tad, lamp_tad.outputcontrol, 0, true));
		lamp_isz.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_isz_on.png",
				lamp_isz, lamp_isz.outputcontrol, 0, true));
		lamp_dca.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_dca_on.png",
				lamp_dca, lamp_dca.outputcontrol, 0, true));
		lamp_jms.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_jms_on.png",
				lamp_jms, lamp_jms.outputcontrol, 0, true));
		lamp_jmp.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_jmp_on.png",
				lamp_jmp, lamp_jmp.outputcontrol, 0, true));
		lamp_iot.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_iot_on.png",
				lamp_iot, lamp_iot.outputcontrol, 0, true));
		lamp_opr.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_opr_on.png",
				lamp_opr, lamp_opr.outputcontrol, 0, true));

		lamp_fetch.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_fetch_on.png", lamp_fetch, lamp_fetch.outputcontrol, 0, true));
		lamp_execute.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_execute_on.png", lamp_execute, lamp_execute.outputcontrol, 0, true));
		lamp_defer.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_defer_on.png", lamp_defer, lamp_defer.outputcontrol, 0, true));
		lamp_word_count.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_word_count_on.png",
						lamp_word_count, lamp_word_count.outputcontrol, 0, true));
		lamp_current_address.visualization
				.add(new DimmableLightbulbControlSliceVisualization("lightbulb_current_address_on.png",
						lamp_current_address, lamp_current_address.outputcontrol, 0, true));
		lamp_break.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_break_on.png", lamp_break, lamp_break.outputcontrol, 0, true));

		lamp_ion.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_ion_on.png",
				lamp_ion, lamp_ion.outputcontrol, 0, true));
		lamp_pause.visualization.add(new DimmableLightbulbControlSliceVisualization(
				"lightbulb_pause_on.png", lamp_pause, lamp_pause.outputcontrol, 0, true));
		lamp_run.visualization.add(new DimmableLightbulbControlSliceVisualization("lightbulb_run_on.png",
				lamp_run, lamp_run.outputcontrol, 0, true));
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
		ControlSliceStateImage.resourceImageFilePathPrefix = "blinkenbone/panelsim/panelsimPDP8I/images/";
		ControlSliceStateImage.resourceImageFileNamePrefix = "pdp8i_size="
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
		for (PanelPDP8IControl panelcontrol : controls)
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
	public ControlSliceStateImage getTestStateImage(PanelPDP8IControl panelcontrol,
			ControlSliceVisualization csv, int testmode) {

		switch (panelcontrol.type) {
		case PDP8_LAMP: // show brightest led/lamp image in all test modes
			return csv.getStateImage(csv.maxState);
		case PDP8_SWITCH: //
		case PDP8_KEY: //
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
		// System.out.printf("paintComponent(incomplete=%b)\n", incomplete);

		// incomplete |= (controlVisualization == null); ??????????????
		if (!incomplete) {
			for (PanelPDP8IControl panelcontrol : controls)
				for (ControlSliceVisualization csv : panelcontrol.visualization) {
					cssi = null;
					// selftest over Blinkenlight API:
					if (blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST
							|| blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST) {
						cssi = getTestStateImage(panelcontrol, csv, blinkenlightApiPanel.mode);
					} else {
						// show regular state. cssi == null, if "inactive" state
						// already painten on background
						cssi = csv.getStateImage(csv.getState());
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
		for (PanelPDP8IControl panelcontrol : controls)
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
		for (PanelPDP8IControl panelcontrol : controls)
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
	 * The blinkenlight API control value is calcuaalted from the visible
	 * statte.
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
		PanelPDP8IControl panelcontrol = (PanelPDP8IControl) csv.panelControl;
		switch (panelcontrol.type) {
		case PDP8_SWITCH:
			// toggle between state 1 and 0 on separate clicks
			if (csv.getState() == 0)
				csv.setStateExact(1);
			else
				csv.setStateExact(0);
			break;
		case PDP8_KEY:
			// activate only while mouse pressed down
			csv.setStateExact(1);
			// if (panelcontrol == switch_LAMPTEST) no lamptest on pdp8
			// setSelftest(rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST);
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
		PanelPDP8IControl panelcontrol = (PanelPDP8IControl) csv.panelControl;
		switch (panelcontrol.type) {
		case PDP8_KEY:
			// deactivate button (= unpress) because mouse button is released
			csv.setStateExact(0);
			break;
		default:
			;
		}
		// if (panelcontrol == switch_LAMPTEST)
		// setSelftest(rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_NORMAL);
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
		for (PanelPDP8IControl panelcontrol : controls)
			// default knob position can be set over command line
			if (panelcontrol == keyswitch_power)
				keyswitch_power.visualization.get(0).setStateExact(keyswitch_power.resetState);
			else
				for (ControlSliceVisualization csv : panelcontrol.visualization) {
					csv.setStateExact(0);
				}
		// calc new control values
		inputImageState2BlinkenlightApiControlValues();
	}

	/*
	 * decode input (Switches) image controls into BlinkenLight API control
	 * values.
	 *
	 * LED control:
	 */
	public void inputImageState2BlinkenlightApiControlValues() {

		for (PanelPDP8IControl panelcontrol : controls) {
			Control c = panelcontrol.inputcontrol;
			if (c != null)
				synchronized (c) {
					// RPC server may not change control values in parallel
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

	/*
	 * set the visible state of control slices according to BlinkenLight API
	 * control values.
	 *
	 * Here all output controls are LEDs with 8 brightness states. At first, set
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
		for (PanelPDP8IControl panelcontrol : controls) {
			Control c = panelcontrol.outputcontrol;
			if (c != null)
				synchronized (c) {
					// RPC server may not change control values in parallel
					for (ControlSliceVisualization csv : panelcontrol.visualization)
						synchronized (csv) {
							// set visibility for all images of this control
							boolean visible = false;
							// all other: Lightbulb image shows a single bit of
							// value.
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
	 * all lamps go dark, the power button goes to the "off" state.
	 */
	private void setPowerMode(int mode) {
		ControlSliceVisualization powerSwitchCsv = keyswitch_power.visualization.get(0);

		// default: do not change power button state
		int newPowerSwitchState = powerSwitchCsv.getState();

		if (mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_POWERLESS) {
			// all BlinkenLight API output controls to state 0
			// Alternatively, lamps can be painted as OFF in paintComponent()
			for (PanelPDP8IControl panelcontrol : controls) {
				Control c = panelcontrol.outputcontrol;
				if (c != null)
					c.value = 0; // atomic, not synchronized
			}
			// Power button visual OFF
			newPowerSwitchState = 0;
		} else {
			// make sure power button is ON in normal & test modes
			newPowerSwitchState = 1;
		}
		// power button state: change only if user does not operate it
		if (currentMouseControlSliceVisualization != powerSwitchCsv
				&& newPowerSwitchState != powerSwitchCsv.getState()) {
			// power button state changed
			powerSwitchCsv.setStateExact(newPowerSwitchState);
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
