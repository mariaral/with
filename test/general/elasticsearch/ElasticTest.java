/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package general.elasticsearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.MediaObject;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithDate;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.RecordResource;
import model.resources.RecordResource.RecordDescriptiveData;
import model.resources.WithResource.ExternalCollection;
import model.resources.WithResourceType;
import model.resources.collection.CollectionObject;
import model.usersAndGroups.User;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import scala.Array;
import db.DB;
import elastic.Elastic;
import elastic.ElasticEraser;
import elastic.ElasticIndexer;
import elastic.ElasticReindexer;
import elastic.ElasticUtils;

public class ElasticTest {

	@Test
	public void testIndex() {


		RecordResource rr = getRecordResource();
		//CollectionObject co = DB.getCollectionObjectDAO().getById(new ObjectId("569e1f284f55a2655367ec1e"));
		//if (DB.getRecordResourceDAO().makePermanent(rr) == null) { System.out.println("No storage!"); return; }
		System.out.println("Stored!");
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		Json.setObjectMapper(mapper);
		//System.out.println(Json.toJson(rr));
		//System.out.println(rr.transform());
		System.out.println(Json.toJson(ElasticUtils.toIndex(rr)));
		//ElasticIndexer.index(Elastic.typeResource, rr.getDbId(), rr.transform());

	}

	@Test
	public void testIndexMany() {
		List<ObjectId> ids = new ArrayList<ObjectId>();
		ids.add(new ObjectId("56aa0c994f55a23b71669814"));
		ids.add(new ObjectId("56aa0c654f55a23a4494ace1"));
		ids.add(new ObjectId("56aa0c334f55a238e81cb44a"));
		ids.add(new ObjectId("56a8e76d2260ea229ed73886"));
		List<RecordResource> rrs = DB.getRecordResourceDAO().getByIds(ids);
		User u = DB.getUserDAO().getByUsername("qwerty");
		if(u == null) {
			System.out.println("No user found");
			return;
		}
		for(RecordResource co: rrs) {
			co.getAdministrative().setWithCreator(u.getDbId());
		}

		List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
		ids.clear();
		for(RecordResource rr: rrs) {
			ids.add(rr.getDbId());
			docs.add(rr.transform());
		}
		ElasticIndexer.indexMany(Elastic.typeResource, ids, docs);
	}

	@Test
	public void testIndexManyCollections() {
		List<ObjectId> ids = new ArrayList<ObjectId>();
		ids.add(new ObjectId("56aa04aa4f55a2145bbc8e7f"));
		ids.add(new ObjectId("56aa047c4f55a21327f10bbc"));
		ids.add(new ObjectId("56aa04034f55a20fb7166f15"));
		ids.add(new ObjectId("56aa03d94f55a20ed2ffcf81"));
		List<CollectionObject> rrs = DB.getCollectionObjectDAO().getByIds(ids);
		User u = DB.getUserDAO().getByUsername("qwerty");
		if(u == null) {
			System.out.println("No user found");
			return;
		}
		for(CollectionObject co: rrs) {
			co.getAdministrative().setWithCreator(u.getDbId());
		}

		List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
		ids.clear();
		for(CollectionObject rr: rrs) {
			ids.add(rr.getDbId());
			docs.add(rr.transform());
		}
		ElasticIndexer.indexMany(WithResourceType.SimpleCollection.toString().toLowerCase(), ids, docs);
	}

	@Test
	public void testDeleteResource() {
		RecordResource rr = getRecordResource();
		DB.getRecordResourceDAO().makePermanent(rr);
		ElasticIndexer.index(Elastic.typeResource, rr.getDbId(), rr.transform());

		TermQueryBuilder termQ = QueryBuilders.termQuery("_id", rr.getDbId());
		SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertThat(resp.getHits().getTotalHits(), not(equalTo(0)));

		ElasticEraser.deleteResource(Elastic.typeResource, rr.getDbId().toString());

		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertEquals(resp.getHits().getTotalHits(), 0);
		DB.getRecordResourceDAO().deleteById(rr.getDbId());
	}

	@Test
	public void testDeleteManyResources() {
		RecordResource rr1 = getRecordResource();
		RecordResource rr2 = getRecordResource();
		DB.getRecordResourceDAO().makePermanent(rr1);
		DB.getRecordResourceDAO().makePermanent(rr2);

		ElasticIndexer.index(Elastic.typeResource, rr1.getDbId(), rr1.transform());
		ElasticIndexer.index(Elastic.typeResource, rr2.getDbId(), rr2.transform());

		TermQueryBuilder termQ = QueryBuilders.termQuery("_id", rr1.getDbId());
		SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();
		assertThat(resp.getHits().getTotalHits(), not(equalTo(0)));

		termQ = QueryBuilders.termQuery("_id", rr2.getDbId());
		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();
		assertThat(resp.getHits().getTotalHits(), not(equalTo(0)));


		List<ObjectId> ids = new ArrayList<ObjectId>();
		ids.add(rr1.getDbId());
		ids.add(rr2.getDbId());
		ElasticEraser.deleteManyResources(ids);

		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertEquals(resp.getHits().getTotalHits(), 0);
		DB.getRecordResourceDAO().deleteById(rr1.getDbId());
		DB.getRecordResourceDAO().deleteById(rr2.getDbId());
	}


	/* **************** PRIVATE METHODS ********************** */


	private RecordResource getRecordResource() {
		RecordResource<RecordDescriptiveData> rr = new RecordResource<RecordResource.RecordDescriptiveData>();

		/*
		 * Owner of the CollectionObject
		 *
		 */
		User u = DB.getUserDAO().getByUsername("qwerty");
		if(u == null) {
			System.out.println("No user found");
			return null;
		}

		/*
		 * Administative metadata
		 */
		rr.getAdministrative().setCreated(new Date());
		//wa.setWithCreator(u.getDbId());
		WithAccess waccess = new WithAccess();
		waccess.setIsPublic(true);
		waccess.getAcl().add(new AccessEntry(u.getDbId(), Access.OWN));
		rr.getAdministrative().setWithCreator(u.getDbId());
		rr.getAdministrative().setAccess(waccess);

		//no externalCollections
		List<ExternalCollection> ec;

		//no provenance
		ProvenanceInfo prov2 = new ProvenanceInfo("Europeana", "http://", "00");
		ProvenanceInfo prov1 = new ProvenanceInfo("Mint", "http://", "001");
		List<ProvenanceInfo> prov = new ArrayList<ProvenanceInfo>();
		prov.add(prov2);
		prov.add(prov1);
		rr.setProvenance(prov);

		//collectedIn
		rr.setCollectedIn(new ArrayList<ObjectId>() {{ add(new ObjectId()); }});

		//resourceType is collectionObject
		//co.setResourceType(WithResourceType.CollectionObject);
		// type: metadata specific for a collection
		MultiLiteral label = new MultiLiteral(Language.EN, "Grass field");
		label.addLiteral(Language.UNKNOWN, "I don't know");
		label.addLiteral(Language.DEFAULT, "DEFAULT");
		RecordDescriptiveData cdd = new RecordDescriptiveData();
		cdd.setLabel(label);
		MultiLiteral desc = new MultiLiteral(Language.EN, "This is a description");
		cdd.setDescription(desc);

		LiteralOrResource metaRights = new LiteralOrResource("CCO");
		cdd.setMetadataRights(metaRights);

		WithDate date = new WithDate();
		date.setYear(1998);
		List<WithDate> dates = new ArrayList<WithDate>();
		dates.add(date);
		WithDate date1 = new WithDate();
		date1.setYear(2004);
		dates.add(date1);
		cdd.setDates(dates);

		rr.setDescriptiveData(cdd);
		/*
		 * no content for the collection
		 */
		Map<String, String> content;

		/*
		 * media thumbnail for collection
		 */
		ArrayList<EmbeddedMediaObject> medias = new ArrayList<EmbeddedMediaObject>();
		EmbeddedMediaObject emo = getMediaObject();
		medias.add(emo);
		//co.setMedia(medias);

		return rr;
	}



	private MediaObject getMediaObject() {

		MediaObject mo = new MediaObject();
		byte[] rawbytes = null;
		URL url = null;
		try {
			url = new URL("http://www.ntua.gr/schools/ece.jpg");
			File file = new File("test_java.txt");
			ImageInputStream iis = ImageIO.createImageInputStream(file);
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

			if (readers.hasNext()) {

                // pick the first available ImageReader
                ImageReader reader = readers.next();

                // attach source to the reader
                reader.setInput(iis, true);

                // read metadata of first image
                IIOMetadata metadata = reader.getImageMetadata(0);

                String[] names = metadata.getMetadataFormatNames();
                int length = names.length;
                for (int i = 0; i < length; i++) {
                    System.out.println( "Format name: " + names[ i ] );
                }
            }

			FileUtils.copyURLToFile(url, file);
			FileInputStream fileStream = new FileInputStream(
					file);

			rawbytes = IOUtils.toByteArray(fileStream);
		} catch(Exception e) {
			System.out.println(e);
			System.exit(-1);
		}

		mo.setMediaBytes(rawbytes);
		//mo.setMimeType(MediaType.ANY_IMAGE_TYPE);
		mo.setHeight(875);
		mo.setWidth(1230);
		LiteralOrResource lor = new LiteralOrResource(Language.EN, url.toString());
		mo.setOriginalRights(lor);
		mo.setWithRights(WithMediaRights.Creative);
		mo.setType(WithMediaType.IMAGE);
		mo.setUrl(url.toString());

		try {
			DB.getMediaObjectDAO().makePermanent(mo);
			System.out.println("Media succesfully saved!");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mo;
	}
}
