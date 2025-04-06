/* PanelPDP15Control.java: A special control for the PDP15 panel.

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


   26-Jul-2016  JH      cloned from PDP8/I

 */

package blinkenbone.panelsim.panelsimPDP15;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import blinkenbone.blinkenlight_api.Control;
import blinkenbone.blinkenlight_api.Panel;
import blinkenbone.panelsim.ControlSliceVisualization;
import blinkenbone.panelsim.PanelControl;

public class PanelPDP15Control extends PanelControl {

	// type of a control
	public enum PanelPDP15ControlType {
		PDP15_SWITCH, // switch with two stable positions
		PDP15_KEY, // switch with auto return to inactive psoition, "momentary action"
		PDP15_LAMP, // pure LED, binary coded
		PDP15_KNOB // rotational knob
	}

	/*
	 * 3 ways to show the panel:
	 * The knobs "repeat_rate" and "register_select" are normally visible from
	 * the side, user can not see imprintings.
	 * So additionaly logic is implemented:
	 * First click onto a knob: show the knob enlarged and from above.
	 * more clicks: turn knob left and right, as usual.
	 * After timeout the "normal" display is restored
	 */
	public static final int DISPLAYMODE_ALL = 0; // control is painted the same
													// in all modes
	public static final int DISPLAYMODE_NORMAL = 1; // both knob visible and
														// small
	public static final int DISPLAYMODE_REPEAT_RATE = 2; // knob "repeat_rate"
															// large and frontal
	public static final int DISPLAYMODE_REGISTER_SELECT = 3; // knob "register
																// select"
	public static final int DISPLAYMODES_COUNT	= 4 ;	

	// two dimensional list: mutliple slices for each displaymode 
	public ArrayList<ControlSliceVisualization>[] visualizations = (ArrayList<ControlSliceVisualization>[]) new ArrayList[DISPLAYMODES_COUNT];

	// Make inherited unusable 
	public int visualization = 0;

	PanelPDP15ControlType type; // different behaviour on click actions

	public Control control;

	// override inherited visualizaiton: now multiple visualizations for
	// differnt displaymodes

	PanelPDP15Control(PanelPDP15ControlType controltype, Control control, Panel p) {
		// ONE set of visualizations for each DISPLAY_MODE 
		visualizations[DISPLAYMODE_ALL] = new ArrayList<ControlSliceVisualization>();
		visualizations[DISPLAYMODE_NORMAL] = new ArrayList<ControlSliceVisualization>();
		visualizations[DISPLAYMODE_REPEAT_RATE] = new ArrayList<ControlSliceVisualization>();
		visualizations[DISPLAYMODE_REGISTER_SELECT] = new ArrayList<ControlSliceVisualization>();

		this.type = controltype;
		this.control = control;

		// add controls to Blinkenlight API panel
		if (control != null) {
			p.addControl(control);
			control.parent = this; // link Blinkenlight API control to Control
		}
	}
	
	// fixed getters, for easy build up of database
	public ArrayList<ControlSliceVisualization> visualizationAll() {
		return visualizations[DISPLAYMODE_ALL];
	}
	public ArrayList<ControlSliceVisualization> visualizationNormal() {
		return visualizations[DISPLAYMODE_NORMAL];
	}
	public ArrayList<ControlSliceVisualization> visualizationRepeatRate() {
		return visualizations[DISPLAYMODE_REPEAT_RATE];
		
	}
	public ArrayList<ControlSliceVisualization> visualizationRegisterSelect() {
		return visualizations[DISPLAYMODE_REGISTER_SELECT];
		
	}


}
