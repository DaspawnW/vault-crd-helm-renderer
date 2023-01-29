package de.koudingspawn.vaultcrdhelmrenderer.parser.rotation;

import de.koudingspawn.vaultcrdhelmrenderer.utils.FileUtils;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class StatefulsetRotationTest {

    @ParameterizedTest
    @ValueSource(strings = {"statefulset-with-volume-mount.yaml", "statefulset-with-env.yaml", "statefulset-with-env-from.yaml", "statefulset-with-init-env.yaml", "statefulset-with-init-env-from.yaml"})
    void referencesSecretInVolumeMount(String resource) {
        String s = FileUtils.fileAsString("/yamls/statefulsetrotation/%s".formatted(resource));
        StatefulSet statefulset = Serialization.unmarshal(s, StatefulSet.class);

        StatefulSet annotatedResource = (StatefulSet) new StatefulsetRotation().annotate(statefulset, "application-properties", "annotation-value");
        Assertions.assertTrue(annotatedResource.getSpec().getTemplate().getMetadata().getAnnotations().containsKey("vault.koudingspawn.de-application-properties/compare"));
        Assertions.assertEquals("annotation-value", annotatedResource.getSpec().getTemplate().getMetadata().getAnnotations().get("vault.koudingspawn.de-application-properties/compare"));
    }

    @Test
    void shouldNotAnnotateResource() {
        String s = FileUtils.fileAsString("/yamls/statefulsetrotation/statefulset-without.yaml");
        StatefulSet statefulSet = Serialization.unmarshal(s, StatefulSet.class);

        StatefulSet annotatedResource = (StatefulSet) new StatefulsetRotation().annotate(statefulSet, "application-properties", "annotation-value");
        Assertions.assertFalse(annotatedResource.getSpec().getTemplate().getMetadata().getAnnotations().containsKey("vault.koudingspawn.de-application-properties/compare"));
    }

}
