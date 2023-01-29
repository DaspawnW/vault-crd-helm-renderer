package de.koudingspawn.vaultcrdhelmrenderer;

import de.koudingspawn.vaultcrdhelmrenderer.crd.Vault;
import de.koudingspawn.vaultcrdhelmrenderer.parser.HelmChartParser;
import de.koudingspawn.vaultcrdhelmrenderer.vault.PropertiesGenerator;
import de.koudingspawn.vaultcrdhelmrenderer.vault.SecretNotAccessibleException;
import de.koudingspawn.vaultcrdhelmrenderer.vault.VaultCommunication;
import de.koudingspawn.vaultcrdhelmrenderer.vault.VaultTokenConnection;
import io.fabric8.kubernetes.api.model.Secret;

import java.util.Scanner;

public class Processor {

    private final VaultTokenConnection vaultTokenConnection;

    public Processor(String vaultAddr, String vaultToken) {
        this.vaultTokenConnection = new VaultTokenConnection(vaultAddr, vaultToken);
    }

    String processYaml(String yamlContent) throws SecretNotAccessibleException {
        HelmChartParser helmChartParser = new HelmChartParser(yamlContent);

        VaultCommunication vaultCommunication = new VaultCommunication(vaultTokenConnection.getVaultTemplate());
        PropertiesGenerator propertiesGenerator = new PropertiesGenerator(vaultCommunication);

        for (Vault vaultResource : helmChartParser.findVaultPropertiesResources()) {
            Secret secret = propertiesGenerator.generateSecret(vaultResource);
            helmChartParser.replaceVaultPropertyWithSecret(vaultResource, secret);
        }

        return helmChartParser.toString();
    }

    String readInput() {
        Scanner scanner = new Scanner(System.in);

        String input = "";
        while (scanner.hasNextLine()) {
            input += scanner.nextLine() + System.lineSeparator();
        }

        return input;
    }

}
