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


package model.basicDataTypes;

import sources.core.Utils;

public class MultiLiteralOrResource extends MultiLiteral {

	public MultiLiteralOrResource() {
		super();
	}

	public MultiLiteralOrResource(Language lang, String label) {
		super(lang, label);
	}

	public MultiLiteralOrResource(String label) {
		if (Utils.isValidURL(label)) {
			addURI(label);
		}
		addLiteral(label);
	}

	public void addLiteral(Language lang, String value) {
		if (lang.equals(Language.DEF) && Utils.isValidURL(value))
			addURI(value);
		else
			super.addLiteral(lang, value);
	}

	public void addURI(String uri) {
		add(LiteralOrResource.URI, uri);
	}

}