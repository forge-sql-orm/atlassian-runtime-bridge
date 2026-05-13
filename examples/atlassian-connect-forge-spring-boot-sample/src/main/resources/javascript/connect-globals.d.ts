export {};

declare global {
    interface Window {
        AP: {
            context: {
                getToken(): Promise<string> | string;
            };
        };
    }
}
