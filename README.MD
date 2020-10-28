# Referendum votes verifier

## Goal
This guide is aimed to explain how to verify honesty of first SORA's referendum proof.

## Environment requirements
You need to have installed Java at least version 8. If you don't have it, install it in the most
 convenient way for you. For example, from [official site](https://openjdk.java.net/install/index.html)
 or with [Sdkman](https://sdkman.io/install).
 
## For impatient readers
1. Open source code of class `jp.co.soramitsu.sora.EntryPoint`
2. There you can see such line of code

        proofChecker.checkProofsByWalletAccountId("<your wallet account id here>");

    Put your wallet account id in Iroha as method's param. If you don't know it then see section 
    ["How to know my wallet account id"](#how-to-know-my-wallet-account-id)
3. Run method `main`

If the user with specified wallet account id had voted for referendum, then you will see 
a Merkle path in standard output. 

## More verbose explanation
### User's DID (Digital IDentity)
Every user in SORA has DID and keypair associated with this DID. DID is created by SORA's mobile application
and persisted in Iroha. Before public key is committed in Iroha signature of request is checked. This 
allows proving subject who performs committing really own this private key and intention to commit was 
not changed after creation. Committing public key allows to bind it with user's DID and later any person can 
retrieve public key of user by his DID. 

### DAuth
Every outgoing request from SORA's mobile application to SORA NET's services signed by this keypair. 
We call it **DAuth**. All significant data from the request is concated into one string (Request method name +
Full url + Request body + Epoch milliseconds from header + User's DID from header + id of User's 
public key from header) and signed with private key. This allow to identify and check integrity of request. 

### Voting proofs
Request to vote for referendum is also was signed by DAuth. Request has signature of voting details, including 
amount of votes to vote. This signature signed by user's public key. Voting trigger transaction in Iroha with command
 [SetAccountDetail](https://iroha.readthedocs.io/en/master/develop/api/commands.html#set-account-detail). 
In this command key is hash(SHA3-256) of signature and value is JSON with hex representation of signature and payload which 
contains details of voting. Example

    {
       "sig":"0038865abe7d2515074d10092ec257c444c9883e00778638a1bcfa8345c94645bfa87380e78a051ad82689704e4c88b7389fcc18ee074d42a7f045dbc64aa60b",
       "payload":"POSThttps://s1.sora.soramitsu.co.jp/project/v1/referendum/5cf070da-480d-498a-ab27-c51b020ccb9b/support?votes=51601026172667did:sora:6bcb68cc7518aaebdb71did:sora:6bcb68cc7518aaebdb71#keys-1"
    } 
        
Here `sig` is signature of `payload` signed by private key of the User.

We gather this proofs in dedicated Iroha account until referendum's deadline reached.

### Merkle trie and Merkle root
When referendum voting is finished we initiate calculating of Merkle root of signatures of 
all proofs. We retrieve all committed in Iroha proofs and sort them in [lexicographic order](https://en.wikipedia.org/wiki/Lexicographic_order).
Then we create a [Merkle Tree](https://en.wikipedia.org/wiki/Merkle_tree) of all signatures. As digest
algorithm we use SHA3-256. Eventually we commit root of Merkle trie in the Iroha. 

### Prove my voting was counted
To provide ability to check honesty of referendum all proofs of referendum were retrieved from
Iroha and zipped into archive in this repository. You can find them in root folder `proofs`. During first run this archive will be unzipped. 
Runner code can be found in `jp.co.soramitsu.sora.EntryPoint`. There is single static method `main`.
In this method you need to put your wallet account id as param of method invocation.

    proofChecker.checkProofsByWalletAccountId("<your wallet account id here>");

After that just run the `main` method. Firstly, all proofs will be parsed, sorted and new Merkle trie
will be created. The trie root will be compared with known root to be sure the same Merkle trie was
created. Then for every voting will be created Merkle path. This path allow us to prove integrity and presence 
of voting without necessity to recalculate hash of every elements in trie. The digest algorithm remain
still the same SHA3-256. Let's consider example path 
 
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

That do we see here?  The first entry is hash of signature.
Remember this signature was created from all significant data of intention to vote and signed by user's
private key. To totally verify the path we need to get first and second entries, each of them need to decode 
from hex string to bytes and concat them(merge in one byte array). After that we need to calculate hash of this
array. We will get some new byte array. 
As a next step we need to get next entry below and repeat the same steps. Decode it from hex string to byte 
array and concat into one byte array with hash from a previous step. Again we need to calculate 
hash. If we will repeat this steps until last entry at the end we will get a byte array as the same as expected 
root of Merkle trie. It proves our voting was count. 

## FAQ
### How to get my wallet account id?
You should make the list of steps in order to get your wallet account id in the mobile application:
1. Open the Wallet tab
2. Click on your VAL account
3. In the bottom select "Copy SORA wallet address"
4. Paste the result somewhere. It should look like did_sora_xxxxxxxxxxxxxxxxx@sora
### What is SORA NET?
SORA NET is built on Iroha blockchain. It consists of Iroha itself and some services that provide API for Iroha.
### What is DID(Digital IDentity)
DIDs are URLs that relate a DID subject to means for trustable
interactions with that subject. DIDs resolve to DID Documents — simple documents that describe how 
to use that specific DID. Each DID Document contains at least three things: cryptographic material,
authentication suites, and service endpoints. Cryptographic material combined with authentication 
suites provide a set of mechanisms to authenticate as the DID subject (e.g. public keys, pseudonymous 
biometric templates, etc.). Service endpoints enable trusted interactions with the DID subject.
DID is a unique identifier, which identifies company, people, service or device.
### How to get public key if I know DID?
With public key it is possible to verify a payload was created by user and was not changed after sign. To retrieve
public key need to perform such GET request:

    curl -L -X GET 'https://s1.sora.soramitsu.co.jp/didresolver/v1/did/<DID of the user here>'
       
DID can be obtained from wallet account id by this pattern 

    did_sora_foo@sora --> did:sora:foo




