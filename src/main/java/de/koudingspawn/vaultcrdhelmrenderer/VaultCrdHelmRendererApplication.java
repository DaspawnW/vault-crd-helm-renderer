package de.koudingspawn.vaultcrdhelmrenderer;

public class VaultCrdHelmRendererApplication {

    public static void main(String[] args) {
        new VaultCrdHelmRendererApplication().runner();
    }

    public void runner() {
        String vaultAddr = System.getenv("VAULT_ADDR");
        String vaultToken = System.getenv("VAULT_TOKEN");

        Processor processor = new Processor(vaultAddr, vaultToken);
        String input = processor.readInput();

        try {
            System.out.println(processor.processYaml(input));
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

}
