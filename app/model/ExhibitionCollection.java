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

import java.util.HashMap;

import org.mongodb.morphia.annotations.Entity;

import model.EmbeddedMediaObject.MediaVersion;
import model.resources.CollectionObject;
import model.resources.RecordResource.RecordDescriptiveData;

@Entity("RecordResource")
public class ExhibitionCollection extends CollectionObject {
	
	private String intro;
	private HashMap<MediaVersion, EmbeddedMediaObject> backgroundImg;
	private String credits;

	public String getIntro() {
		return intro;
	}

	public void setIntro(String intro) {
		this.intro = intro;
	}

	public HashMap<MediaVersion, EmbeddedMediaObject> getBackgroundImg() {
		return backgroundImg;
	}

	public void setBackgroundImg(HashMap<MediaVersion, EmbeddedMediaObject> backgroundImg) {
		this.backgroundImg = backgroundImg;
	}

	public String getCredits() {
		return credits;
	}

	public void setCredits(String credits) {
		this.credits = credits;
	}
}
