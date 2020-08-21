package org.fao.geonet.kernel;

import java.io.IOException;

import org.apache.lucene.document.Document;
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
	private LuceneConfig luceneConfig;
	
	
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
	
		Element element = Xml
				.loadString("<request><isAdmin>true</isAdmin><_isTemplate>n</_isTemplate><eCatId>"
						+ eCatId + "</eCatId><fast>index</fast></request>", false);
		
		MetadataRepository mdRepo = context.getBean(MetadataRepository.class);

		try {
			
			Document doc = getLuceneTopDocs(element, Geonet.IndexFieldNames.UUID);
			
			String uuid = doc.get(Geonet.IndexFieldNames.UUID);
			Log.debug(Geonet.SEARCH_ENGINE, "ECatOperationManager >> getMetadataByLuceneSearch --> uuid: " + uuid);
			
			if (uuid != null) {
				Metadata md = mdRepo.findOneByUuid(uuid);
				return md;
			}

		} catch (Exception e) {
			throw new Exception("failed to get this record using lucene search");
		}

		return null;
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
	public String getECatIdFromUUID(ApplicationContext context, String uuid) throws Exception  {
		
		
		Element element = Xml
				.loadString("<request><isAdmin>true</isAdmin><_isTemplate>n</_isTemplate><_uuid>"
						+ uuid + "</_uuid><fast>index</fast></request>", false);
		
		try {
			
			Document doc = getLuceneTopDocs(element, Geonet.IndexFieldNames.ECAT_ID);
			String eCatId = doc.get(Geonet.IndexFieldNames.ECAT_ID);
			
			Log.debug(Geonet.SEARCH_ENGINE, "ECatOperationManager >> getECatIdFromUUID --> eCatId: " + eCatId);
			
			return eCatId;

		} catch (Exception e) {
			throw new Exception("Invalid Record/Corrupted Record " + uuid);
		}

		
	}
	
	private Document getLuceneTopDocs(Element element, String fieldVisitor) throws IOException {
		IndexAndTaxonomy indexAndTaxonomy = null;
		try {
			indexAndTaxonomy = this.tracker.acquire("eng", -1);
		} catch (IOException e) {
			Log.error(Geonet.SEARCH_ENGINE, "Unable to index and taxonomy, "+e.getMessage());
		}
		IndexSearcher searcher = new IndexSearcher(indexAndTaxonomy.indexReader);
		
		LuceneQueryInput luceneQueryInput = new LuceneQueryInput(element);
		
		Query _query = new LuceneQueryBuilder(luceneConfig, luceneConfig.getTokenizedField(),
				SearchManager.getAnalyzer(Geonet.DEFAULT_LANGUAGE, true), Geonet.DEFAULT_LANGUAGE)
						.build(luceneQueryInput);
		
		TopDocs tdocs = searcher.search(_query, 1);
		
		DocumentStoredFieldVisitor docVisitor = new DocumentStoredFieldVisitor(fieldVisitor);

		indexAndTaxonomy.indexReader.document(tdocs.scoreDocs[0].doc, docVisitor);
		Document doc = docVisitor.getDocument();
		
		return doc;
	}


	public LuceneIndexLanguageTracker getTracker() {
		return tracker;
	}


	public void setTracker(LuceneIndexLanguageTracker tracker) {
		this.tracker = tracker;
	}


	public LuceneConfig getLuceneConfig() {
		return luceneConfig;
	}


	public void setLuceneConfig(LuceneConfig luceneConfig) {
		this.luceneConfig = luceneConfig;
	}
	
	
}
