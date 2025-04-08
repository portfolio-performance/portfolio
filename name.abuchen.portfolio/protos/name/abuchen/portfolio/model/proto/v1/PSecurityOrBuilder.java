// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: client.proto

package name.abuchen.portfolio.model.proto.v1;

public interface PSecurityOrBuilder extends
    // @@protoc_insertion_point(interface_extends:name.abuchen.portfolio.PSecurity)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string uuid = 1;</code>
   * @return The uuid.
   */
  java.lang.String getUuid();
  /**
   * <code>string uuid = 1;</code>
   * @return The bytes for uuid.
   */
  com.google.protobuf.ByteString
      getUuidBytes();

  /**
   * <code>optional string onlineId = 2;</code>
   * @return Whether the onlineId field is set.
   */
  boolean hasOnlineId();
  /**
   * <code>optional string onlineId = 2;</code>
   * @return The onlineId.
   */
  java.lang.String getOnlineId();
  /**
   * <code>optional string onlineId = 2;</code>
   * @return The bytes for onlineId.
   */
  com.google.protobuf.ByteString
      getOnlineIdBytes();

  /**
   * <code>string name = 3;</code>
   * @return The name.
   */
  java.lang.String getName();
  /**
   * <code>string name = 3;</code>
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString
      getNameBytes();

  /**
   * <code>optional string currencyCode = 4;</code>
   * @return Whether the currencyCode field is set.
   */
  boolean hasCurrencyCode();
  /**
   * <code>optional string currencyCode = 4;</code>
   * @return The currencyCode.
   */
  java.lang.String getCurrencyCode();
  /**
   * <code>optional string currencyCode = 4;</code>
   * @return The bytes for currencyCode.
   */
  com.google.protobuf.ByteString
      getCurrencyCodeBytes();

  /**
   * <code>optional string targetCurrencyCode = 5;</code>
   * @return Whether the targetCurrencyCode field is set.
   */
  boolean hasTargetCurrencyCode();
  /**
   * <code>optional string targetCurrencyCode = 5;</code>
   * @return The targetCurrencyCode.
   */
  java.lang.String getTargetCurrencyCode();
  /**
   * <code>optional string targetCurrencyCode = 5;</code>
   * @return The bytes for targetCurrencyCode.
   */
  com.google.protobuf.ByteString
      getTargetCurrencyCodeBytes();

  /**
   * <code>optional string note = 6;</code>
   * @return Whether the note field is set.
   */
  boolean hasNote();
  /**
   * <code>optional string note = 6;</code>
   * @return The note.
   */
  java.lang.String getNote();
  /**
   * <code>optional string note = 6;</code>
   * @return The bytes for note.
   */
  com.google.protobuf.ByteString
      getNoteBytes();

  /**
   * <code>optional string isin = 7;</code>
   * @return Whether the isin field is set.
   */
  boolean hasIsin();
  /**
   * <code>optional string isin = 7;</code>
   * @return The isin.
   */
  java.lang.String getIsin();
  /**
   * <code>optional string isin = 7;</code>
   * @return The bytes for isin.
   */
  com.google.protobuf.ByteString
      getIsinBytes();

  /**
   * <code>optional string tickerSymbol = 8;</code>
   * @return Whether the tickerSymbol field is set.
   */
  boolean hasTickerSymbol();
  /**
   * <code>optional string tickerSymbol = 8;</code>
   * @return The tickerSymbol.
   */
  java.lang.String getTickerSymbol();
  /**
   * <code>optional string tickerSymbol = 8;</code>
   * @return The bytes for tickerSymbol.
   */
  com.google.protobuf.ByteString
      getTickerSymbolBytes();

  /**
   * <code>optional string wkn = 9;</code>
   * @return Whether the wkn field is set.
   */
  boolean hasWkn();
  /**
   * <code>optional string wkn = 9;</code>
   * @return The wkn.
   */
  java.lang.String getWkn();
  /**
   * <code>optional string wkn = 9;</code>
   * @return The bytes for wkn.
   */
  com.google.protobuf.ByteString
      getWknBytes();

  /**
   * <code>optional string calendar = 10;</code>
   * @return Whether the calendar field is set.
   */
  boolean hasCalendar();
  /**
   * <code>optional string calendar = 10;</code>
   * @return The calendar.
   */
  java.lang.String getCalendar();
  /**
   * <code>optional string calendar = 10;</code>
   * @return The bytes for calendar.
   */
  com.google.protobuf.ByteString
      getCalendarBytes();

  /**
   * <code>optional string feed = 11;</code>
   * @return Whether the feed field is set.
   */
  boolean hasFeed();
  /**
   * <code>optional string feed = 11;</code>
   * @return The feed.
   */
  java.lang.String getFeed();
  /**
   * <code>optional string feed = 11;</code>
   * @return The bytes for feed.
   */
  com.google.protobuf.ByteString
      getFeedBytes();

  /**
   * <code>optional string feedURL = 12;</code>
   * @return Whether the feedURL field is set.
   */
  boolean hasFeedURL();
  /**
   * <code>optional string feedURL = 12;</code>
   * @return The feedURL.
   */
  java.lang.String getFeedURL();
  /**
   * <code>optional string feedURL = 12;</code>
   * @return The bytes for feedURL.
   */
  com.google.protobuf.ByteString
      getFeedURLBytes();

  /**
   * <code>repeated .name.abuchen.portfolio.PHistoricalPrice prices = 13;</code>
   */
  java.util.List<name.abuchen.portfolio.model.proto.v1.PHistoricalPrice> 
      getPricesList();
  /**
   * <code>repeated .name.abuchen.portfolio.PHistoricalPrice prices = 13;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PHistoricalPrice getPrices(int index);
  /**
   * <code>repeated .name.abuchen.portfolio.PHistoricalPrice prices = 13;</code>
   */
  int getPricesCount();
  /**
   * <code>repeated .name.abuchen.portfolio.PHistoricalPrice prices = 13;</code>
   */
  java.util.List<? extends name.abuchen.portfolio.model.proto.v1.PHistoricalPriceOrBuilder> 
      getPricesOrBuilderList();
  /**
   * <code>repeated .name.abuchen.portfolio.PHistoricalPrice prices = 13;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PHistoricalPriceOrBuilder getPricesOrBuilder(
      int index);

  /**
   * <code>optional string latestFeed = 14;</code>
   * @return Whether the latestFeed field is set.
   */
  boolean hasLatestFeed();
  /**
   * <code>optional string latestFeed = 14;</code>
   * @return The latestFeed.
   */
  java.lang.String getLatestFeed();
  /**
   * <code>optional string latestFeed = 14;</code>
   * @return The bytes for latestFeed.
   */
  com.google.protobuf.ByteString
      getLatestFeedBytes();

  /**
   * <code>optional string latestFeedURL = 15;</code>
   * @return Whether the latestFeedURL field is set.
   */
  boolean hasLatestFeedURL();
  /**
   * <code>optional string latestFeedURL = 15;</code>
   * @return The latestFeedURL.
   */
  java.lang.String getLatestFeedURL();
  /**
   * <code>optional string latestFeedURL = 15;</code>
   * @return The bytes for latestFeedURL.
   */
  com.google.protobuf.ByteString
      getLatestFeedURLBytes();

  /**
   * <code>optional .name.abuchen.portfolio.PFullHistoricalPrice latest = 16;</code>
   * @return Whether the latest field is set.
   */
  boolean hasLatest();
  /**
   * <code>optional .name.abuchen.portfolio.PFullHistoricalPrice latest = 16;</code>
   * @return The latest.
   */
  name.abuchen.portfolio.model.proto.v1.PFullHistoricalPrice getLatest();
  /**
   * <code>optional .name.abuchen.portfolio.PFullHistoricalPrice latest = 16;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PFullHistoricalPriceOrBuilder getLatestOrBuilder();

  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue attributes = 17;</code>
   */
  java.util.List<name.abuchen.portfolio.model.proto.v1.PKeyValue> 
      getAttributesList();
  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue attributes = 17;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PKeyValue getAttributes(int index);
  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue attributes = 17;</code>
   */
  int getAttributesCount();
  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue attributes = 17;</code>
   */
  java.util.List<? extends name.abuchen.portfolio.model.proto.v1.PKeyValueOrBuilder> 
      getAttributesOrBuilderList();
  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue attributes = 17;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PKeyValueOrBuilder getAttributesOrBuilder(
      int index);

  /**
   * <code>repeated .name.abuchen.portfolio.PSecurityEvent events = 18;</code>
   */
  java.util.List<name.abuchen.portfolio.model.proto.v1.PSecurityEvent> 
      getEventsList();
  /**
   * <code>repeated .name.abuchen.portfolio.PSecurityEvent events = 18;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PSecurityEvent getEvents(int index);
  /**
   * <code>repeated .name.abuchen.portfolio.PSecurityEvent events = 18;</code>
   */
  int getEventsCount();
  /**
   * <code>repeated .name.abuchen.portfolio.PSecurityEvent events = 18;</code>
   */
  java.util.List<? extends name.abuchen.portfolio.model.proto.v1.PSecurityEventOrBuilder> 
      getEventsOrBuilderList();
  /**
   * <code>repeated .name.abuchen.portfolio.PSecurityEvent events = 18;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PSecurityEventOrBuilder getEventsOrBuilder(
      int index);

  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue properties = 19;</code>
   */
  java.util.List<name.abuchen.portfolio.model.proto.v1.PKeyValue> 
      getPropertiesList();
  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue properties = 19;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PKeyValue getProperties(int index);
  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue properties = 19;</code>
   */
  int getPropertiesCount();
  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue properties = 19;</code>
   */
  java.util.List<? extends name.abuchen.portfolio.model.proto.v1.PKeyValueOrBuilder> 
      getPropertiesOrBuilderList();
  /**
   * <code>repeated .name.abuchen.portfolio.PKeyValue properties = 19;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PKeyValueOrBuilder getPropertiesOrBuilder(
      int index);

  /**
   * <code>bool isRetired = 20;</code>
   * @return The isRetired.
   */
  boolean getIsRetired();

  /**
   * <code>.google.protobuf.Timestamp updatedAt = 21;</code>
   * @return Whether the updatedAt field is set.
   */
  boolean hasUpdatedAt();
  /**
   * <code>.google.protobuf.Timestamp updatedAt = 21;</code>
   * @return The updatedAt.
   */
  com.google.protobuf.Timestamp getUpdatedAt();
  /**
   * <code>.google.protobuf.Timestamp updatedAt = 21;</code>
   */
  com.google.protobuf.TimestampOrBuilder getUpdatedAtOrBuilder();
}
