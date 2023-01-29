package de.koudingspawn.vaultcrdhelmrenderer.parser.rotation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.util.HashMap;

import static de.koudingspawn.vaultcrdhelmrenderer.parser.rotation.ContainerReferencesSecretUtil.hasSecretInEnvFrom;
import static de.koudingspawn.vaultcrdhelmrenderer.parser.rotation.ContainerReferencesSecretUtil.hasSecretInEnv;

public class DeploymentRotation implements Rotation {
    @Override
    public String getKind() {
        return "Deployment";
    }

    @Override
    public String getApiVersion() {
        return "apps/v1";
    }

    @Override
    public HasMetadata annotate(HasMetadata obj, String secretName, String annotation) {
        Deployment deployment = Serialization.unmarshal(Serialization.asYaml(obj), Deployment.class);

        if (volumeContainsSecret(deployment, secretName) ||
                envContainerContainsSecret(deployment, secretName) || envFromContainerContainsSecret(deployment, secretName) ||
                envInitContainerContainsSecret(deployment, secretName) || envFromInitContainerContainsSecret(deployment, secretName)) {

            if (deployment.getSpec().getTemplate().getMetadata().getAnnotations() == null) {
                deployment.getSpec().getTemplate().getMetadata().setAnnotations(new HashMap<>());
            }

            String annotationName = "vault.koudingspawn.de-%s/compare".formatted(secretName);
            deployment.getSpec().getTemplate().getMetadata().getAnnotations().put(annotationName, annotation);
        }

        return deployment;
    }

    private boolean volumeContainsSecret(Deployment deployment, String secretName) {
        if (deployment.getSpec() != null &&
                deployment.getSpec().getTemplate() != null &&
                deployment.getSpec().getTemplate().getSpec() != null &&
                deployment.getSpec().getTemplate().getSpec().getVolumes() != null) {

            return deployment.getSpec().getTemplate().getSpec().getVolumes()
                    .stream()
                    .filter(volume -> volume.getSecret() != null)
                    .filter(volume -> volume.getSecret().getSecretName() != null)
                    .anyMatch(volume -> volume.getSecret().getSecretName().equals(secretName));
        }

        return false;
    }

    private boolean envContainerContainsSecret(Deployment deployment, String secretName) {
        if (hasContainer(deployment)) {
            return hasSecretInEnv(deployment.getSpec().getTemplate().getSpec().getContainers(), secretName);
        }

        return false;
    }

    private boolean envFromContainerContainsSecret(Deployment deployment, String secretName) {
        if (hasContainer(deployment)) {
            return hasSecretInEnvFrom(deployment.getSpec().getTemplate().getSpec().getContainers(), secretName);
        }

        return false;
    }

    private boolean envInitContainerContainsSecret(Deployment deployment, String secretName) {
        if (hasInitContainer(deployment)) {
            return hasSecretInEnv(deployment.getSpec().getTemplate().getSpec().getInitContainers(), secretName);
        }

        return false;
    }

    private boolean envFromInitContainerContainsSecret(Deployment deployment, String secretName) {
        if (hasInitContainer(deployment)) {
            return hasSecretInEnvFrom(deployment.getSpec().getTemplate().getSpec().getInitContainers(), secretName);
        }

        return false;
    }

    private boolean hasContainer(Deployment deployment) {
        return deployment.getSpec() != null &&
                deployment.getSpec().getTemplate() != null &&
                deployment.getSpec().getTemplate().getSpec() != null &&
                deployment.getSpec().getTemplate().getSpec().getContainers() != null;
    }

    private boolean hasInitContainer(Deployment deployment) {
        return deployment.getSpec() != null &&
                deployment.getSpec().getTemplate() != null &&
                deployment.getSpec().getTemplate().getSpec() != null &&
                deployment.getSpec().getTemplate().getSpec().getInitContainers() != null;
    }


}
