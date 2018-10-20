package name.abuchen.portfolio.ui.dnd;

import java.util.List;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import name.abuchen.portfolio.model.Security;

public class SecurityTransfer extends ByteArrayTransfer
{
    private static final String TYPE_NAME = "local-security-transfer-format" + (new Long(System.currentTimeMillis())).toString(); //$NON-NLS-1$;

    private static final int TYPEID = registerType(TYPE_NAME);

    private static final SecurityTransfer INSTANCE = new SecurityTransfer();

    private List<Security> securities;

    protected SecurityTransfer()
    {}

    public static SecurityTransfer getTransfer()
    {
        return INSTANCE;
    }

    public List<Security> getSecurities()
    {
        return securities;
    }

    public void setSecurities(List<Security> securities)
    {
        this.securities = securities;
    }

    @Override
    protected int[] getTypeIds()
    {
        return new int[] { TYPEID };
    }

    @Override
    protected String[] getTypeNames()
    {
        return new String[] { TYPE_NAME };
    }

    @Override
    public void javaToNative(Object object, TransferData transferData)
    {
        byte[] check = TYPE_NAME.getBytes();
        super.javaToNative(check, transferData);
    }

    @Override
    protected Object nativeToJava(TransferData transferData)
    {
        Object result = super.nativeToJava(transferData);
        if (result instanceof byte[] && TYPE_NAME.equals(new String((byte[]) result)))
            return securities;
        return null;
    }

}
