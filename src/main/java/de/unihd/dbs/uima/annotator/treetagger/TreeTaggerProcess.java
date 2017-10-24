package de.unihd.dbs.uima.annotator.treetagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class TreeTaggerProcess {
	private final BufferedReader stdout;
	private final BufferedWriter stdin;
	private final Process proc;
	
	public TreeTaggerProcess(Process ttProc) {
		this.stdout = new BufferedReader(new InputStreamReader(ttProc.getInputStream()));
		this.stdin = new BufferedWriter(new OutputStreamWriter(ttProc.getOutputStream()));
		this.proc = ttProc;
	}
	
	public void close() {
		try {
			if(stdout != null) {
				stdout.close();
			}
			if(stdin != null) {
				stdin.close();
			}
			if(proc != null) {
				proc.destroy();
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
