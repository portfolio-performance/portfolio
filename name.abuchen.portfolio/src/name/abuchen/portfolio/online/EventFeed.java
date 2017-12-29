package name.abuchen.portfolio.online;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.impl.HTMLTableEventParser;

public abstract class EventFeed extends Feed
{

    public static String ID = "EVENT"; //$NON-NLS-1$    

    @SuppressWarnings("nls")
    protected void doLoad(String source, PrintWriter writer) throws IOException
    {
        writer.println("--------");
        writer.println(source);
        writer.println("--------");

        List<SecurityElement> elements;
        List<SecurityEvent> events = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        if (source.startsWith("http"))
        {
            elements = new HTMLTableEventParser().parseFromURL(source, errors);
        }
        else
        {
            try (Scanner scanner = new Scanner(new File(source), StandardCharsets.UTF_8.name()))
            {
                String html = scanner.useDelimiter("\\A").next();
                elements = new HTMLTableEventParser().parseFromHTML(html, errors);
            }
        }

        for (Exception error : errors)
            error.printStackTrace(writer); // NOSONAR

        for (SecurityElement e : elements)
        {
            if (e instanceof SecurityEvent) 
            {
                    events.add((SecurityEvent) e); // need to cast each object specifically
            }
            else 
            {
                writer.print("(W) Invalid Object found: " + e.toString());                
            }
        }
        
        for (SecurityEvent e : events)
        {
            writer.print(Values.Date.format(e.getDate()));
            writer.print("\t");
            writer.println(e.getTypeStr());
            writer.print("\t");
            writer.print(e.getAmount().toString());
            writer.print("\t");
            writer.print(e.getRatio());
        }
    }
    
}
