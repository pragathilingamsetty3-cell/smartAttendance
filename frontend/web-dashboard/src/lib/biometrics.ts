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

    // In a real production system, we would send the credential.response.attestationObject
    // to the backend for verification.
    // For this implementation, we extract a unique hash to represent the biometric signature.
    const rawId = new Uint8Array(credential.rawId);
    const signature = Array.from(rawId)
      .map(b => b.toString(16).padStart(2, '0'))
      .join('')
      .toUpperCase();

    return `WBA_${signature}`;
  } catch (error: any) {
    console.error('Biometric error:', error);
    if (error.name === 'NotAllowedError') {
      throw new Error('Biometric access was denied by user or system');
    }
    throw new Error(error.message || 'Failed to initialize biometric sensor');
  }
};
