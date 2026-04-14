import { Client } from '@stomp/stompjs';
import { API_CONFIG } from '../utils/constants';

class WebSocketClient {
  private client: Client;

  constructor() {
    this.client = new Client({
      brokerURL: API_CONFIG.WS_URL,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: (frame) => {
        console.log('Connected to STOMP Broker:', frame);
        // Default subscriptions could go here
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
      beforeConnect: () => {
        // Assume getToken() fetches current robust JWT from memory
        const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
        if (token) {
          this.client.connectHeaders = {
            Authorization: `Bearer ${token}`
          };
        }
      }
    });
  }

  connect() {
    this.client.activate();
  }

  disconnect() {
    this.client.deactivate();
  }

  subscribe(topic: string, callback: (message: any) => void) {
    if (!this.client.connected) {
      console.warn('Cannot subscribe. STOMP client is not connected.');
      return null;
    }
    return this.client.subscribe(topic, (message) => {
      callback(JSON.parse(message.body));
    });
  }
}

export const websocketClient = new WebSocketClient();
export default websocketClient;
