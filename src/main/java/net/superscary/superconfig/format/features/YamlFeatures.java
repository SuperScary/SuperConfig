package net.superscary.superconfig.format.features;

/**
 * Features configuration for YAML parsing.
 * This class defines which YAML features are enabled during parsing.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class YamlFeatures {
	private final boolean allowAnchors;
	private final boolean allowTags;
	private final boolean allowFlowStyle;
	private final boolean allowBlockStyle;
	private final boolean allowComments;
	private final boolean allowDirectives;

	private YamlFeatures (Builder builder) {
		this.allowAnchors = builder.allowAnchors;
		this.allowTags = builder.allowTags;
		this.allowFlowStyle = builder.allowFlowStyle;
		this.allowBlockStyle = builder.allowBlockStyle;
		this.allowComments = builder.allowComments;
		this.allowDirectives = builder.allowDirectives;
	}

	public boolean isAllowAnchors () {
		return allowAnchors;
	}

	public boolean isAllowTags () {
		return allowTags;
	}

	public boolean isAllowFlowStyle () {
		return allowFlowStyle;
	}

	public boolean isAllowBlockStyle () {
		return allowBlockStyle;
	}

	public boolean isAllowComments () {
		return allowComments;
	}

	public boolean isAllowDirectives () {
		return allowDirectives;
	}

	public static Builder builder () {
		return new Builder();
	}

	public static class Builder {
		private boolean allowAnchors = true;
		private boolean allowTags = true;
		private boolean allowFlowStyle = true;
		private boolean allowBlockStyle = true;
		private boolean allowComments = true;
		private boolean allowDirectives = true;

		public Builder allowAnchors (boolean allow) {
			this.allowAnchors = allow;
			return this;
		}

		public Builder allowTags (boolean allow) {
			this.allowTags = allow;
			return this;
		}

		public Builder allowFlowStyle (boolean allow) {
			this.allowFlowStyle = allow;
			return this;
		}

		public Builder allowBlockStyle (boolean allow) {
			this.allowBlockStyle = allow;
			return this;
		}

		public Builder allowComments (boolean allow) {
			this.allowComments = allow;
			return this;
		}

		public Builder allowDirectives (boolean allow) {
			this.allowDirectives = allow;
			return this;
		}

		public YamlFeatures build () {
			return new YamlFeatures(this);
		}
	}
} 