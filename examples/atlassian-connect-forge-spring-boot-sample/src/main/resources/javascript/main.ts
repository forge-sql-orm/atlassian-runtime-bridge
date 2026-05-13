import React from 'react';
import { createRoot } from 'react-dom/client';
import { fetchHelloJson } from 'app-transport';
import {HelloMessage} from "./shared/helloBackend";

type AppState = {
    message: string | null;
    error: string | null;
    loading: boolean;
};

class App extends React.Component<{}, AppState> {
    constructor(props: {} | undefined) {
        super(props ?? {});
        this.state = { message: null, error: null, loading: true };
    }

    componentDidMount(): void {
        fetchHelloJson()
            .then((data: HelloMessage) => this.setState({ message: data.message, loading: false }))
            .catch((err: unknown) =>
                this.setState({
                    error: err instanceof Error ? err.message : String(err),
                    loading: false,
                })
            );
    }

    render(): React.ReactElement {
        let status: string | null;
        if (this.state.loading) {
            status = 'Loading…';
        } else if (this.state.error) {
            status = 'Error: ' + this.state.error;
        } else {
            status = this.state.message;
        }
        return React.createElement(
            'div',
            undefined,
            React.createElement('p', { style: { marginTop: '12px' } }, status)
        );
    }
}

function startRender(): void {
    const root = document.getElementById('react-container') ?? document.getElementById('root');
    if (root) {
        createRoot(root).render(React.createElement(App, {}));
    }
}

window.onload = startRender;
