package com.microstar.rules.kegcollection;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;

@Path("/keg-collection/choose-collector")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChooseCollectorResource {

  private final DMNRuntime dmnRuntime;
  private final DMNModel dmnModel;

  public ChooseCollectorResource() {
    KieServices kieServices = KieServices.Factory.get();
    KieContainer kieContainer = kieServices.getKieClasspathContainer();
    KieSession kieSession = kieContainer.newKieSession("keg-collection-ksession");
    this.dmnRuntime = kieSession.getKieRuntime(DMNRuntime.class);
    this.dmnModel =
        dmnRuntime.getModel(
            "https://kiegroup.org/dmn/_50818297-FE5F-4D3C-BE75-E38399BB0470", "ChooseCollector");
  }

  @POST
  @Operation(summary = "Given a venue, choose the appropriate keg collection carrier")
  @APIResponse(responseCode = "200", description = "Collector decision made")
  public CollectorResponse chooseCollector(CollectorRequest request) {
    if (request == null || request.venue == null) {
      throw new IllegalArgumentException("Request and venue cannot be null");
    }

    if (request.venue.PostCode == null || request.venue.PostCode.trim().isEmpty()) {
      throw new IllegalArgumentException("Venue PostCode is required");
    }

    DMNContext dmnContext = dmnRuntime.newContext();

    // Create a map structure that matches the DMN tVenue type
    java.util.Map<String, Object> venueMap = new java.util.HashMap<>();
    venueMap.put("UUID", request.venue.UUID);
    venueMap.put("Name", request.venue.Name);
    venueMap.put("TapCustomerId", request.venue.TapCustomerId);
    venueMap.put("PostCode", request.venue.PostCode);

    dmnContext.set("Venue", venueMap);

    DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, dmnContext);

    if (dmnResult.hasErrors()) {
      throw new RuntimeException("DMN evaluation failed: " + dmnResult.getMessages());
    }

    String collectorName = (String) dmnResult.getDecisionResultByName("CollectorName").getResult();
    String collectorId = (String) dmnResult.getDecisionResultByName("CollectorId").getResult();

    return new CollectorResponse(collectorName, collectorId);
  }

  public static class Venue {
    public String UUID;
    public String Name;
    public Integer TapCustomerId;
    public String PostCode;

    public Venue() {}

    public Venue(String UUID, String Name, Integer TapCustomerId, String PostCode) {
      this.UUID = UUID;
      this.Name = Name;
      this.TapCustomerId = TapCustomerId;
      this.PostCode = PostCode;
    }
  }

  public static class CollectorRequest {
    public Venue venue;

    public CollectorRequest() {}

    public CollectorRequest(Venue venue) {
      this.venue = venue;
    }
  }

  public static class CollectorResponse {
    public String collectorName;
    public String collectorId;

    public CollectorResponse() {}

    public CollectorResponse(String collectorName, String collectorId) {
      this.collectorName = collectorName;
      this.collectorId = collectorId;
    }
  }
}
