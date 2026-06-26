package name.abuchen.portfolio.ui.dialogs.transactions;

import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.ledger.compatibility.LedgerNativeComponentInspectorModel;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerNativeComponentInspectorModel.HeaderField;
import name.abuchen.portfolio.ui.Messages;

/**
 * Shows a read-only inspection view for one Ledger entry.
 * The dialog displays persisted Ledger facts and optional Java-only leg configuration metadata.
 * It does not provide editing support and does not route through legacy transaction setters.
 */
public class LedgerNativeComponentInspectorDialog extends Dialog
{
    private static final int DIALOG_WIDTH = 1050;
    private static final int DIALOG_HEIGHT = 720;

    private final LedgerNativeComponentInspectorModel model;

    public LedgerNativeComponentInspectorDialog(Shell parentShell, LedgerNativeComponentInspectorModel model)
    {
        super(parentShell);
        this.model = model;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.LedgerNativeComponentInspectorDialogTitle);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.CLOSE_ID)
            close();
        else
            super.buttonPressed(buttonId);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        GridDataFactory.fillDefaults().grab(true, true).hint(DIALOG_WIDTH, DIALOG_HEIGHT).applyTo(scrolled);

        Composite container = new Composite(scrolled, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).spacing(8, 8).applyTo(container);

        createTable(container, Messages.LedgerNativeComponentInspectorHeader, model.getHeaderRows(),
                        new String[] { Messages.LedgerNativeComponentInspectorField,
                                        Messages.LedgerNativeComponentInspectorValue },
                        row -> new String[] { headerLabel(row.field()), row.value() });

        createTable(container, Messages.LedgerNativeComponentInspectorEntryParameters, model.getEntryParameters(),
                        new String[] { Messages.LedgerNativeComponentInspectorParameter,
                                        Messages.LedgerNativeComponentInspectorCode,
                                        Messages.LedgerNativeComponentInspectorValueKind,
                                        Messages.LedgerNativeComponentInspectorValue,
                                        Messages.LedgerNativeComponentInspectorDomain },
                        row -> new String[] { row.parameter(), row.code(), row.valueKind(), row.value(),
                                        row.domain() });

        createLegTable(container);

        createTable(container, Messages.LedgerNativeComponentInspectorPostings, model.getPostings(),
                        new String[] { Messages.LedgerNativeComponentInspectorPostingUUID,
                                        Messages.LedgerNativeComponentInspectorPostingType,
                                        Messages.ColumnAmount, Messages.ColumnCurrency,
                                        Messages.ColumnSecurity, Messages.ColumnShares, Messages.ColumnAccount,
                                        Messages.ColumnPortfolio },
                        row -> new String[] { row.postingUUID(), row.postingType(), row.amount(), row.currency(),
                                        row.security(), row.shares(), row.account(), row.portfolio() });

        createTable(container, Messages.LedgerNativeComponentInspectorPostingParameters, model.getPostingParameters(),
                        new String[] { Messages.LedgerNativeComponentInspectorPostingUUID,
                                        Messages.LedgerNativeComponentInspectorPostingType,
                                        Messages.LedgerNativeComponentInspectorParameter,
                                        Messages.LedgerNativeComponentInspectorCode,
                                        Messages.LedgerNativeComponentInspectorValueKind,
                                        Messages.LedgerNativeComponentInspectorValue,
                                        Messages.LedgerNativeComponentInspectorDomain },
                        row -> new String[] { row.postingUUID(), row.postingType(), row.parameter(), row.code(),
                                        row.valueKind(), row.value(), row.domain() });

        createTable(container, Messages.LedgerNativeComponentInspectorProjectionRefs, model.getProjectionRefs(),
                        new String[] { Messages.LedgerNativeComponentInspectorProjectionRole,
                                        Messages.LedgerNativeComponentInspectorOwner,
                                        Messages.LedgerNativeComponentInspectorProjectionUUID,
                                        Messages.LedgerNativeComponentInspectorPrimaryPostingUUID,
                                        Messages.LedgerNativeComponentInspectorPostingGroupUUID },
                        row -> new String[] { row.projectionRole(), row.owner(), row.projectionUUID(),
                                        row.primaryPostingUUID(), row.postingGroupUUID() });

        scrolled.setContent(container);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);
        scrolled.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        return scrolled;
    }

    private void createLegTable(Composite parent)
    {
        Group group = new Group(parent, SWT.NONE);
        group.setText(Messages.LedgerNativeComponentInspectorFunctionalLegs);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
        GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(group);

        if (!model.isNativeEntryDefinitionAvailable())
        {
            Label label = new Label(group, SWT.WRAP);
            label.setText(Messages.LedgerNativeComponentInspectorNoNativeDefinition);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
        }

        createTable(group, null, model.getLegs(),
                        new String[] { Messages.LedgerNativeComponentInspectorLegRole,
                                        Messages.LedgerNativeComponentInspectorPostingType,
                                        Messages.LedgerNativeComponentInspectorCardinality,
                                        Messages.LedgerNativeComponentInspectorProjectionRole,
                                        Messages.LedgerNativeComponentInspectorPrimaryExpected,
                                        Messages.LedgerNativeComponentInspectorGroupExpected },
                        row -> new String[] { row.legRole(), row.postingType(), row.cardinality(),
                                        row.projectionRole(), row.primaryExpected(), row.groupExpected() });
    }

    private <T> void createTable(Composite parent, String title, List<T> input, String[] columns,
                    Function<T, String[]> values)
    {
        Composite group;
        if (title != null)
        {
            Group titledGroup = new Group(parent, SWT.NONE);
            titledGroup.setText(title);
            group = titledGroup;
        }
        else
        {
            group = new Composite(parent, SWT.NONE);
        }

        GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
        GridLayoutFactory.fillDefaults().margins(title != null ? 6 : 0, title != null ? 6 : 0).applyTo(group);

        TableViewer viewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, Math.min(220, 46 + 24 * Math.max(1, input.size())))
                        .applyTo(viewer.getTable());

        for (int columnIndex = 0; columnIndex < columns.length; columnIndex++)
        {
            final int index = columnIndex;
            TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
            column.getColumn().setText(columns[columnIndex]);
            column.getColumn().setWidth(160);
            column.getColumn().setResizable(true);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @SuppressWarnings("unchecked")
                @Override
                public String getText(Object element)
                {
                    String[] rowValues = values.apply((T) element);
                    return index < rowValues.length ? rowValues[index] : ""; //$NON-NLS-1$
                }
            });
        }

        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setInput(input);
    }

    private static String headerLabel(HeaderField field)
    {
        return switch (field)
        {
            case ENTRY_TYPE -> Messages.LedgerNativeComponentInspectorEntryType;
            case ENTRY_UUID -> Messages.LedgerNativeComponentInspectorEntryUUID;
            case DATE_TIME -> Messages.LedgerNativeComponentInspectorDateTime;
            case NOTE -> Messages.ColumnNote;
            case SOURCE -> Messages.ColumnSource;
            case SHAPE -> Messages.LedgerNativeComponentInspectorShape;
            case NATIVE_TARGETED -> Messages.LedgerNativeComponentInspectorNativeTargeted;
            case SELECTED_PROJECTION_ROLE -> Messages.LedgerNativeComponentInspectorSelectedProjectionRole;
            case SELECTED_PROJECTION_UUID -> Messages.LedgerNativeComponentInspectorSelectedProjectionUUID;
            case SELECTED_PRIMARY_POSTING_UUID -> Messages.LedgerNativeComponentInspectorSelectedPrimaryPostingUUID;
            case SELECTED_POSTING_GROUP_UUID -> Messages.LedgerNativeComponentInspectorSelectedPostingGroupUUID;
        };
    }
}
