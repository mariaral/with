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


package annotators;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.MotivationType;
import model.annotations.bodies.AnnotationBodyTagging;
import model.annotations.selectors.PropertyTextFragmentSelector;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import actors.TokenLoginActor;
import actors.ApiKeyManager.Access;
import actors.TokenLoginActor.UserIdResponseMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.bson.types.ObjectId;

import play.libs.Akka;
import play.libs.Json;

public class DBPediaAnnotator extends TextAnnotator {

	private static String DPBEDIA_ENDPOINT = "http://zenon.image.ece.ntua.gr:8890/sparql";
	
	public static Map<Language, String> serverMap = new HashMap<>();
	static {
		serverMap.put(Language.EN, "http://spotlight.sztaki.hu:2222/rest/annotate");
		serverMap.put(Language.FR, "http://spotlight.sztaki.hu:2225/rest/annotate");
		serverMap.put(Language.DE, "http://spotlight.sztaki.hu:2226/rest/annotate");
		serverMap.put(Language.RU, "http://spotlight.sztaki.hu:2227/rest/annotate");
		serverMap.put(Language.PT, "http://spotlight.sztaki.hu:2228/rest/annotate");
		serverMap.put(Language.HU, "http://spotlight.sztaki.hu:2229/rest/annotate");
		serverMap.put(Language.IT, "http://spotlight.sztaki.hu:2230/rest/annotate");
		serverMap.put(Language.ES, "http://spotlight.sztaki.hu:2231/rest/annotate");
		serverMap.put(Language.NL, "http://spotlight.sztaki.hu:2232/rest/annotate");
		serverMap.put(Language.TR, "http://spotlight.sztaki.hu:2235/rest/annotate");
	}
	
	public static AnnotatorDescriptor descriptor = new Descriptor();
	
	public static class Descriptor implements TextAnnotator.Descriptor {

		@Override
		public String getName() {
			return "DBPedia Spotlight";
		}

		@Override
		public AnnotatorType getType() {
			return AnnotatorType.NER;
		}

		private static Set<Language> created = new HashSet<>();

	    public ActorSelection getAnnotator(Language lang, boolean cs) {
	    	if (!serverMap.containsKey(lang)) {
				return null;
			}
	    	
	    	String actorName = "DBPediaAnnotator-" + lang.getName();

	    	if (!created.contains(lang)) {
				synchronized (DBPediaAnnotator.class) {
			    	if (created.add(lang)) {
						Akka.system().actorOf( Props.create(DBPediaAnnotator.class, lang), actorName);
					}
				}
			}
		
			return Akka.system().actorSelection("user/" + actorName);
	    }
		
	}
	
    
	private String service;
	private AnnotatorDescriptor descr; 
    
    public DBPediaAnnotator(Language lang) {
    	this.lang = lang;
    	this.service = serverMap.get(lang);
    	this.descr = new DBPediaAnnotator.Descriptor();
    }

	public List<Annotation> annotate(String text, ObjectId user, AnnotationTarget target, Map<String, Object> props) throws Exception {
		text = TextAnnotator.strip(text);
		
		List<Annotation> res = new ArrayList<>();
		
		HttpClient client = HttpClientBuilder.create().build();
		
		HttpPost request = new HttpPost(service);
		request.setHeader("content-type", "application/x-www-form-urlencoded");
		request.setHeader("accept", "application/json");
		
		text = URLEncoder.encode(text, "UTF-8");
		
		request.setEntity(new StringEntity("text=" + text, ContentType.create("application/x-www-form-urlencoded", Charset.forName("UTF-8"))));

		HttpResponse response = client.execute(request);
		
//		int responseCode = response.getStatusLine().getStatusCode();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
    
		StringBuffer resx = new StringBuffer();
	    String line;
	       
	    while ((line = br.readLine()) != null) {
	    	resx.append(line + "\n");
	    }
	    br.close();
	    
	    JsonNode root = Json.parse(resx.toString());
	    
	    JsonNode resources = root.get("Resources");
	    if (resources != null) {
	    	for (Iterator<JsonNode> iter = resources.elements(); iter.hasNext();) {
	    		JsonNode resource = iter.next();
	    		
	    		String URI = resource.get("@URI").asText();
//	    		String types = resource.get("@types").asText();
	    		String surfaceForm = resource.get("@surfaceForm").asText();
	    		int offset = resource.get("@offset").asInt();
	    		double score = resource.get("@similarityScore").asDouble();

	    		String label = "";
	    		Language lang = Language.UNKNOWN;

	    		try { 
		    		String query;
		    		if (URI.startsWith("http://dbpedia.org")) {
		    			query = "select ?label where {<" + URI + "> <http://www.w3.org/2000/01/rdf-schema#label> ?label}";
		    		} else {
		    			query = "select ?label where { ?x <http://www.w3.org/2000/01/rdf-schema#label> ?label . ?x <http://www.w3.org/2002/07/owl#sameAs> <" + URI + "> }";
		    		}
	
		    	    QueryExecution qe = QueryExecutionFactory.sparqlService(DPBEDIA_ENDPOINT, QueryFactory.create(query, Syntax.syntaxSPARQL));
		    		ResultSet rs = qe.execSelect();
	
		    		if (rs.hasNext()) {
		    			QuerySolution sol = rs.next();
		    			
		    			List<String> vars = rs.getResultVars();
		    			
		    			RDFNode s = sol.get(vars.get(0));
		    			Literal literal = s.asLiteral();
		    			lang = Language.getLanguage(literal.getLanguage());
		    			label = literal.getString();
		    		}
	    		} catch (Exception ex) {
	    			ex.printStackTrace();
	    		}
	    		
	    		Annotation<AnnotationBodyTagging> ann = new Annotation<>();
	    		
	    		AnnotationBodyTagging annBody = new AnnotationBodyTagging();
	    		annBody.setUri(URI);
	    		annBody.setUriVocabulary("dbr");
	    		
	    		MultiLiteral ml = new MultiLiteral(lang, label);
	    		ml.fillDEF();
	    		
	    		annBody.setLabel(ml);

	    		AnnotationTarget annTarget = (AnnotationTarget) target.clone();
	    		
	    		PropertyTextFragmentSelector selector = (PropertyTextFragmentSelector)annTarget.getSelector();
	    		selector.setStart(offset);
	    		selector.setEnd(offset + surfaceForm.length());

	    		annTarget.setSelector(selector);

	    		ArrayList<AnnotationAdmin> admins  = new ArrayList<>();
	    		AnnotationAdmin admin = new Annotation.AnnotationAdmin();
	    		admin.setGenerator(descr.getName());
	    		admin.setGenerated(new Date());
	    		admin.setConfidence(score);
	    		
	    		admins.add(admin);
	    		
	    		ann.setBody(annBody);
	    		ann.setTarget(annTarget);
	    		ann.setAnnotators(admins);
	    		ann.setMotivation(MotivationType.Tagging);
	    		
	    		res.add(ann);
	    	}
	    }
	    
	    return res;
	    
	}
}
