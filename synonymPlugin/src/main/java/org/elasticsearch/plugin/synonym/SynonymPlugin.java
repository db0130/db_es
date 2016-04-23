package org.elasticsearch.plugin.synonym;

import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.plugin.synonym.analysis.SynonymTokenFilterFactory;
import org.elasticsearch.plugins.Plugin;

public class SynonymPlugin  extends Plugin {

	@Override
	public String name() {
		return "synonym";
	}

	@Override
	public String description() {
		return "synonym-plugin";
	}
	
	public void onModule(AnalysisModule module) {
		module.addTokenFilter("synonymUrl", SynonymTokenFilterFactory.class);	
	}
}
