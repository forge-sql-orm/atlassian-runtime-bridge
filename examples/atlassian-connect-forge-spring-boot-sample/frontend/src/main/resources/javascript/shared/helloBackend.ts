export const HELLO_API_PATH = '/atlaskit/api/hello';

export const CONNECT_REMOTE_KEY = 'connect';

export interface HelloMessage {
    message: string;
}

export function helloGetInit(): RequestInit {
    return {
        method: 'GET',
        headers: { Accept: 'application/json' },
    };
}

export function parseHelloJson(response: Response): Promise<HelloMessage> {
    if (!response.ok) {
        return Promise.reject(new Error('HTTP ' + response.status));
    }
    return response.json() as Promise<HelloMessage>;
}
