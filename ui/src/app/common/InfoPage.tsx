import * as React from "react";
import Page from "./Page";
import InfoBox, {InfoBoxProps} from "./InfoBox";

/**
 * Wrapper of InfoBox
 */
class InfoPage extends React.Component<InfoBoxProps, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {};
    }

    render() {
        return (
            <Page>
                <InfoBox msg={this.props.msg} type={this.props.type} onClose={this.props.onClose}/>
            </Page>
        );
    }
}

export default InfoPage;