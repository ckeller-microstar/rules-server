package com.microstar.rules;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
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
        this.dmnRuntime = kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
        this.dmnModel = dmnRuntime.getModel("https://kie.org/dmn/_C550657A-1516-4B94-92A5-8613A052F7EE", "DMN_318F5DE4-BE6D-4DB7-B371-7ED04F6CE0DD");
    }

    @POST
    @Path("/decide")
    public PartyResponse decideParty(PartyRequest request) {
        DMNContext dmnContext = dmnRuntime.newContext();
        dmnContext.set("Day of the week", request.dayOfWeek);

        DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, dmnContext);
        
        String partyDecision = (String) dmnResult.getDecisionResultByName("Party").getResult();
        
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