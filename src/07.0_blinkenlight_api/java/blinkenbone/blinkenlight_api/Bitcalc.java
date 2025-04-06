/* Bitcalc.java: bit arithmetic utilities

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


   01-May-2012  JH      created
*/

package blinkenbone.blinkenlight_api ;

public class Bitcalc {

	/*
	 * byte is signed in Java ! still no unsigned types in 1.6
	 * convert (byte)  0..255 => (int) 0..255
	 */
	  public static int unsignedByteToInt(byte b) {
		    return (int) b & 0xFF;
		    }

	/*
	 * write the bytes, which form a 64 bit integer, to the byte array "buffer"
	 * lsb first, only the "bytecount" lowest bytes.
	 *
	 * Example: value= 0x123456789a, bytecount = 6 =>
	 *
	 * buffer [0] = 9a, buffer[1] = 78, 56, 34, 12, buffer[5] = 00
	 */
	public static void encode_long_to_bytes(byte buffer[], long value,
			int start, int bytecount) {
		assert (bytecount < 9);
		// loop unrolled
		if (bytecount > 0)
			buffer[start + 0] = (byte) (value & 0xff);
		if (bytecount > 1)
			buffer[start + 1] = (byte) ((value >> 8) & 0xff);
		if (bytecount > 2)
			buffer[start + 2] = (byte) ((value >> 16) & 0xff);
		if (bytecount > 3)
			buffer[start + 3] = (byte) ((value >> 24) & 0xff);
		if (bytecount > 4)
			buffer[start + 4] = (byte) ((value >> 32) & 0xff);
		if (bytecount > 5)
			buffer[start + 5] = (byte) ((value >> 36) & 0xff);
		if (bytecount > 6)
			buffer[start + 6] = (byte) ((value >> 40) & 0xff);
		if (bytecount > 7)
			buffer[start + 7] = (byte) ((value >> 44) & 0xff);
	}

	/*
	 * build a 64 bit integer from the byte array "buffer" lsb first, only the
	 * "bytecount" lowest bytes
	 *
	 * Example: buffer[0] = 9a, buffer[1] = 78, 56, 34, Buffer[4] = 12, buffer[5]
	 * = 00, bytecount = 4 => result = 0x3456789a
	 *
	 * !!! byte is signed in java: -128 ..127
	 */
	public static long decode_long_from_bytes(byte buffer[], int start,
			int bytecount) {
		long value = 0;
		assert (bytecount < 9);
		// loop unrolled
		if (bytecount > 0)
			value |= (long) unsignedByteToInt(buffer[start + 0]);
		if (bytecount > 1)
			value |= (long) unsignedByteToInt(buffer[start + 1]) << 8;
		if (bytecount > 2)
			value |= (long) unsignedByteToInt(buffer[start + 2]) << 16;
		if (bytecount > 3)
			value |= (long) unsignedByteToInt(buffer[start + 3]) << 24;
		if (bytecount > 4)
			value |= (long) unsignedByteToInt(buffer[start + 4]) << 32;
		if (bytecount > 5)
			value |= (long) unsignedByteToInt(buffer[start + 5]) << 36;
		if (bytecount > 6)
			value |= (long) unsignedByteToInt(buffer[start + 6]) << 40;
		if (bytecount > 7)
			value |= (long) unsignedByteToInt(buffer[start + 7]) << 44;
		return value;
	}
}
