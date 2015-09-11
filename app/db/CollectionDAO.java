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

import java.util.List;

import model.Collection;
import model.Rights.Access;
import model.User;

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import play.Logger;
import play.Logger.ALogger;
import utils.Tuple;

public class CollectionDAO extends DAO<Collection> {
	public static final ALogger log = Logger.of(CollectionDAO.class);

	public CollectionDAO() {
		super(Collection.class);
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

	public Collection getByOwnerAndTitle(ObjectId ownerId, String title) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).field("title").equal(title);
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

	public List<Collection> getByOwner(ObjectId ownerId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).field("isExhibition").equal(false)
				.offset(offset).limit(count);
		return this.find(q).asList();
	}
	
	public Criteria[] formQueryAccessCriteria(Tuple<ObjectId, Access> userAccess) {
		Criteria[] criteria = new Criteria[userAccess.y.ordinal()];
		int ordinal = userAccess.y.ordinal();
		for (int i=0; i < ordinal; i++)
			criteria[i] = this.createQuery().criteria("rights." + userAccess.x.toHexString())
			.equal(userAccess.y.toString());
		return criteria;
	}
	
	
	public Criteria[] formQueryAccessCriteria(List<Tuple<ObjectId, Access>> filterByUserAccess) {
		Criteria[] criteria = new Criteria[0];
		for (Tuple<ObjectId, Access> userAccess: filterByUserAccess) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(userAccess));
		}
		return criteria;
	}
	
	public List<Collection> getByAccess(ObjectId userId, List<Tuple<ObjectId, Access>> filterByUserName, 
			List<Tuple<ObjectId, Access>> filterByUserGroup, Boolean isExhibition, int offset, int count) {
		Query<Collection> q = this.createQuery().offset(offset).limit(count);
		if (isExhibition != null)
			q.field("isExhibition").equal(isExhibition);
		Criteria[] criteria = formQueryAccessCriteria(filterByUserName);
		//TODO: check whether userGroup Ids are treated the same as userIds
		criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(filterByUserGroup));
	    q.or(criteria);
		return this.find(q).asList();
	}
	
	public List<Collection> getPublic(ObjectId userId, List<Tuple<ObjectId, Access>> filterByUserName, 
			List<Tuple<ObjectId, Access>> filterByUserGroup, Boolean isExhibition, int offset, int count) {
		Query<Collection> q = this.createQuery().offset(offset).limit(count);
		if (isExhibition != null)
			q.field("isExhibition").equal(isExhibition);
		Criteria[] criteria = {this.createQuery().criteria("isPublic").equal(true)};
	    criteria = ArrayUtils.addAll(criteria ,formQueryAccessCriteria(filterByUserName));
	    q.or(criteria);
		return this.find(q).asList();
	}
	
	public List<Collection> getPublic(ObjectId userId, List<Tuple<ObjectId, Access>> filterByUserName, 
			List<Tuple<ObjectId, Access>> filterByUserGroup,  int offset, int count) {		
		return getPublic(userId, filterByUserName, filterByUserGroup, null, offset, count);
	}

	/*
	public List<Collection> getByReadAccess(ObjectId userId, int offset,
			int count) {
		Query<Collection> q = this.createQuery().field("isExhibition")
				.equal(false).offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("OWN"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("READ"),
				this.createQuery().criteria("isPublic").equal(true) };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getByWriteAccess(ObjectId userId, int offset,
			int count) {
		Query<Collection> q = this.createQuery().field("isExhibition")
				.equal(false).offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("OWN"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE") };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getByReadAccessFiltered(ObjectId userId,
			ObjectId ownerId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).field("isExhibition").equal(false)
				.offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("OWN"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("READ"),
				this.createQuery().criteria("isPublic").equal(true) };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getByWriteAccessFiltered(ObjectId userId,
			ObjectId ownerId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).field("isExhibition").equal(false)
				.offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("OWN"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE") };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getSharedFiltered(ObjectId userId,
			ObjectId ownerId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).field("isExhibition").equal(false)
				.offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("READ").criteria("isPublic").equal(false) };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getShared(ObjectId userId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.notEqual(userId).field("isExhibition").equal(false)
				.offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("READ").criteria("isPublic").equal(false) };
		q.or(critiria);
		return this.find(q).asList();
	}
/*
	public List<Collection> getPublicFiltered(ObjectId ownerId, int offset,
			int count) {
		Query<Collection> q = this.createQuery().field("isPublic").equal(true)
				.field("isExhibition").equal(false).field("ownerId")
				.equal(ownerId).offset(offset).limit(count);
		return this.find(q).asList();
	}
*/
	public List<Collection> getPublic(int offset, int count) {
		Query<Collection> q = this.createQuery().field("isPublic").equal(true)
				.field("isExhibition").equal(false);
		return this.find(q).asList();
	}

	public User getCollectionOwner(ObjectId id) {
		Query<Collection> q = this.createQuery().field("_id").equal(id)
				.retrievedFields(true, "ownerId");
		return findOne(q).retrieveOwner();
	}
/*
	public List<Collection> getExhibitionsByOwner(ObjectId ownerId, int offset,
			int count) {
		Query<Collection> q = this.createQuery().field("isExhibition")
				.equal(true).field("ownerId").equal(ownerId).offset(offset)
				.limit(count);
		return this.find(q).asList();
	}
*/
	public int removeById(ObjectId id) {

		Collection c = get(id);

		/*
		 * User owner = c.retrieveOwner(); for (ObjectId colId :
		 * owner.getCollectionIds()) { if (colId.equals(id)) {
		 * owner.getCollectionIds().remove(colId);
		 * DB.getUserDAO().makePermanent(owner); break; } }
		 */

		DB.getCollectionRecordDAO().deleteByCollection(id);
		return makeTransient(c);
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
