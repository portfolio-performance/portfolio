// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: client.proto

package name.abuchen.portfolio.model.proto.v1;

/**
 * Protobuf type {@code name.abuchen.portfolio.PExchangeRateTimeSeries}
 */
public final class PExchangeRateTimeSeries extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:name.abuchen.portfolio.PExchangeRateTimeSeries)
    PExchangeRateTimeSeriesOrBuilder {
private static final long serialVersionUID = 0L;
  // Use PExchangeRateTimeSeries.newBuilder() to construct.
  private PExchangeRateTimeSeries(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private PExchangeRateTimeSeries() {
    baseCurrency_ = "";
    termCurrency_ = "";
    exchangeRates_ = java.util.Collections.emptyList();
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new PExchangeRateTimeSeries();
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return name.abuchen.portfolio.model.proto.v1.ClientProtos.internal_static_name_abuchen_portfolio_PExchangeRateTimeSeries_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return name.abuchen.portfolio.model.proto.v1.ClientProtos.internal_static_name_abuchen_portfolio_PExchangeRateTimeSeries_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries.class, name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries.Builder.class);
  }

  public static final int BASECURRENCY_FIELD_NUMBER = 1;
  @SuppressWarnings("serial")
  private volatile java.lang.Object baseCurrency_ = "";
  /**
   * <code>string baseCurrency = 1;</code>
   * @return The baseCurrency.
   */
  @java.lang.Override
  public java.lang.String getBaseCurrency() {
    java.lang.Object ref = baseCurrency_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      baseCurrency_ = s;
      return s;
    }
  }
  /**
   * <code>string baseCurrency = 1;</code>
   * @return The bytes for baseCurrency.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getBaseCurrencyBytes() {
    java.lang.Object ref = baseCurrency_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      baseCurrency_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int TERMCURRENCY_FIELD_NUMBER = 2;
  @SuppressWarnings("serial")
  private volatile java.lang.Object termCurrency_ = "";
  /**
   * <code>string termCurrency = 2;</code>
   * @return The termCurrency.
   */
  @java.lang.Override
  public java.lang.String getTermCurrency() {
    java.lang.Object ref = termCurrency_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      termCurrency_ = s;
      return s;
    }
  }
  /**
   * <code>string termCurrency = 2;</code>
   * @return The bytes for termCurrency.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getTermCurrencyBytes() {
    java.lang.Object ref = termCurrency_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      termCurrency_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int EXCHANGERATES_FIELD_NUMBER = 3;
  @SuppressWarnings("serial")
  private java.util.List<name.abuchen.portfolio.model.proto.v1.PExchangeRate> exchangeRates_;
  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
   */
  @java.lang.Override
  public java.util.List<name.abuchen.portfolio.model.proto.v1.PExchangeRate> getExchangeRatesList() {
    return exchangeRates_;
  }
  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
   */
  @java.lang.Override
  public java.util.List<? extends name.abuchen.portfolio.model.proto.v1.PExchangeRateOrBuilder> 
      getExchangeRatesOrBuilderList() {
    return exchangeRates_;
  }
  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
   */
  @java.lang.Override
  public int getExchangeRatesCount() {
    return exchangeRates_.size();
  }
  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
   */
  @java.lang.Override
  public name.abuchen.portfolio.model.proto.v1.PExchangeRate getExchangeRates(int index) {
    return exchangeRates_.get(index);
  }
  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
   */
  @java.lang.Override
  public name.abuchen.portfolio.model.proto.v1.PExchangeRateOrBuilder getExchangeRatesOrBuilder(
      int index) {
    return exchangeRates_.get(index);
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(baseCurrency_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, baseCurrency_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(termCurrency_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, termCurrency_);
    }
    for (int i = 0; i < exchangeRates_.size(); i++) {
      output.writeMessage(3, exchangeRates_.get(i));
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(baseCurrency_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, baseCurrency_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(termCurrency_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, termCurrency_);
    }
    for (int i = 0; i < exchangeRates_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(3, exchangeRates_.get(i));
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries)) {
      return super.equals(obj);
    }
    name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries other = (name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries) obj;

    if (!getBaseCurrency()
        .equals(other.getBaseCurrency())) return false;
    if (!getTermCurrency()
        .equals(other.getTermCurrency())) return false;
    if (!getExchangeRatesList()
        .equals(other.getExchangeRatesList())) return false;
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + BASECURRENCY_FIELD_NUMBER;
    hash = (53 * hash) + getBaseCurrency().hashCode();
    hash = (37 * hash) + TERMCURRENCY_FIELD_NUMBER;
    hash = (53 * hash) + getTermCurrency().hashCode();
    if (getExchangeRatesCount() > 0) {
      hash = (37 * hash) + EXCHANGERATES_FIELD_NUMBER;
      hash = (53 * hash) + getExchangeRatesList().hashCode();
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code name.abuchen.portfolio.PExchangeRateTimeSeries}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:name.abuchen.portfolio.PExchangeRateTimeSeries)
      name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeriesOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return name.abuchen.portfolio.model.proto.v1.ClientProtos.internal_static_name_abuchen_portfolio_PExchangeRateTimeSeries_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return name.abuchen.portfolio.model.proto.v1.ClientProtos.internal_static_name_abuchen_portfolio_PExchangeRateTimeSeries_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries.class, name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries.Builder.class);
    }

    // Construct using name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      baseCurrency_ = "";
      termCurrency_ = "";
      if (exchangeRatesBuilder_ == null) {
        exchangeRates_ = java.util.Collections.emptyList();
      } else {
        exchangeRates_ = null;
        exchangeRatesBuilder_.clear();
      }
      bitField0_ = (bitField0_ & ~0x00000004);
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return name.abuchen.portfolio.model.proto.v1.ClientProtos.internal_static_name_abuchen_portfolio_PExchangeRateTimeSeries_descriptor;
    }

    @java.lang.Override
    public name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries getDefaultInstanceForType() {
      return name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries.getDefaultInstance();
    }

    @java.lang.Override
    public name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries build() {
      name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries buildPartial() {
      name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries result = new name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries(this);
      buildPartialRepeatedFields(result);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartialRepeatedFields(name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries result) {
      if (exchangeRatesBuilder_ == null) {
        if (((bitField0_ & 0x00000004) != 0)) {
          exchangeRates_ = java.util.Collections.unmodifiableList(exchangeRates_);
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.exchangeRates_ = exchangeRates_;
      } else {
        result.exchangeRates_ = exchangeRatesBuilder_.build();
      }
    }

    private void buildPartial0(name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.baseCurrency_ = baseCurrency_;
      }
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.termCurrency_ = termCurrency_;
      }
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries) {
        return mergeFrom((name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries other) {
      if (other == name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries.getDefaultInstance()) return this;
      if (!other.getBaseCurrency().isEmpty()) {
        baseCurrency_ = other.baseCurrency_;
        bitField0_ |= 0x00000001;
        onChanged();
      }
      if (!other.getTermCurrency().isEmpty()) {
        termCurrency_ = other.termCurrency_;
        bitField0_ |= 0x00000002;
        onChanged();
      }
      if (exchangeRatesBuilder_ == null) {
        if (!other.exchangeRates_.isEmpty()) {
          if (exchangeRates_.isEmpty()) {
            exchangeRates_ = other.exchangeRates_;
            bitField0_ = (bitField0_ & ~0x00000004);
          } else {
            ensureExchangeRatesIsMutable();
            exchangeRates_.addAll(other.exchangeRates_);
          }
          onChanged();
        }
      } else {
        if (!other.exchangeRates_.isEmpty()) {
          if (exchangeRatesBuilder_.isEmpty()) {
            exchangeRatesBuilder_.dispose();
            exchangeRatesBuilder_ = null;
            exchangeRates_ = other.exchangeRates_;
            bitField0_ = (bitField0_ & ~0x00000004);
            exchangeRatesBuilder_ = 
              com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders ?
                 getExchangeRatesFieldBuilder() : null;
          } else {
            exchangeRatesBuilder_.addAllMessages(other.exchangeRates_);
          }
        }
      }
      this.mergeUnknownFields(other.getUnknownFields());
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 10: {
              baseCurrency_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000001;
              break;
            } // case 10
            case 18: {
              termCurrency_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000002;
              break;
            } // case 18
            case 26: {
              name.abuchen.portfolio.model.proto.v1.PExchangeRate m =
                  input.readMessage(
                      name.abuchen.portfolio.model.proto.v1.PExchangeRate.parser(),
                      extensionRegistry);
              if (exchangeRatesBuilder_ == null) {
                ensureExchangeRatesIsMutable();
                exchangeRates_.add(m);
              } else {
                exchangeRatesBuilder_.addMessage(m);
              }
              break;
            } // case 26
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }
    private int bitField0_;

    private java.lang.Object baseCurrency_ = "";
    /**
     * <code>string baseCurrency = 1;</code>
     * @return The baseCurrency.
     */
    public java.lang.String getBaseCurrency() {
      java.lang.Object ref = baseCurrency_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        baseCurrency_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string baseCurrency = 1;</code>
     * @return The bytes for baseCurrency.
     */
    public com.google.protobuf.ByteString
        getBaseCurrencyBytes() {
      java.lang.Object ref = baseCurrency_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        baseCurrency_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string baseCurrency = 1;</code>
     * @param value The baseCurrency to set.
     * @return This builder for chaining.
     */
    public Builder setBaseCurrency(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      baseCurrency_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>string baseCurrency = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearBaseCurrency() {
      baseCurrency_ = getDefaultInstance().getBaseCurrency();
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     * <code>string baseCurrency = 1;</code>
     * @param value The bytes for baseCurrency to set.
     * @return This builder for chaining.
     */
    public Builder setBaseCurrencyBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      baseCurrency_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }

    private java.lang.Object termCurrency_ = "";
    /**
     * <code>string termCurrency = 2;</code>
     * @return The termCurrency.
     */
    public java.lang.String getTermCurrency() {
      java.lang.Object ref = termCurrency_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        termCurrency_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string termCurrency = 2;</code>
     * @return The bytes for termCurrency.
     */
    public com.google.protobuf.ByteString
        getTermCurrencyBytes() {
      java.lang.Object ref = termCurrency_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        termCurrency_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string termCurrency = 2;</code>
     * @param value The termCurrency to set.
     * @return This builder for chaining.
     */
    public Builder setTermCurrency(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      termCurrency_ = value;
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     * <code>string termCurrency = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearTermCurrency() {
      termCurrency_ = getDefaultInstance().getTermCurrency();
      bitField0_ = (bitField0_ & ~0x00000002);
      onChanged();
      return this;
    }
    /**
     * <code>string termCurrency = 2;</code>
     * @param value The bytes for termCurrency to set.
     * @return This builder for chaining.
     */
    public Builder setTermCurrencyBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      termCurrency_ = value;
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }

    private java.util.List<name.abuchen.portfolio.model.proto.v1.PExchangeRate> exchangeRates_ =
      java.util.Collections.emptyList();
    private void ensureExchangeRatesIsMutable() {
      if (!((bitField0_ & 0x00000004) != 0)) {
        exchangeRates_ = new java.util.ArrayList<name.abuchen.portfolio.model.proto.v1.PExchangeRate>(exchangeRates_);
        bitField0_ |= 0x00000004;
       }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<
        name.abuchen.portfolio.model.proto.v1.PExchangeRate, name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder, name.abuchen.portfolio.model.proto.v1.PExchangeRateOrBuilder> exchangeRatesBuilder_;

    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public java.util.List<name.abuchen.portfolio.model.proto.v1.PExchangeRate> getExchangeRatesList() {
      if (exchangeRatesBuilder_ == null) {
        return java.util.Collections.unmodifiableList(exchangeRates_);
      } else {
        return exchangeRatesBuilder_.getMessageList();
      }
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public int getExchangeRatesCount() {
      if (exchangeRatesBuilder_ == null) {
        return exchangeRates_.size();
      } else {
        return exchangeRatesBuilder_.getCount();
      }
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public name.abuchen.portfolio.model.proto.v1.PExchangeRate getExchangeRates(int index) {
      if (exchangeRatesBuilder_ == null) {
        return exchangeRates_.get(index);
      } else {
        return exchangeRatesBuilder_.getMessage(index);
      }
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public Builder setExchangeRates(
        int index, name.abuchen.portfolio.model.proto.v1.PExchangeRate value) {
      if (exchangeRatesBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureExchangeRatesIsMutable();
        exchangeRates_.set(index, value);
        onChanged();
      } else {
        exchangeRatesBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public Builder setExchangeRates(
        int index, name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder builderForValue) {
      if (exchangeRatesBuilder_ == null) {
        ensureExchangeRatesIsMutable();
        exchangeRates_.set(index, builderForValue.build());
        onChanged();
      } else {
        exchangeRatesBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public Builder addExchangeRates(name.abuchen.portfolio.model.proto.v1.PExchangeRate value) {
      if (exchangeRatesBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureExchangeRatesIsMutable();
        exchangeRates_.add(value);
        onChanged();
      } else {
        exchangeRatesBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public Builder addExchangeRates(
        int index, name.abuchen.portfolio.model.proto.v1.PExchangeRate value) {
      if (exchangeRatesBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureExchangeRatesIsMutable();
        exchangeRates_.add(index, value);
        onChanged();
      } else {
        exchangeRatesBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public Builder addExchangeRates(
        name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder builderForValue) {
      if (exchangeRatesBuilder_ == null) {
        ensureExchangeRatesIsMutable();
        exchangeRates_.add(builderForValue.build());
        onChanged();
      } else {
        exchangeRatesBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public Builder addExchangeRates(
        int index, name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder builderForValue) {
      if (exchangeRatesBuilder_ == null) {
        ensureExchangeRatesIsMutable();
        exchangeRates_.add(index, builderForValue.build());
        onChanged();
      } else {
        exchangeRatesBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public Builder addAllExchangeRates(
        java.lang.Iterable<? extends name.abuchen.portfolio.model.proto.v1.PExchangeRate> values) {
      if (exchangeRatesBuilder_ == null) {
        ensureExchangeRatesIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, exchangeRates_);
        onChanged();
      } else {
        exchangeRatesBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public Builder clearExchangeRates() {
      if (exchangeRatesBuilder_ == null) {
        exchangeRates_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
      } else {
        exchangeRatesBuilder_.clear();
      }
      return this;
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public Builder removeExchangeRates(int index) {
      if (exchangeRatesBuilder_ == null) {
        ensureExchangeRatesIsMutable();
        exchangeRates_.remove(index);
        onChanged();
      } else {
        exchangeRatesBuilder_.remove(index);
      }
      return this;
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder getExchangeRatesBuilder(
        int index) {
      return getExchangeRatesFieldBuilder().getBuilder(index);
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public name.abuchen.portfolio.model.proto.v1.PExchangeRateOrBuilder getExchangeRatesOrBuilder(
        int index) {
      if (exchangeRatesBuilder_ == null) {
        return exchangeRates_.get(index);  } else {
        return exchangeRatesBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public java.util.List<? extends name.abuchen.portfolio.model.proto.v1.PExchangeRateOrBuilder> 
         getExchangeRatesOrBuilderList() {
      if (exchangeRatesBuilder_ != null) {
        return exchangeRatesBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(exchangeRates_);
      }
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder addExchangeRatesBuilder() {
      return getExchangeRatesFieldBuilder().addBuilder(
          name.abuchen.portfolio.model.proto.v1.PExchangeRate.getDefaultInstance());
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder addExchangeRatesBuilder(
        int index) {
      return getExchangeRatesFieldBuilder().addBuilder(
          index, name.abuchen.portfolio.model.proto.v1.PExchangeRate.getDefaultInstance());
    }
    /**
     * <code>repeated .name.abuchen.portfolio.PExchangeRate exchangeRates = 3;</code>
     */
    public java.util.List<name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder> 
         getExchangeRatesBuilderList() {
      return getExchangeRatesFieldBuilder().getBuilderList();
    }
    private com.google.protobuf.RepeatedFieldBuilderV3<
        name.abuchen.portfolio.model.proto.v1.PExchangeRate, name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder, name.abuchen.portfolio.model.proto.v1.PExchangeRateOrBuilder> 
        getExchangeRatesFieldBuilder() {
      if (exchangeRatesBuilder_ == null) {
        exchangeRatesBuilder_ = new com.google.protobuf.RepeatedFieldBuilderV3<
            name.abuchen.portfolio.model.proto.v1.PExchangeRate, name.abuchen.portfolio.model.proto.v1.PExchangeRate.Builder, name.abuchen.portfolio.model.proto.v1.PExchangeRateOrBuilder>(
                exchangeRates_,
                ((bitField0_ & 0x00000004) != 0),
                getParentForChildren(),
                isClean());
        exchangeRates_ = null;
      }
      return exchangeRatesBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:name.abuchen.portfolio.PExchangeRateTimeSeries)
  }

  // @@protoc_insertion_point(class_scope:name.abuchen.portfolio.PExchangeRateTimeSeries)
  private static final name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries();
  }

  public static name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PExchangeRateTimeSeries>
      PARSER = new com.google.protobuf.AbstractParser<PExchangeRateTimeSeries>() {
    @java.lang.Override
    public PExchangeRateTimeSeries parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<PExchangeRateTimeSeries> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<PExchangeRateTimeSeries> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

