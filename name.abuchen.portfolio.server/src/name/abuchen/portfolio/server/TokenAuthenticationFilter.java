package name.abuchen.portfolio.server;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;

import name.abuchen.portfolio.ui.UIConstants;

public class TokenAuthenticationFilter implements Filter
{
    private String token;

    @Inject
    @Optional
    public void setServerPort(
                    @Preference(nodePath = "name.abuchen.portfolio.ui", value = UIConstants.Preferences.WEB_SERVER_TOKEN) String token)
    {
        this.token = token;
    }

    @Override
    public void init(FilterConfig fConfig) throws ServletException // NOSONAR
    {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (!token.equals(httpRequest.getHeader(ServerConstants.HEADER_TOKEN)))
        {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
        else
        {
            chain.doFilter(httpRequest, response);
        }
    }

    @Override
    public void destroy() // NOSONAR
    {}

}
