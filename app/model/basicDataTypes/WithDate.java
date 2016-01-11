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

import java.util.Date;

import model.ExampleDataModels.LiteralOrResource;
import model.ExampleDataModels.LiteralOrResource.ResourceType;

/**
 * Capture accurate and inaccurate dates in a visualisable way. Enable search for year.
 * This is a point in time. If you mean a timespan, use different class.
 */
public class WithDate {
	Date isoDate;
	//facet
	//year should be filled in whenever possible
	//100 bc is translated to -100
	int year;
	
	// controlled expression of an epoch "stone age", "renaissance", "16th century"
	LiteralOrResource epoch;
	
	// if the year is not accurate, give the inaccuracy here( 0- accurate)
	int approximation;
	
	// ontology based time 
	String uri;
	ResourceType uriType;
	
	//mandatory, other fields are extracted from that
	String free;
	
	public void setDate(String free){
		this.free = free;
		//code to init the other Date representations
	}
}
