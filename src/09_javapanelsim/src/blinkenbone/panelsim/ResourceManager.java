/*
 * ResourceManager.java:  Access to stuff in package "resources"
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * 
 * All filenames must be relative to project root,
 * so starting with "blinkenbone/panelsim/..."
 * and NO LEADING "/" !
 * 
 * 
 * http://docs.oracle.com/javase/1.3/docs/guide/resources/resources.html
 */

package blinkenbone.panelsim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.image.BufferedImage;

/*
 *
 */
public class ResourceManager {
	// pointed to by CLASSPATH, or subd dir in JAR
	// NO leading "/", see below
	// public static final String imageFileDir =
	// "blinkenbone/panelsim/resources/images/";

	private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());

	private final Class demoClass;

	// Resource bundle for internationalized and accessible text
	private ResourceBundle bundle = null;

	public ResourceManager(Class demoClass) {
		this.demoClass = demoClass;

		// bundel not found
		String bundleName = "blinkenbone.panelsim." + demoClass.getSimpleName();

		// if <bundle>...app.properties not found: copied to
		// bin\blinkenbone\panelsim\ ???
		try {
			bundle = ResourceBundle.getBundle(bundleName);
		} catch (MissingResourceException e) {
			logger.log(Level.SEVERE, "Couldn't load bundle: " + bundleName);
		}
	}

	public String getString(String key) {
		return bundle != null ? bundle.getString(key) : key;
	}

	public char getMnemonic(String key) {
		return (getString(key)).charAt(0);
	}

	public ImageIcon createImageIcon(String resourceFilePath, String description) {
		// String path = imageFileDir + "/" + resourceFilePath;

		URL imageURL = demoClass.getResource(resourceFilePath);

		if (imageURL == null) {
			logger.log(Level.SEVERE, "unable to access image file: " + resourceFilePath);

			return null;
		} else {
			return new ImageIcon(imageURL, description);
		}
	}

	/*
	 * resourceFilePath must be something like
	 * "blinkenbone/panelsim/sim1140/images/mypicture.png"
	 */
	public BufferedImage createBufferedImage(String resourceFilePath) {
		// System.out.printf("createBufferedImage(filename=%s):", filename) ;
		// String filepath = imageFileDir + resourceFilePath;
		// System.out.printf("  filepath=%s", filepath) ;

		/*
		 * Important:
		 * + Path "imageFileDir" without leading "/",
		 * + AND getContextClassLoader()
		 * 
		 * else image is only found under Eclipse, not at runtime from jar
		 */

		// URL imageURL = demoClass.getResource(filepath);
		// System.out.printf(" imageURL=createBufferedImage(filename=%s):",
		// filename) ;

		// logger.log(Level.INFO, "class path is: " +
		// System.getProperty("java.class.path")) ;

		URL imageURL = Thread.currentThread().getContextClassLoader()
				.getResource(resourceFilePath);
		if (imageURL == null) {

			logger.log(Level.SEVERE, "unable to access image file: " + resourceFilePath);
			logger.log(Level.SEVERE, "class path is: " + System.getProperty("java.class.path"));

			return null;
		} else {
			// load scaled image from file
			try {
				return ImageIO.read(imageURL);
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			return null; // not reached
		}

	}

	public URI getAbsolutePath(String resourceFilePath) {
		try {
			URL url;
			url = Thread.currentThread().getContextClassLoader().getResource(resourceFilePath);
			return url.toURI();
		} catch (URISyntaxException ex) {
			ex.printStackTrace();
			return null;
		}
	}

}
