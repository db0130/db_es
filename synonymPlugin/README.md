
The file heat synonym plugin adds a synonym token filter that reloads the synonym file at given intervals (default 60s).

Example:

	{
	    "index" : {
	        "analysis" : {
	            "analyzer" : {
	                "synonym" : {
	                    "tokenizer" : "whitespace",
	                    "filter" : ["synonym"]
 	               }
	            },
	            "filter" : {
	                "synonym" : {
	                    "type" : "synonymUrl",
	                    "synonyms_path" : "analysis/synonym.txt(Ö§³Öhttp://localhost/synonym.txt)"
	                    "interval" : "10"
	                }
	            }
	        }
	    }
	}
