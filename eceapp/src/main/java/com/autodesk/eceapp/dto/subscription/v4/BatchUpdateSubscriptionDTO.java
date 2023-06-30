package com.autodesk.eceapp.dto.subscription.v4;

import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
public class BatchUpdateSubscriptionDTO {
    private Meta meta;
    private Data data;

    @SuperBuilder
    public static class Meta {
        private String subscriptionIds;
        private String context;
        private String origin;
    }

    @SuperBuilder
    public static class Data {
        private String exportControlStatus;
        private String exportControlStatusLastUpdated;
    }
}