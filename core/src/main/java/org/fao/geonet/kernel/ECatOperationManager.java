package org.fao.geonet.kernel;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.persistence.Tuple;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataIndexedField;
import org.fao.geonet.exceptions.BatchEditException;
import org.fao.geonet.kernel.search.IndexAndTaxonomy;
import org.fao.geonet.kernel.search.IndexFields;
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

import com.google.common.base.Joiner;

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
	
		MetadataRepository mdRepo = context.getBean(MetadataRepository.class);

		try {
			
			String uuid = getUuidFromECatId(eCatId);
			
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
	public String getUuidFromECatId(String eCatId) throws Exception  {
	
		Element element = Xml
				.loadString("<request><isAdmin>true</isAdmin><_isTemplate>n</_isTemplate><eCatId>"
						+ eCatId + "</eCatId><fast>index</fast></request>", false);
		
		try {
			
			Document doc = getLuceneTopDocs(element, Geonet.IndexFieldNames.UUID);
			
			String uuid = doc.get(Geonet.IndexFieldNames.UUID);
			
			return uuid;

		} catch (Exception e) {
			return "";
		}

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
	public MetadataIndexedField getMetadataIndexedFieldsFromECatId(String eCatId) throws Exception  {
	
		Element element = Xml
				.loadString("<request><isAdmin>true</isAdmin><_isTemplate>n</_isTemplate><eCatId>"
						+ eCatId + "</eCatId><fast>index</fast></request>", false);
		
		//Log.info(Geonet.SEARCH_ENGINE, "ECatOperation eCatId : " + eCatId);
		try {
			
			Document doc = getLuceneTopDocs(element, IndexFields.UUID, IndexFields.KEYWORD, IndexFields.PID, IndexFields.AUTHOR);
			
			String uuid = doc.get(IndexFields.UUID).trim();
			String pid = doc.get(IndexFields.PID).trim();
			String authors = Arrays.asList(doc.getFields(IndexFields.AUTHOR)).stream().map(IndexableField::stringValue).collect(Collectors.joining(", "));
			String keywords = Arrays.asList(doc.getFields(IndexFields.KEYWORD)).stream().map(IndexableField::stringValue).collect(Collectors.joining(", "));
			
			MetadataIndexedField indexField = new MetadataIndexedField();
			indexField.setUuid(uuid);
			indexField.seteCatId(eCatId);
			indexField.setAuthors(authors);
			indexField.setKeywords(keywords);
			indexField.setPid(pid);
			
			return indexField;
			

		} catch (Exception e) {
			throw new Exception("failed to get MetadataIndexedFields using lucene search" + e.getMessage());
		}
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
			
			return eCatId;

		} catch (Exception e) {
			throw new Exception("Invalid Record/Corrupted Record " + uuid);
		}

		
	}
	
	public Element getResults(ApplicationContext context, List<String> uuids) throws Exception  {
		
		String _uuids = Joiner.on(" or ").join(uuids);
		
		Log.info(Geonet.SEARCH_ENGINE, "ECatOperation uuids size : " + uuids.size());
		//Log.info(Geonet.SEARCH_ENGINE, "ECatOperation uuids : " + _uuids);
		Element element = Xml
				.loadString("<request><_isTemplate>n</_isTemplate><_uuid>"
						+ _uuids + "</_uuid><fast>index</fast></request>", false);
		
		try {
			
			return getLuceneTopDocs(element, uuids.size(), new HashSet<String>(Arrays.asList(Geonet.IndexFieldNames.ECAT_ID, Geonet.IndexFieldNames.UUID, Geonet.IndexFieldNames.DATABASE_CHANGE_DATE)));
			
		} catch (Exception e) {
			throw new Exception("Unable to get list of uuid using lucene");
		}

	}


	private Document getLuceneTopDocs(Element element, String... fieldVisitor) throws IOException {
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
	
	private Element getLuceneTopDocs(Element element, int n, Set<String> fieldsToLoad) throws IOException {
		Element result = new Element("metadata");
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
		
		TopDocs tdocs = searcher.search(_query, n);
		
		Log.info(Geonet.SEARCH_ENGINE, "ECatOperation tdocs.totalHits : " + tdocs.totalHits);
		
		//DocumentStoredFieldVisitor docVisitor = new DocumentStoredFieldVisitor(fieldVisitor);

		ScoreDoc[] docs = tdocs.scoreDocs;
		
		for (ScoreDoc scoreDoc : docs) {
			Document doc = searcher.doc(scoreDoc.doc, fieldsToLoad);
			
			String eCatId = doc.get(Geonet.IndexFieldNames.ECAT_ID);
			String uuid = doc.get(Geonet.IndexFieldNames.UUID);
			String changeDate = doc.get(Geonet.IndexFieldNames.DATABASE_CHANGE_DATE);
			
			
			
			Element recordE = new Element("record");
			Element eCatIdE = new Element("eCatId");
            Element uuidE = new Element("uuid");
            Element schemaidE = new Element("schemaid");
            Element changedateE = new Element("changedate");

            uuidE.addContent(uuid);
            eCatIdE.addContent(eCatId);
            changedateE.addContent(changeDate);
            schemaidE.addContent("");

            recordE.addContent(uuidE);
            recordE.addContent(eCatIdE);
            recordE.addContent(changedateE);
            recordE.addContent(schemaidE);

            result.addContent(recordE);
		}
		
		return result;
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
