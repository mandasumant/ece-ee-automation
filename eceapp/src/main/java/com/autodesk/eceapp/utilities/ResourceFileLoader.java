package com.autodesk.eceapp.utilities;

import com.autodesk.eceapp.constants.EceAppConstants;
import com.autodesk.eceapp.dto.UpdateO2PSubscription;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.restassured.path.json.JsonPath;
import lombok.experimental.UtilityClass;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public final class ResourceFileLoader {
    public static Map<?, ?> getBicOrderYaml() {
        return getResourceByEnvironmentPath("BicOrder.yml");
    }

    public static Map<?, ?> getNewtYaml() {
        return getResourceByEnvironmentPath("NEWT.yml");
    }

    public static Map<?, ?> getLocaleConfigYaml() {
        return getResourceByMiscPath("LocaleConfig.yml");
    }

    public static Map<?, ?> getBankInformationByLocaleYaml() {
        return getResourceByMiscPath("BankInformationByLocale.yml");
    }

    public static Map<?, ?> getSAPOrderYaml() {
        return getResourceByEnvironmentPath("SAPOrder.yml");
    }

    public static Map<?, ?> getEDUyamlInSTG() {
        if (GlobalConstants.ENV.equalsIgnoreCase("stg")) {
            return getResourceByEnvironmentPath("EDU.yml");
        }
        return new HashMap<>();
    }

    public static Map<?, ?> getMailosaurCredentialsYaml() {
        return getResourceByMiscPath("MailosaurCredentials.yml");
    }

    public static JsonPath getCountryCodesJson() throws FileNotFoundException {
        InputStreamReader inputStream = getResourceInMiscAsInputStreamReader("countryCodes.json");
        return new JsonPath(inputStream);
    }

    public static JsonPath getProvincesJson() throws FileNotFoundException {
        InputStreamReader inputStream = getResourceInMiscAsInputStreamReader("provinces.json");
        return new JsonPath(inputStream);
    }

    public static TaxExemptionMappings getTaxExemptionMappings() throws IOException {
        InputStreamReader inputStream = getResourceInMiscAsInputStreamReader("TaxExemptionMappings.yml");
        ObjectMapper mapper = new YAMLMapper();
        return mapper.readValue(inputStream, TaxExemptionMappings.class);
    }

    private static InputStreamReader getResourceInMiscAsInputStreamReader(final String resourceName) throws FileNotFoundException {
        final FileInputStream fileStream = new FileInputStream(EceAppConstants.APP_MISC_RESOURCE_PATH + resourceName);
        return new InputStreamReader(fileStream, StandardCharsets.UTF_8);
    }

    private static InputStreamReader getResourceInPayloadDirAsInputStreamReader(final String resourceName) throws FileNotFoundException {
        final FileInputStream fileStream = new FileInputStream(EceAppConstants.APP_PAYLOAD_RESOURCE_PATH + resourceName);
        return new InputStreamReader(fileStream, StandardCharsets.UTF_8);
    }

    private static Map<?, ?> getResourceByMiscPath(final String resourceName) {
        return YamlUtil.loadYmlWithFileLocation(EceAppConstants.APP_MISC_RESOURCE_PATH + resourceName);
    }

    private static Map<?, ?> getResourceByEnvironmentPath(final String resourceName) {
        return YamlUtil.loadYmlWithFileLocation(EceAppConstants.APP_ENV_RESOURCE_PATH + resourceName);
    }
}
