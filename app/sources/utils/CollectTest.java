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

import java.util.List;

import play.GlobalSettings;
import play.test.FakeApplication;
import play.test.Helpers;
import sources.core.ESpaceSources;
import sources.core.ISpaceSource;
import sources.core.RecordJSONMetadata;
import sources.core.RecordJSONMetadata.Format;

public class CollectTest {
	
	public static void main(String[] args) {
		GlobalSettings s = Helpers.fakeGlobal();
		FakeApplication ap = Helpers.fakeApplication(s);
		Helpers.running(ap, new Runnable() {
			public void run() {
				int k = 0;
				String source = args[k++];
				String id = args[k++];
				ESpaceSources.getESources();
				for (ISpaceSource src : ESpaceSources.getESources()) {
					if (src.LABEL.equals(source)) {
						List<RecordJSONMetadata> l = src.getRecordFromSource(id, null);
						for (RecordJSONMetadata recordJSONMetadata : l) {
							if (recordJSONMetadata.hasFormat(Format.JSON_WITH)) {
								System.out.println("----JSON-WITH BEGIN--------");
								System.out.println(recordJSONMetadata.getJsonContent());
								System.out.println("----JSON-WITH END----------");
								
							}
						}
					}
				}
			}
		});

	}

}
