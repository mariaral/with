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

import java.util.HashMap;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.ElasticUpdater;
import model.Collection;
import model.Notification;
import model.Notification.Activity;
import model.basicDataTypes.WithAccess.Access;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import model.usersAndGroups.UserOrGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;
import utils.NotificationCenter;

public class RightsController extends Controller {
	public static final ALogger log = Logger.of(CollectionController.class);

	/**
	 * Set access rights for object for user.
	 *
	 * @param colId
	 *            the internal Id of the object you wish to share (or unshare)
	 * @param right
	 *            the right to give ("none" to withdraw previously given right)
	 * @param username
	 *            the username of user to give rights to (or take away from)
	 * @return OK or Error with JSON detailing the problem
	 *
	 */
	public static Result shareCollection(String colId, String right,
			String username) {

		ObjectNode result = Json.newObject();
		Collection collection = null;
		try {
			collection = DB.getCollectionDAO().get(new ObjectId(colId));
		} catch (Exception e) {
			log.error("Cannot retrieve collection from database!", e);
			result.put("message", "Cannot retrieve collection from database!");
			return internalServerError(result);
		}
		List<String> userIds = Arrays.asList(session().get("effectiveUserIds")
				.split(","));
		ObjectId userId = new ObjectId(session().get("user"));
		User admin = DB.getUserDAO().get(userId);
		if (!AccessManager.checkAccess(collection.getRights(), userIds,
				Action.DELETE) && !admin.isSuperUser()) {
			result.put("error",
					"Sorry! You do not own this collection so you cannot set rights. "
							+ "Please contact the owner of this collection");
			return forbidden(result);
		}
		ObjectId owner = new ObjectId(userIds.get(0));
		// set rights
		// the receiver can be either a User or a UserGroup
		// Map<ObjectId, Access> rightsMap = new HashMap<ObjectId, Access>();
		ObjectId userOrGroupId = null;
		boolean groupRelated = false;
		if (username != null) {
			User user = DB.getUserDAO().getByUsername(username);
			if (user != null) {
				userOrGroupId = user.getDbId();
			} else {
				UserGroup userGroup = DB.getUserGroupDAO().getByName(username);
				if (userGroup != null) {
					userOrGroupId = userGroup.getDbId();
				}
				groupRelated = true;
			}
		}
		if (userOrGroupId == null) {
			result.put("error",
					"No user or userGroup with given username/email");
			return badRequest(result);
		}
		ObjectId collectionId = collection.getDbId();
		if (right.equals("NONE")) {
			collection.getRights().removeFromAcl(userOrGroupId);
			if (DB.getCollectionDAO().makePermanent(collection) == null) {
				result.put("error", "Cannot store collection to database!");
				return internalServerError(result);
			}
			// update collection rights in index
			//ElasticUpdater updater = new ElasticUpdater(collection);
			//updater.updateCollectionRights();
			// Inform user or group for the unsharing
			Notification notification = new Notification();
			notification.setActivity(Activity.COLLECTION_UNSHARED);
			if (groupRelated) {
				notification.setGroup(userOrGroupId);
			}
			notification.setReceiver(userOrGroupId);
			notification.setCollection(collectionId);
			notification.setSender(owner);
			notification.setPendingResponse(false);
			Date now = new Date();
			notification.setOpenedAt(new Timestamp(now.getTime()));
			DB.getNotificationDAO().makePermanent(notification);
			NotificationCenter.sendNotification(notification);
			result.put("mesage", "Collection unshared with user or group");
			return ok(result);
		} else if (admin.isSuperUser()) {
			collection.getRights().addToAcl(userOrGroupId, Access.valueOf(right));
			if (DB.getCollectionDAO().makePermanent(collection) == null) {
				result.put("error", "Cannot store collection to database!");
				return internalServerError(result);
			}
			// update collection rights in index
			//ElasticUpdater updater = new ElasticUpdater(collection);
			//updater.updateCollectionRights();
			Notification newNotification = new Notification();
			newNotification.setActivity(Activity.COLLECTION_SHARED);
			if (groupRelated) {
				newNotification.setGroup(userOrGroupId);
			}
			newNotification.setReceiver(userOrGroupId);
			newNotification.setCollection(collectionId);
			newNotification.setSender(owner);
			newNotification.setPendingResponse(false);
			Date now = new Date();
			newNotification.setOpenedAt(new Timestamp(now.getTime()));
			DB.getNotificationDAO().makePermanent(newNotification);
			NotificationCenter.sendNotification(newNotification);
			result.put("message", "Collection shared");
			return ok(result);
		} else {
			Access access = Access.valueOf(right);
			List<Notification> requests = DB.getNotificationDAO()
					.getPendingCollectionNotifications(userOrGroupId,
							collectionId, Activity.COLLECTION_SHARE, access);
			if (requests.isEmpty()) {
				// Find if there is a request for other type of access and
				// override
				// it
				requests = DB.getNotificationDAO()
						.getPendingCollectionNotifications(userOrGroupId,
								collectionId, Activity.COLLECTION_SHARE);
				for (Notification request : requests) {
					request.setPendingResponse(false);
					Date now = new Date();
					request.setReadAt(new Timestamp(now.getTime()));
					DB.getNotificationDAO().makePermanent(request);
				}
				// Make a new request for collection sharing request
				Notification notification = new Notification();
				notification.setActivity(Activity.COLLECTION_SHARE);
				if (groupRelated) {
					notification.setGroup(userOrGroupId);
				}
				notification.setAccess(access);
				notification.setReceiver(userOrGroupId);
				notification.setCollection(collectionId);
				notification.setSender(owner);
				notification.setPendingResponse(true);
				Date now = new Date();
				notification.setOpenedAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(notification);
				NotificationCenter.sendNotification(notification);
				result.put("message",
						"Request for collection sharing sent to the user");
				return ok(result);
			} else {
				result.put("error", "Request has already been sent to the user");
				return badRequest(result);
			}
		}
	}
}
