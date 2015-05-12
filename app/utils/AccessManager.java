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
import java.util.Map.Entry;

import model.User.Access;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;

public class AccessManager {
	public static final ALogger log = Logger.of( AccessManager.class);

	public static boolean checkAccess(Map<ObjectId, Access> rights, List<ObjectId> ids) {
		for(ObjectId id: ids) {
			if(rights.containsKey(id)) {
				return true;
			}
		}
		return false;
	}

	public static void addRight(Map<ObjectId, Access> rights, Map<ObjectId, Access> rightsToGive) {
		rights.putAll(rightsToGive);
	}

	public static void removeRight(Map<ObjectId, Access> rights, Map<ObjectId, Access> rightToGo) {
		for(Entry<ObjectId, Access> e: rightToGo.entrySet())
			rights.remove(e.getKey(), e.getValue());
	}

}