
/* CSVParser.java: A CSV file parser

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


 20-Oct-2014  JH      created


 Usage: see test()

 Read more : http://www.ehow.com/how_6796642_read-csv-file-java.html
 */

package blinkenbone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class CSVParser {

	public static TableModel parse(File f, String delimiter) throws FileNotFoundException {

		ArrayList<String> headers = new ArrayList<String>();
		ArrayList<String> oneDdata = new ArrayList<String>();

		// Get the headers of the table.
		Scanner lineScan = new Scanner(f);
		Scanner s = new Scanner(lineScan.nextLine());
		s.useDelimiter(delimiter);
		while (s.hasNext()) {
			headers.add(s.next());
		}
		// Go through each line of the table and add each cell to the ArrayList
		while (lineScan.hasNextLine()) {
			String nextline = lineScan.nextLine();
			s = new Scanner(nextline);
			s.useDelimiter(delimiter);
			while (s.hasNext()) {
				oneDdata.add(s.next());
			}
		}

		int colCount = headers.size();
		int rowCount = oneDdata.size() / headers.size();
		// first index row, 2nd index column
		String[][] data = new String[rowCount][colCount];

		// Move the data into a vanilla array so it can be put in a table.
		for (int y = 0; y < rowCount; y++)
			for (int x = 0; x < colCount; x++) {
				data[y][x] = oneDdata.remove(0);
			}

		/*
		 * for (int x = 0; x < colCount; x++) {
		 * for (int y = 0; y < data[0].length; y++) {
		 * data[x][y] = oneDdata.remove(0);
		 * }
		 * }
		 */
		// Create a table and return it.
		return new DefaultTableModel(data, headers.toArray());
	}

	public static TableModel parse(InputStream is, String delimiter) {

		ArrayList<String> headers = new ArrayList<String>();
		ArrayList<String> oneDdata = new ArrayList<String>();

		// Get the headers of the table.
		Scanner lineScan = new Scanner(is);
		Scanner s = new Scanner(lineScan.nextLine());
		s.useDelimiter(delimiter);
		while (s.hasNext()) {
			headers.add(s.next());
		}
		// Go through each line of the table and add each cell to the ArrayList
		while (lineScan.hasNextLine()) {
			String nextline = lineScan.nextLine();
			s = new Scanner(nextline);
			s.useDelimiter(delimiter);
			while (s.hasNext()) {
				oneDdata.add(s.next());
			}
		}

		int colCount = headers.size();
		int rowCount = oneDdata.size() / headers.size();
		// first index row, 2nd index column
		String[][] data = new String[rowCount][colCount];

		// Move the data into a vanilla array so it can be put in a table.
		for (int y = 0; y < rowCount; y++)
			for (int x = 0; x < colCount; x++) {
				data[y][x] = oneDdata.remove(0);
			}

			s.close(); 
			lineScan.close();
		// Create a table and return it.
		return new DefaultTableModel(data, headers.toArray());
	}

	// parse "filename" and write result to System.out
	public static void test(URI uri, String delimiter) throws FileNotFoundException {
		TableModel t = parse(new File(uri), delimiter);
		// Print all the columns of the table, followed by a new line.
		for (int c = 0; c < t.getColumnCount(); c++) {
			System.out.print(t.getColumnName(c) + delimiter);
		}
		System.out.println();
		// Print all the data from the table.
		for (int r = 0; r < t.getRowCount(); r++) {
			for (int c = 0; c < t.getColumnCount(); c++) {
				System.out.print(t.getValueAt(r, c) + delimiter);
			}
			System.out.println();
		}
	}

}
