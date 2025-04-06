/* PanelPDP15.java: A JPanel, which displays the Blinkenlight panel
 					as stack of ControlImagesDescription

   Copyright (c) 2016, Joerg Hoppe
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

   26-Jul-2016  JH  cloned from PDP8/i


   A JPanel, which displays the Blinkenlight panel as stack of ControlImages.

   http://www.java2s.com/Code/Java/Swing-JFC/Panelwithbackgroundimage.htm

   Also functions to integrate the	Blinkenlight API panel & control structs
   with ControlImages

   PDP-15 is special in three ways:
   1. each switch image contains the shadow of its right neighbor
   	so a switch image is defgined by the ControlSlice state and the state of the nighbor
   	This is handled by TwoStateSituativeControlSliceVisualization

   2. because the iamges interact, the paint order is significant.
      Images must be painted in the order of the CSV file.

   3. the knob can not be read in normal view (side view)
     Therefore several "dispalymodes" are defined
     the knob API control has one visuaization for each displaymode
      (normally there's only on visualizaton (=set of ControlSlice Visualoization)
      for a control

 */

package blinkenbone.panelsim.panelsimPDP15;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
import blinkenbone.panelsim.TwoStateSituativeControlSliceVisualization;
import blinkenbone.panelsim.panelsimPDP15.PanelPDP15Control.PanelPDP15ControlType;
import blinkenbone.rpcgen.rpc_blinkenlight_api;

public class PanelPDP15 extends JPanel implements Observer {

	// public static String version = "v 1.01" ;
	private static final long serialVersionUID = 1L;

	/*
	 * frame between panel border and background image
	 */
	private int borderTopBottom = 0;
	private int borderLeftRight = 0;
	private Color borderColor = Color.gray;

	static final long DISPLAYMODE_TIMEOUT_MS = 3000;
	int displayMode; // one of PanelPDP15Control.DISPLAYMODE_*
	long displayModeUpdateTime_ms = 0;

	public Panel blinkenlightApiPanel; // the Blinkenlight API panel

	public ArrayList<PanelPDP15Control> controls;

	/*
	 * PDP-15 controls which are accessible over Blinkenlight API
	 */

	// *** Indicator Board ***
	PanelPDP15Control lamp_dch_active;
	PanelPDP15Control lamps_api_states_active;
	PanelPDP15Control lamp_api_enable;
	PanelPDP15Control lamp_pi_active;
	PanelPDP15Control lamp_pi_enable;
	PanelPDP15Control lamp_mode_index;
	PanelPDP15Control lamp_major_state_fetch;
	PanelPDP15Control lamp_major_state_inc;
	PanelPDP15Control lamp_major_state_defer;
	PanelPDP15Control lamp_major_state_eae;
	PanelPDP15Control lamp_major_state_exec;
	PanelPDP15Control lamps_time_states;

	PanelPDP15Control lamp_extd;
	PanelPDP15Control lamp_clock;
	PanelPDP15Control lamp_error;
	PanelPDP15Control lamp_prot;
	PanelPDP15Control lamp_link;
	PanelPDP15Control lamps_register;

	PanelPDP15Control lamp_power;
	PanelPDP15Control lamp_run;
	PanelPDP15Control lamps_instruction;
	PanelPDP15Control lamp_instruction_defer;
	PanelPDP15Control lamp_instruction_index;
	PanelPDP15Control lamps_memory_buffer;

	// *** Switch Board ***
	PanelPDP15Control switch_stop;
	PanelPDP15Control switch_reset;
	PanelPDP15Control switch_read_in;
	PanelPDP15Control switch_reg_group;
	PanelPDP15Control switch_clock;
	PanelPDP15Control switch_bank_mode;
	PanelPDP15Control switch_rept;
	PanelPDP15Control switch_prot;
	PanelPDP15Control switch_sing_time;
	PanelPDP15Control switch_sing_step;
	PanelPDP15Control switch_sing_inst;
	PanelPDP15Control switches_address;

	PanelPDP15Control switch_start;
	PanelPDP15Control switch_exec;
	PanelPDP15Control switch_cont;
	PanelPDP15Control switch_deposit_this; // visual and API
	PanelPDP15Control switch_deposit_next; // visual, not API
	PanelPDP15Control switch_examine_this; // visual and API
	PanelPDP15Control switch_examine_next; // visual, not API
	PanelPDP15Control switch_deposit_examine_next; // invisible, API only
	PanelPDP15Control switches_data;

	PanelPDP15Control switch_power; // invisible, API only
	PanelPDP15Control knob_repeat_rate;

	PanelPDP15Control knob_register_select;

	// the global background
	TwoStateControlSliceVisualization backgroundGlobalVisualization;
	// background around knobs in frontal view
	TwoStateControlSliceVisualization backgroundKnobsFrontalVisualization;

	private ResourceManager resourceManager;

	private int scaledBackgroundWidth; // width of background image, after
										// load()

	/*
	 *
	 */
	public PanelPDP15(ResourceManager resourceManager) {
		controls = new ArrayList<PanelPDP15Control>();
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
		// Evaluation in panelPDP15.clearUserinput()
		knob_repeat_rate.resetState = commandlineParameters.getInt("repeat_rate");
		knob_register_select.resetState = commandlineParameters.getInt("register_select");

		// do state initialization
		clearUserinput();

		displayMode = PanelPDP15Control.DISPLAYMODE_NORMAL;

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
		return new String("PDP-15 panel simulation (Blinkenlight API server interface) " + PanelsimPDP15_app.version);
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

		p = new Panel("PDP15");
		p.info = "Photorealistic simulation of a PDP-15 panel. Java.";
		p.default_radix = 8;

		/*
		 * Build List of controls: interconnected lists of panel 11/70 controls
		 * and BlinkenlightAPI controls Control definitions exact like in
		 * blinkenlightd.conf! SimH relies on those!.
		 */
		// compare controls names with SimH and PDP 15 server

		controls.add(lamp_dch_active = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("DCH_ACTIVE", ControlType.output_lamp, 1), p));
		controls.add(lamps_api_states_active = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("API_STATES_ACTIVE", ControlType.output_lamp, 8), p));
		controls.add(lamp_api_enable = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("API_ENABLE", ControlType.output_lamp, 1), p));
		controls.add(lamp_pi_active = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("PI_ACTIVE", ControlType.output_lamp, 1), p));
		controls.add(lamp_pi_enable = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("PI_ENABLE", ControlType.output_lamp, 1), p));
		controls.add(lamp_mode_index = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("MODE_INDEX", ControlType.output_lamp, 1), p));
		controls.add(lamp_major_state_fetch = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("STATE_FETCH", ControlType.output_lamp, 1), p));
		controls.add(lamp_major_state_inc = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("STATE_INC", ControlType.output_lamp, 1), p));
		controls.add(lamp_major_state_defer = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("STATE_DEFER", ControlType.output_lamp, 1), p));
		controls.add(lamp_major_state_eae = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("STATE_EAE", ControlType.output_lamp, 1), p));
		controls.add(lamp_major_state_exec = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("STATE_EXEC", ControlType.output_lamp, 1), p));
		controls.add(lamps_time_states = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("TIME_STATES", ControlType.output_lamp, 3), p));
		controls.add(lamp_extd = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("EXTD", ControlType.output_lamp, 3), p));
		controls.add(lamp_clock = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("CLOCK", ControlType.output_lamp, 3), p));
		controls.add(lamp_error = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("ERROR", ControlType.output_lamp, 3), p));
		controls.add(lamp_prot = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("PROT", ControlType.output_lamp, 3), p));
		controls.add(lamp_link = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("LINK", ControlType.output_lamp, 3), p));
		controls.add(lamps_register = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("REGISTER", ControlType.output_lamp, 18), p));
		controls.add(lamp_power = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("POWER", ControlType.output_lamp, 1), p));
		controls.add(lamp_run = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("RUN", ControlType.output_lamp, 1), p));
		controls.add(lamps_instruction = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("INSTRUCTION", ControlType.output_lamp, 4), p));
		controls.add(lamp_instruction_defer = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("INSTRUCTION_DEFER", ControlType.output_lamp, 1), p));
		controls.add(lamp_instruction_index = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("INSTRUCTION_INDEX", ControlType.output_lamp, 1), p));
		controls.add(lamps_memory_buffer = new PanelPDP15Control(PanelPDP15ControlType.PDP15_LAMP,
				new Control("MEMORY_BUFFER", ControlType.output_lamp, 18), p));

		controls.add(switch_stop = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("STOP", ControlType.input_switch, 1), p));
		controls.add(switch_reset = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("RESET", ControlType.input_switch, 1), p));
		controls.add(switch_read_in = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("READ_IN", ControlType.input_switch, 1), p));
		controls.add(switch_reg_group = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("REG_GROUP", ControlType.input_switch, 1), p));
		controls.add(switch_clock = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("CLOCK", ControlType.input_switch, 1), p));
		controls.add(switch_bank_mode = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("BANK_MODE", ControlType.input_switch, 1), p));
		controls.add(switch_rept = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("REPT", ControlType.input_switch, 1), p));
		controls.add(switch_prot = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("PROT", ControlType.input_switch, 1), p));
		controls.add(switch_sing_time = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("SING_TIME", ControlType.input_switch, 1), p));
		controls.add(switch_sing_step = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("SING_STEP", ControlType.input_switch, 1), p));
		controls.add(switch_sing_inst = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("SING_INST", ControlType.input_switch, 1), p));
		controls.add(switches_address = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("ADDRESS", ControlType.input_switch, 15), p));
		controls.add(switch_start = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("START", ControlType.input_switch, 1), p));
		controls.add(switch_exec = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("EXECUTE", ControlType.input_switch, 1), p));
		controls.add(switch_cont = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("CONT", ControlType.input_switch, 1), p));
		controls.add(switch_deposit_this = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("DEPOSIT_THIS", ControlType.input_switch, 1), p));
		// DEPOSIT/EXAMINE NEXT: not implemented on physical panel and not
		// needed for API,
		// see "switch_deposit_examine_next". Needed here because operated by
		// user
		controls.add(switch_deposit_next = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("DEPOSIT_NEXT", ControlType.input_switch, 1), p));
		controls.add(switch_examine_this = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("EXAMINE_THIS", ControlType.input_switch, 1), p));
		controls.add(switch_examine_next = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("EXAMINE_NEXT", ControlType.input_switch, 1), p));
		// virtual control for API: "pressed" if "EXAMINE_NEXT" or
		// "DEPOSIT_NEXT"
		controls.add(switch_deposit_examine_next = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KEY,
				new Control("DEP_EXAM_NEXT", ControlType.input_switch, 1), p));
		controls.add(switches_data = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("DATA", ControlType.input_switch, 18), p));
		// power is virtual: visible control is knob_repeat_rate
		controls.add(switch_power = new PanelPDP15Control(PanelPDP15ControlType.PDP15_SWITCH,
				new Control("POWER", ControlType.input_switch, 18), p));
		controls.add(knob_repeat_rate = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KNOB,
				new Control("REPEAT_RATE", ControlType.input_knob, 8), p));
		// coded as moving "1" bit
		controls.add(knob_register_select = new PanelPDP15Control(PanelPDP15ControlType.PDP15_KNOB,
				new Control("REGISTER_SELECT", ControlType.input_knob, 12), p));

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
		backgroundGlobalVisualization = new TwoStateControlSliceVisualization("background.png", null, null, 0);
		backgroundKnobsFrontalVisualization = new TwoStateControlSliceVisualization("knobs_frontal_background.png",
				null, null, 0);

		// clear visualization of all controls
		for (PanelPDP15Control panelcontrol : controls)
			for (int dm = 0; dm < PanelPDP15Control.DISPLAYMODES_COUNT; dm++) {
				panelcontrol.visualizations[dm].clear();
			}

		// All coordinates must have been loaded: loadImageCoordinates()

		// Attention: On PDP15 the MSB is labeld "0" and the LSB has the highest
		// number!/ image names contain PDP15 bit codes.

		DimmableLightbulbControlSliceVisualization.defaultAveragingInterval_ms = 200;
		// create all lightbulbs with a low pass of 1/5 sec.
		// this is quite slow, but we need to remove flicker.

		// dimmable lamps use variable transparency, so they do not disturb the
		// background if they are "dim"
		lamp_dch_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_dch_active_on.png",
				lamp_dch_active, lamp_dch_active.control, 0, true));
		lamps_api_states_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_api_states_active_0_on.png", lamps_api_states_active, lamps_api_states_active.control, 0, true));
		lamps_api_states_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_api_states_active_1_on.png", lamps_api_states_active, lamps_api_states_active.control, 1, true));
		lamps_api_states_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_api_states_active_2_on.png", lamps_api_states_active, lamps_api_states_active.control, 2, true));
		lamps_api_states_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_api_states_active_3_on.png", lamps_api_states_active, lamps_api_states_active.control, 3, true));
		lamps_api_states_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_api_states_active_4_on.png", lamps_api_states_active, lamps_api_states_active.control, 4, true));
		lamps_api_states_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_api_states_active_5_on.png", lamps_api_states_active, lamps_api_states_active.control, 5, true));
		lamps_api_states_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_api_states_active_6_on.png", lamps_api_states_active, lamps_api_states_active.control, 6, true));
		lamps_api_states_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_api_states_active_7_on.png", lamps_api_states_active, lamps_api_states_active.control, 7, true));
		lamp_api_enable.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_api_enable_on.png",
				lamp_api_enable, lamp_api_enable.control, 0, true));
		lamp_pi_active.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_pi_active_on.png",
				lamp_pi_active, lamp_pi_active.control, 0, true));
		lamp_pi_enable.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_pi_enable_on.png",
				lamp_pi_enable, lamp_pi_enable.control, 0, true));
		lamp_mode_index.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_mode_index_on.png",
				lamp_mode_index, lamp_mode_index.control, 0, true));
		lamp_major_state_fetch.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_major_states_fetch_on.png", lamp_major_state_fetch, lamp_major_state_fetch.control, 0, true));
		lamp_major_state_inc.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_major_states_inc_on.png", lamp_major_state_inc, lamp_major_state_inc.control, 0, true));
		lamp_major_state_defer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_major_states_defer_on.png", lamp_major_state_defer, lamp_major_state_defer.control, 0, true));
		lamp_major_state_eae.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_major_states_eae_on.png", lamp_major_state_eae, lamp_major_state_eae.control, 0, true));
		lamp_major_state_exec.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_major_states_exec_on.png", lamp_major_state_exec, lamp_major_state_exec.control, 0, true));
		lamps_time_states.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_time_states_1_on.png", lamps_time_states, lamps_time_states.control, 0, true));
		lamps_time_states.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_time_states_2_on.png", lamps_time_states, lamps_time_states.control, 1, true));
		lamps_time_states.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_time_states_3_on.png", lamps_time_states, lamps_time_states.control, 2, true));

		lamp_extd.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_extd_on.png", lamp_extd,
				lamp_extd.control, 0, true));
		lamp_clock.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_clock_on.png",
				lamp_clock, lamp_clock.control, 0, true));
		lamp_error.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_error_on.png",
				lamp_error, lamp_error.control, 0, true));
		lamp_prot.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_prot_on.png", lamp_prot,
				lamp_prot.control, 0, true));
		lamp_link.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_link_on.png", lamp_link,
				lamp_link.control, 0, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_17_on.png",
				lamps_register, lamps_register.control, 0, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_16_on.png",
				lamps_register, lamps_register.control, 1, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_15_on.png",
				lamps_register, lamps_register.control, 2, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_14_on.png",
				lamps_register, lamps_register.control, 3, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_13_on.png",
				lamps_register, lamps_register.control, 4, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_12_on.png",
				lamps_register, lamps_register.control, 5, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_11_on.png",
				lamps_register, lamps_register.control, 6, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_10_on.png",
				lamps_register, lamps_register.control, 7, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_09_on.png",
				lamps_register, lamps_register.control, 8, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_08_on.png",
				lamps_register, lamps_register.control, 9, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_07_on.png",
				lamps_register, lamps_register.control, 10, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_06_on.png",
				lamps_register, lamps_register.control, 11, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_05_on.png",
				lamps_register, lamps_register.control, 12, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_04_on.png",
				lamps_register, lamps_register.control, 13, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_03_on.png",
				lamps_register, lamps_register.control, 14, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_02_on.png",
				lamps_register, lamps_register.control, 15, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_01_on.png",
				lamps_register, lamps_register.control, 16, true));
		lamps_register.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_register_00_on.png",
				lamps_register, lamps_register.control, 17, true));

		lamp_power.visualizationAll().add(new DimmableLightbulbControlSliceVisualization("lamp_power_on.png",
				lamp_power, lamp_power.control, 0, true));
		lamp_run.visualizationAll().add(
				new DimmableLightbulbControlSliceVisualization("lamp_run_on.png", lamp_run, lamp_run.control, 0, true));
		lamps_instruction.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_instruction_3_on.png", lamps_instruction, lamps_instruction.control, 0, true));
		lamps_instruction.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_instruction_2_on.png", lamps_instruction, lamps_instruction.control, 1, true));
		lamps_instruction.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_instruction_1_on.png", lamps_instruction, lamps_instruction.control, 2, true));
		lamps_instruction.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_instruction_0_on.png", lamps_instruction, lamps_instruction.control, 3, true));
		lamp_instruction_defer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_instruction_defer_on.png", lamp_instruction_defer, lamp_instruction_defer.control, 0, true));
		lamp_instruction_index.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_instruction_index_on.png", lamp_instruction_index, lamp_instruction_index.control, 0, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_17_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 0, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_16_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 1, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_15_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 2, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_14_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 3, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_13_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 4, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_12_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 5, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_11_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 6, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_10_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 7, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_09_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 8, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_08_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 9, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_07_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 10, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_06_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 11, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_05_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 12, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_04_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 13, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_03_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 14, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_02_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 15, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_01_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 16, true));
		lamps_memory_buffer.visualizationAll().add(new DimmableLightbulbControlSliceVisualization(
				"lamp_memory_buffer_00_on.png", lamps_memory_buffer, lamps_memory_buffer.control, 17, true));

		// a switch image depends also on the state of the right neighbor
		// (shadow visible)
		TwoStateSituativeControlSliceVisualization tsscsv, neighborTsscsv;
		// build up switches right to left, with neigborCsv shifting
		// normally a switch is in state 0 = OFF in the "to user" orientation
		neighborTsscsv = null;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address17_2user.png", 0, 0); // OFF, no right neighbor
		tsscsv.addStateImageFilename("switch_address17_2panel.png", 1, 0); // ON, no right neighbor
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 1,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address16_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address16_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address16_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address16_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 2,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address15_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address15_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address15_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address15_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 3,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address14_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address14_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address14_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address14_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 4,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address13_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address13_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address13_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address13_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 5,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address12_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address12_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address12_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address12_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 6,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address11_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address11_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address11_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address11_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 7,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address10_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address10_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address10_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address10_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 8,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address09_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address09_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address09_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address09_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 9,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address08_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address08_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address08_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address08_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 10,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address07_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address07_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address07_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address07_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 11,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address06_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address06_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address06_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address06_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 12,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address05_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address05_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address05_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address05_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 13,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address04_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address04_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address04_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address04_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_address, switches_address.control, 14,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_address03_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_address03_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_address03_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_address03_2panel(+2panel).png", 1, 1); // both ON
		switches_address.visualizationAll().add(tsscsv);
		// no switches address 0,1,2 !

		neighborTsscsv = null;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_sing_inst, switch_sing_inst.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_sing_inst_2user.png", 0, 0); // OFF, no right neighbor
		tsscsv.addStateImageFilename("switch_sing_inst_2panel.png", 1, 0); // ON, no right neighbor
		switch_sing_inst.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_sing_step, switch_sing_step.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_sing_step_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_sing_step_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_sing_step_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_sing_step_2panel(+2panel).png", 1, 1); // both ON
		switch_sing_step.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_sing_time, switch_sing_time.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_sing_time_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_sing_time_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_sing_time_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_sing_time_2panel(+2panel).png", 1, 1); // both ON
		switch_sing_time.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_prot, switch_prot.control, 0, neighborTsscsv);
		tsscsv.addStateImageFilename("switch_prot_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_prot_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_prot_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_prot_2panel(+2panel).png", 1, 1); // both ON
		switch_prot.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_rept, switch_rept.control, 0, neighborTsscsv);
		tsscsv.addStateImageFilename("switch_rept_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_rept_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_rept_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_rept_2panel(+2panel).png", 1, 1); // both ON
		switch_rept.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_bank_mode, switch_bank_mode.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_bank_mode_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_bank_mode_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_bank_mode_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_bank_mode_2panel(+2panel).png", 1, 1); // both ON
		switch_bank_mode.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_clock, switch_clock.control, 0, neighborTsscsv);
		tsscsv.addStateImageFilename("switch_clock_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_clock_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_clock_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_clock_2panel(+2panel).png", 1, 1); // both ON
		switch_clock.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_reg_group, switch_reg_group.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_reg_group_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_reg_group_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_reg_group_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_reg_group_2panel(+2panel).png", 1, 1); // both ON
		switch_reg_group.visualizationAll().add(tsscsv);

		// STOP,RESET,READIN are momentary action.
		// GUI allows only one switch to be active, so
		// there is no combination of "switch On and neighbor ON"
		neighborTsscsv = null;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_read_in, switch_read_in.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_read_in_2user.png", 0, 0); // OFF, no right neighbor
		tsscsv.addStateImageFilename("switch_read_in_2panel.png", 1, 0); // ON, no right neighbor
		switch_read_in.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_reset, switch_reset.control, 0, neighborTsscsv);
		tsscsv.addStateImageFilename("switch_reset_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_reset_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_reset_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_reset_2panel(+2panel).png", 1, 1); // both ON (only test mode)
		switch_reset.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_stop, switch_stop.control, 0, neighborTsscsv);
		tsscsv.addStateImageFilename("switch_stop_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_stop_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_stop_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_stop_2panel(+2panel).png", 1, 1); // both ON (only test mode)
		switch_stop.visualizationAll().add(tsscsv);

		neighborTsscsv = null;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data17_2user.png", 0, 0); // OFF, no right neighbor
		tsscsv.addStateImageFilename("switch_data17_2panel.png", 1, 0); // ON, no right neighbor
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 1,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data16_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data16_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data16_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data16_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 2,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data15_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data15_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data15_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data15_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 3,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data14_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data14_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data14_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data14_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 4,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data13_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data13_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data13_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data13_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 5,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data12_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data12_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data12_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data12_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 6,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data11_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data11_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data11_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data11_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 7,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data10_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data10_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data10_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data10_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 8,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data09_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data09_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data09_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data09_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 9,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data08_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data08_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data08_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data08_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 10,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data07_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data07_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data07_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data07_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 11,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data06_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data06_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data06_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data06_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 12,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data05_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data05_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data05_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data05_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 13,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data04_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data04_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data04_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data04_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 14,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data03_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data03_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data03_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data03_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 15,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data02_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data02_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data02_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data02_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 16,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data01_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data01_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data01_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data01_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switches_data, switches_data.control, 17,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_data00_2user(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_data00_2panel(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_data00_2user(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_data00_2panel(+2panel).png", 1, 1); // both ON
		switches_data.visualizationAll().add(tsscsv);

		// EXAMINE THIS/NEXT: inversed polarity, ON = "to user"
		// and they are "momentary action"./ GUI allows only one switch to be
		// active, so
		// there is no combination of "switch On and neighbor ON"
		neighborTsscsv = null;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_examine_next, switch_examine_next.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_examine_next_2panel.png", 0, 0); // OFF, no right neighbor
		tsscsv.addStateImageFilename("switch_examine_next_2user.png", 1, 0); // ON, no right neighbor
		switch_examine_next.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_examine_this, switch_examine_this.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_examine_this_2panel(+2panel).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_examine_this_2user(+2panel).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_examine_this_2panel(+2user).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_examine_this_2user(+2user).png", 1, 1); // both ON (only test mode)
		switch_examine_this.visualizationAll().add(tsscsv);
		// DEPOSIT properties see EXAMINE above
		neighborTsscsv = null;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_deposit_next, switch_deposit_next.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_deposit_next_2panel.png", 0, 0); // OFF, no right neighbor
		tsscsv.addStateImageFilename("switch_deposit_next_2user.png", 1, 0); // ON, no right neighbor
		switch_deposit_next.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_deposit_this, switch_deposit_this.control, 0,
				neighborTsscsv);
		tsscsv.addStateImageFilename("switch_deposit_this_2panel(+2panel).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_deposit_this_2user(+2panel).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_deposit_this_2panel(+2user).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_deposit_this_2user(+2user).png", 1, 1); // both ON (only test mode)
		switch_deposit_this.visualizationAll().add(tsscsv);
		// START, EXEC,CONT momentary action. START& CONT reversed
		neighborTsscsv = null;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_cont, switch_cont.control, 0, neighborTsscsv);
		tsscsv.addStateImageFilename("switch_cont_2panel.png", 0, 0); // OFF, no right neighbor
		tsscsv.addStateImageFilename("switch_cont_2user.png", 1, 0); // ON, no right neighbor
		switch_cont.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_exec, switch_exec.control, 0, neighborTsscsv);
		tsscsv.addStateImageFilename("switch_exec_2user(+2panel).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_exec_2panel(+2panel).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_exec_2user(+2user).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_exec_2panel(+2user).png", 1, 1); // both ON (only test mode)
		switch_exec.visualizationAll().add(tsscsv);
		neighborTsscsv = tsscsv;
		tsscsv = new TwoStateSituativeControlSliceVisualization(switch_start, switch_start.control, 0, neighborTsscsv);
		tsscsv.addStateImageFilename("switch_start_2panel(+2user).png", 0, 0); // both OFF
		tsscsv.addStateImageFilename("switch_start_2user(+2user).png", 1, 0); // ON, neighbor OFF
		tsscsv.addStateImageFilename("switch_start_2panel(+2panel).png", 0, 1); // OFF, neighbor ON
		tsscsv.addStateImageFilename("switch_start_2user(+2panel).png", 1, 1); // both ON (only test mode)
		switch_start.visualizationAll().add(tsscsv);

		MultiStateControlSliceVisualization mscsv;
		// knob images depend on display mode. "Normal = side view"
		mscsv = new MultiStateControlSliceVisualization(knob_repeat_rate, knob_repeat_rate.control);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos00.png", 0);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos01.png", 1);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos02.png", 2);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos03.png", 3);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos04.png", 4);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos05.png", 5);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos06.png", 6);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos07.png", 7);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos08.png", 8);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos09.png", 9);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos10.png", 10);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos11.png", 11);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos12.png", 12);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos13.png", 13);
		mscsv.addStateImageFilename("knob_normal_repeat_rate_pos14.png", 14);
		knob_repeat_rate.visualizationNormal().add(mscsv);

		// in mode DISPLAYMODE_REPEAT_RATE only a big repeat rate knob is
		// visible
		mscsv = new MultiStateControlSliceVisualization(knob_repeat_rate, knob_repeat_rate.control);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos00.png", 0);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos01.png", 1);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos02.png", 2);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos03.png", 3);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos04.png", 4);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos05.png", 5);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos06.png", 6);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos07.png", 7);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos08.png", 8);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos09.png", 9);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos10.png", 10);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos11.png", 11);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos12.png", 12);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos13.png", 13);
		mscsv.addStateImageFilename("knob_frontal_repeat_rate_pos14.png", 14);
		knob_repeat_rate.visualizationRepeatRate().add(mscsv);

		mscsv = new MultiStateControlSliceVisualization(knob_register_select, knob_register_select.control);
		// return values as moving single bit
		mscsv.addStateImageFilename("knob_normal_register_select_ac.png", 11); // 04000
		mscsv.addStateImageFilename("knob_normal_register_select_pc.png", 10); // 02000
		mscsv.addStateImageFilename("knob_normal_register_select_oa.png", 9); // 01000
		mscsv.addStateImageFilename("knob_normal_register_select_mq.png", 8); // 00400
		mscsv.addStateImageFilename("knob_normal_register_select_l_sc.png", 7); // 00200
		mscsv.addStateImageFilename("knob_normal_register_select_xr.png", 6); // 00100
		mscsv.addStateImageFilename("knob_normal_register_select_lr.png", 5); // 00040
		mscsv.addStateImageFilename("knob_normal_register_select_eae.png", 4); // 00020
		mscsv.addStateImageFilename("knob_normal_register_select_dsr.png", 3); // 00010
		mscsv.addStateImageFilename("knob_normal_register_select_iob.png", 2); // 00004
		mscsv.addStateImageFilename("knob_normal_register_select_sta.png", 1); // 00002
		mscsv.addStateImageFilename("knob_normal_register_select_mo.png", 0); // 00001
		knob_register_select.visualizationNormal().add(mscsv);

		// in mode DISPLAYMODE_REGISTER_SELECT only a big "register select" knob
		// is visible
		mscsv = new MultiStateControlSliceVisualization(knob_register_select, knob_register_select.control);
		mscsv.addStateImageFilename("knob_frontal_register_select_ac.png", 11); // 04000
		mscsv.addStateImageFilename("knob_frontal_register_select_pc.png", 10); // 02000
		mscsv.addStateImageFilename("knob_frontal_register_select_oa.png", 9); // 01000
		mscsv.addStateImageFilename("knob_frontal_register_select_mq.png", 8); // 00400
		mscsv.addStateImageFilename("knob_frontal_register_select_l_sc.png", 7); // 00200
		mscsv.addStateImageFilename("knob_frontal_register_select_xr.png", 6); // 00100
		mscsv.addStateImageFilename("knob_frontal_register_select_lr.png", 5); // 00040
		mscsv.addStateImageFilename("knob_frontal_register_select_eae.png", 4); // 00020
		mscsv.addStateImageFilename("knob_frontal_register_select_dsr.png", 3); // 00010
		mscsv.addStateImageFilename("knob_frontal_register_select_iob.png", 2); // 00004
		mscsv.addStateImageFilename("knob_frontal_register_select_sta.png", 1); // 00002
		mscsv.addStateImageFilename("knob_frontal_register_select_mo.png", 0); // 00001
		knob_register_select.visualizationRegisterSelect().add(mscsv);
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
		ControlSliceStateImage.resourceImageFilePathPrefix = "blinkenbone/panelsim/panelsimPDP15/images/";
		ControlSliceStateImage.resourceImageFileNamePrefix = "pdp15_size=" + scaledBackgroundWidth + "_";

		// full file name is something like
		// "pdpPDP15_size=1200_coordinates.csv"
		ControlSliceStateImage.loadImageInfos("coordinates.csv");

		// background: load image
		backgroundGlobalVisualization.createStateImages();
		backgroundGlobalVisualization.setStateExact(1); // always visible

		backgroundKnobsFrontalVisualization.createStateImages();
		backgroundKnobsFrontalVisualization.setStateExact(1);

		/*
		 * all visualisations: loadImages
		 */
		for (PanelPDP15Control panelcontrol : controls)
			for (int dm = 0; dm < PanelPDP15Control.DISPLAYMODES_COUNT; dm++) {
				for (ControlSliceVisualization csv : panelcontrol.visualizations[dm]) {
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
			}
		// adjust JPanel size to background size + frame border around
		// background size = size of single state image
		// if (controlVisualization != null) { ??????????????????
		Dimension size = new Dimension(
				backgroundGlobalVisualization.getVisibleStateImage().scaledStateImage.getWidth() + 2 * borderLeftRight,
				backgroundGlobalVisualization.getVisibleStateImage().scaledStateImage.getHeight()
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
		displayMode = PanelPDP15Control.DISPLAYMODE_NORMAL;

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
	public ControlSliceStateImage getTestStateImage(PanelPDP15Control panelcontrol, ControlSliceVisualization csv,
			int testmode) {
		ControlSliceStateImage resultCssi = null;
		switch (panelcontrol.type) {
		case PDP15_LAMP: // show brightest led/lamp image in all test modes
			resultCssi = csv.getStateImage(csv.maxState);
			break;
		case PDP15_SWITCH: //
		case PDP15_KEY: //
			TwoStateSituativeControlSliceVisualization tsscsv = (TwoStateSituativeControlSliceVisualization) csv;
			switch (testmode) {
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST:
				resultCssi = csv.getStateImage(tsscsv.getState());
				break;
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST:
				// get image of "active" switch with neigbor also "active" 
				if (tsscsv.neighborControlSliceVisualization != null)
					resultCssi = tsscsv.getStateImage(tsscsv.maxState,
							tsscsv.neighborControlSliceVisualization.maxState);
				if (resultCssi == null) // active momentary action keys have only inactive neighbors
					resultCssi = tsscsv.getStateImage(tsscsv.maxState, 0);
				break;
			}
			break;
		case PDP15_KNOB: //
			switch (testmode) {
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST:
				resultCssi = csv.getStateImage(csv.getState()); // no switch change in
				// "lamptest", why
				// is this
				// necessary?
				break;
			case rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST:
				// total test: show as "pressed"
				resultCssi = csv.getStateImage(csv.maxState);
				break;
			}
			break;
		}
		return resultCssi;
	}

	/*
	 * Draw the stack of images onto a Graphics. To be used in a JPanel
	 * "paintComponent(Graphics g)"
	 *
	 * draw order as defined.: background first!
	 *
	 * double buffering?
	 * http://docs.oracle.com/javase/tutorial/extra/fullscreen/doublebuf.html
	 *
	 * Interaction with mouse:
	 * ControlSliceStateImage.clickable is set here
	 */
	public void paintComponent(Graphics g) {

		timeoutDisplayMode(); // fallback to NORMAL after timeout

		boolean incomplete = false;
		Graphics2D g2d = (Graphics2D) g; // for transparency with AlphaComposite

		ControlSliceStateImage cssi;

		// save initial transparency
		java.awt.Composite originalComposite = g2d.getComposite();

		// clear "clickable" on all images
		for (PanelPDP15Control panelcontrol : controls)
			for (int dm = 0; dm < PanelPDP15Control.DISPLAYMODES_COUNT; dm++)
				for (ControlSliceVisualization csv : panelcontrol.visualizations[dm])
					for (ControlSliceStateImage cssi1 : csv.stateImages)
						cssi1.clickable = false;

		// fill panel => frame around background image
		setForeground(borderColor);
		g2d.fillRect(0, 0, getWidth(), getHeight());

		// always draw background, is always visible
		if (backgroundGlobalVisualization != null
				&& (cssi = backgroundGlobalVisualization.getVisibleStateImage()) != null
				&& cssi.scaledStateImage != null) {
			g.drawImage(cssi.scaledStateImage, borderLeftRight, borderTopBottom, null);

		} else
			incomplete = true;
		// System.out.printf("paintComponent(incomplete=%b)%n", incomplete);

		List<ControlSliceStateImage> zOrderedImageList = new ArrayList<ControlSliceStateImage>();

		// incomplete |= (controlVisualization == null); ??????????????
		if (!incomplete) {
			if (blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST
					|| blinkenlightApiPanel.mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST) {
				// Test mode
				for (PanelPDP15Control panelcontrol : controls) {
					for (ControlSliceVisualization csv : panelcontrol.visualizations[PanelPDP15Control.DISPLAYMODE_NORMAL]) {
						// display NORMAL images
						cssi = getTestStateImage(panelcontrol, csv, blinkenlightApiPanel.mode);
						if (cssi != null)
							zOrderedImageList.add(cssi); // nothing is clickable in test mode
					}
					for (ControlSliceVisualization csv : panelcontrol.visualizations[PanelPDP15Control.DISPLAYMODE_ALL]) {
						// display images identical in ALL modes
						cssi = getTestStateImage(panelcontrol, csv, blinkenlightApiPanel.mode);
						if (cssi != null)
							zOrderedImageList.add(cssi); // nothing is clickable in test mode
					}
				}
			} else {
				// No test, regular display
				// display image of "Displaymode" or DISPLAY_ALL visualization
				// !image may not have entries in both visualizations!
				for (PanelPDP15Control panelcontrol : controls) {
					for (ControlSliceVisualization csv : panelcontrol.visualizations[displayMode]) {
						cssi = csv.getStateImage(csv.getState());
						if (cssi != null) {
							zOrderedImageList.add(cssi); // register for paint
							if (csv.blinkenlightApiControl.is_input)
								cssi.clickable = true; // clickable if visible and input (siwtch, knob)
						}
					}
					for (ControlSliceVisualization csv : panelcontrol.visualizations[PanelPDP15Control.DISPLAYMODE_ALL]) {
						cssi = csv.getStateImage(csv.getState());
						if (cssi != null) {
							zOrderedImageList.add(cssi); // register for paint
							if (csv.blinkenlightApiControl.is_input)
								cssi.clickable = true; // clickable if visible and input (siwtch, knob)
						}
					}
				}
				if (displayMode == PanelPDP15Control.DISPLAYMODE_REGISTER_SELECT
						|| displayMode == PanelPDP15Control.DISPLAYMODE_REPEAT_RATE) {
					// special background around frontal knob image
					zOrderedImageList.add(backgroundKnobsFrontalVisualization.getStateImage(1));
				}
			}
		}

		// paint images in reverse Z-order: lower "paintOrders" = background first
		// http://javatricks.de/tricks/arraylist-sortieren
		// sort operator in anonymous inner class from Comparable.
		Collections.sort(zOrderedImageList, new Comparator<ControlSliceStateImage>() {
			@Override
			public int compare(ControlSliceStateImage cssi1, ControlSliceStateImage cssi2) {
				return cssi1.paintOrder - cssi2.paintOrder;
			}
		});

		for (ControlSliceStateImage cssi1 : zOrderedImageList) {
			if (cssi1.alphaComposite != null)
				g2d.setComposite(cssi1.alphaComposite); // transparency
			else
				g2d.setComposite(originalComposite); // no transparency
			// System.out.printf("%d) %s%n", cssi1.paintOrder, cssi1.resourceFilepath) ;
			g2d.drawImage(cssi1.scaledStateImage, cssi1.scaledPosition.x + borderLeftRight,
					cssi1.scaledPosition.y + borderTopBottom, null);
		}

		if (incomplete) {
			g2d.setComposite(originalComposite); // image has no transparency
			// no display, for WindowsBuilder designer
			super.paintComponent(g);
		}
	}

	/* set the display mode and update the timeout-timer
	 *
	 */
	void setDisplayMode(int displayMode) {
		this.displayMode = displayMode;
		if (displayMode != PanelPDP15Control.DISPLAYMODE_NORMAL)
			displayModeUpdateTime_ms = System.currentTimeMillis();
	}

	// update timeout, fall back to NORMAL
	void timeoutDisplayMode() {
		if (displayMode != PanelPDP15Control.DISPLAYMODE_NORMAL) {
			long curMillis = System.currentTimeMillis();
			if (curMillis > (displayModeUpdateTime_ms + DISPLAYMODE_TIMEOUT_MS))
				displayMode = PanelPDP15Control.DISPLAYMODE_NORMAL;
		}
	}

	/*
	 * checks, whether image "cssi" of control slice "csv" was clicked
	 * if true, save event in csv.
	 */
	private boolean stateImageClicked(Point clickpoint, ControlSliceVisualization csv, ControlSliceStateImage cssi) {
		if (cssi.clickable && cssi.scaledRectangle.contains(clickpoint)
		// image transparency at clickpoint must be > 50%
				&& cssi.getPixelAt(clickpoint).getAlpha() > 128) {
			csv.clickedStateImage = cssi;
			if (csv.clickedStateImagePoint == null)
				csv.clickedStateImagePoint = new Point(); // only one needed
			csv.clickedStateImagePoint.x = (int) (clickpoint.x - cssi.scaledRectangle.getCenterX());
			csv.clickedStateImagePoint.y = (int) (clickpoint.y - cssi.scaledRectangle.getCenterY());
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
		//System.out.printf("getControlVisualizationAt(): clickpoint=(%d,%d)%n", clickpoint.x, clickpoint.y);

		// map clickpoint to background image coordinates
		clickpoint.x -= borderLeftRight;
		clickpoint.y -= borderTopBottom;
		for (PanelPDP15Control panelcontrol : controls)
			for (int dm = 0; dm < PanelPDP15Control.DISPLAYMODES_COUNT; dm++)
				for (ControlSliceVisualization csv : panelcontrol.visualizations[dm]) {
					ControlSliceStateImage cssi = csv.getClickableStateImage();
					// getVisibleStateImage() deprecated!
					//System.out.printf("control=%s, displaymode=%d, csv=%s%n", panelcontrol.control.name, dm, csv.name);
					if (cssi != null && stateImageClicked(clickpoint, csv, cssi))
						return csv;
				}
		// No visible state image was clicked
		// but there may be the picture of an "inactive" control
		// be painted onto the background.
		//
		// Check, wether any state image of a ControlSliceVisualization
		// could be under the click point
		for (PanelPDP15Control panelcontrol : controls)
			for (int dm = 0; dm < PanelPDP15Control.DISPLAYMODES_COUNT; dm++)
				for (ControlSliceVisualization csv : panelcontrol.visualizations[dm]) {
					for (ControlSliceStateImage cssi : csv.stateImages) {
						if (cssi.clickable && stateImageClicked(clickpoint, csv, cssi))
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
		PanelPDP15Control panelcontrol = (PanelPDP15Control) csv.panelControl;
		switch (panelcontrol.type) {
		case PDP15_SWITCH:
			// toggle between state 1 and 0 on separate clicks
			if (csv.getState() == 0)
				csv.setStateExact(1);
			else
				csv.setStateExact(0);
			break;
		case PDP15_KEY:
			// activate only while mouse pressed down
			csv.setStateExact(1);
			// if (panelcontrol == switch_LAMPTEST) no lamptest on pdp8
			// setSelftest(rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_LAMPTEST);
			break;
		case PDP15_KNOB:
			// click on knob in NORMAl display mode: siwtch to enlarge display
			if (displayMode == PanelPDP15Control.DISPLAYMODE_NORMAL) {
				int state;
				if (panelcontrol == knob_repeat_rate)
					setDisplayMode(PanelPDP15Control.DISPLAYMODE_REPEAT_RATE);
				else if (panelcontrol == knob_register_select)
					setDisplayMode(PanelPDP15Control.DISPLAYMODE_REGISTER_SELECT);
				// update "frontal" state from "normal" visualization state
				state = knob_repeat_rate.visualizationNormal().get(0).getState();
				knob_repeat_rate.visualizationRepeatRate().get(0).setStateExact(state);
				state = knob_register_select.visualizationNormal().get(0).getState();
				knob_register_select.visualizationRegisterSelect().get(0).setStateExact(state);

			} else {
				int state;
				// frontal view: rotate knob.
				if (panelcontrol == knob_repeat_rate) { // REPEAT_RATE can not rotate over 0
					if (csv.clickedStateImagePoint.x < 0) // LEFT click LEFT in knob Image = decrement
						csv.setStateExact(csv.getNextLowerState());
					else // click RIGHT = increment
						csv.setStateExact(csv.getNextHigherState());
					// update "normal" state from "frontal" visualization state
					state = knob_repeat_rate.visualizationRepeatRate().get(0).getState();
					knob_repeat_rate.visualizationNormal().get(0).setStateExact(state);
				}
				if (panelcontrol == knob_register_select) { // REG SELECT can rotate over 0
					if (csv.clickedStateImagePoint.x < 0) // click LEFT in Knob Image = decrement
						csv.setStateExact(csv.getNextRotateToLowerState());
					else // click RIGHT = increment
						csv.setStateExact(csv.getNextRotateToHigherState());
					// update "normal" state from "frontal" visualization state
					state = knob_register_select.visualizationRegisterSelect().get(0).getState();
					knob_register_select.visualizationNormal().get(0).setStateExact(state);
				}
				setDisplayMode(displayMode); // reset timeout
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
		PanelPDP15Control panelcontrol = (PanelPDP15Control) csv.panelControl;
		switch (panelcontrol.type) {
		case PDP15_KEY:
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
		for (PanelPDP15Control panelcontrol : controls)
			for (int dm = 0; dm < PanelPDP15Control.DISPLAYMODES_COUNT; dm++)
				for (ControlSliceVisualization csv : panelcontrol.visualizations[dm]) {
					csv.setStateExact(0);
				}
		// default knob position can be set over command line
		knob_repeat_rate.visualizationNormal().get(0).setStateExact(knob_repeat_rate.resetState);
		knob_register_select.visualizationNormal().get(0).setStateExact(knob_register_select.resetState);

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

		for (PanelPDP15Control panelcontrol : controls) {
			Control c = panelcontrol.control;
			boolean visDefined = false;
			if (c != null && c.is_input)
				// set value from first visualization, which is not null
				// this is "ALL" for most, and "NORMAL" for knobs
				// (the REPEAT_RATE and REGISTER_SELECT visualizations copy their states to NORMAL)
				for (int dm = 0; !visDefined && dm < PanelPDP15Control.DISPLAYMODES_COUNT; dm++)
					if (panelcontrol.visualizations[dm].size() > 0)
						synchronized (c) {
							visDefined = true;
							// RPC server may not change control values in parallel
							// is a switch or switch bank.compose value of "active"
							// bit images
							c.value = 0; // init value
							// use "normal" visualization for API values. Other vis's must copy to "normal"
							for (ControlSliceVisualization csv : panelcontrol.visualizations[dm]) {
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
		{
			// calc API value for knob states. only one "Slice" => get(0)
			int knob_state;
			knob_state = knob_repeat_rate.visualizationNormal().get(0).getState();
			// power is OFF if repeat_rate is 0
			if (knob_state > 0)
				switch_power.control.value = 1;
			else
				switch_power.control.value = 0;
			// repeat_rate state 0..14 must be returned as 0..255. Readout from physical panel!
			int[] potivals = { 0, 4, 7, 021, 032, 043, 053, 070, 0115, 0140, 0177, 0216, 0273, 0347, 0377 };
			knob_repeat_rate.control.value = potivals[knob_state];

			// register_select is encoded as set bit, not as integer
			knob_state = knob_register_select.visualizationNormal().get(0).getState();
			knob_register_select.control.value = 1 << knob_state;
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
		for (PanelPDP15Control panelcontrol : controls) {
			Control c = panelcontrol.control;
			if (c != null && !c.is_input)
				synchronized (c) {
					// RPC server may not change control values in parallel
					for (int dm = 0; dm < PanelPDP15Control.DISPLAYMODES_COUNT; dm++)
						for (ControlSliceVisualization csv : panelcontrol.visualizations[dm])
							synchronized (csv) {
								// set visibility for all images of this control
								boolean visible = false;
								// all other: Lightbulb image shows a single bit
								// of
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
	 * all lamps go dark, the repeat_rate switch goes to the "off" state.
	 */
	private void setPowerMode(int mode) {
		ControlSliceVisualization repeatrateKnobButtonCsv = knob_repeat_rate.visualizationNormal().get(0);

		// default: do not change power button state
		int newPowerSwitchState = repeatrateKnobButtonCsv.getState();

		if (mode == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_POWERLESS) {
			// all BlinkenLight API output controls to state 0
			// Alternatively, lamps can be painted as OFF in paintComponent()
			for (PanelPDP15Control panelcontrol : controls) {
				Control c = panelcontrol.control;
				if (c != null && !c.is_input)
					c.value = 0; // atomic, not synchronized
			}
			// RepeatRateKnob into 0 position
			newPowerSwitchState = 0;
		} else {
			// if knob was OFF, set to something ON. Else do not "turn"
			if (newPowerSwitchState == 0)
				newPowerSwitchState = 1;
		}
		// power button state: change only if user does not operate it
		if (currentMouseControlSliceVisualization != repeatrateKnobButtonCsv
				&& newPowerSwitchState != repeatrateKnobButtonCsv.getState()) {
			// power button state changed
			repeatrateKnobButtonCsv.setStateExact(newPowerSwitchState);
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
