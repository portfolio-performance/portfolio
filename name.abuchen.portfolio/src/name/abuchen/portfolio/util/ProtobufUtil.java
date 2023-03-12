package name.abuchen.portfolio.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import name.abuchen.portfolio.model.proto.v1.PDecimalValue;

public class ProtobufUtil
{
    private ProtobufUtil()
    {
    }

    public static Timestamp asTimestamp(LocalDateTime localDateTime)
    {
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);

        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    public static LocalDateTime fromTimestamp(Timestamp ts)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()), ZoneOffset.UTC);
    }

    public static Timestamp asUpdatedAtTimestamp(Instant instant)
    {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    public static Instant fromUpdatedAtTimestamp(Timestamp ts)
    {
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }

    public static PDecimalValue asDecimalValue(BigDecimal number)
    {
        return PDecimalValue.newBuilder().setScale(number.scale()).setPrecision(number.precision())
                        .setValue(ByteString.copyFrom(number.unscaledValue().toByteArray())).build();
    }

    public static BigDecimal fromDecimalValue(PDecimalValue number)
    {
        MathContext mc = new MathContext(number.getPrecision());
        return new BigDecimal(new BigInteger(number.getValue().toByteArray()), number.getScale(), mc);
    }
}
