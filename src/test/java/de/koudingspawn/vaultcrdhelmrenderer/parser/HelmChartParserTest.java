package de.koudingspawn.vaultcrdhelmrenderer.parser;

import de.koudingspawn.vaultcrdhelmrenderer.crd.Vault;
import de.koudingspawn.vaultcrdhelmrenderer.utils.FileUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

class HelmChartParserTest {

    private static String rawYaml = FileUtils.fileAsString("/yamls/output.yaml");

    @Test
    void shouldParseYamlSuccessful() {
        Assertions.assertDoesNotThrow(() -> new HelmChartParser(rawYaml));
    }

    @Test
    void shouldFindVaultResources() {
        List<Vault> vaultPropertiesResources = new HelmChartParser(rawYaml).findVaultPropertiesResources();
        Assertions.assertEquals(1, vaultPropertiesResources.size());
        Assertions.assertEquals("application-properties", vaultPropertiesResources.get(0).getMetadata().getName());
    }

    @Test
    void shouldReplaceVaultResourceWithSecret() {
        HelmChartParser helmChartParser = new HelmChartParser(rawYaml);
        List<Vault> vaultPropertiesResources = helmChartParser.findVaultPropertiesResources();

        HashMap<String, String> data = new HashMap<>();
        data.put("application.properties", "VkVSWSBTRUNVUkUgU0VDUkVUCg==");
        Secret secret = new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder().withName("application-properties").build())
                .withData(data)
                .build();

        helmChartParser.replaceVaultPropertyWithSecret(vaultPropertiesResources.get(0), secret);
        String rawOutput = helmChartParser.toString();

        List<HasMetadata> vaultResources = Arrays.stream(rawOutput.split("---"))
                .map(Serialization::unmarshal)
                .map(HasMetadata.class::cast)
                .filter(HelmChartParser::isVaultResource).toList();
        Assertions.assertEquals(0, vaultResources.size());

        List<HasMetadata> secretResources = Arrays.stream(rawOutput.split("---"))
                .map(Serialization::unmarshal)
                .filter(Objects::nonNull)
                .map(HasMetadata.class::cast)
                .filter(r -> r.getKind().equalsIgnoreCase("Secret"))
                .toList();
        Assertions.assertEquals(1, secretResources.size());
    }

}