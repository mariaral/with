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


package db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.Literal.Language;
import model.basicDataTypes.WithAccess.Access;
import model.resources.RecordResource;
import model.resources.WithResource;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;

import ch.qos.logback.core.util.AggregationType;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import utils.Tuple;

/*
 * The class consists of methods that can be both query
 * a CollectionObject or a RecordResource_ (CollectionObject,
 * CulturalObject, WithResource etc).
 *
 * Special methods referring to one of these entities go to the
 * specific DAO class.
 */
public abstract class CommonResourceDAO<T> extends DAO<T>{

	public CommonResourceDAO() {
		super(WithResource.class);
	}

	/*
	 * The value of the entity class is either
	 * CollectionObject.class or RecordResource.class
	 */
	public CommonResourceDAO(Class<?> entityClass) {
		super(entityClass);
	}

	/**
	 * Retrieve an Object from DB using its dbId
	 * @param id
	 * @return
	 */
	public T getById(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id);
		return this.findOne(q);
	}

	/**
	 * Get a CollectionObject by the dbId and retrieve
	 * only a bunch of fields from the whole document
	 * @param id
	 * @param retrievedFields
	 * @return
	 */
	public T getById(ObjectId id, List<String> retrievedFields) {
		Query<T> q = this.createQuery().field("_id").equal(id);
		q.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.findOne(q);

	}

	/**
	 * Remove a CollectionObject and all collected resources using the dbId
	 * @param id
	 * @return
	 */
	public int removeById(ObjectId id) {
		/*
		 * 0 - no documents returned
		 * * - number of documents returned
		 */
		return this.deleteById(id).getN();
	}

	public Query<T> createColIdElemMatchQuery(ObjectId colId) {
		Query<T> q = this.createQuery();
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		q.filter("collectedIn", elemMatch1);
		return q;
	}

	/**
	 * Return all resources that belong to a 'collection' throwing
	 * away duplicate entries in a 'collection'
	 * This methods is here cause in the future may a collection
	 * belong to a another collection.
	 *
	 *
	 * TODO: Return only some fields for these resources.
	 *
	 * @param colId
	 * @param offset
	 * @param count
	 * @return
	 */
	public List<T> getSingletonCollectedResources(ObjectId colId, int offset, int count) {
		//Query<T> q = this.createQuery().field("collectedIn.collectionId").equal(colId).offset(offset).limit(count);
		return this.find(createColIdElemMatchQuery(colId).offset(offset).limit(count)).asList();
	}

	/**
	 * Retrieve all records from specific collection checking
	 * out for duplicates and restore them.
	 *
	 * @param colId
	 * @return
	 */
	public List<RecordResource> getByCollection(ObjectId colId) {
		int MAX = 10000;
		return getByCollectionBtwnPositions(colId, 0, MAX);
	}

	/**
	 * Retrieve records from specific collection using position
	 * which is between lowerBound and upperBound
	 *
	 * @param colId, lowrBound, upperBound
	 * @return
	 */
	public List<RecordResource> getByCollectionBtwnPositions(ObjectId colId, int lowerBound, int upperBound) {
		Query<T> q = this.createQuery();
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject elemMatch2 = new BasicDBObject();
		BasicDBObject geq = new BasicDBObject();
		geq.put("$gte", lowerBound);
		geq.append("$lt", upperBound);
		colIdQuery.append("position", geq);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		q.filter("collectedIn", elemMatch1);
		List<RecordResource> resources  = (List<RecordResource>) this.find(q).asList();
		/*DBCursor cursor = this.getDs().getCollection(entityClass).find(query);
		List<T> ds = new ArrayList<T>();
		while (cursor.hasNext()) {
		   DBObject o = cursor.next();
		   T d = (T) DB.getMorphia().fromDBObject(entityClass, o);
		   ds.add(d);
		}*/
		List<RecordResource> repeatedResources = new ArrayList<RecordResource>(upperBound-lowerBound);
		for (int i=0; i<(upperBound - lowerBound); i++) {
			repeatedResources.add(new RecordResource());
		}
		int maxPosition = -1;
		for (RecordResource d: resources) {
			ArrayList<CollectionInfo> collectionInfos = (ArrayList<CollectionInfo>) d.getCollectedIn();
			//May be a long iteration, if a record belongs to many collections
			for (CollectionInfo ci: collectionInfos) {
				ObjectId collectionId = ci.getCollectionId();
				if (collectionId.equals(colId)) {
					int pos = ci.getPosition();
					if ((lowerBound <= pos) && (pos < upperBound)) {
						int arrayPosition = pos - lowerBound;
						if (arrayPosition > maxPosition)
							maxPosition = arrayPosition;
						repeatedResources.add(arrayPosition, d);
					}
				}
			}
		}
		if (maxPosition > -1)
			return repeatedResources.subList(0, maxPosition+1);
		else
			return new ArrayList<RecordResource>();
	}

	/**
	 * Given a list of ObjectId's (dbId's)
	 * return the specified  resources
	 * @param ids
	 * @return
	 */
	public List<T> getByIds(List<ObjectId> ids) {
		Query<T> colQuery = this.createQuery().field("_id")
				.hasAnyOf(ids);
		return find(colQuery).asList();
	}

	/**
	 * List all CollectionObjects with the title provided for the language specified
	 * @param title
	 * @return
	 */
	public List<T> getByLabel(String lang, String title) {
		if (lang == null) lang = "default";
		Query<T> q = this.createQuery().field("descriptiveData.label." + lang)
				.equal(title);
		return this.find(q).asList();
	}

	public List<T> getByLabel(Language lang, String title) {
		Query<T> q = this.createQuery().field("descriptiveData.label." + lang.toString()).equal(title);
		return this.find(q).asList();
	}

	/**
	 * Get a user's CollectionObject according to the title given
	 * @param creatorId
	 * @param title
	 * @return
	 */
	public T getByOwnerAndLabel(ObjectId creatorId, String lang, String title) {
		if(lang == null) lang = "en";
		Query<T> q = this.createQuery().field("administrative.withCreator")
				.equal(creatorId).field("descriptiveData.label." + lang).equal(title);
		return this.findOne(q);
	}


	/**
	 * Get all CollectionObject using the creator's/owner's id.
	 * @param creatorId
	 * @param offset
	 * @param count
	 * @return
	 */
	public List<T> getByOwner(ObjectId creatorId, int offset, int count) {
		Query<T> q = this.createQuery().field("administrative.withCreator")
				.equal(creatorId).offset(offset).limit(count);
		return this.find(q).asList();
	}

	/**
	 * Get the first CollectionObject that a user has created
	 * using the creator's/owner's id.
	 * We are using MongoDB's paging.
	 * @param id
	 * @return
	 */
	public List<T> getFirstResourceByOwner(ObjectId id) {
		return getByOwner(id, 0, 1);
	}

	/**
	 * Retrieve the owner/creator of a Resource
	 * using collection's dbId
	 * @param id
	 * @return
	 */
	public User getOwner(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id)
				.retrievedFields(true, "administrative.withCreator");
		return ((WithResource) findOne(q)).retrieveCreator();
	}

	/**
	 * Retrieve a resource using the source that provided it
	 * @param sourceName
	 * @return
	 */
	public List<T> getByProvider(String sourceName) {

		//TODO: faster if could query on last entry of provenance array. Mongo query!
		/*
		 * We can sort the array in inverted order so to query
		 * only the first element of this array directly!
		 */
		Query<T> q = this.createQuery();
		//q.field("provenance").hasThisElement(q.field("provider").equals(sourceName));
		BasicDBObject provQuery = new BasicDBObject();
		provQuery.put("provider", sourceName);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("$elemMatch", provQuery);
		q.filter("provenance", elemMatch);
		return this.find(q).asList();
	}

	/**
	 * Return the number of resources that belong to a source
	 * @param sourceId
	 * @return
	 */
	public long countBySource(String sourceId) {
		//TODO: faster if could query on last entry of provenance array. Mongo query!
		Query<T> q = this.createQuery().disableValidation();
		//q.field("provenance").hasThisElement(q.field("provider").equals(sourceId));
		return this.find(q).countAll();
	}

	/**
	 * Create a Mongo access query criteria
	 * @param userAccess
	 * @return
	 */
	private Criteria formAccessLevelQuery(Tuple<ObjectId, Access> userAccess) {
		int ordinal = userAccess.y.ordinal();
		/*Criteria[] criteria = new Criteria[Access.values().length-ordinal];
		for (int i=0; i<Access.values().length-ordinal; i++)
			criteria[i] = this.createQuery().criteria("rights." + userAccess.x.toHexString())
			.equal(Access.values()[i+ordinal].toString());*/
		return this.createQuery().criteria("administrative.access." + userAccess.x.toHexString()).greaterThanOrEq(ordinal);
	}

	/**
	 * Create Mongo access criteria for the current logged in user
	 * @param loggedInUserEffIds
	 * @return
	 */
	private CriteriaContainer formLoggedInUserQuery(List<ObjectId> loggedInUserEffIds) {
		int ordinal = Access.READ.ordinal();
		Criteria[] criteria = new Criteria[loggedInUserEffIds.size()+1];
		for (int i=0; i<loggedInUserEffIds.size(); i++) {
			criteria[i] = this.createQuery().criteria("rights." + loggedInUserEffIds.get(i)).greaterThanOrEq(ordinal);
		}
		criteria[loggedInUserEffIds.size()] = this.createQuery().criteria("rights.isPublic").equal(true);
		return this.createQuery().or(criteria);
	}

	/**
	 * Create general Mongo access criteria for users-access level specified
	 * @param filterByUserAccess
	 * @return
	 */
	private CriteriaContainer formQueryAccessCriteria(List<Tuple<ObjectId, Access>> filterByUserAccess) {
		Criteria[] criteria = new Criteria[0];
		for (Tuple<ObjectId, Access> userAccess: filterByUserAccess) {
			criteria = ArrayUtils.addAll(criteria, formAccessLevelQuery(userAccess));
		}
		return this.createQuery().or(criteria);
	}

	/**
	 * Create a basic Mongo query with withCreator field matching, offset, limit and criteria.
	 * @param criteria
	 * @param creator
	 * @param isExhibition
	 * @param offset
	 * @param count
	 * @return
	 */
	private Query<T> formBasicQuery(CriteriaContainer[] criteria, ObjectId creator, Boolean isExhibition,  int offset, int count) {
		Query<T> q = this.createQuery().offset(offset).limit(count+1);
		if (creator != null)
			q.field("administrative.withCreator").equal(creator);
		if (criteria.length > 0)
			q.and(criteria);
		return q;
	}

	/**
	 * Return a tuple containing a list of CollectionObjects (usually bounded from a limit)
	 * together with the total number of entities corresponded to the query.
	 * @param q
	 * @param isExhibition
	 * @return
	 */
	public Tuple<List<T>, Tuple<Integer, Integer>> getResourcesWithCount(Query<T> q,
			Boolean isExhibition) {

		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		QueryResults<T> result;
		List<T> collections = new ArrayList<T>();
		if (isExhibition == null) {
			result = this.find(q);
			collections = result.asList();
			Query<T> q2 = q.cloneQuery();
			q2.field("administrative.isExhibition").equal(true);
			q.field("administrative.isExhibition").equal(false);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			q.field("administrative.isExhibition").equal(isExhibition);
			result = this.find(q);
			collections = result.asList();
			if (isExhibition)
				hits.y = (int) result.countAll();
			else
				hits.x = (int) result.countAll();
		}
		return new Tuple<List<T>, Tuple<Integer, Integer>>(collections, hits);
	}

	/**
	 * Return the total number of CollectionObject entities for a specific query
	 * @param q
	 * @param isExhibition
	 * @return
	 */
	public Tuple<Integer, Integer> getResourceCount(Query<T> q, Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		if (isExhibition == null) {
			Query<T> q2 = q.cloneQuery();
			q2.field("administrative.isExhibition").equal(true);
			q.field("administrative.isExhibition").equal(false);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			q.field("administrative.isExhibition").equal(isExhibition);
			if (isExhibition)
				hits.y = (int) this.find(q).countAll();
			else
				hits.x = (int)  this.find(q).countAll();
		}
		return hits;
	}

	/**
	 * Return all CollectionObjects (usually bounded by a limit) some user access criteria.
	 * The method can be parametrised to return also the total number of entities for the specified query.
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<T>, Tuple<Integer, Integer>>  getByACL(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(orAccessed));
		}
		Query<T> q = formBasicQuery(criteria, creator, isExhibition, offset, count);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("administrative.isExhibition").equal(isExhibition);
			return new Tuple<List<T>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	/**
	 * Return all CollectionObjects (usually bounded by a limit) that satisfy the loggin user's access
	 * criteria and optionally some other user access criteria. Typically all the CollectionObject that a user has access.
	 * The method can be parametrised to return also the total number of entities for the specified query.
	 * @param loggeInEffIds
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<T>, Tuple<Integer, Integer>>  getUsersAccessibleWithACL(List<ObjectId> loggeInEffIds,
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {

		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		criteria = ArrayUtils.addAll(criteria, formLoggedInUserQuery(loggeInEffIds));
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(orAccessed));
		}
		Query<T> q = formBasicQuery(criteria, creator, isExhibition, offset, count);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("administrative.isExhibition").equal(isExhibition);
			return new Tuple<List<T>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	/**
	 * Return all CollectionObjects (usually bounded by a limit) of a user that satisfy some user
	 * access criteria (that are shared with some users).
	 * The method can be parametrised to return also the total number of entities for the specified query.
	 * @param userId
	 * @param accessedByUserOrGroup
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<T>, Tuple<Integer, Integer>> getSharedWithACL(ObjectId userId, List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {

		Query<T> q = this.createQuery().offset(offset).limit(count+1);
		q.field("administrative.withCreator").notEqual(userId);
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,formQueryAccessCriteria(orAccessed));
		}
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("administrative.isExhibition").equal(isExhibition);
			return new Tuple<List<T>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	/**
	 * Return all public CollectionObjects (usually bounded by a limit) that also satisfy some user access criteria.
	 * The method can be parametrised to return also the total number of entities for the specified query.
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<T>, Tuple<Integer, Integer>> getPublicWithACL(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {

		Query<T> q = this.createQuery().offset(offset).limit(count+1);
		if (creator != null)
			q.field("administrative.withCreator").equal(creator);
		Criteria[] criteria = {this.createQuery().criteria("administrative.access.isPublic").equal(true)};
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,formQueryAccessCriteria(orAccessed));
		}
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<T>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	/**
	 * Return the total number of likes for a resource.
	 * @param id
	 * @return
	 */
	public int getTotalLikes(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id)
				.retrievedFields(true, "usage.likes");
		return ((WithResource) this.findOne(q)).getUsage().getLikes();
	}




	/**
	 * ??????? do we have external Ids ??????
	 * @param extId
	 * @return
	 */
	public List<T> getByExternalId(String extId) {
		Query<T> q = this.createQuery().field("administrative.externalId")
				.equal(extId);
		return this.find(q).asList();
	}


	/**
	 * ??????? do we have external Ids ??????
	 * @param extId
	 * @return
	 */
	public long countByExternalId(String extId) {
		Query<T> q = this.createQuery()
				.field("externalId").equal(extId);
		return this.find(q).countAll();
	}

	/**
	 * Not a good Idea problably want work
	 * @param resourceId
	 * @param colId
	 * @param position
	 */
	public boolean removeFromCollection(ObjectId dbId, ObjectId colId, int position) {
		return false;
	}

	//TODO:Mongo query!
	/**
	 * Also wrong implementation
	 * @param colId
	 * @param position
	 */
	public void shiftRecordsToLeft(ObjectId colId, int position) {
		String colField = "collectedIn."+colId;
		Query<T> q = this.createQuery().field(colField).exists();
	    UpdateOperations<T> updateOps = this.createUpdateOperations();
		BasicDBObject geq = new BasicDBObject();
		geq.put("$gte", position);
		BasicDBObject geq1 = new BasicDBObject();
		geq1.put("$elemMatch", geq);
		q.filter(colField, geq1);
		List<WithResource> resources  = (List<WithResource>) this.find(q).asList();
		for (WithResource resource: resources) {
			/*List<CollectionInfo> collectedIn = resource.getCollectedIn();
			ArrayList<Integer> positions = collectedIn.get(colId);
			int index = 0;
			for (Integer pos: positions) {
				if (pos >= position) {
					updateOps.disableValidation().dec(colField+"."+index);
				}
				index+=1;
			}*/
		}
		this.update(q, updateOps);
		/*attempts to update without retrieving the documents: does not work
		/*if collectedIn is of type Map
		 * update only works on first matching element, so discard
		BasicDBObject colIdQuery = new BasicDBObject();
		BasicDBObject existsField = new BasicDBObject();
		existsField.put("$exists", true);
		colIdQuery.put(colField, existsField);
		BasicDBObject geq = new BasicDBObject();
		geq.put("$gte", position);
		BasicDBObject geq1 = new BasicDBObject();
		geq1.put("$elemMatch", geq);
		colIdQuery.append(colField, geq1);
		//System.out.println(colIdQuery);
		BasicDBObject update = new BasicDBObject();
		BasicDBObject entrySpec = new BasicDBObject();
		entrySpec.put(colField+".0", -1);
		update.put("$inc", entrySpec);
		this.getDs().getCollection(entityClass).update(colIdQuery, update, false, true);
		*/
		/* if collectedIn is of type Array<CollectionInfo>
		 * BasicDBObject query = new BasicDBObject();
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject geq = new BasicDBObject();
		geq.put("$gte", position);
		colIdQuery.append("position", geq);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("$elemMatch", colIdQuery);
		query.put("collectedIn", elemMatch);
		System.out.println(query);
		BasicDBObject update = new BasicDBObject();
		BasicDBObject entrySpec = new BasicDBObject();
		entrySpec.put("collectedIn.$.position", -1);
		update.put("$inc", entrySpec);
		System.out.println(this.getDs().getCollection(entityClass).find(query).count());
		this.getDs().getCollection(entityClass).update(query, update, false, true);*/
	}

	/**
>>>>>>> changing of structures and indexing off mongo fields
	 * This method is to update the 'public' field on all the records of a
	 * collection. By default update method is invoked to all documents of a
	 * collection.
	 *
	 **/
	public void setFieldValueOfCollectedResource(ObjectId colId, String fieldName,
			String value) {
		Query<T> q = createColIdElemMatchQuery(colId);
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		updateOps.set(fieldName, value);
		this.update(q, updateOps);
	}

	public void updateContent(ObjectId recId, String format, String content) {
		Query<T> q = this.createQuery().field("_id").equal(recId);
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.set("content."+format, content);
		this.update(q, updateOps);
	}

	/**
	 * Increment likes for this specific resource
	 * @param externalId
	 */
	public void incrementLikes(ObjectId dbId) {
		incField("usage.likes", dbId);
	}

	/**
	 * Decrement likes for this specific resource
	 * @param dbId
	 */
	public void decrementLikes(ObjectId dbId) {
		decField("usage.likes", dbId);
	}

	/**
	 * Increment the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void incField( String fieldName, ObjectId dbId) {
		Query<T> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		updateOps.inc(fieldName);
		this.update(q, updateOps);
	}

	/**
	 * Decrement the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void decField(String fieldName, ObjectId dbId) {
		Query<T> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		updateOps.dec(fieldName);
		this.update(q, updateOps);
	}
}
