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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import model.EmbeddedMediaObject.MediaVersion;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.WithResourceType;
import model.resources.collection.CollectionObject;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import notifications.Notification;
import notifications.Notification.Activity;
import notifications.ResourceNotification;
import notifications.ResourceNotification.ShareInfo;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.RedeemablePromise;
import play.libs.Json;
import play.mvc.Result;
import sources.core.ParallelAPICall;
import utils.NotificationCenter;

public class RightsController extends WithResourceController {
	public static final ALogger log = Logger.of(RightsController.class);
	
	public static Result editCollectionPublicity(String colId, Boolean isPublic, boolean membersDowngrade) {
		ObjectNode result = Json.newObject();
		ObjectId colDbId = new ObjectId(colId);
		Result response = errorIfNoAccessToCollection(Action.DELETE, colDbId);
		if (!response.toString().equals(ok().toString()))
			return response;
		else {
			CollectionObject collection = DB.getCollectionObjectDAO().
				getUniqueByFieldAndValue("_id", colDbId, new ArrayList<String>(Arrays.asList("administrative.access")));
			boolean oldIsPublic = collection.getAdministrative().getAccess().getIsPublic();
			if (oldIsPublic != isPublic) {
				List<ObjectId> effectiveIds = effectiveUserDbIds();
				DB.getCollectionObjectDAO().updateField(colDbId, "administrative.access.isPublic", isPublic);
				if (isPublic) //upgrade
					ParallelAPICall.Priority.BACKEND.getExcecutionContext().execute(() -> {
						DB.getRecordResourceDAO().updateMembersToNewPublicity(colDbId, isPublic, effectiveIds);
					});
				else
				ParallelAPICall.Priority.BACKEND.getExcecutionContext().execute(() -> {
					changePublicity(colDbId, isPublic, effectiveIds, isPublic == false, membersDowngrade);			
				});
			}
			return ok(result);
		}
	}
	
	public static Result editCollectionRights(String colId, String right, String username, boolean membersDowngrade) {
		ObjectNode result = Json.newObject();
		ObjectId colDbId = new ObjectId(colId);
		Access newAccess = Access.valueOf(right);
		if (newAccess == null) {
			result.put("error", right + " is not an admissible value for access rights " +
					"(should be one of NONE, READ, WRITE, OWN).");
			return badRequest(result);
		}
		else {
			ObjectId loggedIn = effectiveUserDbId();
			if (loggedIn == null)
				return forbidden("No rights to edit collection rights.");
			UserGroup userGroup = null;
			User user = null;
			// the receiver can be either a User or a UserGroup
			RedeemablePromise<Result> err = RedeemablePromise.empty();
			ObjectId userOrGroupId = UserAndGroupManager.findUserByUsername( username , err );
			if( userOrGroupId == null ) return err.get(1000l);

			//check whether the newAccess entails a downgrade or upgrade of the current access of the collection
			CollectionObject collection = DB.getCollectionObjectDAO().
					getUniqueByFieldAndValue("_id", colDbId, new ArrayList<String>(Arrays.asList("administrative.access")));
			WithAccess oldColAccess = collection.getAdministrative().getAccess();
			int downgrade = isDowngrade(oldColAccess.getAcl(), userOrGroupId, newAccess);
			//the logged in user has the right to downgrade his own access level (unshare)
			boolean hasDowngradeRight = loggedIn.equals(userOrGroupId);
			List<ObjectId> effectiveIds = effectiveUserDbIds();
			if (userGroup != null) {
				hasDowngradeRight = userGroup.getAdminIds().contains(loggedIn);
			}
			if (downgrade == 1 && hasDowngradeRight) {
					changeAccess(colDbId, userOrGroupId, newAccess, effectiveIds, true, membersDowngrade);
				return sendShareCollectionNotification(userOrGroupId, colDbId, loggedIn, Access.NONE, newAccess, effectiveIds, 
						true, membersDowngrade);
			}
			else if (downgrade > -1){//downgrade and no downgradeRights or upgrade
				Result response = errorIfNoAccessToCollection(Action.DELETE, colDbId);
				if (!response.toString().equals(ok().toString()))
					return response;
				else {
					Access oldAccess = Access.NONE;
					for (AccessEntry ae: oldColAccess.getAcl()) {
						if (ae.getUser().equals(userOrGroupId)) {
							oldAccess = ae.getLevel();
							break;
						}
					}
					changeAccess(colDbId, userOrGroupId, newAccess, effectiveIds, downgrade == 1, membersDowngrade);
					return sendShareCollectionNotification(userOrGroupId, colDbId, loggedIn, oldAccess, newAccess, effectiveIds, 
							downgrade == 1, membersDowngrade);
				}
			}
			else {//if downgrade == -1, the rights are not changed, do nothing
				result.put("error", "The user already has the required access to the collection.");
				return ok(result);
			}
		}
	}
	
	public static void changePublicity(ObjectId colId, boolean isPublic, List<ObjectId> effectiveIds, boolean downgrade, boolean membersDowngrade) {
		DB.getCollectionObjectDAO().updateField(colId, "administrative.access.isPublic", isPublic);
		if (downgrade && membersDowngrade) {//the publicity of all records that belong to the collection is downgraded
			ParallelAPICall.Priority.BACKEND.getExcecutionContext().execute(() -> {
			DB.getRecordResourceDAO().updateMembersToNewPublicity(colId, isPublic, effectiveIds);
		});}
		else {//if upgrade, or downgrade but !membersDowngrade the new rights of the collection are merged to all records that belong to the collection. 
			ParallelAPICall.Priority.BACKEND.getExcecutionContext().execute(() -> {
			DB.getRecordResourceDAO().updateMembersToMergedPublicity(colId, isPublic, effectiveIds);
			});}
	}
	
	public static void changeAccess(ObjectId colId, ObjectId userOrGroupId, Access newAccess, List<ObjectId> effectiveIds, 
			boolean downgrade, boolean membersDowngrade) {
		Access oldAccess = DB.getCollectionObjectDAO().changeAccess(colId, userOrGroupId, newAccess);
		if (downgrade && membersDowngrade) {//the rights of all records that belong to the collection are downgraded
			ParallelAPICall.Priority.BACKEND.getExcecutionContext().execute(() -> {
				DB.getRecordResourceDAO().updateMembersToNewAccess(colId, userOrGroupId, oldAccess, newAccess, effectiveIds);
			});
		}
		else {//if upgrade, or downgrade but !membersDowngrade the new rights of the collection are merged to all records that belong to the collection. 
			ParallelAPICall.Priority.BACKEND.getExcecutionContext().execute(() -> {
				DB.getRecordResourceDAO().updateMembersToMergedRights(colId, oldAccess, new AccessEntry(userOrGroupId, newAccess), effectiveIds);
			});
		}		
	}
	
	public static int isDowngrade(List<AccessEntry> oldColAcl, ObjectId userOrGroupId, Access newAccess) {
		for (AccessEntry ae: oldColAcl) {
			if (ae.getUser().equals(userOrGroupId))
				if (ae.getLevel().ordinal() > newAccess.ordinal())
					return 1;
				else if (ae.getLevel().ordinal() == newAccess.ordinal())
					return -1;
		}
		return 0;
	}
	
				
	public static Result sendShareCollectionNotification(ObjectId userOrGroupId, ObjectId colDbId, 
			ObjectId ownerId, Access oldAccess, Access newAccess, List<ObjectId> effectiveIds, boolean downgrade, boolean membersDowngrade) {
		ObjectNode result = Json.newObject();	
		ResourceNotification notification = new ResourceNotification();
		//what if the receiver is a group?
		notification.setReceiver(userOrGroupId);
		notification.setResource(colDbId);
		notification.setSender(ownerId);
		WithResourceType collectionType = DB.getCollectionObjectDAO().getById(colDbId, Arrays.asList("resourceType")).getResourceType();
		Activity share = (collectionType == WithResourceType.SimpleCollection) ? Activity.COLLECTION_SHARE : Activity.EXHIBITION_SHARE;
		Activity shared = (collectionType == WithResourceType.SimpleCollection) ? Activity.COLLECTION_SHARED : Activity.EXHIBITION_SHARED;
		Activity unshared = (collectionType == WithResourceType.SimpleCollection) ? Activity.COLLECTION_UNSHARED : Activity.EXHIBITION_UNSHARED;
		User sender = DB.getUserDAO().getById(ownerId, new ArrayList<String>(Arrays.asList("avatar")));
		HashMap<MediaVersion, String> avatar = sender.getAvatar();
		if (avatar != null && avatar.containsKey(MediaVersion.Square))
			notification.setSenderLogoUrl(avatar.get(MediaVersion.Square));
		ShareInfo shareInfo = new ShareInfo();
		shareInfo.setNewAccess(newAccess);
		shareInfo.setUserOrGroup(userOrGroupId);
		Date now = new Date();
		notification.setOpenedAt(new Timestamp(now.getTime()));
		if (downgrade) {
			notification.setPendingResponse(false);
			notification.setActivity(unshared);
			notification.setShareInfo(shareInfo);
			DB.getNotificationDAO().makePermanent(notification);
			NotificationCenter.sendNotification(notification);
			result.put("message", "Access of user or group to collection has been downgraded.");
			return ok(result);
		}
		else {//upgrade
			UserGroup userGroup = DB.getUserGroupDAO().getById(userOrGroupId, new ArrayList<String>(Arrays.asList("_id", "adminIds")));
			if (DB.getUserDAO().isSuperUser(ownerId) || (userGroup != null && userGroup.getAdminIds().contains(ownerId))) {
				notification.setPendingResponse(false);
				notification.setActivity(shared);
				notification.setShareInfo(shareInfo);
				DB.getNotificationDAO().makePermanent(notification);
				NotificationCenter.sendNotification(notification);
				result.put("message", "Collection shared.");
				return ok(result);
			}
			else {//receiver can reject the notification
				List<Notification> requests = DB.getNotificationDAO()
						.getPendingResourceNotifications(userOrGroupId,
								colDbId, share, newAccess);
				if (requests.isEmpty()) {
					// Find if there is a request for other type of access and
					// override it
					requests = DB.getNotificationDAO()
							.getPendingResourceNotifications(userOrGroupId,
									colDbId, share);
					for (Notification request : requests) {
						request.setPendingResponse(false);
						now = new Date();
						request.setReadAt(new Timestamp(now.getTime()));
						DB.getNotificationDAO().makePermanent(request);
					}
					// Make a new request for collection sharing request
					shareInfo.setPreviousAccess(oldAccess);
					notification.setPendingResponse(true);
					notification.setActivity(share);
					now = new Date();
					shareInfo.setOwnerEffectiveIds(effectiveIds);
					notification.setShareInfo(shareInfo);
					notification.setOpenedAt(new Timestamp(now.getTime()));
					DB.getNotificationDAO().makePermanent(notification);
					NotificationCenter.sendNotification(notification);
					result.put("message",
							"Request for collection sharing sent to the user.");
					return ok(result);
				} else {
					result.put("error", "Request has already been sent to the user.");
					return badRequest(result);
				}
			}
		}
	}
}
