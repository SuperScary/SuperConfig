package net.superscary.superconfig.format.features;

/**
 * Features for XML format parsing and writing.
 * <p>
 * This class defines the features that can be enabled or disabled
 * when parsing or writing XML configuration files.
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class XmlFeatures {
	private final boolean allowComments;
	private final boolean allowAttributes;
	private final boolean allowCData;
	private final boolean allowNamespaces;
	private final boolean allowProcessingInstructions;

	private XmlFeatures (Builder builder) {
		this.allowComments = builder.allowComments;
		this.allowAttributes = builder.allowAttributes;
		this.allowCData = builder.allowCData;
		this.allowNamespaces = builder.allowNamespaces;
		this.allowProcessingInstructions = builder.allowProcessingInstructions;
	}

	public static Builder builder () {
		return new Builder();
	}

	public boolean isAllowComments () {
		return allowComments;
	}

	public boolean isAllowAttributes () {
		return allowAttributes;
	}

	public boolean isAllowCData () {
		return allowCData;
	}

	public boolean isAllowNamespaces () {
		return allowNamespaces;
	}

	public boolean isAllowProcessingInstructions () {
		return allowProcessingInstructions;
	}

	public static class Builder {
		private boolean allowComments = true;
		private boolean allowAttributes = true;
		private boolean allowCData = true;
		private boolean allowNamespaces = false;
		private boolean allowProcessingInstructions = false;

		public Builder allowComments (boolean allowComments) {
			this.allowComments = allowComments;
			return this;
		}

		public Builder allowAttributes (boolean allowAttributes) {
			this.allowAttributes = allowAttributes;
			return this;
		}

		public Builder allowCData (boolean allowCData) {
			this.allowCData = allowCData;
			return this;
		}

		public Builder allowNamespaces (boolean allowNamespaces) {
			this.allowNamespaces = allowNamespaces;
			return this;
		}

		public Builder allowProcessingInstructions (boolean allowProcessingInstructions) {
			this.allowProcessingInstructions = allowProcessingInstructions;
			return this;
		}

		public XmlFeatures build () {
			return new XmlFeatures(this);
		}
	}
} 