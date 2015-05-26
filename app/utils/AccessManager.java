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


package utils;

import java.util.List;
import java.util.Map;

import model.User.Access;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;

public class AccessManager {
	public static final ALogger log = Logger.of(AccessManager.class);

	public static enum Action {
		READ, EDIT, DELETE
	};

	public static boolean checkAccess(Map<ObjectId, Access> rights,
			List<String> userIds, Action action) {
		for (String id : userIds) {
			if (rights.containsKey(new ObjectId(id))
					&& (rights.get(new ObjectId(id)).ordinal() > action
							.ordinal())) {
				return true;
			}
		}
		return false;
	}

	public static Access getMaxAccess(Map<ObjectId, Access> rights,
			List<String> userIds) {
		Access maxAccess = Access.NONE;
		for (String id : userIds) {
			if (rights.containsKey(new ObjectId(id))) {
				Access access = rights.get(new ObjectId(id));
				if (access.ordinal() > maxAccess.ordinal()) {
					maxAccess = access;
				}
			}
		}
		return maxAccess;
	}

}
