package edu.jhu.marmota.syntax.dependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DepTreeReader {
	
	BufferedReader treeReader;
	
	public DepTreeReader(String tree) throws FileNotFoundException {
		this.treeReader = new BufferedReader(new FileReader(new File(tree)));
	}
	
	public DepTree read() throws IOException {
		List<String> constr = new ArrayList<String>();
		List<String> depstr = new ArrayList<String>();
		String currentString = treeReader.readLine();
		// reaching EOF
		if (currentString == null) {
			return null;
		}
		
		while (!currentString.trim().equals("")) {
			constr.add(currentString);
			currentString = treeReader.readLine();
		}
		currentString = treeReader.readLine();
		while (!currentString.trim().equals("")) {
			depstr.add(currentString);
			currentString = treeReader.readLine();
		}
		return DepTree.StanfordDepTreeBuilder(constr.toArray(new String[constr.size()]),
				depstr.toArray(new String[depstr.size()]));
	}

	public void close() throws IOException {
		treeReader.close();
	}
}

