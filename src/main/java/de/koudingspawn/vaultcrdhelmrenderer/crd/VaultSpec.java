package de.koudingspawn.vaultcrdhelmrenderer.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaultSpec implements KubernetesResource {
    private VaultType type;
    private VaultPropertiesConfiguration propertiesConfiguration;

    public VaultType getType() {
        return type;
    }

    public VaultSpec setType(VaultType type) {
        this.type = type;
        return this;
    }

    public VaultPropertiesConfiguration getPropertiesConfiguration() {
        return propertiesConfiguration;
    }

    public VaultSpec setPropertiesConfiguration(VaultPropertiesConfiguration propertiesConfiguration) {
        this.propertiesConfiguration = propertiesConfiguration;
        return this;
    }
}
