package name.abuchen.portfolio.ui.views.taxonomy;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class TaxonomyNodeTransfer extends ByteArrayTransfer
{
    private static final String TYPE_NAME = "local-taxonomy-node-transfer-format" + (new Long(System.currentTimeMillis())).toString(); //$NON-NLS-1$;

    private static final int TYPEID = registerType(TYPE_NAME);

    private static final TaxonomyNodeTransfer INSTANCE = new TaxonomyNodeTransfer();

    private TaxonomyNode taxonomyNode;

    protected TaxonomyNodeTransfer()
    {}

    public static TaxonomyNodeTransfer getTransfer()
    {
        return INSTANCE;
    }

    public TaxonomyNode getTaxonomyNode()
    {
        return taxonomyNode;
    }

    public void setTaxonomyNode(TaxonomyNode security)
    {
        this.taxonomyNode = security;
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
            return taxonomyNode;
        return null;
    }

}
