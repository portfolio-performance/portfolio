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
import name.abuchen.portfolio.model.InvestmentVehicle;
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
        return getDefaultColumnImage(object, settings, false);
    }

    public Image getDefaultColumnImage(Object object, ClientSettings settings, boolean disabled)
    {
        Image logo = getLogoImage(object, settings, disabled);
        return logo != null ? logo : getFallbackColumnImage(object, disabled);
    }

    public Image getAttributeImage(Attributable object, AttributeType attribute)
    {
        return usesRetiredAlpha(object, attribute, false)
                        ? ImageManager.instance().getImageWithAlpha(object, attribute, ImageManager.RETIRED_ALPHA)
                        : ImageManager.instance().getImage(object, attribute);
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

    private Image getLogoImage(Object object, ClientSettings settings, boolean disabled)
    {
        if (object instanceof Attributable target)
        {
            Optional<AttributeType> logoAttr = settings.getOptionalLogoAttributeType(target.getClass());
            if (!logoAttr.isPresent())
                return null;

            AttributeType attribute = logoAttr.get();
            if (usesRetiredAlpha(target, attribute, disabled))
                return ImageManager.instance().getImageWithAlpha(target, attribute, ImageManager.RETIRED_ALPHA);

            return ImageManager.instance().getImage(target, attribute, disabled);
        }
        return null;
    }

    private Image getFallbackColumnImage(Object object, boolean disabled)
    {
        if (object instanceof Account account)
            return columnImage(Images.ACCOUNT, account, disabled);
        else if (object instanceof Security security)
            return columnImage(security.isRetired() ? Images.SECURITY_RETIRED : Images.SECURITY, security, disabled);
        else if (object instanceof Portfolio portfolio)
            return columnImage(Images.PORTFOLIO, portfolio, disabled);
        else if (object instanceof InvestmentPlan)
            return Images.INVESTMENTPLAN.image(disabled);
        else if (object instanceof Classification)
            return Images.CATEGORY.image(disabled);
        else
            return null;
    }

    private static Image columnImage(Images icon, Object owner, boolean disabled)
    {
        return usesRetiredAlpha(owner, disabled) ? icon.imageWithAlpha(ImageManager.RETIRED_ALPHA)
                        : icon.image(disabled);
    }

    static boolean usesRetiredAlpha(Attributable object, AttributeType attribute, boolean disabled)
    {
        return !disabled && isLogoAttribute(attribute) && isRetiredLogoOwner(object);
    }

    static boolean usesRetiredAlpha(Object object, boolean disabled)
    {
        return !disabled && isRetiredLogoOwner(object);
    }

    private static boolean isLogoAttribute(AttributeType attribute)
    {
        return attribute != null && attribute.getConverter() instanceof AttributeType.ImageConverter
                        && "logo".equalsIgnoreCase(attribute.getName()); //$NON-NLS-1$
    }

    private static boolean isRetiredLogoOwner(Object object)
    {
        if (object instanceof InvestmentVehicle vehicle)
            return vehicle.isRetired();
        else if (object instanceof Portfolio portfolio)
            return portfolio.isRetired();
        else
            return false;
    }
}
