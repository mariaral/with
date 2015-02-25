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


package model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

@Entity
public class CollectionEntry {
	@Id
	private ObjectId dbID;

	@Reference
	private Collection collection;
	@Embedded
	private RecordLink recordLink;

	// the place in the collection of this record,
	// mostly irrelevant I would think ..
	private int position;

	public ObjectId getDbID() {
		return dbID;
	}

	public void setDbID(ObjectId dbID) {
		this.dbID = dbID;
	}

	public Collection getCollection() {
		return collection;
	}

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

	public RecordLink getRecordLink() {
		return recordLink;
	}

	public void setRecordLink(RecordLink recordLink) {
		this.recordLink = recordLink;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}


}
