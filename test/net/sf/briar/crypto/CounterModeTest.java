package net.sf.briar.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import junit.framework.TestCase;
import net.sf.briar.api.Bytes;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

public class CounterModeTest extends TestCase {

	private static final String CIPHER_ALGO = "AES";
	private static final String CIPHER_MODE = "AES/CTR/NoPadding";
	private static final String PROVIDER = "BC";
	private static final int KEY_SIZE_BYTES = 32; // AES-256
	private static final int BLOCK_SIZE_BYTES = 16;

	private final SecureRandom random;
	private final byte[] keyBytes;
	private final SecretKeySpec key;

	public CounterModeTest() {
		super();
		Security.addProvider(new BouncyCastleProvider());
		random = new SecureRandom();
		keyBytes = new byte[KEY_SIZE_BYTES];
		random.nextBytes(keyBytes);
		key = new SecretKeySpec(keyBytes, CIPHER_ALGO);
	}

	@Test
	public void testEveryBitOfIvIsSignificant()
	throws GeneralSecurityException {
		// Set each bit of the IV in turn, encrypt the same plaintext and check
		// that all the resulting ciphertexts are distinct
		byte[] plaintext = new byte[BLOCK_SIZE_BYTES];
		random.nextBytes(plaintext);
		Set<Bytes> ciphertexts = new HashSet<Bytes>();
		for(int i = 0; i < BLOCK_SIZE_BYTES * 8; i++) {
			// Set the i^th bit of the IV
			byte[] ivBytes = new byte[BLOCK_SIZE_BYTES];
			ivBytes[i / 8] |= (byte) (128 >> i % 8);
			IvParameterSpec iv = new IvParameterSpec(ivBytes);
			// Encrypt the plaintext
			Cipher cipher = Cipher.getInstance(CIPHER_MODE, PROVIDER);
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			byte[] ciphertext =
				new byte[cipher.getOutputSize(plaintext.length)];
			cipher.doFinal(plaintext, 0, plaintext.length, ciphertext, 0);
			ciphertexts.add(new Bytes(ciphertext));
		}
		// All the ciphertexts should be distinct using Arrays.equals()
		assertEquals(BLOCK_SIZE_BYTES * 8, ciphertexts.size());
	}

	@Test
	public void testRepeatedIvsProduceRepeatedCiphertexts()
	throws GeneralSecurityException {
		// This is the inverse of the previous test, to check that the
		// distinct ciphertexts were due to using distinct IVs
		byte[] plaintext = new byte[BLOCK_SIZE_BYTES];
		random.nextBytes(plaintext);
		byte[] ivBytes = new byte[BLOCK_SIZE_BYTES];
		random.nextBytes(ivBytes);
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		Set<Bytes> ciphertexts = new HashSet<Bytes>();
		for(int i = 0; i < BLOCK_SIZE_BYTES * 8; i++) {
			Cipher cipher = Cipher.getInstance(CIPHER_MODE, PROVIDER);
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			byte[] ciphertext =
				new byte[cipher.getOutputSize(plaintext.length)];
			cipher.doFinal(plaintext, 0, plaintext.length, ciphertext, 0);
			ciphertexts.add(new Bytes(ciphertext));
		}
		assertEquals(1, ciphertexts.size());
	}

	@Test
	public void testLeastSignificantBitsUsedAsCounter()
	throws GeneralSecurityException {
		// Initialise the least significant 32 bits of the IV to zero and
		// encrypt ten blocks of zeroes
		byte[] plaintext = new byte[BLOCK_SIZE_BYTES * 10];
		byte[] ivBytes = new byte[BLOCK_SIZE_BYTES];
		random.nextBytes(ivBytes);
		for(int i = BLOCK_SIZE_BYTES - 4; i < BLOCK_SIZE_BYTES; i++) {
			ivBytes[i] = 0;
		}
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		Cipher cipher = Cipher.getInstance(CIPHER_MODE, PROVIDER);
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] ciphertext =
			new byte[cipher.getOutputSize(plaintext.length)];
		cipher.doFinal(plaintext, 0, plaintext.length, ciphertext, 0);
		// Initialise the least significant 32 bits of the IV to one and
		// encrypt another ten blocks of zeroes
		for(int i = BLOCK_SIZE_BYTES - 4; i < BLOCK_SIZE_BYTES; i++) {
			assertEquals(0, ivBytes[i]);
		}
		ivBytes[BLOCK_SIZE_BYTES - 1] = 1;
		iv = new IvParameterSpec(ivBytes);
		cipher = Cipher.getInstance(CIPHER_MODE, PROVIDER);
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] ciphertext1 =
			new byte[cipher.getOutputSize(plaintext.length)];
		cipher.doFinal(plaintext, 0, plaintext.length, ciphertext1, 0);
		// The last nine blocks of the first ciphertext should be identical to
		// the first nine blocks of the second ciphertext
		for(int i = 0; i < BLOCK_SIZE_BYTES * 9; i++) {
			assertEquals(ciphertext[i + BLOCK_SIZE_BYTES], ciphertext1[i]);
		}
	}

	@Test
	public void testCounterUsesMoreThan32Bits()
	throws GeneralSecurityException {
		// Initialise the least significant bits of the IV to 2^32-1 and
		// encrypt ten blocks of zeroes
		byte[] plaintext = new byte[BLOCK_SIZE_BYTES * 10];
		byte[] ivBytes = new byte[BLOCK_SIZE_BYTES];
		random.nextBytes(ivBytes);
		ivBytes[BLOCK_SIZE_BYTES - 5] = 0;
		for(int i = BLOCK_SIZE_BYTES - 4; i < BLOCK_SIZE_BYTES; i++) {
			ivBytes[i] = (byte) 255;
		}
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		Cipher cipher = Cipher.getInstance(CIPHER_MODE, PROVIDER);
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] ciphertext =
			new byte[cipher.getOutputSize(plaintext.length)];
		cipher.doFinal(plaintext, 0, plaintext.length, ciphertext, 0);
		// Initialise the least significant bits of the IV to 2^32 and
		// encrypt another ten blocks of zeroes
		assertEquals(0, ivBytes[BLOCK_SIZE_BYTES - 5]);
		for(int i = BLOCK_SIZE_BYTES - 4; i < BLOCK_SIZE_BYTES; i++) {
			assertEquals((byte) 255, ivBytes[i]);
		}
		ivBytes[BLOCK_SIZE_BYTES - 5] = 1;
		for(int i = BLOCK_SIZE_BYTES - 4; i < BLOCK_SIZE_BYTES; i++) {
			ivBytes[i] = 0;
		}
		iv = new IvParameterSpec(ivBytes);
		cipher = Cipher.getInstance(CIPHER_MODE, PROVIDER);
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] ciphertext1 =
			new byte[cipher.getOutputSize(plaintext.length)];
		cipher.doFinal(plaintext, 0, plaintext.length, ciphertext1, 0);
		// The last nine blocks of the first ciphertext should be identical to
		// the first nine blocks of the second ciphertext
		for(int i = 0; i < BLOCK_SIZE_BYTES * 9; i++) {
			assertEquals(ciphertext[i + BLOCK_SIZE_BYTES], ciphertext1[i]);
		}
	}
}
