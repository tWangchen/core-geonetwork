package org.fao.geonet.kernel;

import java.io.IOException;

import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.exceptions.BatchEditException;
import org.fao.geonet.kernel.search.IndexAndTaxonomy;
import org.fao.geonet.kernel.search.LuceneConfig;
import org.fao.geonet.kernel.search.LuceneQueryBuilder;
import org.fao.geonet.kernel.search.LuceneQueryInput;
import org.fao.geonet.kernel.search.SearchManager;
import org.fao.geonet.kernel.search.index.LuceneIndexLanguageTracker;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class ECatOperationManager {

	@Autowired
	ApplicationContext context;
	
	private LuceneIndexLanguageTracker tracker;
	private IndexAndTaxonomy indexAndTaxonomy;
	private IndexSearcher searcher;
	private LuceneConfig luceneConfig;

	public void init() {
	
		tracker = context.getBean(LuceneIndexLanguageTracker.class);
		try {
			indexAndTaxonomy = tracker.acquire("eng", -1);
		} catch (IOException e) {
			Log.error(Geonet.SEARCH_ENGINE, "Unable to index and taxonomy, "+e.getMessage());
		}
		searcher = new IndexSearcher(indexAndTaxonomy.indexReader);
		luceneConfig = context.getBean(LuceneConfig.class);
		
	}
	
	/**
	 * Search metadata using lucene index
	 * @param context
	 * @param srvContext
	 * @param request
	 * @return
	 * @throws JDOMException 
	 * @throws IOException 
	 * @throws BatchEditException
	 */
	public Metadata getMetadataFromECatId(ApplicationContext context, String eCatId) throws Exception  {
		
		init();
		Element element = Xml
				.loadString("<request><isAdmin>true</isAdmin><_isTemplate>n</_isTemplate><eCatId>"
						+ eCatId + "</eCatId><fast>index</fast></request>", false);
		
		LuceneQueryInput luceneQueryInput = new LuceneQueryInput(element);
		
		Query _query = new LuceneQueryBuilder(luceneConfig, luceneConfig.getTokenizedField(),
				SearchManager.getAnalyzer(Geonet.DEFAULT_LANGUAGE, true), Geonet.DEFAULT_LANGUAGE)
						.build(luceneQueryInput);

		MetadataRepository mdRepo = context.getBean(MetadataRepository.class);

		try {
			
			TopDocs tdocs = searcher.search(_query, 1);
			DocumentStoredFieldVisitor docVisitor = new DocumentStoredFieldVisitor("_uuid");

			indexAndTaxonomy.indexReader.document(tdocs.scoreDocs[0].doc, docVisitor);
			org.apache.lucene.document.Document doc = docVisitor.getDocument();

			String uuid = doc.get("_uuid");
			Log.debug(Geonet.SEARCH_ENGINE, "getMetadataByLuceneSearch --> uuid: " + uuid);

			if (uuid != null) {
				Metadata md = mdRepo.findOneByUuid(uuid);
				return md;
			}

		} catch (Exception e) {
			throw new Exception("failed to get this record using lucene search");
		}

		return null;
	}
}
