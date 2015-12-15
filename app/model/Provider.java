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


package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class Provider {
	
	public enum Sources {
		Mint("Mint"), Europeana("Europeana"), UploadedByUser("UpladedByUser"), 
		BritishLibrary("The British Library"), DDB("DDB"),
		DigitalNZ("DigitalNZ"), DPLA("DPLA"), EFashion("EFashion"), NLA("NLA"),
		Rijksmuseum("Rijksmuseum");

		private final String text;

	    private Sources(final String text) {
	        this.text = text;
	    }
	    @Override
	    public String toString() {
	        return text;
	    }

	}
	
	public String providerName;
	public String recordId;
	public String recordUrl;
	
	public Provider(String providerName) {
		this.providerName = providerName;
	}
	
	public Provider(String providerName, String recordId, String recordUrl) {
		this.providerName = providerName;
		this.recordId = recordId;
		this.recordUrl = recordUrl;
	}

}
