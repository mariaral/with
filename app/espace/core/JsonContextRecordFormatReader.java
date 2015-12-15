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

import model.resources.WithResource;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.utils.JsonContextRecord;

public abstract class JsonContextRecordFormatReader<T extends WithResource> {
	
	protected T object;
	
	public T fillObjectFrom(String text) {
		JsonContextRecord rec = new JsonContextRecord(text);
		return fillObjectFrom(rec);
	}
	
	public T fillObjectFrom(JsonNode text) {
		JsonContextRecord rec = new JsonContextRecord(text);
		return fillObjectFrom(rec);
	}
	
	public abstract T fillObjectFrom(JsonContextRecord text);
	
	public T readObjectFrom(JsonNode text){
		return fillObjectFrom(text);
	}

}
