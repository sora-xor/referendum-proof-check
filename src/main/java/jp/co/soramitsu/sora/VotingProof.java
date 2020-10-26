package jp.co.soramitsu.sora;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jp.co.soramitsu.sora.sdk.did.model.dto.DID;


public class VotingProof {

  /**
   * ID of the referendum
   */
  UUID referendumId;
  /**
   * Concated details of voting
   */
  String payload;
  /**
   * Signature of {@link #payload} signed by voter's public key and represented as hex string
   */
  String signatureHex;
  /**
   * True if it was support voting or false otherwise
   */
  boolean isSupport;
  /**
   * How much votes voter gave for referendum
   */
  BigDecimal givenVotes;
  /**
   * Digital IDentity of the voter
   */
  DID authId;
  /**
   * Key id of the user. User may has various public keys
   */
  DID publicKeyId;
  /**
   * Time of creation of signature
   */
  Instant authTimestamp;

  public VotingProof() {
  }

  public UUID getReferendumId() {
    return referendumId;
  }

  public void setReferendumId(UUID referendumId) {
    this.referendumId = referendumId;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getSignatureHex() {
    return signatureHex;
  }

  public void setSignatureHex(String signatureHex) {
    this.signatureHex = signatureHex;
  }

  public boolean getIsSupport() {
    return isSupport;
  }

  public void setIsSupport(boolean support) {
    isSupport = support;
  }

  public BigDecimal getGivenVotes() {
    return givenVotes;
  }

  public void setGivenVotes(BigDecimal givenVotes) {
    this.givenVotes = givenVotes;
  }

  public DID getAuthId() {
    return authId;
  }

  public void setAuthId(DID authId) {
    this.authId = authId;
  }

  public DID getPublicKeyId() {
    return publicKeyId;
  }

  public void setPublicKeyId(DID publicKeyId) {
    this.publicKeyId = publicKeyId;
  }

  public Instant getAuthTimestamp() {
    return authTimestamp;
  }

  public void setAuthTimestamp(Instant authTimestamp) {
    this.authTimestamp = authTimestamp;
  }

  @Override
  public String toString() {
    return "VotingProof{" +
        "referendumId=" + referendumId +
        ", payload='" + payload + '\'' +
        ", signatureHex='" + signatureHex + '\'' +
        ", isSupport=" + isSupport +
        ", givenVotes=" + givenVotes +
        ", authId=" + authId +
        ", publicKeyId=" + publicKeyId +
        ", authTimestamp=" + authTimestamp +
        '}';
  }
}
