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

import java.util.List;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import search.FiltersFields;
import search.Sources;
import sources.FilterValuesMap;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class DPLARecordFormatter extends CulturalRecordFormatter {

	public DPLARecordFormatter() {
		super(FilterValuesMap.getMap(Sources.DPLA));
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		Language[] language = null;
		List<String> langs = rec.getStringArrayValue(false,"sourceResource.language[.*].iso639_3","originalRecord.language");
		if (Utils.hasInfo(langs)){
			language = new Language[langs.size()];
			for (int i = 0; i < langs.size(); i++) {
				language[i] = Language.getLanguage(langs.get(i));
			}
		}
		if (!Utils.hasInfo(language)){
			language = getLanguagesFromText(rec.getStringValue("sourceResource.title"),
											rec.getStringValue("sourceResource.description"),
											rec.getStringValue("sourceResource.subject"));
		}
		rec.setLanguages(language);
		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		model.setDctermsspatial(rec.getMultiLiteralOrResourceValue(false,"originalRecord.spatial","sourceResource.spatial[.*].name"));
		model.setCountry(rec.getMultiLiteralOrResourceValue("sourceResource.spatial[.*].country"));
		model.setCity(rec.getMultiLiteralOrResourceValue("sourceResource.spatial[.*].city"));
		model.setCoordinates(StringUtils.getPoint(rec.getStringValue("sourceResource.spatial[.*].coordinates")));
		model.setLabel(rec.getMultiLiteralValue(false,"sourceResource.title","originalRecord.label"));
		model.setDescription(rec.getMultiLiteralValue(false,"sourceResource.description","originalRecord.description"));
		model.setIsShownBy( rec.getResource("hasView.@id"));
		model.setIsShownAt(rec.getResource("isShownAt"));
		model.setDates(rec.getWithDateArrayValue("sourceResource.date.begin"));
		model.setDccontributor(rec.getMultiLiteralOrResourceValue(false,"sourceResource.contributor","originalRecord.contributor"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue(false,"sourceResource.creator","originalRecord.creator"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue(false,"sourceResource.subject[.*].name","originalRecord.subject"));
		String uriAt = model.getIsShownAt()==null?null:model.getIsShownAt().toString();
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider"), uriAt,null));
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("provider.name"), rec.getStringValue("provider.@id"), null));
		String recID = rec.getStringValue("id");
		String uri = "http://dp.la/item/"+recID;
		object.addToProvenance(
				new ProvenanceInfo(Sources.DPLA.toString(), uri, recID));
		List<String> rights = rec.getStringArrayValue("sourceResource.rights");
		String stringValue = rec.getStringValue("sourceResource.type","originalRecord.type");
		List<Object> translateToCommon = getValuesMap().translateToCommon(FiltersFields.TYPE.getFilterId(), stringValue);
		WithMediaType type = translateToCommon==null?null:(WithMediaType.getType(translateToCommon.get(0).toString())) ;
		WithMediaRights withRights = getWithMediaRights(rights);
		String uri3 = rec.getStringValue("object");
		String uri2 = model.getIsShownBy()==null?null:model.getIsShownBy().toString();
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			if (Utils.hasInfo(rights))
			medThumb.setOriginalRights(new LiteralOrResource(rights.get(0)));
			medThumb.setWithRights(withRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		if (Utils.hasInfo(uri2)){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setUrl(uri2);
			if (Utils.hasInfo(rights))
			med.setOriginalRights(new LiteralOrResource(rights.get(0)));
			med.setWithRights(withRights);
			med.setType(type);
			object.addMedia(MediaVersion.Original, med);
		}
		return object;

	}

}
