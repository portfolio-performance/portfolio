package name.abuchen.portfolio.ui.util;

import java.util.Optional;

import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.ImageManager;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;

public final class LogoManager
{
    private static LogoManager instance = new LogoManager();

    public static LogoManager instance()
    {
        return instance;
    }

    private LogoManager()
    {
    }

    public Image getDefaultColumnImage(Object object, ClientSettings settings)
    {
        Image logo = getLogoImage(object, settings);
        return logo != null ? logo : getFallbackColumnImage(object);
    }

    public boolean hasCustomLogo(Attributable object, ClientSettings settings)
    {
        if (object == null)
            return false;

        Optional<AttributeType> logoAttr = settings.getOptionalLogoAttributeType(object.getClass());
        if (!logoAttr.isPresent())
            return false;

        if (!object.getAttributes().exists(logoAttr.get()))
            return false;

        return object.getAttributes().get(logoAttr.get()) != null;
    }

    public void clearCustomLogo(Attributable object, ClientSettings settings)
    {
        if (object == null)
            return;

        Optional<AttributeType> logoAttr = settings.getOptionalLogoAttributeType(object.getClass());
        if (!logoAttr.isPresent())
            return;

        object.getAttributes().remove(logoAttr.get());
    }

    private Image getLogoImage(Object object, ClientSettings settings)
    {
        if (object instanceof Attributable target)
        {
            Optional<AttributeType> logoAttr = settings.getOptionalLogoAttributeType(target.getClass());
            return logoAttr.isPresent() ? ImageManager.instance().getImage(target, logoAttr.get()) : null;
        }
        return null;
    }

    private Image getFallbackColumnImage(Object object)
    {
        if (object instanceof Account)
            return Images.ACCOUNT.image();
        else if (object instanceof Security)
            return ((Security) object).isRetired() ? Images.SECURITY_RETIRED.image() : Images.SECURITY.image();
        else if (object instanceof Portfolio)
            return Images.PORTFOLIO.image();
        else if (object instanceof InvestmentPlan)
            return Images.INVESTMENTPLAN.image();
        else if (object instanceof Classification)
            return Images.CATEGORY.image();
        else
            return null;
    }
}
