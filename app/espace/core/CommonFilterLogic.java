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
import java.util.Collection;
import java.util.HashMap;

public class CommonFilterLogic<a> implements Cloneable {

	private HashMap<String, ValueCount> counts;

	public CommonFilterResponse data = new CommonFilterResponse();

	public CommonFilterLogic() {
		super();
		counts = new HashMap<String, ValueCount>();
	}

	public void addValue(String value, int count) {
		if (value != null) {
			// System.out.println(filterName + " Added " + value);
			getOrSet(value).add(count);
		}
	}

	public void addValueCounts(Collection<ValueCount> value) {
		if (value != null) {
			for (ValueCount valueCount : value) {
				getOrSet(valueCount.value).add(valueCount.count);
			}
		}
	}

	private ValueCount getOrSet(String value) {
		if (!counts.containsKey(value)) {
			counts.put(value, new ValueCount(value, 0));
		}
		return counts.get(value);
	}

	public void addValue(Collection<String> values, int count) {
		for (String string : values) {
			addValue(string, count);
		}
	}

	public static CommonFilterLogic typeFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.TYPE_ID;
		r.data.filterName = CommonFilters.TYPE_NAME;
		return r;
	}

	public static CommonFilterLogic providerFilter() {
		CommonFilterLogic r = new CommonFilterLogic();
		r.data.filterID = CommonFilters.PROVIDER_ID;
		r.data.filterName = CommonFilters.PROVIDER_NAME;
		return r;
	}

	@Override
	public String toString() {
		return "Logic [filterName=" + data.filterName + ", filterID=" + data.filterID + ", suggestedValues="
				+ counts.values() + "]";
	}

	public CommonFilterResponse export() {
		data.suggestedValues = new ArrayList<>();
		data.suggestedValues.addAll(counts.values());
		return data;
	}

	@Override
	protected CommonFilterLogic clone() {
		CommonFilterLogic res = new CommonFilterLogic();
		res.data.filterID = this.data.filterID;
		res.data.filterName = this.data.filterName;
		res.counts = (HashMap<String, ValueCount>) counts.clone();
		return res;
	}

	public Collection<ValueCount> values() {
		return counts.values();
	}
}