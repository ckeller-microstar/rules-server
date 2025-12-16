package com.microstar.rules;

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

@Path("/party")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartyDecisionResource {

  private final DMNRuntime dmnRuntime;
  private final DMNModel dmnModel;

  public PartyDecisionResource() {
    KieServices kieServices = KieServices.Factory.get();
    KieContainer kieContainer = kieServices.getKieClasspathContainer();
    KieSession kieSession = kieContainer.newKieSession("party-ksession");
    this.dmnRuntime = kieSession.getKieRuntime(DMNRuntime.class);
    // this.dmnModel = dmnRuntime.getModelById("com.example.party", "_7C49E3DB-E4BF-4170-B2E6-7231A4AB1CC1");
    this.dmnModel = dmnRuntime.getModel("com.example.party", "Party Decision");
  }

  @POST
  @Path("/decide")
  @Operation(summary = "Make party decision based on day of week")
  @APIResponse(responseCode = "200", description = "Party decision made")
  public PartyResponse decideParty(PartyRequest request) {
    DMNContext dmnContext = dmnRuntime.newContext();
    dmnContext.set("Day of the week", request.dayOfWeek);

    DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, dmnContext);

    // String partyDecision = (String) dmnResult.getDecisionResultById("_BF13B278-9F52-4DA6-81AB-CE3C3B24DA7A").getResult();
    String partyDecision = (String) dmnResult.getDecisionResultByName("Party Node").getResult();

    return new PartyResponse(request.dayOfWeek, partyDecision);
  }

  public static class PartyRequest {
    public String dayOfWeek;

    public PartyRequest() {}

    public PartyRequest(String dayOfWeek) {
      this.dayOfWeek = dayOfWeek;
    }
  }

  public static class PartyResponse {
    public String dayOfWeek;
    public String partyDecision;

    public PartyResponse() {}

    public PartyResponse(String dayOfWeek, String partyDecision) {
      this.dayOfWeek = dayOfWeek;
      this.partyDecision = partyDecision;
    }
  }
}
