/*
 Copyright (C) 2010 by
 * 
 * 	Cam-Tu Nguyen 
 *  ncamtu@ecei.tohoku.ac.jp or ncamtu@gmail.com
 *
 *  Xuan-Hieu Phan  
 *  pxhieu@gmail.com 
 *
 *  College of Technology, Vietnamese University, Hanoi
 * 	Graduate School of Information Sciences, Tohoku University
 *
 * JVnTextPro-v.2.0 is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JVnTextPro-v.2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with  JVnTextPro-v.2.0); if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package jvnsegmenter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jvntextpro.data.ContextGenerator;

// TODO: Auto-generated Javadoc
/**
 * The Class BasicContextGenerator.
 */
public abstract class BasicContextGenerator extends ContextGenerator {

	// common variables
	/** The cpnames. */
	Vector<String> cpnames;
	
	/** The paras. */
	Vector<Vector<Integer>> paras;
	
	// common template reader methods	
	/**
	 * Read feature parameters.
	 *
	 * @param node the node
	 * @return true, if successful
	 */
	protected boolean readFeatureParameters(Element node){
		try{
			NodeList childrent = node.getChildNodes();
			cpnames = new Vector<String>();
			paras = new Vector<Vector<Integer>>();
			
			for (int i = 0; i < childrent.getLength(); i++)
				if (childrent.item(i) instanceof Element) {
					Element child = (Element) childrent.item(i);
					String value = child.getAttribute("value");
					
					//parse the value and get the parameters
					String [] parastr = value.split(":");
					Vector<Integer> para = new Vector<Integer>();
					for (int j = 3; j < parastr.length; ++j){
						para.add(Integer.parseInt(parastr[j]));
					}
					
					cpnames.add(parastr[2]);
					paras.add(para);
				}
			
		}
		catch (Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Read feature nodes.
	 *
	 * @param templateFile the template file
	 * @return the vector
	 */
	public static Vector<Element> readFeatureNodes(String templateFile){
		Vector<Element> feaTypes = new Vector<Element>();
		
		try {
			// Read feature template file........
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			InputStream feaTplStream = new FileInputStream(templateFile);
			Document doc = builder.parse(feaTplStream);
		
			Element root = doc.getDocumentElement();
			NodeList childrent = root.getChildNodes();
			for (int i = 0; i < childrent.getLength(); i++)
				if (childrent.item(i) instanceof Element) {
					Element child = (Element) childrent.item(i);
					feaTypes.add(child);
				}
		}
		catch (Exception e){
			System.out.println("Reading featuretemplate fail " + e.getMessage());
			e.printStackTrace();			
		}
		
		return feaTypes;		
	}
}
