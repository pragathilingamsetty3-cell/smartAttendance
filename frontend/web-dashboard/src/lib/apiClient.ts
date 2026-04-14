import axios from "axios";
import { getDeviceFingerprint } from "./utils";
import { useAuthStore } from "../stores/authStore";
import { API_CONFIG } from "../utils/constants";

const apiClient = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true, // Crucial for sending/receiving the HTTPOnly Refresh Cookie
});

// ZERO-TRUST Interceptor: Inject Authorization & deviceFingerprint
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  const fp = getDeviceFingerprint();

  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  if (config.headers) {
    // Injecting Device Fingerprint (simulated via Random UUID or FingerprintJS if available)
    config.headers["X-Device-Fingerprint"] = fp;

    // Fallback body injection removed - fingerprint belongs in header X-Device-Fingerprint
  }

  return config;
});

// Response Interceptor for handling 401 & Silent Refresh logic
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Trigger refresh on 401 (Unauthorized)
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const authStore = useAuthStore.getState();
        const refreshToken = authStore.refreshToken;
        
        if (!refreshToken) {
          authStore.logout();
          return Promise.reject(error);
        }

        console.log("🔄 Access token expired. Attempting silent refresh...");

        // Refresh logic strictly bound to /api/v1/auth/refresh-token
        const { data } = await axios.post(
          `${apiClient.defaults.baseURL}/api/v1/auth/refresh-token`,
          { refreshToken: refreshToken },
          { 
            headers: { "X-Device-Fingerprint": getDeviceFingerprint() }, 
            withCredentials: true 
          }
        );

        const newToken = data.token || data.accessToken;
        authStore.setToken(newToken);

        console.log("✅ Token refreshed successfully.");

        // Update original request and retry
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);

      } catch (refreshError) {
        console.error("🚨 Refresh token failed or expired. Logging out...");
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
