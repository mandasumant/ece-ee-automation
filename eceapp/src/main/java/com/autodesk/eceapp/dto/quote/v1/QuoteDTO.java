package com.autodesk.eceapp.dto.quote.v1;

import com.autodesk.eceapp.dto.quote.AgentAccountDTO;
import com.autodesk.eceapp.dto.quote.AgentContactDTO;
import com.autodesk.eceapp.dto.quote.EndCustomerDTO;
import com.autodesk.eceapp.dto.quote.PurchaserDTO;
import java.util.List;
import lombok.Builder;

public @Builder
class QuoteDTO {

  private String quoteNote;
  private String currency;
  private AgentAccountDTO agentAccount;
  private AgentContactDTO agentContact;
  private EndCustomerDTO endCustomer;
  private PurchaserDTO purchaser;
  private List<LineItemDTO> lineItems;
}
