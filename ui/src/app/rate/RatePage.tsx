import * as React from "react";
import Page from "../common/Page";

class RatePage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {};
    }

    render() {
        return (
            <Page>
                <h2>Rate</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam eget metus volutpat, imperdiet est ac, scelerisque leo. Aenean accumsan nisi tellus. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc molestie, odio at imperdiet gravida, mi mauris convallis ligula, iaculis finibus leo nulla id enim. Mauris eu hendrerit dui, sagittis tempus leo. Suspendisse ultrices vestibulum mauris, vel sagittis augue porta ut. Nulla pharetra semper nunc, quis suscipit leo finibus eget. Aliquam feugiat finibus rutrum. Fusce pellentesque nunc in pretium facilisis. Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
            </Page>
        );
    }
}

export default RatePage;