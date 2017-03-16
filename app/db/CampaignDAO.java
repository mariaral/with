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
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import model.Campaign;
import model.resources.collection.CollectionObject;


public class CampaignDAO extends DAO<Campaign> {

	public CampaignDAO() {
		super(Campaign.class);
	}
	
	public List<Campaign> getCampaigns(ObjectId groupId, boolean active, int offset, int count) {
		
		Query<Campaign> q = this.createQuery();
		
		if (groupId != null) {
			q = q.field("space").equal(groupId);
		}
		
		Date today = new Date();
		if (active) {
			q = q.field("startDate").lessThanOrEq(today).field("endDate").greaterThanOrEq(today);
		}
		else {
			q.or(
				q.criteria("startDate").greaterThanOrEq(today),
				q.criteria("endDate").lessThanOrEq(today)
			);
		}

		q = q.offset(offset).limit(count);
		
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = this.find(q).asList();

		return campaigns;
	}
	
	public long getCampaignsCount(ObjectId groupId) {
		
		Query<Campaign> q = this.createQuery();
		
		if (groupId != null) {
			q = q.field("space").equal(groupId);
		}
		return q.countAll();
	}
		
	public void incUserPoints(ObjectId campaignId, String userid, String annotType) {
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		UpdateOperations<Campaign> updateOps = this
				.createUpdateOperations().disableValidation();
		updateOps.inc("contributorsPoints."+userid+"."+annotType);
		
		this.update(q, updateOps);
	}
	
	public void decUserPoints(ObjectId campaignId, String userid, String annotType) {
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		UpdateOperations<Campaign> updateOps = this
				.createUpdateOperations().disableValidation();
		updateOps.dec("contributorsPoints."+userid+"."+annotType);
		
		this.update(q, updateOps);
	}
	
}
