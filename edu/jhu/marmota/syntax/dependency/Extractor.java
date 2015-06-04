package edu.jhu.marmota.syntax.dependency;

import fig.basic.Option;
import fig.exec.Execution;

import java.io.IOException;

/**
 * Rule Extractor
 *
 * @author shuoyang
 */
public class Extractor implements Runnable {
    @Option(name = "align", required = true, gloss = "word alignment")
	public String align;

	@Option(name = "fr", required = true, gloss = "foreign sentence")
	public String fr;

	@Option(name = "en", required = true, gloss = "english sentence")
	public String en;

	@Option(name = "dep", required = true, gloss = "dependency tree file")
	public String dep;

	@Option(name = "f2e", required = true, gloss = "direct lexical translation table")
	public String f2e;

	@Option(name = "e2f", required = true, gloss = "inverse lexical translation table")
	public String e2f;

	@Option(name = "rule", required = true, gloss = "phrase table")
	public String rule;

	public void run() {
		try {
			Dep2StrRuleExtractor extractor = new Dep2StrRuleExtractor(align,
					fr, en, dep, rule, f2e, e2f);
			extractor.extract();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	static public void main(String[] args) {
		Extractor extractor = new Extractor();
		Execution.run(args, extractor);
	}
}

