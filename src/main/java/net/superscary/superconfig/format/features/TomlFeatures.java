package net.superscary.superconfig.format.features;

/**
 * Features configuration for TOML parsing.
 * This class defines which TOML features are enabled during parsing.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class TomlFeatures {
	private final boolean allowMultilineStrings;
	private final boolean allowLiteralStrings;
	private final boolean allowMultilineTables;
	private final boolean allowInlineTables;
	private final boolean allowArrayOfTables;

	private TomlFeatures (Builder builder) {
		this.allowMultilineStrings = builder.allowMultilineStrings;
		this.allowLiteralStrings = builder.allowLiteralStrings;
		this.allowMultilineTables = builder.allowMultilineTables;
		this.allowInlineTables = builder.allowInlineTables;
		this.allowArrayOfTables = builder.allowArrayOfTables;
	}

	public boolean isAllowMultilineStrings () {
		return allowMultilineStrings;
	}

	public boolean isAllowLiteralStrings () {
		return allowLiteralStrings;
	}

	public boolean isAllowMultilineTables () {
		return allowMultilineTables;
	}

	public boolean isAllowInlineTables () {
		return allowInlineTables;
	}

	public boolean isAllowArrayOfTables () {
		return allowArrayOfTables;
	}

	public static Builder builder () {
		return new Builder();
	}

	public static class Builder {
		private boolean allowMultilineStrings = true;
		private boolean allowLiteralStrings = true;
		private boolean allowMultilineTables = true;
		private boolean allowInlineTables = true;
		private boolean allowArrayOfTables = true;

		public Builder allowMultilineStrings (boolean allow) {
			this.allowMultilineStrings = allow;
			return this;
		}

		public Builder allowLiteralStrings (boolean allow) {
			this.allowLiteralStrings = allow;
			return this;
		}

		public Builder allowMultilineTables (boolean allow) {
			this.allowMultilineTables = allow;
			return this;
		}

		public Builder allowInlineTables (boolean allow) {
			this.allowInlineTables = allow;
			return this;
		}

		public Builder allowArrayOfTables (boolean allow) {
			this.allowArrayOfTables = allow;
			return this;
		}

		public TomlFeatures build () {
			return new TomlFeatures(this);
		}
	}
} 