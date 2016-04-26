package org.elasticsearch.plugin.synonym.analysis;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.io.FastStringReader;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.AnalysisSettingsRequired;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.index.analysis.TokenizerFactoryFactory;
import org.elasticsearch.index.settings.IndexSettingsService;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.threadpool.ThreadPool;


@AnalysisSettingsRequired
public class SynonymTokenFilterFactory extends AbstractTokenFilterFactory {
	
	private final String GAP = "#####";
    private SynonymMap synonymMap;
    private final boolean ignoreCase;
    private volatile ScheduledFuture scheduledFuture;
    private final String synonyms_url;
    private final Analyzer analyzer;
    private final boolean expand;
    private final TimeValue interval;
    private int lastMaxId = 0;
    @Inject
    public SynonymTokenFilterFactory(Index index, IndexSettingsService indexSettingsService, Environment env, IndicesAnalysisService indicesAnalysisService, Map<String, TokenizerFactoryFactory> tokenizerFactories,
                                     @Assisted String name, @Assisted Settings settings,ThreadPool threadPool) {
        super(index, indexSettingsService.getSettings(), name, settings);
        
        
        Loggers.getLogger(getClass()).info("SynonymTokenFilterFactory Inject...");
        Reader rulesReader = null;
        if (indexSettingsService.getSettings().get("synonyms_url") != null) {
        	synonyms_url = indexSettingsService.getSettings().get("synonyms_url");
        	String synonymText = "";
        	try {
				synonymText =  HttpUtils.doPost(synonyms_url);
	        	Loggers.getLogger(getClass()).info("synonyms_url = {},text = {}", synonyms_url,synonymText);
			} catch (IOException e) {
				Loggers.getLogger(getClass()).error(e.getMessage(), e);
			}
        	rulesReader = new FastStringReader(synonymText);
        }else {
            throw new IllegalArgumentException("synonym requires synonyms_url to be configured");
        }

        this.ignoreCase = settings.getAsBoolean("ignore_case", true);
        
        expand = settings.getAsBoolean("expand", true);
        
        this.interval = settings.getAsTime("interval", TimeValue.timeValueSeconds(60));

        String tokenizerName = settings.get("tokenizer", "whitespace");

        TokenizerFactoryFactory tokenizerFactoryFactory = tokenizerFactories.get(tokenizerName);
        if (tokenizerFactoryFactory == null) {
            tokenizerFactoryFactory = indicesAnalysisService.tokenizerFactoryFactory(tokenizerName);
        }
        if (tokenizerFactoryFactory == null) {
            throw new IllegalArgumentException("failed to find tokenizer [" + tokenizerName + "] for synonym token filter");
        }

        final TokenizerFactory tokenizerFactory = tokenizerFactoryFactory.create(tokenizerName, Settings.builder().put(indexSettingsService.getSettings()).put(settings).build());

        this.analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer tokenizer = tokenizerFactory == null ? new WhitespaceTokenizer() : tokenizerFactory.create();
                TokenStream stream = ignoreCase ? new LowerCaseFilter(tokenizer) : tokenizer;
                return new TokenStreamComponents(tokenizer, stream);
            }
        };

        try {
            SynonymMap.Builder parser = new SolrSynonymParser(true, expand, analyzer);
            ((SolrSynonymParser) parser).parse(rulesReader);
            
            synonymMap = parser.build();
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to build synonyms", e);
        }
        
        Loggers.getLogger(getClass()).info("init SynonymMonitor interval= {}",interval);
        scheduledFuture = threadPool.scheduleWithFixedDelay(new SynonymMonitor(),interval);
    }

	public TokenStream create(TokenStream tokenStream) {
		return synonymMap.fst == null ? tokenStream : new SynonymFilter(tokenStream, synonymMap, ignoreCase);
	}
	
	
	public class SynonymMonitor implements Runnable {

		public void run() {
        	Loggers.getLogger(getClass()).info("Synonym Synchronize begin...");
			if(synonyms_url != null){
				String synonymText = "";
	        	try {
					synonymText =  HttpUtils.doPost(synonyms_url);
				} catch (IOException e) {
					Loggers.getLogger(getClass()).error(e.getMessage(), e);
				}
	        	int indexOfSize = synonymText.indexOf(GAP);
	        	if(indexOfSize > 0){
	        		int currId = Integer.parseInt(synonymText.substring(0,indexOfSize));
	            	Loggers.getLogger(getClass()).info("Synonym Synchronize lastMaxId = {}",lastMaxId);
	            	if(currId > lastMaxId){
	            		Reader rulesReader = new FastStringReader(synonymText.substring(indexOfSize)+GAP.length());
						SynonymMap.Builder parser = new SolrSynonymParser(true, expand, analyzer);
			            try {
							((SolrSynonymParser) parser).parse(rulesReader);

				            synonymMap = parser.build();
				            lastMaxId = currId;
			            	Loggers.getLogger(getClass()).info("Synonym Synchronize text = {} , lastMaxId = {}",synonymText.substring(indexOfSize)+GAP.length(),lastMaxId);
						} catch (Exception e) {
							Loggers.getLogger(getClass()).error(e.getMessage(), e);
						}
	            	}	            	
	        	}
	        	
	            
			}

        	Loggers.getLogger(getClass()).info("Synonym Synchronize success...");
		}

		
		
	}
    
}
