package com.autodesk.eceapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({"data", "nextRenewalDate", "suspensionDate", "terminationDate", "expirationDate", "status"})
public class UpdateO2PSubscription {
    @JsonProperty("data")
    private UpdateO2PSubscription.Data data;

    @JsonProperty("meta")
    private UpdateO2PSubscription.Meta meta;

    public UpdateO2PSubscription() {
    }

    @JsonProperty("data")
    public UpdateO2PSubscription.Data getData() {
        return this.data;
    }

    @JsonProperty("data")
    public void setData(UpdateO2PSubscription.Data data) {
        this.data = data;
    }

    public static class Data {
        @JsonProperty("nextRenewalDate")
        private String nextRenewalDate;

        @JsonProperty("suspensionDate")
        private String suspensionDate;

        @JsonProperty("terminationDate")
        private String terminationDate;

        @JsonProperty("expirationDate")
        private String expirationDate;

        @JsonProperty("status")
        private String status;

        public Data() {
        }

        @JsonProperty("nextRenewalDate")
        public String getNextRenewalDate() {
            return this.nextRenewalDate;
        }

        @JsonProperty("suspensionDate")
        public String getSuspensionDate() {
            return this.suspensionDate;
        }

        @JsonProperty("terminationDate")
        public String getTerminationDate() {
            return this.terminationDate;
        }

        @JsonProperty("expirationDate")
        public String getExpirationDate() {
            return this.expirationDate;
        }

        @JsonProperty("status")
        public String getStatus() {
            return this.status;
        }

        @JsonProperty("nextRenewalDate")
        public void setNextRenewalDate(String nextRenewalDate) {
            this.nextRenewalDate = nextRenewalDate;
        }

        @JsonProperty("suspensionDate")
        public void setSuspensionDate(String suspensionDate) {
            this.suspensionDate = suspensionDate;
        }

        @JsonProperty("terminationDate")
        public void setTerminationDate(String terminationDate) {
            this.terminationDate = terminationDate;
        }

        @JsonProperty("expirationDate")
        public void setExpirationDate(String expirationDate) {
            this.expirationDate = expirationDate;
        }

        @JsonProperty("status")
        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class Meta {
        @JsonProperty("context")
        private String context = "EDIT_DIRECT";
    }
}
