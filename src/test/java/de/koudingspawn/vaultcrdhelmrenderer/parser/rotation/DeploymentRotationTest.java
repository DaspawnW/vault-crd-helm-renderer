package de.koudingspawn.vaultcrdhelmrenderer.parser.rotation;

import de.koudingspawn.vaultcrdhelmrenderer.utils.FileUtils;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DeploymentRotationTest {

    @ParameterizedTest
    @ValueSource(strings = {"deployment-with-volume-mount.yaml", "deployment-with-env.yaml", "deployment-with-env-from.yaml", "deployment-with-init-env.yaml", "deployment-with-init-env-from.yaml"})
    void referencesSecretInVolumeMount(String resource) {
        String s = FileUtils.fileAsString(String.format("/yamls/deploymentrotation/%s", resource));
        Deployment deployment = Serialization.unmarshal(s, Deployment.class);

        Deployment annotatedResource = (Deployment) new DeploymentRotation().annotate(deployment, "application-properties", "annotation-value");
        Assertions.assertTrue(annotatedResource.getSpec().getTemplate().getMetadata().getAnnotations().containsKey("vault.koudingspawn.de-application-properties/compare"));
        Assertions.assertEquals("annotation-value", annotatedResource.getSpec().getTemplate().getMetadata().getAnnotations().get("vault.koudingspawn.de-application-properties/compare"));
    }

    @Test
    void shouldNotAnnotateResource() {
        String s = FileUtils.fileAsString("/yamls/deploymentrotation/deployment-without.yaml");
        Deployment deployment = Serialization.unmarshal(s, Deployment.class);

        Deployment annotatedResource = (Deployment) new DeploymentRotation().annotate(deployment, "application-properties", "annotation-value");
        Assertions.assertFalse(annotatedResource.getSpec().getTemplate().getMetadata().getAnnotations().containsKey("vault.koudingspawn.de-application-properties/compare"));
    }

}