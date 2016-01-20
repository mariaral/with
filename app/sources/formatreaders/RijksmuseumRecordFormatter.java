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


package sources.formatreaders;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.Provider.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.utils.JsonContextRecord;

public class RijksmuseumRecordFormatter extends CulturalRecordFormatter {

	public RijksmuseumRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		model.setLabel(rec.getMultiLiteralValue("title"));
		model.setDescription(rec.getMultiLiteralValue("longTitle"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("edmIsShownBy"));
		model.setIsShownAt(rec.getLiteralOrResourceValue("edmIsShownAt"));
		// model.setYear(Integer.parseInt(rec.getStringValue("year")));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("principalOrFirstMaker"));

		// object.addToProvenance(new
		// ProvenanceInfo(rec.getStringValue("dataProvider")));
		// object.addToProvenance(new
		// ProvenanceInfo(rec.getStringValue("provider")));
		String id = rec.getStringValue("objectNumber");
		object.addToProvenance(new ProvenanceInfo(Sources.Rijksmuseum.toString(), 
				"https://www.rijksmuseum.nl/en/search/objecten?q=dance&p=1&ps=12&ii=0#/" + id + ",0", id));
		EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
		medThumb.setUrl(rec.getStringValue("webImage.url"));
		object.addMedia(MediaVersion.Thumbnail, medThumb);
		// TODO: add rights!
		EmbeddedMediaObject med = new EmbeddedMediaObject();
		med.setUrl(rec.getStringValue("edmIsShownBy"));
		med.setWidth(rec.getIntValue("webImage.width"));
		med.setHeight(rec.getIntValue("webImage.height"));
		object.addMedia(MediaVersion.Original, med);
		// med.setUrl(rec.getStringValue("edmIsShownBy"));
		return object;
		//// record.setContributors(rec.getStringArrayValue("contributor"));
		// // TODO: add years
		// object.setYears(ListUtils.transform(rec.getStringArrayValue("year"),
		//// (String y)->{return Year.parse(y);}));
		// // TODO: add rights
		//// record.setItemRights(rec.getStringValue("rights"));
	}

}
