/* PanelKI10Control.java: A special control for the PDP-10 KI10 panel.

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


   15-Oct-2014  JH      created


   A special control, with an "input" (button")
   and "output" (lamp) component.
   Either component can be NULL
 */


package blinkenbone.panelsim.panelsimKI10;

import java.util.ArrayList;

import blinkenbone.blinkenlight_api.Control;
import blinkenbone.blinkenlight_api.Panel;
import blinkenbone.panelsim.ControlSliceVisualization;
import blinkenbone.panelsim.PanelControl;
import blinkenbone.panelsim.TwoStateControlSliceVisualization;



public class PanelKI10Control extends PanelControl{


	// type of a control
	public enum KI10ControlType {
		PDP10_KEY, // lamp button: momentary action
		PDP10_SWITCH, // lamp button: two state toggle on touch
		PDP10_LAMP, // pure lamp
		PDP10_KNOB, // rotating knob
		PDP10_INPUT, // other input
		PDP10_OUTPUT // other output
	}
	KI10ControlType type; // different behaviour on click actions

	public Control inputcontrol ;
	public Control outputcontrol ;

	boolean wiredFeedback ; // true, if pressed button always has lamp ON


	// if any control slice is repainted, this background must be paitned first
	// The background may be shared with other PanelKI10Control, so other
	// controls may need repaint too ... even if unchanged.
	TwoStateControlSliceVisualization localBackground ;

        boolean needRepaint ; // flag for background paint logic


	PanelKI10Control(KI10ControlType controltype, Control inputcontrol, Control outputcontrol,
			Panel p // Blinkenlight API panel for both controls
			)
	{

		visualization = new ArrayList<ControlSliceVisualization>();

		this.type = controltype ;
		this.inputcontrol = inputcontrol ;
		this.outputcontrol = outputcontrol ;
		wiredFeedback = false ;

		// add controls to Blinkenlight API panel
		if (inputcontrol != null) {
			p.addControl(inputcontrol);
			inputcontrol.parent = this ; // link Blinkenlight API control to KI10Control
		}
		if (outputcontrol != null) {
			p.addControl(outputcontrol);
			outputcontrol.parent = this ; // link Blinkenlight API control to KI10Control
		}

	}
}
