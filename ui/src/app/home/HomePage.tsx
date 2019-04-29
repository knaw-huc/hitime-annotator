import * as React from "react";
import Page from "../common/Page";

class HomePage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {};
    }

    render() {
        return (
            <Page>
                <h2>Home</h2>
                <p>Lorem ipsum dolor sit amet.</p>
            </Page>
        );
    }
}

export default HomePage;