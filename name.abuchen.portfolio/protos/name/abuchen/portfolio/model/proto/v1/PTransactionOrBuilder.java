// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: client.proto

package name.abuchen.portfolio.model.proto.v1;

public interface PTransactionOrBuilder extends
    // @@protoc_insertion_point(interface_extends:name.abuchen.portfolio.PTransaction)
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
   * <code>.name.abuchen.portfolio.PTransaction.Type type = 2;</code>
   * @return The enum numeric value on the wire for type.
   */
  int getTypeValue();
  /**
   * <code>.name.abuchen.portfolio.PTransaction.Type type = 2;</code>
   * @return The type.
   */
  name.abuchen.portfolio.model.proto.v1.PTransaction.Type getType();

  /**
   * <code>optional string account = 3;</code>
   * @return Whether the account field is set.
   */
  boolean hasAccount();
  /**
   * <code>optional string account = 3;</code>
   * @return The account.
   */
  java.lang.String getAccount();
  /**
   * <code>optional string account = 3;</code>
   * @return The bytes for account.
   */
  com.google.protobuf.ByteString
      getAccountBytes();

  /**
   * <code>optional string portfolio = 4;</code>
   * @return Whether the portfolio field is set.
   */
  boolean hasPortfolio();
  /**
   * <code>optional string portfolio = 4;</code>
   * @return The portfolio.
   */
  java.lang.String getPortfolio();
  /**
   * <code>optional string portfolio = 4;</code>
   * @return The bytes for portfolio.
   */
  com.google.protobuf.ByteString
      getPortfolioBytes();

  /**
   * <code>optional string otherAccount = 5;</code>
   * @return Whether the otherAccount field is set.
   */
  boolean hasOtherAccount();
  /**
   * <code>optional string otherAccount = 5;</code>
   * @return The otherAccount.
   */
  java.lang.String getOtherAccount();
  /**
   * <code>optional string otherAccount = 5;</code>
   * @return The bytes for otherAccount.
   */
  com.google.protobuf.ByteString
      getOtherAccountBytes();

  /**
   * <code>optional string otherPortfolio = 6;</code>
   * @return Whether the otherPortfolio field is set.
   */
  boolean hasOtherPortfolio();
  /**
   * <code>optional string otherPortfolio = 6;</code>
   * @return The otherPortfolio.
   */
  java.lang.String getOtherPortfolio();
  /**
   * <code>optional string otherPortfolio = 6;</code>
   * @return The bytes for otherPortfolio.
   */
  com.google.protobuf.ByteString
      getOtherPortfolioBytes();

  /**
   * <code>optional string otherUuid = 7;</code>
   * @return Whether the otherUuid field is set.
   */
  boolean hasOtherUuid();
  /**
   * <code>optional string otherUuid = 7;</code>
   * @return The otherUuid.
   */
  java.lang.String getOtherUuid();
  /**
   * <code>optional string otherUuid = 7;</code>
   * @return The bytes for otherUuid.
   */
  com.google.protobuf.ByteString
      getOtherUuidBytes();

  /**
   * <code>optional .google.protobuf.Timestamp otherUpdatedAt = 8;</code>
   * @return Whether the otherUpdatedAt field is set.
   */
  boolean hasOtherUpdatedAt();
  /**
   * <code>optional .google.protobuf.Timestamp otherUpdatedAt = 8;</code>
   * @return The otherUpdatedAt.
   */
  com.google.protobuf.Timestamp getOtherUpdatedAt();
  /**
   * <code>optional .google.protobuf.Timestamp otherUpdatedAt = 8;</code>
   */
  com.google.protobuf.TimestampOrBuilder getOtherUpdatedAtOrBuilder();

  /**
   * <code>.google.protobuf.Timestamp date = 9;</code>
   * @return Whether the date field is set.
   */
  boolean hasDate();
  /**
   * <code>.google.protobuf.Timestamp date = 9;</code>
   * @return The date.
   */
  com.google.protobuf.Timestamp getDate();
  /**
   * <code>.google.protobuf.Timestamp date = 9;</code>
   */
  com.google.protobuf.TimestampOrBuilder getDateOrBuilder();

  /**
   * <code>string currencyCode = 10;</code>
   * @return The currencyCode.
   */
  java.lang.String getCurrencyCode();
  /**
   * <code>string currencyCode = 10;</code>
   * @return The bytes for currencyCode.
   */
  com.google.protobuf.ByteString
      getCurrencyCodeBytes();

  /**
   * <code>int64 amount = 11;</code>
   * @return The amount.
   */
  long getAmount();

  /**
   * <code>optional int64 shares = 12;</code>
   * @return Whether the shares field is set.
   */
  boolean hasShares();
  /**
   * <code>optional int64 shares = 12;</code>
   * @return The shares.
   */
  long getShares();

  /**
   * <code>optional string note = 13;</code>
   * @return Whether the note field is set.
   */
  boolean hasNote();
  /**
   * <code>optional string note = 13;</code>
   * @return The note.
   */
  java.lang.String getNote();
  /**
   * <code>optional string note = 13;</code>
   * @return The bytes for note.
   */
  com.google.protobuf.ByteString
      getNoteBytes();

  /**
   * <code>optional string security = 14;</code>
   * @return Whether the security field is set.
   */
  boolean hasSecurity();
  /**
   * <code>optional string security = 14;</code>
   * @return The security.
   */
  java.lang.String getSecurity();
  /**
   * <code>optional string security = 14;</code>
   * @return The bytes for security.
   */
  com.google.protobuf.ByteString
      getSecurityBytes();

  /**
   * <code>repeated .name.abuchen.portfolio.PTransactionUnit units = 15;</code>
   */
  java.util.List<name.abuchen.portfolio.model.proto.v1.PTransactionUnit> 
      getUnitsList();
  /**
   * <code>repeated .name.abuchen.portfolio.PTransactionUnit units = 15;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PTransactionUnit getUnits(int index);
  /**
   * <code>repeated .name.abuchen.portfolio.PTransactionUnit units = 15;</code>
   */
  int getUnitsCount();
  /**
   * <code>repeated .name.abuchen.portfolio.PTransactionUnit units = 15;</code>
   */
  java.util.List<? extends name.abuchen.portfolio.model.proto.v1.PTransactionUnitOrBuilder> 
      getUnitsOrBuilderList();
  /**
   * <code>repeated .name.abuchen.portfolio.PTransactionUnit units = 15;</code>
   */
  name.abuchen.portfolio.model.proto.v1.PTransactionUnitOrBuilder getUnitsOrBuilder(
      int index);

  /**
   * <code>.google.protobuf.Timestamp updatedAt = 16;</code>
   * @return Whether the updatedAt field is set.
   */
  boolean hasUpdatedAt();
  /**
   * <code>.google.protobuf.Timestamp updatedAt = 16;</code>
   * @return The updatedAt.
   */
  com.google.protobuf.Timestamp getUpdatedAt();
  /**
   * <code>.google.protobuf.Timestamp updatedAt = 16;</code>
   */
  com.google.protobuf.TimestampOrBuilder getUpdatedAtOrBuilder();

  /**
   * <code>optional string source = 17;</code>
   * @return Whether the source field is set.
   */
  boolean hasSource();
  /**
   * <code>optional string source = 17;</code>
   * @return The source.
   */
  java.lang.String getSource();
  /**
   * <code>optional string source = 17;</code>
   * @return The bytes for source.
   */
  com.google.protobuf.ByteString
      getSourceBytes();
}
