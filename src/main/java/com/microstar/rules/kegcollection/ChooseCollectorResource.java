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

  /** The namespace of the DMN file. Must match {@code ChooseCollector.dmn}. */
  private static final String DMN_NAMESPACE =
      "https://kiegroup.org/dmn/_50818297-FE5F-4D3C-BE75-E38399BB0470";

  /** The DMN model. Must match {@code ChooseCollector.dmn}. */
  private static final String DMN_MODEL_NAME = "ChooseCollector";

  /** The named session. Must match {@code kmodule.xml}. */
  private static final String DMN_KSESSION = "keg-collection-ksession";

  private final KieContainer kieContainer;

  public ChooseCollectorResource() {
    KieServices kieServices = KieServices.Factory.get();
    this.kieContainer = kieServices.getKieClasspathContainer();
  }

  @POST
  @Operation(summary = "Given a venue, choose the appropriate keg collection carrier")
  @APIResponse(responseCode = "200", description = "Collector decision made")
  public CollectorResponse chooseCollector(CollectorRequest request) {
    if (request == null || request.venue == null) {
      throw new IllegalArgumentException("Request and venue cannot be null");
    }

    if (request.venue.postCode == null || request.venue.postCode.trim().isEmpty()) {
      throw new IllegalArgumentException("Venue PostCode is required");
    }

    try (KieSession kieSession = kieContainer.newKieSession(DMN_KSESSION)) {
      DMNRuntime dmnRuntime = kieSession.getKieRuntime(DMNRuntime.class);
      DMNModel dmnModel = dmnRuntime.getModel(DMN_NAMESPACE, DMN_MODEL_NAME);

      if (dmnModel == null) {
        throw new RuntimeException("DMN model not found: " + DMN_MODEL_NAME);
      }

      return executeDmn(request, dmnRuntime, dmnModel);
    }
  }

  private static CollectorResponse executeDmn(
      CollectorRequest request, DMNRuntime dmnRuntime, DMNModel dmnModel) {
    DMNContext dmnContext = bindInputs(request, dmnRuntime);
    DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, dmnContext);
    if (dmnResult.hasErrors()) {
      throw new RuntimeException("DMN evaluation failed: " + dmnResult.getMessages());
    }
    return bindOutputs(dmnResult);
  }

  private static DMNContext bindInputs(CollectorRequest request, DMNRuntime dmnRuntime) {
    DMNContext dmnContext = dmnRuntime.newContext();
    java.util.Map<String, Object> venueMap = new java.util.HashMap<>();
    venueMap.put("UUID", request.venue.uuid);
    venueMap.put("Name", request.venue.name);
    venueMap.put("TapCustomerId", request.venue.tapCustomerId);
    venueMap.put("PostCode", request.venue.postCode);
    dmnContext.set("Venue", venueMap);
    return dmnContext;
  }

  private static CollectorResponse bindOutputs(DMNResult dmnResult) {
    String collectorName = (String) dmnResult.getDecisionResultByName("CollectorName").getResult();
    String collectorId = (String) dmnResult.getDecisionResultByName("CollectorId").getResult();
    return new CollectorResponse(collectorName, collectorId);
  }

  public static class Venue {
    public String uuid;
    public String name;
    public Integer tapCustomerId;
    public String postCode;

    public Venue() {}

    public Venue(String uuid, String name, Integer tapCustomerId, String postCode) {
      this.uuid = uuid;
      this.name = name;
      this.tapCustomerId = tapCustomerId;
      this.postCode = postCode;
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
