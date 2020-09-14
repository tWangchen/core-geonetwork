package org.fao.geonet.api.records;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataType;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.UpdateDatestamp;
import org.fao.geonet.kernel.datamanager.IMetadataUtils;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.repository.SourceRepository;
import org.fao.geonet.services.AbstractServiceIntegrationTest;
import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.collect.Lists;

import jeeves.server.context.ServiceContext;

public class DoiApiTest extends AbstractServiceIntegrationTest {

	@Autowired private WebApplicationContext wac;
	@Autowired private SchemaManager schemaManager;
	@Autowired private DataManager dataManager;
	@Autowired private IMetadataUtils metadataUtils;
	@Autowired private SourceRepository sourceRepository;
	@Autowired private MetadataRepository metadataRepository;
	
	private String uuid;
	private int id;
	private int eCatId; 
	private AbstractMetadata md;
	private ServiceContext context;

	@Before
	public void setUp() throws Exception {
		this.context = createServiceContext();
		createTestData();
	}

	private void createTestData() throws Exception {
		loginAsAdmin(context);

		final Element sampleMetadataXml = getSampleMetadata19115Xml();
		this.uuid = UUID.randomUUID().toString();
		
		eCatId = ThreadLocalRandom.current().nextInt(190000, 195000);

		String source = sourceRepository.findAll().get(0).getUuid();
		String schema = schemaManager.autodetectSchema(sampleMetadataXml);
		
		metadataUtils.setUUID(schema, uuid, sampleMetadataXml);
		metadataUtils.setGAID(schema, String.valueOf(eCatId), sampleMetadataXml);
		
		final Metadata metadata = new Metadata();
		metadata.setDataAndFixCR(sampleMetadataXml).setUuid(uuid);
		metadata.getDataInfo().setRoot(sampleMetadataXml.getQualifiedName()).setSchemaId(schema)
				.setType(MetadataType.METADATA);
		metadata.getDataInfo().setPopularity(1000);
		metadata.getSourceInfo().setOwner(1).setSourceId(source);
		metadata.getHarvestInfo().setHarvested(false);

		this.id = dataManager.insertMetadata(context, metadata, sampleMetadataXml, false, false, false,
				UpdateDatestamp.NO, false, false).getId();

		dataManager.indexMetadata(Lists.newArrayList("" + this.id));
		this.md = metadataRepository.findOne(this.id);
	}

	@Test
	public void getRecord() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		MockHttpSession mockHttpSession = loginAsAdmin();

		Map<String, String> contentTypes = new LinkedHashMap<>();

		for (Map.Entry<String, String> entry : contentTypes.entrySet()) {
			mockMvc.perform(get("/srv/api/records/"+eCatId+"/doi").session(mockHttpSession).accept(entry.getKey()))
					.andDo(print()).andExpect(status().isOk());
		}
	}

}
