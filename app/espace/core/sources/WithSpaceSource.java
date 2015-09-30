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


package espace.core.sources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Collection;
import model.User;
import model.Rights.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import utils.Tuple;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import espace.core.CommonQuery;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.SourceResponse;

public class WithSpaceSource extends ISpaceSource {
	public static final Logger.ALogger log = Logger.of(WithSpaceSource.class);


	@Override
	public String getSourceName() {
		return "WITHin";
	}

	private List<List<Tuple<ObjectId, Access>>> mergeLists(List<Tuple<ObjectId, Access>> ... lists) {
		List<List<Tuple<ObjectId, Access>>> outputList = new ArrayList<List<Tuple<ObjectId, Access>>>();
		for (List<Tuple<ObjectId, Access>> list: lists) {
			for (Tuple<ObjectId, Access> tuple: list) {
				List<Tuple<ObjectId, Access>> sepList = new ArrayList<Tuple<ObjectId, Access>>(1);
				sepList.add(tuple);
				outputList.add(sepList);
			}
	    }
		return outputList;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		/*
		 * Set the basic search parameters
		 */
		ElasticSearcher searcher = new ElasticSearcher(Elastic.type_general);
		String term = q.getQuery();
		int count = Integer.parseInt(q.pageSize);
		//int offset = (Integer.parseInt(q.page)-1)*count;
		int offset = Integer.parseInt(q.page)-1;

		/*
		 * Prepare access lists for searching
		 */
		//This value (1000) is problematic. We have to deal with scan and scroll
		SearchOptions elasticoptions = new SearchOptions(0, 1000);
		List<Collection> colFields = new ArrayList<Collection>();
		List<String> userIds = q.getEffectiveUserIds();
		List<Tuple<ObjectId, Access>> userAccess = new ArrayList<Tuple<ObjectId, Access>>();
		if ((userIds != null) && !userIds.isEmpty()) {
			for (String userId: userIds)
				userAccess.add(new Tuple<ObjectId, Access>(new ObjectId(userId), Access.READ));
		}
		List<List<Tuple<ObjectId, Access>>> accessFilters = mergeLists(q.getDirectlyAccessedByGroupName(), q.getDirectlyAccessedByUserName());
		if(!userAccess.isEmpty())
			accessFilters.add(userAccess);
		elasticoptions.accessList = accessFilters;

		/*
		 * Search index for accessible collections
		 */
		searcher.setType(Elastic.type_collection);
		SearchResponse response = searcher.searchAccessibleCollections(null, elasticoptions);
		colFields = getCollectionMetadaFromHit(response.getHits());


		System.out.println(accessFilters.toString());

		/*
		 * Search index for merged records according to
		 * collection ids gathered above
		 */
		elasticoptions = new SearchOptions(offset, count);
		elasticoptions.addFilter("isPublic", "true");
		searcher.setType(Elastic.type_general);
		for(Collection collection : colFields) {
			elasticoptions.addFilter("collections", collection.getDbId().toString());
		}

		SearchResponse resp = searcher.search(term, elasticoptions);
		searcher.closeClient();
		return new SourceResponse(resp, count);
	}


	private List<Collection> getCollectionMetadaFromHit(
			SearchHits hits) {


		List<Collection> colFields = new ArrayList<Collection>();
		for(SearchHit hit: hits.getHits()) {
			JsonNode json         = Json.parse(hit.getSourceAsString());
			JsonNode accessRights = json.get("rights");
			if(!accessRights.isMissingNode()) {
				ObjectNode ar = Json.newObject();
				for(JsonNode r: accessRights) {
					String user   = r.get("user").asText();
					String access = r.get("access").asText();
					ar.put(user, access);
				}
				((ObjectNode)json).remove("rights");
				((ObjectNode)json).put("rights", ar);
			}
			Collection collection = Json.fromJson(json, Collection.class);
			collection.setDbId(new ObjectId(hit.getId()));
			colFields.add(collection);
		}
		return colFields;
	}

	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		log.debug("Method not implemented yet");
		return null;
	}



}
