/* Control.java: Blinkenlight API data struct - control element of a panel

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

/**
 * @author joerg
 *
 */

public class Control {

	// type of a control
	public enum ControlType {
		none(0, "NONE", false), /* value 1 */
		input_switch(1, "SWITCH", true), /* value 1 */
		output_lamp(2, "LAMP", false), /* value 2 */
		input_knob(3, "KNOB", true), /* value 3 */
		output_pointer_instrument(4, "POINTER", false), // POINTER
		input_other(5, "INPUT", true), /* value 5 */
		output_other(6, "OUTPUT", false) /* value 6 */
		;
		public int value;
		public String text;
		public boolean is_input;

		ControlType(int value, String text, boolean is_input) {
			this.value = value;
			this.text = text;
			this.is_input = is_input;
		}
	}

	public int index; // index of this record in control list of parent panel
	public String name;
	public boolean is_input; // 0 = out, 1 = in
	public ControlType type;
	public long value; // 64bit: for instance for the LED row of a PDP-10
						// register
	// (36 bit)
	public long value_previous; // "old" value before change
	public long value_default; // startup value
	public int radix; // number representation: 8 (octal) or 16 (hex)?
	public int value_bitlen; // relevant lsb's in value
	public int value_bytelen; // len of value in bytes ... for RPC transmissions

	//@public int mode; // 0 = normal, 1 = selftest (lamp test)


	/* not published by Blinkenligth APiControl */
	public Object	parent ; 	// back link to container

	/*
	 * constructor
	 */
	public Control(String name, ControlType type, int value_bitlen) {
		radix = 0; // set by panel to default
		this.name = name;
		this.is_input = true;
		this.type = type;
		this.is_input = type.is_input;
		this.value_bitlen = value_bitlen;
		// round bitlen up to bytes
		this.value_bytelen = (value_bitlen + 7) / 8; // 0-> 0, 1->1 8->1, 9->2,
														// ...
		value = 0;
		value_previous = value;
	}

}
