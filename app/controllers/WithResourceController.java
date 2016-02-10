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


package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CollectionObject;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
import model.resources.WithResource;
import model.resources.WithResource.WithResourceType;
import model.annotations.ContextData.ContextDataType;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataTarget;
import model.annotations.ExhibitionData;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sources.core.ISpaceSource;
import sources.core.ParallelAPICall;
import sources.core.RecordJSONMetadata;
import utils.AccessManager;
import utils.AccessManager.Action;
import utils.Locks;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.parameterTypes.StringTuple;
import db.DB;
import db.WithResourceDAO;

/**
 * @author mariaral
 *
 */
@SuppressWarnings("rawtypes")
public class WithResourceController extends Controller {

	public static final ALogger log = Logger.of(WithResourceController.class);

	public static Status errorIfNoAccessToWithResource(
			WithResourceDAO resourceDAO, Action action, ObjectId id) {
		ObjectNode result = Json.newObject();
		List<String> effectiveUserIds = AccessManager
				.effectiveUserIds(session().get("effectiveUserIds"));
		if (!resourceDAO.existsEntity(id)) {
			log.error("Cannot retrieve resource from database");
			result.put("error", "Cannot retrieve resource " + id
					+ " from database");
			return internalServerError(result);
			// TODO superuser
		} else if (!resourceDAO.hasAccess(
				AccessManager.effectiveUserDbIds(session().get(
						"effectiveUserIds")), action, id)
				&& !AccessManager.isSuperUser(effectiveUserIds.get(0))) {
			result.put("error", "User does not have " + action
					+ " access for resource " + id);
			return forbidden(result);
		} else {
			return ok();
		}
	}

	public static Status errorIfNoAccessToCollection(Action action,
			ObjectId collectionDbId) {
		return errorIfNoAccessToWithResource(DB.getCollectionObjectDAO(),
				action, collectionDbId);
	}

	public static Status errorIfNoAccessToRecord(Action action,
			ObjectId recordId) {
		return errorIfNoAccessToWithResource(DB.getRecordResourceDAO(), action,
				recordId);
	}

	/**
	 * @param id
	 *            the collection id
	 * @param position
	 *            the position of the record in the collection
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Result addRecordToCollection(String colId,
			Option<Integer> position) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId collectionDbId = new ObjectId(colId);
		Locks locks = null;
		try {
			locks = Locks.create().write("Collection #" + colId).acquire();
			Status response = errorIfNoAccessToCollection(Action.EDIT,
					collectionDbId);
			if (!response.toString().equals(ok().toString())) {
				return response;
			} else {
				if (json == null) {
					result.put("error", "Invalid JSON");
					return badRequest(result);
				}
				String resourceType = null;
				ObjectId userId = AccessManager.effectiveUserDbIds(
						session().get("effectiveUserIds")).get(0);
				if (json.has("resourceType"))
					resourceType = json.get("resourceType").asText();
				if (resourceType == null
						|| WithResourceType.valueOf(resourceType) == null)
					resourceType = WithResourceType.CulturalObject.toString();
				Class<?> clazz = Class.forName("model.resources."
						+ resourceType);
				RecordResource record = (RecordResource) Json.fromJson(json,
						clazz);
				int last = 0;
				Sources source = Sources.UploadedByUser;
				if (record.getProvenance() != null
						&& !record.getProvenance().isEmpty()) {
					last = record.getProvenance().size() - 1;
					source = Sources.valueOf(((ProvenanceInfo) record
							.getProvenance().get(last)).getProvider());
				} else
					record.setProvenance(new ArrayList<ProvenanceInfo>(Arrays
							.asList(new ProvenanceInfo(source.toString()))));
				String externalId = ((ProvenanceInfo) record.getProvenance()
						.get(last)).getResourceId();
				if (externalId == null)
					externalId = record.getAdministrative().getExternalId();
				ObjectId recordId = null;
				if (externalId != null
						&& DB.getRecordResourceDAO().existsWithExternalId(
								externalId)) {// get dbId of existing resource
					RecordResource resource = DB
							.getRecordResourceDAO()
							.getUniqueByFieldAndValue(
									"administrative.externalId", externalId,
									new ArrayList<String>(Arrays.asList("_id")));
					recordId = resource.getDbId();
					response = errorIfNoAccessToRecord(Action.READ, recordId);
					if (!response.toString().equals(ok().toString())) {
						return response;
					} else {
						// In case the record already exists we overwrite the
						// existing record's descriptive data for the fields
						// included
						// in the json, if the user has WRITE.
						// access.
						if (DB.getRecordResourceDAO().hasAccess(
								AccessManager.effectiveUserDbIds(session().get(
										"effectiveUserIds")), Action.EDIT,
								recordId)
								&& (json.get("descriptiveData") != null))
							DB.getRecordResourceDAO().editRecord(
									"descriptiveData", resource.getDbId(),
									json.get("descriptiveData"));
						// TODO: if record has annotations, update/add
						// annotations

					}
				} else { // create new record in db
					ObjectNode errors;
					// Create a new record
					record.getAdministrative().setCreated(new Date());
					// TODO: if record has annotations, update/add annotations
					switch (source) {
					case UploadedByUser:
						// Fill the EmbeddedMediaObject from the MediaObject
						// that has been created
						record.getAdministrative().setWithCreator(userId);
						String mediaUrl;
						WithMediaRights withRights;
						EmbeddedMediaObject media;
						EmbeddedMediaObject embeddedMedia;
						for (MediaVersion version : MediaVersion.values()) {
							if ((embeddedMedia = ((HashMap<MediaVersion, EmbeddedMediaObject>) record
									.getMedia().get(0)).get(version)) != null) {
								mediaUrl = embeddedMedia.getUrl();
								withRights = embeddedMedia.getWithRights();
								media = new EmbeddedMediaObject(DB
										.getMediaObjectDAO().getByUrl(mediaUrl));
								media.setWithRights(withRights);
								record.addMedia(version, media);
							}
						}
						DB.getRecordResourceDAO().makePermanent(record);
						recordId = record.getDbId();
						// setting of provevance's resourceId and externalId
						// (based on recordDbId) is taken care by postLoad in
						// WithResource
						DB.getRecordResourceDAO().updateWithURI(recordId,
								"/record/" + recordId);
						DB.getRecordResourceDAO().updateProvenance(
								recordId,
								last,
								new ProvenanceInfo("UploadedByUser", "record/"
										+ recordId, recordId.toString()));
						DB.getRecordResourceDAO().updateField(recordId,
								"administrative.externalId",
								recordId.toString());
						// DB.getRecordResourceDAO().updateField(recordId,
						// "administrative.externalId", recordId.toString());
						break;
					case Mint:
						errors = RecordResourceController
								.validateRecord(record);
						record.getAdministrative().setWithCreator(userId);
						List<ProvenanceInfo> provenance = record
								.getProvenance();
						record.getAdministrative().setExternalId(
								provenance.get(last).getResourceId());
						if (errors != null) {
							return badRequest(errors);
						}
						DB.getRecordResourceDAO().makePermanent(record);
						DB.getRecordResourceDAO().updateWithURI(recordId,
								"/record/" + recordId);
						recordId = record.getDbId();
						break;
					default:// imported first time from other sources
						// there is no withCreator and the record is public
						record.getAdministrative().getAccess()
								.setIsPublic(true);
						errors = RecordResourceController
								.validateRecord(record);
						provenance = record.getProvenance();
						record.getAdministrative().setExternalId(
								provenance.get(last).getResourceId());
						if (errors != null) {
							return badRequest(errors);
						}
						DB.getRecordResourceDAO().makePermanent(record);
						recordId = record.getDbId();
						DB.getRecordResourceDAO().updateWithURI(recordId,
								"/record/" + recordId);
						addContentToRecord(record.getDbId(), source.toString(),
								externalId);
						break;
					}
				}
				// Updates collection administrative metadata and record's usage
				// and collectedIn
				// the rights of all collections the resource belongs to are
				// merged and are copied to the record
				// only if the user OWNs the resource
				boolean owns = DB.getRecordResourceDAO().hasAccess(
						AccessManager.effectiveUserDbIds(session().get(
								"effectiveUserIds")), Action.DELETE, recordId);
				// make sure that update is called after record has been saved
				// in the db
				// while (!record.isPostPersist()) {};
				if (position.isDefined() && recordId != null) {
					Integer pos = position.get();
					DB.getRecordResourceDAO().addToCollection(recordId,
							collectionDbId, pos, owns);
				} else {
					DB.getRecordResourceDAO().appendToCollection(recordId,
							collectionDbId, owns);
				}
				result.put("message", "Record succesfully added to collection");
				return ok(result);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	public static void updateContextData(JsonNode contextAnnsJson,
			WithResource record, ObjectId colId, ObjectId userId) {
		List<ContextData> ContextData = getContextDataFromJson(contextAnnsJson);
		for (ContextData contextAnn : ContextData) {
			ObjectId contextAnnId = contextAnn.getTarget().getCollectionId();
			if (colId.equals(contextAnnId)) {// ignore annotations that refer to
												// other collections! to be
												// faster
				for (ContextData ca : (List<ContextData>) record
						.getContextData()) {
					if (ca.getTarget().getCollectionId().equals(contextAnnId)) {
						ca = contextAnn;
					}
				}
			}
		}
	}

	public static List<ContextData> getContextDataFromJson(
			JsonNode contextAnnsJson) {
		ArrayList<ContextData> ContextData = new ArrayList<ContextData>();
		if (contextAnnsJson != null && contextAnnsJson.isArray()) {
			for (final JsonNode contextAnnJson : contextAnnsJson) {
				JsonNode annTypeJson = contextAnnJson.get("contextDataType");
				if (annTypeJson != null && annTypeJson.isValueNode()) {
					String annTypeString = annTypeJson.asText();
					ContextDataType annType = ContextDataType
							.valueOf(annTypeString);
					if (annType != null) {
						Class<?> clazz;
						try {
							clazz = Class.forName("model.annotations."
									+ annType);
							ContextData contextAnn = (ContextData) Json
									.fromJson(contextAnnJson, clazz);
							ContextData.add(contextAnn);
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return ContextData;
	}

	/**
	 * @param id
	 * @param recordId
	 * @param position
	 * @return
	 */
	public static Result removeRecordFromCollection(String id, String recordId,
			Option<Integer> position) {
		ObjectNode result = Json.newObject();
		Locks locks = null;
		try {
			ObjectId collectionDbId = new ObjectId(id);
			locks = Locks.create().write("Collection #" + collectionDbId)
					.acquire();
			Result response = errorIfNoAccessToCollection(Action.EDIT,
					collectionDbId);
			ObjectId recordDbId = new ObjectId(recordId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				if (!position.isDefined()) {
					result.put(
							"error",
							"The position in the collection from which the record is to be removed, must be specified");
					return badRequest(result);
				} else {
					DB.getRecordResourceDAO().removeFromCollection(recordDbId,
							collectionDbId, position.get());
				}
				// TODO: if position undefined, remove from all Positions. shift
				// other records will be harder in that case though.
				// modify record's access: the record gets the most liberal
				// rights of the collections it belongs to after the removal
				DB.getRecordResourceDAO()
						.updateRecordRightsUponRemovalFromCollection(
								recordDbId, collectionDbId);
				DB.getCollectionObjectDAO().removeCollectionMedia(
						collectionDbId, position.get());
				if (DB.getCollectionObjectDAO().isFavorites(collectionDbId))
					DB.getRecordResourceDAO().decrementLikes(recordDbId);
				else
					DB.getRecordResourceDAO().decField("usage.collected",
							recordDbId);
				// Change the collection metadata as well
				DB.getCollectionObjectDAO().decEntryCount(collectionDbId);
				DB.getCollectionObjectDAO().updateField(collectionDbId,
						"administrative.lastModified", new Date());
				result.put("message",
						"Record succesfully removed from collection");
				return ok(result);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	// TODO: update collection's media
	public static Result moveRecordInCollection(String id, String recordId,
			int oldPosition, int newPosition) {
		ObjectNode result = Json.newObject();
		Locks locks = null;
		try {
			ObjectId collectionDbId = new ObjectId(id);
			locks = Locks.create().write("Collection #" + collectionDbId)
					.acquire();
			ObjectId recordDbId = new ObjectId(recordId);
			Result response = errorIfNoAccessToRecord(Action.EDIT,
					collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				if (oldPosition > newPosition) {
					DB.getRecordResourceDAO().shiftRecordsToRight(
							collectionDbId, newPosition, oldPosition - 1);
				} else if (newPosition > oldPosition) {
					DB.getRecordResourceDAO().shiftRecordsToRight(
							collectionDbId, oldPosition + 1, newPosition - 1);
				}
				DB.getRecordResourceDAO().updatePosition(recordDbId,
						collectionDbId, oldPosition, newPosition);
				DB.getCollectionObjectDAO().updateField(collectionDbId,
						"administrative.lastModified", new Date());
				result.put("message", "Record succesfully added to collection");
				return ok(result);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	/**
	 * @param recordId
	 * @param source
	 * @param sourceId
	 */
	private static void addContentToRecord(ObjectId recordId, String source,
			String sourceId) {
		BiFunction<RecordResource, String, Boolean> methodQuery = (
				RecordResource record, String sourceClassName) -> {
			try {
				Class<?> sourceClass = Class.forName(sourceClassName);
				ISpaceSource s = (ISpaceSource) sourceClass.newInstance();
				RecordResource fullRecord = DB.getRecordResourceDAO().get(
						recordId);
				List<RecordJSONMetadata> recordsData = s.getRecordFromSource(
						sourceId, fullRecord);
				for (RecordJSONMetadata data : recordsData) {
					if (data.getFormat().equals("JSON-WITH")) {
						log.info(data.getJsonContent());
						ObjectMapper mapper = new ObjectMapper();
						JsonNode json = mapper.readTree(data.getJsonContent())
								.get("descriptiveData");
						DescriptiveData descriptiveData = Json.fromJson(json,
								CulturalObjectData.class);
						DB.getWithResourceDAO().updateDescriptiveData(recordId,
								descriptiveData);
						String mediaString = mapper
								.readTree(data.getJsonContent()).get("media")
								.toString();
						List<HashMap<MediaVersion, EmbeddedMediaObject>> media = new ObjectMapper()
								.readValue(
										mediaString,
										new TypeReference<List<HashMap<MediaVersion, EmbeddedMediaObject>>>() {
										});
						DB.getWithResourceDAO().updateEmbeddedMedia(recordId,
								media);
					} else {
						DB.getRecordResourceDAO().updateContent(
								record.getDbId(), data.getFormat(),
								data.getJsonContent());
					}
				}
				return true;
			} catch (Exception e) {
				return false;
			}
		};
		RecordResource record = DB.getRecordResourceDAO().getById(recordId);
		String sourceClassName = "sources." + source + "SpaceSource";
		ParallelAPICall.createPromise(methodQuery, record, sourceClassName);
	}

	/**
	 * @return
	 */
	public static Result addToFavorites() {
		ObjectId userId = new ObjectId(session().get("user"));
		String fav = DB.getCollectionObjectDAO()
				.getByOwnerAndLabel(userId, null, "_favorites").getDbId()
				.toString();
		return addRecordToCollection(fav, Option.None());
	}

	/**
	 * @return
	 */
	public static Result removeFromFavorites(String externalId) {
		ObjectId userId = new ObjectId(session().get("user"));
		String fav = DB.getCollectionObjectDAO()
				.getByOwnerAndLabel(userId, null, "_favorites").getDbId()
				.toString();
		RecordResource record = DB.getRecordResourceDAO().getByExternalId(
				externalId);
		List<CollectionInfo> collected = record.getCollectedIn();
		for (CollectionInfo c : collected) {
			if (c.getCollectionId().toString().equals(fav)) {
				return removeRecordFromCollection(fav, record.getDbId()
						.toString(), Option.Some(c.getPosition()));
			}
		}
		return removeRecordFromCollection(fav, record.getDbId().toString(),
				Option.None());
	}

}
