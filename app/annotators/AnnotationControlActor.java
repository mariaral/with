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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import notifications.AnnotationNotification;
import notifications.Notification.Activity;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.types.ObjectId;

import play.Play;
import play.libs.Akka;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.AnnotationController;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.NotificationCenter;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import actors.TokenLoginActor.TokenCreateMessage;
import actors.TokenLoginActor.TokenResponseMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class AnnotationControlActor extends UntypedActor {

	private static Timeout timeout = new Timeout(Duration.create(5, "seconds"));
	public static String ip = "";

	static {
		try {
			ip = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + Play.application().configuration().getString("http.port");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	private String requestId;
	private ObjectId recordId; 
	private ObjectId userId;
	private boolean record;
	
	private Map<RequestAnnotator.Descriptor, List<ObjectNode>> requestMap;
	private Map<RequestAnnotator.Descriptor, Integer> requestCount;
	private Set<String> requestIds;
	
	private Random rand; 
	
	private ActorSelection tokenLoginActor;
	
	public AnnotationControlActor(String requestId, ObjectId recordId, ObjectId userId, boolean record) {
		this.requestId = requestId;
		this.recordId = recordId;
		this.userId = userId;
		this.record = record;
		
		requestMap = new HashMap<>();
		requestCount = new HashMap<>();
		requestIds = new HashSet<>();
		
		rand = new Random();
		
		tokenLoginActor = Akka.system().actorSelection("/user/tokenLoginActor");
	}

	
	@Override
	public void onReceive(Object msg) {
		if (msg instanceof AnnotateText) {
//			System.out.println("***************** AnnotateText");
			AnnotateText atm = (AnnotateText)msg;

			ActorSelection annotator = atm.annotator.getAnnotator(atm.lang);
			if (annotator != null) {
				String mid = (System.currentTimeMillis() + Math.abs(rand.nextLong())) + "" + Math.abs(rand.nextLong());
				requestIds.add(mid);
				annotator.tell(new TextAnnotator.Annotate(atm.userId, atm.text, atm.target, atm.props, requestId, mid), ActorRef.noSender());
			}
			
		} else if (msg instanceof AnnotateTextDone) {
//			System.out.println("***************** AnnotationTextDone");
			requestIds.remove(((AnnotateTextDone)msg).messageId);
			
			sendFinishNotification();

		} else if (msg instanceof AnnotateRequest) {
//			System.out.println("***************** AnnotateRequest");
			handleRequestAnnotateMessage((AnnotateRequest)msg);
			
		} else if (msg instanceof AnnotateRequestPartialResult) {
//			System.out.println("***************** AnnotateRequestPartialResult");
			System.out.println("ANN 1" + Json.toJson(AnnotationController.getAnnotationFromJson(((AnnotateRequestPartialResult) msg).annotation, userId)));
			AnnotationController.addAnnotation(AnnotationController.getAnnotationFromJson(((AnnotateRequestPartialResult) msg).annotation, userId), userId);
			
		} else if (msg instanceof AnnotateRequestsEnd) {
//			System.out.println("***************** AnnotateRequestsEnd");
			try {
				sendPendingRequests();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			sendFinishNotification();
			
		} else if (msg instanceof AnnotateRequestBulkAnswered) {
//			System.out.println("***************** AnnotateRequestAnswered");
			requestIds.remove(((AnnotateRequestBulkAnswered)msg).requestId);
			
			sendFinishNotification();
		}
	}

	private void sendFinishNotification() {
		if (requestIds.size() == 0) {
	        AnnotationNotification notification = new AnnotationNotification();
	
			if (record) {
	            notification.setActivity(Activity.RECORD_ANNOTATING_COMPLETED);
			} else {
	            notification.setActivity(Activity.COLLECTION_ANNOTATING_COMPLETED);
			}
	
			notification.setOpenedAt(new Timestamp(new Date().getTime()));
	        notification.setResource(recordId);
	        notification.setReceiver(userId);
//	        DB.getNotificationDAO().makePermanent(notification);
	        
	        NotificationCenter.sendNotification(notification);
	        
	        getContext().stop(getSelf());
		}
	}
	
	private void sendPendingRequests() {
		for (Map.Entry<RequestAnnotator.Descriptor, List<ObjectNode>> entry : requestMap.entrySet()) {
			sendRequests(entry.getKey(), entry.getValue());
		}
	}
	
	private void sendRequests(RequestAnnotator.Descriptor ad, List<ObjectNode> list) {
		if (list.size() == 0) {
			return;
		}
		
		int count = requestCount.get(ad);
		
		try {
			TokenCreateMessage tokenCreateMsg = new TokenCreateMessage(this.userId);
			Future<Object> future2 = Patterns.ask(tokenLoginActor, tokenCreateMsg, timeout);
			
			TokenResponseMessage trm2 = (TokenResponseMessage)Await.result(future2, timeout.duration());
			String rid = requestId + "Z" + count;
			
			ObjectNode json = ad.createMessage(rid, list, trm2.token);
			
			HttpClient client = HttpClientBuilder.create().build();

			HttpPost request = new HttpPost(ad.getService());
			request.setHeader("content-type", "application/json");
			
			request.setEntity(new StringEntity(json.toString()));
		
//			System.out.println(">>>B " + ad.getService());
//			System.out.println(">>>B " + json.toString());
		
			if (client.execute(request).getStatusLine().getStatusCode() == 200) {
//				System.out.println(">>> SEND OK");
				requestCount.put(ad, ++count);
				requestIds.add(rid);
			}

			list.clear();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void handleRequestAnnotateMessage(AnnotateRequest atm)	{
		TokenCreateMessage tokenCreateMsg = new TokenCreateMessage(atm.userId);
		
		for (String url : atm.urls) {
		
			try {
				Future<Object> future1 = Patterns.ask(tokenLoginActor, tokenCreateMsg, timeout);
				TokenResponseMessage trm1 = (TokenResponseMessage)Await.result(future1, timeout.duration());

				ObjectNode json = atm.annotator.createDataEntry(url, atm.target.getRecordId().toString(), trm1.token);

				List<ObjectNode> list = requestMap.get(atm.annotator);
				if (list == null) {
					list = new ArrayList<>();
					requestMap.put(atm.annotator, list);
					requestCount.put(atm.annotator, 0);
				}
				list.add(json);
				
				if (list.size() == atm.annotator.getDataLimit()) {
					sendRequests(atm.annotator, list);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class AnnotateText {
		public ObjectId userId;
		public String text;
		public AnnotationTarget target;
		public Map<String, Object> props;
		public TextAnnotator.Descriptor annotator;
		public Language lang;

		public AnnotateText(ObjectId userId, String text, AnnotationTarget target, Map<String, Object> props, TextAnnotator.Descriptor annotator, Language lang) {
			this.userId = userId;
			this.text = text;
			this.target = target;
			this.props = props;
			this.annotator = annotator;
			this.lang = lang;
		}
	}
	
	public static class AnnotateRequest {
		public ObjectId userId;
		public String[] urls;
		public AnnotationTarget target;
		public Map<String, Object> props;
		public RequestAnnotator.Descriptor annotator;

		public AnnotateRequest(ObjectId userId, String[] urls, AnnotationTarget target, Map<String, Object> props, RequestAnnotator.Descriptor annotator) {
			this.userId = userId;
			this.urls = urls;
			this.target = target;
			this.props = props;
			this.annotator = annotator;
		}
	}
	
	public static class AnnotateTextDone {
		public String messageId;
		
		public AnnotateTextDone(String messageId) {
			this.messageId = messageId;
		}

	}

	public static class AnnotateRequestBulkAnswered {
		public String requestId;
		
		public AnnotateRequestBulkAnswered(String requestId) {
			this.requestId = requestId;
		}
	}

	public static class AnnotateRequestsEnd {
	}

	public static class AnnotateRequestPartialResult {
		public JsonNode annotation;
		
		public AnnotateRequestPartialResult(JsonNode annotation) {
			this.annotation = annotation;
		}
	}
	

}
