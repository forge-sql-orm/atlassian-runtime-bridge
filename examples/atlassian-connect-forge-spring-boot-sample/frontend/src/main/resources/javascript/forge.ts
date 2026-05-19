import { invokeRemote } from '@forge/bridge';
import {
    HELLO_API_PATH,
    type HelloMessage,
} from './shared/helloBackend';
import type { BackendTransport } from './shared/transportContract';

export async function fetch(path: string): Promise<any> {
    return invokeRemote({path, method: 'GET'});
}

export async function fetchHelloJson(): Promise<HelloMessage> {
    let ret = await fetch(HELLO_API_PATH);
    return ret.body;
}

export const transport: BackendTransport = { fetch, fetchHelloJson };
