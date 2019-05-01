import * as React from "react";
import Page from "./Page";

class LoadingPage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {};
    }

    render() {
        return (
            <Page>
                <div className="text-center"><i className="fa fa-spinner fa-pulse fa-1x fa-fw"/></div>
            </Page>
        );
    }
}

export default LoadingPage;