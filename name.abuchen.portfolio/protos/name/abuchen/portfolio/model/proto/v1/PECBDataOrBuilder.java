// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: client.proto

package name.abuchen.portfolio.model.proto.v1;

public interface PECBDataOrBuilder extends
    // @@protoc_insertion_point(interface_extends:name.abuchen.portfolio.PECBData)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int64 lastModified = 1;</code>
   * @return The lastModified.
   */
  long getLastModified();

  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRateTimeSeries series = 2;</code>
   */
  java.util.List<name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries> 
      getSeriesList();
  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRateTimeSeries series = 2;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries getSeries(int index);
  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRateTimeSeries series = 2;</code>
   */
  int getSeriesCount();
  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRateTimeSeries series = 2;</code>
   */
  java.util.List<? extends name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeriesOrBuilder> 
      getSeriesOrBuilderList();
  /**
   * <code>repeated .name.abuchen.portfolio.PExchangeRateTimeSeries series = 2;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeriesOrBuilder getSeriesOrBuilder(
      int index);
}
