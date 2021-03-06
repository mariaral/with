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


package model.usersAndGroups;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Entity("UserGroup")
public class Project extends UserGroup {

	@Embedded
	private Page page;
	private ObjectNode uiSettings;

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}
	
	public String getType() {
		return "Project";
	}

	
	public ObjectNode getUiSettings() {
		return this.uiSettings;
	}
	
	public void setUiSettings( ObjectNode uiSettings ) {
		this.uiSettings = uiSettings;
	}
}

