package jp.co.soramitsu.sora;

public class EntryPoint {

  /**
   * Check proof of voting for referendum
   * Put your own wallet account id. For example
   * {@code proofChecker.checkProofsByWalletAccountId("did_sora_3510542788352afc4167@sora")}
   */
  public static void main(String[] args) {
    ProofChecker proofChecker = new ProofChecker();
    proofChecker.checkProofsByWalletAccountId("<your wallet account id here>");
  }
}
