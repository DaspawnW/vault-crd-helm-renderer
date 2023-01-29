package de.koudingspawn.vaultcrdhelmrenderer;

import de.koudingspawn.vaultcrdhelmrenderer.utils.FileUtils;
import de.koudingspawn.vaultcrdhelmrenderer.vault.SecretNotAccessibleException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Testcontainers
class ProcessorIntegrationTest {

    private static String VAULT_TOKEN = "dev";
    @Container
    public static VaultContainer<?> vaultContainer = new VaultContainer<>("vault:1.6.1")
            .withVaultToken(VAULT_TOKEN)
            .withInitCommand("secrets enable --version=2 --path=datasource kv",
                    "kv put datasource/host host=localhost",
                    "kv put datasource/credentials username=user123 password=password123");

    @Test
    void shouldProcessYamlFile() throws SecretNotAccessibleException {
        String s = FileUtils.fileAsString("/yamls/output.yaml");
        String renderedYaml = new Processor(vaultContainer.getHttpHostAddress(), VAULT_TOKEN).processYaml(s);

        List<HasMetadata> resources = listOfResources(renderedYaml);
        Optional<Secret> secret = resources
                .stream()
                .filter(r -> r.getKind().equals("Secret"))
                .filter(r -> r.getMetadata().getName().equals("application-properties"))
                .map(Secret.class::cast)
                .findAny();

        Assertions.assertTrue(secret.isPresent());
        Assertions.assertTrue(secret.get().getData().containsKey("application.properties"));
        String appProps = new String(Base64.getDecoder().decode(secret.get().getData().get("application.properties")));

        Assertions.assertTrue(appProps.contains("spring.datasource.url=jdbc:postgresql://localhost:5432/database_name"));
        Assertions.assertTrue(appProps.contains("spring.datasource.username=user123"));
        Assertions.assertTrue(appProps.contains("spring.datasource.password=password123"));

        Optional<Deployment> deployment = resources
                .stream()
                .filter(r -> r.getKind().equals("Deployment"))
                .map(Deployment.class::cast)
                .findAny();

        Assertions.assertTrue(deployment.isPresent());
        Assertions.assertEquals("/okdObRP1C739CXvOm2mCX+j5Jfz0+bDyseebmNg3/k=", deployment.get().getSpec().getTemplate().getMetadata().getAnnotations().get("vault.koudingspawn.de-application-properties/compare"));
    }

    @Test
    void shouldProcessYamlFileWithoutPropertiesSecret() throws SecretNotAccessibleException {
        String s = FileUtils.fileAsString("/yamls/without-match.yaml");
        String renderedYaml = new Processor(vaultContainer.getHttpHostAddress(), VAULT_TOKEN).processYaml(s);

        List<HasMetadata> resources = listOfResources(renderedYaml);

        Assertions.assertTrue(resources.stream()
                .anyMatch(r -> r.getKind().equals("Vault")));
        Assertions.assertTrue(resources.stream()
                .anyMatch(r -> r.getKind().equals("Service")));
        Assertions.assertTrue(resources.stream()
                .anyMatch(r -> r.getKind().equals("Deployment")));

        Optional<Deployment> deployment = resources
                .stream()
                .filter(r -> r.getKind().equals("Deployment"))
                .map(Deployment.class::cast)
                .findAny();
        Assertions.assertFalse(deployment.get().getSpec().getTemplate().getMetadata().getAnnotations().containsKey("vault.koudingspawn.de-application-properties/compare"));
    }

    private List<HasMetadata> listOfResources(String yaml) {
        List<HasMetadata> resources = new ArrayList<>();
        for (String s : yaml.split("---")) {
            HasMetadata resource = Serialization.unmarshal(s);
            if (resource != null) {
                resources.add(resource);
            }
        }

        return resources;
    }

}