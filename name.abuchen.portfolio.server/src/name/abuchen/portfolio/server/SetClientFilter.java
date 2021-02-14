package name.abuchen.portfolio.server;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;

public class SetClientFilter implements Filter
{
    @Inject
    private ClientInputFactory factory;

    @Override
    public void init(FilterConfig fConfig) throws ServletException // NOSONAR
    {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String filename = httpRequest.getHeader(ServerConstants.HEADER_FILE);

        if (filename == null)
        {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Optional<ClientInput> input = factory.lookupIfPresent(new File(filename));
        if (input.isPresent())
        {
            request.setAttribute(Client.class.getName(), input.get().getClient());
            chain.doFilter(request, response);
        }
        else
        {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public void destroy() // NOSONAR
    {}

}
