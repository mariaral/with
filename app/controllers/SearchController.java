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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.elasticsearch.action.suggest.SuggestResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DAO;
import db.DB;
import elastic.Elastic;
import elastic.ElasticCoordinator;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import model.EmbeddedMediaObject.WithMediaRights;
import play.Logger;
import play.Logger.ALogger;
import play.data.Form;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import search.ChainedSearchResult;
import search.Fields;
import search.FiltersCache;
import search.Query;
import search.RecordsList;
import search.Response;
import search.Response.SingleResponse;
import search.SimilarCreatorSearch;
import search.SimilarProviderSearch;
import search.SimilarSearchTerm;
import search.Source;
import search.Sources;
import sources.core.CommonFilterLogic;
import sources.core.CommonFilterResponse;
import sources.core.CommonQuery;
import sources.core.ESpaceSources;
import sources.core.FiltersHelper;
import sources.core.ISpaceSource;
import sources.core.ParallelAPICall;
import sources.core.ParallelAPICall.Priority;
import sources.core.SearchResponse;
import sources.core.SourceResponse;
import sources.core.Utils;
import utils.ListUtils;

public class SearchController extends WithController {

	final static Form<CommonQuery> userForm = Form.form(CommonQuery.class);
	public static final ALogger log = Logger.of(SearchController.class);
	static final Set<String> excludedFieldnames = new HashSet<String>();
	static {
 		excludedFieldnames.add( "COLOURPALETE" );
		excludedFieldnames.add( "IMAGE_COLOUR" );
		excludedFieldnames.add( "IMAGE_SIZE" );
		excludedFieldnames.add( "anywhere" );
	}

	public static Promise<Result> search() {
		JsonNode json = request().body().asJson();
		if (log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> e : session().entrySet()) {
				sb.append(e.getKey() + " = " + "'" + e.getValue() + "'\n");
			}
			log.debug(sb.toString());
		}

		if (json == null) {
			return Promise.pure((Result) badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				final CommonQuery q = Utils.parseJson(json);
				q.setTypes(Elastic.allTypes);
				List<String> userIds = effectiveUserIds();
				q.setEffectiveUserIds(userIds);
				Iterable<Promise<SourceResponse>> promises = callSources(q);
				// compose all futures, blocks until all futures finish
				return ParallelAPICall.<SourceResponse> combineResponses(r -> {
					log.info(r.source + " found " + r.count);
					return true;
				} , promises, Priority.FRONTEND);
			} catch (Exception e) {
				log.error("",e);
				return Promise.pure((Result) badRequest(e.getMessage()));
			}
		}
	}
	
	public static Promise<Result> relatedSearch(){
		JsonNode json = request().body().asJson();
		if (log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> e : session().entrySet()) {
				sb.append(e.getKey() + " = " + "'" + e.getValue() + "'\n");
			}
			log.debug(sb.toString());
		}

		if (json == null) {
			return Promise.pure((Result) badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				final search.SimilarsQuery q = Json.fromJson(json, search.SimilarsQuery.class );
				List<Promise<RecordsList>> groups = new ArrayList<>();
				groups.add(new SimilarSearchTerm().query(q));
				groups.add(new SimilarProviderSearch().query(q));
				groups.add(new SimilarCreatorSearch().query(q));
				
				// TODO here make a list of promises and then a promise of the list... then return it
				
				return ParallelAPICall.<RecordsList> combineResponses(r -> {
					return true;
				} , groups, Priority.FRONTEND);
				
			} catch (Exception e) {
				log.error("",e);
				return Promise.pure((Result) badRequest(e.getMessage()));
			}
		}

		// return Promise.pure( badRequest( "Not implemented yet"));
	}

	/**
	 * read the JSON Query structure, send to the correct backends, merge the results or make continuation
	 * add readability filters for within sources.
	 * @return
	 */
	public static Promise<Result> search2() {
		JsonNode json = request().body().asJson();
		if (log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> e : session().entrySet()) {
				sb.append(e.getKey() + " = " + "'" + e.getValue() + "'\n");
			}
			log.debug(sb.toString());
		}

		if (json == null) {
			return Promise.pure((Result) badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				final search.Query q = Json.fromJson(json, search.Query.class );
				// fix it up or write a deserializer
				return search2internal(q);

				// bad request
			} catch (Exception e) {
				log.error("",e);
				return Promise.pure((Result) badRequest(e.getMessage()));
			}
		}

		// return Promise.pure( badRequest( "Not implemented yet"));
	}

	private static Promise<Result> search2internal(final search.Query q) {
		// Promise.pure( Controller.ok(Json.toJson(sr)));
		final Set<String> commonSupportedFields = prepareQuery(q);
		
		// split the query
		Map<Sources, Query> queries = q.splitBySource();
		
		// create promises
		Iterable<Promise<Response.SingleResponse>> promises = splitQuery(queries);

		if( q.continuation ) {
			// initiate a continuation
			return ChainedSearchResult.create(promises, q );
		} else if( StringUtils.isNotBlank( q.continuationId)) {
			// try to respond with continuation
			return continuedSearch( q.continuationId );
		} else {
			// normal query
			Promise<Response> res = search2internalResponseNormal(q, commonSupportedFields, promises);
			return res.map( (Response r)-> {
				return ok(Json.toJson(r));
			});
		}
	}

	private static Promise<Response> search2internalResponse(final search.Query q) {
		// Promise.pure( Controller.ok(Json.toJson(sr)));
		final Set<String> commonSupportedFields = prepareQuery(q);
		// split the query
		Map<Sources, Query> queries = q.splitBySource();
		// create promises
		Iterable<Promise<Response.SingleResponse>> promises = splitQuery(queries);
		// normal query
		Promise<Response> res = search2internalResponseNormal(q, commonSupportedFields, promises);
		return res;
	}

	private static Promise<Response> search2internalResponseNormal(final search.Query q,
			final Set<String> commonSupportedFields, Iterable<Promise<Response.SingleResponse>> promises) {
		Promise<List<Response.SingleResponse>> allResponses = Promise.sequence(promises,
				ParallelAPICall.Priority.FRONTEND.getExcecutionContext());
		Promise<Response> res = allResponses.map(singleResponses -> {
			Response r = new Response();
			r.query = q;
			for (Response.SingleResponse sr : singleResponses) {
				// TODO change this logic
				sr.pruneFacets(commonSupportedFields);
				r.addSingleResponse(sr);
			}
			r.createAccumulated();
			return r;
		});
		return res;
	}

	private static Iterable<Promise<Response.SingleResponse>> splitQuery(Map<Sources, Query> queries) {
		Iterable<Promise<Response.SingleResponse>> promises =
				queries.entrySet().stream()
				.map( 
						entry -> 
						entry.getKey().getDriver().execute(entry.getValue())
					)
				.collect( Collectors.toList());
		return promises;
	}

	private static Set<String> prepareQuery(final search.Query q) {
		if( q.getPage() > 0 ) {
			q.setPageAndSize( q.getPage(), q.getPageSize());
		} else {
			q.setStartCount( q.getStart(), q.getCount());
		}
		// check if the query needs readability additions for WITHin
		if( q.containsSource( Sources.WITHin)) {
			// add conditions for visibility in WITH
			Query.Clause visible = Query.Clause.create()
					.add( "administrative.isPublic", "true", true );
			for( String userId: effectiveUserIds()) {
				visible.add( "administrative.access.READ", userId, true );
				visible.add( "administrative.access.WRITE", userId, true );
				visible.add( "administrative.access.OWN", userId, true );
			}
			q.addClause( visible.filters());
		}
		// print warnings in the log for fields not known
		q.validateFieldIds();
		
		// remove facet requests that are not relevant
		q.pruneFacets();
		final Set<String> commonSupportedFields = q.commonSupportedFields();
		return commonSupportedFields;
	}

	public static Promise<Result> continuedSearch( String continuationId ) {
		return ChainedSearchResult.search(continuationId);
	}

	public static Result searchSources() {
		
		List<JsonNode> res = new ArrayList<>();
		for( Sources source: Sources.values()) {
			Source s = source.getDriver();
			ObjectNode jsonSource = Json.newObject();
			Set<String> fieldIds = s.supportedFieldIds();
			if( fieldIds == null ) fieldIds = Collections.emptySet();
			for(String fieldId: fieldIds ) {
				if( !excludedFieldnames.contains(fieldId))
					jsonSource.withArray("supportedFields").add( fieldId );
			}
			jsonSource.put("id", source.getID());
			if( s.apiConsole() != null)
				jsonSource.put("apiConsole", s.apiConsole());
			res.add( jsonSource );
		}
		return ok(Json.toJson(res));
	}

	public static Promise<Result> searchwithfilter() {
		JsonNode json = request().body().asJson();
		if (json == null) {
			return Promise.pure((Result)badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				final CommonQuery q = Utils.parseJson(json);
				q.setTypes(q.getTypes());
				q.setEffectiveUserIds(effectiveUserIds());
				Promise<SearchResponse> myResults = getMyResutlsPromise(q);
				play.libs.F.Function<SearchResponse, Result> function =
				new play.libs.F.Function<SearchResponse, Result>() {
				  public Result apply(SearchResponse r) {
				    return ok(Json.toJson(r));
				  }
				};
				return myResults.map(function);

			} catch (Exception e) {
				log.error("",e);
				return Promise.pure((Result)badRequest(e.getMessage()));
			}
		}
	}

	public static Promise<Result> searchwithfilterGET(CommonQuery q) {
//		Form<CommonQuery> qf = Form.form(CommonQuery.class).bindFromRequest();
//		CommonQuery q = qf.get();
		// Parse the query.
		try {
			q.setTypes(Elastic.allTypes);
			q.setEffectiveUserIds(effectiveUserIds());
			Promise<SearchResponse> myResults = getMyResutlsPromise(q);
			play.libs.F.Function<SearchResponse, Result> function = new play.libs.F.Function<SearchResponse, Result>() {
				public Result apply(SearchResponse r) {
					return ok(Json.toJson(r));
				}
			};
			return myResults.map(function);

		} catch (Exception e) {
			log.error("",e);
			return Promise.pure((Result) badRequest(e.getMessage()));
		}
	}
	
	public static Result clearFilters() {
		DAO<FiltersCache> dbhelper = (DAO<FiltersCache>) new DAO(FiltersCache.class);
		dbhelper.dropCollection();
		return ok("");
	}
	
	public static Result getRightsByCategory(String category) {
		return ok(Json.toJson(ListUtils.transform(WithMediaRights.getRightsByCategory(category), (x)->x.toString())));
	}

	public static Promise<Result> getfilters(String source, int days) {
		// Parse the query.

		try {
			// final CommonQuery q = Utils.parseJson(json);
			// q.searchTerm=null;
			// q.setTypes(Elastic.allTypes);
			// q.setEffectiveUserIds(effectiveUserIds());
			//
			if (!Utils.hasAny(source)) {
				source = "ALL";
			}
//			System.out.println("Results for "+source);
			DAO<FiltersCache> dbhelper = (DAO<FiltersCache>) new DAO(FiltersCache.class);
			org.mongodb.morphia.query.Query<FiltersCache> qr = dbhelper.createQuery().field("source").equal(source);
			Stream<FiltersCache> st = dbhelper.find(qr , "source","accumulatedValues", "creationTime");
			Iterator<FiltersCache> iterator = st.iterator();
			boolean update = false;

			ObjectId id=null;
			FiltersCache backupCache = null;
			if (iterator.hasNext()) {
				FiltersCache cache = iterator.next();
				if (cache.isUpToDate(days)) {
					return Promise.pure( Controller.ok(Json.toJson(cache.exportAccumulatedValues())));
				} else {
					backupCache = cache;
					id  = cache.getDbId();
					update = true;
				}
			}
			FiltersCache backupCache1 = backupCache;
			ObjectId idp=id;

			boolean updatef = update;
			Query q = new Query();
			q.addClause(new search.Filter(Fields.anywhere.fieldId(),null));
			q.addClause(new search.Filter(Fields.resourceType.fieldId(), "CulturalObject", true));
			q.setStartCount(0, 1);
			if (Utils.hasAny(source) && !source.equals("ALL")) {
				q.addSource(Sources.getSourceByID(source));
			} else {
				q.addSource(Sources.Europeana, Sources.DPLA);
				q.addSource(Sources.Rijksmuseum);
				q.addSource(Sources.BritishLibrary, Sources.DigitalNZ);
			}
//			System.out.println("from "+q.sources);
			String sourcep = source;
			q.facets = new ArrayList<>(q.commonSupportedFields());
			Promise<Response> myResults = search2internalResponse(q);
			play.libs.F.Function<Response, Result> function = new play.libs.F.Function<Response, Result>() {
				public Result apply(Response r) {
					// save the result.
					if (!Utils.hasInfo(r.accumulatedValues)) {
						return Controller.ok(Json.toJson(backupCache1.exportAccumulatedValues()));
					}
					FiltersCache c = new FiltersCache(sourcep, r.accumulatedValues, System.currentTimeMillis());
					if (updatef) {
						dbhelper.updateField(idp, "accumulatedValues", c.getAccumulatedValues());
						dbhelper.updateField(idp, "creationTime", c.getCreationTime());
					} else {
						dbhelper.makePermanent(c);
					}
					return ok(Json.toJson(r.accumulatedValues));
				}
			};
			return myResults.map(function);

		} catch (Exception e) {
			log.error("", e);
			e.printStackTrace();
			return Promise.pure((Result) badRequest(e.getMessage()));
		}
	}

	public static Result mergeFilters(){
		ArrayList<CommonFilterLogic> merge = new ArrayList<CommonFilterLogic>();
		JsonNode json = request().body().asJson();
		Collection<Collection<CommonFilterResponse>> filters = new ArrayList<>();
		ObjectMapper m = new ObjectMapper();
		try {
			filters = m.readValue(json.toString(),
					new TypeReference<Collection<Collection<CommonFilterResponse>>>() {
			        }
					);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			log.error("",e);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			log.error("",e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("",e);
		}
		for (Collection<CommonFilterResponse> fresponse : filters) {
			if (fresponse!=null){
				FiltersHelper.mergeAux(merge, fresponse);
			}
		}
		return ok(Json.toJson(ListUtils.transform(merge, (x)->x.export())));
	}

	static Promise<SearchResponse> getMyResutlsPromise(final CommonQuery q) {
		Iterable<Promise<SourceResponse>> promises = callSources(q);
		// compose all futures, blocks until all futures finish
		java.util.function.Function<CommonFilterLogic, CommonFilterResponse> f = (CommonFilterLogic o) -> {
			return o.export();
		};

		Promise<List<SourceResponse>> promisesSequence = Promise.sequence(promises);
		return promisesSequence.map(new play.libs.F.Function<Collection<SourceResponse>, SearchResponse>() {
			List<SourceResponse> finalResponses = new ArrayList<SourceResponse>();

			public SearchResponse apply(Collection<SourceResponse> responses) {
				finalResponses.addAll(responses);
				// Logger.debug("Total time for all sources to respond:
				// "
				// + (System.currentTimeMillis()- initTime));
				SearchResponse r1 = new SearchResponse();
				ArrayList<CommonFilterLogic> merge = new ArrayList<CommonFilterLogic>();
				for (SourceResponse sourceResponse : finalResponses) {
					if (sourceResponse!=null){
						sourceResponse.setFilters(ListUtils.transform(sourceResponse.filtersLogic, f));
						FiltersHelper.merge(merge, sourceResponse.filtersLogic);
					}
				}
				r1.filters = ListUtils.transform(merge, f);
				r1.responses = mergeResponses(finalResponses);
				return r1;
			}

			private List<SourceResponse> mergeResponses(List<SourceResponse> finalResponses2) {
				List<SourceResponse> res = new ArrayList<>();
				for (SourceResponse r : finalResponses2) {
					boolean merged = false;
					for (SourceResponse r2 : res) {
						if ((r2!=null) && (r!=null)){
							if (r2.source.equals(r.source)) {
								// merge these 2 and replace r.
								res.remove(r2);
								res.add(r.merge(r2));
								merged = true;
								break;
							}
						}
					}
					if (!merged) {
						res.add(r);
					}
				}
				return res;
			}

		});
	}

//	public static List<String> getTheSources() {
//		Function<ISpaceSource, String> function = (ISpaceSource x) -> {
//			return x.getSourceName();
//		};
//		return ListUtils.transform(ESpaceSources.getESources(), function);
//	}

	private static Iterable<Promise<SourceResponse>> callSources(final CommonQuery q) {
		List<Promise<SourceResponse>> promises = new ArrayList<Promise<SourceResponse>>();
		BiFunction<ISpaceSource, CommonQuery, SourceResponse> methodQuery = (ISpaceSource src, CommonQuery cq) -> {
			try{
				SourceResponse res = src
						.getResults(cq);
					if (res.source == null){
						log.info("Error "+src.getSourceName());
					}
					return res;
			} catch(Exception e){
				log.error("",e);
				return null;
			}
			};
		for (final ISpaceSource src : ESpaceSources.getESources()) {
			if ((q.source == null) || (q.source.size() == 0) || q.source.contains(src.getSourceName().toString())) {
				List<CommonQuery> list = src.splitFilters(q);
				for (CommonQuery commonQuery : list) {
					promises.add(ParallelAPICall.<ISpaceSource, CommonQuery, SourceResponse> createPromise(methodQuery,
							src, commonQuery,Priority.FRONTEND));
				}
			}
		}
		return promises;
	}


	/*
	 * public static Result testsearch() { return buildresult(new
	 * CommonQuery("Zeus")); }
	 *
	 * private static Result buildresult(CommonQuery q) { // q.source =
	 * Arrays.asList(DigitalNZSpaceSource.LABEL); List<SourceResponse> res =
	 * search(q); SearchResponse r1 = new SearchResponse(); r1.responses = res;
	 * ArrayList<CommonFilterLogic> merge = new ArrayList<CommonFilterLogic>();
	 * for (SourceResponse sourceResponse : res) { //
	 * System.out.println(sourceResponse.source + " Filters: " + //
	 * sourceResponse.filters); FiltersHelper.merge(merge,
	 * sourceResponse.filtersLogic); } Function<CommonFilterLogic,
	 * CommonFilterResponse> f = (CommonFilterLogic o) -> { return o.export();
	 * }; List<CommonFilterResponse> merge1 = ListUtils.transform(merge, f); //
	 * System.out.println(" Merged Filters: " + merge1);
	 *
	 * return ok(views.html.testsearch.render(userForm, res, merge1));
	 */

	public static Promise<Result> searchForMLTRelatedItems() {
		JsonNode json = request().body().asJson();
		final search.Query q = Json.fromJson(json, search.Query.class );
		// fix it up or write a deserializer
		if( q.getPage() > 0 ) {
			q.setPageAndSize( q.getPage(), q.getPageSize());
		} else {
			q.setStartCount( q.getStart(), q.getCount());
		}

		try {

			SearchOptions options = new SearchOptions(0, 10);
			options.setScroll(false);
			options.setOffset(q.getStart());
			options.setCount(q.getCount());
			ElasticCoordinator co = new ElasticCoordinator(options);
			SingleResponse sr = co.relatedMLTSearch(q.filters);

			return Promise.pure((Result)ok(Json.toJson(sr)));
		} catch(NullPointerException npe) {

			return Promise.pure((Result)badRequest("Some fields are missing from the provided json"));
		} catch(Exception e) {

			return Promise.pure((Result)internalServerError("We are sorry something happened excecuting your query!"));
		}

	}

	public static Promise<Result> searchForDisMaxRelatedItems() {
		JsonNode json = request().body().asJson();
		final search.Query q = Json.fromJson(json, search.Query.class );
		// fix it up or write a deserializer
		if( q.getPage() > 0 ) {
			q.setPageAndSize( q.getPage(), q.getPageSize());
		} else {
			q.setStartCount( q.getStart(), q.getCount());
		}

		try {

			SearchOptions options = new SearchOptions(0, 10);
			options.setScroll(false);
			options.setOffset(q.getStart());
			options.setCount(q.getCount());
			ElasticCoordinator co = new ElasticCoordinator(options);
			SingleResponse sr = co.relatedDisMaxSearch(q.filters);

			return Promise.pure((Result)ok(Json.toJson(sr)));
		} catch(NullPointerException npe) {

			return Promise.pure((Result)badRequest("Some fields are missing from the provided json"));
		} catch(Exception e) {

			return Promise.pure((Result)internalServerError("We are sorry something happened excecuting your query!"));
		}

	}

	public static Result suggestions() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		try {

			String text = json.get("suggested").asText();
			String field = json.get("field").asText();

			ElasticSearcher suggester = new ElasticSearcher();
			SuggestResponse resp = suggester.searchSuggestions(text, field, new SearchOptions());

			Map<String, List<String>> suggestions = new HashMap<String, List<String>>();
			resp.getSuggest().getSuggestion(text).forEach( (w) ->
			{
				List<String> words = new ArrayList<String>();
				w.forEach( (s) -> words.add(s.getText().toString()) );
				suggestions.put(w.getText().toString(), words);
			});
			result.put("suggestions", Json.toJson(suggestions));

		} catch(Exception e) {
			log.error("Cannot bring suggestions to user", e);
			return internalServerError(e.getMessage());
		}

		return ok(result);
	}


}
