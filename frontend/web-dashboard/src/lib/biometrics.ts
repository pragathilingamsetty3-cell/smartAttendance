/**
 * Biometric Utility for WebAuthn interaction
 * Provides a realistic "Add Fingerprint" experience by triggering
 * the browser's native biometric prompts.
 */

export const isBiometricSupported = (): boolean => {
  return typeof window !== 'undefined' && 
         !!(window.PublicKeyCredential && 
            navigator.credentials && 
            navigator.credentials.create);
};

/**
 * 🔐 REGISTER biometric — called during setup
 * Uses navigator.credentials.create() to register a new WebAuthn credential.
 * Returns a unique signature string (WBA_<hex>) stored in the DB.
 */
export const registerBiometric = async (userName: string): Promise<string> => {
  if (!isBiometricSupported()) {
    throw new Error('Biometrics not supported on this device/browser');
  }

  try {
    // Generate a random challenge
    const challenge = new Uint8Array(32);
    window.crypto.getRandomValues(challenge);

    // Create a new credential (this triggers the OS fingerprint/FaceID prompt)
    const credential = await navigator.credentials.create({
      publicKey: {
        challenge,
        rp: {
          name: "Smart Attendance System",
          id: window.location.hostname === 'localhost' ? 'localhost' : window.location.hostname,
        },
        user: {
          id: Uint8Array.from(userName, c => c.charCodeAt(0)),
          name: userName,
          displayName: userName,
        },
        pubKeyCredParams: [{ alg: -7, type: "public-key" }], // ES256
        authenticatorSelection: {
          authenticatorAttachment: "platform", // Uses built-in sensors (TouchID, FaceID, Windows Hello)
          userVerification: "required",
        },
        timeout: 60000,
        attestation: "direct",
      }
    }) as PublicKeyCredential;

    if (!credential) {
      throw new Error('Biometric registration cancelled or failed');
    }

    // Extract the credential rawId as the biometric signature
    const rawId = new Uint8Array(credential.rawId);
    const signature = Array.from(rawId)
      .map(b => b.toString(16).padStart(2, '0'))
      .join('')
      .toUpperCase();

    // Also store the credential ID in localStorage so we can use it for verification later
    localStorage.setItem('sa_webauthn_credential_id', signature);

    return `WBA_${signature}`;
  } catch (error: any) {
    console.error('Biometric error:', error);
    if (error.name === 'NotAllowedError') {
      throw new Error('Biometric access was denied by user or system');
    }
    throw new Error(error.message || 'Failed to initialize biometric sensor');
  }
};

/**
 * 🔐 VERIFY biometric — called when marking attendance
 * Uses navigator.credentials.get() to trigger the native fingerprint/face prompt.
 * Returns the biometric signature string for backend verification.
 */
export const verifyBiometric = async (storedBiometricSignature?: string): Promise<string> => {
  if (!isBiometricSupported()) {
    throw new Error('Biometrics not supported on this device/browser');
  }

  try {
    console.log('🔐 [BIOMETRIC] Triggering fingerprint verification...');
    
    // Generate a fresh challenge
    const challenge = new Uint8Array(32);
    window.crypto.getRandomValues(challenge);

    // Build allowCredentials from stored signature if available
    const allowCredentials: PublicKeyCredentialDescriptor[] = [];
    
    // Try to reconstruct the credential ID from the stored biometric signature
    const credentialHex = storedBiometricSignature?.replace('WBA_', '') || 
                          localStorage.getItem('sa_webauthn_credential_id');
    
    if (credentialHex) {
      try {
        const rawId = new Uint8Array(
          credentialHex.match(/.{1,2}/g)!.map(byte => parseInt(byte, 16))
        );
        allowCredentials.push({
          type: 'public-key',
          id: rawId,
          transports: ['internal'],
        });
        console.log('🔐 [BIOMETRIC] Using stored credential ID for verification');
      } catch (e) {
        console.warn('🔐 [BIOMETRIC] Could not parse stored credential, using open verification');
      }
    }

    // Request authentication — this triggers the fingerprint/face prompt
    const assertion = await navigator.credentials.get({
      publicKey: {
        challenge,
        rpId: window.location.hostname === 'localhost' ? 'localhost' : window.location.hostname,
        allowCredentials: allowCredentials.length > 0 ? allowCredentials : undefined,
        userVerification: 'required',
        timeout: 60000,
      }
    }) as PublicKeyCredential;

    if (!assertion) {
      throw new Error('Biometric verification cancelled');
    }

    console.log('🟢 [BIOMETRIC] Fingerprint verified successfully!');

    // Return the credential rawId as the biometric signature (should match the registered one)
    const rawId = new Uint8Array(assertion.rawId);
    const signature = Array.from(rawId)
      .map(b => b.toString(16).padStart(2, '0'))
      .join('')
      .toUpperCase();

    return `WBA_${signature}`;
  } catch (error: any) {
    console.error('🔴 [BIOMETRIC] Verification failed:', error);
    if (error.name === 'NotAllowedError') {
      throw new Error('Fingerprint verification was cancelled or denied. Please try again.');
    }
    if (error.name === 'InvalidStateError') {
      throw new Error('No registered fingerprint found. Please complete setup first.');
    }
    throw new Error(error.message || 'Fingerprint verification failed');
  }
};
