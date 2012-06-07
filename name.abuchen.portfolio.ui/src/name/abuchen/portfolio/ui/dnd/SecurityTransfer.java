package name.abuchen.portfolio.ui.dnd;

import name.abuchen.portfolio.model.Security;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class SecurityTransfer extends ByteArrayTransfer
{
    private static final String TYPE_NAME = "local-security-transfer-format" + (new Long(System.currentTimeMillis())).toString(); //$NON-NLS-1$;

    private static final int TYPEID = registerType(TYPE_NAME);

    private static final SecurityTransfer INSTANCE = new SecurityTransfer();

    private Security security;

    protected SecurityTransfer()
    {}

    public static SecurityTransfer getTransfer()
    {
        return INSTANCE;
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        this.security = security;
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
            return security;
        return null;
    }

}
