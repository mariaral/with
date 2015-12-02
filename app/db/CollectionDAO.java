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
import java.util.List;

import model.Collection;
import model.basicDataTypes.WithAccess.Access;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;

import com.mongodb.Cursor;

import play.Logger;
import play.Logger.ALogger;
import utils.Tuple;

public class CollectionDAO extends DAO<Collection> {
	public static final ALogger log = Logger.of(CollectionDAO.class);

	public CollectionDAO() {
		super(Collection.class);
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

	public List<Collection> getCollectionsByIds(List<ObjectId> ids) {
		Query<Collection> colQuery = this.createQuery().field("_id")
				.hasAnyOf(ids);
		return find(colQuery).asList();
	}

	public List<Collection> getByTitle(String title) {
		Query<Collection> q = this.createQuery().field("title")
				.equal("_favorites");
		return this.find(q).asList();
	}

	public List<Collection> getAll(int offset, int count) {
		Query<Collection> q = this.createQuery().offset(offset).limit(count);
		return this.find(q).asList();
	}

	public Collection getByOwnerAndTitle(ObjectId creatorId, String title) {
		Query<Collection> q = this.createQuery().field("creatorId")
				.equal(creatorId).field("title").equal(title);
		return this.findOne(q);
	}

	public Collection getById(ObjectId id) {
		Query<Collection> q = this.createQuery().field("_id").equal(id);
		return findOne(q);
	}

	public Collection getById(ObjectId id, List<String> retrievedFields) {
		Query<Collection> q = this.createQuery().field("_id").equal(id);
		if (retrievedFields != null)
			for (int i = 0; i < retrievedFields.size(); i++)
				q.retrievedFields(true, retrievedFields.get(i));
		return this.findOne(q);

	}

	public List<Collection> getByOwner(ObjectId id) {
		return getByOwner(id, 0, 1);
	}

	public List<Collection> getByOwner(ObjectId creatorId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("creatorId")
				.equal(creatorId).field("isExhibition").equal(false)
				.offset(offset).limit(count);
		return this.find(q).asList();
	}

	//userId has userAccess if its accessLevel in rights is equal to or greater than userAccess
	public Criteria formAccessLevelQuery(Tuple<ObjectId, Access> userAccess) {
		int ordinal = userAccess.y.ordinal();
		/*Criteria[] criteria = new Criteria[Access.values().length-ordinal];
		for (int i=0; i<Access.values().length-ordinal; i++)
			criteria[i] = this.createQuery().criteria("rights." + userAccess.x.toHexString())
			.equal(Access.values()[i+ordinal].toString());*/
		return this.createQuery().criteria("rights." + userAccess.x.toHexString()).greaterThanOrEq(ordinal);
	}

	public CriteriaContainer formLoggedInUserQuery(List<ObjectId> loggedInUserEffIds) {
		int ordinal = Access.READ.ordinal();
		Criteria[] criteria = new Criteria[loggedInUserEffIds.size()+1];
		for (int i=0; i<loggedInUserEffIds.size(); i++) {
			criteria[i] = this.createQuery().criteria("rights." + loggedInUserEffIds.get(i)).greaterThanOrEq(ordinal);
		}
		criteria[loggedInUserEffIds.size()] = this.createQuery().criteria("rights.isPublic").equal(true);
		return this.createQuery().or(criteria);
	}

	public CriteriaContainer formQueryAccessCriteria(List<Tuple<ObjectId, Access>> filterByUserAccess) {
		Criteria[] criteria = new Criteria[0];
		for (Tuple<ObjectId, Access> userAccess: filterByUserAccess) {
			criteria = ArrayUtils.addAll(criteria, formAccessLevelQuery(userAccess));
		}
		return this.createQuery().or(criteria);
	}

	public Tuple<List<Collection>, Tuple<Integer, Integer>> getCollectionsAndHits(Query<Collection> q,
			Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		QueryResults<Collection> result;
		List<Collection> collections = new ArrayList<Collection>();
		if (isExhibition == null) {
			result = this.find(q);
			collections = result.asList();
			Query<Collection> q2 = q.cloneQuery();
			q2.field("isExhibition").equal(true);
			q.field("isExhibition").equal(false);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			q.field("isExhibition").equal(isExhibition);
			result = this.find(q);
			collections = result.asList();
			if (isExhibition)
				hits.y = (int) result.countAll();
			else
				hits.x = (int) result.countAll();
		}
		return new Tuple<List<Collection>, Tuple<Integer, Integer>>(collections, hits);
	}

	public Tuple<Integer, Integer> getHits(Query<Collection> q, Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		if (isExhibition == null) {
			Query<Collection> q2 = q.cloneQuery();
			q2.field("isExhibition").equal(true);
			q.field("isExhibition").equal(false);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			q.field("isExhibition").equal(isExhibition);
			if (isExhibition)
				hits.y = (int) this.find(q).countAll();
			else
				hits.x = (int)  this.find(q).countAll();
		}
		return hits;
	}

	public Query<Collection> formBasicQuery(CriteriaContainer[] criteria, ObjectId creator, Boolean isExhibition,  int offset, int count) {
		Query<Collection> q = this.createQuery().offset(offset).limit(count+1);
		if (creator != null)
			q.field("creatorId").equal(creator);
		if (criteria.length > 0)
			q.and(criteria);
		return q;
	}

	public Tuple<List<Collection>, Tuple<Integer, Integer>>  getByAccess(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(orAccessed));
		}
		Query<Collection> q = formBasicQuery(criteria, creator, isExhibition, offset, count);
		if (totalHits) {
			return getCollectionsAndHits(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<Collection>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	public Tuple<List<Collection>, Tuple<Integer, Integer>>  getByAccess(
			List<ObjectId> loggeInEffIds, List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		criteria = ArrayUtils.addAll(criteria, formLoggedInUserQuery(loggeInEffIds));
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(orAccessed));
		}
		Query<Collection> q = formBasicQuery(criteria, creator, isExhibition, offset, count);
		if (totalHits) {
			return getCollectionsAndHits(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<Collection>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	public Tuple<List<Collection>, Tuple<Integer, Integer>> getShared(ObjectId userId, List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {
		Query<Collection> q = this.createQuery().offset(offset).limit(count+1);
		q.field("creatorId").notEqual(userId);
		/*if (isExhibition != null)
			q.field("isExhibition").equal(isExhibition);*/
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,formQueryAccessCriteria(orAccessed));
		}
		//criteria =ArrayUtils.add(criteria, this.createQuery().criteria("rights." + userId.toHexString()).notEqual(3));
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getCollectionsAndHits(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<Collection>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	public Tuple<List<Collection>, Tuple<Integer, Integer>> getPublic(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {
		Query<Collection> q = this.createQuery().offset(offset).limit(count+1);
		/*if (isExhibition != null)
			q.field("isExhibition").equal(isExhibition);*/
		if (creator != null)
			q.field("creatorId").equal(creator);
		Criteria[] criteria = {this.createQuery().criteria("rights.isPublic").equal(true)};
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,formQueryAccessCriteria(orAccessed));
		}
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getCollectionsAndHits(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<Collection>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	public User getCollectionOwner(ObjectId id) {
		Query<Collection> q = this.createQuery().field("_id").equal(id)
				.retrievedFields(true, "creatorId");
		return findOne(q).retrieveCreator();
	}

	/**
	 * This method is updating one specific User. By default update method is
	 * invoked to all documents of a collection.
	 **/
	public void setSpecificCollectionField(ObjectId dbId, String fieldName,
			String value) {
		Query<Collection> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<Collection> updateOps = this.createUpdateOperations();
		updateOps.set(fieldName, value);
		this.update(q, updateOps);
	}

	public void incItemCount(ObjectId dbId, String fieldName) {
		Query<Collection> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<Collection> updateOps = this.createUpdateOperations();
		updateOps.inc(fieldName);
		this.update(q, updateOps);
	}

	public void decItemCount(ObjectId dbId, String fieldName) {
		Query<Collection> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<Collection> updateOps = this.createUpdateOperations();
		updateOps.dec(fieldName);
		this.update(q, updateOps);
	}
}
