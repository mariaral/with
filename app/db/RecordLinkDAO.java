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

import model.Media;
import model.RecordLink;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import play.Logger;

public class RecordLinkDAO extends DAO<RecordLink> {
	static private final Logger.ALogger log = Logger.of(RecordLink.class);

	public RecordLinkDAO() {
		super( RecordLink.class );
	}

	public RecordLink getByDbId(String dbId) {
		Query<RecordLink> q = this.createQuery()
				.field("_id").equal(new ObjectId(dbId));
		return this.findOne(q);
	}

	public String getTitle(String dbId) {
		return getByDbId(dbId).getTitle();
	}

	public String getDescription(String dbId) {
		return getByDbId(dbId).getDescription();
	}

	public String getSource(String dbId) {
		return getByDbId(dbId).getSource();
	}

	public String getSourceId(String dbId) {
		return getByDbId(dbId).getSourceId();
	}

	public String getSourceUrl(String dbId) {
		return getByDbId(dbId).getSourceUrl();
	}

	public String getThumbnailUrl(String dbId) {
		return getByDbId(dbId).getThumbnailUrl();
	}

	public void blabla(Media media) {

	}
}
