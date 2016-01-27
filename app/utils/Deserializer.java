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


package utils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.ExhibitionRecord;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Deserializer {

	public static class ExhibitionRecordDeserializer extends
			JsonDeserializer<ExhibitionRecord> {

		@Override
		public ExhibitionRecord deserialize(JsonParser annot,
				DeserializationContext arg1) throws IOException,
				JsonProcessingException {
			ExhibitionRecord exhRec = new ExhibitionRecord();
			exhRec.setExtraDescription(annot.getValueAsString());
			return exhRec;
		}
	}

	public static class AccessDeserializer extends JsonDeserializer<Access> {

		@Override
		public Access deserialize(JsonParser accessOrdinal,
				DeserializationContext arg1) throws IOException,
				JsonProcessingException {
			return Access.values()[accessOrdinal.readValueAs(Integer.class)];
		}

	}

	public static class WithAccessDeserializer extends
			JsonDeserializer<WithAccess> {

		@Override
		public WithAccess deserialize(JsonParser accessString,
				DeserializationContext arg1) throws IOException,
				JsonProcessingException {
			WithAccess rights = new WithAccess();
			TreeNode treeNode = accessString.readValueAsTree();
			ObjectNode isPublic = (ObjectNode) treeNode.get("isPublic");
			if (isPublic != null) {
				rights.setIsPublic(isPublic.asBoolean());
			}
			ObjectNode jsonAcl = (ObjectNode) treeNode.get("acl");
			if (jsonAcl != null) {
				ArrayList<AccessEntry> acl = jsonAcl.traverse().readValueAs(
						new TypeReference<AccessEntry>() {
						});
				rights.setAcl(acl);
				// if (rightsMap != null)
				// for(Entry<String, Integer> e : rightsMap.entrySet())
				// rights.put(new ObjectId(e.getKey()),
				// Access.values()[e.getValue()]);
			}
			return rights;
		}
	}

	public static class MultiLiteralDesiarilizer extends
			JsonDeserializer<MultiLiteral> {

		@Override
		public MultiLiteral deserialize(JsonParser string,
				DeserializationContext arg1) throws IOException,
				JsonProcessingException {
			Map<String, List<String>> outMap = new HashMap<String, List<String>>();
			Map<String, String[]> map = string
					.readValueAs(new TypeReference<Map<String, String[]>>() {
					});
			for (Entry<String, String[]> e : map.entrySet()) {
				if (Language.contains(e.getKey())) {
					outMap.put(e.getKey(), Arrays.asList(e.getValue()));
				}
			}
			return (MultiLiteral) outMap;
		}
	}

	/*
	 * public static class MultiLiteralOrResourceDesiarilizer extends
	 * JsonDeserializer<MultiLiteralOrResource> {
	 * 
	 * @Override public MultiLiteralOrResource deserialize(JsonParser string,
	 * DeserializationContext arg1) { Map<String, String[]> map; try { map =
	 * string .readValueAs(new TypeReference<Map<String, String[]>>() { }); }
	 * catch (JsonProcessingException e1) { try { return new
	 * MultiLiteralOrResource(string.getText()); } catch (Exception e2) { return
	 * null; } } catch (IOException e1) { return null; } MultiLiteralOrResource
	 * outMap = new MultiLiteralOrResource(); for (Entry<String, String[]> e :
	 * map.entrySet()) { if (Language.contains(e.getKey())) {
	 * outMap.addMultiLiteral(Language.valueOf(e.getKey()),
	 * Arrays.asList(e.getValue())); } else if
	 * (e.getKey().equals(LiteralOrResource.URI)) { LiteralOrResource out = new
	 * MultiLiteralOrResource(); out.addURI(e.getValue()); return out; } else {
	 * return new MultiLiteralOrResource(e.getValue()); } } return null;
	 * 
	 * } }
	 */
	public static class LiteralDesiarilizer extends JsonDeserializer<Literal> {

		@Override
		public Literal deserialize(JsonParser string,
				DeserializationContext arg1) {
			Map<String, String> map;
			Literal out = new Literal();
			try {
				map = string
						.readValueAs(new TypeReference<Map<String, String>>() {
						});
			} catch (JsonProcessingException e1) {
				try {
					out = new Literal(string.getText());
					out.fillDEF();
					return out;
				} catch (Exception e2) {
					return null;
				}
			} catch (IOException e1) {
				return null;
			}
			for (Entry<String, String> e : map.entrySet()) {
				if (Language.contains(e.getKey())) {
					out.addLiteral(Language.valueOf(e.getKey()), e.getValue());
				}
			}
			out.fillDEF();
			return out;

		}
	}

	public static class LiteralOrResourceDesiarilizer extends
			JsonDeserializer<LiteralOrResource> {

		@Override
		public LiteralOrResource deserialize(JsonParser string,
				DeserializationContext arg1) {
			Map<String, String> map;
			LiteralOrResource out = new LiteralOrResource();
			try {
				map = string
						.readValueAs(new TypeReference<Map<String, String>>() {
						});
			} catch (JsonProcessingException e1) {
				try {
					out = new LiteralOrResource(string.getText());
					out.fillDEF();
					return out;
				} catch (Exception e2) {
					return null;
				}
			} catch (IOException e1) {
				return null;
			}
			for (Entry<String, String> e : map.entrySet()) {
				if (Language.contains(e.getKey())) {
					out.addLiteral(Language.valueOf(e.getKey()), e.getValue());
				} else if (e.getKey().equals(LiteralOrResource.URI)) {
					out.addURI(e.getValue());
				}
			}
			out.fillDEF();
			return out;
		}
	}

	public static class AccessMapDeserializer extends
			JsonDeserializer<Map<ObjectId, Access>> {

		@Override
		public Map<ObjectId, Access> deserialize(JsonParser rights_string,
				DeserializationContext arg1) throws IOException,
				JsonProcessingException {
			Map<String, Integer> rights_map = rights_string
					.readValueAs(new TypeReference<Map<String, Integer>>() {
					});
			Map<ObjectId, Access> r = new HashMap<ObjectId, Access>();
			for (Entry<String, Integer> e : rights_map.entrySet()) {
				r.put(new ObjectId(e.getKey()), Access.values()[e.getValue()]);
			}
			return r;
		}
	}

	public static class DateDeserializer extends JsonDeserializer<Object> {

		@Override
		public Object deserialize(JsonParser date, DeserializationContext arg1)
				throws IOException, JsonProcessingException {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			try {
				return sdf.parse(date.getValueAsString());
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
}
