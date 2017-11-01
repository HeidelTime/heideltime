package de.unihd.dbs.uima.annotator.treetagger;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.List;

public class TreeTaggerWriter implements Runnable {
	private List<String> tokens;
	private PrintWriter writer;
	
	public TreeTaggerWriter(List<String> tokens, BufferedWriter writer) {
		this.tokens = tokens;
		this.writer = new PrintWriter(writer);
	}

	@Override
	public void run() {
		try {
			// signal to the reader that this is the beginning of the document
			writer.println(TreeTaggerProperties.STARTOFTEXT);
			writer.flush();
			
			// send the tokens one by one as tokenized before
			for(String token : tokens) {
				writer.println(token);
				writer.flush();
			}
			
			// signal to the reader that this is the end of the document
			writer.println(TreeTaggerProperties.ENDOFTEXT);
			writer.flush();
			
			// perform a reset for treetagger's model
			writer.println(TreeTaggerProperties.FLUSH_SEQUENCE);
			writer.flush();
		} catch(Exception e) {
			// ignore as we can't really do anything about it anyway
			e.printStackTrace();
		}
	}

}
