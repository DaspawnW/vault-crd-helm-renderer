package de.koudingspawn.vaultcrdhelmrenderer.vault;

import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;

public class VaultTokenConnection extends AbstractVaultConfiguration {

    private final String vaultAddr;
    private final String vaultToken;

    public VaultTokenConnection() {
        this.vaultAddr = System.getenv("VAULT_ADDR");
        this.vaultToken = System.getenv("VAULT_TOKEN");
    }

    public VaultTokenConnection(String vaultAddr, String vaultToken) {
        this.vaultAddr = vaultAddr;
        this.vaultToken = vaultToken;
    }

    @Override
    public VaultEndpoint vaultEndpoint() {
        return VaultEndpoint.from(getVaultUrlWithoutPath(vaultAddr));
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(vaultToken);
    }

    public VaultTemplate getVaultTemplate() {
        return new VaultTemplate(this.vaultEndpoint(), this.clientAuthentication());
    }

    private static URI getVaultUrlWithoutPath(String vaultUrl) {
        return URI.create(vaultUrl.replace("/v1/", ""));
    }
}
