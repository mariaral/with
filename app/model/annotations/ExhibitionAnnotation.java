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


package model.annotations;

import org.mongodb.morphia.annotations.Entity;

import model.annotations.Annotation.AnnotationType;
import model.basicDataTypes.Literal;

@Entity("Αnnotation")
public class ExhibitionAnnotation extends ContextAnnotation<ExhibitionAnnotation.ExhibitionAnnotationBody> {
	
	public ExhibitionAnnotation() {
		super();
		this.annotationType = AnnotationType.valueOf(this.getClass().getSimpleName());
	}

	public static class ExhibitionAnnotationBody extends Annotation.AnnotationBody {
		Literal exhibitionDescription;
		String audioUrl;
		String videoUrl;
		
		public Literal getExhibitionDescription() {
			return exhibitionDescription;
		}
		public void setExhibitionDescription(Literal exhibitionDescription) {
			this.exhibitionDescription = exhibitionDescription;
		}
		public String getAudioUrl() {
			return audioUrl;
		}
		public void setAudioUrl(String audioUrl) {
			this.audioUrl = audioUrl;
		}
		public String getVideoUrl() {
			return videoUrl;
		}
		public void setVideoUrl(String videoUrl) {
			this.videoUrl = videoUrl;
		}
	}

}
