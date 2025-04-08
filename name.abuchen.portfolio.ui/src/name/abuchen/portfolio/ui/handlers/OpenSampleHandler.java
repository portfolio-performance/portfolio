package name.abuchen.portfolio.ui.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Taxonomy.Visitor;
import name.abuchen.portfolio.model.TaxonomyTemplate;
import name.abuchen.portfolio.online.impl.EurostatHICPLabels;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.views.dashboard.WidgetFactory;
import name.abuchen.portfolio.util.ProgressMonitorInputStream;
import name.abuchen.portfolio.util.TokenReplacingReader;
import name.abuchen.portfolio.util.TokenReplacingReader.ITokenResolver;

public class OpenSampleHandler
{
    @Inject
    private UISynchronize sync;

    @Inject
    private ClientInputFactory clientInputFactory;

    private static final ResourceBundle RESOURCES = ResourceBundle
                    .getBundle("name.abuchen.portfolio.ui.parts.samplemessages"); //$NON-NLS-1$

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell, //
                    final MApplication app, //
                    final EPartService partService, final EModelService modelService,
                    @Named(UIConstants.Parameter.SAMPLE_FILE) final String sampleFile)
    {
        try
        {
            IRunnableWithProgress loadResourceOperation = new IRunnableWithProgress()
            {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try (InputStream in = this.getClass().getResourceAsStream(sampleFile))
                    {
                        InputStream inputStream = new ProgressMonitorInputStream(in, monitor);
                        Reader replacingReader = new TokenReplacingReader(
                                        new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                                        buildResourcesTokenResolver());

                        final Client client = ClientFactory.load(replacingReader);

                        fixTaxonomyLabels(client);
                        fixDashboardLabels(client);

                        sync.asyncExec(() -> {
                            String label = sampleFile.substring(sampleFile.lastIndexOf('/') + 1);
                            label = label.substring(0, label.lastIndexOf('.'));
                            ClientInput clientInput = clientInputFactory.create(label, client);

                            MPart part = partService.createPart(UIConstants.Part.PORTFOLIO);
                            part.setLabel(label);
                            part.getTransientData().put(ClientInput.class.getName(), clientInput);

                            MPartStack stack = (MPartStack) modelService.find(UIConstants.PartStack.MAIN, app);
                            stack.getChildren().add(part);

                            partService.showPart(part, PartState.ACTIVATE);
                        });
                    }
                    catch (IOException ignore)
                    {
                        PortfolioPlugin.log(ignore);
                    }
                }

            };
            new ProgressMonitorDialog(shell).run(true, true, loadResourceOperation);
        }
        catch (InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    protected void fixTaxonomyLabels(Client client)
    {
        for (Taxonomy taxonomy : new ArrayList<>(client.getTaxonomies()))
        {
            TaxonomyTemplate template = TaxonomyTemplate.byId(taxonomy.getId());

            if (template != null)
                applyTaxonomyLabels(template, taxonomy);

            // classification ids must be unique per file. Usually, the ids are
            // randomized upon creating a taxonomy from a template. However, in
            // order to translate the sample file, we keep the original UUIDs.
            // By copying the taxonomy, we ensure unique ids.

            // but because assetclasses and assetallocation are referenced in
            // diagrams and dashboards, do not change the ids there. These ids
            // are manually unique, but the regions and sector taxonomies are
            // not.

            if (!taxonomy.getId().startsWith("asset")) //$NON-NLS-1$
            {
                client.removeTaxonomy(taxonomy);
                client.addTaxonomy(taxonomy.copy());
            }
        }
    }

    private void applyTaxonomyLabels(TaxonomyTemplate template, Taxonomy taxonomy)
    {
        Taxonomy original = template.buildOriginal();

        taxonomy.setName(original.getName());
        taxonomy.setDimensions(original.getDimensions());

        Map<String, Classification> translated = original.getAllClassifications() //
                        .stream().collect(Collectors.toMap(Classification::getId, c -> c));

        taxonomy.foreach(new Visitor()
        {
            @Override
            public void visit(Classification classification)
            {
                Classification t = translated.get(classification.getId());
                if (t != null)
                {
                    classification.setName(t.getName());
                    classification.setNote(t.getNote());
                }
            }
        });
    }

    protected void fixDashboardLabels(Client client)
    {
        client.getDashboards().forEach(dashboard -> {
            for (Dashboard.Column column : dashboard.getColumns())
            {
                for (Dashboard.Widget widget : column.getWidgets())
                {
                    WidgetFactory factory = WidgetFactory.valueOf(widget.getType());
                    if (factory == null)
                        continue;
                    if (factory == WidgetFactory.HEADING)
                        continue;
                    widget.setLabel(factory.getLabel());
                }
            }
        });
    }

    private static ITokenResolver buildResourcesTokenResolver()
    {
        var messagesPrefix = "Messages."; //$NON-NLS-1$
        var hcpiPrefix = "EurostatHICPLabels."; //$NON-NLS-1$

        return tokenName -> {
            try
            {
                if (tokenName.startsWith(messagesPrefix))
                {
                    return Messages.class.getField(tokenName.substring(messagesPrefix.length())).get(null).toString();
                }
                else if (tokenName.startsWith(hcpiPrefix))
                {
                    return EurostatHICPLabels.getString(tokenName.substring(hcpiPrefix.length()));
                }
                else
                {
                    return RESOURCES.getString(tokenName);
                }
            }
            catch (MissingResourceException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException
                            | SecurityException e)
            {
                return tokenName;
            }
        };
    }
}
