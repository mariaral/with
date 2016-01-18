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


package elastic;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.Collection;
import model.CollectionRecord;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.WithAccess.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.ScriptService.ScriptType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import db.DB;

public class ElasticUpdater {
	static private final Logger.ALogger log = Logger.of(ElasticUpdater.class);


	/*
	 * Update one document with the structure provided.
	 */
	public static void updateOne(ObjectId id, Map<String, Object> doc) {
		Elastic.getTransportClient().prepareUpdate(
				Elastic.index,
				Elastic.type,
				id.toString())
				.setDoc(doc)
				.get();
	}


	/*
	 * Bulk updates. Updates all documents provided with the structure
	 * provided.
	 */
	public static void updateMany(List<ObjectId> ids, List<Map<String, Object>> docs) throws Exception {

		if(ids.size() != docs.size()) {
			throw new Exception("Error: ids list does not have the same size with upDocs list");
		}

		if( ids.size() == 0 ) {
			log.debug("No resources to update!");
		} else if( ids.size() == 1 ) {
					Elastic.getTransportClient().prepareUpdate(
							Elastic.index,
							Elastic.type,
							ids.get(0).toString())
							.setDoc(docs.get(0))
							.get();
		} else {

				int i = 0;
				for(Map<String, Object> doc: docs) {
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							Elastic.type,
							ids.get(i).toString())
					.doc(doc));
					i++;
				}
				Elastic.getBulkProcessor().close();
		}

	}

	/*
	 * Completes the whole update process of a merged document
	 */
	public static void addResourceToCollection(String id, ObjectId colId, int position) throws IOException {
		try {
			Elastic.getTransportClient().prepareUpdate()
				.setIndex(Elastic.index)
				.setType(Elastic.type)
				.setId(id)
			//.addScriptParam("colInfo", new CollectionInfo(colId, position))
			.addScriptParam("colInfo", jsonBuilder().startObject().field("collectionId", colId).field("position", position).endObject())
			.setScript("ctx._source.collectedIn.add(colInfo)", ScriptType.INLINE)
			.execute().actionGet();
		} catch (ElasticsearchException  e) {
			log.error("Cannot add entry to collectedIn and update document!", e);
		}
	}


	public static void removeResourceFromCollection(String id, ObjectId colId, int position) {
		try {
			Elastic.getTransportClient().prepareUpdate()
				.setIndex(Elastic.index)
				.setType(Elastic.type)
				.setId(id)
			.addScriptParam("colId", colId.toString())
			.addScriptParam("pos", position)
			.setScript("info = null"
					+ "ctx._source.collectedIn.each {"
					+ " if( it.collectionId.equals(colId) &&"
					+ "    it.position == pos) { "
					+ "      info = it "
					+ " } "
					+ "  }"
					+ "ctx._source.collectedIn.remove(info) ", ScriptType.INLINE)
			.execute().actionGet();
		} catch (ElasticsearchException  e) {
			log.error("Cannot remove entry from collectedIn and update document!", e);
		}
	}


	public static void updatePositionInCollection(String id, ObjectId colId, int old_position, int new_position) {
		try {
			Elastic.getTransportClient().prepareUpdate()
				.setIndex(Elastic.index)
				.setType(Elastic.type)
				.setId(id)
			.addScriptParam("colId", colId.toString())
			.addScriptParam("old_pos", old_position)
			.addScriptParam("new_pos", new_position)
			.setScript("ctx._source.collectedIn.each {"
					+ " if( it.collectionId.equals(colId) &&"
					+ "    it.position == old_pos) { "
					+ "      position = new_pos "
					+ " } "
					+ "  }", ScriptType.INLINE)
			.execute().actionGet();
		} catch (ElasticsearchException  e) {
			log.error("Cannot update merged record document!", e);
		}
	}


	/*
	 * Update rights on a collection
	 */
	public static void updateCollectionRights(ObjectId id) {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						Elastic.type,
						id.toString())
				.setDoc(prepareUpdateOnRights(id))
				.execute().actionGet();		} catch (Exception e) {
			log.error("Cannot update collection rights!", e);
		}
	}

	public static XContentBuilder prepareUpdateOnRights(ObjectId id) {
		XContentBuilder doc = null;
		try {

			doc = jsonBuilder().startObject();
			ArrayNode array = Json.newObject().arrayNode();
			/*for(Entry<ObjectId, Access> e: collection.getRights().entrySet()) {
				ObjectNode right = Json.newObject();
				right.put("user", e.getKey().toString());
				switch (e.getValue().toString()) {
				case "OWN":
					right.put("access", 3);
					break;
				case "WRITE":
					right.put("access", 2);
					break;
				case "READ":
					right.put("access", 1);
					break;
				case "NONE":
					right.put("access", 0);
					break;
				default:
					break;
				}
				array.add(right);
			}*/
			doc.rawField("rights", array.toString().getBytes());
			doc.endObject();

		} catch(IOException io) {
			log.error("Cannot create document to update!", io);
			return null;
		}

		return doc;
	}


	/*
	 * Takes a list of ids and visibility values and updates
	 * the visibility on these documents
	 *
	 * For example when a Collection becomes public then we have to make all the nested douments public
	 * Or when a Collection becomes private the all the resources become private unless that resources that
	 * belong in public collections.
	 */
	public static void updateVisibility(List<ObjectId> ids, List<Boolean> visibility) throws Exception {

		if(ids.size() != visibility.size()) {
			throw new Exception("Error: ids list does not have the same size with upDocs list");
		}


		XContentBuilder doc = null;
		if( ids.size() == 0 ) {
			log.debug("No resources to update!");
		} else if( ids.size() == 1 ) {

			try {
				doc = jsonBuilder().startObject();
				doc.field("isPublic", visibility.get(0));
				doc.endObject();
			} catch(IOException io) {
				log.error("Cannot create document to update!", io);
			}

			Elastic.getTransportClient().prepareUpdate(
					Elastic.index,
					Elastic.type,
					ids.get(0).toString())
				.setDoc(doc)
				.get();
		} else {
				for(int i = 0; i<ids.size(); i++) {

					try {
						doc = jsonBuilder().startObject();
						doc.field("isPublic", visibility.get(i));
						doc.endObject();
					} catch(IOException io) {
						log.error("Cannot create document to update!", io);
					}
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							Elastic.type,
							ids.get(i).toString())
						.doc(doc));
				}
				Elastic.getBulkProcessor().flush();
		}
	}


	/*
	 * Increment likes on collection type
	 */

	public static void incLikes(ObjectId id) {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						Elastic.type,
						id.toString())
				.setScript("ctx._source.usage.likes++;", ScriptType.INLINE)
				.execute().actionGet();
			} catch (Exception e) {
			log.error("Cannot update collection likes!", e);
			}
	}

	/*
	 * Decrement likes on collection type
	 */
	public static void decLikes(ObjectId id) {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						Elastic.type,
						id.toString())
				.setScript("ctx._source.usage.likes--;", ScriptType.INLINE)
				.execute().actionGet();
			} catch (Exception e) {
			log.error("Cannot update collection likes!", e);
			}
	}

}
