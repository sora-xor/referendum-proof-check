# Referendum Vote Verifier

## Community test ⚠️
This instruction contains proofs and Merkle root for Community test environment of Sora project. For Sora's production environment you can find instruction and code [here](https://github.com/sora-xor/referendum-proof-check).

## Goal
This guide explains how to verify the fairness of the first SORA referendum proof.

## Environment requirements
You need to have Java at least of version 8 installed. If you don't have it, install it the most
 convenient way for you. For example, from [the official website](https://openjdk.java.net/install/index.html)
 or with [Sdkman](https://sdkman.io/install).
 
## For impatient readers
1. Open the source code of class `jp.co.soramitsu.sora.EntryPoint`
2. There you can see this line of code:

        proofChecker.checkProofsByWalletAccountId("<your wallet account id here>");

    Put your wallet account id into Iroha as method's parameter. If you don't know it then see section 
    ["How to get my wallet account id"](#how-to-get-my-wallet-account-id)
3. Run method `main`

If the user with the specified wallet account id voted for the referendum, then you will see 
a Merkle path in the standard output. 

## More verbose explanation
### User's DID (Digital IDentity)
Every user in SORA has a DID and a keypair associated with this DID. The DID is created by SORA's mobile application
and is kept in Iroha. Prior to the public key being committed in Iroha, the signature of the request is checked. This 
allows to prove that the subject performing the commit really owns the private key and that their intention to commit was 
not changed after its creation. Committing the public key makes it possible to match it with user's DID so later any person could 
retrieve the public key of the user using their DID. 

### DAuth
Every outgoing request from SORA mobile application to SORA NET's services is signed by this keypair. 
We call it **DAuth**. All significant data from the request is concated into one string (Request method name +
Full url + Request body + Epoch milliseconds from the header + User's DID from the header + id of User's 
public key from the header) and signed with a private key. This helps to identify and check the integrity of the request. 

### Voting proofs
Request to vote for the referendum was also signed by DAuth. Request has the signature of voting details, including the 
amount of votes. This signature is signed by the user's public key. Voting triggers a transaction in Iroha with the
 [SetAccountDetail](https://iroha.readthedocs.io/en/master/develop/api/commands.html#set-account-detail) 
command. 
In this command the key is a hash (SHA3-256) of the signature and value is a JSON with hex representation of the signature and payload which 
contains details of the voting. Example:

    {
       "sig":"0038865abe7d2515074d10092ec257c444c9883e00778638a1bcfa8345c94645bfa87380e78a051ad82689704e4c88b7389fcc18ee074d42a7f045dbc64aa60b",
       "payload":"POSThttps://s1.sora.soramitsu.co.jp/project/v1/referendum/5cf070da-480d-498a-ab27-c51b020ccb9b/support?votes=51601026172667did:sora:6bcb68cc7518aaebdb71did:sora:6bcb68cc7518aaebdb71#keys-1"
    } 
        
Here `sig` is the signature of `payload` signed by a private key of the User.

We gather these proofs in a dedicated Iroha account until referendum deadline is reached.

### Merkle trie and Merkle root
When referendum voting is finished we initiate calculating of Merkle root of the signatures of 
all proofs. We retrieve all proofs committed in Iroha and sort them in [lexicographic order](https://en.wikipedia.org/wiki/Lexicographic_order).
Then we create a [Merkle Tree](https://en.wikipedia.org/wiki/Merkle_tree) of all signatures. We use SHA3-256 as  the digest
algorithm. Eventually, we commit the root of Merkle trie in Iroha. 

### Making sure your vote has been counted
To provide the ability to check the fairness of the referendum, all proofs of the referendum were retrieved from
Iroha and zipped into an archive in this repository. You can find them in the `proofs` root folder. During the first run this archive will be extracted. 
Runner code can be found in `jp.co.soramitsu.sora.EntryPoint`. There is a single static method `main`.
In this method you need to put your wallet account id as a parameter of the method invocation:

    proofChecker.checkProofsByWalletAccountId("<your wallet account id here>");

After that just run the `main` method. Firstly, all proofs will be parsed, sorted and a new Merkle trie
will be created. The trie root will be compared with the known root to be sure the same Merkle trie was
created. Then, a Merkle path will be created for every voting. This path allows us to prove the integrity and presence 
of voting without recalculating hashes of all the elements in the trie. The digest algorithm is
still the same SHA3-256. Let's consider an example path:
 
    hash: "5f4c3efa6551a92183e3fe2f76215f66f79e43571372ee53243bf7acf04471e7", position: 6979
    hash: "40a6d764bd637593890e2c76d492867648cdd23f08329c2e958f38af4a204f64", position: 6980
    hash: "70337bcbef1cffffafe9ce00befd5f213cc5a7df12c28cc974adc9cd02d8ef5b", position: 3490
    hash: "b8a5d32b5fd6dbbd76612cf5936325115822a244a0c121b8c8006a12f8d5f154", position: 1743
    hash: "2521f814a76ffc58950e7edbffd53ebee0934435b63e4602613d71afe2fd5674", position: 872
    hash: "a6eb00081e46367ddc751f37948052fc2bf03d6720099ce59e72720f94f9f947", position: 436
    hash: "6b36663fc8c88508fdd71e5fd2c045bcecaa32e3e59b4523001f19e8650be02e", position: 218
    hash: "67fe25ae1f1313b335722c5afea1dcaa1903c5343593c49f03915db199fe6553", position: 107
    hash: "fdc3a2d22b4315641c26b54b84fe9c341eae59961464c8e5593ec48b03cc9b66", position: 54
    hash: "5c641ce547297e72d5234e8c9a83a35353ac15faf3fec301415c5b206a9328db", position: 25
    hash: "d14d477ed8018a8ae6ac5d287116cea832d644905c8869e4a3cf6d72dba0b1b9", position: 11
    hash: "791d10ab585f42de3728bfb9958148e722542075490c07d7ead4a88b6aea0876", position: 6
    hash: "180b1d30e064ada8bc259b9da3ea1a09e9f6f48531ab35eefe650488e6942562", position: 1

That can we see here? The first entry is a hash of a signature.
Remember that this signature has been created from all significant data of the intention to vote and signed by the user's
private key. To ultimately verify the path, we need to get the first and the second entries, and each of them needs to be decoded 
from hex string to bytes and be concated (merged in one byte array). After that we need to calculate the hash of this
array. We will get some new byte array. 
As a next step we need to get the next entry below and repeat the same steps. Decode it from hex string to byte 
array and concat into one byte array with a hash from the previous step. Again, we need to calculate 
the hash. If we repeat these steps until the last entry, at the end we will get the same byte array as the expected 
root of the Merkle trie. It proves our voting has been counted. 

## FAQ
### How to get my wallet account id?
In Sora mobile application:
1. Open the Wallet tab
2. Click on your VAL account
3. At the bottom select "Copy SORA wallet address"
4. Paste the result somewhere. It should look like did_sora_xxxxxxxxxxxxxxxxx@sora

### What is SORA NET?
SORA NET is built on Hyperledger Iroha blockchain. It consists of Iroha itself and some services that provide API for it.

### What is DID (Digital IDentity)
DIDs are URLs created for a DID subject allow trusted
interactions with that subject. DIDs resolve to DID Documents — simple documents that describe how 
to use that specific DID. Each DID Document contains at least three things: cryptographic material,
authentication suites, and service endpoints. Cryptographic material combined with authentication 
suites provide a set of mechanisms to authenticate as the DID subject (e.g. public keys, pseudonymous 
biometric templates, etc.). Service endpoints enable trusted interactions with the DID subject.
DID is a unique identifier, which identifies company, people, a service or a device.

### How to calculate a public key if I know the DID?
With public key it is possible to verify that a payload had been created by the user and has not been changed after signing it. To retrieve the
public key perform a GET request:

    curl -L -X GET 'https://s1.sora.soramitsu.co.jp/didresolver/v1/did/<DID of the user here>'
       
DID can be obtained from the wallet account id with this pattern 

    did_sora_foo@sora --> did:sora:foo




