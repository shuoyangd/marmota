package edu.jhu.marmota.alignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fig.basic.Pair;

public class AlignmentReader {
	
	private BufferedReader alignReader;
	private BufferedReader foreignReader;
	private BufferedReader englishReader;
	private int lineCounter = 0;
	
	public AlignmentReader(String align, String foreign, String english) throws FileNotFoundException {
		alignReader = new BufferedReader(new FileReader(new File(align)));
		foreignReader = new BufferedReader(new FileReader(new File(foreign)));
		englishReader = new BufferedReader(new FileReader(new File(english)));
	}
	
	public WordAlignedSentencePair read() throws IOException {
		String align = alignReader.readLine();
		String fsent = foreignReader.readLine();
		String esent = englishReader.readLine();
		lineCounter += 1;
		// reaching EOF
		if (align == null && fsent == null && esent == null) {
			return null;
		}
		else if (align == null || fsent == null || esent == null) {
			throw new IllegalStateException("line " + String.valueOf(lineCounter) + ": different length between " +
					"alignment, foreign sentence and english sentence file");
		}

		String[] aligntokens = align.split(" ");
		List<Pair<Integer, Integer>> alignments = new ArrayList<Pair<Integer, Integer>>();
		for (String aligntoken: aligntokens) {
			int findex = Integer.parseInt(aligntoken.split("-")[0]);
			int eindex = Integer.parseInt(aligntoken.split("-")[1]);
			alignments.add(new Pair<Integer, Integer>(findex, eindex));
		}
		return new WordAlignedSentencePair(fsent, esent, alignments);
	}

	public void close() throws IOException {
		alignReader.close();
		foreignReader.close();
		englishReader.close();
	}
}

