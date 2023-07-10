package com.autodesk.eceapp.dto.subscription.v4;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
public class UpdateSubscriptionDTO {
    private Meta meta;
    private Data data;

    @SuperBuilder
    @lombok.Data
    public static class Meta {
        @Builder.Default
        private String context = "EDIT_DIRECT";
    }

    @SuperBuilder
    @lombok.Data
    public static class Data {
        private String status;
        private String nextRenewalDate;
        private String suspensionDate;
        private String terminationDate;
        private String expirationDate;
        private String resolveByDate;
    }
}
