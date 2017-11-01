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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Vector;
import jvntextpro.JVnTextPro;

// TODO: Auto-generated Javadoc
/**
 * The Class Session.
 */
public class Session extends Thread {
	
	//------------------------
	// Data
	//------------------------
	/** The textpro. */
	JVnTextPro textpro;
	
	/** The incoming. */
	private Socket incoming;
	
	//-----------------------
	// Methods
	//-----------------------
	/**
	 * Instantiates a new session.
	 *
	 * @param textpro the textpro
	 */
	public Session(JVnTextPro textpro){		
		this.textpro = textpro;
	}
		 	    
	/**
	 * Sets the socket.
	 *
	 * @param s the new socket
	 */
	public synchronized void setSocket(Socket s){
		this.incoming = s;
		notify();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public synchronized void run(){	
		while (true){
			try {
				if (incoming == null) {
		            wait();	            
		        }
				
				System.out.println("Socket opening ...");
				BufferedReader in = new BufferedReader(new InputStreamReader(
						incoming.getInputStream(), "UTF-8"));				
				//PrintStream out = (PrintStream) incoming.getOutputStream();
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						incoming.getOutputStream(), "UTF-8"));
				
				String content = "";
				
				while (true){				
					int ch = in.read();
					if (ch == 0) //end of string
						break;
					
					content += (char) ch;
				}
				
				//System.out.println(content);
				String tagged = textpro.process(content);
				//Thread.sleep(4000);
				
				out.write(tagged.trim());
				out.write((char)0);
				out.flush();
			}
			catch (InterruptedIOException e){
				System.out.println("The conection is interrupted");	
			}
			catch (Exception e){
				System.out.println(e);
				e.printStackTrace();
			}
			
			//update pool
			//go back in wait queue if there is fewer than max
			this.setSocket(null);
			Vector<Session> pool = TaggingService.pool;
			synchronized (pool) {
				if (pool.size() >= TaggingService.maxNSession){
					/* too many threads, exit this one*/
					return;
				}
				else {				
					pool.addElement(this);
				}
			}
		}
	}
}
