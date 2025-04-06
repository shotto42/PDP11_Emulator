/* Panel1170Control.java: A special control for the PDP-11/70 panel.

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

   20-Apr-2016  JH      added POWER/LOCK keyswitch
   20-Sep-2015  JH      created


   A special control for the 1170 panel.
   Special support for 11/70 knobs:
   - optional "feedback" control (LEds for KNOBs indication)
   - nextState() and preState() functions for rotating order, since knob state encodings are
     quite randomized and
	Has an "input" (button") and "output" (lamp) component.
   Either component can be NULL
 */

package blinkenbone.panelsim.panelsim1170;

import java.util.ArrayList;

import blinkenbone.blinkenlight_api.Control;
import blinkenbone.blinkenlight_api.Panel;
import blinkenbone.panelsim.ControlSliceVisualization;
import blinkenbone.panelsim.PanelControl;
import blinkenbone.panelsim.TwoStateControlSliceVisualization;

public class Panel1170Control extends PanelControl {

	// type of a control
	public enum Panel1170ControlType {
		PDP11_SWITCH, // switch with two permanent positions
		PDP11_KEY, // switch with auto return to inactve psoition,
					// "momentary action"
		PDP11_LAMP, // pure LED, binary coded
		PDP11_KNOB, // rotating knob with LED indicating the position
		PDP11_KEYSWITCH // rotating key switch for POWER, LOCK. no roll around
	}

	Panel1170ControlType type; // different behaviour on click actions

	public Control inputcontrol;
	public Control outputcontrol;

	boolean wiredFeedback; // true, if pressed button always has lamp ON

	Panel1170Control(Panel1170ControlType controltype, Control inputcontrol,
			Control outputcontrol, Panel p // Blinkenlight API panel for both
											// controls
	) {

		visualization = new ArrayList<ControlSliceVisualization>();

		this.type = controltype;
		this.inputcontrol = inputcontrol;
		this.outputcontrol = outputcontrol;
		wiredFeedback = false;

		// add controls to Blinkenlight API panel
		if (inputcontrol != null) {
			p.addControl(inputcontrol);
			inputcontrol.parent = this; // link Blinkenlight API control to
										// KI10Control
		}
		if (outputcontrol != null) {
			p.addControl(outputcontrol);
			outputcontrol.parent = this; // link Blinkenlight API control to
											// KI10Control
		}

	}
}
