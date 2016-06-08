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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.basicDataTypes.Language;
import model.resources.RecordResource;

public abstract class Annotator {

	protected Language lang;
	
	public static String LANGUAGE = "lang";
	
	public abstract String getName();
	
	public abstract String getService();
	
	public abstract List<Annotation> annotate(String text, Map<String, Object> properties) throws Exception;
	
	public void annotate(RecordResource rr) {	}
	
	private Pattern p = Pattern.compile("(<.*?>)");
	
	protected String strip(String text) {
		Matcher m = p.matcher(text);
		
		StringBuffer sb = new StringBuffer();
		
		int prev = -1;
		while (m.find()) {
			int s = m.start(1);
			int e = m.end(1);
			
			if (prev == -1) {
				sb.append(text.substring(0,s));
			} else {
				sb.append(text.substring(prev, s));
			}
			
			char[] c = new char[e - s];
			Arrays.fill(c, ' ');
			
			sb.append(c);
			
			prev = e;
		}
		
		if (prev == -1) {
			return text;
		} else {
			sb.append(text.substring(prev));
		
			return sb.toString();
		}
	}
	
	public static List<Annotator> getAnnotators(Language lang) {
		List<Annotator> res = new ArrayList<>();
		
		Annotator ann;
		ann = DBPediaSpotlightAnnotator.getAnnotator(lang);
		if (ann != null) {
			res.add(ann);
		}
		
		ann = DictionaryAnnotator.getAnnotator(lang, true);
		if (ann != null) {
			res.add(ann);
		}

		
		return res;
	}
}
