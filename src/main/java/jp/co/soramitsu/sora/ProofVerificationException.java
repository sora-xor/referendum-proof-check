package jp.co.soramitsu.sora;

public class ProofVerificationException extends RuntimeException {

  public ProofVerificationException(String message) {
    super(message);
  }

  public ProofVerificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
