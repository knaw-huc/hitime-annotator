import * as React from "react";
import Page from "./Page";
import Loader from "./Loader";

class LoadingPage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {};
    }

    render() {
        return (
            <Page>
                <Loader />
            </Page>
        );
    }
}

export default LoadingPage;