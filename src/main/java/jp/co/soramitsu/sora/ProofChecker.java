package jp.co.soramitsu.sora;

import static org.spongycastle.util.encoders.Hex.decode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import jp.co.soramitsu.sora.sdk.crypto.common.Hash;
import jp.co.soramitsu.sora.sdk.crypto.merkle.MerkleNode;
import jp.co.soramitsu.sora.sdk.crypto.merkle.MerkleTree;
import jp.co.soramitsu.sora.sdk.crypto.merkle.MerkleTreeFactory;
import jp.co.soramitsu.sora.sdk.crypto.merkle.MerkleTreeProof;
import jp.co.soramitsu.sora.sdk.did.model.dto.DID;
import jp.co.soramitsu.sora.sdk.did.parser.generated.ParserException;
import org.spongycastle.jcajce.provider.digest.SHA3.Digest256;
import org.spongycastle.util.encoders.Hex;

public class ProofChecker {

  public static final Path PROOFS_PATH = Paths.get("proofs/proofs.json");
  public static final Path ZIPPED_PROOFS_PATH = Paths.get("proofs/proofs.zip");

  public static final String EXPECTED_REFERENDUM_MERKLE_ROOT = "7db3dfb4b137fc1313e1dc1227d7c14f9111f368b23705e52da0609ab4699df9";

  public static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .registerModule(new JavaTimeModule());

  public ProofChecker() {
    try {
      unzipIfNecessary();
    } catch (IOException e) {
      final String errMessage = "Could not unzip proofs. Please check file by path \"" +
          ZIPPED_PROOFS_PATH + "\" exists and permissions to read and write are granted, error: " + e.getMessage();
      throw new ProofVerificationException(errMessage, e);
    }
  }

  /**
   * Check proofs created by user with such walletAccountId
   *
   * @param walletAccountId - wallet account id to check proofs
   */
  public void checkProofsByWalletAccountId(String walletAccountId) {
    try {
      checkProofsByDid(createDidFromWalletAccountId(walletAccountId));
    } catch (ParserException e) {
      throw new ProofVerificationException("Incorrect wallet account format", e);
    }
  }

  /**
   * Check proofs created by user with such string representation of DID
   *
   * @param did - string representation of did
   */
  public void checkProofsByDid(String did) {
    try {
      checkProofsByDid(DID.parse(did));
    } catch (ParserException e) {
      throw new ProofVerificationException("Incorrect DID format", e);
    }
  }

  /**
   * Check proofs created by user with such DID
   *
   * @param did - DID of the user
   */
  public void checkProofsByDid(DID did) {
    final List<VotingProof> allProofs;
    try {
      allProofs = parseProofs();
    } catch (IOException e) {
      throw new ProofVerificationException("Could not parse file with proofs", e);
    }
    final MerkleTree merkleTree = createMerkleTree(allProofs);

    final String merkleRoot = Hex.toHexString(merkleTree.getRoot().getData());
    System.out.println(merkleRoot);
    if (!EXPECTED_REFERENDUM_MERKLE_ROOT.equalsIgnoreCase(merkleRoot)) {
      final String errMessage = "Expected and actual Merkle root does not match, expected: \"" +
          EXPECTED_REFERENDUM_MERKLE_ROOT + "\", actual: \"" + merkleRoot + "\"";
      throw new ProofVerificationException(errMessage);
    }
    final List<VotingProof> proofsByDid = findAllProofsByDid(allProofs, did);
    final Digest256 digest = new Digest256();
    for (VotingProof votingProof : proofsByDid) {
      final String sig = votingProof.getSignatureHex();
      final byte[] sigDigest = digest.digest(decode(sig));
      System.out.println("Creating Merkle proof for signature: " + sig + ", digest: " + Hex.toHexString(sigDigest));
      final MerkleTreeProof proof = merkleTree.createProof(new Hash(sigDigest));
      if (proof == null) {
        throw new ProofVerificationException("Could not create Merkle proof for signature: \"" + sig + "\", proof is null");
      }
      if(!proof.verify(Hash.fromHex(EXPECTED_REFERENDUM_MERKLE_ROOT))) {
        throw new ProofVerificationException("Could not verify Merkle path against root");
      }
      System.out.println("Merkle path for signature is:");
      for (MerkleNode node : proof.getPath()) {
        System.out.println("\thash: \"" + node.getHash() + "\", position: " + node.getPosition());
      }
      System.out.println();
      System.out.println();
    }
  }

    /**
     * Unzip archive with proofs if it has not already unzipped.
     * If the archive has already unzipped, then just returns.
     *
     * @throws IOException if there are any error during I/O operations
     */
    public void unzipIfNecessary() throws IOException {
    if (Files.exists(PROOFS_PATH)) {
      return;
    }
    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(ZIPPED_PROOFS_PATH))) {
      while ((zis.getNextEntry()) != null) {
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(PROOFS_PATH))) {
          byte[] buffer = new byte[1024];
          int len;
          while ((len = zis.read(buffer)) > 0) {
            os.write(buffer, 0, len);
          }
        }
      }
      zis.closeEntry();
    }
  }

  /**
   * Read list of proofs from file
   *
   * @return - list of proofs
   * @throws IOException if there are any error during I/O operations
   */
  public List<VotingProof> parseProofs() throws IOException {
    List<VotingProof> proofs;
    try (InputStream is = Files.newInputStream(PROOFS_PATH)) {
      proofs = MAPPER.readValue(is, new TypeReference<List<VotingProof>>(){});
    }
    proofs.sort(Comparator.comparing(VotingProof::getSignatureHex));
    return proofs;
  }

  /**
   * Filter out all proofs which is not belong to specific DID
   *
   * @param proofs - list of proofs to filter
   * @param target - DID as filter target
   * @return - list of proofs created from
   */
  private List<VotingProof> findAllProofsByDid(List<VotingProof> proofs, DID target) {
    return proofs.stream()
        .filter(votingProof -> votingProof.getAuthId().equals(target))
        .collect(Collectors.toList());
  }

  /**
   * Create DID from wallet account id
   * @param walletAccountId - source of transformation
   *
   * @return - DID if transformation was successful
   *
   * @throws ParserException if cannot transform wallet account id
   */
  public DID createDidFromWalletAccountId(String walletAccountId) throws ParserException {
    return DID.parse(walletAccountId.substring(0, walletAccountId.indexOf('@')).replace("_", ":"));
  }

  /**
   * Create Merkle Tree
   *
   * @param proofs - list of proofs from which signature tree will be created
   * @return created Merkle Tree
   */
  public MerkleTree createMerkleTree(List<VotingProof> proofs) {
    final Digest256 digest = new Digest256();
    List<Hash> leaves = proofs.stream()
        .map(proof -> new Hash(digest.digest(decode(proof.getSignatureHex()))))
        .collect(Collectors.toList());
    return new MerkleTreeFactory(digest).createFromLeaves(leaves);
  }
}
