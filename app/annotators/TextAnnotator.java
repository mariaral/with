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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.annotations.Annotation;
import model.annotations.targets.AnnotationTarget;

import org.bson.types.ObjectId;

public interface TextAnnotator {
	
	public List<Annotation> annotate(String text, ObjectId user, AnnotationTarget target, Map<String, Object> properties) throws Exception;
	
	static Pattern p = Pattern.compile("(<.*?>)");

	default String strip(String text) {
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


}
