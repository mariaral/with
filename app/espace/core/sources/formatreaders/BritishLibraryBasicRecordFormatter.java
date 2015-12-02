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


package espace.core.sources.formatreaders;

import espace.core.ExternalBasicRecordReader;
import espace.core.utils.JsonContextRecord;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;

public class BritishLibraryBasicRecordFormatter extends ExternalBasicRecordReader<CulturalObject> {


	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = new CulturalObjectData();
		object.setDescriptiveData(model);
//		object.setThumbnailUrl(rec.getStringValue("url_s"));
//		object.setTitle(rec.getStringValue("title"));
//		object.setDescription(rec.getStringValue("description"));
//		object.setCreator(rec.getStringValue("principalOrFirstMaker"));
//		object.setContributors(rec.getStringArrayValue("sourceResource.contributor"));
//		object.setYears(StringUtils.getYears(rec.getStringArrayValue("datetaken")));
//		// TODO: add rights
//		// object.setItemRights(rec.getStringValue("rights"));
//		object.setExternalId(object.getIsShownAt());
//		if (object.getExternalId() == null || object.getExternalId() == "")
//			object.setExternalId(object.getIsShownBy());
//		object.setExternalId(DigestUtils.md5Hex(object.getExternalId()));
//		String id = rec.getStringValue("id");
//		Provider recordProvider = new Provider(BritishLibrarySpaceSource.LABEL, id,
//				"https://www.flickr.com/photos/britishlibrary/" + id + "/");
//		object.addProvider(recordProvider);
		return object;
	}

}
