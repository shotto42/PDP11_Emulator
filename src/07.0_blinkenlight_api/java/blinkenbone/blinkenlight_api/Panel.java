/* Panel.java: Blinkenlight API data struct - single panel as set of controls

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


   04-May-2012  JH      created
*/

package blinkenbone.blinkenlight_api;

import java.util.ArrayList;
import java.util.Observable;

import blinkenbone.blinkenlight_api.Control.ControlType;
import blinkenbone.rpcgen.rpc_blinkenlight_api;

/**
 * a blinkenlight panel is a set of controls
 *
 * @author joerg
 *
 */
public class Panel extends Observable {
	public int index; // index of this record in parent list
	public String name;
	public String info;

	public int default_radix; // default number representation for controls: 8
								// (octal) or 16 (hex)?

	public ArrayList<Control> controls;

	public int controls_inputs_count; // separate count of inputs and
	// outputs.(auxilliary)
	public int controls_outputs_count;
	// sum of bytes for values of all input/output controls
	// needed for compressed transmission of all values over RPC byte stream
	public int controls_inputs_values_bytecount;
	public int controls_outputs_values_bytecount;

	// working mode
	// 0 = normal,
	// 0x01 = "lamp test": LAMP controls, historical accurate
	// 0x02 = "all controls"" : show all inputs as activated.
	// (show flipped switches on panel simulations)
	// also non-lamp outputs (KNOB)
	public int mode;

	public Panel(String name) {
		controls = new ArrayList<Control>();
		this.name = name;
		this.info = "";
		default_radix = 10;
		this.mode = 0;
		clear();
	}

	// observable
	public void setChanged() {
		super.setChanged();
	}

	/*
	 * delete all controls
	 */
	public void clear() {
		controls.clear();
		controls_inputs_count = 0;
		controls_outputs_count = 0;
		controls_inputs_values_bytecount = 0;
		controls_outputs_values_bytecount = 0;
	}

	/*
	 * new control for panel
	 */
	public Control addControl(Control control) {
		controls.add(control);
		control.index = controls.indexOf(control);
		if (control.radix == 0)
			control.radix = default_radix;
		if (control.is_input) {
			controls_inputs_count++;
			controls_inputs_values_bytecount += control.value_bytelen;
		} else {
			controls_outputs_count++;
			controls_outputs_values_bytecount += control.value_bytelen;
		}

		return control;
	}

	/*
	 * search a control over name (case insensitive), and input/output direction
	 */
	public Control getControl(String controlname, boolean is_input) {
		for (Control c : controls) {
			if (c.name.equalsIgnoreCase(controlname) && c.is_input == is_input)
				return c; // found
		}
		return null; // not found
	}

	/*
	 * count, how many controls have value != value->previous
	 */
	public int getControlValueChanges(boolean is_input) {
		int n = 0;

		for (Control c : controls) {
			if (c.is_input == is_input && c.value != c.value_previous)
				n++;
		}
		return n;
	}

	/*
	 * Set a selftest/powerless mode for the panel.
     * mode is one of RPC_PARAM_VALUE_PANEL_MODE_*
	 */
	public void setMode(int mode) {
		// set self test mode for all controls
		this.mode = mode;
	}

}
