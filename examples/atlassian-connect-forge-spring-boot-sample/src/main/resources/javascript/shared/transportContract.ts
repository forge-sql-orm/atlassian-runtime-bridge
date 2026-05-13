import type { HelloMessage } from './helloBackend';

/** Shared surface for {@code connect.ts} (iframe) and {@code forge.ts} (Forge remote). */
export interface BackendTransport {
    fetch(path: string): Promise<any>;
    fetchHelloJson(): Promise<HelloMessage>;
}
