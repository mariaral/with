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


package sources.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;

public class JsonNodeUtils {
	
	public static String asString(JsonNode node) {
		if (node!=null && !node.isMissingNode()){
			if (node.isArray()){
				JsonNode jsonNode = node.get(0);
				if (jsonNode!=null)
				return jsonNode.asText();
			} else
				return node.asText();
		}
		return null;
	}
	
	public static MultiLiteral asLiteral(JsonNode node) {
		if (node!=null && !node.isMissingNode()){
			if (node.isArray()){
				node = node.get(0);
			} 
			if (node.isTextual()){
				return new MultiLiteral(Language.DEF,node.asText());
			}
			MultiLiteral res = new MultiLiteral();
			for (Iterator<Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext();) {
				Entry<String, JsonNode> next = iterator.next();
				// TODO ask if the key is a language
//				System.out.println(next);
				//TODO: check transformation from string to lang enum
				res.add(Language.getLanguage(next.getKey()), next.getValue().get(0).asText());
			}
			return res;
		}
		return null;
	}
	
	public static List<String> asStringArray(JsonNode node) {
		if (node!=null && !node.isMissingNode()){
			ArrayList<String> res = new ArrayList<>();
			if (node.isArray()){
				for (int i = 0; i < node.size(); i++) {
					res.add(node.get(i).asText());
				}
			} else{
				res.add(node.asText());
			}
			return res;
		}
		return null;
	}

}
