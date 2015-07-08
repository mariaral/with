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


package espace.core.sources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonFilter;
import espace.core.CommonFilterLogic;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.QueryBuilder;
import espace.core.QueryModifier;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;

public class EuropeanaFashionSpaceSource extends ISpaceSource {
	
	public EuropeanaFashionSpaceSource() {
		super();

		Function<List<String>, QueryModifier> f = (List<String> args) -> {
			return new QueryModifier() {
				@Override
				public QueryBuilder modify(QueryBuilder builder) {
					FashionSearch fs = (FashionSearch) builder.getData();
					for (String string : args) {
						Filter ft = new Filter();
						ft.setType("objectType");
						ft.setId("219");
						ft.setValue("http://thesaurus.europeanafashion.eu/thesaurus/10460");
						ft.setTerm(string);
						fs.getFilters().add(ft);
					}
					return builder;
				}
			};
		};
		//		addDefaultWriter(CommonFilters.TYPE_ID, qfwriter("TYPE"));
		addDefaultQueryModifier(CommonFilters.TYPE_ID, f );
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "photograph");
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "artwork");
//		addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "VIDEO");
//		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, "SOUND");
//		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "TEXT");
			
	}

//	public String getHttpQuery(CommonQuery q) {
//		addfilters(q, builder)
//		return "http://www.europeanafashion.eu/api/search/";
//	}
	
	@Override
	public QueryBuilder getBuilder(CommonQuery q) {
		QueryBuilder builder = super.getBuilder(q);
		builder.setBaseUrl("http://www.europeanafashion.eu/api/search/");
		FashionSearch fq = new FashionSearch();
		fq.setTerm(q.searchTerm);
		fq.setCount(Integer.parseInt(q.pageSize));
		fq.setOffset(((Integer.parseInt(q.page) - 1)
				* Integer.parseInt(q.pageSize)));
		builder.setData(fq);
		addfilters(q, builder);
		return builder;
	}

	
	public String getSourceName() {
		return "EFashion";
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
//		String httpQuery = getHttpQuery(q);
		QueryBuilder builder = getBuilder(q);
		res.query = builder.getHttp();
		JsonNode response;
		if (checkFilters(q)){
		try {
			response = HttpConnector.getPOSTURLContent(res.query, Json.toJson(builder.getData()).toString());
			// System.out.println(response.toString());
			JsonNode docs = response.path("results");
			res.totalCount = Utils.readIntAttr(response, "total", true);
			res.count = docs.size();
			res.startIndex = Utils.readIntAttr(response, "offset", true);
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			for (JsonNode item : docs) {
				ItemsResponse it = new ItemsResponse();
				it.id = Utils.readAttr(item, "id", true);
				it.thumb = Utils.readArrayAttr(item, "thumbnail", false);
				it.fullresolution = null;
				it.title = Utils.readLangAttr(item, "title", false);
				it.description = Utils.readLangAttr(item, "description", false);
				// it.creator = Utils.readLangAttr(item.path("sourceResource"),
				// "creator", false);
				// it.year = null;
				// it.dataProvider = Utils.readLangAttr(item.path("provider"),
				// "name", false);
				it.url = new MyURL();
				// it.url.original = Utils.readArrayAttr(item, "isShownAt",
				// false);
				it.url.fromSourceAPI = "http://www.europeanafashion.eu/record/a/"
						+ it.id;
				a.add(it);
			}
			res.items = a;
			
			CommonFilterLogic type = CommonFilterLogic.typeFilter();
			
			JsonNode o = response.path("facets");
			readList(o.path("objectType"), type);


			res.filtersLogic = new ArrayList<>();
			res.filtersLogic.add(type);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}

		return res;
	}
	
	private void readList(JsonNode json, CommonFilterLogic filter) {
		 for (JsonNode node : json.path("terms")) {
			String label = node.path("term").asText();
			int count = node.path("count").asInt();
			String id = node.path("uri").asText();
			countValue(filter, label, count);
		}
	}

	

	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector
					.getURLContent("http://www.europeanafashion.eu/api/record/"
							+ recordId);
			JsonNode record = response;
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON_EDM, record
					.toString()));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

}
