import { HELLO_API_PATH, parseHelloJson, type HelloMessage } from './shared/helloBackend';
import type { BackendTransport } from './shared/transportContract';

const AP = window.AP;

function apGetToken(): Promise<string> {
    return Promise.resolve(AP.context.getToken()).then(String);
}

export async function fetch(path: string): Promise<Response> {
    const mergedHeaders:Record<string, string> = { Accept: 'application/json' };
    return apGetToken().then((token) => {
        mergedHeaders.Authorization = 'JWT ' + token;
        return window.fetch(path, {
            credentials: 'include',
            headers: mergedHeaders,
        });
    });
}

export async function fetchHelloJson(): Promise<HelloMessage> {
    return fetch(HELLO_API_PATH).then(parseHelloJson);
}

export const transport: BackendTransport = { fetch, fetchHelloJson };
