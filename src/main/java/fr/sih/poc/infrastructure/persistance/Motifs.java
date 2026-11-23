package fr.sih.poc.infrastructure.persistance;

/** Construction de motifs LIKE en neutralisant les jokers saisis par l'utilisateur. */
final class Motifs {

    private Motifs() {
    }

    static String contient(String texte) {
        String echappe = texte.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + echappe + "%";
    }
}
