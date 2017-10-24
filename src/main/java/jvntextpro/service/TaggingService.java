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

package jvntextpro.service;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import jvntextpro.JVnTextPro;

// TODO: Auto-generated Javadoc
/**
 * The Class TaggingService.
 */
public class TaggingService extends Thread {
	
	//---------------------------
	// Data
	//---------------------------
	/** The port at which the tagging service is listening. */
	private int port = 2929;
	
	/** The server socket. */
	private ServerSocket socket;
	
	/** The Constant maxNSession. */
	public static final int maxNSession = 5;
	
	/** The pool of TaggingService threads. */
	public static Vector<Session> pool;
	
	/** The vn text pro. */
	private JVnTextPro vnTextPro = null;
	
	/** The option. */
	private ServiceOption option;
	
	//---------------------------
	// Constructor
	//---------------------------
	/**
	 * Instantiates a new tagging service.
	 *
	 * @param p the port
	 * @param option the service option (specifying which tools to be used for text processing)
	 */
	public TaggingService(int p, ServiceOption option){
		this.port = p;
		this.option = option;		
		
	}
	
	/**
	 * Instantiates a new tagging service.
	 *
	 * @param option the service option
	 */
	public TaggingService(ServiceOption option){
		this.option = option;
	}
	
	/**
	 * Inits the.
	 */
	private void init(){
		try {
			vnTextPro = new JVnTextPro();
			
			if (option.doSenToken)
				vnTextPro.initSenTokenization();		
			
			if (option.doSenSeg)
				vnTextPro.initSenSegmenter(option.modelDir+ File.separator + "jvnsensegmenter");
			
			if (option.doWordSeg)
				vnTextPro.initSegmenter(option.modelDir + File.separator + "jvnsegmenter");
			
			if (option.doPosTagging)
				vnTextPro.initPosTagger(option.modelDir + File.separator + "jvnpostag" + File.separator + "maxent");
			
			/* start session threads*/
			pool = new Vector<Session>();
			for (int i = 0; i < maxNSession; ++i){
				Session w = new Session(vnTextPro);
				w.start(); //start a pool of session threads at start-up time rather than on demand for efficiency 
				pool.add(w);
			}
		}
		catch (Exception e){
			System.out.println("Error while initilizing service:" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	//------------------------
	// main methods
	//------------------------
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run(){
		System.out.println("Starting tagging service!");
		try {
			this.socket = new ServerSocket(this.port);
		}
		catch (IOException ioe){
			System.out.println(ioe);
			System.exit(1);
		}
		
		init();
		System.out.println("Tagging service is started successfully");
		while (true){
			Socket incoming = null;
			try{
				incoming = this.socket.accept();
				Session w = null;
				synchronized (pool) {
					if (pool.isEmpty()){
						w = new Session(vnTextPro);
						w.setSocket(incoming); //additional sessions
						w.start();						
					} else{
						w = pool.elementAt(0);
						pool.removeElementAt(0);						
						w.setSocket(incoming);
					}
				}
			}
			catch (IOException e){
				System.out.println(e);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String [] args){
		ServiceOption option = new ServiceOption();
		CmdLineParser parser = new CmdLineParser(option);
		
		if (args.length == 0) {
			System.out.println("TaggingService [options...] [arguments..]");
			parser.printUsage(System.out);
			return;
		}
		new TaggingService(option).run();
	}
}

class ServiceOption{
	@Option(name="-modeldir", usage="Specify model directory, which is the folder containing model directories of subproblem tools (Word Segmentation, POS Tag)")
	String modelDir;
	
	@Option(name="-senseg", usage="Specify if doing sentence segmentation is set or not, not set by default")
	boolean doSenSeg = false;
	
	@Option(name="-wordseg", usage = "Specify if doing word segmentation is set or not, not set by default")
	boolean doWordSeg = false;
	
	@Option(name="-sentoken", usage = "Specify if doing sentence tokenization is set or not, not set by default")
	boolean doSenToken = false;
	
	@Option(name="-postag", usage = "Specify if doing pos tagging or not is set or not, not set by default")
	boolean doPosTagging = false;
}
