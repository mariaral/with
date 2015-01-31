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


package espace.core;

import java.util.ArrayList;
import java.util.List;

import play.libs.Json;
import sun.security.pkcs.ParsingException;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.SourceResponse.Lang;

public class Utils {

	public static String spacesFormatQuery(String text) {
		return text.replaceAll("(\\s)+", "+");
	}

	// public static <T> T gsonLoad(String filepath, Class<T> className) {
	// try (Reader reader = new InputStreamReader(new
	// FileInputStream(filepath))) {
	// Gson gson = new GsonBuilder().create();
	// return gson.fromJson(reader, className);
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// return null;
	// }
	//
	// public void gsonWrite(String filepath, Object object) {
	// try (Writer writer = new OutputStreamWriter(new
	// FileOutputStream(filepath))) {
	// Gson gson = new GsonBuilder().create();
	// gson.toJson(object, writer);
	// } catch (UnsupportedEncodingException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	public static CommonQuery parseJson(JsonNode json) throws ParsingException {
		CommonQuery q1 = Json.fromJson(json, CommonQuery.class);
		return q1;
	}

	public static String readAttr(JsonNode json, String string, boolean force) throws ParsingException {
		return readAttr(json, string, force, null);
	}

	public static int readIntAttr(JsonNode json, String string, boolean force) throws ParsingException {
		return readIntAttr(json, string, force, 0);
	}

	public static String readAttr(JsonNode json, String string, boolean force, String def) throws ParsingException {
		String res = json.findPath(string).asText();
		if (res == null) {
			if (force)
				throw new ParsingException("Missing " + string);
			else
				return null;
		}
		return res;
	}

	public static List<String> readArrayAttr(JsonNode json, String string, boolean force) throws ParsingException {
		JsonNode a = json.path(string);
		if (a == null) {
			if (force)
				throw new ParsingException("Missing " + string);
			else
				return null;
		} else {
			List<String> res = new ArrayList<String>(a.size());
			if (a.isArray()) {
				for (int i = 0; i < a.size(); i++) {
					res.add(a.get(i).textValue());
				}
			} else {
				res.add(a.asText());
			}
			return res;
		}
	}

	public static List<Lang> readLangAttr(JsonNode json, String string, boolean force) throws ParsingException {
		JsonNode a = json.path(string);
		if (a == null) {
			if (force)
				throw new ParsingException("Missing " + string);
			else
				return null;
		} else {
			List<Lang> res = new ArrayList<Lang>(a.size());
			if (a.isArray()) {
				for (int i = 0; i < a.size(); i++) {
					res.add(new Lang(null, a.get(i).textValue()));
				}
			} else {
				res.add(new Lang(null, a.asText()));
			}
			return res;
		}
	}

	public static int readIntAttr(JsonNode json, String string, boolean force, int def) throws ParsingException {
		String readAttr = readAttr(json, string, force, "" + def);
		if (readAttr == null || readAttr.equals(""))
			return def;
		// System.out.println("Int? " + readAttr);
		return Integer.parseInt(readAttr);
	}

}
