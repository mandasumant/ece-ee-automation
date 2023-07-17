package com.autodesk.eceapp.dto.purchaseOrder.v4;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
@Data
public class LineItemDTO {
    private String lineItemid;
    private String purchaseOrderId;
    private String subscriptionId;
    private String state;
    private String offeringId;
}