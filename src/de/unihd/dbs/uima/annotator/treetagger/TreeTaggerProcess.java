package de.unihd.dbs.uima.annotator.treetagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class TreeTaggerProcess {
	private final BufferedReader stdout;
	private final BufferedWriter stdin;
	
	public TreeTaggerProcess(Process ttProc) {
		this.stdout = new BufferedReader(new InputStreamReader(ttProc.getInputStream()));
		this.stdin = new BufferedWriter(new OutputStreamWriter(ttProc.getOutputStream()));
	}
	
	public void close() {
		try {
			if(stdout != null) {
				stdout.close();
			}
			if(stdin != null) {
				stdin.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final BufferedReader getStdout() {
		return stdout;
	}

	public final BufferedWriter getStdin() {
		return stdin;
	}
}
