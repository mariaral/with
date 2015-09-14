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

import java.io.IOException;
import java.util.Set;
import java.util.function.Function;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.User;
import model.User.Access;
import model.UserGroup;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class GroupManager extends Controller {

	public static final ALogger log = Logger.of(UserGroup.class);

	/**
	 * Creates a {@link UserGroup} with the specified user as administrator and
	 * with the given body as JSON.
	 * <p>
	 * The name of the group must be unique. If the administrator is not
	 * provided as a parameter the administrator of the group becomes the user
	 * who made the call.
	 *
	 * @param adminId
	 *            the administrator id
	 * @param adminUsername
	 *            the administrator username
	 * @return the JSON of the new group
	 */
	public static Result createGroup(String adminId, String adminUsername) {

		ObjectId admin;
		UserGroup newGroup = null;
		JsonNode json = request().body().asJson();

		if (json == null) {
			return badRequest("Invalid JSON");
		}
		if (!json.has("name")) {
			return badRequest("Must specify name for the group");
		}
		if (!uniqueGroupName(json.get("name").asText())) {
			return badRequest("Group name already exists! Please specify another name.");
		}
		newGroup = Json.fromJson(json, UserGroup.class);
		if (adminId != null) {
			admin = new ObjectId(adminId);
		} else if (adminUsername != null) {
			admin = DB.getUserDAO().getByUsername(adminUsername).getDbId();
		} else {
			if ((adminId = AccessManager.effectiveUserId(session().get(
					"effectiveUserIds"))).isEmpty()) {
				return internalServerError("Must specify administrator of group");
			}
			admin = new ObjectId(AccessManager.effectiveUserId(session().get(
					"effectiveUserIds")));
		}
		newGroup.setAdministrator(admin);
		newGroup.getUsers().add(admin);
		Set<ConstraintViolation<UserGroup>> violations = Validation
				.getValidator().validate(newGroup);
		for (ConstraintViolation<UserGroup> cv : violations) {
			return badRequest("[" + cv.getPropertyPath() + "] "
					+ cv.getMessage());
		}
		try {
			DB.getUserGroupDAO().makePermanent(newGroup);
			Set<ObjectId> parentGroups = newGroup.retrieveParents();
			parentGroups.add(newGroup.getDbId());
			User user = DB.getUserDAO().get(admin);
			user.addUserGroup(parentGroups);
			DB.getUserDAO().makePermanent(user);
		} catch (Exception e) {
			log.error("Cannot save group to database!", e);
			return internalServerError("Cannot save group to database!");
		}
		return ok(Json.toJson(newGroup));
	}

	private static boolean uniqueGroupName(String name) {
		return (DB.getUserGroupDAO().getByName(name) == null);
	}

	/**
	 * Edits group metadata and updates them according to the POST body.
	 * <p>
	 * Only the administrator of the group and the superuser have the right to
	 * edit the group.
	 * 
	 * @param groupId
	 *            the group id
	 * @return the updated group metadata
	 */
	public static Result editGroup(String groupId) {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		String adminId = AccessManager.effectiveUserId(session().get(
				"effectiveUserIds"));
		if ((adminId == null) || (adminId.equals(""))) {
			return forbidden("Only administrator of group has the right to edit the group");
		}
		User admin = DB.getUserDAO().get(new ObjectId(adminId));
		UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
		if (group == null) {
			return internalServerError("Cannot retrieve group from database!");
		}
		if (!group.getAdministrator().equals(new ObjectId(adminId))
				&& (!admin.isSuperUser())) {
			return forbidden("Only administrator of group has the right to edit the group");
		}
		UserGroup oldVersion = group;
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader updator = objectMapper.readerForUpdating(oldVersion);
		UserGroup newVersion;
		try {
			newVersion = updator.readValue(json);
			Set<ConstraintViolation<UserGroup>> violations = Validation
					.getValidator().validate(newVersion);
			for (ConstraintViolation<UserGroup> cv : violations) {
				result.put("message",
						"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			}
			if (!violations.isEmpty()) {
				return badRequest(result);
			}
			if (!uniqueGroupName(newVersion.getName())) {
				return badRequest("Group name already exists! Please specify another name.");
			}
			// update group on mongo
			if (DB.getUserGroupDAO().makePermanent(newVersion) == null) {
				log.error("Cannot save group to database!");
				return internalServerError("Cannot save group to database!");
			}
			return ok(DB.getJson(newVersion));
		} catch (IOException e) {
			e.printStackTrace();
			return internalServerError(e.getMessage());
		}
	}

	/**
	 * Deletes a group from the database. The users who participate are not
	 * deleted as well.
	 *
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	public static Result deleteGroup(String groupId) {

		try {
			DB.getUserGroupDAO().deleteById(new ObjectId(groupId));
		} catch (Exception e) {
			log.error("Cannot delete group from database!", e);
			return internalServerError("Cannot delete group from database!");
		}
		return ok("Group deleted succesfully from database");
	}

	/**
	 * Gets the group.
	 *
	 * @param groupId
	 *            the group id
	 * @return the group
	 */
	public static Result getGroup(String groupId) {

		try {
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			return ok(DB.getJson(group));
		} catch (Exception e) {
			log.error("Cannot retrieve group from database!", e);
			return internalServerError("Cannot retrieve group from database!");
		}
	}

	/**
	 * Adds a user to group.
	 * <p>
	 * Right now only the administrator of the group and the superuser have the
	 * rights to add a group to the group.
	 *
	 * @param userId
	 *            the user id
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	public static Result addUserToGroup(String userId, String groupId) {

		String adminId = AccessManager.effectiveUserId(session().get(
				"effectiveUserIds"));
		if ((adminId == null) || (adminId.equals(""))) {
			return forbidden("Only administrator of group has the right to add users");
		}
		User admin = DB.getUserDAO().get(new ObjectId(adminId));
		UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
		if (group == null) {
			return internalServerError("Cannot retrieve group from database!");
		}
		if (!group.getAdministrator().equals(new ObjectId(adminId))
				&& (!admin.isSuperUser())) {
			return forbidden("Only administrator of group has the right to add users");
		}
		User user = DB.getUserDAO().get(new ObjectId(userId));
		group.getUsers().add(new ObjectId(userId));
		Set<ObjectId> parentGroups = group.retrieveParents();

		if (user == null) {
			return internalServerError("Cannot retrieve user from database!");
		}
		parentGroups.add(group.getDbId());
		user.addUserGroup(parentGroups);

		if (!(DB.getUserDAO().makePermanent(user) == null)
				&& !(DB.getUserGroupDAO().makePermanent(group) == null)) {
			return ok("User succesfully added to group");
		}
		return internalServerError("Cannot store to database!");

	}

	/**
	 * Removes a user from the group.
	 * <p>
	 * The users allowed to remove a user from a group is the administrator of
	 * the group, the superuser and the user himself.
	 *
	 * @param userId
	 *            the user id
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	public static Result removeUserFromGroup(String userId, String groupId) {
		String userSession = AccessManager.effectiveUserId(session().get(
				"effectiveUserIds"));
		if ((userSession == null) || (userSession.equals(""))) {
			return forbidden("No rights for user removal");
		}
		User userS = DB.getUserDAO().get(new ObjectId(userSession));
		UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
		if (group == null) {
			return internalServerError("Cannot retrieve group from database!");
		}
		if (!group.getAdministrator().equals(new ObjectId(userSession))
				&& (!userS.isSuperUser() && (!userSession.equals(userId)))) {
			return forbidden("No rights for user removal");
		}
		User user = DB.getUserDAO().get(new ObjectId(userId));
		group.getUsers().remove(new ObjectId(userId));
		user.recalculateGroups();
		return ok("User successfully removed from group");

	}

	/**
	 * @param name
	 *            the group name
	 * @return the result
	 */
	public static Result findByGroupName(String name, String collectionId) {
		Function<UserGroup, Status> getGroupJson = (UserGroup group) -> {
			ObjectNode groupJSON = Json.newObject();
			groupJSON.put("groupId", group.getDbId().toString());
			groupJSON.put("name", group.getName());
			groupJSON.put("description", group.getDesc());
			if (collectionId != null) {
				Collection collection = DB.getCollectionDAO().getById(
						new ObjectId(collectionId));
				if (collection != null) {
					Access accessRights = collection.getRights().get(
							group.getDbId());
					if (accessRights != null)
						groupJSON.put("accessRights", accessRights.toString());
					else
						groupJSON.put("accessRights", Access.NONE.toString());
				}
			}
			return ok(groupJSON);
		};
		UserGroup group = DB.getUserGroupDAO().getByName(name);
		return getGroupJson.apply(group);
	}
}
