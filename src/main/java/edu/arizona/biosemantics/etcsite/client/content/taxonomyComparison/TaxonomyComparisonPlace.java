package edu.arizona.biosemantics.etcsite.client.content.taxonomyComparison;

import com.google.gwt.place.shared.PlaceTokenizer;

import edu.arizona.biosemantics.etcsite.client.common.HasTaskPlace;
import edu.arizona.biosemantics.etcsite.client.common.RequiresAuthenticationPlace;
import edu.arizona.biosemantics.etcsite.shared.model.Task;

public class TaxonomyComparisonPlace extends HasTaskPlace implements RequiresAuthenticationPlace  {

	public TaxonomyComparisonPlace() {
		super(null);
	}
	
	public TaxonomyComparisonPlace(Task task) {
		super(task);
	}
	
	public static class Tokenizer implements PlaceTokenizer<TaxonomyComparisonPlace> {

		@Override
		public TaxonomyComparisonPlace getPlace(String token) {
			return new TaxonomyComparisonPlace();
		}

		@Override
		public String getToken(TaxonomyComparisonPlace place) {
			return "";
		}

	}

}
