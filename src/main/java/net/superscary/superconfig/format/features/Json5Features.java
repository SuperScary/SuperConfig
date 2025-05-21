package net.superscary.superconfig.format.features;

/**
 * Features configuration for JSON5 parsing.
 * This class defines which JSON5 features are enabled during parsing.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class Json5Features {
	private final boolean allowComments;
	private final boolean allowTrailingCommas;
	private final boolean allowUnquotedKeys;
	private final boolean allowSingleQuotes;
	private final boolean allowLeadingZeros;
	private final boolean allowLeadingDecimalPoint;
	private final boolean allowMultiLineStrings;

	private Json5Features (Builder builder) {
		this.allowComments = builder.allowComments;
		this.allowTrailingCommas = builder.allowTrailingCommas;
		this.allowUnquotedKeys = builder.allowUnquotedKeys;
		this.allowSingleQuotes = builder.allowSingleQuotes;
		this.allowLeadingZeros = builder.allowLeadingZeros;
		this.allowLeadingDecimalPoint = builder.allowLeadingDecimalPoint;
		this.allowMultiLineStrings = builder.allowMultiLineStrings;
	}

	public boolean isAllowComments () {
		return allowComments;
	}

	public boolean isAllowTrailingCommas () {
		return allowTrailingCommas;
	}

	public boolean isAllowUnquotedKeys () {
		return allowUnquotedKeys;
	}

	public boolean isAllowSingleQuotes () {
		return allowSingleQuotes;
	}

	public boolean isAllowLeadingZeros () {
		return allowLeadingZeros;
	}

	public boolean isAllowLeadingDecimalPoint () {
		return allowLeadingDecimalPoint;
	}

	public boolean isAllowMultiLineStrings () {
		return allowMultiLineStrings;
	}

	public static Builder builder () {
		return new Builder();
	}

	public static class Builder {
		private boolean allowComments = true;
		private boolean allowTrailingCommas = true;
		private boolean allowUnquotedKeys = true;
		private boolean allowSingleQuotes = true;
		private boolean allowLeadingZeros = true;
		private boolean allowLeadingDecimalPoint = true;
		private boolean allowMultiLineStrings = true;

		public Builder allowComments (boolean allowComments) {
			this.allowComments = allowComments;
			return this;
		}

		public Builder allowTrailingCommas (boolean allowTrailingCommas) {
			this.allowTrailingCommas = allowTrailingCommas;
			return this;
		}

		public Builder allowUnquotedKeys (boolean allowUnquotedKeys) {
			this.allowUnquotedKeys = allowUnquotedKeys;
			return this;
		}

		public Builder allowSingleQuotes (boolean allowSingleQuotes) {
			this.allowSingleQuotes = allowSingleQuotes;
			return this;
		}

		public Builder allowLeadingZeros (boolean allowLeadingZeros) {
			this.allowLeadingZeros = allowLeadingZeros;
			return this;
		}

		public Builder allowLeadingDecimalPoint (boolean allowLeadingDecimalPoint) {
			this.allowLeadingDecimalPoint = allowLeadingDecimalPoint;
			return this;
		}

		public Builder allowMultiLineStrings (boolean allowMultiLineStrings) {
			this.allowMultiLineStrings = allowMultiLineStrings;
			return this;
		}

		public Json5Features build () {
			return new Json5Features(this);
		}
	}
} 