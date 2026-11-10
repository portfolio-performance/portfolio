package name.abuchen.portfolio.rest.internal;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.RestApiConstants;

/**
 * Wire format of the pairing endpoints. Runs on the HTTP worker thread; only
 * the user's decision (reported through the SPI) involves the UI.
 */
public final class PairingHandler
{
    private PairingHandler()
    {
    }

    public static Response create(PairingService service, JsonObject body)
    {
        rejectUnknownFields(body);

        String clientName = null;
        if (body.has("clientName") && body.get("clientName").isJsonPrimitive() //$NON-NLS-1$ //$NON-NLS-2$
                        && body.getAsJsonPrimitive("clientName").isString()) //$NON-NLS-1$
            clientName = body.get("clientName").getAsString(); //$NON-NLS-1$

        var id = service.create(clientName);

        var json = new JsonObject();
        json.addProperty("id", id); //$NON-NLS-1$
        json.addProperty("status", PairingService.PairingStatus.PENDING.toJsonValue()); //$NON-NLS-1$
        return new Response(201, "application/json", json.toString().getBytes(StandardCharsets.UTF_8), //$NON-NLS-1$
                        Map.of("Location", RestApiConstants.PAIRING_ENDPOINT + "/" + id)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Refuses any member besides {@code clientName}. Ignoring them would be
     * unsafe: a later client asking for narrower access (say, read-only) would
     * be granted a full token by this version without either side noticing.
     */
    private static void rejectUnknownFields(JsonObject body)
    {
        var unknown = body.keySet().stream() //
                        .filter(name -> !"clientName".equals(name)) //$NON-NLS-1$
                        .sorted() // JSON member order is not significant; the response should be stable
                        .map(name -> new ApiException.FieldError(name, "unknown-field", //$NON-NLS-1$
                                        "unknown field '" + name + "'; this endpoint accepts: clientName")) //$NON-NLS-1$ //$NON-NLS-2$
                        .toList();

        if (!unknown.isEmpty())
            throw ApiException.validation(unknown);
    }

    public static Response poll(PairingService service, String id)
    {
        var result = service.poll(id);

        var json = new JsonObject();
        json.addProperty("status", result.status().toJsonValue()); //$NON-NLS-1$
        if (result.token() != null)
            json.addProperty("token", result.token()); //$NON-NLS-1$
        return Response.json(200, json);
    }
}
