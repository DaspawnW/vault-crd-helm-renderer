package de.koudingspawn.vaultcrdhelmrenderer.parser.rotation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.util.HashMap;

import static de.koudingspawn.vaultcrdhelmrenderer.parser.rotation.ContainerReferencesSecretUtil.hasSecretInEnvFrom;
import static de.koudingspawn.vaultcrdhelmrenderer.parser.rotation.ContainerReferencesSecretUtil.hasSecretInEnv;

public class StatefulsetRotation implements Rotation {
    @Override
    public String getKind() {
        return "StatefulSet";
    }

    @Override
    public String getApiVersion() {
        return "apps/v1";
    }

    @Override
    public HasMetadata annotate(HasMetadata obj, String secretName, String annotation) {
        StatefulSet statefulSet = Serialization.unmarshal(Serialization.asYaml(obj), StatefulSet.class);

        if (volumeContainsSecret(statefulSet, secretName) ||
                envContainerContainsSecret(statefulSet, secretName) || envFromContainerContainsSecret(statefulSet, secretName) ||
                envInitContainerContainsSecret(statefulSet, secretName) || envFromInitContainerContainsSecret(statefulSet, secretName)) {

            if (statefulSet.getSpec().getTemplate().getMetadata().getAnnotations() == null) {
                statefulSet.getSpec().getTemplate().getMetadata().setAnnotations(new HashMap<>());
            }

            String annotationName = "vault.koudingspawn.de-%s/compare".formatted(secretName);
            statefulSet.getSpec().getTemplate().getMetadata().getAnnotations().put(annotationName, annotation);
        }


        return statefulSet;
    }

    private boolean volumeContainsSecret(StatefulSet statefulSet, String secretName) {
        if (statefulSet.getSpec() != null &&
                statefulSet.getSpec().getTemplate() != null &&
                statefulSet.getSpec().getTemplate().getSpec() != null &&
                statefulSet.getSpec().getTemplate().getSpec().getVolumes() != null) {
            return statefulSet.getSpec().getTemplate().getSpec().getVolumes()
                    .stream()
                    .filter(v -> v.getSecret() != null)
                    .filter(v -> v.getSecret().getSecretName() != null)
                    .anyMatch(v -> v.getSecret().getSecretName().equals(secretName));
        }

        return false;
    }

    private boolean envContainerContainsSecret(StatefulSet statefulSet, String secretName) {
        if (hasContainer(statefulSet)) {
            return hasSecretInEnv(statefulSet.getSpec().getTemplate().getSpec().getContainers(), secretName);
        }

        return false;
    }

    private boolean envFromContainerContainsSecret(StatefulSet statefulSet, String secretName) {
        if (hasContainer(statefulSet)) {
            return hasSecretInEnvFrom(statefulSet.getSpec().getTemplate().getSpec().getContainers(), secretName);
        }

        return false;
    }

    private boolean envInitContainerContainsSecret(StatefulSet statefulSet, String secretName) {
        if (hasInitContainer(statefulSet)) {
            return hasSecretInEnv(statefulSet.getSpec().getTemplate().getSpec().getInitContainers(), secretName);
        }

        return false;
    }

    private boolean envFromInitContainerContainsSecret(StatefulSet statefulSet, String secretName) {
        if (hasInitContainer(statefulSet)) {
            return hasSecretInEnvFrom(statefulSet.getSpec().getTemplate().getSpec().getInitContainers(), secretName);
        }

        return false;
    }

    private boolean hasContainer(StatefulSet statefulSet) {
        return statefulSet.getSpec() != null &&
                statefulSet.getSpec().getTemplate() != null &&
                statefulSet.getSpec().getTemplate().getSpec() != null &&
                statefulSet.getSpec().getTemplate().getSpec().getContainers() != null;
    }

    private boolean hasInitContainer(StatefulSet statefulSet) {
        return statefulSet.getSpec() != null &&
                statefulSet.getSpec().getTemplate() != null &&
                statefulSet.getSpec().getTemplate().getSpec() != null &&
                statefulSet.getSpec().getTemplate().getSpec().getInitContainers() != null;
    }
}
